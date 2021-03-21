package com.thatgamerblue.osrs.proxchat.client;

import java.awt.event.KeyEvent;
import javax.inject.Inject;
import net.runelite.client.input.KeyListener;

/**
 * Handles keypresses for push-to-talk and toggle mute
 */
public class ProxKeyHandler implements KeyListener
{
	/**
	 * Plugin instance to get the keybinds and access the mic thread
	 */
	@Inject
	private ProxChatClientPlugin plugin;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void keyTyped(KeyEvent e)
	{
		if (plugin.getConfig().pushToTalk().matches(e))
		{
			plugin.getMicThread().setPttDown(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void keyPressed(KeyEvent e)
	{
		if (plugin.getConfig().pushToTalk().matches(e))
		{
			plugin.getMicThread().setPttDown(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void keyReleased(KeyEvent e)
	{
		if (plugin.getConfig().pushToTalk().matches(e))
		{
			plugin.getMicThread().setPttDown(false);
		}

		if (plugin.getConfig().toggleMute().matches(e))
		{
			plugin.getMicThread().toggleMute();
		}

		if (plugin.getConfig().toggleMuteSpeaker().matches(e))
		{
			plugin.getNetwork().toggleSpeakerMute();
		}
	}
}
