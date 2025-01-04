package me.devtec.craftyserversystem.commands.internal.afk;

import java.util.UUID;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.internal.AfkListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

@IgnoredClass
public class AfkManager {

	@Nonnull
	private static AfkManager provider;

	@Nonnull
	public static AfkManager getProvider() {
		if (provider == null)
			provider = new AfkManager();
		return provider;
	}

	private AfkManager() {

	}

	public boolean isAfk(UUID uuid) {
		return me.devtec.shared.API.getUser(uuid).getBoolean("afk");
	}

	public void startAfk(UUID uuid, boolean runActions) {
		if (AfkListener.autoAfk != null)
			AfkListener.autoAfk.put(uuid, 0L);
		Config user = me.devtec.shared.API.getUser(uuid);
		if (!user.getBoolean("afk")) {
			user.set("afk", true);
			if (runActions) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", me.devtec.shared.API.offlineCache().lookupNameById(uuid)).papi(uuid);
				// Send json message
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "afk.start.broadcast", placeholders, BukkitLoader.getOnlinePlayers());
				BukkitLoader.getNmsProvider().postToMainThread(() -> {
					for (String cmd : placeholders.apply(API.get().getConfigManager().getMain().getStringList("afk.start.commands")))
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
				});
			}
		}
	}

	public void stopAfk(UUID uuid, boolean runActions) {
		if (AfkListener.autoAfk != null)
			AfkListener.autoAfk.put(uuid, System.currentTimeMillis() / 1000);
		Config user = me.devtec.shared.API.getUser(uuid);
		if (user.getBoolean("afk")) {
			user.set("afk", false);
			if (runActions) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", me.devtec.shared.API.offlineCache().lookupNameById(uuid)).papi(uuid);
				// Send json message
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "afk.stop.broadcast", placeholders, BukkitLoader.getOnlinePlayers());
				BukkitLoader.getNmsProvider().postToMainThread(() -> {
					for (String cmd : placeholders.apply(API.get().getConfigManager().getMain().getStringList("afk.stop.commands")))
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
				});
			}
		}
	}
}
