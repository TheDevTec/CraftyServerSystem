package me.devtec.craftyserversystem.events.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.events.internal.supportlp.TablistLP;
import me.devtec.craftyserversystem.permission.LuckPermsPermissionHook;
import me.devtec.craftyserversystem.permission.VaultPermissionHook;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
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
			global = new TablistData();
			fill(global, "");

			if (API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class)
				lpListener = new TablistLP().register(this);
			else if (API.get().getPermissionHook().getClass() == VaultPermissionHook.class)
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
					for (UserTablistData userData : data.values()) {
						Location loc = userData.getPlayer().getLocation();
						userData.process(PlaceholdersExecutor.i().papi(userData.getPlayer().getUniqueId()).add("player", userData.getPlayer().getName())
								.add("ping", BukkitLoader.getNmsProvider().getPing(userData.getPlayer())).add("online", BukkitLoader.getOnlinePlayers().size())
								.add("max_players", Bukkit.getMaxPlayers())
								.add("balance", API.get().getEconomyHook().format(API.get().getEconomyHook().getBalance(userData.getPlayer().getName(), userData.getPlayer().getWorld().getName())))
								.add("health", userData.getPlayer().getHealth()).add("food", userData.getPlayer().getFoodLevel()).add("x", loc.getX()).add("y", loc.getY()).add("z", loc.getZ())
								.add("world", loc.getWorld().getName()));
					}
				}
			}.runRepeating(8, Math.max(1, getConfig().getLong("data-reflesh-every-ticks")));
			for (Player player : BukkitLoader.getOnlinePlayers()) {
				Location loc = player.getLocation();
				data.put(player.getUniqueId(),
						generateData(player).process(PlaceholdersExecutor.i().papi(player.getUniqueId()).add("player", player.getName())
								.add("money", API.get().getEconomyHook().format(API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName()))).add("health", player.getHealth())
								.add("food", player.getFoodLevel()).add("x", loc.getX()).add("y", loc.getY()).add("z", loc.getZ()).add("world", loc.getWorld().getName())));
			}
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
		if (disabledInWorlds.contains(e.getPlayer().getWorld().getName()))
			return;
		Player player = e.getPlayer();
		Location loc = player.getLocation();
		data.put(player.getUniqueId(),
				generateData(e.getPlayer()).process(PlaceholdersExecutor.i().papi(player.getUniqueId()).add("player", player.getName())
						.add("money", API.get().getEconomyHook().format(API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName()))).add("health", player.getHealth())
						.add("food", player.getFoodLevel()).add("x", loc.getX()).add("y", loc.getY()).add("z", loc.getZ()).add("world", loc.getWorld().getName())));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		UserTablistData user;
		if ((user = data.remove(e.getPlayer().getUniqueId())) != null)
			user.removeTablist();
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		if (disabledInWorlds.contains(e.getPlayer().getWorld().getName())) {
			UserTablistData user;
			if ((user = data.remove(e.getPlayer().getUniqueId())) != null)
				user.removeTablist();
			return;
		}
		Player player = e.getPlayer();
		Location loc = player.getLocation();
		data.put(player.getUniqueId(),
				generateData(e.getPlayer()).process(PlaceholdersExecutor.i().papi(player.getUniqueId()).add("player", player.getName())
						.add("money", API.get().getEconomyHook().format(API.get().getEconomyHook().getBalance(player.getName(), player.getWorld().getName()))).add("health", player.getHealth())
						.add("food", player.getFoodLevel()).add("x", loc.getX()).add("y", loc.getY()).add("z", loc.getZ()).add("world", loc.getWorld().getName())));
	}
}
