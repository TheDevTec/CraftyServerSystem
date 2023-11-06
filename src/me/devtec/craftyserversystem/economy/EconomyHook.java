package me.devtec.craftyserversystem.economy;

import me.devtec.craftyserversystem.annotations.Nonnull;
import me.devtec.craftyserversystem.annotations.Nullable;

public interface EconomyHook {
	public double getBalance(@Nonnull String name, @Nullable String world);

	public void deposit(@Nonnull String name, @Nullable String world, double balance);

	public void withdraw(@Nonnull String name, @Nullable String world, double balance);

	public default boolean has(@Nonnull String name, @Nullable String world, double balance) {
		return getBalance(name, world) >= balance;
	}

	public String format(Double value);
}
