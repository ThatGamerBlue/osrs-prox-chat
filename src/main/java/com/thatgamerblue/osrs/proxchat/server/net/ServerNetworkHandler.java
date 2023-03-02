package com.thatgamerblue.osrs.proxchat.server.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import com.thatgamerblue.osrs.proxchat.common.net.NetworkHandler;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SAuth;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SMicPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SUpdatePacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CAuthReq;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CKillDecoder;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CMicPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CUpdateReq;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.runelite.api.Perspective;

/**
 * Network handler for the server side.
 * Entrypoint for all packets received from the client
 */
public class ServerNetworkHandler extends NetworkHandler
{
	/**
	 * A secure random instance used for generating nonces
	 */
	private static Random random;

	/**
	 * Holds clients that are waiting for authentication
	 */
	private static final ConcurrentHashMap<Integer, Integer> nonceMap = new ConcurrentHashMap<>();

	/**
	 * Holds clients that are authenticated
	 */
	private static final CopyOnWriteArrayList<Integer> authenticatedClients = new CopyOnWriteArrayList<>();

	/**
	 * Holds game states of clients
	 */
	private static final ConcurrentHashMap<Integer, ClientState> clientStates = new ConcurrentHashMap<>();

	/**
	 * Holds an executor used for doing time-based cleanup e.g. of the nonce map
	 */
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	static
	{
		try
		{
			random = SecureRandom.getInstance("SHA1PRNG");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			System.out.println("Failed to initialize secure random instance, security may be diminished.");
			random = new Random();
		}
	}

	/**
	 * Holds the bind address for the server
	 */
	private final Supplier<String> bindAddress;

	/**
	 * Holds the port for the server to bind to
	 */
	private final Supplier<Integer> port;

	/**
	 * Holds the pre-shared key of the server
	 */
	private final Supplier<String> password;

	/**
	 * Are we shutting down?
	 */
	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

	/**
	 * Initializes an {@link NetworkHandler} in server operating mode
	 * Handles all connections to clients.
	 * Mainly just acts as a relay for audio.
	 *
	 * @param bindAddress address supplier, see {@link #bindAddress}
	 * @param port        port to bind to
	 * @param password    password to require clients to provide
	 */
	public ServerNetworkHandler(
		Supplier<String> bindAddress,
		Supplier<Integer> port,
		Supplier<String> password
	)
	{
		super(Mode.SERVER);
		this.bindAddress = bindAddress;
		this.port = port;
		this.password = password;
	}

	/**
	 * Sends an object to a specific client over the network
	 *
	 * @param connection where to send the object to
	 * @param object     object to send
	 */
	public void sendTCP(int connection, Object object)
	{
		netServer.sendToTCP(connection, object);
	}

	/**
	 * Sends an object to everyone except one client over the network
	 *
	 * @param connection where to not send the object to
	 * @param object     object to send
	 */
	public void sendTCPToAllExcept(int connection, Object object)
	{
		netServer.sendToAllExceptTCP(connection, object);
	}

	/**
	 * Opens the listen server on the address supplied by {@link #bindAddress}
	 *
	 * @throws java.io.IOException if the initialization fails, for example due to port being in use
	 */
	@Override
	public void connect() throws IOException
	{
		shuttingDown.set(false);
		netServer.start();
		netServer.bind(new InetSocketAddress(bindAddress.get(), port.get()), null);
	}

	/**
	 * Called when a client connects to the server
	 *
	 * @param connection connection object from kryonet
	 */
	@Override
	protected void onConnected(Connection connection)
	{
		int nonce = random.nextInt();
		nonceMap.put(connection.getID(), nonce);
		System.out.println("Received connection w/ id " + connection.getID() + ", sending back nonce " + nonce);
		executor.schedule(() ->
		{
			nonceMap.remove(connection.getID());

			if (connection.isConnected() && !authenticatedClients.contains(connection.getID()))
			{
				connection.close();
			}

		}, 30, TimeUnit.SECONDS);
		sendTCP(connection.getID(), new S2CAuthReq(nonce));
	}

