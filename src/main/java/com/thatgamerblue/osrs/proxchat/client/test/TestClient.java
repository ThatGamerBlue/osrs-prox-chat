package com.thatgamerblue.osrs.proxchat.client.test;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.thatgamerblue.osrs.proxchat.common.net.messages.test.C2STestPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.test.S2CTestPacket;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Simple test client, created so I can get familiar with how kryonet works
 */
public class TestClient
{
	/**
	 * Main entrypoint for the test client
	 *
	 * @param args any command line arguments
	 * @throws IOException if an error occurs. It's a test client nobody cares about exceptions.
	 */
	public static void main(String[] args) throws IOException
	{
		Log.set(Log.LEVEL_TRACE);
		TestClientNetworkHandler netHandler = new TestClientNetworkHandler(
			() -> "mainland.server",
			() -> 30666
		);
		netHandler.connect();
		// block forever because the client thread is a daemon
		while (true)
		{
			;
		}
	}

	/**
	 * Class for handling connections to the test client
	 */
	static class TestClientNetworkHandler
	{
		/**
		 * Kryonet client instance
		 */
		Client client = new Client();
		/**
		 * Executor for sending packets on a delay
		 */
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		/**
		 * Hostname/IP address of the server
		 */
		Supplier<String> host;
		/**
		 * Port of the server
		 */
		Supplier<Integer> port;

		/**
		 * Constructs a new TestClientNetworkHandler with the hostname and port supplied
		 *
		 * @param host hostname/ip address of the server
		 * @param port port of the server
		 */
		public TestClientNetworkHandler(
			Supplier<String> host,
			Supplier<Integer> port
		)
		{
			this.host = host;
			this.port = port;
		}

		/**
		 * Initialize kryonet and connect to the server
		 *
		 * @throws IOException if connecting to the server fails
		 */
		public void connect() throws IOException
		{
			initKryonet();
			doConnect();
		}

		/**
		 * Registers all the packets, serializers and listeners used
		 */
		private void initKryonet()
		{
			client.getKryo().register(S2CTestPacket.class);
			client.getKryo().register(C2STestPacket.class);

			client.addListener(new Listener()
			{
				@Override
				public void received(Connection connection, Object object)
				{
					if (object instanceof S2CTestPacket)
					{
						System.out.println("recv: " + ((S2CTestPacket) object).s);
						executor.schedule(() -> connection.sendTCP(new C2STestPacket("Client to server")), 10, TimeUnit.SECONDS);
					}
				}
			});
		}

		/**
		 * Connects to the server on the provided port/tcp
		 *
		 * @throws IOException if connecting to the server fails
		 */
		private void doConnect() throws IOException
		{
			client.start();
			client.connect(5000, host.get(), port.get());
		}
	}
}
