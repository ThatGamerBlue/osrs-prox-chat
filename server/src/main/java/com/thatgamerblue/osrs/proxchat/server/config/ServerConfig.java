package com.thatgamerblue.osrs.proxchat.server.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;

/**
 * Data class for holding the server's configuration
 */
@Getter
public class ServerConfig
{
	/**
	 * Holds the original comment so we can put it back if the user changes it
	 */
	public static final List<String> STATIC_COMMENT = ImmutableList.of(
		"You must change the password before use!",
		"An empty password will allow any user to connect with any password.",
		"Any modifications to this comment field will be overwritten."
	);

	/**
	 * Let people know some rules about the config
	 */
	@SerializedName("__comment__readme")
	public List<String> comment = STATIC_COMMENT;

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
