package com.thatgamerblue.osrs.proxchat.common.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.UUID;

/**
 * Kryo {@link com.esotericsoftware.kryo.Serializer} for UUIDs
 */
public class UUIDSerializer extends Serializer<UUID>
{
	/**
	 * Serializes a UUID into the output
	 * <p>
	 * Writes:
	 * <ul>
	 * <li>long: uuid MSB</li>
	 * <li>long: uuid LSB</li>
	 * </ul>
	 *
	 * @param kryo   unused
	 * @param output output stream to write into
	 * @param uuid   uuid to write
	 */
	@Override
	public void write(Kryo kryo, Output output, UUID uuid)
	{
		output.writeLong(uuid.getMostSignificantBits());
		output.writeLong(uuid.getLeastSignificantBits());
	}

	/**
	 * Deserializes a UUID from the input
	 * <p>
	 * Reads:
	 * <ul>
	 * <li>long: uuid MSB</li>
	 * <li>long: uuid LSB</li>
	 * </ul>
	 *
	 * @param kryo   unused
	 * @param input  input stream to read from
	 * @param aClass unused
	 */
	@Override
	public UUID read(Kryo kryo, Input input, Class<UUID> aClass)
	{
		return new UUID(input.readLong(), input.readLong());
	}
}
