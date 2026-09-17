package me.devtec.craftyserversystem.events.internal;

import java.util.BitSet;
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
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;

public class TablistListener implements CssListener {

	private final Map<String, PerWorldTablistData> perWorld = new HashMap<>();
	private final Map<String, TablistData> perGroup = new HashMap<>();
	private final Map<String, TablistData> perPlayer = new HashMap<>();
	private final List<ConditionTablistData> conditions = new LinkedList<>();

	public static final Map<UUID, UserTablistData> data = new ConcurrentHashMap<>();

	/*
	 * Stav conditions z posledního vyhodnocení.
	 *
	 * Nový UserTablistData vytvoříme pouze tehdy,
	 * když se některá condition skutečně změní.
	 */
	private final Map<UUID, BitSet> conditionStates = new ConcurrentHashMap<>();

	/*
	 * Aktuální permission group.
	 *
	 * Díky tomu nemusíme při každém refresh ticku
	 * znovu sahat do Vault/LuckPerms.
	 */
	private final Map<UUID, String> groupStates = new ConcurrentHashMap<>();

	private TablistData global;

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
		/*
		 * Nejdřív odstraníme existující tablist data,
		 * aby po reloadu nezůstalo něco starého zobrazené.
		 */
		if(!data.isEmpty())
			for(UserTablistData userData : data.values())
				userData.removeTablist();

		perWorld.clear();
		perGroup.clear();
		perPlayer.clear();
		conditions.clear();

		data.clear();
		conditionStates.clear();
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

		TabAPI.register();
		NametagManagerAPI.get().load();

		disabledInWorlds = getConfig().getStringList("disabled-in-worlds");

		for(String world : getConfig().getKeys("world")) {
			PerWorldTablistData worldData = new PerWorldTablistData();

			perWorld.put(world, worldData);

			fill(worldData, "world." + world + ".");

			for(String player : getConfig().getKeys("world." + world + ".player")) {
				TablistData tablistData = new TablistData();

				worldData.perPlayer.put(player, tablistData);

				fill(tablistData, "world." + world + ".player." + player + ".");
			}

			for(String group : getConfig().getKeys("world." + world + ".group")) {
				TablistData tablistData = new TablistData();

				worldData.perGroup.put(group, tablistData);

				fill(tablistData, "world." + world + ".group." + group + ".");
			}
		}

		for(String player : getConfig().getKeys("player")) {
			TablistData tablistData = new TablistData();

			perPlayer.put(player, tablistData);

			fill(tablistData, "player." + player + ".");
		}

		for(String group : getConfig().getKeys("group")) {
			TablistData tablistData = new TablistData();

			perGroup.put(group, tablistData);

			fill(tablistData, "group." + group + ".");
		}

		for(String id : getConfig().getKeys("conditions")) {
			String condition = getConfig().getString("conditions." + id + ".condition", "");

			int separator = condition.indexOf("==");

			if(separator == -1)
				continue;

			ConditionTablistData conditionData = new ConditionTablistData();

			conditionData.setPlaceholder(condition.substring(0, separator));

			conditionData.setRequestValue(condition.substring(separator + 2));

			fill(conditionData, "conditions." + id + ".");

			conditions.add(conditionData);
		}

		global = new TablistData();

		fill(global, "");

