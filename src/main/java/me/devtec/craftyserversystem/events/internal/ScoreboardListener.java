package me.devtec.craftyserversystem.events.internal;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.events.internal.supportlp.ScoreboardLP;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.permission.VaultPermissionHook;
import me.devtec.craftyserversystem.utils.InternalPlaceholders;
import me.devtec.craftyserversystem.utils.scoreboard.ConditionScoreboardData;
import me.devtec.craftyserversystem.utils.scoreboard.PerWorldScoreboardData;
import me.devtec.craftyserversystem.utils.scoreboard.ScoreboardData;
import me.devtec.craftyserversystem.utils.scoreboard.UserScoreboardData;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;

public class ScoreboardListener implements CssListener {

	private Map<String, PerWorldScoreboardData> perWorld = new HashMap<>();
	private Map<String, ScoreboardData> perGroup = new HashMap<>();
	private Map<String, ScoreboardData> perPlayer = new HashMap<>();
	private List<ConditionScoreboardData> conditions = new LinkedList<>();
	private Map<UUID, BitSet> conditionStates = new ConcurrentHashMap<>();
	private ScoreboardData global;
	public static Map<UUID, UserScoreboardData> data = new ConcurrentHashMap<>();
	private int taskId;
	private int refleshTaskId;
	private ScoreboardLP lpListener;
	private List<String> disabledInWorlds;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getScoreboard();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {
		perWorld.clear();
		perGroup.clear();
		perPlayer.clear();
		conditions.clear();
		data.clear();
		conditionStates.clear();
		if(lpListener != null) {
			lpListener.unregister();
			lpListener = null;
		}
		if(taskId != 0)
			Scheduler.cancelTask(taskId);
		if(refleshTaskId != 0)
			Scheduler.cancelTask(refleshTaskId);
		disabledInWorlds = getConfig().getStringList("disabled-in-worlds");
		for(String world : getConfig().getKeys("world")) {
			PerWorldScoreboardData pw;
			perWorld.put(world, pw = new PerWorldScoreboardData());
			pw.setTitle(getConfig().getString("world." + world + ".title"));
			pw.setLines(getConfig().existsKey("world." + world + ".lines") ? getConfig().getStringList("world." + world + ".lines") : null);
			for(String player : getConfig().getKeys("world." + world + ".player")) {
				ScoreboardData data;
				pw.perPlayer.put(player, data = new ScoreboardData());
				data.setTitle(getConfig().getString("world." + world + ".player." + player + ".title"));
				data.setLines(getConfig().existsKey("world." + world + ".player." + player + ".lines") ? getConfig().getStringList("world." + world + ".player." + player + ".lines") : null);
			}
			for(String group : getConfig().getKeys("world." + world + ".group")) {
				ScoreboardData data;
				pw.perGroup.put(group, data = new ScoreboardData());
				data.setTitle(getConfig().getString("world." + world + ".group." + group + ".title"));
				data.setLines(getConfig().existsKey("world." + world + ".group." + group + ".lines") ? getConfig().getStringList("world." + world + ".group." + group + ".lines") : null);
			}
		}
		for(String player : getConfig().getKeys("player")) {
			ScoreboardData data;
			perPlayer.put(player, data = new ScoreboardData());
			data.setTitle(getConfig().getString("player." + player + ".title"));
			data.setLines(getConfig().existsKey("player." + player + ".lines") ? getConfig().getStringList("player." + player + ".lines") : null);
		}
		for(String group : getConfig().getKeys("group")) {
			ScoreboardData data;
			perGroup.put(group, data = new ScoreboardData());
			data.setTitle(getConfig().getString("group." + group + ".title"));
			data.setLines(getConfig().existsKey("group." + group + ".lines") ? getConfig().getStringList("group." + group + ".lines") : null);
		}
		for(String id : getConfig().getKeys("conditions")) {
			String condition = getConfig().getString("conditions." + id + ".condition", "");
			int cond = condition.indexOf("==");
			if(cond == -1)
				continue;
			ConditionScoreboardData data;
			conditions.add(data = new ConditionScoreboardData());
			data.setPlaceholder(condition.substring(0, cond));
			data.setRequestValue(condition.substring(cond + 2));
			data.setTitle(getConfig().getString("conditions." + id + ".title"));
			data.setLines(getConfig().existsKey("conditions." + id + ".lines") ? getConfig().getStringList("conditions." + id + ".lines") : null);
		}
		global = new ScoreboardData();
		global.setTitle(getConfig().getString("title"));
		global.setLines(getConfig().getStringList("lines"));
		refleshTaskId = new Tasker() {

			@Override
			public void run() {
				if(!isEnabled()) {
					taskId = 0;
					cancel();
					return;
				}
				if(data.isEmpty())
					return;

				for(UUID uuid : data.keySet()) {
					Player player = Bukkit.getPlayer(uuid);

					if(player == null || !player.isOnline())
						continue;

					UserScoreboardData userData = refreshConditions(player);

					userData.process(InternalPlaceholders.generatePlaceholders(player));
				}
			}
		}.runRepeating(12, Math.max(1, getConfig().getLong("settings.data-reflesh-every-ticks")));
		if(API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class)
			lpListener = new ScoreboardLP().register(this);
		else if(API.get().getPermissionHook().getClass() == VaultPermissionHook.class)
			taskId = new Tasker() {

				@Override
				public void run() {
					for(UserScoreboardData userData : data.values()) {
						Player player = userData.getPlayer();

						if(player == null || !player.isOnline())
							continue;

						refreshPermissionData(player, API.get().getPermissionHook().getGroup(player));
					}
				}
			}.runRepeating(100, 100);
		for(Player player : BukkitLoader.getOnlinePlayers())
			if(!disabledInWorlds.contains(player.getWorld().getName()))
				generateData(player);
	}

