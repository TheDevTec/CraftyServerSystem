package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.warp.WarpManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;

public class DelWarp extends CssCommand {

	@Override
	public String section() {
		return "delwarp";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).callableArgument((sender, structure, args) -> StringUtils.copyPartialMatches(args[0], WarpManager.getProvider().getWarps()), (sender, structure, args) -> {
			delWarp(args[0].toLowerCase(), sender);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void delWarp(String warpName, CommandSender sender) {
		if (WarpManager.getProvider().delete(warpName)) {
			PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("warp", warpName);
			msg(sender, "deleted", placeholders);
		}
	}

}
