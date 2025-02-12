package me.devtec.craftyserversystem.events.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.events.internal.supportlp.TablistLP;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.utils.InternalPlaceholders;
import me.devtec.craftyserversystem.utils.tablist.ConditionTablistData;
import me.devtec.craftyserversystem.utils.tablist.PerWorldTablistData;
import me.devtec.craftyserversystem.utils.tablist.TablistData;
import me.devtec.craftyserversystem.utils.tablist.UserTablistData;
import me.devtec.craftyserversystem.utils.tablist.YellowNumberDisplayMode;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;

public class TablistListener implements CssListener {

	private Map<String, PerWorldTablistData> perWorld = new HashMap<>();
	private Map<String, TablistData> perGroup = new HashMap<>();
	private Map<String, TablistData> perPlayer = new HashMap<>();
	private TablistData global;
	public static Map<UUID, UserTablistData> data = new ConcurrentHashMap<>();
	private List<ConditionTablistData> conditions = new LinkedList<>();
	private int taskId;
	private int refleshTaskId;
	private TablistLP lpListener;
	private List<String> disabledInWorlds;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getTab();
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
		Scheduler.cancelTask(refleshTaskId);
		if (NametagManagerAPI.get().isLoaded())
			NametagManagerAPI.get().unload();
		if (isEnabled()) {
			NametagManagerAPI.get().load();
			disabledInWorlds = getConfig().getStringList("disabled-in-worlds");
			for (String world : getConfig().getKeys("world")) {
				PerWorldTablistData pw;
				perWorld.put(world, pw = new PerWorldTablistData());
				fill(pw, "world." + world + ".");
				for (String player : getConfig().getKeys("world." + world + ".player")) {
					TablistData data;
					pw.perPlayer.put(player, data = new TablistData());
					fill(data, "world." + world + ".player." + player + ".");
				}
				for (String group : getConfig().getKeys("world." + world + ".group")) {
					TablistData data;
					pw.perGroup.put(group, data = new TablistData());
					fill(data, "world." + world + ".group." + group + ".");
				}
			}
			for (String player : getConfig().getKeys("player")) {
				TablistData data;
				perPlayer.put(player, data = new TablistData());
				fill(data, "player." + player + ".");
			}
			for (String group : getConfig().getKeys("group")) {
				TablistData data;
				perGroup.put(group, data = new TablistData());
				fill(data, "group." + group + ".");
			}
			for (String id : getConfig().getKeys("conditions")) {
				String condition = getConfig().getString("conditions." + id + ".condition", "");
				int cond = condition.indexOf("==");
				if (cond == -1)
					continue;
				ConditionTablistData data;
				conditions.add(data = new ConditionTablistData());
				data.setPlaceholder(condition.substring(0, cond));
				data.setRequestValue(condition.substring(cond + 2));
				fill(data, "conditions." + id + ".");
			}
			global = new TablistData();
			fill(global, "");

			if (API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class)
				lpListener = new TablistLP().register(this);
			else
				taskId = new Tasker() {

					@Override
					public void run() {
						for (UserTablistData userData : data.values())
							data.put(userData.getPlayer().getUniqueId(), generateData(userData.getPlayer()));
					}
				}.runRepeating(100, 100);
			refleshTaskId = new Tasker() {

				@Override
				public void run() {
					for (UserTablistData userData : data.values())
						userData.process(InternalPlaceholders.generatePlaceholders(userData.getPlayer()));
				}
			}.runRepeating(8, Math.max(1, getConfig().getLong("data-reflesh-every-ticks")));
			for (Player player : BukkitLoader.getOnlinePlayers())
				data.put(player.getUniqueId(),
						generateData(player).process(InternalPlaceholders.generatePlaceholders(player)));
		}
	}

	@Override
	public void unregister() {
		if (refleshTaskId != 0)
			Scheduler.cancelTask(refleshTaskId);
		if (taskId != 0)
			Scheduler.cancelTask(taskId);
		if (NametagManagerAPI.get().isLoaded())
			NametagManagerAPI.get().unload();
		if (!TablistListener.data.isEmpty()) {
			for (UserTablistData data : TablistListener.data.values())
				data.removeTablist();
			TablistListener.data.clear();
		}
	}

	private void fill(TablistData data, String path) {
		data.setHeader(getConfig().existsKey(path + "header") ? getConfig().getStringList(path + "header") : null);
		data.setFooter(getConfig().existsKey(path + "footer") ? getConfig().getStringList(path + "footer") : null);
		data.setTabNameFormat(getConfig().getString(path + "tab.format"));
		data.setTabPrefix(getConfig().getString(path + "tab.prefix"));
		data.setTabSuffix(getConfig().getString(path + "tab.suffix"));
		data.setTagNameFormat(getConfig().getString(path + "tag.format"));
		data.setTagPrefix(getConfig().getString(path + "tag.prefix"));
		data.setTagSuffix(getConfig().getString(path + "tag.suffix"));
		data.setYellowNumberText(getConfig().getString(path + "yellowNumber.value"));
		String yellowNumberDisplay;
		if ((yellowNumberDisplay = getConfig().getString(path + "yellowNumber.displayAs")) != null)
			data.setDisplayYellowNumberMode(YellowNumberDisplayMode.valueOf(yellowNumberDisplay));
	}

	public UserTablistData generateData(Player player) {
		String vaultGroup = API.get().getPermissionHook().getGroup(player);
		UserTablistData user = data.get(player.getUniqueId());
		UserTablistData userData = user == null ? new UserTablistData(player) : new UserTablistData(player, user);
		for (ConditionTablistData cond : conditions)
			if (cond.canBeApplied(player)) {
				userData.fillMissing(cond);
				if (userData.isComplete())
					return userData;
			}
		PerWorldTablistData pwData;
		TablistData data;
		if ((pwData = perWorld.get(player.getWorld().getName())) != null) {
			if ((data = pwData.perPlayer.get(player.getName())) != null) {
				userData.fillMissing(data);
				if (userData.isComplete())
					return userData;
			}
			if ((data = pwData.perGroup.get(vaultGroup)) != null) {
				userData.fillMissing(data);
				if (userData.isComplete())
					return userData;
			}
			userData.fillMissing(pwData);
			if (userData.isComplete())
				return userData;
		}
		if ((data = perPlayer.get(player.getName())) != null) {
			userData.fillMissing(data);
			if (userData.isComplete())
				return userData;
		}
		if ((data = perGroup.get(vaultGroup)) != null) {
			userData.fillMissing(data);
			if (userData.isComplete())
				return userData;
		}
		userData.fillMissing(global);
		return userData;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		new Tasker() {

			@Override
			public void run() {
				BukkitLoader.getNmsProvider()
						.postToMainThread(() -> NametagManagerAPI.get().getPlayer(player).afterJoin());
				if (disabledInWorlds.contains(player.getWorld().getName()))
					return;
				data.put(player.getUniqueId(),
						generateData(player).process(InternalPlaceholders.generatePlaceholders(player)));
			}
		}.runLater(20);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		UserTablistData user;
		if ((user = data.remove(e.getPlayer().getUniqueId())) != null)
			user.removeTablist();
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		Player player = e.getPlayer();
		if (disabledInWorlds.contains(player.getWorld().getName())) {
			UserTablistData user;
			if ((user = data.remove(player.getUniqueId())) != null)
				user.removeTablist();
			return;
		}
		data.put(player.getUniqueId(), generateData(player).process(InternalPlaceholders.generatePlaceholders(player)));
	}
}
