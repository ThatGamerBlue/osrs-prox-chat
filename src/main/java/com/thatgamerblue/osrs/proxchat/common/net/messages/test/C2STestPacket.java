package com.thatgamerblue.osrs.proxchat.common.net.messages.test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Packet sent from test client to test server containing a string
 */
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class C2STestPacket
{
	/**
	 * Will be printed out on the other side
	 */
	public String s;

	/**
	 * Serializes a C2STestPacket packet to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<C2STestPacket>
	{
		@Override
		public void write(Kryo kryo, Output output, C2STestPacket packet)
		{
			output.writeString(packet.s);
		}

		@Override
		public C2STestPacket read(Kryo kryo, Input input, Class<C2STestPacket> aClass)
		{
			return new C2STestPacket(input.readString());
		}
	}
}
