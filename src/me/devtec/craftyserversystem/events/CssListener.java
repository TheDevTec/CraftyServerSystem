package me.devtec.craftyserversystem.events;

import me.devtec.shared.dataholder.Config;

public interface CssListener {
	public Config getConfig();

	public boolean isEnabled();

	public void reload();
}
