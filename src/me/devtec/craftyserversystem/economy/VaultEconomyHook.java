package me.devtec.craftyserversystem.economy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.devtec.craftyserversystem.Loader;
import me.devtec.shared.scheduler.Tasker;
import net.milkbowl.vault.economy.Economy;

public class VaultEconomyHook implements EconomyHook {
	public Economy economy;

	public VaultEconomyHook() {
		new Tasker() {
			@Override
			public void run() {
				if (!Loader.getPlugin().getEconomyHook().equals(VaultEconomyHook.this) || getVault())
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

	@Override
	public double getBalance(String name) {
		if (economy == null)
			return 0;
		return economy.getBalance(name);
	}

	@Override
	public void deposit(String name, double balance) {
		if (economy != null)
			economy.depositPlayer(name, balance);
	}

	@Override
	public void withdraw(String name, double balance) {
		if (economy != null)
			economy.withdrawPlayer(name, balance);
	}
}
