package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.events.internal.AfkListener;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Afk extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			if (!AfkListener.isAfk(((Player) sender).getUniqueId()))
				AfkListener.startAfk(((Player) sender).getUniqueId());
			else
				AfkListener.stopAfk(((Player) sender).getUniqueId(), true);
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player target = Bukkit.getPlayer(args[0]);
			if (!AfkListener.isAfk(target.getUniqueId()))
				AfkListener.startAfk(target.getUniqueId());
			else
				AfkListener.stopAfk(target.getUniqueId(), true);
		}).permission(getPerm("other"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
