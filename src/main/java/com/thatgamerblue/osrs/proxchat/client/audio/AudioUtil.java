package com.thatgamerblue.osrs.proxchat.client.audio;

/**
 * Various utilities for audio processing
 */
public class AudioUtil
{
	/**
	 * Converts two bytes into a short
	 *
	 * @param b1 low byte
	 * @param b2 high byte
	 * @return {@code ((b2 & 0xff) << 8 | b1 & 0xff)}
	 */
	public static short bytesToShort(byte b1, byte b2)
	{
		return (short) ((b2 & 0xff) << 8 | b1 & 0xff);
	}

	/**
	 * Converts a short into two bytes
	 *
	 * @param s short value
	 * @return short in a byte array in little endian format
	 */
	public static byte[] shortToBytes(short s)
	{
		return new byte[]{(byte) (s & 0xff), (byte) ((s >> 8) & 0xff)};
	}

	/**
	 * Amplifies the volume of 16-bit audio.
	 * Modifies the input array
	 *
	 * @param buf   input 16-bit pcm
	 * @param scale multiplication factor
	 */
	public static void amplify(byte[] buf, float scale)
	{
		for (int i = 0; i < buf.length; i += 2)
		{
			// we want to do the multiplication in the float domain so we dont lose precision in `scale`
			float fSample = bytesToShort(buf[i], buf[i + 1]);
			fSample *= scale;
			short sSample = (short) fSample;
			buf[i] = (byte) sSample;
			buf[i + 1] = (byte) (sSample >> 8);
		}
	}

	/**
	 * Converts mono pcm data to stereo pcm data, with equal volume on each channel
	 *
	 * @param mono mono pcm audio data
	 * @return stereo pcm audio data
	 */
	public static byte[] convertToStereo(byte[] mono)
	{
		byte[] stereo = new byte[mono.length * 2];
		for (int i = 0; i < mono.length; i += 2)
		{
			// left channel
			stereo[i * 2] = mono[i];
			stereo[i * 2 + 1] = mono[i + 1];

			// right channel
			stereo[i * 2 + 2] = mono[i];
			stereo[i * 2 + 3] = mono[i + 1];
		}

		return stereo;
	}

	/**
	 * Calculates the audio level of a signal with specific samples.
	 *
	 * @param samples the samples of the signal to calculate the audio level of
	 * @return the audio level of the specified signal in db
	 */
	public static double calculateAudioLevel(byte[] samples)
	{
		double rms = 0D; // root mean square (RMS) amplitude

		for (int i = 0; i < samples.length; i += 2)
		{
			double sample = (double) bytesToShort(samples[i], samples[i + 1]) / Short.MAX_VALUE;
			rms += sample * sample;
		}

		int sampleCount = samples.length / 2;

		rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

		double db;

		if (rms > 0D)
		{
			db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
		}
		else
		{
			db = -127D;
		}

		return db;
	}
}
