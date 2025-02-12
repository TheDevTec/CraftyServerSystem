package me.devtec.craftyserversystem.events.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.events.internal.supportlp.BossBarLP;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.utils.InternalPlaceholders;
import me.devtec.craftyserversystem.utils.bossbar.BossBarData;
import me.devtec.craftyserversystem.utils.bossbar.BossBarEmulator.Color;
import me.devtec.craftyserversystem.utils.bossbar.BossBarEmulator.Style;
import me.devtec.craftyserversystem.utils.bossbar.PerWorldBossBarData;
import me.devtec.craftyserversystem.utils.bossbar.UserBossBarData;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;

public class BossBarListener implements CssListener {

	private Map<String, PerWorldBossBarData> perWorld = new HashMap<>();
	private Map<String, BossBarData> perGroup = new HashMap<>();
	private Map<String, BossBarData> perPlayer = new HashMap<>();
	private BossBarData global;
	public static Map<UUID, UserBossBarData> data = new ConcurrentHashMap<>();
	private int taskId;
	private int refleshTaskId;
	private BossBarLP lpListener;
	private List<String> disabledInWorlds;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getBossBar();
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
				PerWorldBossBarData pw;
				perWorld.put(world, pw = new PerWorldBossBarData());
				fill(pw, "world." + world + ".");
				for (String player : getConfig().getKeys("world." + world + ".player")) {
					BossBarData data;
					pw.perPlayer.put(player, data = new BossBarData());
					fill(data, "world." + world + ".player." + player + ".");
				}
				for (String group : getConfig().getKeys("world." + world + ".group")) {
					BossBarData data;
					pw.perGroup.put(group, data = new BossBarData());
					fill(data, "world." + world + ".group." + group + ".");
				}
			}
			for (String player : getConfig().getKeys("player")) {
				BossBarData data;
				perPlayer.put(player, data = new BossBarData());
				fill(data, "player." + player + ".");
			}
			for (String group : getConfig().getKeys("group")) {
				BossBarData data;
				perGroup.put(group, data = new BossBarData());
				fill(data, "group." + group + ".");
			}
			global = new BossBarData();
			fill(global, "");

			if (API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class)
				lpListener = new BossBarLP().register(this);
			else
				taskId = new Tasker() {

					@Override
					public void run() {
						for (UserBossBarData userData : data.values())
							data.put(userData.getPlayer().getUniqueId(), generateData(userData.getPlayer()));
					}
				}.runRepeating(100, 100);
			refleshTaskId = new Tasker() {

				@Override
				public void run() {
					for (UserBossBarData userData : data.values())
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
		if (!BossBarListener.data.isEmpty()) {
			for (UserBossBarData data : BossBarListener.data.values())
				data.removeBossBar();
			BossBarListener.data.clear();
		}
	}

	private void fill(BossBarData data, String path) {
		data.setText(getConfig().existsKey(path + "text") ? getConfig().getString(path + "text") : null);
		data.setProgress(getConfig().existsKey(path + "progress") ? getConfig().getString(path + "progress") : null);
		String style;
		if ((style = getConfig().getString(path + "style")) != null)
			try {
				data.setStyle(Style.valueOf(style.toUpperCase()));
			} catch (Exception | NoSuchFieldError e) {
				Loader.getPlugin().getLogger().warning("[BossBar] Failed to load bossbar '" + path + "' - Style "
						+ style + " doesn't exist! Valid styles are: " + Arrays.asList(Style.values()));
			}
		String color;
		if ((color = getConfig().getString(path + "color")) != null)
			try {
				data.setColor(Color.valueOf(color.toUpperCase()));
			} catch (Exception | NoSuchFieldError e) {
				Loader.getPlugin().getLogger().warning("[BossBar] Failed to load bossbar '" + path + "' - Color "
						+ color + " doesn't exist! Valid colors are: " + Arrays.asList(Color.values()));
			}
	}

	public UserBossBarData generateData(Player player) {
		String vaultGroup = API.get().getPermissionHook().getGroup(player);
		UserBossBarData previous = data.get(player.getUniqueId());
		UserBossBarData userData = new UserBossBarData(player, vaultGroup,
				previous == null ? false : previous.isHidden(), previous == null ? null : previous.getBossBar());
		PerWorldBossBarData pwData;
		BossBarData data;
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
		BukkitLoader.getNmsProvider().postToMainThread(() -> NametagManagerAPI.get().getPlayer(player).afterJoin());
		if (disabledInWorlds.contains(player.getWorld().getName()))
			return;
		data.put(player.getUniqueId(), generateData(player).process(InternalPlaceholders.generatePlaceholders(player)));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		UserBossBarData user;
		if ((user = data.remove(e.getPlayer().getUniqueId())) != null)
			user.removeBossBar();
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		Player player = e.getPlayer();
		if (disabledInWorlds.contains(player.getWorld().getName())) {
			UserBossBarData user;
			if ((user = data.remove(player.getUniqueId())) != null)
				user.removeBossBar();
			return;
		}
		data.put(player.getUniqueId(), generateData(player).process(InternalPlaceholders.generatePlaceholders(player)));
	}
}
