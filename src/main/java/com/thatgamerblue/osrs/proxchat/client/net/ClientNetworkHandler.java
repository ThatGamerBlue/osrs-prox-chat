package com.thatgamerblue.osrs.proxchat.client.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.google.common.hash.Hashing;
import com.thatgamerblue.osrs.proxchat.client.ProxChatClientPlugin;
import com.thatgamerblue.osrs.proxchat.client.audio.SpeakerThread;
import com.thatgamerblue.osrs.proxchat.common.net.NetworkHandler;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SAuth;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SMicPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SUpdatePacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CAuthReq;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CKillDecoder;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CMicPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CUpdateReq;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

/**
 * Network handler for the client side.
 * Entrypoint for all packets received from the server
 */
@Slf4j
public class ClientNetworkHandler extends NetworkHandler
{
	/**
	 * Instance of the plugin controlling this instance
	 */
	private final ProxChatClientPlugin plugin;

	/**
	 * Instance of the RS client
	 */
	private final Client client;

	/**
	 * Holds the server's address for connection
	 */
	private final Supplier<String> connectionAddress;

	/**
	 * Holds the port open on the server
	 */
	private final Supplier<Integer> port;

	/**
	 * Holds the pre-shared key of the server
	 */
	private final Supplier<String> password;

	/**
	 * Holds all the Opus decoder instances this handler controls
	 */
	private final ConcurrentHashMap<UUID, SpeakerThread> speakers = new ConcurrentHashMap<>();

	/**
	 * Holds the speaker's muted state. Held here so we can drop packets early to not bother the speaker threads
	 */
	@Getter
	private final AtomicBoolean speakerMuted = new AtomicBoolean(false);

	/**
	 * Holds the mic's transmitting state. Held here because the speakerMuted value is too
	 */
	@Getter
	private final AtomicBoolean micTransmitting = new AtomicBoolean(false);

	/**
	 * Whether or not we should attempt a reconnect after we get disconnected
	 */
	private final AtomicBoolean reconnect = new AtomicBoolean(false);

	/**
	 * Whether or not we are currently attempting to connect to the server
	 */
	private final AtomicBoolean connecting = new AtomicBoolean(false);

	/**
	 * Maximum value for the backoff timer in seconds
	 * <p>
	 * 4 minutes
	 */
	private static final int BACKOFF_TIMER_MAX = 240;

	/**
	 * Minimum value for the backoff timer in seconds
	 * <p>
	 * 30 seconds
	 */
	private static final int BACKOFF_TIMER_MIN = 30;

	/**
	 * Exponential backoff timer in seconds
	 */
	private int backoffTimer = BACKOFF_TIMER_MIN;

	/**
	 * Future holding the task execution of the connection
	 */
	private ScheduledFuture<?> connectionFuture;

	/**
	 * Initializes an {@link NetworkHandler} in client operating mode
	 * Handles all connections to the server.
	 * Handles memory management of OPUS encoders and decoders.
	 *
	 * @param plugin            instance of the plugin that controls this
	 * @param client            instance of the RS client
	 * @param connectionAddress address supplier, see {@link #connectionAddress}
	 * @param port              port of the voice server
	 * @param password          password of the voice server
	 */
	public ClientNetworkHandler(
		ProxChatClientPlugin plugin,
		Client client,
		Supplier<String> connectionAddress,
		Supplier<Integer> port,
		Supplier<String> password
	)
	{
		super(Mode.CLIENT);
		this.plugin = plugin;
		this.client = client;
		this.connectionAddress = connectionAddress;
		this.port = port;
		this.password = password;
	}

	/**
	 * Initializes networking, should be called AFTER {@link NetworkHandler#initKryonet()}
	 * Connects to the server
	 */
	@Override
	public void connect()
	{
		if (connecting.get())
		{
			return;
		}

		backoffTimer = BACKOFF_TIMER_MIN;
		connecting.set(true);

		doConnect();
	}

	private void doConnect()
	{
		try
		{
			netClient.start();
			netClient.connect(5000, connectionAddress.get(), port.get());

			connecting.set(false);
			log.info("Connected to voice server");
		}
		catch (IOException ex)
		{
			log.error("Failed to connect to the server, retrying in " + backoffTimer + " seconds.");
			connectionFuture = plugin.getExecutor().schedule(this::doConnect, backoffTimer, TimeUnit.SECONDS);
			backoffTimer *= 2;
			if (backoffTimer > BACKOFF_TIMER_MAX)
			{
				backoffTimer = BACKOFF_TIMER_MAX;
			}
		}
	}