		if(API.get().getPermissionHook().getClass() == LuckPermsPermissionHook.class) {
			lpListener = new TablistLP().register(this);
		} else {
			/*
			 * Vault a ostatní hooky většinou nemají event.
			 *
			 * Pouze periodicky zjistíme group a rebuild
			 * provedeme jen pokud se opravdu změnila.
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

		refleshTaskId = new Tasker() {

			@Override
			public void run() {
				for(UserTablistData current : data.values()) {
					Player player = current.getPlayer();

					if(player == null || !player.isOnline())
						continue;

					/*
					 * Conditiony se mohou měnit bez:
					 *
					 * - world change,
					 * - permission group change,
					 * - LuckPerms eventu.
					 *
					 * Proto je při datovém refreshi levně porovnáme.
					 */
					UserTablistData userData = refreshConditions(player);

					if(userData != null)
						userData.process(InternalPlaceholders.generatePlaceholders(player));
				}
			}
		}.runRepeating(8, Math.max(1, getConfig().getLong("data-reflesh-every-ticks")));

		for(Player player : BukkitLoader.getOnlinePlayers()) {
			if(isDisabled(player))
				continue;

			UserTablistData userData = generateData(player);

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

		if(NametagManagerAPI.get().isLoaded())
			NametagManagerAPI.get().unload();

		if(!data.isEmpty())
			for(UserTablistData userData : data.values())
				userData.removeTablist();

		data.clear();
		conditionStates.clear();
		groupStates.clear();
	}

	private void fill(TablistData data, String path) {
		data.setHeader(getConfig().existsKey(path + "header") ? getConfig().getStringList(path + "header") : null);

		data.setFooter(getConfig().existsKey(path + "footer") ? getConfig().getStringList(path + "footer") : null);

		data.setTabNameFormat(getConfig().getString(path + "tab.format"));

		data.setTabPrefix(getConfig().getString(path + "tab.prefix"));

		data.setTabSuffix(getConfig().getString(path + "tab.suffix"));

		if(!getConfig().getStringList(path + "tag.lines").isEmpty())
			data.setNametagLines(getConfig().getStringList(path + "tag.lines"));

		data.setTagPrefix(getConfig().getString(path + "tag.prefix"));

		data.setTagSuffix(getConfig().getString(path + "tag.suffix"));

		data.setYellowNumberText(getConfig().getString(path + "yellowNumber.value"));

		String yellowNumberDisplay = getConfig().getString(path + "yellowNumber.displayAs");

		if(yellowNumberDisplay != null)
			data.setDisplayYellowNumberMode(YellowNumberDisplayMode.valueOf(yellowNumberDisplay));
	}

	private boolean isDisabled(Player player) {
		return disabledInWorlds != null && disabledInWorlds.contains(player.getWorld().getName());
	}

	private String normalizeGroup(String group) {
		return group == null ? "" : group;
	}

	private BitSet evaluateConditions(Player player) {
		BitSet result = new BitSet(conditions.size());

		for(int i = 0; i < conditions.size(); ++i)
			if(conditions.get(i).canBeApplied(player))
				result.set(i);

		return result;
	}

	private UserTablistData createData(Player player, String group, BitSet activeConditions) {

		UserTablistData previous = data.get(player.getUniqueId());

		UserTablistData userData = previous == null ? new UserTablistData(player) : new UserTablistData(player, previous);

		/*
		 * Conditions mají nejvyšší prioritu.
		 */
		for(int index = activeConditions.nextSetBit(0); index >= 0; index = activeConditions.nextSetBit(index + 1)) {

			if(index >= conditions.size())
				break;

			userData.fillMissing(conditions.get(index));

			if(userData.isComplete())
				return userData;
		}

		PerWorldTablistData worldData;
		TablistData tablistData;

		if((worldData = perWorld.get(player.getWorld().getName())) != null) {
			if((tablistData = worldData.perPlayer.get(player.getName())) != null) {
				userData.fillMissing(tablistData);

				if(userData.isComplete())
					return userData;
			}

			if((tablistData = worldData.perGroup.get(group)) != null) {
				userData.fillMissing(tablistData);

				if(userData.isComplete())
					return userData;
			}

			userData.fillMissing(worldData);

			if(userData.isComplete())
				return userData;
		}

		if((tablistData = perPlayer.get(player.getName())) != null) {
			userData.fillMissing(tablistData);

			if(userData.isComplete())
				return userData;
		}

		if((tablistData = perGroup.get(group)) != null) {
			userData.fillMissing(tablistData);

			if(userData.isComplete())
				return userData;
		}

		userData.fillMissing(global);

		return userData;
	}

	public UserTablistData generateData(Player player) {
		String group = normalizeGroup(API.get().getPermissionHook().getGroup(player));

		BitSet activeConditions = evaluateConditions(player);

		UserTablistData userData = createData(player, group, activeConditions);

		UUID uuid = player.getUniqueId();

		groupStates.put(uuid, group);

		conditionStates.put(uuid, (BitSet) activeConditions.clone());

		data.put(uuid, userData);

		return userData;
	}

	private UserTablistData refreshConditions(Player player) {
		UUID uuid = player.getUniqueId();

		if(isDisabled(player)) {
			removePlayerData(player);
			return null;
		}

		UserTablistData current = data.get(uuid);

		if(current == null)
			return generateData(player);

		if(conditions.isEmpty())
			return current;

		BitSet activeConditions = evaluateConditions(player);

		BitSet previousConditions = conditionStates.get(uuid);

		if(previousConditions != null && previousConditions.equals(activeConditions))
			return current;

		String group = groupStates.get(uuid);

		if(group == null) {
			group = normalizeGroup(API.get().getPermissionHook().getGroup(player));

			groupStates.put(uuid, group);
		}

		UserTablistData updated = createData(player, group, activeConditions);

		conditionStates.put(uuid, (BitSet) activeConditions.clone());

		data.put(uuid, updated);

		return updated;
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

		UserTablistData current = data.get(uuid);

		if(current == null) {
			generateData(player);
			return;
		}

		BitSet activeConditions = evaluateConditions(player);

		BitSet previousConditions = conditionStates.get(uuid);

		String previousGroup = groupStates.get(uuid);

		boolean groupChanged = previousGroup == null || !previousGroup.equals(normalizedGroup);

		boolean conditionsChanged = previousConditions == null || !previousConditions.equals(activeConditions);

		if(!groupChanged && !conditionsChanged)
			return;

		UserTablistData updated = createData(player, normalizedGroup, activeConditions);

		groupStates.put(uuid, normalizedGroup);

		conditionStates.put(uuid, (BitSet) activeConditions.clone());

		data.put(uuid, updated);
	}

	private void removePlayerData(Player player) {
		UUID uuid = player.getUniqueId();

		groupStates.remove(uuid);
		conditionStates.remove(uuid);

		UserTablistData userData = data.remove(uuid);

		if(userData != null)
			userData.removeTablist();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		TabAPI.getHolder(player).afterConnection();

		if(isDisabled(player))
			return;

		UserTablistData userData = generateData(player);

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

		UserTablistData userData = generateData(player);

		userData.process(InternalPlaceholders.generatePlaceholders(player));
	}
}
