package com.thatgamerblue.osrs.proxchat.client;

import club.minnced.opus.util.OpusLibrary;
import com.esotericsoftware.minlog.Log;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.thatgamerblue.osrs.proxchat.client.audio.MicThread;
import com.thatgamerblue.osrs.proxchat.client.net.ClientNetworkHandler;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Entrypoint for the Proximity Chat client
 * <p>
 * This is a RuneLite plugin
 */
@Slf4j
@PluginDescriptor(
	name = "<html><!--making pretty labels since 2012--><p style=\"color: #00bfff\">Proximity Chat</p></html>"//,
	//name = "Proximity Chat"
)
public class ProxChatClientPlugin extends Plugin
{
	/**
	 * Set of config keys that require disconnecting and reconnecting to the server
	 */
	private static final Set<String> RECONNECT_CONFIGS = ImmutableSet.of("address", "port", "password");

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
		network = new ClientNetworkHandler(this, client, config::address, config::port, config::password);
		network.initKryonet();

		try
		{
			OpusLibrary.loadFromJar();
		}
		catch (IOException e)
		{
			log.error("Failed to load Opus library.", e);
		}

		micThread = new MicThread(network, config::micVolume, config::activationThreshold, config::audioMode, client::getGameState);
		micThread.start();

		executor.submit(() -> network.connect());

		keyManager.registerKeyListener(keyHandler);
	}

	/**
	 * Called when the plugin shuts down
	 */
	@Override
	protected void shutDown()
	{
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

		if (!RECONNECT_CONFIGS.contains(event.getKey()))
		{
			return;
		}

		executor.submit(() ->
		{
			network.cancelConnecting();
			network.disconnect();
			network.connect();
		});
	}

	/**
	 * Event fired roughly every 600ms by the RuneScape client
	 *
	 * @param event ignored
	 */
	@Subscribe
	public void onGameTick(GameTick event)
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
		network.sendUpdate(event.getGameState().getState());
	}
}
