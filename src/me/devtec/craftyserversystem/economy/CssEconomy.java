package me.devtec.craftyserversystem.economy;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.devtec.shared.API;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;

public class CssEconomy {

	private double defaultBalance;
	private double minimumMoney;
	private double maximumMoney;
	private boolean perWorldEconomy;
	private Map<String, List<String>> groupAndWorlds;

	public CssEconomy(double defaultBalance, double minimumMoney, double maximumMoney, boolean perWorldEconomy, Map<String, List<String>> groupAndWorlds) {
		this.perWorldEconomy = perWorldEconomy;
		this.groupAndWorlds = groupAndWorlds;
		this.minimumMoney = minimumMoney;
		this.maximumMoney = maximumMoney;
	}

	public String lookupPath(String player, String world) {
		if (perWorldEconomy)
			return "css.eco-pw." + getWorldGroup(world);
		return "css.eco";
	}

	public String getWorldGroup(String world) {
		if (world != null)
			for (Entry<String, List<String>> entry : groupAndWorlds.entrySet())
				if (entry.getValue().contains(world))
					return entry.getKey();
		return "default";
	}

	public boolean isEnabledPerWorldEconomy() {
		return perWorldEconomy;
	}

	public Map<String, List<String>> getPerWorldGroups() {
		return groupAndWorlds;
	}

	public String getName() {
		return "CssEconomy";
	}

	public String format(double value) {
		return StringUtils.formatDouble(FormatType.COMPLEX, value);
	}

	public String currencyNamePlural() {
		return "CraftyDolar";
	}

	public String currencyNameSingular() {
		return "$";
	}

	public boolean createPlayerAccount(String player, String world) {
		Config data = API.getUser(player);
		String path = lookupPath(player, world);
		if (data.existsKey(path))
			return false;
		data.set(path, defaultBalance);
		return true;
	}

	public boolean deposit(String player, String world, double balance) {
		assert player != null : "Player cannot be null";
		if (balance <= 0)
			return false;
		Config data = API.getUser(player);
		String path = lookupPath(player, world);
		double totalBalance = data.getDouble(path) + balance;
		if (totalBalance < minimumMoney)
			totalBalance = minimumMoney;
		if (totalBalance > maximumMoney)
			totalBalance = maximumMoney;
		data.set(path, totalBalance);
		return true;
	}

	public boolean withdraw(String player, String world, double balance) {
		assert player != null : "Player cannot be null";
		if (balance <= 0)
			return false;
		Config data = API.getUser(player);
		String path = lookupPath(player, world);
		double totalBalance = data.getDouble(path) - balance;
		if (totalBalance < minimumMoney)
			totalBalance = minimumMoney;
		if (totalBalance > maximumMoney)
			totalBalance = maximumMoney;
		data.set(path, totalBalance);
		return true;
	}

	public double getBalance(String player, String world) {
		assert player != null : "Player cannot be null";
		double totalBalance = API.getUser(player).getDouble(lookupPath(player, world));
		if (totalBalance < minimumMoney)
			totalBalance = minimumMoney;
		if (totalBalance > maximumMoney)
			totalBalance = maximumMoney;
		return totalBalance;
	}

	public boolean has(String player, String world, double balance) {
		assert player != null : "Player cannot be null";
		return getBalance(player, world) >= balance;
	}

	public boolean hasAccount(String player, String world) {
		assert player != null : "Player cannot be null";
		return API.getUser(player).existsKey(lookupPath(player, world));
	}
}
