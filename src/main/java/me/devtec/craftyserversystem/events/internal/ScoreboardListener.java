package me.devtec.craftyserversystem.events.internal;

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
		if (lpListener != null) {
			lpListener.unregister();
			lpListener = null;
		}
		if (taskId != 0)
			Scheduler.cancelTask(taskId);
		if (refleshTaskId != 0)
			Scheduler.cancelTask(refleshTaskId);
		disabledInWorlds = getConfig().getStringList("disabled-in-worlds");
		for (String world : getConfig().getKeys("world")) {
			PerWorldScoreboardData pw;
			perWorld.put(world, pw = new PerWorldScoreboardData());
			pw.setTitle(getConfig().getString("world." + world + ".title"));
			pw.setLines(getConfig().existsKey("world." + world + ".lines")
					? getConfig().getStringList("world." + world + ".lines")
					: null);
			for (String player : getConfig().getKeys("world." + world + ".player")) {
				ScoreboardData data;
				pw.perPlayer.put(player, data = new ScoreboardData());
				data.setTitle(getConfig().getString("world." + world + ".player." + player + ".title"));
				data.setLines(getConfig().existsKey("world." + world + ".player." + player + ".lines")
						? getConfig().getStringList("world." + world + ".player." + player + ".lines")
						: null);
			}
			for (String group : getConfig().getKeys("world." + world + ".group")) {
				ScoreboardData data;
				pw.perGroup.put(group, data = new ScoreboardData());
				data.setTitle(getConfig().getString("world." + world + ".group." + group + ".title"));
				data.setLines(getConfig().existsKey("world." + world + ".group." + group + ".lines")
						? getConfig().getStringList("world." + world + ".group." + group + ".lines")
						: null);
			}
		}
		for (String player : getConfig().getKeys("player")) {
			ScoreboardData data;
			perPlayer.put(player, data = new ScoreboardData());
			data.setTitle(getConfig().getString("player." + player + ".title"));
			data.setLines(getConfig().existsKey("player." + player + ".lines")
					? getConfig().getStringList("player." + player + ".lines")
					: null);
		}
		for (String group : getConfig().getKeys("group")) {
			ScoreboardData data;
			perGroup.put(group, data = new ScoreboardData());
			data.setTitle(getConfig().getString("group." + group + ".title"));
			data.setLines(getConfig().existsKey("group." + group + ".lines")
					? getConfig().getStringList("group." + group + ".lines")
					: null);
		}
		for (String id : getConfig().getKeys("conditions")) {
			String condition = getConfig().getString("conditions." + id + ".condition", "");
			int cond = condition.indexOf("==");
			if (cond == -1)
				continue;
			ConditionScoreboardData data;
			conditions.add(data = new ConditionScoreboardData());
			data.setPlaceholder(condition.substring(0, cond));
			data.setRequestValue(condition.substring(cond + 2));
			data.setTitle(getConfig().getString("conditions." + id + ".title"));
			data.setLines(getConfig().existsKey("conditions." + id + ".lines")
					? getConfig().getStringList("conditions." + id + ".lines")
					: null);
		}
		global = new ScoreboardData();
		global.setTitle(getConfig().getString("title"));
		global.setLines(getConfig().getStringList("lines"));
		refleshTaskId = new Tasker() {

			@Override
			public void run() {
				if (!isEnabled()) {
					taskId = 0;
					cancel();
					return;
				}
				synchronized (data) {
					if (data.isEmpty())
						return;
					for (Entry<UUID, UserScoreboardData> entry : data.entrySet()) {
						Player player = Bukkit.getPlayer(entry.getKey());
						if (player == null || !player.isOnline())
							continue;
						entry.getValue().process(InternalPlaceholders.generatePlaceholders(player));
					}
				}
			}
		}.runRepeating(12, Math.max(1, getConfig().getLong("settings.data-reflesh-every-ticks")));
		if (API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class)
			lpListener = new ScoreboardLP().register(this);
		else if (API.get().getPermissionHook().getClass() == VaultPermissionHook.class)
			taskId = new Tasker() {

				@Override
				public void run() {
					for (UserScoreboardData userData : data.values())
						data.put(userData.getPlayer().getUniqueId(), generateData(userData.getPlayer()));
				}
			}.runRepeating(100, 100);
		for (Player player : BukkitLoader.getOnlinePlayers())
			data.put(player.getUniqueId(), generateData(player));
	}

	@Override
	public void unregister() {
		if (refleshTaskId != 0)
			Scheduler.cancelTask(refleshTaskId);
		if (taskId != 0)
			Scheduler.cancelTask(taskId);
		if (!ScoreboardListener.data.isEmpty()) {
			for (UserScoreboardData data : ScoreboardListener.data.values())
				data.removeScoreboard();
			ScoreboardListener.data.clear();
		}
	}

	public UserScoreboardData generateData(Player player) {
		String vaultGroup = API.get().getPermissionHook().getGroup(player);
		UserScoreboardData previous = data.get(player.getUniqueId());
		UserScoreboardData userData = new UserScoreboardData(player, vaultGroup,
				previous == null ? false : previous.isHidden());
		for (ConditionScoreboardData cond : conditions)
			if (cond.canBeApplied(player)) {
				userData.fillMissing(cond);
				if (userData.isComplete())
					return userData.markModified();
			}
		PerWorldScoreboardData pwData;
		ScoreboardData data;
		if ((pwData = perWorld.get(player.getWorld().getName())) != null) {
			if ((data = pwData.perPlayer.get(player.getName())) != null) {
				userData.fillMissing(data);
				if (userData.isComplete())
					return userData.markModified();
			}
			if ((data = pwData.perGroup.get(vaultGroup)) != null) {
				userData.fillMissing(data);
				if (userData.isComplete())
					return userData.markModified();
			}
			userData.fillMissing(pwData);
			if (userData.isComplete())
				return userData.markModified();
		}
		if ((data = perPlayer.get(player.getName())) != null) {
			userData.fillMissing(data);
			if (userData.isComplete())
				return userData.markModified();
		}
		if ((data = perGroup.get(vaultGroup)) != null) {
			userData.fillMissing(data);
			if (userData.isComplete())
				return userData.markModified();
		}
		userData.fillMissing(global);
		return userData.markModified();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (disabledInWorlds.contains(e.getPlayer().getWorld().getName()))
			return;
		data.put(e.getPlayer().getUniqueId(), generateData(e.getPlayer()));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		UserScoreboardData userData = data.remove(e.getPlayer().getUniqueId());
		if (userData != null)
			userData.removeScoreboard();
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		if (disabledInWorlds.contains(e.getPlayer().getWorld().getName())) {
			UserScoreboardData user;
			if ((user = data.remove(e.getPlayer().getUniqueId())) != null)
				user.removeScoreboard();
			return;
		}
		data.put(e.getPlayer().getUniqueId(), generateData(e.getPlayer()));
	}
}
