package com.thatgamerblue.osrs.proxchat.client.audio;

import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import com.thatgamerblue.osrs.proxchat.common.net.messages.s2c.S2CMicPacket;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import lombok.extern.slf4j.Slf4j;

/**
 * Plays speaker data fed to it via the queue
 */
@Slf4j
public class SpeakerThread extends Thread
{
	/**
	 * Holds audio data in FIFO queue, initial capacity is one second of audio
	 */
	private final ArrayBlockingQueue<S2CMicPacket> soundQueue = new ArrayBlockingQueue<>(1000 / AudioConstants.MS_PER_PACKET);

	/**
	 * Microphone input device
	 */
	private final SourceDataLine speaker;

	/**
	 * Atomic used to tell when the thread should stop
	 */
	private final AtomicBoolean running = new AtomicBoolean(true);

	/**
	 * Output volume scale
	 */
	private final Supplier<Integer> volume;

	/**
	 * Constructs a thread to play mic data
	 *
	 * @param uuid   server-assigned uuid of this thread
	 * @param volume volume of the speaker
	 */
	public SpeakerThread(
		UUID uuid,
		Supplier<Integer> volume
	)
	{
		this.volume = volume;

		SourceDataLine __speaker;
		try
		{
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, null);
			__speaker = (SourceDataLine) AudioSystem.getLine(info);
		}
		catch (LineUnavailableException e)
		{
			log.error("Failed to initialize speaker", e);
			__speaker = null;
		}
		speaker = __speaker;

		setDaemon(true);
		setName("Prox-SpeakerThread-" + uuid.toString());
	}

	/**
	 * Tries to play mic data as fast as possible. Probably will lag behind lmao
	 */
	@Override
	public void run()
	{
		try
		{
			speaker.open(AudioConstants.STEREO_FORMAT);
		}
		catch (LineUnavailableException e)
		{
			log.error("Failed to initialize speaker", e);
			speaker.close();
			return;
		}

		FloatControl gainControl = (FloatControl) speaker.getControl(FloatControl.Type.MASTER_GAIN);

		while (running.get())
		{
			// prevent the last sample from being repeated infinitely
			if (speaker.getBufferSize() - speaker.available() <= 0 && speaker.isActive())
			{
				speaker.stop();
			}

			S2CMicPacket packet;

			try
			{
				packet = soundQueue.poll(5, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				continue;
			}

			if (packet == null)
			{
				continue;
			}

			// here we'd check gamestate if required, the server shouldn't send us packets if we're logged out of the game anyway

			if (speaker.getBufferSize() - speaker.available() <= 0)
			{
				// nothing in the buffer, buffer some silence
				byte[] data = new byte[Math.min(AudioConstants.FRAME_SIZE * 4, speaker.getBufferSize() - AudioConstants.FRAME_SIZE)];
				speaker.write(data, 0, data.length);
			}

			float volumeScale = (float) scaleAudio(packet.distance);
			float volumeDb = (float) (10d * Math.log(volumeScale)) * ((float) volume.get() / 50.0f);
			gainControl.setValue(Math.min(Math.max(volumeDb, gainControl.getMinimum()), gainControl.getMaximum()));

			byte[] decoded = packet.data;

			byte[] stereo = AudioUtil.convertToStereo(decoded);

			speaker.write(stereo, 0, stereo.length);
			speaker.start();
		}

		speaker.stop();
		speaker.close();
		soundQueue.clear();
	}

	/**
	 * Scales audio levels based off distance
	 * Algorithm from https://stackoverflow.com/a/929107 because I'm too stupid to write it myself
	 *
	 * @param distance distance away from player in tiles * 128
	 * @return audio multiplier
	 */
	private float scaleAudio(int distance)
	{
		float val = 1.0f - ((((distance - (5f * 128f)) * 0.99f) / 1280f) + 0.01f);
		return Math.max(0.0f, Math.min(val, 1.0f));
	}

	/**
	 * Stops this thread from running, leaves cleanup to the {@link SpeakerThread#run()} method
	 */
	public void destroy()
	{
		running.set(false);
	}

	/**
	 * Adds a mic packet to the buffer, to be played as soon as possible
	 *
	 * @param micPacket microphone packet to buffer
	 */
	public void push(S2CMicPacket micPacket)
	{
		try
		{
			soundQueue.offer(micPacket, 25, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ignored)
		{
		}
	}
}
