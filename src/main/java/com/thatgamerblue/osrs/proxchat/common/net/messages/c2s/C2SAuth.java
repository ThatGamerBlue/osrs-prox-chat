package com.thatgamerblue.osrs.proxchat.common.net.messages.c2s;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from client to server to authenticate. Uses the number in the S2CHello to help prevent replay attacks
 * <p>
 * I know this isn't secure by any stretch of the imagination, but I don't care enough to fix it.
 * Use a strong random password and just don't get sniffed 4Head
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class C2SAuth
{
	/**
	 * The password to authenticate with the server
	 */
	public byte[] password;

	/**
	 * Serializes a C2SAuth packet to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<C2SAuth>
	{
		/**
		 * Serialize to binary
		 *
		 * @param kryo   gets serializers for other types
		 * @param output output stream
		 * @param packet packet to write
		 */
		@Override
		public void write(Kryo kryo, Output output, C2SAuth packet)
		{
			kryo.getSerializer(byte[].class).write(kryo, output, packet.password);
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
		public C2SAuth read(Kryo kryo, Input input, Class<C2SAuth> aClass)
		{
			return new C2SAuth((byte[]) kryo.getSerializer(byte[].class).read(kryo, input, byte[].class));
		}
	}
}
