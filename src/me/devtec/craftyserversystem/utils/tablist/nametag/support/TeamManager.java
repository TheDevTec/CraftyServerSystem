package me.devtec.craftyserversystem.utils.tablist.nametag.support;

import java.util.UUID;

public interface TeamManager {
	public String getTeam(UUID playerUuid);

	public String getTeam(String vaultGroup);

	public void reload();

	public void disable();
}
