package com.thatgamerblue.osrs.proxchat.common.net.messages.c2s;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from client to server containing player position and ID
 */
@ToString
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
}
