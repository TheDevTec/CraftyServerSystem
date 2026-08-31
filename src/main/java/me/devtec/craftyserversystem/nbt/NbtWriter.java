package me.devtec.craftyserversystem.nbt;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class NbtWriter {

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

	private NbtWriter() {
	}

	public static void write(File file, NbtTag tag) throws IOException {
		write(file.toPath(), tag, false);
	}

	public static void write(File file, NbtTag tag, boolean gzip) throws IOException {
		write(file.toPath(), tag, gzip);
	}

	public static void write(Path path, NbtTag tag) throws IOException {
		write(path, tag, false);
	}

	public static void write(Path path, NbtTag tag, boolean gzip) throws IOException {
		Path parent = path.getParent();

		if (parent != null)
			Files.createDirectories(parent);

		try (OutputStream file = new FileOutputStream(path.toFile());
				OutputStream output = gzip ? new GZIPOutputStream(file) : file;
				DataOutputStream out = new DataOutputStream(new BufferedOutputStream(output))) {

			writeRoot(out, tag);
		}
	}

	private static void writeRoot(DataOutputStream out, NbtTag tag) throws IOException {
		int type = typeOf(tag);

		out.writeByte(type);

		if (type == TAG_END)
			return;

		out.writeUTF(tag.name() == null ? "" : tag.name());
		writePayload(out, type, tag);
	}

	private static void writePayload(DataOutputStream out, int type, Object value) throws IOException {
		if (value instanceof NbtValue)
			value = ((NbtValue)value).value();

		switch (type) {
		case TAG_END:
			return;

		case TAG_BYTE:
			if (value instanceof Boolean)
				out.writeByte((Boolean)value ? 1 : 0);
			else
				out.writeByte(((Number) value).byteValue());
			return;

		case TAG_SHORT:
			out.writeShort(((Number) value).shortValue());
			return;

		case TAG_INT:
			out.writeInt(((Number) value).intValue());
			return;

		case TAG_LONG:
			out.writeLong(((Number) value).longValue());
			return;

		case TAG_FLOAT:
			out.writeFloat(((Number) value).floatValue());
			return;

		case TAG_DOUBLE:
			out.writeDouble(((Number) value).doubleValue());
			return;

		case TAG_BYTE_ARRAY:
			writeByteArray(out, value);
			return;

		case TAG_STRING:
			out.writeUTF(String.valueOf(value));
			return;

		case TAG_LIST:
			writeList(out, (NbtList) value);
			return;

		case TAG_COMPOUND:
			writeCompound(out, (NbtCompound) value);
			return;

		case TAG_INT_ARRAY:
			writeIntArray(out, value);
			return;

		case TAG_LONG_ARRAY:
			writeLongArray(out, value);
			return;

		default:
			throw new IOException("Unknown NBT tag type: " + type);
		}
	}

	private static void writeCompound(DataOutputStream out, NbtCompound compound) throws IOException {
		Map<?, ?> values = compound.values();

		for (Map.Entry<?, ?> entry : values.entrySet()) {
			String name = String.valueOf(entry.getKey());
			Object value = entry.getValue();

			if (value == null || value instanceof NbtEnd)
				continue;

			int type = typeOf(value);

			if (type == TAG_END)
				continue;

			out.writeByte(type);
			out.writeUTF(name);
			writePayload(out, type, value);
		}

		out.writeByte(TAG_END);
	}

	private static void writeList(DataOutputStream out, NbtList list) throws IOException {
		int childType = list.childType();
		List<?> values = list.values();

		out.writeByte(childType);
		out.writeInt(values.size());

		for (Object value : values)
			writePayload(out, childType, value);
	}

	private static void writeByteArray(DataOutputStream out, Object value) throws IOException {
		if (value instanceof byte[]) {
			out.writeInt(((byte[])value).length);
			out.write((byte[])value);
			return;
		}

		if (value instanceof List) {
			out.writeInt(((List<?>)value).size());

			for (Object element : (List<?>)value)
				out.writeByte(((Number) element).byteValue());

			return;
		}

		throw new IOException("Invalid TAG_Byte_Array value: " + value.getClass().getName());
	}

	private static void writeIntArray(DataOutputStream out, Object value) throws IOException {
		if (value instanceof int[]) {
			out.writeInt(((int[])value).length);

			for (int element : (int[])value)
				out.writeInt(element);

			return;
		}

		if (value instanceof List) {
			out.writeInt(((List<?>)value).size());


			for (Object element : (List<?>)value)
				out.writeInt(((Number) element).intValue());

			return;
		}

		throw new IOException("Invalid TAG_Int_Array value: " + value.getClass().getName());
	}

	private static void writeLongArray(DataOutputStream out, Object value) throws IOException {
		if (value instanceof long[]) {
			out.writeInt(((long[])value).length);

			for (long element : (long[])value)
				out.writeLong(element);

			return;
		}

		if (value instanceof List) {
			out.writeInt(((List<?>)value).size());

			for (Object element : (List<?>)value)
				out.writeLong(((Number) element).longValue());

			return;
		}

		throw new IOException("Invalid TAG_Long_Array value: " + value.getClass().getName());
	}

	private static int typeOf(Object value) throws IOException {
		if (value instanceof NbtEnd)
			return TAG_END;

		if (value instanceof NbtValue)
			return ((NbtValue)value).type();

		if (value instanceof NbtList)
			return TAG_LIST;

		if (value instanceof NbtCompound)
			return TAG_COMPOUND;

		if (value instanceof Byte || value instanceof Boolean)
			return TAG_BYTE;

		if (value instanceof Short)
			return TAG_SHORT;

		if (value instanceof Integer)
			return TAG_INT;

		if (value instanceof Long)
			return TAG_LONG;

		if (value instanceof Float)
			return TAG_FLOAT;

		if (value instanceof Double)
			return TAG_DOUBLE;

		if (value instanceof byte[])
			return TAG_BYTE_ARRAY;

		if (value instanceof String || value instanceof Character)
			return TAG_STRING;

		if (value instanceof int[])
			return TAG_INT_ARRAY;

		if (value instanceof long[])
			return TAG_LONG_ARRAY;

		throw new IOException("Unsupported NBT value: "
				+ (value == null ? "null" : value.getClass().getName()));
	}
}