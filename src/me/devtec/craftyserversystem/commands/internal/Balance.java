package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Balance extends CssCommand {

	@Override
	public String section() {
		return "balance";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			msg(sender, "self", PlaceholdersExecutor.i().add("balance", API.get().getEconomyHook().getBalance(sender.getName(), ((Player) sender).getWorld().getName())));
		}).permission(getPerm("cmd"));
		// other
		cmd.argument(null, 1, (sender, structure, args) -> {
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[0]);
			if (query != null) {
				World world = null;
				if (sender instanceof Player)
					world = ((Player) sender).getWorld();
				else if (sender instanceof BlockCommandSender)
					world = ((BlockCommandSender) sender).getBlock().getWorld();
				else
					world = Bukkit.getWorlds().get(0);
				msg(sender, "other", PlaceholdersExecutor.i().add("target", query.getName()).add("balance", API.get().getEconomyHook().getBalance(query.getName(), world.getName())));
			} else
				msg(sender, "no-account", PlaceholdersExecutor.i().add("target", args[0]));
		}, (sender, structure, args) -> {
			List<String> players = new ArrayList<>();
			players.add("{offlinePlayer}");
			for (Player player : BukkitLoader.getOnlinePlayers())
				players.add(player.getName());
			return players;
		}).permission(getPerm("other"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
