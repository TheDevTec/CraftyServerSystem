package me.devtec.craftyserversystem.events;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

public class JoinListener implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		String time = e.getPlayer().hasPlayedBefore() ? "normal" : "first";
		e.setJoinMessage(null); // Remove message

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());
		// Send json message
		Config join = API.get().getConfigManager().getJoin();
		Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
		API.get().getMsgManager().sendMessageFromFile(join, "join." + time + ".text", placeholders, onlinePlayers);
		API.get().getMsgManager().sendMessageFromFile(join, "join." + time + ".broadcast", placeholders, onlinePlayers);
		for (String cmd : placeholders.apply(join.getStringList("join." + time + ".commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}
}
