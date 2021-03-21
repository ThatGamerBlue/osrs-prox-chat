package com.thatgamerblue.osrs.proxchat.client;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ProxChatPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ProxChatClientPlugin.class);
		RuneLite.main(new String[]{"--developer-mode"});
	}
}