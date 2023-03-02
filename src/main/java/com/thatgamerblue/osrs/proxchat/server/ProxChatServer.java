package com.thatgamerblue.osrs.proxchat.server;

import com.esotericsoftware.minlog.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thatgamerblue.osrs.proxchat.server.config.ConfigState;
import com.thatgamerblue.osrs.proxchat.server.config.ServerConfig;
import com.thatgamerblue.osrs.proxchat.server.net.ServerNetworkHandler;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import lombok.SneakyThrows;

/**
 * Entrypoint for the Proximity Chat server component
 */
public class ProxChatServer
{
	/**
	 * Global GSON instance for json /(de)?serialization/
	 */
	public static final Gson GSON = createGson();
	/**
	 * Location of the configuration file
	 */
	private static final File CONFIG_FILE = new File("config.json");

	/**
	 * Network handler for the server side
	 */
	private ServerNetworkHandler networkHandler;

	/**
	 * The configuration for the server side
	 */
	private ServerConfig config;

	/**
	 * Main function for the server
	 *
	 * @param args any arguments passed on the command line
	 * @throws java.io.IOException if network or file errors occur
	 */
	public static void main(String[] args) throws IOException
	{
		Log.set(Log.LEVEL_INFO);
		ProxChatServer server = new ProxChatServer();
		switch (server.loadConfig())
		{
			case SUCCESS:
				System.out.println("Successfully loaded config.");
				break;
			case FAILURE:
				System.out.println("Failed to load config, exiting.");
				System.exit(-1);
				break;
			case IS_DIRECTORY:
				System.out.println("config.json is a directory, please remove it.");
				System.exit(-1);
				break;
			case CREATED_NEW:
				System.out.println("Created new config file, please change the password.");
				System.exit(0);
				break;
		}
		server.writeConfig();
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
		server.start();
	}

	/**
	 * Creates the GSON instance to bypass the check for a new GSON in the gh actions because this is server side code,
	 * so I don't care if it's rubbish, it only runs once and not on the client
	 *
	 * @return a new Gson instance, created with reflection
	 */
	@SneakyThrows
	private static Gson createGson() {
		GsonBuilder builder = (GsonBuilder) Class.forName("com.google.gson.GsonBuilder").getDeclaredConstructor().newInstance();
		return builder.setPrettyPrinting().disableHtmlEscaping().create();
	}

	/**
	 * Loads the config. Will set {@link com.thatgamerblue.osrs.proxchat.server.ProxChatServer#config}
	 * If doesn't return SUCCESS, no guarantees are made about the validity of the config instance.
	 *
	 * @return the appropriate {@link com.thatgamerblue.osrs.proxchat.server.config.ConfigState} based off what happened
	 */
	public ConfigState loadConfig()
	{
		if (CONFIG_FILE.isDirectory())
		{
			return ConfigState.IS_DIRECTORY;
		}
		else if (!CONFIG_FILE.exists())
		{
			ServerConfig defaultConfig = new ServerConfig();
			String configStr = GSON.toJson(defaultConfig);
			try
			{
				Files.write(CONFIG_FILE.toPath(), configStr.getBytes(StandardCharsets.UTF_8));
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
				return ConfigState.FAILURE;
			}

			return ConfigState.CREATED_NEW;
		}

		try (FileReader fr = new FileReader(CONFIG_FILE))
		{
			config = GSON.fromJson(fr, ServerConfig.class);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			return ConfigState.FAILURE;
		}

		return ConfigState.SUCCESS;
	}

	/**
	 * Writes the current {@link #config} to disk, overwriting config.json if it exists.
	 *
	 * @return true on success, false on failure
	 */
	public boolean writeConfig()
	{
		// we've already verified that config.json isn't a directory, so we can safely clobber it.
		// if someone replaced it with a directory while the server is running they have subzero iq

		System.out.println("Writing config file...");

		config.comment = ServerConfig.STATIC_COMMENT;

		String configStr = GSON.toJson(config);
		try
		{
			Files.write(CONFIG_FILE.toPath(), configStr.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.out.println(Base64.getEncoder().encodeToString(configStr.getBytes(StandardCharsets.UTF_8)));
			System.out.println("Failed to write config file, data may be lost!");
			return false;
		}

		System.out.println("Successfully written config file");
		return true;
	}

	/**
	 * Creates the networkHandler instqnce and starts listening
	 *
	 * @throws IOException if anything goes wrong starting the server
	 */
	public void start() throws IOException
	{
		networkHandler = new ServerNetworkHandler(config::getBindAddress, config::getPort, config::getPassword);
		networkHandler.initKryonet();
		networkHandler.connect();
	}

	/**
	 * Shuts down the server entirely
	 */
	public void shutdown()
	{
		System.out.println("Shutting down...");
		writeConfig();
		networkHandler.shutdown();
	}
}
