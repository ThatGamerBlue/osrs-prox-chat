package com.thatgamerblue.osrs.proxchat.common.net.messages.test;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Packet sent from test server to test client containing a string
 */
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class S2CTestPacket
{
	/**
	 * Will be printed out on the other side
	 */
	public String s;
}
