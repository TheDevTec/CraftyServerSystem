package me.devtec.craftyserversystem.commands.internal.afk;

import java.util.UUID;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.api.events.AfkToggleEvent;
import me.devtec.craftyserversystem.events.internal.AfkListener;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.events.EventManager;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.text.TextRenderer;
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

	private TextRenderer renderer() {
		return TextRenderer.create().placeholder("prefix", API.get().getConfigManager().getPrefix()).colorize();
	}

	private String render(TextRenderer renderer, String text) {
		return renderer.render(PlaceholderAPI.apply(text, renderer.target()));
	}

	public void startAfk(UUID uuid, boolean runActions) {
		Config user = me.devtec.shared.API.getUser(uuid);

		if (!user.getBoolean("afk")) {
			AfkToggleEvent event = new AfkToggleEvent(uuid, true);
			EventManager.call(event);

			if (event.isCancelled())
				return;

			user.set("afk", true);

			if (runActions) {
				TextRenderer renderer = renderer().target(uuid).placeholder("player",
						me.devtec.shared.API.offlineCache().lookupNameById(uuid));

				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(),
						"afk.start.broadcast", renderer, BukkitLoader.getOnlinePlayers());

				BukkitLoader.getNmsProvider().postToMainThread(() -> {
					for (String cmd : API.get().getConfigManager().getMain().getStringList("afk.start.commands"))
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), render(renderer, cmd));
				});
			}
		}

		if (AfkListener.autoAfk != null)
			AfkListener.autoAfk.put(uuid, System.currentTimeMillis() / 1000);
	}

	public void stopAfk(UUID uuid, boolean runActions) {
		Config user = me.devtec.shared.API.getUser(uuid);

		if (user.getBoolean("afk")) {
			AfkToggleEvent event = new AfkToggleEvent(uuid, false);
			EventManager.call(event);

			if (event.isCancelled())
				return;

			user.set("afk", false);

			if (runActions) {
				TextRenderer renderer = renderer().target(uuid).placeholder("player",
						me.devtec.shared.API.offlineCache().lookupNameById(uuid));

				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(),
						"afk.stop.broadcast", renderer, BukkitLoader.getOnlinePlayers());

				BukkitLoader.getNmsProvider().postToMainThread(() -> {
					for (String cmd : API.get().getConfigManager().getMain().getStringList("afk.stop.commands"))
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), render(renderer, cmd));
				});
			}
		}

		if (AfkListener.autoAfk != null)
			AfkListener.autoAfk.put(uuid, System.currentTimeMillis() / 1000);
	}
}