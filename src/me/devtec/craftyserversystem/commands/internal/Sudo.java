package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;

public class Sudo implements CssCommand {

	private CommandHolder<CommandSender> cmd;

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
			if (value.charAt(0) == '/')
				Bukkit.dispatchCommand(target, value.substring(1));
			else
				target.chat(value);
			msg(sender, "", PlaceholdersExecutor.i().add("target", target.getName()).add("value", value));
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
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
