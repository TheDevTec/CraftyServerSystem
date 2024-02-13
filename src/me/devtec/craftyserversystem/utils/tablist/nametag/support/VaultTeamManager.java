package me.devtec.craftyserversystem.utils.tablist.nametag.support;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.sorting.SortingAPI;
import me.devtec.shared.sorting.SortingAPI.ComparableObject;

public class VaultTeamManager implements TeamManager {
	private final Map<String, String> groupAndTeamName = new HashMap<>();

	private int taskId;

	public VaultTeamManager() {
		taskId = new Tasker() {

			@Override
			public void run() {
				for (NametagPlayer player : NametagManagerAPI.get().getPlayers()) {
					player.getNametag().setText(player.getNametagGenerator().apply(player.getPlayer()));
					player.setTeamName(getTeam(API.get().getPermissionHook().getGroup(player.getName())));
				}
			}
		}.runRepeating(100, 100);
	}

	@Override
	public void disable() {
		Scheduler.cancelTask(taskId);
	}

	@Override
	public String getTeam(UUID playerUuid) {
		return getTeam(API.get().getPermissionHook().getGroup(me.devtec.shared.API.offlineCache().lookupNameById(playerUuid)));
	}

	@Override
	public String getTeam(String vaultGroup) {
		return groupAndTeamName.getOrDefault(vaultGroup, "z");
	}

	@Override
	public void reload() {
		groupAndTeamName.clear();

		Map<String, Integer> weights = new HashMap<>();
		int pos = API.get().getConfigManager().getTab().getStringList("sorting.list").size();
		for (String group : API.get().getConfigManager().getTab().getStringList("sorting.list"))
			weights.put(group, pos--);

		int startAt = 'a';
		for (ComparableObject<String, Integer> entry : SortingAPI.sortByValueArray(weights, true))
			groupAndTeamName.put(entry.getKey(), "_" + (char) startAt++);
	}

}
