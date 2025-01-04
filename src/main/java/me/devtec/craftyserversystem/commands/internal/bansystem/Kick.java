package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Kick extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, (sender, structure, args) -> {
			String player = args[0];
			String reason = null;
			API.get().getCommandsAPI().getBanAPI().kick(player, sender.getName(), reason);
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			if (API.get().getConfigManager().getMain().getBoolean("bansystem.tab-completer-list-player-ips"))
				for (Player player : BukkitLoader.getOnlinePlayers())
					list.add(player.getAddress().getAddress().getHostAddress());
			else
				for (Player player : BukkitLoader.getOnlinePlayers())
					list.add(player.getName());
			list.add("{offlinePlayer}");
			list.add("{ip}");
			return list;
		}).argument(null, (sender, structure, args) -> {
			String player = args[0];
			String reason = StringUtils.buildString(1, args);
			API.get().getCommandsAPI().getBanAPI().kick(player, sender.getName(), reason);
		}, (sender, structure, args) -> API.get().getConfigManager().getMain().getStringList("bansystem.tab-completer-reasons"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
