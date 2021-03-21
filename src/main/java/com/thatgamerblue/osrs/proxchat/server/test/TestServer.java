package com.thatgamerblue.osrs.proxchat.server.test;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.thatgamerblue.osrs.proxchat.common.net.messages.test.C2STestPacket;
import com.thatgamerblue.osrs.proxchat.common.net.messages.test.S2CTestPacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple test server, created so I can get familiar with how kryonet works
 */
public class TestServer
{
	/**
	 * Main entrypoint of the test server
	 *
	 * @param args any commandline arguments
	 * @throws IOException if an error occurs. It's a test server nobody cares about exceptions.
	 */
	public static void main(String[] args) throws IOException
	{
		TestServerNetworkHandler handler = new TestServerNetworkHandler();
		handler.connect();
	}

	/**
	 * Class for handing connections for the test server
	 */
	static class TestServerNetworkHandler
	{
		/**
		 * Kryonet server instance
		 */
		Server server = new Server(16384, 16384);
		/**
		 * Executor for sending packets on a delay
		 */
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

		/**
		 * Initialize kryonet and start listening
		 *
		 * @throws IOException if starting the listener fails
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
			server.getKryo().register(S2CTestPacket.class);
			server.getKryo().register(C2STestPacket.class);

			server.addListener(new Listener()
			{
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void received(Connection connection, Object object)
				{
					if (object instanceof C2STestPacket)
					{
						System.out.println("recv: " + ((C2STestPacket) object).s);
						executor.schedule(() -> connection.sendTCP(new S2CTestPacket("Server to client")), 10, TimeUnit.SECONDS);
					}
				}

				/**
				 * {@inheritDoc}
				 */
				@Override
				public void connected(Connection connection)
				{
					System.out.println("New connection id: " + connection.getID());
					connection.sendTCP(new S2CTestPacket("Server to client"));
				}
			});
		}

		/**
		 * Binds to port 30666/tcp on all interfaces and starts listening
		 *
		 * @throws IOException if binding fails
		 */
		private void doConnect() throws IOException
		{
			server.start();
			server.bind(new InetSocketAddress("0.0.0.0", 30666), null);
		}
	}
}
