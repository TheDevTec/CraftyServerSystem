package me.devtec.craftyserversystem.economy;

public class CssEconomyHook implements EconomyHook {
	public CssEconomy economy;

	public CssEconomyHook(CssEconomy economy) {
		this.economy = economy;
	}

	@Override
	public double getBalance(String name, String world) {
		return economy.getBalance(name, world);
	}

	@Override
	public void deposit(String name, String world, double balance) {
		economy.deposit(name, world, balance);
	}

	@Override
	public void withdraw(String name, String world, double balance) {
		economy.withdraw(name, world, balance);
	}

	@Override
	public String format(Double value) {
		return economy.format(value);
	}
}
