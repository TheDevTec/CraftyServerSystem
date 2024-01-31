package me.devtec.craftyserversystem.events;

import org.bukkit.event.Listener;

import me.devtec.shared.dataholder.Config;

public interface CssListener extends Listener {
	public Config getConfig();

	public boolean isEnabled();

	public void reload();
}
