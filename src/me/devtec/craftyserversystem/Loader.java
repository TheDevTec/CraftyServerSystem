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

import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.economy.EmptyEconomyHook;
import me.devtec.craftyserversystem.economy.VaultEconomyHook;
import me.devtec.craftyserversystem.managers.CommandManager;
import me.devtec.craftyserversystem.managers.ConfigurationManager;
import me.devtec.craftyserversystem.managers.CooldownManager;
import me.devtec.craftyserversystem.managers.MessageManager;
import me.devtec.craftyserversystem.permission.EmptyPermissionHook;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.permission.PermissionHook;
import me.devtec.craftyserversystem.permission.VaultPermissionHook;

public class Loader extends JavaPlugin implements Listener {

	private static Loader plugin;

	private ConfigurationManager cfgManager;
	private CommandManager cmdManager;
	private CooldownManager cdManager;
	private MessageManager msgManager;

	private PermissionHook permissionHook;
	private EconomyHook economyHook;

	// TODO check list:
	// Heal - freeze ticks

	@Override
	public void onLoad() {
		plugin = this;

		if (!checkOrInstallTheAPI())
			return; // Error

		// Init managers
		permissionHook = new EmptyPermissionHook();
		economyHook = new EmptyEconomyHook();
		cfgManager = new ConfigurationManager("config.yml", "translations.yml", "commands.yml", "cooldowns.yml").initFromJar();
		cmdManager = new CommandManager(cfgManager);
		cdManager = new CooldownManager();
		msgManager = new MessageManager();
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
		if (cmdManager != null)
			cmdManager.register();
	}

	@Override
	public void onDisable() {
		if (cmdManager != null)
			cmdManager.unregister();
	}

	@EventHandler
	public void onPluginEnable(PluginEnableEvent e) {
		if (e.getPlugin().getName().equals("Vault")) {
			permissionHook = new VaultPermissionHook();
			economyHook = new VaultEconomyHook();
		}
		if (e.getPlugin().getName().equals("LuckPerms"))
			permissionHook = new LuckPermsPermissionHook();
	}

	/**
	 *
	 * @return ConfigurationManager
	 */
	public ConfigurationManager getConfigManager() {
		return cfgManager;
	}

	/**
	 *
	 * @return CommandManager
	 */
	public CommandManager getCmdManager() {
		return cmdManager;
	}

	/**
	 *
	 * @return CooldownManager
	 */
	public CooldownManager getCdManager() {
		return cdManager;
	}

	/**
	 *
	 * @return MessageManager
	 */
	public MessageManager getMsgManager() {
		return msgManager;
	}

	/**
	 * This plugin instance
	 *
	 * @return Loader
	 */
	public static Loader getPlugin() {
		return plugin;
	}

	public PermissionHook getPermissionHook() {
		return permissionHook;
	}

	public EconomyHook getEconomyHook() {
		return economyHook;
	}
}
