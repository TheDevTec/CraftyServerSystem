package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.economy.CssEconomyHook;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.placeholders.PlaceholderExpansion;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.sorting.SortingAPI;
import me.devtec.shared.sorting.SortingAPI.ComparableObject;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.shared.utility.ParseUtils;

public class BalanceTop extends CssCommand {

	static Map<String, ComparableObject<String, Double>[]> balanceTop = new HashMap<>();

	public int task;
	public PlaceholderExpansion placeholder;
	private double minimumBalanceToShow;

	@Override
	public void register() {
		if (isRegistered())
			return;

		minimumBalanceToShow = API.get().getConfigManager().getEconomy().getDouble("settings.balance-top.minimum-money");
		int entriesPerPage = API.get().getConfigManager().getEconomy().getInt("settings.balance-top.entries-per-page");
		if (API.get().getConfigManager().getEconomy().getBoolean("settings.balance-top.enable-global-placeholder")) {
			EconomyHook hook = API.get().getEconomyHook();
			placeholder = new PlaceholderExpansion("css_baltop") {

				@Override
				public String apply(String placeholder, UUID uuid) {
					if (placeholder.startsWith("css_baltop_name_")) {
						int pos = ParseUtils.getInt(placeholder, 11, placeholder.length());
						String worldGroup = "default";
						if (hook instanceof CssEconomyHook) {
							CssEconomyHook css = (CssEconomyHook) hook;
							if (css.economy.isEnabledPerWorldEconomy())
								if (uuid != null && Bukkit.getPlayer(uuid) != null)
									worldGroup = css.economy.getWorldGroup(Bukkit.getPlayer(uuid).getWorld().getName());
						}
						ComparableObject<String, Double>[] comp = balanceTop.get(worldGroup);
						return comp.length > pos ? comp[pos].getKey() : "-";
					}
					if (placeholder.startsWith("css_baltop_balance_")) {
						int pos = ParseUtils.getInt(placeholder, 11, placeholder.length());
						String worldGroup = "default";
						if (hook instanceof CssEconomyHook) {
							CssEconomyHook css = (CssEconomyHook) hook;
							if (css.economy.isEnabledPerWorldEconomy())
								if (uuid != null && Bukkit.getPlayer(uuid) != null)
									worldGroup = css.economy.getWorldGroup(Bukkit.getPlayer(uuid).getWorld().getName());
						}
						ComparableObject<String, Double>[] comp = balanceTop.get(worldGroup);
						return comp.length > pos ? hook.format(comp[pos].getValue()) : "-";
					}
					return null;
				}
			}.register();
		}
		task = new Tasker() {

			@Override
			public void run() {
				calculate(minimumBalanceToShow);
			}
		}.runRepeating((long) (Math.random() * 10 * 8), 20 * 60 * 60);

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			listBalanceTop(sender, balanceTop.get(getWorldGroup(sender)), 1, entriesPerPage);
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.INTEGER, (sender, structure, args) -> {
			int page = ParseUtils.getInt(args[0]);
			listBalanceTop(sender, balanceTop.get(getWorldGroup(sender)), page, entriesPerPage);
		}, (sender, structure, args) -> new ArrayList<>());
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void listBalanceTop(CommandSender sender, ComparableObject<String, Double>[] comparableObjects, int page, int maxEntries) {
		EconomyHook hook = API.get().getEconomyHook();
		int totalPages = Math.max(comparableObjects.length / maxEntries, 1);
		if (page <= 0)
			page = 1;
		if (page > totalPages)
			page = totalPages;
		msg(sender, "header", PlaceholdersExecutor.i().add("page", page).add("totalPages", totalPages));
		for (int i = page * maxEntries - maxEntries; i < page * maxEntries && i < comparableObjects.length; ++i) {
			ComparableObject<String, Double> comp = comparableObjects[i];
			msg(sender, "key", PlaceholdersExecutor.i().add("position", i).add("target", comp.getKey()).add("balance", hook.format(comp.getValue())));
		}
		msg(sender, "footer", PlaceholdersExecutor.i().add("page", page).add("totalPages", totalPages).add("previousPage", Math.max(1, page - 1)).add("nextPage", Math.min(totalPages, page + 1)));
	}

	private String getWorldGroup(CommandSender sender) {
		EconomyHook hook = API.get().getEconomyHook();
		String worldGroup = "default";
		if (hook instanceof CssEconomyHook) {
			CssEconomyHook css = (CssEconomyHook) hook;
			if (css.economy.isEnabledPerWorldEconomy())
				if (sender instanceof BlockCommandSender)
					worldGroup = css.economy.getWorldGroup(((BlockCommandSender) sender).getBlock().getWorld().getName());
				else if (sender instanceof Player)
					worldGroup = css.economy.getWorldGroup(((Player) sender).getWorld().getName());
		}
		return worldGroup;
	}

	@Override
	public void unregister() {
		super.unregister();
		if (task != 0)
			Scheduler.cancelTask(task);
		task = 0;
		if (placeholder != null)
			placeholder.unregister();
		placeholder = null;
	}

	public void calculate() {
		calculate(minimumBalanceToShow);
	}

	public void calculate(double minimumBalanceToShow) {
		if (API.get().getEconomyHook() == null)
			return;
		balanceTop.clear();
		if (API.get().getEconomyHook() instanceof CssEconomyHook) {
			CssEconomyHook hook = (CssEconomyHook) API.get().getEconomyHook();
			if (hook.economy.isEnabledPerWorldEconomy()) {
				Map<String, Map<String, Double>> bal = new HashMap<>();
				for (Entry<String, List<String>> entry : hook.economy.getPerWorldGroups().entrySet()) {
					if (entry.getValue().isEmpty())
						continue;
					bal.put(entry.getKey(), new HashMap<>());
				}
				for (Query query : me.devtec.shared.API.offlineCache().getQueries())
					for (Entry<String, List<String>> entry : hook.economy.getPerWorldGroups().entrySet()) {
						if (entry.getValue().isEmpty())
							continue;
						double balance = API.get().getEconomyHook().getBalance(query.getName(), entry.getValue().get(0));
						if (balance > minimumBalanceToShow)
							bal.get(entry.getKey()).put(query.getName(), balance);
					}
				for (Entry<String, Map<String, Double>> entry : bal.entrySet())
					balanceTop.put(entry.getKey(), SortingAPI.sortByValueArray(entry.getValue(), false));
				return;
			}
		}
		Map<String, Double> bal = new HashMap<>();
		for (Query query : me.devtec.shared.API.offlineCache().getQueries()) {
			double balance = API.get().getEconomyHook().getBalance(query.getName(), null);
			if (balance > minimumBalanceToShow)
				bal.put(query.getName(), balance);
		}
		balanceTop.put("default", SortingAPI.sortByValueArray(bal, false));
	}
}
