package me.devtec.craftyserversystem.permission;

import org.bukkit.command.CommandSender;

public interface PermissionHook {
	public String getGroup(CommandSender sender);

	public String getGroup(String name);
}
