package me.devtec.craftyserversystem.nbt;

import java.util.LinkedHashMap;

import me.devtec.shared.json.Json;
import me.devtec.theapi.bukkit.BukkitLoader;

public class NbtEnd implements NbtTag {
	@Override
	public String name() {
		return "";
	}

	@Override
	public String toString() {
		return "END";
	}

	@Override
	public java.util.Map<String, Object> asJson() {
		return new LinkedHashMap<>();
	}

	@Override
	public String asJsonString() {
		return Json.writer().simpleWrite(asJson());
	}

	@Override
	public Object asNms() {
		return BukkitLoader.getNmsProvider().parseNBT(asJsonString());
	}
}