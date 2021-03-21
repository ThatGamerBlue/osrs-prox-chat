package com.thatgamerblue.osrs.proxchat.common.net.messages.c2s;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from client to server to authenticate. Uses the number in the S2CHello to help prevent replay attacks
 * <p>
 * I know this isn't secure by any stretch of the imagination, but I don't care enough to fix it.
 * Use a strong random password and just don't get sniffed 4Head
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class C2SAuth
{
	/**
	 * The password to authenticate with the server
	 */
	public byte[] password;
}
