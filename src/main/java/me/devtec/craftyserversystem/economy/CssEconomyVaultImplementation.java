package me.devtec.craftyserversystem.economy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

public class CssEconomyVaultImplementation extends CssEconomy implements Economy {

	public CssEconomyVaultImplementation(double defaultBalance, double minimumMoney, double maximumMoney, boolean perWorldEconomy, Map<String, List<String>> groupAndWorlds) {
		super(defaultBalance, minimumMoney, maximumMoney, perWorldEconomy, groupAndWorlds);
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public int fractionalDigits() {
		return 2;
	}

	@Override
	public boolean createPlayerAccount(String player) {
		return createPlayerAccount(player, Bukkit.getWorlds().get(0).getName());
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player) {
		return createPlayerAccount(player.getName(), Bukkit.getWorlds().get(0).getName());
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player, String world) {
		return createPlayerAccount(player.getName(), world);
	}

	@Override
	public EconomyResponse depositPlayer(String player, String world, double balance) {
		if (deposit(player, world, balance))
			return new EconomyResponse(balance, balance, ResponseType.SUCCESS, "Deposited money into player's account");
		return new EconomyResponse(balance, balance, ResponseType.FAILURE, "Amount of money cannot be in the negative");
	}

	@Override
	public EconomyResponse depositPlayer(String player, double balance) {
		return depositPlayer(player, Bukkit.getWorlds().get(0).getName(), balance);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, double balance) {
		return depositPlayer(player.getName(), Bukkit.getWorlds().get(0).getName(), balance);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, String world, double balance) {
		return depositPlayer(player.getName(), world, balance);
	}

	@Override
	public EconomyResponse withdrawPlayer(String player, String world, double balance) {
		if (withdraw(player, world, balance))
			return new EconomyResponse(balance, balance, ResponseType.SUCCESS, "Withdrawn money from player's account");
		return new EconomyResponse(balance, balance, ResponseType.FAILURE, "Amount of money cannot be in the negative");
	}

	@Override
	public EconomyResponse withdrawPlayer(String player, double balance) {
		return withdrawPlayer(player, Bukkit.getWorlds().get(0).getName(), balance);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, double balance) {
		return withdrawPlayer(player.getName(), Bukkit.getWorlds().get(0).getName(), balance);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double balance) {
		return withdrawPlayer(player.getName(), world, balance);
	}

	@Override
	public double getBalance(String player) {
		assert player != null : "Player cannot be null";
		return getBalance(player, Bukkit.getWorlds().get(0).getName());
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		assert player != null : "Player cannot be null";
		return getBalance(player.getName(), Bukkit.getWorlds().get(0).getName());
	}

	@Override
	public double getBalance(OfflinePlayer player, String world) {
		assert player != null : "Player cannot be null";
		return getBalance(player.getName(), world);
	}

	@Override
	public boolean has(String player, double balance) {
		assert player != null : "Player cannot be null";
		return has(player, Bukkit.getWorlds().get(0).getName(), balance);
	}

	@Override
	public boolean has(OfflinePlayer player, double balance) {
		assert player != null : "Player cannot be null";
		return has(player.getName(), Bukkit.getWorlds().get(0).getName(), balance);
	}

	@Override
	public boolean has(OfflinePlayer player, String world, double balance) {
		assert player != null : "Player cannot be null";
		return has(player.getName(), world, balance);
	}

	@Override
	public boolean hasAccount(String player) {
		assert player != null : "Player cannot be null";
		return hasAccount(player, Bukkit.getWorlds().get(0).getName());
	}

	@Override
	public boolean hasAccount(OfflinePlayer player) {
		assert player != null : "Player cannot be null";
		return hasAccount(player.getName(), Bukkit.getWorlds().get(0).getName());
	}

	@Override
	public boolean hasAccount(OfflinePlayer player, String world) {
		assert player != null : "Player cannot be null";
		return hasAccount(player.getName(), world);
	}

	@Override
	public boolean hasBankSupport() {
		return false;
	}

	@Override
	public List<String> getBanks() {
		return Collections.emptyList();
	}

	@Override
	public EconomyResponse bankBalance(String arg0) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse bankDeposit(String arg0, double arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse bankHas(String arg0, double arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse bankWithdraw(String arg0, double arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse createBank(String arg0, String arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse createBank(String arg0, OfflinePlayer arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse deleteBank(String arg0) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse isBankMember(String arg0, String arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse isBankMember(String arg0, OfflinePlayer arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse isBankOwner(String arg0, String arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}

	@Override
	public EconomyResponse isBankOwner(String arg0, OfflinePlayer arg1) {
		return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Not implemented");
	}
}
