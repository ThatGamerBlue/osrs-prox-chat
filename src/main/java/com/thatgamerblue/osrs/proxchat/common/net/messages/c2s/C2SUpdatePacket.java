package com.thatgamerblue.osrs.proxchat.common.net.messages.c2s;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from client to server containing player position and game state
 */
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class C2SUpdatePacket
{
	/**
	 * Player x position
	 */
	public int x;
	/**
	 * Player y position
	 */
	public int y;
	/**
	 * Player plane
	 */
	public int plane;
	/**
	 * Player world
	 */
	public int world;
	/**
	 * Client game state
	 */
	public int gameState;
	/**
	 * Shared room name
	 */
	public String room;

	/**
	 * Serializes a C2SUpdatePacket to binary
	 */
	public static class Serializer extends com.esotericsoftware.kryo.Serializer<C2SUpdatePacket>
	{
		/**
		 * Serialize to binary
		 *
		 * @param kryo   unused
		 * @param output output stream
		 * @param packet packet to write
		 */
		@Override
		public void write(Kryo kryo, Output output, C2SUpdatePacket packet)
		{
			output.writeInt(packet.x);
			output.writeInt(packet.y);
			output.writeInt(packet.plane);
			output.writeInt(packet.world);
			output.writeInt(packet.gameState);
			output.writeString(packet.room);
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
		public C2SUpdatePacket read(Kryo kryo, Input input, Class<C2SUpdatePacket> aClass)
		{
			return new C2SUpdatePacket(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readString());
		}
	}
}