	/**
	 * Called when a client sends a message to the server
	 *
	 * @param connection connection object from kryonet
	 * @param message    message object deserialized by kryonet
	 */
	@Override
	protected void onMessageReceived(Connection connection, Object message)
	{
		if (message instanceof FrameworkMessage)
		{
			// dont care about these messages
			return;
		}

		if (message instanceof C2SAuth)
		{
			Integer nonce;
			if ((nonce = nonceMap.get(connection.getID())) == null)
			{
				System.out.println("Closing connection with " + connection.getID() + " due to lack of nonce");
				connection.close();
				return;
			}

			nonceMap.remove(connection.getID());

			// if password is not empty, do authorization
			if (!Strings.isNullOrEmpty(password.get()))
			{
				byte[] provided = ((C2SAuth) message).password;
				if (provided == null)
				{
					connection.close();
					return;
				}
				byte[] serverPw = password.get().getBytes(StandardCharsets.UTF_8);

				Random rand = new Random(nonce);
				for (int i = 0; i < serverPw.length; i++)
				{
					serverPw[i] = (byte) (serverPw[i] ^ rand.nextInt());
				}

				byte[] expected = Hashing.sha256().hashBytes(serverPw).asBytes();

				if (provided.length != expected.length)
				{
					connection.close();
					return;
				}

				int result = 0;
				for (int i = 0; i < expected.length; i++)
				{
					result |= provided[i] ^ expected[i];
				}

				if (result != 0)
				{
					System.out.println("Closing connection with " + connection.getID() + " due to an invalid password");
					connection.close();
					return;
				}
			}

			authenticatedClients.add(connection.getID());

			sendTCP(connection.getID(), new S2CUpdateReq());

			System.out.println("Client " + connection.getID() + " successfully authorized.");

			// because of this return we can now hoist out the auth check
			return;
		}

		// authentication check
		if (!authenticatedClients.contains(connection.getID()))
		{
			connection.close();
			return;
		}

		if (message instanceof C2SUpdatePacket)
		{
			C2SUpdatePacket update = (C2SUpdatePacket) message;
			ClientState oldState;
			UUID uuid;
			if ((oldState = clientStates.get(connection.getID())) != null)
			{
				uuid = oldState.getUuid();
			}
			else
			{
				uuid = UUID.randomUUID();
			}

			clientStates.put(connection.getID(), new ClientState(uuid, update.x, update.y, update.plane, update.world, update.gameState, update.room));
		}
		else if (message instanceof C2SMicPacket)
		{
			C2SMicPacket micPacket = (C2SMicPacket) message;

			if (micPacket.data == null)
			{
				return;
			}

			int sender = connection.getID();
			ClientState senderState = clientStates.get(sender);

			if (senderState == null)
			{
				sendTCP(sender, new S2CUpdateReq());
				return;
			}

			for (Map.Entry<Integer, ClientState> e : clientStates.entrySet())
			{
				int k = e.getKey();
				ClientState v = e.getValue();
				if (k == sender)
				{
					continue;
				}

				int dist;
				if ((dist = senderState.distanceTo(v)) > AudioConstants.MAX_DISTANCE)
				{
					continue;
				}

				sendTCP(k, new S2CMicPacket(senderState.getUuid(), micPacket.data, dist));
			}
		}
	}

	/**
	 * Called when a client disconnects from the server
	 *
	 * @param connection connection object from kryonet
	 */
	@Override
	protected void onDisconnected(Connection connection)
	{
		System.out.println("Client " + connection.getID() + " disconnected.");
		nonceMap.remove(connection.getID());
		authenticatedClients.remove((Object) connection.getID());
		ClientState state = clientStates.remove(connection.getID());
		if (!shuttingDown.get())
		{
			if (state != null)
			{
				sendTCPToAllExcept(connection.getID(), new S2CKillDecoder(state.getUuid()));
			}
		}
	}

	/**
	 * Disconnects all open client connections and closes the server for connections.
	 * Reusable after calling {@link ServerNetworkHandler#connect()}
	 */
	@Override
	public void disconnect()
	{
		netServer.stop();
	}

	/**
	 * Destroys any long-lasting instances this class holds. Will not be reusable after this is called.
	 * Also calls {@link #disconnect()}
	 */
	public void shutdown()
	{
		System.out.println("Shutting down network...");
		shuttingDown.set(true);
		disconnect();
		System.out.println("Shutting down executor...");
		executor.shutdown();
	}
}
