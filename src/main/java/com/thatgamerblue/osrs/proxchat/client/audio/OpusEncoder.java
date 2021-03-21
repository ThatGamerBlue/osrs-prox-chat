package com.thatgamerblue.osrs.proxchat.client.audio;

import com.sun.jna.ptr.PointerByReference;
import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import tomp2p.opuswrapper.Opus;

/**
 * Encodes PCM data into Opus encoded data
 */
public class OpusEncoder
{
	/**
	 * Instance of the Opus native library
	 */
	private static final Opus opus = Opus.INSTANCE;
	/**
	 * Native ptr to the Opus encoder
	 */
	private PointerByReference ptr;

	// Don't allow initialization from outside
	private OpusEncoder()
	{
	}

	/**
	 * Creates an OpusEncoder with sample rate of {@link AudioConstants#SAMPLE_RATE}, 1 channel, opus_application_voip
	 *
	 * @return OpusEncoder with the settings mentioned earlier
	 * @throws java.lang.RuntimeException if creating the encoder fails
	 */
	public static OpusEncoder create()
	{
		OpusEncoder encoder = new OpusEncoder();
		IntBuffer errorBuf = IntBuffer.allocate(1);
		encoder.ptr = opus.opus_encoder_create(AudioConstants.SAMPLE_RATE, 1, Opus.OPUS_APPLICATION_VOIP, errorBuf);
		int err = errorBuf.get(0);
		if (err != Opus.OPUS_OK)
		{
			throw new RuntimeException("Error creating Opus encoder: " + err);
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
	public int encode(ShortBuffer pcm, ByteBuffer out)
	{
		if (ptr != null)
		{
			return opus.opus_encode(ptr, pcm, AudioConstants.FRAME_SIZE / 2, out, out.capacity());
		}
		throw new RuntimeException("Encoder PTR null");
	}

	/**
	 * Destroys this Opus encoder instance, freeing all memory used by this native instance
	 */
	public void destroy()
	{
		if (ptr != null)
		{
			opus.opus_encoder_destroy(ptr);
		}
		ptr = null;
	}
}
