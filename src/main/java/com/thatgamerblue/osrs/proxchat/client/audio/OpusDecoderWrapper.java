package com.thatgamerblue.osrs.proxchat.client.audio;

import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import org.concentus.OpusDecoder;
import org.concentus.OpusException;

/**
 * Decodes Opus encoded data into PCM data
 */
public class OpusDecoderWrapper
{
	/**
	 * Opus decoder instance
	 */
	private OpusDecoder decoder;

	// Don't allow initialization from outside
	private OpusDecoderWrapper()
	{
	}

	/**
	 * Creates an OpusDecoder with sample rate of {@link AudioConstants#SAMPLE_RATE}, 1 channel
	 *
	 * @return OpusDecoder with the settings mentioned earlier
	 * @throws java.lang.RuntimeException if creating the decoder fails
	 */
	public static OpusDecoderWrapper create()
	{
		OpusDecoderWrapper decoder = new OpusDecoderWrapper();
		try
		{
			decoder.decoder = new OpusDecoder(AudioConstants.SAMPLE_RATE, 1);
		}
		catch (OpusException e)
		{
			throw new RuntimeException(e);
		}

		return decoder;
	}

	/**
	 * Decodes the opus data in the data buffer into pcm data and puts it in the out buffer up to data.length bytes
	 *
	 * @param data opus data in
	 * @param pcm  pcm data out
	 * @return 0 on success, error code otherwise
	 */
	public int decode(byte[] data, short[] pcm)
	{
		try
		{
			return decoder.decode(data, 0, data.length, pcm, 0, AudioConstants.FRAME_SIZE / 2, false);
		}
		catch (OpusException e)
		{
			throw new RuntimeException(e);
		}
	}
}
