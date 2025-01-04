package me.devtec.craftyserversystem.economy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import net.milkbowl.vault.economy.Economy;

public class VaultEconomyHook implements EconomyHook {
	public Economy economy;

	public VaultEconomyHook() {
		new Tasker() {
			@Override
			public void run() {
				if (!API.get().getEconomyHook().equals(VaultEconomyHook.this) || getVault())
					cancel();
			}
		}.runRepeatingTimes(5, 5, 480);
	}

	public boolean getVault() {
		try {
			RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
			if (provider != null)
				economy = provider.getProvider();
			return economy != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static void registerOurEconomy() {
		Loader.getPlugin().getLogger().info("Registering our Economy service.");
		Bukkit.getServicesManager().register(Economy.class, (CssEconomyVaultImplementation) ((CssEconomyHook) API.get().getEconomyHook()).economy, Loader.getPlugin(), ServicePriority.Normal);
	}

	public static void unregisterOurEconomy() {
		Loader.getPlugin().getLogger().info("Unregistering our Economy service.");
		Bukkit.getServicesManager().unregister(((CssEconomyHook) API.get().getEconomyHook()).economy);
	}

	@Override
	public double getBalance(String name, String world) {
		if (economy == null)
			return 0;
		return economy.getBalance(name, world);
	}

	@Override
	public void deposit(String name, String world, double balance) {
		if (economy != null)
			economy.depositPlayer(name, world, balance);
	}

	@Override
	public void withdraw(String name, String world, double balance) {
		if (economy != null)
			economy.withdrawPlayer(name, world, balance);
	}

	@Override
	public String format(Double value) {
		if (economy != null)
			return economy.format(value);
		return StringUtils.formatDouble(FormatType.NORMAL, value);
	}
}
