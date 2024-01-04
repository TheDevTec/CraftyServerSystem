package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;

public class DelHome extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// home
		cmd.callableArgument((sender, structure, args) -> HomeManager.get().getHomes(sender.getName()), 1, (sender, structure, args) -> {
			HomeManager.get().delHome(sender.getName(), args[0].toLowerCase());
			msg(sender, "del", PlaceholdersExecutor.i().add("home", args[0].toLowerCase()));
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
