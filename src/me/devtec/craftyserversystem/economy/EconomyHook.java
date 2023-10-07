package me.devtec.craftyserversystem.economy;

import me.devtec.craftyserversystem.annotations.Nonnull;

public interface EconomyHook {
	public double getBalance(@Nonnull String name);

	public void deposit(@Nonnull String name, double balance);

	public void withdraw(@Nonnull String name, double balance);

	public default boolean has(@Nonnull String name, double balance) {
		return getBalance(name) >= balance;
	}
}
