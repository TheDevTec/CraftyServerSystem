package me.devtec.craftyserversystem.events.internal;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.economy.CssEconomyHook;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.dataholder.Config;

public class EconomyJoinListener implements CssListener {

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getEconomy();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {

	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (API.get().getEconomyHook() instanceof CssEconomyHook)
			if (!((CssEconomyHook) API.get().getEconomyHook()).economy.hasAccount(e.getPlayer().getName(), e.getPlayer().getWorld().getName()))
				((CssEconomyHook) API.get().getEconomyHook()).economy.createPlayerAccount(e.getPlayer().getName(), e.getPlayer().getWorld().getName());
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		if (API.get().getEconomyHook() instanceof CssEconomyHook)
			if (((CssEconomyHook) API.get().getEconomyHook()).economy.isEnabledPerWorldEconomy()
					&& !((CssEconomyHook) API.get().getEconomyHook()).economy.hasAccount(e.getPlayer().getName(), e.getPlayer().getWorld().getName()))
				((CssEconomyHook) API.get().getEconomyHook()).economy.createPlayerAccount(e.getPlayer().getName(), e.getPlayer().getWorld().getName());
	}
}
