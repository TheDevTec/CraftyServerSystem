package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.warp.WarpInfo;
import me.devtec.craftyserversystem.commands.internal.warp.WarpManager;
import me.devtec.craftyserversystem.commands.internal.warp.WarpResult;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;

public class Warp implements CssCommand {

	private CommandHolder<CommandSender> cmd;

	@Override
	public String section() {
		return "warp";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).callableArgument((sender, structure, args) -> StringUtils.copyPartialMatches(args[0], WarpManager.getProvider().getWarps()), (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), true, false, sender);
		});
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), false, false, sender);
		});
		// instant
		cmd.argument("-i", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), true, true, sender);
		}).permission(getPerm("instant"));

		// silent & instant
		cmd.argument("-si", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), false, true, sender);
		}).permission(getPerm("instant"));

		// other
		cmd = cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				warp(player, args[0].toLowerCase(), true, false, sender);
		}).permission(getPerm("other"));

		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				warp(player, args[0].toLowerCase(), false, false, sender);
		});
		// instant
		cmd.argument("-i", (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				warp(player, args[0].toLowerCase(), true, true, sender);
		}).permission(getPerm("other-instant"));

		// silent & instant
		cmd.argument("-si", (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				warp(player, args[0].toLowerCase(), false, true, sender);
		}).permission(getPerm("other-instant"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void warp(Player target, String warpName, boolean sendMessage, boolean instant, CommandSender sender) {
		WarpInfo warp = WarpManager.getProvider().get(warpName);
		WarpResult result = warp.warp(target, instant);
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("admin", sender.getName()).add("target", target.getName()).add("warp", warpName).add("cost", warp.getCost())
						.add("permission", warp.getPermission() + "");
				switch (result) {
				case FAILED_NO_MONEY:
					msg(target, "other.failed.money.target", placeholders);
					msg(sender, "other.failed.money.admin", placeholders);
					break;
				case FAILED_NO_PERMISSION:
					msg(target, "other.failed.perm.target", placeholders);
					msg(sender, "other.failed.perm.admin", placeholders);
					break;
				case SUCCESS:
					msg(target, "other.success.target", placeholders);
					msg(sender, "other.success.admin", placeholders);
					break;
				}
			} else {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("warp", warpName).add("cost", warp.getCost()).add("permission",
						warp.getPermission() + "");
				switch (result) {
				case FAILED_NO_MONEY:
					msg(target, "self.failed.money", placeholders);
					break;
				case FAILED_NO_PERMISSION:
					msg(target, "self.failed.perm", placeholders);
					break;
				case SUCCESS:
					msg(target, "self.success", placeholders);
					break;
				}
			}
	}

	@Override
	public void unregister() {
		if (!isRegistered())
			return;
		cmd.unregister();
		cmd = null;
	}

	@Override
	public boolean isRegistered() {
		return cmd != null;
	}

}
