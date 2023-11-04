package me.devtec.craftyserversystem.economy;

public class CssEconomyHook implements EconomyHook {
	public CssEconomy economy;

	public CssEconomyHook(CssEconomy economy) {
		this.economy = economy;
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
			economy.deposit(name, world, balance);
	}

	@Override
	public void withdraw(String name, String world, double balance) {
		if (economy != null)
			economy.withdraw(name, world, balance);
	}
}
