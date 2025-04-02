package me.devtec.craftyserversystem.events.internal;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

public class JoinListener implements CssListener {

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getJoin();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {

	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		String time = e.getPlayer().hasPlayedBefore() ? "normal" : "first";
		e.setJoinMessage(null); // Remove message

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName())
				.papi(e.getPlayer().getUniqueId());
		// Send json message
		Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
		API.get().getMsgManager().sendMessageFromFile(getConfig(), "join." + time + ".text", placeholders,
				onlinePlayers);
		API.get().getMsgManager().sendMessageFromFile(getConfig(), "join." + time + ".broadcast", placeholders,
				onlinePlayers);
		for (String cmd : placeholders.apply(getConfig().getStringList("join." + time + ".commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}
}
