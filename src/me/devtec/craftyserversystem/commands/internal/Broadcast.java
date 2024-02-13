package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Broadcast extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		cmd.argument(null, -1, (sender, structure, args) -> {
			List<CommandSender> all = new ArrayList<>(BukkitLoader.getOnlinePlayers());
			all.add(Bukkit.getConsoleSender());
			API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "broadcast",
					PlaceholdersExecutor.i().add("sender", sender.getName()).add("message", StringUtils.buildString(args)), all);
		}, (sender, structure, args) -> Arrays.asList("{message}"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
