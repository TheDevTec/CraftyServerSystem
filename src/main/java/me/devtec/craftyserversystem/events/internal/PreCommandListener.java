package me.devtec.craftyserversystem.events.internal;

import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.cache.TempMap;
import me.devtec.theapi.bukkit.gui.GUI;

public class PreCommandListener implements CssListener {
	public static final Map<String, GUI> guis = new TempMap<String, GUI>(20 * 60 * 15).setCallback(value -> value.getValue().close());

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getChat();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {

	}

	@EventHandler
	public void onJoin(PlayerCommandPreprocessEvent e) {
		if (e.getMessage().startsWith("/css-openinv ")) {
			e.setCancelled(true);
			GUI gui = guis.get(e.getMessage().substring(13));
			if (gui != null)
				gui.open(e.getPlayer());
		}
	}
}
