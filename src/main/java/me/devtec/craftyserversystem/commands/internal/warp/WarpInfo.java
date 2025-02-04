package me.devtec.craftyserversystem.commands.internal.warp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.Position;

@IgnoredClass
public class WarpInfo {
	private Position position;
	private double cost;
	private ItemStack icon;
	private String permission;
	private CooldownHolder cooldown;

	public WarpInfo(Position position) {
		this.position = position;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public ItemStack getIcon() {
		return icon;
	}

	public void setIcon(ItemStack icon) {
		this.icon = icon;
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public CooldownHolder getCooldown() {
		return cooldown;
	}

	public void setCooldown(CooldownHolder cooldown) {
		this.cooldown = cooldown;
	}

	public WarpResult warp(Player player, boolean instant) {
		if (instant || cost <= 0 && permission == null) {
			if (Bukkit.isPrimaryThread())
				player.teleport(position.toLocation());
			else
				BukkitLoader.getNmsProvider().postToMainThread(() -> player.teleport(position.toLocation()));
			return WarpResult.SUCCESS;
		}
		if (permission != null)
			if (!player.hasPermission(permission))
				return WarpResult.FAILED_NO_PERMISSION;
		if (cost > 0)
			if (!API.get().getEconomyHook().has(player.getName(), player.getWorld().getName(), cost))
				return WarpResult.FAILED_NO_MONEY;
			else
				API.get().getEconomyHook().withdraw(player.getName(), player.getWorld().getName(), cost);
		if (Bukkit.isPrimaryThread())
			player.teleport(position.toLocation());
		else
			BukkitLoader.getNmsProvider().postToMainThread(() -> player.teleport(position.toLocation()));
		return WarpResult.SUCCESS;
	}

	public boolean isValid() {
		return position!=null && position.getWorld() != null;
	}
}
