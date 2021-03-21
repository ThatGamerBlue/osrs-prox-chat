package com.thatgamerblue.osrs.proxchat.server.net;

import java.util.UUID;
import lombok.Value;

/**
 * Data class to hold the state of a client
 */
@Value
public class ClientState
{
	/**
	 * Server-assigned UUID
	 */
	UUID uuid;

	/**
	 * Player world x position
	 */
	int x;

	/**
	 * Player world y position
	 */
	int y;

	/**
	 * Player world plane
	 */
	int plane;

	/**
	 * Player world
	 */
	int world;

	/**
	 * Player game state
	 */
	int gameState;

	/**
	 * Gets the distance to another player. Basically just {@link net.runelite.api.coords.WorldPoint#distanceTo(net.runelite.api.coords.WorldPoint)} with a fancy wrapper
	 *
	 * @param other state of the other client
	 * @return distance to that client
	 */
	public int distanceTo(ClientState other)
	{
		if (other == null)
		{
			return Integer.MAX_VALUE;
		}

		if (this.gameState < 25 || this.gameState > 30)
		{
			return Integer.MAX_VALUE;
		}

		if (other.gameState < 25 || other.gameState > 30)
		{
			return Integer.MAX_VALUE;
		}

		if (this.world != other.world)
		{
			return Integer.MAX_VALUE;
		}

		if (this.x == -1 || other.x == -1)
		{
			return Integer.MAX_VALUE;
		}

		return Math.max(Math.abs(getX() - other.getX()), Math.abs(getY() - other.getY()));
	}
}
