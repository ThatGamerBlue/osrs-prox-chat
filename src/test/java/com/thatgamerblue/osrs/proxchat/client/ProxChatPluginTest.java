package com.thatgamerblue.osrs.proxchat.client;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Test class to launch the runelite client with the plugin
 * Prefer using the runPlugin gradle task (you may have to uncomment line 3 and lines 6-84 in build.gradle)
 */
public class ProxChatPluginTest
{
	/**
	 * Main entrypoint for the plugin test
	 *
	 * @param args command line arguments
	 * @throws Exception if any unhandled exceptions get thrown
	 */
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ProxChatClientPlugin.class);
		RuneLite.main(new String[]{"--developer-mode"});
	}
}