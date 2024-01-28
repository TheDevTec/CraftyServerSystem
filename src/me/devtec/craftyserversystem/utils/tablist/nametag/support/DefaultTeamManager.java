package me.devtec.craftyserversystem.utils.tablist.nametag.support;

import java.util.UUID;

public class DefaultTeamManager implements TeamManager {

	@Override
	public void disable() {
	}

	@Override
	public String getTeam(UUID playerUuid) {
		return getTeam(me.devtec.shared.API.offlineCache().lookupNameById(playerUuid));
	}

	@Override
	public String getTeam(String playerName) {
		return playerName.length() > 16 ? playerName.substring(0, 16) : playerName;
	}

	@Override
	public void reload() {
	}

}