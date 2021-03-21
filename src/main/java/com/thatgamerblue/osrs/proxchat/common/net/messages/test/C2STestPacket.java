package com.thatgamerblue.osrs.proxchat.common.net.messages.test;

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
}
