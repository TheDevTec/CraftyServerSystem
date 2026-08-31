package me.devtec.craftyserversystem.nbt;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class NbtReader {

	private static final int TAG_END = 0;
	private static final int TAG_BYTE = 1;
	private static final int TAG_SHORT = 2;
	private static final int TAG_INT = 3;
	private static final int TAG_LONG = 4;
	private static final int TAG_FLOAT = 5;
	private static final int TAG_DOUBLE = 6;
	private static final int TAG_BYTE_ARRAY = 7;
	private static final int TAG_STRING = 8;
	private static final int TAG_LIST = 9;
	private static final int TAG_COMPOUND = 10;
	private static final int TAG_INT_ARRAY = 11;
	private static final int TAG_LONG_ARRAY = 12;

	public static NbtTag read(Path path) throws IOException {
		try (InputStream raw = Files.newInputStream(path);
				InputStream input = maybeGunzip(raw);
				DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {

			int type = in.readUnsignedByte();

			if (type == TAG_END)
				return new NbtEnd();

			String name = in.readUTF();
			Object value = readPayload(in, type);

			return wrap(type, name, value);
		}
	}

	public static NbtTag read(byte[] data) throws IOException {
		try (InputStream raw = new java.io.ByteArrayInputStream(data)) {
			return read(raw);
		}
	}

	public static NbtTag read(InputStream raw) throws IOException {
		try (InputStream input = maybeGunzip(raw);
				DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {

			int type = in.readUnsignedByte();

			if (type == TAG_END)
				return new NbtEnd();

			String name = in.readUTF();
			Object value = readPayload(in, type);

			return wrap(type, name, value);
		}
	}

	private static InputStream maybeGunzip(InputStream input) throws IOException {
		PushbackInputStream pushback = new PushbackInputStream(input, 2);

		int b1 = pushback.read();
		int b2 = pushback.read();

		if (b2 != -1)
			pushback.unread(b2);

		if (b1 != -1)
			pushback.unread(b1);

		// GZIP magic header: 1F 8B
		if (b1 == 0x1F && b2 == 0x8B)
			return new GZIPInputStream(pushback);

		return pushback;
	}

	private static Object readPayload(DataInputStream in, int type) throws IOException {
		switch (type) {
		case TAG_BYTE:
			return in.readByte();
		case TAG_SHORT:
			return in.readShort();
		case TAG_INT:
			return in.readInt();
		case TAG_LONG:
			return in.readLong();
		case TAG_FLOAT:
			return in.readFloat();
		case TAG_DOUBLE:
			return in.readDouble();
		case TAG_BYTE_ARRAY:
			return readByteArray(in);
		case TAG_STRING:
			return in.readUTF();
		case TAG_LIST:
			return readList(in);
		case TAG_COMPOUND:
			return readCompound(in);
		case TAG_INT_ARRAY:
			return readIntArray(in);
		case TAG_LONG_ARRAY:
			return readLongArray(in);
		default:
			throw new IOException("Uknown NBT tag type: " + type);
		}
	}

	private static byte[] readByteArray(DataInputStream in) throws IOException {
		int length = in.readInt();

		if (length < 0)
			throw new IOException("Invalid length of ByteArray: " + length);

		byte[] array = new byte[length];
		in.readFully(array);
		return array;
	}

	private static int[] readIntArray(DataInputStream in) throws IOException {
		int length = in.readInt();

		if (length < 0)
			throw new IOException("Invalid length of IntArray: " + length);

		int[] array = new int[length];

		for (int i = 0; i < length; i++)
			array[i] = in.readInt();

		return array;
	}

	private static long[] readLongArray(DataInputStream in) throws IOException {
		int length = in.readInt();

		if (length < 0)
			throw new IOException("Invalid length of LongArray: " + length);

		long[] array = new long[length];

		for (int i = 0; i < length; i++)
			array[i] = in.readLong();

		return array;
	}

	private static NbtList readList(DataInputStream in) throws IOException {
		int childType = in.readUnsignedByte();
		int length = in.readInt();

		if (length < 0)
			throw new IOException("Invalid length of List: " + length);

		List<Object> list = new ArrayList<>(length);

		for (int i = 0; i < length; i++)
			list.add(readPayload(in, childType));

		return new NbtList("", childType, list);
	}

	private static NbtCompound readCompound(DataInputStream in) throws IOException {
		Map<String, Object> values = new LinkedHashMap<>();

		while (true) {
			int type = in.readUnsignedByte();

			if (type == TAG_END)
				break;

			String name = in.readUTF();
			Object value = readPayload(in, type);

			values.put(name, value);
		}

		return new NbtCompound("", values);
	}

	private static NbtTag wrap(int type, String name, Object value) {
		if (value instanceof NbtCompound)
			return new NbtCompound(name, ((NbtCompound)value).values);

		if (value instanceof NbtList)
			return new NbtList(name, ((NbtList)value).childType, ((NbtList)value).values);

		return new NbtValue(name, type, value);
	}
}
