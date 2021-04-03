
package com.esotericsoftware.kryo;

import static com.esotericsoftware.kryo.util.Util.*;
import static com.esotericsoftware.minlog.Log.*;

import com.esotericsoftware.kryo.factories.NullSerializerFactory;
import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.BooleanSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ByteSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.CharSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.DoubleSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.FloatSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.IntSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LongSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.ShortSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.VoidSerializer;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.IdentityMap;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;

/** Maps classes to serializers so object graphs can be serialized automatically.
 * @author Nathan Sweet <misc@n4te.com> */
public class Kryo {
	static public final byte NULL = 0;

	private SerializerFactory defaultSerializer = new NullSerializerFactory();
	private final ArrayList<DefaultSerializerEntry> defaultSerializers = new ArrayList(32);
	private final int lowPriorityDefaultSerializerCount;

	private final ClassResolver classResolver;
	private int nextRegisterID;
	private boolean registrationRequired = true;

	private int depth, maxDepth = Integer.MAX_VALUE;
	private boolean autoReset = true;
	private volatile Thread thread;
	private boolean copyReferences = true;

	private int copyDepth;
	private IdentityMap originalToCopy;
	private Object needsCopyReference;

	/** Creates a new Kryo with a {@link DefaultClassResolver} */
	public Kryo () {
		this(new DefaultClassResolver());
	}

	public Kryo(ClassResolver classResolver) {
		if (classResolver == null) throw new IllegalArgumentException("classResolver cannot be null.");

		this.classResolver = classResolver;
		classResolver.setKryo(this);

		lowPriorityDefaultSerializerCount = defaultSerializers.size();

		// Primitives and string. Primitive wrappers automatically use the same registration as primitives.
		register(int.class, new IntSerializer());
		register(String.class, new StringSerializer());
		register(float.class, new FloatSerializer());
		register(boolean.class, new BooleanSerializer());
		register(byte.class, new ByteSerializer());
		register(char.class, new CharSerializer());
		register(short.class, new ShortSerializer());
		register(long.class, new LongSerializer());
		register(double.class, new DoubleSerializer());
		register(void.class, new VoidSerializer());
	}

	// --- Default serializers ---
	/** Sets the serializer factory to use when no {@link #addDefaultSerializer(Class, Class) default serializers} match an object's
	 * type. Default is {@link com.esotericsoftware.kryo.factories.NullSerializerFactory}.
	 * @see #newDefaultSerializer(Class) */
	public void setDefaultSerializer (SerializerFactory serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		defaultSerializer = serializer;
	}

	/** Sets the serializer to use when no {@link #addDefaultSerializer(Class, Class) default serializers} match an object's type.
	 * @see #newDefaultSerializer(Class) */
	public void setDefaultSerializer (Class<? extends Serializer> serializer) {
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		defaultSerializer = new NullSerializerFactory();
	}

