package me.devtec.craftyserversystem.nbt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.devtec.shared.json.Json;
import me.devtec.theapi.bukkit.BukkitLoader;

public class NbtCompound implements NbtTag {
	private final String name;
	protected final Map<String, Object> values;

	public NbtCompound(String name, Map<String, Object> values) {
		this.name = name;
		this.values = values;
	}

	@Override
	public String name() {
		return name;
	}

	public Map<String, Object> values() {
		return values;
	}

	public Object getAny(String key) {
		return values.get(key);
	}

	public boolean contains(String key) {
		return values.containsKey(key);
	}

	public NbtCompound getCompound(String key) {
		Object value = values.get(key);

		if (value instanceof NbtCompound)
			return (NbtCompound)value;

		return null;
	}

	public List<?> getListValue(String key) {
		Object value = values.get(key);

		if (value instanceof NbtList)
			return ((NbtList)value).values();

		return Collections.emptyList();
	}

	public String getString(String key, String def) {
		Object value = values.get(key);

		if (value instanceof String)
			return (String)value;

		return def;
	}

	public byte getByte(String key, byte def) {
		Object value = values.get(key);

		if (value instanceof Number)
			return ((Number)value).byteValue();

		return def;
	}

	public short getShort(String key, short def) {
		Object value = values.get(key);

		if (value instanceof Number)
			return ((Number)value).shortValue();

		return def;
	}

	public int getInt(String key, int def) {
		Object value = values.get(key);

		if (value instanceof Number)
			return ((Number)value).intValue();

		return def;
	}

	public long getLong(String key, long def) {
		Object value = values.get(key);

		if (value instanceof Number)
			return ((Number)value).longValue();

		return def;
	}

	public float getFloat(String key, float def) {
		Object value = values.get(key);

		if (value instanceof Number)
			return ((Number)value).floatValue();

		return def;
	}

	public double getDouble(String key, double def) {
		Object value = values.get(key);

		if (value instanceof Number)
			return ((Number)value).doubleValue();

		return def;
	}

	@Override
	public Map<String, Object> asJson() {
		Map<String, Object> map = new LinkedHashMap<>(values.size());

		for (Map.Entry<String, ? extends Object> entry : values.entrySet())
			map.put(entry.getKey(), rawValue(entry.getValue()));

		return map;
	}

	@Override
	public String asJsonString() {
		return Json.writer().simpleWrite(asJson());
	}

	@Override
	public Object asNms() {
		return BukkitLoader.getNmsProvider().parseNBT(asJsonString());
	}

	private static Object rawValue(Object value) {
		if (value == null)
			return null;

		if (value instanceof NbtValue)
			return rawValue(((NbtValue)value).value());

		if (value instanceof NbtCompound)
			return ((NbtCompound)value).asJson();

		if (value instanceof NbtList) {
			List<Object> result = new ArrayList<>(((NbtList)value).values().size());

			for (Object element : ((NbtList)value).values())
				result.add(rawValue(element));

			return result;
		}

		if (value instanceof Map) {
			Map<String, Object> result = new LinkedHashMap<>(((Map<?, ?>)value).size());

			for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet())
				result.put(String.valueOf(entry.getKey()), rawValue(entry.getValue()));

			return result;
		}

		if (value instanceof List) {
			List<Object> result = new ArrayList<>(((List<?>)value).size());

			for (Object element : (List<?>)value)
				result.add(rawValue(element));

			return result;
		}

		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			List<Object> result = new ArrayList<>(length);

			for (int i = 0; i < length; i++)
				result.add(rawValue(Array.get(value, i)));

			return result;
		}

		return value;
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
