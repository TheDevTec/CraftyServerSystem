package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.shared.utility.ParseUtils;

public class Pay extends CssCommand {

	@Override
	public String section() {
		return "pay";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).priority(1).selector(Selector.NUMBER, (sender, structure, args) -> {
			Player target = Bukkit.getPlayer(args[0]);
			if (sender.equals(target)) {
				msg(sender, "failed.self", PlaceholdersExecutor.EMPTY);
				return;
			}
			World world = sender.getWorld();
			double money = ParseUtils.getDouble(args[1]);
			if (pay(sender.getName(), target.getName(), world.getName(), money)) {
				PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("balance", money);
				msg(sender, "success.sender", executor);
				msg(target, "success.target", executor);
			} else
				msg(sender, "failed.money", PlaceholdersExecutor.i().add("target", target.getName()).add("balance", money));

		});
		// other
		cmd.argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).selector(Selector.NUMBER, (sender, structure, args) -> {
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[0]);
			if (query != null) {
				World world = sender.getWorld();
				double money = ParseUtils.getDouble(args[1]);
				if (pay(sender.getName(), query.getName(), world.getName(), money))
					msg(sender, "success.sender", PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", query.getName()).add("balance", money));
				else
					msg(sender, "failed.money", PlaceholdersExecutor.i().add("target", query.getName()).add("balance", money));
			} else
				msg(sender, "no-account", PlaceholdersExecutor.i().add("target", args[0]));
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
