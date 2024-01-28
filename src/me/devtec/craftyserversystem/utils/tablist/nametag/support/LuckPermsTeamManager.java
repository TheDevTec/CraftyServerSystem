package me.devtec.craftyserversystem.utils.tablist.nametag.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
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
					NametagPlayer player = (NametagPlayer) pair.getKey();
					player.getNametag().setText(player.getNametagGenerator().apply(player.getPlayer()));
					player.setTeamName(getTeam((String) pair.getValue()));
					pair = updates.poll();
				}

			}
		}.runRepeating(20, 20);
		lpEventUsers = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), UserDataRecalculateEvent.class, e -> {
			NametagPlayer player = NametagManagerAPI.get().lookupByUuid(e.getUser().getUniqueId());
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
				NametagPlayer player = NametagManagerAPI.get().lookupByUuid(online.getUniqueId());
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
