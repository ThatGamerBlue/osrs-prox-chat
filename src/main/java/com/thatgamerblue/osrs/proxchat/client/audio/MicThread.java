package com.thatgamerblue.osrs.proxchat.client.audio;

import com.thatgamerblue.osrs.proxchat.client.net.ClientNetworkHandler;
import com.thatgamerblue.osrs.proxchat.common.audio.AudioConstants;
import com.thatgamerblue.osrs.proxchat.common.net.messages.c2s.C2SMicPacket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;

/**
 * Reads microphone data and sends packets over the network
 */
@Slf4j
public class MicThread extends Thread
{
	/**
	 * Atomic used to tell when the thread should stop
	 */
	private final AtomicBoolean running = new AtomicBoolean(true);
	/**
	 * Atomic used to tell when the PTT key is pressed
	 */
	private final AtomicBoolean pttDown = new AtomicBoolean(false);
	/**
	 * Atomic used to tell when toggle mute is enabled
	 */
	private final AtomicBoolean toggleMute = new AtomicBoolean(false);
	/**
	 * Client network handler for sending mic data out
	 */
	private final ClientNetworkHandler networkHandler;
	/**
	 * How much to amplify microphone volume by
	 */
	private final Supplier<Integer> amplificationSupplier;
	/**
	 * Minimum audio level for mic to activate on voice activation mode.
	 */
	private final Supplier<Integer> thresholdSupplier;
	/**
	 * Push-to-talk or voice activity
	 */
	private final Supplier<AudioMode> audioModeSupplier;
	/**
	 * Game state supplier so we don't blast the server with nobody can hear
	 */
	private final Supplier<GameState> gameStateSupplier;
	/**
	 * Microphone input device
	 */
	private final TargetDataLine mic;
	/**
	 * Hold-on time before deactivating mic output
	 */
	private long micHoldOnTime = -1;

	/**
	 * Constructs a new thread to read microphone pcm data
	 *
	 * @param network               connection to the server
	 * @param amplificationSupplier supplier of mic volume amplification
	 * @param thresholdSupplier     supplier of mic activation threshold
	 * @param audioModeSupplier     supplier of audio mode
	 * @param gameStateSupplier     supplier of current game state
	 */
	public MicThread(
		ClientNetworkHandler network,
		Supplier<Integer> amplificationSupplier,
		Supplier<Integer> thresholdSupplier,
		Supplier<AudioMode> audioModeSupplier,
		Supplier<GameState> gameStateSupplier
	)
	{
		this.networkHandler = network;
		this.amplificationSupplier = amplificationSupplier;
		this.thresholdSupplier = thresholdSupplier;
		this.audioModeSupplier = audioModeSupplier;
		this.gameStateSupplier = gameStateSupplier;
		TargetDataLine __mic;
		try
		{
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, null);
			__mic = (TargetDataLine) AudioSystem.getLine(info);
			__mic.open(AudioConstants.MONO_FORMAT);
		}
		catch (LineUnavailableException e)
		{
			log.error("Failed to initialize microphone", e);
			__mic = null;
		}
		mic = __mic;

		setDaemon(true);
		setName("Prox-MicThread");
	}

	/**
	 * Blasts voice data across the network. Good luck everybody else!
	 */
	@Override
	public void run()
	{
		if (mic == null)
		{
			throw new RuntimeException("Microphone uninitialized.");
		}

		byte[] inBuf = new byte[AudioConstants.FRAME_SIZE];

		while (running.get())
		{
			if (toggleMute.get())
			{
				sleep(1);
				continue;
			}

			if (!networkHandler.isConnected())
			{
				sleep(1);
				continue;
			}

			GameState currentState = gameStateSupplier.get();
			if (currentState != GameState.LOADING && currentState != GameState.LOGGED_IN)
			{
				sleep(1);
				continue;
			}

			mic.start();
			int avail = mic.available();
			if (avail < AudioConstants.FRAME_SIZE)
			{
				sleep(1);
				continue;
			}

			Arrays.fill(inBuf, (byte) 0);
			mic.read(inBuf, 0, inBuf.length);
			AudioUtil.amplify(inBuf, amplificationSupplier.get() / 25.f);

			switch (audioModeSupplier.get())
			{
				case PUSH_TO_TALK:
					if (pttDown.get())
					{
						micHoldOnTime = System.currentTimeMillis() + 120;
						networkHandler.sendTCP(new C2SMicPacket(inBuf));
						continue;
					}
					break;
				case VOICE_ACTIVITY:
					double highestLvl = AudioUtil.calculateAudioLevel(inBuf);
					if (highestLvl > thresholdSupplier.get())
					{
						micHoldOnTime = System.currentTimeMillis() + 120;
						networkHandler.sendTCP(new C2SMicPacket(inBuf));
						continue;
					}
					break;
			}

			if (micHoldOnTime > System.currentTimeMillis())
			{
				// if the mic is being held on, send data anyway
				networkHandler.sendTCP(new C2SMicPacket(inBuf));
				continue;
			}

			// if we reached here we haven't transmitted anything
			// the sendTCP calls above will set the boolean to true when we send a mic packet
			networkHandler.getMicTransmitting().set(false);
		}

		log.info("MicThread stopping...");

		mic.stop();
		mic.close();
	}

	/**
	 * Stops this thread from running and cleans up
	 */
	public void end()
	{
		running.set(false);
	}

	/**
	 * Atomically sets {@link #pttDown}
	 *
	 * @param ptt whether or not the PTT key is pressed
	 */
	public void setPttDown(boolean ptt)
	{
		pttDown.set(ptt);
	}

	/**
	 * Atomically sets {@link #toggleMute} to the opposite of its current value
	 * <p>
	 * Source: https://stackoverflow.com/a/1255633
	 */
	public void toggleMute()
	{
		boolean v;
		do
		{
			v = toggleMute.get();
		}
		while (!toggleMute.compareAndSet(v, !v));
	}

	/**
	 * Is the mic currently deactivated
	 *
	 * @return if the mic is muted by the user
	 */
	public boolean isToggleMute()
	{
		return toggleMute.get();
	}

	/**
	 * Sleeps for {@code ms} milliseconds
	 *
	 * @param ms ms to sleep for
	 */
	private void sleep(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException ignored)
		{
		}
	}
}
