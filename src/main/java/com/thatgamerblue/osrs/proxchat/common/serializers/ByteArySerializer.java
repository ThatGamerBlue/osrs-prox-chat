package com.thatgamerblue.osrs.proxchat.common.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.IOException;

/**
 * Kryo {@link com.esotericsoftware.kryo.Serializer} for byte arrays
 */
public class ByteArySerializer extends Serializer<byte[]>
{
	/**
	 * Serializes a byte array into the output
	 * <p>
	 * Writes:
	 * <ul>
	 * <li>int: length of byte array</li>
	 * <li>byte[]: contents of byte array</li>
	 * </ul>
	 *
	 * @param kryo   unused
	 * @param output output stream to write into
	 * @param bytes  byte array to write
	 */
	@Override
	public void write(Kryo kryo, Output output, byte[] bytes)
	{
		output.writeInt(bytes.length);
		output.write(bytes);
	}

	/**
	 * Deserializes a byte array from the input
	 * <p>
	 * Reads:
	 * <ul>
	 * <li>int: length of byte array</li>
	 * <li>byte[]: contents of byte array</li>
	 * </ul>
	 *
	 * @param kryo   unused
	 * @param input  input stream to read from
	 * @param aClass unused
	 * @return a byte array read from input
	 */
	@Override
	public byte[] read(Kryo kryo, Input input, Class<byte[]> aClass)
	{
		int len = input.readInt();
		byte[] ary = new byte[len];
		input.read(ary);
		return ary;
	}
}
