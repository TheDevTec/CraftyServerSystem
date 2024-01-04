package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class PlayerTime extends CssCommand {

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
			setDay(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setDay(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setDay(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setDay(sender, player, false);
		});

		cmd.argument("noon", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNoon(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNoon(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setNoon(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setNoon(sender, player, false);
		});

		cmd.argument("night", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNight(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNight(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setNight(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setNight(sender, player, false);
		});

		cmd.argument("midnight", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setMidnight(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setMidnight(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setMidnight(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setMidnight(sender, player, false);
		});

		cmd.argument("reset", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			reset(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			reset(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				reset(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				reset(sender, player, false);
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void reset(CommandSender sender, Player target, boolean sendMessages) {
		target.resetPlayerTime();
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playtime-reset.other.sender", PLACEHOLDERS);
				msgOut(target, "playtime-reset.other.target", PLACEHOLDERS);
			} else
				msgOut(target, "playtime-reset.self", PlaceholdersExecutor.EMPTY);
	}

	private void setDay(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerTime(1000, false);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playerday.other.sender", PLACEHOLDERS);
				msgOut(target, "playerday.other.target", PLACEHOLDERS);
			} else
				msgOut(sender, "playerday.self", PlaceholdersExecutor.EMPTY);
	}

	private void setNoon(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerTime(6000, false);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playernoon.other.sender", PLACEHOLDERS);
				msgOut(target, "playernoon.other.target", PLACEHOLDERS);
			} else
				msgOut(sender, "playernoon.self", PlaceholdersExecutor.EMPTY);
	}

	private void setNight(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerTime(13000, false);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playernight.other.sender", PLACEHOLDERS);
				msgOut(target, "playernight.other.target", PLACEHOLDERS);
			} else
				msgOut(sender, "playernight.self", PlaceholdersExecutor.EMPTY);
	}

	private void setMidnight(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerTime(18000, false);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playermidnight.other.sender", PLACEHOLDERS);
				msgOut(target, "playermidnight.other.target", PLACEHOLDERS);
			} else
				msgOut(sender, "playermidnight.self", PlaceholdersExecutor.EMPTY);
	}

}
