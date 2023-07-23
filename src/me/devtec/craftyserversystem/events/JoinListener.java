package me.devtec.craftyserversystem.events;

import org.bukkit.Bukkit;
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
		String time = e.getPlayer().hasPlayedBefore() ? "next" : "first";
		e.setJoinMessage(null); // Remove message

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());
		// Send json message
		Config join = API.get().getConfigManager().getJoin();
		API.get().getMsgManager().sendMessageFromFile(join, "join." + time + ".text", placeholders, BukkitLoader.getOnlinePlayers());
		API.get().getMsgManager().sendMessageFromFile(join, "join." + time + ".broadcast", placeholders, BukkitLoader.getOnlinePlayers());
		for (String cmd : placeholders.apply(join.getStringList("join." + time + ".commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}
}
