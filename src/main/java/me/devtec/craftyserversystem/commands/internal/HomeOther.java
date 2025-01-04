package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.Position;

public class HomeOther extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, (sender, structure, args) -> msgUsage(sender, "cmd"), (sender, structure, args) -> {
			List<String> players = new ArrayList<>();
			players.add("{offlinePlayer}");
			for (Player player : BukkitLoader.getOnlinePlayers())
				players.add(player.getName());
			return players;
		});
		// home
		cmd.callableArgument((sender, structure, args) -> HomeManager.get().getHomes(args[0]), 1, (sender, structure, args) -> {
			String home = args[1].toLowerCase();
			Position pos = HomeManager.get().getHomePosition(args[0], home);
			sender.teleport(pos.toLocation());
			msg(sender, "teleport", PlaceholdersExecutor.i().add("target", API.offlineCache().lookupName(args[0])).add("home", home));
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
