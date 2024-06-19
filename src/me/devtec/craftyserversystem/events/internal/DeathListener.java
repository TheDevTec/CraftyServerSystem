package me.devtec.craftyserversystem.events.internal;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

public class DeathListener implements CssListener {
	boolean hideMessage;
	boolean keepInventory;
	boolean keepExp;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getDeath();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {
		hideMessage = getConfig().getBoolean("death.hide-death-message");
		keepInventory = getConfig().getBoolean("death.keep-inventory");
		keepExp = getConfig().getBoolean("death.keep-exp");
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (hideMessage)
			e.setDeathMessage(null);
		if (keepInventory || e.getPlayer().hasPermission("css.death.keep-inventory")) {
			e.setKeepInventory(true);
			e.getDrops().clear();
		}
		if (keepExp || e.getPlayer().hasPermission("css.death.keep-exp")) {
			e.setKeepLevel(true);
			e.setDroppedExp(0);
		}
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());
		// Send json message
		API.get().getMsgManager().sendMessageFromFile(getConfig(), "death.broadcast", placeholders, BukkitLoader.getOnlinePlayers());
		for (String cmd : placeholders.apply(getConfig().getStringList("death.commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}
}
