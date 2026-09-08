package me.devtec.craftyserversystem.events.internal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.text.TextRenderer;
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

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerSpawnLocationEvent e) {
		if (getConfig().getBoolean("force-spawn-location")
				|| me.devtec.shared.API.offlineCache().lookupQuery(e.getPlayer().getUniqueId()) == null)
			e.setSpawnLocation(API.get().getConfigManager().getSpawn().toLocation());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent e) {
		String time = e.getPlayer().hasPlayedBefore() ? "normal" : "first";
		e.setJoinMessage(null);

		TextRenderer renderer = TextRenderer.forTarget(e.getPlayer().getUniqueId())
				.placeholder("prefix", API.get().getConfigManager().getPrefix())
				.placeholder("player", e.getPlayer().getName()).colorize();

		List<Player> players = new ArrayList<>();

		for (Player online : BukkitLoader.getOnlinePlayers())
			if (online.equals(e.getPlayer()) || online.canSee(e.getPlayer()))
				players.add(online);

		API.get().getMsgManager().sendMessageFromFile(getConfig(), "join." + time + ".text", renderer, players);

		API.get().getMsgManager().sendMessageFromFile(getConfig(), "join." + time + ".messages", renderer,
				e.getPlayer());

		for (String command : getConfig().getStringList("join." + time + ".commands"))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), renderer
					.render(PlaceholderAPI.apply(command, e.getPlayer().getUniqueId()), e.getPlayer().getUniqueId()));
	}
}
