package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Kick extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;
		BanAPI.init();

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).argument(null, (sender, structure, args) -> {
			String player = args[0];
			String reason = null;
			BanAPI.kick(player, sender.getName(), reason);
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				list.add(player.getName());
			list.add("{offlinePlayer}");
			return list;
		}).argument(null, (sender, structure, args) -> {
			String player = args[0];
			String reason = StringUtils.buildString(1, args);
			BanAPI.kick(player, sender.getName(), reason);
		}, (sender, structure, args) -> Arrays.asList("{reason}")).permission(getPerm("cmd"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
