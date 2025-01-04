package me.devtec.craftyserversystem.permission;

import org.bukkit.command.CommandSender;

public class EmptyPermissionHook implements PermissionHook {

	public EmptyPermissionHook() {
	}

	@Override
	public String getGroup(CommandSender sender) {
		return "default";
	}

	@Override
	public String getGroup(String name) {
		return "default";
	}
}
