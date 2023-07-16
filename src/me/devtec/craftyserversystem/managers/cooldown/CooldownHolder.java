package me.devtec.craftyserversystem.managers.cooldown;

import org.bukkit.command.CommandSender;

public abstract class CooldownHolder {

	private final String id;

	public CooldownHolder(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public abstract boolean accept(CommandSender sender);

}
