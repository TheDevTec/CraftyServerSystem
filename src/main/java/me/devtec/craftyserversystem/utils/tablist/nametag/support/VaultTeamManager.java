package me.devtec.craftyserversystem.utils.tablist.nametag.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Math;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI.SimpleTeam;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer;
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
				for (ClassicTabPlayer player : TabAPI.getPlayers()) {
					String team = makeItOriginal(player.getId(),getTeam(API.get().getPermissionHook().getGroup(player.getPlayer())));
					List<SimpleTeam> teams = new ArrayList<>();
					for(SimpleTeam t : player.getTeams()) {
						if(t.getTeam().equals(team))
							//TODO update team
							break;
						if(t.getTeam().startsWith("css_"))
							if(t.getPlayers().size()==1)
								teams.add(t);
							else
								teams.add(t.leavePlayer(player.getPlayer().getName()));
					}
					for(SimpleTeam t : teams) {
						if(t.getPlayers().size()==1) {
							//remove
							for(ClassicTabPlayer holder : TabAPI.getPlayers())
								holder.removeTeam(t.getTeam());
							continue;
						}

						for(ClassicTabPlayer holder : TabAPI.getPlayers())
							holder.leaveTeam(t);
					}
					//TODO update lines
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

	private String makeItOriginal(int id, String team) {
		String prefix = "css_"+id;
		return prefix+team.substring(0, Math.min(16-prefix.length(),team.length()));
	}

}
