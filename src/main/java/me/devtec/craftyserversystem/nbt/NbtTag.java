package me.devtec.craftyserversystem.nbt;

import java.util.Map;

public interface NbtTag {
	String name();

	Map<String, Object> asJson();

	String asJsonString();

	Object asNms();
}
