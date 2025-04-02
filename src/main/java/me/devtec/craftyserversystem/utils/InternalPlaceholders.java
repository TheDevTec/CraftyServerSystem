package me.devtec.craftyserversystem.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.theapi.bukkit.BukkitLoader;

public class InternalPlaceholders {

	public static PlaceholdersExecutor generatePlaceholders(Player player) {
		Location loc = player.getLocation();
		return PlaceholdersExecutor.i().papi(player.getUniqueId()).add("player", player.getName())
				.add("tps", BukkitLoader.getNmsProvider().getServerTPS()[0])
				.add("ping", BukkitLoader.getNmsProvider().getPing(player)).add("online", countNonVanishPlayers(player))
				.add("max_players", Bukkit.getMaxPlayers())
				.add("balance",
						API.get().getEconomyHook().format(
								API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName())))
				.add("money",
						API.get().getEconomyHook().format(
								API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName())))
				.add("health", player.getHealth()).add("food", player.getFoodLevel()).add("x", loc.getX())
				.add("y", loc.getY()).add("z", loc.getZ()).add("pos_x", loc.getBlockX()).add("pos_y", loc.getBlockY())
				.add("pos_z", loc.getBlockZ()).add("world", loc.getWorld().getName());
	}

	private static int countNonVanishPlayers(Player player) {
		int count = 0;
		for (Player online : BukkitLoader.getOnlinePlayers())
			if (online.equals(player) || player.canSee(online))
				++count;
		return count;
	}
}
