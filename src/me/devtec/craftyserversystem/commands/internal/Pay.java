package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Pay extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// other
		cmd.argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}, (sender, structure, args) -> {
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size() + 1);
			players.add("{offlinePlayer}");
			for (Player player : onlinePlayers)
				players.add(player.getName());
			players.remove(sender.getName());
			return players;
		}).argument(null, 1, (sender, structure, args) -> {
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[0]);
			if (query != null) {
				if (query.getUUID().equals(sender.getUniqueId())) {
					msg(sender, "failed.self", PlaceholdersExecutor.EMPTY);
					return;
				}
				World world = sender.getWorld();
				double money = Economy.multipleByMoneyFormat(ParseUtils.getDouble(args[1]), args[1]);
				if (pay(sender.getName(), query.getName(), world.getName(), money))
					msg(sender, "success.sender",
							PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", query.getName()).add("balance", StringUtils.formatDouble(FormatType.COMPLEX, money)));
				else
					msg(sender, "failed.money", PlaceholdersExecutor.i().add("target", query.getName()).add("balance", StringUtils.formatDouble(FormatType.COMPLEX, money)));
			} else
				msg(sender, "no-account", PlaceholdersExecutor.i().add("target", args[0]));
		}, (sender, structure, args) -> {
			List<String> tabCompleter = new ArrayList<>();
			if (args[1].isEmpty()) {
				tabCompleter.add("1k");
				tabCompleter.add("100");
			} else {
				if (Character.isDigit(args[1].charAt(args[1].length() - 1)))
					tabCompleter.add(args[1] + "k");
				tabCompleter.add(args[1]);
			}
			return tabCompleter;
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public boolean pay(String player, String target, String world, double balance) {
		EconomyHook hook = API.get().getEconomyHook();
		if (hook.has(player, world, balance)) {
			hook.withdraw(player, world, balance);
			hook.deposit(target, world, balance);
			return true;
		}
		return false;
	}
}
