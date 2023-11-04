package me.devtec.craftyserversystem.economy;

public class EmptyEconomyHook implements EconomyHook {

	public EmptyEconomyHook() {
	}

	@Override
	public double getBalance(String name, String world) {
		return 0;
	}

	@Override
	public void deposit(String name, String world, double balance) {

	}

	@Override
	public void withdraw(String name, String world, double balance) {

	}
}
