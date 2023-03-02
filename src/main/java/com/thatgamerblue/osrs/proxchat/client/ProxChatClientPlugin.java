package com.thatgamerblue.osrs.proxchat.client;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.thatgamerblue.osrs.proxchat.client.audio.MicThread;
import com.thatgamerblue.osrs.proxchat.client.net.ClientNetworkHandler;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Entrypoint for the Proximity Chat client
 * <p>
 * This is a RuneLite plugin
 */
@Slf4j
@PluginDescriptor(
	name = "Proximity Chat"
)
public class ProxChatClientPlugin extends Plugin
{
	/**
	 * License string for icons
	 */
	private static final String ICON_LICENSE =
		"Icons from iconfinder.com (Benjamin STAWARZ)\r\n" +
			"All licensed under CC Abbrib. 3.0 Unported:\r\n" +
			"https://creativecommons.org/licenses/by/3.0/\r\n" +
			"Disconnected: iconfinder.com/icons/6137632\r\n" +
			"Output Muted: iconfinder.com/icons/6138050\r\n" +
			"Mic Muted: iconfinder.com/icons/6138089\r\n" +
			"Mic Active: iconfinder.com/icons/6138088\r\n" +
			"Modified by ThatGamerBlue, filled in icons";

	/**
	 * Set of config keys that require disconnecting and reconnecting to the server
	 */
	private static final Set<String> RECONNECT_CONFIGS = ImmutableSet.of("address", "port", "password", "enabled");

	/**
	 * Instance of the RuneScape game client
	 */
	@Inject
	private Client client;

	/**
	 * Holds the configuration for the client side
	 */
	@Getter
	@Inject
	private ProxChatClientConfig config;

	/**
	 * Used to connect to the server on so we don't block the UI or Client threads
	 */
	@Getter
	private ScheduledExecutorService executor;

	/**
	 * RuneLite's key manager. Used to detect if PTT or toggle mute is in effect
	 */
	@Inject
	private KeyManager keyManager;

	/**
	 * Listener for PTT and toggle mute keys
	 */
	@Inject
	private ProxKeyHandler keyHandler;

	/**
	 * RuneLite's overlay manager, for registering overlays to be drawn
	 */
	@Inject
	private OverlayManager overlayManager;

	/**
	 * Instance of our overlay for displaying state to the user
	 */
	@Inject
	private ProxChatClientOverlay overlay;

	/**
	 * The networking handler for the client side
	 */
	@Getter
	private ClientNetworkHandler network;

	/**
	 * The thread responsible for reading and sending out microphone data
	 */
	@Getter
	private MicThread micThread;

	/**
	 * Does some magic with guice to make the config work
	 *
	 * @param configManager RuneLite's {@link net.runelite.client.config.ConfigManager} instance
	 * @return the {@link com.thatgamerblue.osrs.proxchat.client.ProxChatClientConfig} instance
	 */
	@Provides
	ProxChatClientConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ProxChatClientConfig.class);
	}

	/**
	 * Main entrypoint for the plugin
	 */
	@Override
	protected void startUp()
	{
		Log.set(Log.LEVEL_INFO);
		executor = Executors.newSingleThreadScheduledExecutor();
		network = new ClientNetworkHandler(this, client, config::address, config::port, config::password, config::enabled, config::room);
		network.initKryonet();

		micThread = new MicThread(network, config::micVolume, config::activationThreshold, config::audioMode, client::getGameState);
		micThread.start();

		executor.submit(() -> network.connect());

		keyManager.registerKeyListener(keyHandler);

		overlayManager.add(overlay);
	}

	/**
	 * Called when the plugin shuts down
	 */
	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		
		keyManager.unregisterKeyListener(keyHandler);
		network.cancelConnecting();
		executor.submit(() -> network.disconnect());
		if (micThread != null)
		{
			micThread.end();
		}

		executor.submit(System::gc);
		executor.shutdown();
	}

	/**
	 * Fired whenever a config key changes. Used to detect when to disconnect from and reconnect to the server
	 * Also used to show icon license information
	 *
	 * @param event ConfigChanged event object fired by runelite
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"blueproxchat".equals(event.getGroup()))
		{
			return;
		}

		if (RECONNECT_CONFIGS.contains(event.getKey()))
		{
			executor.submit(() ->
			{
				if (!event.getKey().equals("enabled") && !network.isConnected() && !network.isConnecting())
				{
					return;
				}
				network.cancelConnecting();
				network.disconnect();
				network.connect();
			});
		}
		else if ("showLicenseInfo".equals(event.getKey()))
		{
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, ICON_LICENSE, "Blue's Prox Chat", JOptionPane.INFORMATION_MESSAGE));
		}
	}

	/**
	 * Event fired roughly every 20ms by the RuneScape client
	 *
	 * @param event ignored
	 */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		network.sendUpdate(client.getGameState().getState());
	}

	/**
	 * Event fired every time the {@link net.runelite.api.GameState} of the client changes
	 *
	 * @param event event object containing the new state
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
		{
			return;
		}

		network.sendUpdate(event.getGameState().getState());
	}
}
