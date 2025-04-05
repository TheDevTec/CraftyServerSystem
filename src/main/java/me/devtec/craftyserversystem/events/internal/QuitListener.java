package me.devtec.craftyserversystem.events.internal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

public class QuitListener implements CssListener {

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getQuit();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {

	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		e.setQuitMessage(null); // Remove message

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName())
				.papi(e.getPlayer().getUniqueId());
		// Send json message
		List<Player> players = new ArrayList<>();
		for (Player online : BukkitLoader.getOnlinePlayers())
			if (online.equals(e.getPlayer()) || online.canSee(e.getPlayer()))
				players.add(online);
		API.get().getMsgManager().sendMessageFromFile(getConfig(), "quit.text", placeholders, players);
		for (String cmd : placeholders.apply(getConfig().getStringList("quit.commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}
}
