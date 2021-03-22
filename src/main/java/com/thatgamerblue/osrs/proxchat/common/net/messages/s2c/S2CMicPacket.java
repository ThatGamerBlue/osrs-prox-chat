package com.thatgamerblue.osrs.proxchat.common.net.messages.s2c;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from server to client containing audio data to play
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class S2CMicPacket
{
	/**
	 * UUID of the decoder to use
	 */
	public UUID decoder;
	/**
	 * Audio data to decode
	 */
	public byte[] data;
	/**
	 * Distance away of the transmitting player
	 */
	public int distance;

	/**
	 * Serializes a S2CMicPacket to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<S2CMicPacket>
	{
		/**
		 * Serialize to binary
		 *
		 * @param kryo   gets serializers for other types
		 * @param output output stream
		 * @param packet packet to write
		 */
		@Override
		public void write(Kryo kryo, Output output, S2CMicPacket packet)
		{
			kryo.getSerializer(UUID.class).write(kryo, output, packet.decoder);
			kryo.getSerializer(byte[].class).write(kryo, output, packet.data);
			output.writeInt(packet.distance);
		}

		/**
		 * Deserialize from binary
		 *
		 * @param kryo   gets serializers for other types
		 * @param input  input stream
		 * @param aClass unused
		 * @return deserialized packet
		 */
		@Override
		public S2CMicPacket read(Kryo kryo, Input input, Class<S2CMicPacket> aClass)
		{
			UUID uuid = (UUID) kryo.getSerializer(UUID.class).read(kryo, input, UUID.class);
			byte[] data = (byte[]) kryo.getSerializer(byte[].class).read(kryo, input, byte[].class);
			return new S2CMicPacket(uuid, data, input.readInt());
		}
	}
}
