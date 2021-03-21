package com.thatgamerblue.osrs.proxchat.client.audio;

import lombok.RequiredArgsConstructor;

/**
 * Audio detection modes for microphone input
 */
@RequiredArgsConstructor
public enum AudioMode
{
	/**
	 * Activates microphone when sound is detected
	 */
	VOICE_ACTIVITY("Voice Activity"),
	/**
	 * Activates microphone when a button is pushed
	 */
	PUSH_TO_TALK("PTT");

	/**
	 * Human-friendly name of this {@link AudioMode}
	 */
	private final String name;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
	{
		return name;
	}
}
