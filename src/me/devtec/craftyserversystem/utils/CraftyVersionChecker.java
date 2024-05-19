package me.devtec.craftyserversystem.utils;

import java.net.URL;

import org.bukkit.Bukkit;

import me.devtec.shared.utility.StreamUtils;

public class CraftyVersionChecker {

	public static String versionOfTheAPIFromSpigot() {
		try {
			return StreamUtils.fromStream(new URL("https://api.spigotmc.org/legacy/update.php?resource=72679").openStream(), 64);
		} catch (Exception e) {
			return Bukkit.getPluginManager().getPlugin("TheAPI").getDescription().getVersion();
		}
	}
}
