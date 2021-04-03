
package com.esotericsoftware.kryonet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;

/** Marker interface to denote that a message is used by the Ninja framework and is generally invisible to the developer. Eg, these
 * messages are only logged at the {@link Log#LEVEL_TRACE} level.
 * @author Nathan Sweet <misc@n4te.com> */
public interface FrameworkMessage {
	static final FrameworkMessage.KeepAlive keepAlive = new KeepAlive();

	/** Internal message to give the client the server assigned connection ID. */
	static public class RegisterTCP implements FrameworkMessage {
		public int connectionID;

		public static class Serializer extends com.esotericsoftware.kryo.Serializer<RegisterTCP>
		{
			@Override
			public void write(Kryo kryo, Output output, RegisterTCP packet)
			{
				output.writeInt(packet.connectionID);
			}

			@Override
			public RegisterTCP read(Kryo kryo, Input input, Class<RegisterTCP> aClass)
			{
				RegisterTCP packet = new RegisterTCP();
				packet.connectionID = input.readInt();
				return packet;
			}
		}
	}

	/** Internal message to give the server the client's UDP port. */
	static public class RegisterUDP implements FrameworkMessage {
		public int connectionID;

		public static class Serializer extends com.esotericsoftware.kryo.Serializer<RegisterUDP>
		{
			@Override
			public void write(Kryo kryo, Output output, RegisterUDP packet)
			{
				output.writeInt(packet.connectionID);
			}

			@Override
			public RegisterUDP read(Kryo kryo, Input input, Class<RegisterUDP> aClass)
			{
				RegisterUDP packet = new RegisterUDP();
				packet.connectionID = input.readInt();
				return packet;
			}
		}
	}

	/** Internal message to keep connections alive. */
	static public class KeepAlive implements FrameworkMessage {
		public static class Serializer extends com.esotericsoftware.kryo.Serializer<KeepAlive>
		{
			@Override
			public void write(Kryo kryo, Output output, KeepAlive s2CUpdateReq)
			{

			}

			@Override
			public KeepAlive read(Kryo kryo, Input input, Class<KeepAlive> aClass)
			{
				return new KeepAlive();
			}
		}
	}

	/** Internal message to discover running servers. */
	static public class DiscoverHost implements FrameworkMessage {
		public static class Serializer extends com.esotericsoftware.kryo.Serializer<DiscoverHost>
		{
			@Override
			public void write(Kryo kryo, Output output, DiscoverHost s2CUpdateReq)
			{

			}

			@Override
			public DiscoverHost read(Kryo kryo, Input input, Class<DiscoverHost> aClass)
			{
				return new DiscoverHost();
			}
		}
	}

	/** Internal message to determine round trip time. */
	static public class Ping implements FrameworkMessage {
		public int id;
		public boolean isReply;

		public static class Serializer extends com.esotericsoftware.kryo.Serializer<Ping>
		{
			@Override
			public void write(Kryo kryo, Output output, Ping packet)
			{
				output.writeInt(packet.id);
				output.writeBoolean(packet.isReply);
			}

			@Override
			public Ping read(Kryo kryo, Input input, Class<Ping> aClass)
			{
				Ping packet = new Ping();
				packet.id = input.readInt();
				packet.isReply = input.readBoolean();
				return packet;
			}
		}
	}
}
