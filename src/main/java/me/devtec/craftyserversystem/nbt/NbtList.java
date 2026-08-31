package me.devtec.craftyserversystem.nbt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.devtec.shared.json.Json;
import me.devtec.theapi.bukkit.BukkitLoader;

public class NbtList implements NbtTag {
	private final String name;
	protected final int childType;
	protected final List<Object> values;

	public NbtList(String name, int childType, List<Object> values) {
		this.name = name;
		this.childType = childType;
		this.values = values;
	}

	@Override
	public String name() {
		return name;
	}

	public int childType() {
		return childType;
	}

	public List<Object> values() {
		return values;
	}

	@Override
	public String toString() {
		return values.toString();
	}

	@Override
	public Map<String, Object> asJson() {
		Map<String, Object> map = new LinkedHashMap<>();
		List<Object> list = new ArrayList<>(values.size());

		for (Object value : values)
			list.add(rawValue(value));

		map.put(name, list);
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
}
