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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.economy.CssEconomyHook;
import me.devtec.craftyserversystem.economy.VaultEconomyHook;
import me.devtec.craftyserversystem.events.ChatListener;
import me.devtec.craftyserversystem.events.JoinListener;
import me.devtec.craftyserversystem.events.QuitListener;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.permission.VaultPermissionHook;

public class Loader extends JavaPlugin implements Listener {

	private static Loader plugin;

	// TODO check list:
	// Heal - freeze ticks

	@Override
	public void onLoad() {
		plugin = this;

		if (!checkOrInstallTheAPI())
			return; // Error

		// Init managers
		API.get().init();
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
		return true;
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
		if (API.get().getCommandManager() == null)
			return;
		API.get().getCommandManager().register();
		API.get().getConfigManager().loadSpawn();

		if (API.get().getConfigManager().getChat().getBoolean("enabled"))
			Bukkit.getPluginManager().registerEvents(new ChatListener(), plugin);
		if (API.get().getConfigManager().getJoin().getBoolean("enabled"))
			Bukkit.getPluginManager().registerEvents(new JoinListener(), plugin);
		if (API.get().getConfigManager().getQuit().getBoolean("enabled"))
			Bukkit.getPluginManager().registerEvents(new QuitListener(), plugin);
	}

	@Override
	public void onDisable() {
		if (API.get().getCommandManager() == null)
			return;
		API.get().getCommandManager().unregister();
	}

	@EventHandler
	public void onPluginEnable(PluginEnableEvent e) {
		if (e.getPlugin().getName().equals("Vault")) {
			API.get().setPermissionHook(new VaultPermissionHook());
			if (!(API.get().getEconomyHook() instanceof CssEconomyHook))
				API.get().setEconomyHook(new VaultEconomyHook());
		}
		if (e.getPlugin().getName().equals("LuckPerms"))
			API.get().setPermissionHook(new LuckPermsPermissionHook());
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
