package me.devtec.craftyserversystem.economy;

public class EmptyEconomyHook implements EconomyHook {

	public EmptyEconomyHook() {
	}

	@Override
	public double getBalance(String name) {
		return 0;
	}

	@Override
	public void deposit(String name, double balance) {

	}

	@Override
	public void withdraw(String name, double balance) {

	}
}
