package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.afk.AfkManager;
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
			if (!AfkManager.getProvider().isAfk(((Player) sender).getUniqueId()))
				AfkManager.getProvider().startAfk(((Player) sender).getUniqueId(), true);
			else
				AfkManager.getProvider().stopAfk(((Player) sender).getUniqueId(), true);
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player target = Bukkit.getPlayer(args[0]);
			if (!AfkManager.getProvider().isAfk(target.getUniqueId()))
				AfkManager.getProvider().startAfk(target.getUniqueId(), true);
			else
				AfkManager.getProvider().stopAfk(target.getUniqueId(), true);
		}).permission(getPerm("other"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
