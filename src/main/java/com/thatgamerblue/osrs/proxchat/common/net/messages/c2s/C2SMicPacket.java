package com.thatgamerblue.osrs.proxchat.common.net.messages.c2s;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from client to server containing microphone data
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class C2SMicPacket
{
	/**
	 * Microphone data
	 */
	public byte[] data;

	/**
	 * Serializes a C2SMicPacket packet to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<C2SMicPacket>
	{
		/**
		 * Serialize to binary
		 *
		 * @param kryo   gets serializers for other types
		 * @param output output stream
		 * @param packet packet to write
		 */
		@Override
		public void write(Kryo kryo, Output output, C2SMicPacket packet)
		{
			kryo.getSerializer(byte[].class).write(kryo, output, packet.data);
		}

		/**
		 * Deserialize from binary
		 *
		 * @param kryo   unused
		 * @param input  input stream
		 * @param aClass unused
		 * @return deserialized packet
		 */
		@Override
		public C2SMicPacket read(Kryo kryo, Input input, Class<C2SMicPacket> aClass)
		{
			return new C2SMicPacket((byte[]) kryo.getSerializer(byte[].class).read(kryo, input, byte[].class));
		}
	}
}
