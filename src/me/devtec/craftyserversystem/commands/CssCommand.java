package me.devtec.craftyserversystem.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.manager.PermissionChecker;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public interface CssCommand {

	public static final PermissionChecker<CommandSender> DEFAULT_PERMS_CHECKER = (sender, permission, tablist) -> {
		if (tablist)
			return sender.hasPermission(permission);
		if (sender.hasPermission(permission))
			return true;
		API.get().getMsgManager().sendMessageTrans("other.no-perms", PlaceholdersExecutor.i().add("permission", permission), sender);
		return false;
	};

	public static final PermissionChecker<Player> P_DEFAULT_PERMS_CHECKER = (sender, permission, tablist) -> {
		if (tablist)
			return sender.hasPermission(permission);
		if (sender.hasPermission(permission))
			return true;
		API.get().getMsgManager().sendMessageTrans("other.no-perms", PlaceholdersExecutor.i().add("permission", permission), sender);
		return false;
	};

	String section();

	void register();

	void unregister();

	boolean isRegistered();

	default List<String> getCommands() {
		return API.get().getConfigManager().getCommands().getStringList(section() + ".cmd");
	}

	default <T> CommandStructure<T> addBypassSettings(CommandStructure<T> cmd) {
		String cdGroup = API.get().getConfigManager().getCommands().getString(section() + ".cooldown");
		if (cdGroup != null) {
			CooldownHolder cdHolder = API.get().getCooldownManager().getOrPrepare(cdGroup);
			if (cdHolder != null)
				cmd.first().cooldownDetection((sender, structure, args) -> !cdHolder.accept((CommandSender) sender));
		}
		return cmd;
	}

	default boolean perm(CommandSender sender, String path) {
		return sender.hasPermission(getPerm(path));
	}

	default String getPerm(String path) {
		return API.get().getConfigManager().getCommands().getString(section() + ".perms." + path);
	}

	default void msg(CommandSender sender, String path) {
		msg(sender, path, PlaceholdersExecutor.EMPTY);
	}

	default void msg(CommandSender sender, String path, PlaceholdersExecutor ex) {
		API.get().getMsgManager().sendMessageTrans(section() + (path.isEmpty() ? "" : "." + path), ex, sender);
	}

	default void msgOut(CommandSender sender, String path) {
		msgOut(sender, path, PlaceholdersExecutor.EMPTY);
	}

	default void msgOut(CommandSender sender, String path, PlaceholdersExecutor ex) {
		API.get().getMsgManager().sendMessageTrans(path, ex, sender);
	}

	default void msgUsage(CommandSender sender, String path) {
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getCommands(), section() + ".usage." + path, PlaceholdersExecutor.EMPTY, sender);
	}

	default Collection<? extends Player> selector(CommandSender sender, String selector) {
		char lowerCase = selector.charAt(0) == '*' ? '*' : selector.charAt(0) == '@' && selector.length() == 2 ? Character.toLowerCase(selector.charAt(1)) : 0;
		if (lowerCase != 0)
			switch (lowerCase) {
			case 'a':
			case 'e':
			case '*':
				return BukkitLoader.getOnlinePlayers();
			case 'r':
				return Arrays.asList(StringUtils.randomFromCollection(BukkitLoader.getOnlinePlayers()));
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
				for (Player sameWorld : pos.getWorld().getPlayers())
					if (distance == -1 || distance < sameWorld.getLocation().distance(pos)) {
						distance = sameWorld.getLocation().distance(pos);
						nearestPlayer = sameWorld;
					}
				return BukkitLoader.getOnlinePlayers().isEmpty() ? Collections.emptyList() : Arrays.asList(nearestPlayer == null ? BukkitLoader.getOnlinePlayers().iterator().next() : nearestPlayer);
			}
		Player target = Bukkit.getPlayer(selector);
		return target == null ? Collections.emptyList() : Arrays.asList(target);
	}
}
