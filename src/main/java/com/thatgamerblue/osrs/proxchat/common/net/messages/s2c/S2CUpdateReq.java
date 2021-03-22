package com.thatgamerblue.osrs.proxchat.common.net.messages.s2c;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from server to client requesting a state update
 */
@ToString
@NoArgsConstructor
public class S2CUpdateReq
{
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<S2CUpdateReq>
	{
		@Override
		public void write(Kryo kryo, Output output, S2CUpdateReq s2CUpdateReq)
		{

		}

		@Override
		public S2CUpdateReq read(Kryo kryo, Input input, Class<S2CUpdateReq> aClass)
		{
			return new S2CUpdateReq();
		}
	}
}
