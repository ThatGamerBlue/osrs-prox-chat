package com.thatgamerblue.osrs.proxchat.client;

import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

/**
 * Overlay for displaying current microphone, speaker and connection states to the user
 */
public class ProxChatClientOverlay extends Overlay
{
	/**
	 * Plugin instance for getting the states
	 */
	private final ProxChatClientPlugin plugin;
	/**
	 * Image to display if the network is disconnected
	 */
	private final BufferedImage disconnectedImg;
	/**
	 * Image to display if the microphone is muted
	 */
	private final BufferedImage micMutedImg;
	/**
	 * Image to display if the mic is transmitting
	 */
	private final BufferedImage micActiveImg;
	/**
	 * Image to display if the output is muted
	 */
	private final BufferedImage outputMutedImg;

	/**
	 * Constructs a new instance of this overlay. Uses guice for that fancy dependency injection
	 *
	 * @param plugin plugin instance provided by guice
	 */
	@Inject
	public ProxChatClientOverlay(ProxChatClientPlugin plugin)
	{
		this.plugin = plugin;

		disconnectedImg = ImageUtil.loadImageResource(ProxChatClientPlugin.class, "disconnected.png");
		micMutedImg = ImageUtil.loadImageResource(ProxChatClientPlugin.class, "mic-muted.png");
		micActiveImg = ImageUtil.loadImageResource(ProxChatClientPlugin.class, "mic-active.png");
		outputMutedImg = ImageUtil.loadImageResource(ProxChatClientPlugin.class, "output-muted.png");

		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	/**
	 * Renders this overlay to the provided graphics instance
	 *
	 * @param graphics graphics instance to draw to
	 * @return dimensions of the drawn overlay, or 32x32 in the case of no drawing
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		BufferedImage image1 = null;
		BufferedImage image2 = null;
		if (!plugin.getNetwork().isConnected())
		{
			image1 = disconnectedImg;
		}
		else if (plugin.getMicThread().isToggleMute())
		{
			image1 = micMutedImg;
		}
		else if (plugin.getNetwork().getMicTransmitting().get())
		{
			image1 = micActiveImg;
		}

		if (plugin.getNetwork().getSpeakerMuted().get())
		{
			if (image1 == null)
			{
				image1 = outputMutedImg;
			}
			else
			{
				image2 = outputMutedImg;
			}
		}

		if (image1 == null)
		{
			// return a 32x32 square so the user can still move the overlay around
			return new Dimension(32, 32);
		}

		graphics.drawImage(image1, 0, 0, null);
		if (image2 != null)
		{
			graphics.drawImage(image2, 0, 34, null);
		}

		return new Dimension(32, image2 == null ? 32 : 66);
	}
}
