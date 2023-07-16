package me.devtec.craftyserversystem.commands.internal.warp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.game.Position;

public class WarpManager {

	private static WarpManager instance;

	public static WarpManager getProvider() {
		if (instance == null)
			instance = new WarpManager();
		return instance;
	}

	private WarpManager() {
	}

	private Map<String, WarpInfo> warps = new HashMap<>();
	private boolean isLoaded;

	public void load() {
		if (isLoaded())
			return;
		isLoaded = true;
		Config file = Loader.getPlugin().getConfigManager().getWarpsStorage();
		for (String name : file.getKeys()) {
			WarpInfo info;
			warps.put(name, info = new WarpInfo(file.getAs(name + ".pos", Position.class)));
			info.setCost(file.getDouble(name + ".cost"));
			info.setIcon(file.getAs(name + ".icon", ItemStack.class, new ItemStack(Material.STONE)));
			String cd = file.getString(name + ".cd");
			if (cd != null) {
				CooldownHolder cooldown = Loader.getPlugin().getCdManager().getOrPrepare(cd);
				if (cooldown != null)
					info.setCooldown(cooldown);
			}
			info.setPermission(file.getString(name + ".perm"));
		}
	}

	public void unload(boolean save) {
		if (!isLoaded())
			return;
		isLoaded = false;
		if (save) {
			Config file = Loader.getPlugin().getConfigManager().getWarpsStorage().clear();
			for (Entry<String, WarpInfo> entry : warps.entrySet()) {
				if (entry.getValue().getCost() > 0)
					file.set(entry.getKey() + ".cost", entry.getValue().getCost());
				file.set(entry.getKey() + ".pos", entry.getValue().getPosition());
				file.set(entry.getKey() + ".icon", entry.getValue().getIcon());
				file.set(entry.getKey() + ".perm", entry.getValue().getPermission());
				if (entry.getValue().getCooldown() != null)
					file.set(entry.getKey() + ".cd", entry.getValue().getCooldown().id());
			}
			file.save("yaml");
		}
		warps.clear();
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public WarpInfo get(String name) {
		return warps.get(name.toLowerCase());
	}

	public WarpInfo create(String name, Position pos) {
		WarpInfo info;
		warps.put(name.toLowerCase(), info = new WarpInfo(pos));
		info.setIcon(new ItemStack(Material.STONE));
		return info;
	}

	public boolean delete(String name) {
		return warps.remove(name.toLowerCase()) != null;
	}

	public Set<String> getWarps() {
		return warps.keySet();
	}
}
