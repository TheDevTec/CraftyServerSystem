package me.devtec.craftyserversystem.events;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

public class QuitListener implements Listener {

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		e.setQuitMessage(null); // Remove message

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());
		// Send json message
		Config quit = API.get().getConfigManager().getQuit();
		API.get().getMsgManager().sendMessageFromFile(quit, "quit.text", placeholders, BukkitLoader.getOnlinePlayers());
		API.get().getMsgManager().sendMessageFromFile(quit, "quit.broadcast", placeholders, BukkitLoader.getOnlinePlayers());
		for (String cmd : placeholders.apply(quit.getStringList("quit.commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}
}
