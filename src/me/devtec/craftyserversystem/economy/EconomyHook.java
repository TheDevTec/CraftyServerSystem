package me.devtec.craftyserversystem.economy;

public interface EconomyHook {
	public double getBalance(String name);

	public void deposit(String name, double balance);

	public void withdraw(String name, double balance);

	public default boolean has(String name, double balance) {
		return getBalance(name) >= balance;
	}
}
