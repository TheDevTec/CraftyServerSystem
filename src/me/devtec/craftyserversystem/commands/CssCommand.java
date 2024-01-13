package me.devtec.craftyserversystem.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.annotations.Nonnull;
import me.devtec.craftyserversystem.annotations.Nullable;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.manager.PermissionChecker;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public abstract class CssCommand {

	public static final PermissionChecker<CommandSender> DEFAULT_PERMS_CHECKER = (sender, permission, tablist) -> {
		if (tablist)
			return sender.hasPermission(permission);
		if (sender.hasPermission(permission))
			return true;
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "other.no-perms", PlaceholdersExecutor.i().add("permission", permission), sender);
		return false;
	};

	public static final PermissionChecker<Player> P_DEFAULT_PERMS_CHECKER = (sender, permission, tablist) -> {
		if (tablist)
			return sender.hasPermission(permission);
		if (sender.hasPermission(permission))
			return true;
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "other.no-perms", PlaceholdersExecutor.i().add("permission", permission), sender);
		return false;
	};

	@Nonnull
	protected CommandHolder<? extends CommandSender> cmd;

	@Nonnull
	public String section() {
		return getClass().getSimpleName().toLowerCase();
	}

	public abstract void register();

	public void unregister() {
		if (!isRegistered())
			return;
		cmd.unregister();
		cmd = null;
	}

	public boolean isRegistered() {
		return cmd != null;
	}

	@Nonnull
	public List<String> getCommands() {
		return API.get().getConfigManager().getCommands().getStringList(section() + ".cmd");
	}

	@Nonnull
	public <T> CommandStructure<T> addBypassSettings(CommandStructure<T> cmd) {
		String cdGroup = API.get().getConfigManager().getCommands().getString(section() + ".cooldown");
		if (cdGroup != null) {
			CooldownHolder cdHolder = API.get().getCooldownManager().getOrPrepare(cdGroup);
			if (cdHolder != null)
				cmd.first().cooldownDetection((sender, structure, args) -> !cdHolder.accept((CommandSender) sender));
		}
		return cmd;
	}

	public boolean perm(CommandSender sender, String path) {
		String perm = getPerm(path);
		return perm == null ? true : sender.hasPermission(perm);
	}

	@Nullable
	public String getPerm(String path) {
		return API.get().getConfigManager().getCommands().getString(section() + ".perms." + path);
	}

	public void msg(CommandSender sender, String path) {
		msg(sender, path, PlaceholdersExecutor.EMPTY);
	}

	public void msg(CommandSender sender, String path, PlaceholdersExecutor ex) {
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), section() + (path.isEmpty() ? "" : "." + path), ex, sender);
	}

	public void msgOut(CommandSender sender, String path) {
		msgOut(sender, path, PlaceholdersExecutor.EMPTY);
	}

	public void msgOut(CommandSender sender, String path, PlaceholdersExecutor ex) {
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), path, ex, sender);
	}

	public void msgUsage(CommandSender sender, String path) {
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getCommands(), section() + ".usage." + path, PlaceholdersExecutor.EMPTY, sender);
	}

	@Nonnull
	public Collection<? extends Player> selector(CommandSender sender, String selector) {
		char lowerCase = selector.length() == 1 && selector.charAt(0) == '*' ? '*' : selector.length() == 2 && selector.charAt(0) == '@' ? Character.toLowerCase(selector.charAt(1)) : 0;
		if (lowerCase != 0)
			switch (lowerCase) {
			case 'a':
			case 'e':
			case '*':
				return BukkitLoader.getOnlinePlayers();
			case 'r':
				return Collections.singleton(StringUtils.randomFromCollection(BukkitLoader.getOnlinePlayers()));
			case 's':
			case 'p':
				Location pos = null;
				if (sender instanceof Player)
					pos = ((Player) sender).getLocation();
				else if (sender instanceof BlockCommandSender)
					pos = ((BlockCommandSender) sender).getBlock().getLocation();
				else
					pos = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
				double distance = -1;
				Player nearestPlayer = null;
				for (Player sameWorld : pos.getWorld().getPlayers()) {
					double distanceRange = sameWorld.getLocation().distance(pos);
					if (distance == -1 || distance < distanceRange) {
						distance = distanceRange;
						nearestPlayer = sameWorld;
					}
				}
				Collection<? extends Player> players = BukkitLoader.getOnlinePlayers();
				return players.isEmpty() ? Collections.emptyList() : Collections.singleton(nearestPlayer == null ? players.iterator().next() : nearestPlayer);
			}
		Player target = Bukkit.getPlayer(selector);
		return target == null ? Collections.emptyList() : Collections.singleton(target);
	}
}
