package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Time extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "usage");
		}).permission(getPerm("cmd"));

		cmd.argument("day", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setDay(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setDay(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setDay(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setDay(sender, Bukkit.getWorld(args[1]), false);
		});

		cmd.argument("noon", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNoon(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNoon(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setNoon(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setNoon(sender, Bukkit.getWorld(args[1]), false);
		});

		cmd.argument("night", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNight(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNight(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setNight(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setNight(sender, Bukkit.getWorld(args[1]), false);
		});

		cmd.argument("midnight", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setMidnight(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setMidnight(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setMidnight(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setMidnight(sender, Bukkit.getWorld(args[1]), false);
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void setDay(CommandSender sender, World target, boolean sendMessage) {
		target.setTime(1000);
		if (sendMessage)
			msgOut(sender, "day", PlaceholdersExecutor.i().add("world", target.getName()));
	}

	private void setNoon(CommandSender sender, World target, boolean sendMessage) {
		target.setTime(6000);
		if (sendMessage)
			msgOut(sender, "noon", PlaceholdersExecutor.i().add("world", target.getName()));
	}

	private void setNight(CommandSender sender, World target, boolean sendMessage) {
		target.setTime(13000);
		if (sendMessage)
			msgOut(sender, "night", PlaceholdersExecutor.i().add("world", target.getName()));
	}

	private void setMidnight(CommandSender sender, World target, boolean sendMessage) {
		target.setTime(18000);
		if (sendMessage)
			msgOut(sender, "midnight", PlaceholdersExecutor.i().add("world", target.getName()));
	}

}
