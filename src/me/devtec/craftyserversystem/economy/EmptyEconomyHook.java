package me.devtec.craftyserversystem.economy;

import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;

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

	@Override
	public String format(Double value) {
		return StringUtils.formatDouble(FormatType.NORMAL, value);
	}
}
