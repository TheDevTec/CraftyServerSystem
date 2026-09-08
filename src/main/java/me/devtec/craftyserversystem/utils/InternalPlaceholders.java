package me.devtec.craftyserversystem.utils;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.utility.Animation;
import me.devtec.theapi.bukkit.BukkitLoader;

public class InternalPlaceholders {

	public static TextRenderer generatePlaceholders(Player player) {
		Location loc = player.getLocation();

		TextRenderer renderer = TextRenderer.forTarget(player.getUniqueId())
				.placeholder("prefix", API.get().getConfigManager().getPrefix()).placeholder("player", player.getName())
				.placeholder("tps", BukkitLoader.getNmsProvider().getServerTPS()[0])
				.placeholder("ping", BukkitLoader.getNmsProvider().getPing(player))
				.placeholder("online", countNonVanishPlayers(player)).placeholder("max_players", Bukkit.getMaxPlayers())
				.placeholder("balance",
						API.get().getEconomyHook().format(
								API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName())))
				.placeholder("money",
						API.get().getEconomyHook().format(
								API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName())))
				.placeholder("health", player.getHealth()).placeholder("food", player.getFoodLevel())
				.placeholder("x", loc.getX()).placeholder("y", loc.getY()).placeholder("z", loc.getZ())
				.placeholder("pos_x", loc.getBlockX()).placeholder("pos_y", loc.getBlockY())
				.placeholder("pos_z", loc.getBlockZ()).placeholder("world", loc.getWorld().getName()).colorize();

		for (Entry<String, Animation> entry : API.get().getAnimationManager().getRegistered().entrySet())
			renderer.placeholder("animation:" + entry.getKey(), entry.getValue().get());

		return renderer;
	}

	private static int countNonVanishPlayers(Player player) {
		int count = 0;
		for (Player online : BukkitLoader.getOnlinePlayers())
			if (online.equals(player) || player.canSee(online))
				++count;
		return count;
	}
}
