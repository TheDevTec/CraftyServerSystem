package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Collection;
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
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Economy extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));

		// add
		cmd.argument("add", (sender, structure, args) -> {
			msgUsage(sender, "add");
		}).permission(getPerm("add")).argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "add");
		}, (sender, structure, args) -> {
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size() + 1);
			players.add("{offlinePlayer}");
			for (Player player : onlinePlayers)
				players.add(player.getName());
			return players;
		}).argument(null, 1, (sender, structure, args) -> {
			double value = multipleByMoneyFormat(ParseUtils.getDouble(args[2]), args[2]);
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[1]);
			if (query != null) {
				World world = null;
				if (sender instanceof Player)
					world = ((Player) sender).getWorld();
				else if (sender instanceof BlockCommandSender)
					world = ((BlockCommandSender) sender).getBlock().getWorld();
				else
					world = Bukkit.getWorlds().get(0);
				API.get().getEconomyHook().deposit(query.getName(), world.getName(), value);
				msg(sender, "add", PlaceholdersExecutor.i().add("target", query.getName()).add("balance", StringUtils.formatDouble(FormatType.COMPLEX, value)));
			} else
				msg(sender, "no-account", PlaceholdersExecutor.i().add("target", args[1]));
		}, (sender, structure, args) -> {
			List<String> tabCompleter = new ArrayList<>();
			if (args[2].isEmpty()) {
				tabCompleter.add("1k");
				tabCompleter.add("100");
			} else {
				if (Character.isDigit(args[2].charAt(args[2].length() - 1)))
					tabCompleter.add(args[2] + "k");
				tabCompleter.add(args[2]);
			}
			return tabCompleter;
		});

		// remove
		cmd.argument("remove", (sender, structure, args) -> {
			msgUsage(sender, "remove");
		}).permission(getPerm("remove")).argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "remove");
		}, (sender, structure, args) -> {
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size() + 1);
			players.add("{offlinePlayer}");
			for (Player player : onlinePlayers)
				players.add(player.getName());
			return players;
		}).argument(null, 1, (sender, structure, args) -> {
			double value = multipleByMoneyFormat(ParseUtils.getDouble(args[2]), args[2]);
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[1]);
			if (query != null) {
				World world = null;
				if (sender instanceof Player)
					world = ((Player) sender).getWorld();
				else if (sender instanceof BlockCommandSender)
					world = ((BlockCommandSender) sender).getBlock().getWorld();
				else
					world = Bukkit.getWorlds().get(0);
				API.get().getEconomyHook().withdraw(query.getName(), world.getName(), value);
				msg(sender, "remove", PlaceholdersExecutor.i().add("target", query.getName()).add("balance", StringUtils.formatDouble(FormatType.COMPLEX, value)));
			} else
				msg(sender, "no-account", PlaceholdersExecutor.i().add("target", args[1]));
		}, (sender, structure, args) -> {
			List<String> tabCompleter = new ArrayList<>();
			if (args[2].isEmpty()) {
				tabCompleter.add("1k");
				tabCompleter.add("100");
			} else {
				if (Character.isDigit(args[2].charAt(args[2].length() - 1)))
					tabCompleter.add(args[2] + "k");
				tabCompleter.add(args[2]);
			}
			return tabCompleter;
		});

		// set
		cmd.argument("set", (sender, structure, args) -> {
			msgUsage(sender, "set");
		}).permission(getPerm("set")).argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "set");
		}, (sender, structure, args) -> {
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size() + 1);
			players.add("{offlinePlayer}");
			for (Player player : onlinePlayers)
				players.add(player.getName());
			return players;
		}).argument(null, 1, (sender, structure, args) -> {
			double value = multipleByMoneyFormat(ParseUtils.getDouble(args[2]), args[2]);
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[1]);
			if (query != null) {
				World world = null;
				if (sender instanceof Player)
					world = ((Player) sender).getWorld();
				else if (sender instanceof BlockCommandSender)
					world = ((BlockCommandSender) sender).getBlock().getWorld();
				else
					world = Bukkit.getWorlds().get(0);
				double currentBalance = API.get().getEconomyHook().getBalance(query.getName(), world.getName());
				if (currentBalance < 0)
					currentBalance = 0;
				if (currentBalance < value)
					API.get().getEconomyHook().deposit(query.getName(), world.getName(), value - currentBalance);
				else if (currentBalance != value)
					API.get().getEconomyHook().withdraw(query.getName(), world.getName(), currentBalance - value);
				msg(sender, "set", PlaceholdersExecutor.i().add("target", query.getName()).add("balance", StringUtils.formatDouble(FormatType.COMPLEX, value)));
			} else
				msg(sender, "no-account", PlaceholdersExecutor.i().add("target", args[1]));
		}, (sender, structure, args) -> {
			List<String> tabCompleter = new ArrayList<>();
			if (args[2].isEmpty()) {
				tabCompleter.add("1k");
				tabCompleter.add("100");
				tabCompleter.add("0");
			} else {
				if (Character.isDigit(args[2].charAt(args[2].length() - 1)))
					tabCompleter.add(args[2] + "k");
				tabCompleter.add(args[2]);
			}
			return tabCompleter;
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public static double multipleByMoneyFormat(double balance, String text) {
		switch (text.charAt(text.length() - 1)) {
		case 'k':
		case 'K':
			return balance * 1000;
		case 'm':
		case 'M':
			return balance * 1000000;
		case 'b':
		case 'B':
			return balance * 1.0E9;
		case 't':
		case 'T':
			return balance * 1.0E12;
		default:
			return balance;
		}
	}
}
