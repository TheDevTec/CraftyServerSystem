package me.devtec.craftyserversystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.economy.CssEconomyHook;
import me.devtec.craftyserversystem.economy.VaultEconomyHook;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.permission.VaultPermissionHook;
import me.devtec.craftyserversystem.utils.CraftyVersionChecker;

public class Loader extends JavaPlugin {

	private static Loader plugin;

	@Override
	public void onLoad() {
		plugin = this;

		if (!checkOrInstallTheAPI())
			return; // Error

		// Init managers
		API.get().start();
	}

	private boolean checkOrInstallTheAPI() {
		if (Bukkit.getPluginManager().getPlugin("TheAPI") == null) {
			File file = new File("plugins/TheAPI.jar");
			try {
				downloadFileFromUrl(new URL("https://api.spiget.org/v2/resources/72679/download"), file);
				Bukkit.getPluginManager().loadPlugin(file);
			} catch (UnknownDependencyException | InvalidPluginException | InvalidDescriptionException | MalformedURLException e) {
				e.printStackTrace();
				plugin.getLogger().severe("Failed to load TheAPI plugin (library)");
				plugin.getLogger().severe("Disabling plugin...");
				Bukkit.getPluginManager().disablePlugin(plugin);
				return false;
			}
		}
		try {
			if (isOlderThan(Bukkit.getPluginManager().getPlugin("TheAPI").getDescription().getVersion(), CraftyVersionChecker.versionOfTheAPIFromSpigot())) {
				File file = new File("plugins/update/TheAPI.jar");
				try {
					downloadFileFromUrl(new URL("https://api.spiget.org/v2/resources/72679/download"), file);
					if (isOlderThan(Bukkit.getPluginManager().getPlugin("TheAPI").getDescription().getVersion(), "12.7")) {
						plugin.getLogger().severe("Downloaded required & newest update of TheAPI plugin, please restart server.");
						Bukkit.getPluginManager().disablePlugin(plugin);
						return false;
					}
					plugin.getLogger().severe("Downloaded newest update of TheAPI plugin, please restart server.");
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					plugin.getLogger().severe("Failed to download newest update of TheAPI plugin (library)");
					plugin.getLogger().severe("Disabling plugin...");
					Bukkit.getPluginManager().disablePlugin(plugin);
					return false;
				}
			}
		} catch (Exception e) {
		}
		return true;
	}

	public static boolean isOlderThan(String version, String compareVersion) {
		if (version == null || compareVersion == null)
			return true;

		version = version.replaceAll("[^0-9.]+", "").trim();
		compareVersion = compareVersion.replaceAll("[^0-9.]+", "").trim();

		if (version.isEmpty() || compareVersion.isEmpty())
			return true;

		String[] primaryVersion = version.split("\\.");
		String[] compareToVersion = compareVersion.split("\\.");

		int max = Math.max(primaryVersion.length, compareToVersion.length);
		for (int i = 0; i <= max; ++i) {
			String number = i >= primaryVersion.length ? "0" : "1" + primaryVersion[i];
			if (compareToVersion.length <= i) {
				if (compareToVersion.length == i && compareToVersion.length == max)
					break;
				return false;
			}
			if (Integer.parseInt(number) > Integer.parseInt("1" + compareToVersion[i]))
				return false;
			if (Integer.parseInt(number) < Integer.parseInt("1" + compareToVersion[i]))
				return true;
		}
		return false;
	}

	public void downloadFileFromUrl(URL url, File file) {
		try {
			if (file.exists() && !file.delete())
				return;
			if (!file.exists()) {
				if (file.getParentFile() != null)
					file.getParentFile().mkdirs();
				file.createNewFile();
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", "DevTec-JavaClient");
				conn.setRequestMethod("GET");
				try (InputStream in = conn.getInputStream()) {
					byte[] buf = new byte[4096];
					int r;
					try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
						while ((r = in.read(buf)) != -1)
							out.write(buf, 0, r);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		if (API.get().getCommandManager() == null) {
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}
		if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms"))
			API.get().setPermissionHook(new LuckPermsPermissionHook());
		if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
			API.get().setPermissionHook(new VaultPermissionHook());
			if (!(API.get().getEconomyHook() instanceof CssEconomyHook))
				API.get().setEconomyHook(new VaultEconomyHook());
		}
		API.get().getCommandManager().register();
		API.get().getConfigManager().loadSpawn();
		API.get().getListenerManager().register();
	}

	@Override
	public void onDisable() {
		if (API.get().getCommandManager() == null)
			return;
		API.get().getCommandManager().unregister();
		API.get().getListenerManager().unregister();
		API.get().shutdown();
	}

	/**
	 * This plugin instance
	 *
	 * @return Loader
	 */
	public static Loader getPlugin() {
		return plugin;
	}
}
