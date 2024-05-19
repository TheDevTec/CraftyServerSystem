package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Unban extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;
		BanAPI.init();

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, (sender, structure, args) -> {
			String player = args[0];
			boolean modified = false;
			for (Entry entry : BanAPI.getActivePunishments(player, null)) {
				if (entry.getType() != BanType.BAN)
					continue;
				entry.setCancelled(true);
				BanAPI.saveModifiedEntry(entry);
				modified = true;
			}
			if (modified)
				msg(sender, "success", PlaceholdersExecutor.i().add("user", player));
			else
				msg(sender, "failed", PlaceholdersExecutor.i().add("user", player));
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				list.add(player.getName());
			list.add("{offlinePlayer}");
			return list;
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
