package com.thatgamerblue.osrs.proxchat.client;

import com.thatgamerblue.osrs.proxchat.client.audio.AudioMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

/**
 * Cool config class fully handled by RuneLite
 */
@ConfigGroup("blueproxchat")
public interface ProxChatClientConfig extends Config
{
	/**
	 * Config for if the plugin should connect to the server
	 * We want this because even if the user turns on the plugin, we want them to look at the settings pane
	 * to change servers if they don't want to connect to the default public server
	 *
	 * @return is the plugin enabled?
	 */
	@ConfigItem(
		keyName = "enabled",
		name = "Enabled",
		description = "Enable the plugin",
		position = 0
	)
	default boolean enabled() {
		return false;
	}

	/**
	 * Config for the address of the voice server
	 *
	 * @return network address of the voice server
	 */
	@ConfigItem(
		keyName = "address",
		name = "Server Address",
		description = "The voice server to use. Default: proxchat.thatgamerblue.com",
		position = 1
	)
	default String address()
	{
		return "proxchat.thatgamerblue.com";
	}

	/**
	 * Config for the port of the voice server
	 *
	 * @return port of the server
	 */
	@Range(min = 1024, max = 65535)
	@ConfigItem(
		keyName = "port",
		name = "Server Port",
		description = "Port of the voice server",
		position = 2
	)
	default int port()
	{
		return 30666;
	}

	/**
	 * Config for the password of the voice server
	 *
	 * @return password of the server
	 */
	@ConfigItem(
		keyName = "password",
		name = "Server Password",
		description = "Password of the voice server",
		position = 3,
		secret = true
	)
	default String password()
	{
		return "";
	}

	/**
	 * Config for the room the player is in
	 *
	 * @return shared room name
	 */
	@ConfigItem(
		keyName = "room",
		name = "Room Name",
		description = "Shared room name for private calls - if this gets leaked anyone can join",
		position = 4
	)
	default String room() {
		return "";
	}

	/**
	 * Config for the audio mode to use, push to talk or voice activity
	 *
	 * @return push-to-talk or voice activity depending on selection
	 */
	@ConfigItem(
		keyName = "audioMode",
		name = "Mic Mode",
		description = "Voice detection type to use.",
		position = 5
	)
	default AudioMode audioMode()
	{
		return AudioMode.VOICE_ACTIVITY;
	}

	/**
	 * Config for the threshold of voice activation
	 *
	 * @return -127 to 0, depending on selection
	 */
	@Range(min = -127, max = 0)
	@ConfigItem(
		keyName = "activationThreshold",
		name = "Activation Threshold",
		description = "Threshold for voice activation in dB. Range: -127 - 0",
		position = 6
	)
	default int activationThreshold()
	{
		return -50;
	}

	/**
	 * Config for the push to talk key
	 *
	 * @return user-set keybind for push to talk
	 */
	@ConfigItem(
		keyName = "pushToTalk",
		name = "PTT Key",
		description = "Push To Talk Key",
		position = 7
	)
	default Keybind pushToTalk()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Config for the mute mic key
	 *
	 * @return user-set keybind for mute mic
	 */
	@ConfigItem(
		keyName = "toggleMute",
		name = "Mute Mic",
		description = "Push to mute mic",
		position = 8
	)
	default Keybind toggleMute()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Config for mic amplification
	 *
	 * @return value to amplify microphone audio by
	 */
	@Range(max = 100)
	@ConfigItem(
		keyName = "micVolume",
		name = "Mic Volume",
		description = "Range 0 - 100",
		position = 9
	)
	default int micVolume()
	{
		return 25;
	}

	/**
	 * Config for the mute speakers key
	 *
	 * @return user-set keybind for mute speaker
	 */
	@ConfigItem(
		keyName = "toggleMuteSpeaker",
		name = "Mute Speaker",
		description = "Push to mute speakers",
		position = 10
	)
	default Keybind toggleMuteSpeaker()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Config for speaker amplification
	 *
	 * @return value to amplify speaker audio by
	 */
	@Range(max = 100)
	@ConfigItem(
		keyName = "speakerVolume",
		name = "Speaker Volume",
		description = "Range 0 - 100",
		position = 11
	)
	default int speakerVolume()
	{
		return 50;
	}

	/**
	 * Toggle to display license information for icons used
	 *
	 * @return unused, boolean to make it a checkbox in the config panel
	 */
	@ConfigItem(
		keyName = "showLicenseInfo",
		name = "Display Icon License",
		description = "Acts as a button. Click to show icon license info",
		position = 99
	)
	default boolean showLicenseInfo()
	{
		return false;
	}
}