	/**
	 * Called when a successful connection is established
	 *
	 * @param connection connection object from kryonet
	 */
	@Override
	protected void onConnected(Connection connection)
	{

	}

	/**
	 * Called when a message is received over the network
	 *
	 * @param connection connection object from kryonet
	 * @param message    message object deserialized by kryonet
	 */
	@Override
	protected void onMessageReceived(Connection connection, Object message)
	{
		if (!(message instanceof FrameworkMessage) && !(message instanceof S2CMicPacket))
		{
			log.info("Recv message: {}", message);
		}

		if (message instanceof S2CAuthReq)
		{
			Random rand = new Random(((S2CAuthReq) message).replay);
			byte[] pw = password.get().getBytes(StandardCharsets.UTF_8);
			for (int i = 0; i < pw.length; i++)
			{
				pw[i] = (byte) (pw[i] ^ rand.nextInt());
			}

			byte[] fin = Hashing.sha256().hashBytes(pw).asBytes();

			sendTCP(new C2SAuth(fin));
		}
		else if (message instanceof S2CUpdateReq)
		{
			sendUpdate(client.getGameState().getState());
		}
		else if (message instanceof S2CKillDecoder)
		{
			UUID uuid = ((S2CKillDecoder) message).uuid;
			SpeakerThread speaker = speakers.get(uuid);
			if (speaker != null)
			{
				speaker.destroy();
				speakers.remove(uuid);
			}
		}
		else if (message instanceof S2CMicPacket)
		{
			S2CMicPacket micPacket = (S2CMicPacket) message;

			if (micPacket.distance < 0 || micPacket.distance > 15)
			{
				// drop the packet, something's gone wrong
				return;
			}

			if (speakerMuted.get())
			{
				// speaker is muted, don't bother the decoders
				return;
			}

			UUID decoderId = micPacket.decoder;
			SpeakerThread speaker = speakers.get(decoderId);
			if (speaker == null)
			{
				speaker = new SpeakerThread(decoderId, () -> plugin.getConfig().speakerVolume());
				speakers.put(decoderId, speaker);
				speaker.start();
			}

			speaker.push(micPacket);
		}
	}

	/**
	 * Is this instance connected to a server
	 *
	 * @return true if this instance is connected to a server
	 */
	public boolean isConnected()
	{
		return netClient.isConnected();
	}

	/**
	 * Sends an object over the network, synchronized on {@code this}
	 *
	 * @param object object to send
	 */
	public void sendTCP(Object object)
	{
		if (object instanceof C2SMicPacket)
		{
			micTransmitting.set(true);
		}

		netClient.sendTCP(object);
	}

	/**
	 * Called when a connection is disconnected
	 *
	 * @param connection connection object from kryonet
	 */
	@Override
	protected void onDisconnected(Connection connection)
	{
		if (!reconnect.get())
		{
			return;
		}

		this.connect();
	}

	/**
	 * Sends an update about the client's current state to the server
	 *
	 * @param gameState current game state
	 */
	public void sendUpdate(int gameState)
	{
		int x, y, plane, world;
		if (client.getLocalPlayer() != null)
		{
			WorldPoint wp = client.getLocalPlayer().getWorldLocation();
			x = wp.getX();
			y = wp.getY();
			plane = client.getPlane();
			world = client.getWorld();
		}
		else
		{
			x = y = plane = world = -1;
		}

		if (!netClient.isConnected())
		{
			return;
		}

		if (gameState != 30)
		{
			return;
		}

		sendTCP(new C2SUpdatePacket(x, y, plane, world, gameState));
	}

	/**
	 * Disconnects the open server connection and destroys all Opus codec instances.
	 * Reusable after calling {@link ClientNetworkHandler#connect()}
	 */
	@Override
	public void disconnect()
	{
		reconnect.set(false);
		netClient.stop();
		speakers.forEach((u, s) -> s.destroy());
		speakers.clear();
	}

	/**
	 * Cancels any waiting connection attempts
	 */
	public void cancelConnecting()
	{
		connectionFuture.cancel(false);
		connecting.set(false);
	}

	/**
	 * Atomically sets {@link #speakerMuted} to the opposite of its current value
	 * <p>
	 * Source: https://stackoverflow.com/a/1255633
	 */
	public void toggleSpeakerMute()
	{
		boolean v;
		do
		{
			v = speakerMuted.get();
		}
		while (!speakerMuted.compareAndSet(v, !v));
	}
}
