package me.devtec.craftyserversystem.commands.internal;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.game.Position;

public class Back extends CssCommand {

	private Listener listener;

	@Override
	public void register() {
		if (isRegistered())
			return;

		listener = new Listener() {

			@EventHandler(ignoreCancelled = true)
			public void playerTeleport(PlayerTeleportEvent e) {
				setPositionOf(e.getPlayer().getUniqueId(), TeleportDestination.TELEPORT, Position.fromLocation(e.getFrom()));
			}

			@EventHandler
			public void playerDeath(PlayerDeathEvent e) {
				setPositionOf(e.getPlayer().getUniqueId(), TeleportDestination.DEATH, Position.fromLocation(e.getPlayer().getLocation()));
			}
		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			back(TeleportDestination.LATEST, (Player) sender, true, sender);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			back(TeleportDestination.LATEST, (Player) sender, false, sender);
		});
		cmd.argument("death", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			back(TeleportDestination.DEATH, (Player) sender, true, sender);
		}).permission(getPerm("death")).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			back(TeleportDestination.DEATH, (Player) sender, false, sender);
		});
		cmd.argument("teleport", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			back(TeleportDestination.TELEPORT, (Player) sender, true, sender);
		}).permission(getPerm("teleport")).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			back(TeleportDestination.TELEPORT, (Player) sender, false, sender);
		});
		// other
		CommandStructure<CommandSender> other = cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			back(TeleportDestination.LATEST, Bukkit.getPlayer(args[0]), true, sender);
		}).permission(getPerm("other"));
		// silent
		other.argument("-s", (sender, structure, args) -> {
			back(TeleportDestination.LATEST, Bukkit.getPlayer(args[0]), false, sender);
		});
		other.argument("death", (sender, structure, args) -> {
			back(TeleportDestination.DEATH, Bukkit.getPlayer(args[0]), true, sender);
		}).permission(getPerm("other-death")).argument("-s", (sender, structure, args) -> {
			back(TeleportDestination.DEATH, Bukkit.getPlayer(args[0]), false, sender);
		});
		other.argument("teleport", (sender, structure, args) -> {
			back(TeleportDestination.TELEPORT, Bukkit.getPlayer(args[0]), true, sender);
		}).permission(getPerm("other-teleport")).argument("-s", (sender, structure, args) -> {
			back(TeleportDestination.TELEPORT, Bukkit.getPlayer(args[0]), false, sender);
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	@Override
	public void unregister() {
		super.unregister();
		if (listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

	public void back(TeleportDestination location, Player target, boolean sendMessage, CommandSender sender) {
		if (location == TeleportDestination.LATEST)
			location = getLatestTeleportDestination(target.getUniqueId());
		if (location == null) {
			if (sendMessage)
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("destination",
							TeleportDestination.TELEPORT.displayName());
					msg(sender, "failed.other", placeholders);
				} else
					msg(target, "failed.self", PlaceholdersExecutor.i().add("target", target.getName()).add("destination", TeleportDestination.TELEPORT.displayName()));
			return;
		}
		Position previous = getPositionOf(target.getUniqueId(), location);
		if (previous == null) {
			if (sendMessage)
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("destination", location.displayName());
					msg(sender, "failed.other", placeholders);
				} else
					msg(target, "failed.self", PlaceholdersExecutor.i().add("target", target.getName()).add("destination", location.displayName()));
			return;
		}
		target.teleport(previous.toLocation());
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("destination", location.displayName());
				msg(target, "other.target", placeholders);
				msg(sender, "other.sender", placeholders);
			} else
				msg(target, "self", PlaceholdersExecutor.i().add("target", target.getName()).add("destination", location.displayName()));
	}

	public static Position getPositionOf(UUID uniqueId, TeleportDestination location) {
		return API.getUser(uniqueId).getAs("back." + location.name().toLowerCase(), Position.class);
	}

	public static TeleportDestination getLatestTeleportDestination(UUID uniqueId) {
		String destination = API.getUser(uniqueId).getString("back-destination");
		return destination == null ? null : TeleportDestination.valueOf(destination.toUpperCase());
	}

	public static void setPositionOf(UUID uniqueId, TeleportDestination location, Position pos) {
		API.getUser(uniqueId).set("back." + location.name().toLowerCase(), pos);
		API.getUser(uniqueId).set("back-destination", location.name());
	}

	public static enum TeleportDestination {
		LATEST, TELEPORT, DEATH;

		public String displayName() {
			return me.devtec.craftyserversystem.api.API.get().getConfigManager().getTranslations().getString("back.destination." + name().toLowerCase(), name().toLowerCase());
		}
	}

}
