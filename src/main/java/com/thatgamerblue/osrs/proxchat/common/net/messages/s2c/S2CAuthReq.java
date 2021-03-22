package com.thatgamerblue.osrs.proxchat.common.net.messages.s2c;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from server to client acknowledging its existence and providing a number to help prevent replay attacks
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class S2CAuthReq
{
	/**
	 * An int used to help prevent replay attacks
	 */
	public int replay;

	/**
	 * Serializes a S2CAuthReq packet to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<S2CAuthReq>
	{
		/**
		 * Serialize to binary
		 *
		 * @param kryo   unused
		 * @param output output stream
		 * @param packet packet to write
		 */
		@Override
		public void write(Kryo kryo, Output output, S2CAuthReq packet)
		{
			output.writeInt(packet.replay);
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
		public S2CAuthReq read(Kryo kryo, Input input, Class<S2CAuthReq> aClass)
		{
			return new S2CAuthReq(input.readInt());
		}
	}
}