	@Override
	public void unregister() {
		if(refleshTaskId != 0)
			Scheduler.cancelTask(refleshTaskId);
		if(taskId != 0)
			Scheduler.cancelTask(taskId);
		if(!ScoreboardListener.data.isEmpty()) {
			for(UserScoreboardData data : ScoreboardListener.data.values())
				data.removeScoreboard();
			ScoreboardListener.data.clear();
		}
	}

	private BitSet evaluateConditions(Player player) {
		BitSet result = new BitSet(conditions.size());

		for(int i = 0; i < conditions.size(); ++i)
			if(conditions.get(i).canBeApplied(player))
				result.set(i);

		return result;
	}

	private UserScoreboardData createData(Player player, String group, BitSet activeConditions) {
		UserScoreboardData previous = data.get(player.getUniqueId());

		UserScoreboardData userData = new UserScoreboardData(player, group, previous != null && previous.isHidden());

		for(int index = activeConditions.nextSetBit(0); index >= 0; index = activeConditions.nextSetBit(index + 1)) {

			if(index >= conditions.size())
				break;

			userData.fillMissing(conditions.get(index));

			if(userData.isComplete())
				return userData.markModified();
		}

		PerWorldScoreboardData worldData;
		ScoreboardData scoreboard;

		if((worldData = perWorld.get(player.getWorld().getName())) != null) {

			if((scoreboard = worldData.perPlayer.get(player.getName())) != null) {
				userData.fillMissing(scoreboard);

				if(userData.isComplete())
					return userData.markModified();
			}

			if((scoreboard = worldData.perGroup.get(group)) != null) {
				userData.fillMissing(scoreboard);

				if(userData.isComplete())
					return userData.markModified();
			}

			userData.fillMissing(worldData);

			if(userData.isComplete())
				return userData.markModified();
		}

		if((scoreboard = perPlayer.get(player.getName())) != null) {
			userData.fillMissing(scoreboard);

			if(userData.isComplete())
				return userData.markModified();
		}

		if((scoreboard = perGroup.get(group)) != null) {
			userData.fillMissing(scoreboard);

			if(userData.isComplete())
				return userData.markModified();
		}

		userData.fillMissing(global);

		return userData.markModified();
	}

	public UserScoreboardData generateData(Player player) {
		BitSet conditionState = evaluateConditions(player);

		UserScoreboardData userData = createData(player, API.get().getPermissionHook().getGroup(player), conditionState);

		UUID uuid = player.getUniqueId();

		conditionStates.put(uuid, (BitSet) conditionState.clone());
		data.put(uuid, userData);

		return userData;
	}

	private UserScoreboardData refreshConditions(Player player) {
		UUID uuid = player.getUniqueId();

		UserScoreboardData current = data.get(uuid);

		if(current == null)
			return generateData(player);

		if(conditions.isEmpty())
			return current;

		BitSet currentState = evaluateConditions(player);
		BitSet previousState = conditionStates.get(uuid);

		if(previousState != null && previousState.equals(currentState))
			return current;

		UserScoreboardData updated = createData(player, API.get().getPermissionHook().getGroup(player), currentState);

		conditionStates.put(uuid, (BitSet) currentState.clone());
		data.put(uuid, updated);

		return updated;
	}

	public void refreshPermissionData(Player player, String group) {
		if(player == null || !player.isOnline())
			return;

		UUID uuid = player.getUniqueId();

		UserScoreboardData current = data.get(uuid);

		if(current == null) {
			generateData(player);
			return;
		}

		BitSet currentConditions = evaluateConditions(player);
		BitSet previousConditions = conditionStates.get(uuid);

		boolean conditionsChanged = previousConditions == null || !previousConditions.equals(currentConditions);

		boolean groupChanged = current.shouldUpdateData(group);

		if(!conditionsChanged && !groupChanged)
			return;

		UserScoreboardData updated = createData(player, group, currentConditions);

		conditionStates.put(uuid, (BitSet) currentConditions.clone());

		data.put(uuid, updated);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if(disabledInWorlds.contains(player.getWorld().getName()))
			return;

		generateData(player);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();

		conditionStates.remove(uuid);

		UserScoreboardData userData = data.remove(uuid);

		if(userData != null)
			userData.removeScoreboard();
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();

		if(disabledInWorlds.contains(player.getWorld().getName())) {

			conditionStates.remove(uuid);

			UserScoreboardData userData = data.remove(uuid);

			if(userData != null)
				userData.removeScoreboard();

			return;
		}

		generateData(player);
	}
}
