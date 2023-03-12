package com.thatgamerblue.osrs.proxchat.client.test;

import com.thatgamerblue.osrs.proxchat.client.ProxChatClientPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Runs the plugin.
 */
public class RunPlugin
{
	/**
	 * Main entrypoint for RuneLite w/ the plugin enabled
	 *
	 * @param args any command line arguments
	 * @throws Exception if runelite throws an exception
	 */
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ProxChatClientPlugin.class);
		RuneLite.main(ArrayUtils.add(args, "--developer-mode"));
	}
}
