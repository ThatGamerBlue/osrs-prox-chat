package com.thatgamerblue.osrs.proxchat.client.audio;

import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import static org.concentus.OpusApplication.OPUS_APPLICATION_VOIP;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;

/**
 * Encodes PCM data into Opus encoded data
 */
public class OpusEncoderWrapper
{
	/**
	 * Opus encoder instance
	 */
	private OpusEncoder encoder;

	// Don't allow initialization from outside
	private OpusEncoderWrapper()
	{
	}

	/**
	 * Creates an OpusEncoder with sample rate of {@link AudioConstants#SAMPLE_RATE}, 1 channel, opus_application_voip
	 *
	 * @return OpusEncoder with the settings mentioned earlier
	 * @throws java.lang.RuntimeException if creating the encoder fails
	 */
	public static OpusEncoderWrapper create()
	{
		OpusEncoderWrapper encoder = new OpusEncoderWrapper();
		try
		{
			encoder.encoder = new OpusEncoder(AudioConstants.SAMPLE_RATE, 1, OPUS_APPLICATION_VOIP);
		}
		catch (OpusException e)
		{
			throw new RuntimeException(e);
		}

		return encoder;
	}

	/**
	 * Encodes the data in the pcm buffer into opus data and puts it in the out buffer up to maxBytes length
	 *
	 * @param pcm pcm data in
	 * @param out opus data out
	 * @return 0 on success, error code otherwise
	 */
	public int encode(short[] pcm, byte[] out)
	{
		try
		{
			return encoder.encode(pcm, 0, AudioConstants.FRAME_SIZE / 2, out, 0, out.length);
		}
		catch (OpusException e)
		{
			throw new RuntimeException(e);
		}
	}
}
