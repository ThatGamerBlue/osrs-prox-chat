package com.thatgamerblue.osrs.proxchat.common.audio;

import java.time.temporal.Temporal;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import net.runelite.api.Perspective;

/**
 * Small class to hold constants related to audio encoding and decoding
 */
public class AudioConstants
{
	/**
	 * Minimum distance before applying volume scaling based off distance
	 */
	public static final int MIN_DISTANCE = 5 * Perspective.LOCAL_TILE_SIZE;

	/**
	 * Max distance for an audio packet before dropping
	 */
	public static final int MAX_DISTANCE = 15 * Perspective.LOCAL_TILE_SIZE;

	/**
	 * Audio sample rate used for encoding and decoding.
	 */
	public static final int SAMPLE_RATE = 24000;

	/**
	 * Milliseconds of audio per packet
	 */
	public static final int MS_PER_PACKET = 20;

	/**
	 * Size for each packet.
	 * <p>
	 * Packet size (in bytes) is equal to
	 * <ul>
	 * <li>(SAMPLE_RATE / 1000) * 2 * 20;</li>
	 * <li>SAMPLES_PER_MS * BYTES_PER_SHORT * MS_PER_PACKET;</li>
	 * </ul>
	 */
	public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * Short.BYTES * MS_PER_PACKET;

	/**
	 * AudioFormat for reading from the mic
	 */
	public static final AudioFormat MONO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

	/**
	 * AudioFormat for playing to the speaker
	 */
	public static final AudioFormat STEREO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
}
