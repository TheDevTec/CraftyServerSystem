package me.devtec.craftyserversystem.permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.devtec.craftyserversystem.API;
import me.devtec.shared.scheduler.Tasker;
import net.milkbowl.vault.permission.Permission;

public class VaultPermissionHook implements PermissionHook {
	public Permission perm;

	public VaultPermissionHook() {
		new Tasker() {
			@Override
			public void run() {
				if (!API.get().getPermissionHook().equals(VaultPermissionHook.this) || getVault())
					cancel();
			}
		}.runRepeatingTimes(5, 5, 480);
	}

	public boolean getVault() {
		try {
			RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);
			if (provider != null)
				perm = provider.getProvider();
			return perm != null;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String getGroup(CommandSender sender) {
		if (sender instanceof Player)
			return getGroup(sender.getName());
		return "default"; // Console or command block
	}

	@Override
	public String getGroup(String name) {
		if (perm == null)
			return "default";
		String group = perm.getPrimaryGroup(Bukkit.getWorlds().get(0).getName(), Bukkit.getOfflinePlayer(name));
		return group == null ? "default" : group;
	}
}
