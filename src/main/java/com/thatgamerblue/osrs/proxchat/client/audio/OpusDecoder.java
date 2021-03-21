package com.thatgamerblue.osrs.proxchat.client.audio;

import com.sun.jna.ptr.PointerByReference;
import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import tomp2p.opuswrapper.Opus;

/**
 * Decodes Opus encoded data into PCM data
 */
public class OpusDecoder
{
	/**
	 * Instance of the Opus native library
	 */
	private static final Opus opus = Opus.INSTANCE;
	/**
	 * Native ptr to the Opus decoder
	 */
	private PointerByReference ptr;

	// Don't allow initialization from outside
	private OpusDecoder()
	{
	}

	/**
	 * Creates an OpusDecoder with sample rate of {@link AudioConstants#SAMPLE_RATE}, 1 channel
	 *
	 * @return OpusDecoder with the settings mentioned earlier
	 * @throws java.lang.RuntimeException if creating the decoder fails
	 */
	public static OpusDecoder create()
	{
		OpusDecoder decoder = new OpusDecoder();
		IntBuffer errorBuf = IntBuffer.allocate(1);
		decoder.ptr = opus.opus_decoder_create(AudioConstants.SAMPLE_RATE, 1, errorBuf);
		int err = errorBuf.get(0);
		if (err != Opus.OPUS_OK)
		{
			throw new RuntimeException("Error creating Opus decoder: " + err);
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
	public int decode(byte[] data, ShortBuffer pcm)
	{
		if (ptr != null)
		{
			return opus.opus_decode(ptr, data, data.length, pcm, AudioConstants.FRAME_SIZE / 2, 0);
		}
		return -1;
	}

	/**
	 * Destroys this Opus decoder instance, freeing all memory used by this native instance
	 */
	public void destroy()
	{
		if (ptr != null)
		{
			opus.opus_decoder_destroy(ptr);
		}
		ptr = null;
	}
}
