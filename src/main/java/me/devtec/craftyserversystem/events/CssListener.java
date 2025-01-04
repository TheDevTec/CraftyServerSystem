package me.devtec.craftyserversystem.events;

import org.bukkit.event.Listener;

import me.devtec.shared.dataholder.Config;

public interface CssListener extends Listener {
	Config getConfig();

	boolean isEnabled();

	void reload();

	default void unregister() {

	}
}
