package com.esotericsoftware.kryo.factories;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

public class NullSerializerFactory implements SerializerFactory {
	@Override
	public Serializer makeSerializer (Kryo kryo, Class<?> type) {
		return makeSerializer();
	}

	public static Serializer makeSerializer() {
		return null;
	}
}
