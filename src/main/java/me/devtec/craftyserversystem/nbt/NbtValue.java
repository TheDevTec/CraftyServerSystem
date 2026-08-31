package me.devtec.craftyserversystem.nbt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.devtec.shared.json.Json;
import me.devtec.theapi.bukkit.BukkitLoader;

public class NbtValue implements NbtTag {
	private final String name;
	private final int type;
	private final Object value;

	public NbtValue(String name, int type, Object value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Map<String, Object> asJson() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(name, rawValue(value));
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

	public int type() {
		return type;
	}

	public Object value() {
		return value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}