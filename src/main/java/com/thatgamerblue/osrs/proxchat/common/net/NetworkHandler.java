package com.thatgamerblue.osrs.proxchat.common.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SAuth;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SMicPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SUpdatePacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CAuthReq;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CKillDecoder;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CMicPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CUpdateReq;
import com.thatgamerblue.osrs.proxchat.common.serializers.ByteArySerializer;
import com.thatgamerblue.osrs.proxchat.common.serializers.UUIDSerializer;
import java.io.IOException;
import java.util.UUID;

/**
 * Superclass for both client and server networking classes.
 * Contains common code such as message receiving and initialization of kryo.
 */
@SuppressWarnings("unused")
public abstract class NetworkHandler
{
	/**
	 * Holds the {@link com.esotericsoftware.kryonet.Client} instance of this NetworkHandler
	 * Null on the server side.
	 */
	protected Client netClient;
	/**
	 * Holds the {@link com.esotericsoftware.kryonet.Server} instance of this NetworkHandler
	 * Null on the client side.
	 */
	protected Server netServer;

	/**
	 * Determines if we're acting as the server or as a client
	 */
	protected final Mode networkMode;

	/**
	 * Should not be initialized directly, rather as a subclass, usually
	 * {@link com.thatgamerblue.osrs.proxchat.client.net.ClientNetworkHandler} or
	 * {@link com.thatgamerblue.osrs.proxchat.server.net.ServerNetworkHandler}
	 *
	 * @param mode operating mode to enter
	 */
	protected NetworkHandler(Mode mode)
	{
		this.networkMode = mode;
	}

	/**
	 * Initializes everything to do with Kryonet.
	 * In shared code because kryonet is finicky about initialization order
	 */
	public void initKryonet()
	{
		// grab the correct instance of kryo based off the operating mode
		Kryo kryo;
		switch (networkMode)
		{
			case CLIENT:
				netClient = new Client(16384, 16384);
				kryo = netClient.getKryo();
				break;
			case SERVER:
				netServer = new Server(16384, 16384);
				kryo = netServer.getKryo();
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + networkMode);
		}

		// register used types
		kryo.register(byte[].class, new ByteArySerializer());
		kryo.register(UUID.class, new UUIDSerializer());

		// register C2S packets
		kryo.register(C2SAuth.class);
		kryo.register(C2SMicPacket.class);
		kryo.register(C2SUpdatePacket.class, new C2SUpdatePacket.Serializer());

		// register S2C packets
		kryo.register(S2CAuthReq.class, new S2CAuthReq.Serializer());
		kryo.register(S2CKillDecoder.class);
		kryo.register(S2CMicPacket.class, new S2CMicPacket.Serializer());
		kryo.register(S2CUpdateReq.class);

		EndPoint endPoint;

		switch (networkMode)
		{
			case CLIENT:
				endPoint = netClient;
				break;
			case SERVER:
				endPoint = netServer;
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + networkMode);
		}

		endPoint.addListener(new Listener()
		{
			@Override
			public void connected(Connection connection)
			{
				onConnected(connection);
			}

			@Override
			public void disconnected(Connection connection)
			{
				onDisconnected(connection);
			}

			@Override
			public void received(Connection connection, Object o)
			{
				onMessageReceived(connection, o);
			}
		});
	}

	/**
	 * Initializes networking, should be called AFTER {@link NetworkHandler#initKryonet()}
	 * Creates listener on the server, connects to the server on the client
	 *
	 * @throws java.io.IOException when any networking errors occur
	 */
	public abstract void connect() throws IOException;

	/**
	 * Called when a successful connection is established
	 *
	 * @param connection connection object from kryonet
	 */
	protected abstract void onConnected(Connection connection);

	/**
	 * Called when a message is received over the network
	 *
	 * @param connection connection object from kryonet
	 * @param message    message object deserialized by kryonet
	 */
	protected abstract void onMessageReceived(Connection connection, Object message);

	/**
	 * Called when a connection is disconnected
	 *
	 * @param connection connection object from kryonet
	 */
	protected abstract void onDisconnected(Connection connection);

	/**
	 * Disconnects all open connections and releases all memory used by natives
	 * Makes no guarantees about the re-usability of this instance.
	 *
	 * @throws java.io.IOException when any networking errors occur
	 */
	public abstract void disconnect() throws IOException;

	/**
	 * Enum for holding possible operating modes.
	 */
	public enum Mode
	{
		/**
		 * Operating mode for acting as a CLIENT
		 */
		CLIENT,
		/**
		 * Operating mode for active as a SERVER
		 */
		SERVER;
	}
}
