package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Pair;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Pay extends CssCommand {

	/**
	 * @apiNote Pair(Sender's name, Money) -> Result is a fee
	 */
	@Nonnull
	public Function<Pair, Double> fees = balance -> 0.0;

	/**
	 * @apiNote Pair(Sender's name, Money) -> Result is a Pair(Balance that can be
	 *          sent, Period) (If result balance is under 0, player can't send
	 *          anything - result will be limit in negative)
	 */
	@Nonnull
	public Function<Pair, Pair> limit = balance -> Pair.of((double) balance.getValue(), 0L);

	@Override
	public void register() {
		if (isRegistered())
			return;
		Config economy = API.get().getConfigManager().getEconomy();
		if (economy.getBoolean("settings.pay-fees.enabled"))
			if (economy.getString("settings.pay-fees.type").equalsIgnoreCase("global")) {
				double globalFee = economy.getDouble("settings.pay-fees.value") / 100;
				if (globalFee > 0)
					fees = balance -> (double) balance.getValue() * globalFee;
			} else {
				Map<String, Double> perGroup = new HashMap<>();
				for (String group : economy.getKeys("settings.pay-fees.groups"))
					perGroup.put(group, economy.getDouble("settings.pay-fees.groups." + group));
				fees = balance -> {
					double fee = perGroup.getOrDefault(API.get().getPermissionHook().getGroup((String) balance.getKey()), 0.0);
					return fee > 0 ? (double) balance.getValue() * fee : 0;
				};
			}
		if (economy.getBoolean("settings.pay-limit.enabled"))
			if (economy.getString("settings.pay-limit.type").equalsIgnoreCase("global")) {
				double globalLimit = Economy.multipleByMoneyFormat(economy.getDouble("settings.pay-limit.global.limit"), economy.getString("settings.pay-limit.global.limit"));
				long globalPeriod = TimeUtils.timeFromString(economy.getString("settings.pay-limit.global.period"));
				if (globalLimit > 0)
					limit = new Function<Pair, Pair>() {

						public double getUsedLimit(Config user) {
							List<String> records = user.getStringList("pay-limit.records");
							Iterator<String> itr = records.iterator();
							boolean someChange = false;

							double usedLimit = 0;

							while (itr.hasNext()) {
								String record = itr.next();
								int split = record.indexOf(':');
								long time = ParseUtils.getLong(record, 0, split);
								if (time - System.currentTimeMillis() / 1000 + globalPeriod <= 0) {
									itr.remove();
									someChange = true;
								} else
									usedLimit += ParseUtils.getDouble(record, split, record.length());
							}
							if (someChange)
								user.set("pay-limit.records", records.isEmpty() ? null : records);
							return usedLimit;
						}

						@Override
						public Pair apply(Pair balance) {
							Config user = me.devtec.shared.API.getUser((String) balance.getKey());
							double availableLimit = globalLimit - getUsedLimit(user);
							return Pair.of(availableLimit <= 0 ? -globalLimit : (double) balance.getValue() > availableLimit ? availableLimit : (double) balance.getValue(), globalPeriod);
						}
					};
			} else {
				Map<String, Pair> perGroup = new HashMap<>();
				for (String group : economy.getKeys("settings.pay-limit.groups"))
					perGroup.put(group, Pair.of(TimeUtils.timeFromString(economy.getString("settings.pay-limit.groups." + group + ".period")),
							Economy.multipleByMoneyFormat(economy.getDouble("settings.pay-limit.groups." + group + ".limit"), economy.getString("settings.pay-limit.groups." + group + ".limit"))));
				Pair DEFAULT_PAIR = Pair.of(0L, 0.0);
				limit = new Function<Pair, Pair>() {

					public double getUsedLimit(Config user, long period) {
						List<String> records = user.getStringList("pay-limit.records");
						Iterator<String> itr = records.iterator();
						boolean someChange = false;

						double usedLimit = 0;

						while (itr.hasNext()) {
							String record = itr.next();
							int split = record.indexOf(':');
							long time = ParseUtils.getLong(record, 0, split);
							if (time - System.currentTimeMillis() / 1000 + period <= 0) {
								itr.remove();
								someChange = true;
							} else
								usedLimit += ParseUtils.getDouble(record, split, record.length());
						}
						if (someChange)
							user.set("pay-limit.records", records.isEmpty() ? null : records);
						return usedLimit;
					}

					@Override
					public Pair apply(Pair balance) {
						Pair limit = perGroup.getOrDefault(API.get().getPermissionHook().getGroup((String) balance.getKey()), DEFAULT_PAIR);
						if ((double) limit.getValue() <= 0)
							return Pair.of((double) balance.getValue(), 0L);
						Config user = me.devtec.shared.API.getUser((String) balance.getKey());
						double availableLimit = (double) limit.getValue() - getUsedLimit(user, (long) limit.getKey());
						return Pair.of(availableLimit <= 0 ? -(double) limit.getValue() : (double) balance.getValue() > availableLimit ? availableLimit : (double) balance.getValue(),
								(long) limit.getKey());
					}
				};
			}

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
				if (money <= 0) {
					msg(sender, "failed.must-be-above-zero", PlaceholdersExecutor.EMPTY);
					return;
				}
				Pair pair = Pair.of(sender.getName(), money);
				if (!sender.hasPermission(getPerm("bypass-limit"))) {
					Pair limitResult = limit.apply(pair);
					money = (double) limitResult.getKey();
					if (money <= 0) {
						msg(sender, "failed.over-limit",
								PlaceholdersExecutor.i().add("limit", StringUtils.formatDouble(FormatType.COMPLEX, money * -1)).add("period", TimeUtils.timeToString((long) limitResult.getValue())));
						return;
					}
				}
				double fee = sender.hasPermission(getPerm("bypass-fee")) ? 0 : fees.apply(pair);
				if (pay(sender.getName(), query.getName(), world.getName(), money, fee)) {
					Config user = me.devtec.shared.API.getUser(sender.getUniqueId());
					if (!sender.hasPermission(getPerm("bypass-limit"))) {
						List<String> records = user.getStringList("pay-limit.records");
						if (records.isEmpty())
							records = new ArrayList<>();
						records.add(System.currentTimeMillis() / 1000 + ":" + money);
						user.set("pay-limit.records", records);
					}
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", query.getName())
							.add("balance_without_fee", StringUtils.formatDouble(FormatType.COMPLEX, money)).add("balance", StringUtils.formatDouble(FormatType.COMPLEX, money - fee));
					msg(sender, "success.sender", placeholders);
					Player target = Bukkit.getPlayer(query.getUUID());
					if (target != null)
						msg(target, "success.target", placeholders);
				} else
					msg(sender, "failed.money", PlaceholdersExecutor.i().add("target", query.getName()).add("balance_without_fee", StringUtils.formatDouble(FormatType.COMPLEX, money))
							.add("balance", StringUtils.formatDouble(FormatType.COMPLEX, money - fee)).add("fee", StringUtils.formatDouble(FormatType.COMPLEX, fee)));
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

	public boolean pay(String player, String target, String world, double balance, double fee) {
		EconomyHook hook = API.get().getEconomyHook();
		if (hook.has(player, world, balance)) {
			hook.withdraw(player, world, balance);
			hook.deposit(target, world, balance - fee);
			return true;
		}
		return false;
	}
}
