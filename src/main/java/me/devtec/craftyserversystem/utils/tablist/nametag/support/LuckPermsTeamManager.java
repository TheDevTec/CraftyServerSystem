package me.devtec.craftyserversystem.utils.tablist.nametag.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;
import org.joml.Math;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI.SimpleTeam;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer;
import me.devtec.shared.Pair;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.sorting.SortingAPI;
import me.devtec.shared.sorting.SortingAPI.ComparableObject;
import me.devtec.theapi.bukkit.BukkitLoader;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;

public class LuckPermsTeamManager implements TeamManager {
	private final Map<String, String> groupAndTeamName = new HashMap<>();

	private int taskId;
	private EventSubscription<UserDataRecalculateEvent> lpEventUsers;
	private EventSubscription<GroupDataRecalculateEvent> lpEventGroups;

	public LuckPermsTeamManager() {
		Queue<Pair> updates = new ConcurrentLinkedQueue<>();
		taskId = new Tasker() {

			@Override
			public void run() {
				Pair pair = updates.poll();
				while (pair != null) {
					ClassicTabPlayer player = (ClassicTabPlayer) pair.getKey();
					String team = makeItOriginal(player.getId(),getTeam((String) pair.getValue()));
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
					pair = updates.poll();
				}

			}
		}.runRepeating(20, 20);
		lpEventUsers = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), UserDataRecalculateEvent.class, e -> {
			ClassicTabPlayer player = TabAPI.getHolder(e.getUser().getUniqueId());
			if (player == null)
				return; // Probably not loaded for uknown reasons..
			updates.add(Pair.of(player, e.getUser().getPrimaryGroup()));
		});
		lpEventGroups = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), GroupDataRecalculateEvent.class, e -> {
			reload();
			for (Player online : BukkitLoader.getOnlinePlayers()) {
				String group;
				if (!(group = LuckPermsProvider.get().getUserManager().getUser(online.getUniqueId()).getPrimaryGroup()).equals(e.getGroup().getName()))
					continue;
				ClassicTabPlayer player = TabAPI.getHolder(online.getUniqueId());
				if (player == null)
					continue; // Probably not loaded for uknown reasons..

				updates.add(Pair.of(player, group));
			}
		});
	}

	@Override
	public void disable() {
		lpEventUsers.close();
		lpEventGroups.close();
		Scheduler.cancelTask(taskId);
	}

	@Override
	public String getTeam(UUID playerUuid) {
		return getTeam(LuckPermsProvider.get().getUserManager().getUser(playerUuid).getPrimaryGroup());
	}

	@Override
	public String getTeam(String vaultGroup) {
		return groupAndTeamName.getOrDefault(vaultGroup, "z");
	}

	private String makeItOriginal(int id, String team) {
		String prefix = "css_"+id;
		return prefix+team.substring(0, Math.min(16-prefix.length(),team.length()));
	}

	@Override
	public void reload() {
		groupAndTeamName.clear();

		Map<String, Integer> weights = new HashMap<>();
		for (Group group : LuckPermsProvider.get().getGroupManager().getLoadedGroups())
			weights.put(group.getName(), group.getWeight().orElse(0));

		int startAt = 'a';
		for (ComparableObject<String, Integer> entry : SortingAPI.sortByValueArray(weights, true))
			groupAndTeamName.put(entry.getKey(), "_" + (char) startAt++);
	}

}
