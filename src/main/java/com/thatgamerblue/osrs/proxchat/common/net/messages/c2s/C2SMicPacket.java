package com.thatgamerblue.osrs.proxchat.common.net.messages.c2s;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from client to server containing opus-encoded microphone data
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class C2SMicPacket
{
	/**
	 * Opus encoded microphone data
	 */
	public byte[] data;
}
