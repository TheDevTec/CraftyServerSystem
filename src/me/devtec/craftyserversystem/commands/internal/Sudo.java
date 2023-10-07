package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Sudo extends CssCommand {

	@Override
	public String section() {
		return "sudo";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).argument(null, -1, (sender, structure, args) -> {
			Player target = Bukkit.getPlayer(args[0]);
			String value = StringUtils.buildString(1, args);
			boolean silent = value.endsWith("-s");
			boolean command = value.charAt(0) == '/';
			if (silent)
				value = value.substring(command ? 1 : 0, value.length() - 2).trim();
			else if (command)
				value = value.substring(1);

			final String finalValue = value;

			// It's async!!
			BukkitLoader.getNmsProvider().postToMainThread(() -> {
				if (command)
					Bukkit.dispatchCommand(target, finalValue);
				else
					target.chat(finalValue);
			});
			if (!silent)
				msg(sender, "", PlaceholdersExecutor.i().add("target", target.getName()).add("value", value));
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