	public void addDefaultSerializer (Class type, SerializerFactory serializerFactory) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerFactory == null) throw new IllegalArgumentException("serializerFactory cannot be null.");
		DefaultSerializerEntry entry = new DefaultSerializerEntry(type, serializerFactory);
		defaultSerializers.add(defaultSerializers.size() - lowPriorityDefaultSerializerCount, entry);
	}

	public void addDefaultSerializer (Class type, Class<? extends Serializer> serializerClass) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializerClass == null) throw new IllegalArgumentException("serializerClass cannot be null.");
		DefaultSerializerEntry entry = new DefaultSerializerEntry(type, new NullSerializerFactory());
		defaultSerializers.add(defaultSerializers.size() - lowPriorityDefaultSerializerCount, entry);
	}

	/** Returns the best matching serializer for a class. This method can be overridden to implement custom logic to choose a
	 * serializer. */
	public Serializer getDefaultSerializer (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		for (int i = 0, n = defaultSerializers.size(); i < n; i++) {
			DefaultSerializerEntry entry = defaultSerializers.get(i);
			if (entry.type.isAssignableFrom(type)) {
				Serializer defaultSerializer = entry.serializerFactory.makeSerializer(this, type);
				return defaultSerializer;
			}
		}

		return newDefaultSerializer(type);
	}

	/** Called by {@link #getDefaultSerializer(Class)} when no default serializers matched the type. Subclasses can override this
	 * method to customize behavior. The default implementation calls {@link SerializerFactory#makeSerializer(Kryo, Class)} using
	 * the {@link #setDefaultSerializer(Class) default serializer}. */
	protected Serializer newDefaultSerializer (Class type) {
		return defaultSerializer.makeSerializer(this, type);
	}

	// --- Registration ---

	/** Registers the class using the lowest, next available integer ID and the {@link Kryo#getDefaultSerializer(Class) default
	 * serializer}. If the class is already registered, the existing entry is updated with the new serializer. Registering a
	 * primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
	 * using this method. The order must be the same at deserialization as it was for serialization. */
	public Registration register (Class type) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) return registration;
		return register(type, getDefaultSerializer(type));
	}

	/** Registers the class using the specified ID and the {@link Kryo#getDefaultSerializer(Class) default serializer}. If the ID is
	 * already in use by the same type, the old entry is overwritten. If the ID is already in use by a different type, a
	 * {@link KryoException} is thrown. Registering a primitive also affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * @param id Must be >= 0. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types and
	 *           String, but these IDs can be repurposed. */
	public Registration register (Class type, int id) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) return registration;
		return register(type, getDefaultSerializer(type), id);
	}

	/** Registers the class using the lowest, next available integer ID and the specified serializer. If the class is already
	 * registered, the existing entry is updated with the new serializer. Registering a primitive also affects the corresponding
	 * primitive wrapper.
	 * <p>
	 * Because the ID assigned is affected by the IDs registered before it, the order classes are registered is important when
	 * using this method. The order must be the same at deserialization as it was for serialization. */
	public Registration register (Class type, Serializer serializer) {
		Registration registration = classResolver.getRegistration(type);
		if (registration != null) {
			registration.setSerializer(serializer);
			return registration;
		}
		return classResolver.register(new Registration(type, serializer, getNextRegistrationId()));
	}

	/** Registers the class using the specified ID and serializer. If the ID is already in use by the same type, the old entry is
	 * overwritten. If the ID is already in use by a different type, a {@link KryoException} is thrown. Registering a primitive
	 * also affects the corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * @param id Must be >= 0. Smaller IDs are serialized more efficiently. IDs 0-8 are used by default for primitive types and
	 *           String, but these IDs can be repurposed. */
	public Registration register (Class type, Serializer serializer, int id) {
		if (id < 0) throw new IllegalArgumentException("id must be >= 0: " + id);
		return register(new Registration(type, serializer, id));
	}

	/** Stores the specified registration. If the ID is already in use by the same type, the old entry is overwritten. If the ID is
	 * already in use by a different type, a {@link KryoException} is thrown. Registering a primitive also affects the
	 * corresponding primitive wrapper.
	 * <p>
	 * IDs must be the same at deserialization as they were for serialization.
	 * <p>
	 * Registration can be suclassed to efficiently store per type information, accessible in serializers via
	 * {@link Kryo#getRegistration(Class)}. */
	public Registration register (Registration registration) {
		int id = registration.getId();
		if (id < 0) throw new IllegalArgumentException("id must be > 0: " + id);

		Registration existing = getRegistration(registration.getId());
		if (DEBUG && existing != null && existing.getType() != registration.getType()) {
			debug("An existing registration with a different type already uses ID: " + registration.getId()
				+ "\nExisting registration: " + existing + "\nUnable to set registration: " + registration);
		}

		return classResolver.register(registration);
	}

	/** @return the lowest, next available integer ID. */
	public int getNextRegistrationId () {
		while (nextRegisterID != -2) {
			if (classResolver.getRegistration(nextRegisterID) == null) return nextRegisterID;
			nextRegisterID++;
		}
		throw new KryoException("No registration IDs are available.");
	}

	/**
	 * @throws IllegalArgumentException if the class is not registered.
	 * @see ClassResolver#getRegistration(Class)
	 */
	public Registration getRegistration (Class type) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		Registration registration = classResolver.getRegistration(type);
		if (registration == null) {
			if (!type.isEnum() && Enum.class.isAssignableFrom(type)) {
				// This handles an enum value that is an inner class. Eg: enum A {b{}};
				registration = getRegistration(type.getEnclosingClass());
			} else if (EnumSet.class.isAssignableFrom(type)) {
				registration = classResolver.getRegistration(EnumSet.class);
			}
			if (registration == null) {
				throw new IllegalArgumentException("Class is not registered: " + className(type)
					+ "\nNote: To register this class use: kryo.register(" + className(type) + ".class);");
			}
		}
		return registration;
	}

	/** @see ClassResolver#getRegistration(int) */
	public Registration getRegistration (int classID) {
		return classResolver.getRegistration(classID);
	}

	/** Returns the serializer for the registration for the specified class.
	 * @see #getRegistration(Class)
	 * @see Registration#getSerializer() */
	public Serializer getSerializer (Class type) {
		return getRegistration(type).getSerializer();
	}

	// --- Serialization ---

	/** Writes a class and returns its registration.
	 * @param type May be null.
	 * @return Will be null if type is null.
	 * @see ClassResolver#writeClass(Output, Class) */
	public Registration writeClass (Output output, Class type) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		try {
			return classResolver.writeClass(output, type);
		} finally {
			if (depth == 0 && autoReset) reset();
		}
	}

	/** Writes the class and object or null using the registered serializer.
	 * @param object May be null. */
	public void writeClassAndObject (Output output, Object object) {
		if (output == null) throw new IllegalArgumentException("output cannot be null.");
		beginObject();
		try {
			if (object == null) {
				writeClass(output, null);
				return;
			}
			Registration registration = writeClass(output, object.getClass());
			if (TRACE || (DEBUG && depth == 1)) log("Write", object);
			registration.getSerializer().write(this, output, object);
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads a class and returns its registration.
	 * @return May be null.
	 * @see ClassResolver#readClass(Input) */
	public Registration readClass (Input input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		try {
			return classResolver.readClass(input);
		} finally {
			if (depth == 0 && autoReset) reset();
		}
	}

	/** Reads an object using the registered serializer. */
	public <T> T readObject (Input input, Class<T> type) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		beginObject();
		try {
			T object = (T)getRegistration(type).getSerializer().read(this, input, type);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads an object using the specified serializer. The registered serializer is ignored. */
	public <T> T readObject (Input input, Class<T> type, Serializer serializer) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (serializer == null) throw new IllegalArgumentException("serializer cannot be null.");
		beginObject();
		try {
			T object = (T)serializer.read(this, input, type);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Reads the class and object or null using the registered serializer.
	 * @return May be null. */
	public Object readClassAndObject (Input input) {
		if (input == null) throw new IllegalArgumentException("input cannot be null.");
		beginObject();
		try {
			Registration registration = readClass(input);
			if (registration == null) return null;
			Class type = registration.getType();

			Object object = registration.getSerializer().read(this, input, type);
			if (TRACE || (DEBUG && depth == 1)) log("Read", object);
			return object;
		} finally {
			if (--depth == 0 && autoReset) reset();
		}
	}

	/** Called by {@link Serializer#read(Kryo, Input, Class)} and {@link Serializer#copy(Kryo, Object)} before Kryo can be used to
	 * deserialize or copy child objects. Calling this method is unnecessary if Kryo is not used to deserialize or copy child
	 * objects.
	 * @param object May be null, unless calling this method from {@link Serializer#copy(Kryo, Object)}. */
	public void reference (Object object) {
		if (copyDepth > 0) {
			if (needsCopyReference != null) {
				if (object == null) throw new IllegalArgumentException("object cannot be null.");
				originalToCopy.put(needsCopyReference, object);
				needsCopyReference = null;
			}
		}
	}

	public void reset () {
		depth = 0;
		classResolver.reset();

		copyDepth = 0;
		if (originalToCopy != null) originalToCopy.clear(2048);

		if (TRACE) trace("kryo", "Object graph complete.");
	}

	/** Returns a deep copy of the object. Serializers for the classes involved must support {@link Serializer#copy(Kryo, Object)}.
	 * @param object May be null. */
	public <T> T copy (T object) {
		if (object == null) return null;
		copyDepth++;
		try {
			if (originalToCopy == null) originalToCopy = new IdentityMap();
			Object existingCopy = originalToCopy.get(object);
			if (existingCopy != null) return (T)existingCopy;

			if (copyReferences) needsCopyReference = object;
			Object copy = getSerializer(object.getClass()).copy(this, object);
			if (needsCopyReference != null) reference(copy);
			if (TRACE || (DEBUG && copyDepth == 1)) log("Copy", copy);
			return (T)copy;
		} finally {
			if (--copyDepth == 0) reset();
		}
	}

	// --- Utility ---

	private void beginObject () {
		if (DEBUG) {
			if (depth == 0)
				thread = Thread.currentThread();
			else if (thread != Thread.currentThread())
				throw new ConcurrentModificationException("Kryo must not be accessed concurrently by multiple threads.");
		}
		if (depth == maxDepth) throw new KryoException("Max depth exceeded: " + depth);
		depth++;
	}

	public boolean isRegistrationRequired () {
		return registrationRequired;
	}

	/** Returns the number of child objects away from the object graph root. */
	public int getDepth () {
		return depth;
	}

	static final class DefaultSerializerEntry {
		final Class type;
		final SerializerFactory serializerFactory;

		DefaultSerializerEntry (Class type, SerializerFactory serializerFactory) {
			this.type = type;
			this.serializerFactory = serializerFactory;
		}
	}
}
