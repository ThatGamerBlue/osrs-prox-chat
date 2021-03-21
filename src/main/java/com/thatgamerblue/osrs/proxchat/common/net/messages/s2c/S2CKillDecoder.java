package com.thatgamerblue.osrs.proxchat.common.net.messages.s2c;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A packet sent from server to client signalling the end of a decoder
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class S2CKillDecoder
{
	/**
	 * The UUID of the decoder to kill
	 */
	public UUID uuid;
}
