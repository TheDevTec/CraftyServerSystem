package me.devtec.craftyserversystem.commands.internal.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.shared.API;
import me.devtec.theapi.bukkit.game.Position;

@IgnoredClass
public class HomeManager {

	private static HomeManager instance;
	private Map<String, Integer> groups = new HashMap<>();

	public static HomeManager get() {
		if (instance == null)
			instance = new HomeManager();
		return instance;
	}

	public int getMaximumHomes(String player) {
		Integer homeLimit = groups.get(me.devtec.craftyserversystem.API.get().getPermissionHook().getGroup(player));
		return homeLimit == null ? groups.get("default") : homeLimit;
	}

	public List<String> getHomes(String player) {
		return API.offlineCache().lookupQuery(player) == null ? Collections.emptyList() : new ArrayList<>(API.getUser(player).getKeys("css.home"));
	}

	public Position getHomePosition(String player, String home) {
		return API.getUser(player).getAs("css.home." + home, Position.class);
	}

	public void setHome(String player, String home, Position pos) {
		API.getUser(player).set("css.home." + home, pos);
	}

	public void load(Map<String, Integer> groups) {
		this.groups.clear();
		this.groups.putAll(groups);
	}

	public Map<String, Integer> getGroups() {
		return groups;
	}

	public void delHome(String player, String home) {
		API.getUser(player).remove("css.home." + home);
	}
}
