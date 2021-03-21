package com.thatgamerblue.osrs.proxchat.common.net.messages.s2c;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from server to client acknowledging its existence and providing a number to help prevent replay attacks
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class S2CAuthReq
{
	/**
	 * An int used to help prevent replay attacks
	 */
	public int replay;
}
