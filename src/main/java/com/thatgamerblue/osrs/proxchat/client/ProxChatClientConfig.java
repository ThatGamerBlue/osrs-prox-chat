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
	 * Config for the address of the voice server
	 *
	 * @return network address of the voice server
	 */
	@ConfigItem(
		keyName = "address",
		name = "Server Address",
		description = "The voice server to use",
		position = 0
	)
	default String address()
	{
		return "<disabled>";
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
		position = 1
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
		position = 2,
		secret = true
	)
	default String password()
	{
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
		position = 3
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
		position = 4
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
		position = 5
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
		position = 6
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
		position = 7
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
		position = 8
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
		position = 9
	)
	default int speakerVolume()
	{
		return 50;
	}
}
