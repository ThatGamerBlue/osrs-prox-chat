
package com.esotericsoftware.kryonet;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.FrameworkMessage.DiscoverHost;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.FrameworkMessage.Ping;
import com.esotericsoftware.kryonet.FrameworkMessage.RegisterTCP;
import com.esotericsoftware.kryonet.FrameworkMessage.RegisterUDP;

public class KryoSerialization implements Serialization {
	private final Kryo kryo;
	private final Input input;
	private final Output output;
	private final ByteBufferInputStream byteBufferInputStream = new ByteBufferInputStream();
	private final ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream();

	public KryoSerialization () {
		this(new Kryo());
	}

	public KryoSerialization (Kryo kryo) {
		this.kryo = kryo;

		kryo.register(RegisterTCP.class, new RegisterTCP.Serializer());
		kryo.register(RegisterUDP.class, new RegisterUDP.Serializer());
		kryo.register(KeepAlive.class, new KeepAlive.Serializer());
		kryo.register(DiscoverHost.class, new DiscoverHost.Serializer());
		kryo.register(Ping.class, new Ping.Serializer());

		input = new Input(byteBufferInputStream, 512);
		output = new Output(byteBufferOutputStream, 512);
	}

	public Kryo getKryo () {
		return kryo;
	}

	public synchronized void write (Connection connection, ByteBuffer buffer, Object object) throws IOException
	{
		byteBufferOutputStream.setByteBuffer(buffer);
		kryo.writeClassAndObject(output, object);
		output.flush();
	}

	public synchronized Object read (Connection connection, ByteBuffer buffer) {
		byteBufferInputStream.setByteBuffer(buffer);
		input.setInputStream(byteBufferInputStream);
		return kryo.readClassAndObject(input);
	}

	public void writeLength (ByteBuffer buffer, int length) {
		buffer.putInt(length);
	}

	public int readLength (ByteBuffer buffer) {
		return buffer.getInt();
	}

	public int getLengthLength () {
		return 4;
	}
}
