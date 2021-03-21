package com.thatgamerblue.osrs.proxchat.server.config;

import lombok.Getter;

/**
 * Data class for holding the server's configuration
 */
@Getter
public class ServerConfig
{
	/**
	 * Password that clients have to supply to connect successfully
	 */
	public String password = "CHANGEME!!!";

	/**
	 * Address for the server to bind to
	 */
	public String bindAddress = "0.0.0.0";

	/**
	 * Port for the server to bind to
	 */
	public int port = 30666;
}
