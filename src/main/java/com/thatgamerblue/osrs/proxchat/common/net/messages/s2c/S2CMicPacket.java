package com.thatgamerblue.osrs.proxchat.common.net.messages.s2c;

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
}
