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

	private final Map<String, PerWorldBossBarData> perWorld = new HashMap<>();
	private final Map<String, BossBarData> perGroup = new HashMap<>();
	private final Map<String, BossBarData> perPlayer = new HashMap<>();

	public static final Map<UUID, UserBossBarData> data = new ConcurrentHashMap<>();

	/*
	 * BossBar nemá conditions, takže nám pro strukturální rebuild
	 * stačí sledovat permission group.
	 */
	private final Map<UUID, String> groupStates = new ConcurrentHashMap<>();

	private BossBarData global;

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
		if(!data.isEmpty())
			for(UserBossBarData userData : data.values())
				userData.removeBossBar();

		perWorld.clear();
		perGroup.clear();
		perPlayer.clear();

		data.clear();
		groupStates.clear();

		if(lpListener != null) {
			lpListener.unregister();
			lpListener = null;
		}

		if(taskId != 0) {
			Scheduler.cancelTask(taskId);
			taskId = 0;
		}

		if(refleshTaskId != 0) {
			Scheduler.cancelTask(refleshTaskId);
			refleshTaskId = 0;
		}

		if(NametagManagerAPI.get().isLoaded())
			NametagManagerAPI.get().unload();

		if(!isEnabled())
			return;

		NametagManagerAPI.get().load();

		disabledInWorlds = getConfig().getStringList("disabled-in-worlds");

		for(String world : getConfig().getKeys("world")) {
			PerWorldBossBarData worldData = new PerWorldBossBarData();

			perWorld.put(world, worldData);

			fill(worldData, "world." + world + ".");

			for(String player : getConfig().getKeys("world." + world + ".player")) {
				BossBarData bossBarData = new BossBarData();

				worldData.perPlayer.put(player, bossBarData);

				fill(bossBarData, "world." + world + ".player." + player + ".");
			}

			for(String group : getConfig().getKeys("world." + world + ".group")) {
				BossBarData bossBarData = new BossBarData();

				worldData.perGroup.put(group, bossBarData);

				fill(bossBarData, "world." + world + ".group." + group + ".");
			}
		}

		for(String player : getConfig().getKeys("player")) {
			BossBarData bossBarData = new BossBarData();

			perPlayer.put(player, bossBarData);

			fill(bossBarData, "player." + player + ".");
		}

		for(String group : getConfig().getKeys("group")) {
			BossBarData bossBarData = new BossBarData();

			perGroup.put(group, bossBarData);

			fill(bossBarData, "group." + group + ".");
		}

		global = new BossBarData();

		fill(global, "");

		if(API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class) {
			lpListener = new BossBarLP().register(this);
		} else {
			/*
			 * Vault nemá event pro změnu group.
			 *
			 * Periodicky group zjistíme, ale rebuild
			 * provedeme pouze pokud se změnila.
			 *
			 * Iterujeme online hráče, nikoli data.values(),
			 * protože hráč momentálně nemusí mít žádný bossbar.
			 */
			taskId = new Tasker() {

				@Override
				public void run() {
					for(Player player : BukkitLoader.getOnlinePlayers()) {
						if(isDisabled(player))
							continue;

						refreshPermissionData(player, API.get().getPermissionHook().getGroup(player));
					}
				}
			}.runRepeating(100, 100);
		}

		/*
		 * Běžný refresh už NEREGENERUJE strukturu.
		 *
		 * Pouze přepočítává placeholders text/progressu.
		 */
		refleshTaskId = new Tasker() {

			@Override
			public void run() {
				for(UserBossBarData userData : data.values()) {
					Player player = userData.getPlayer();

					if(player == null || !player.isOnline())
						continue;

					userData.process(InternalPlaceholders.generatePlaceholders(player));
				}
			}
		}.runRepeating(8, Math.max(1, getConfig().getLong("data-reflesh-every-ticks")));

		for(Player player : BukkitLoader.getOnlinePlayers()) {
			if(isDisabled(player))
				continue;

			UserBossBarData userData = generateData(player);

			if(userData != null)
				userData.process(InternalPlaceholders.generatePlaceholders(player));
		}
	}

	@Override
	public void unregister() {
		if(refleshTaskId != 0) {
			Scheduler.cancelTask(refleshTaskId);
			refleshTaskId = 0;
		}

		if(taskId != 0) {
			Scheduler.cancelTask(taskId);
			taskId = 0;
		}

		if(lpListener != null) {
			lpListener.unregister();
			lpListener = null;
		}

		if(!data.isEmpty())
			for(UserBossBarData userData : data.values())
				userData.removeBossBar();

		data.clear();
		groupStates.clear();
	}

	private void fill(BossBarData data, String path) {
		data.setText(getConfig().existsKey(path + "text") ? getConfig().getString(path + "text") : null);

		data.setProgress(getConfig().existsKey(path + "progress") ? getConfig().getString(path + "progress") : null);

		String style = getConfig().getString(path + "style");

		if(style != null)
			try {
				data.setStyle(Style.valueOf(style.toUpperCase()));
			} catch(Exception | NoSuchFieldError exception) {

				Loader.getPlugin().getLogger().warning("[BossBar] Failed to load bossbar '" + path + "' - Style " + style + " doesn't exist! Valid styles are: " + Arrays.asList(Style.values()));
			}

		String color = getConfig().getString(path + "color");

		if(color != null)
			try {
				data.setColor(Color.valueOf(color.toUpperCase()));
			} catch(Exception | NoSuchFieldError exception) {

				Loader.getPlugin().getLogger().warning("[BossBar] Failed to load bossbar '" + path + "' - Color " + color + " doesn't exist! Valid colors are: " + Arrays.asList(Color.values()));
			}
	}

	private boolean isDisabled(Player player) {
		return disabledInWorlds != null && disabledInWorlds.contains(player.getWorld().getName());
	}

	private String normalizeGroup(String group) {
		return group == null ? "" : group;
	}

	/*
	 * Zásadní pravidlo:
	 *
	 * BossBarData bez textu není částečná konfigurace.
	 * Je kompletně ignorován.
	 *
	 * Takže například:
	 *
	 * group:
	 * vip:
	 * color: RED
	 *
	 * bez textu NEOVLIVNÍ globální bossbar.
	 */
	private boolean canUse(BossBarData bossBarData) {
		if(bossBarData == null)
			return false;

		String text = bossBarData.getText();

		return text != null && !text.trim().isEmpty();
	}

	private UserBossBarData createData(Player player, String group) {

		UserBossBarData previous = data.get(player.getUniqueId());

		UserBossBarData userData = new UserBossBarData(player, group, previous != null && previous.isHidden(), previous == null ? null : previous.getBossBar());

		boolean hasBossBar = false;

		PerWorldBossBarData worldData;
		BossBarData bossBarData;

		if((worldData = perWorld.get(player.getWorld().getName())) != null) {
			if((bossBarData = worldData.perPlayer.get(player.getName())) != null && canUse(bossBarData)) {

				hasBossBar = true;

				userData.fillMissing(bossBarData);

				if(userData.isComplete())
					return userData;
			}

			if((bossBarData = worldData.perGroup.get(group)) != null && canUse(bossBarData)) {

				hasBossBar = true;

				userData.fillMissing(bossBarData);

				if(userData.isComplete())
					return userData;
			}

			/*
			 * PerWorldBossBarData samotný je také BossBarData.
			 *
			 * Pokud nemá text, ignorujeme ho úplně.
			 */
			if(canUse(worldData)) {
				hasBossBar = true;

				userData.fillMissing(worldData);

				if(userData.isComplete())
					return userData;
			}
		}

		if((bossBarData = perPlayer.get(player.getName())) != null && canUse(bossBarData)) {

			hasBossBar = true;

			userData.fillMissing(bossBarData);

			if(userData.isComplete())
				return userData;
		}

		if((bossBarData = perGroup.get(group)) != null && canUse(bossBarData)) {

			hasBossBar = true;

			userData.fillMissing(bossBarData);

			if(userData.isComplete())
				return userData;
		}

		if(canUse(global)) {
			hasBossBar = true;

			userData.fillMissing(global);
		}

		/*
		 * Nikde nebyl žádný BossBarData s neprázdným textem.
		 *
		 * Hráč tedy nemá mít žádný bossbar.
		 */
		return hasBossBar ? userData : null;
	}

	public UserBossBarData generateData(Player player) {
		UUID uuid = player.getUniqueId();

		String group = normalizeGroup(API.get().getPermissionHook().getGroup(player));

		UserBossBarData newData = createData(player, group);

		groupStates.put(uuid, group);

		replaceData(player, newData);

		return newData;
	}

	public void refreshPermissionData(Player player, String group) {

		if(player == null || !player.isOnline())
			return;

		if(isDisabled(player)) {
			removePlayerData(player);
			return;
		}

		UUID uuid = player.getUniqueId();

		String normalizedGroup = normalizeGroup(group);

		String previousGroup = groupStates.get(uuid);

		/*
		 * Group se nezměnila.
		 *
		 * Prefix/meta/placeholdery se mohou změnit,
		 * ale ty řeší běžný process(placeholders).
		 *
		 * Není potřeba přestavovat BossBarData.
		 */
		if(previousGroup != null && previousGroup.equals(normalizedGroup))
			return;

		UserBossBarData newData = createData(player, normalizedGroup);

		groupStates.put(uuid, normalizedGroup);

		replaceData(player, newData);
	}

	private void replaceData(Player player, UserBossBarData newData) {

		UUID uuid = player.getUniqueId();

		if(newData != null) {
			data.put(uuid, newData);

			return;
		}

		/*
		 * Nová konfigurace neobsahuje žádný použitelný bossbar.
		 *
		 * Starý tedy musíme fyzicky odstranit.
		 */
		UserBossBarData previous = data.remove(uuid);

		if(previous != null)
			previous.removeBossBar();
	}

	private void removePlayerData(Player player) {
		UUID uuid = player.getUniqueId();

		groupStates.remove(uuid);

		UserBossBarData userData = data.remove(uuid);

		if(userData != null)
			userData.removeBossBar();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if(isDisabled(player))
			return;

		UserBossBarData userData = generateData(player);

		if(userData != null)
			userData.process(InternalPlaceholders.generatePlaceholders(player));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		removePlayerData(event.getPlayer());
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();

		if(isDisabled(player)) {
			removePlayerData(player);
			return;
		}

		UserBossBarData userData = generateData(player);

		if(userData != null)
			userData.process(InternalPlaceholders.generatePlaceholders(player));
	}
}
