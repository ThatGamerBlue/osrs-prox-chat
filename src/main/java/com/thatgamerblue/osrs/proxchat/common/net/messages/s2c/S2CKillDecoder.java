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
 * A packet sent from server to client signalling the end of a decoder
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class S2CKillDecoder
{
	/**
	 * The UUID of the decoder to kill
	 */
	public UUID uuid;

	/**
	 * Serializes a S2CKillDecoder packet to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<S2CKillDecoder>
	{
		/**
		 * Serialize to binary
		 *
		 * @param kryo   gets serializers for other types
		 * @param output output stream
		 * @param packet packet to write
		 */
		@Override
		public void write(Kryo kryo, Output output, S2CKillDecoder packet)
		{
			kryo.getSerializer(UUID.class).write(kryo, output, packet.uuid);
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
		public S2CKillDecoder read(Kryo kryo, Input input, Class<S2CKillDecoder> aClass)
		{
			return new S2CKillDecoder((UUID) kryo.getSerializer(UUID.class).read(kryo, input, UUID.class));
		}
	}
}
