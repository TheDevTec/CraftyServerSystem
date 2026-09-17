package me.devtec.craftyserversystem.events.internal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.text.TextRenderer;
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
		e.setQuitMessage(null);

		TextRenderer renderer = TextRenderer.forTarget(e.getPlayer().getUniqueId()).placeholder("prefix", API.get().getConfigManager().getPrefix()).placeholder("player", e.getPlayer().getName())
		        .colorize();

		List<Player> players = new ArrayList<>();

		for(Player online : BukkitLoader.getOnlinePlayers())
			if(online.equals(e.getPlayer()) || online.canSee(e.getPlayer()))
				players.add(online);

		API.get().getMsgManager().sendMessageFromFile(getConfig(), "quit.text", renderer, players);

		for(String command : getConfig().getStringList("quit.commands"))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), renderer.render(PlaceholderAPI.apply(command, e.getPlayer().getUniqueId()), e.getPlayer().getUniqueId()));
	}
}
