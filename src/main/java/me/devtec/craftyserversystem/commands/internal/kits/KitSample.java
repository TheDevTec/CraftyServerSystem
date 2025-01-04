package me.devtec.craftyserversystem.commands.internal.kits;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.TimeUtils;

@IgnoredClass
public class KitSample {

	private final String name;
	private List<ItemStack> contents = new ArrayList<>();
	private List<String> commands = new ArrayList<>();
	private List<String> messages = new ArrayList<>();
	private double cost;
	private boolean overrideContents;
	private boolean dropItems;
	private CooldownHolder cooldown;
	private String permission;

	public KitSample(@Nonnull String name) {
		this.name = name;

		String bypassPerm = API.get().getConfigManager().getKits().getString(name + ".settings.cooldown.bypass-perm");
		if (bypassPerm == null || bypassPerm.trim().isEmpty())
			bypassPerm = null;
		cooldown = new CooldownHolder("kit." + name) {

			@Override
			public boolean accept(CommandSender sender) {
				if (getBypassPerm() != null && sender.hasPermission(getBypassPerm()))
					return true; // Skip whole cooldown checker

				long currentTime = System.currentTimeMillis() / 1000;
				Config file = me.devtec.shared.API.getUser(sender.getName());
				long lastUsedTime = file.getLong("css.cd." + id());
				long nextUsageIn = lastUsedTime - currentTime;
				if (nextUsageIn <= 0) {
					file.set("css.cd." + id(), currentTime + getTime());
					return true;
				}
				return false;
			}

			@Override
			public boolean tryWithoutWriting(CommandSender sender) {
				return remainingTime(sender) <= 0;
			}

			@Override
			public long remainingTime(CommandSender sender) {
				if (getBypassPerm() != null && sender.hasPermission(getBypassPerm()))
					return 0; // Skip whole cooldown checker

				long currentTime = System.currentTimeMillis() / 1000;
				Config file = me.devtec.shared.API.getUser(sender.getName());
				long lastUsedTime = file.getLong("css.cd." + id());
				long nextUsageIn = lastUsedTime - currentTime;
				return Math.max(0, nextUsageIn);
			}
		};
		cooldown.setBypassPerm(bypassPerm);
		cooldown.setTime(TimeUtils.timeFromString(API.get().getConfigManager().getKits().getString(name + ".settings.cooldown.time", "0")));
		API.get().getCooldownManager().register(cooldown);
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public List<ItemStack> getContents() {
		return contents;
	}

	public void setContents(@Nonnull List<ItemStack> contents) {
		this.contents = contents;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public boolean isOverrideContents() {
		return overrideContents;
	}

	public void setOverrideContents(boolean overrideContents) {
		this.overrideContents = overrideContents;
	}

	public boolean isDropItems() {
		return dropItems;
	}

	public void setDropItems(boolean dropItems) {
		this.dropItems = dropItems;
	}

	public CooldownHolder getCooldown() {
		return cooldown;
	}

	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}

	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getPermission() {
		return permission;
	}
}
