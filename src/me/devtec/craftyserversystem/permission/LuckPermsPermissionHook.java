package me.devtec.craftyserversystem.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class LuckPermsPermissionHook implements PermissionHook {
	public LuckPerms luckPerms;

	public LuckPermsPermissionHook() {
		luckPerms = LuckPermsProvider.get();
	}

	@Override
	public String getGroup(CommandSender sender) {
		if (sender instanceof Player)
			return getGroup(sender.getName());
		return "default"; // Console or command block
	}

	@Override
	public String getGroup(String name) {
		if (luckPerms == null)
			return "default";
		String group = luckPerms.getUserManager().getUser(name).getPrimaryGroup();
		return group == null ? "default" : group;
	}
}
