package me.devtec.craftyserversystem.commands.internal.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.annotations.Nonnull;
import me.devtec.craftyserversystem.annotations.Nullable;
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

	public int getMaximumHomes(@Nonnull String player) {
		Integer homeLimit = groups.get(me.devtec.craftyserversystem.API.get().getPermissionHook().getGroup(player));
		return homeLimit == null ? groups.get("default") : homeLimit;
	}

	@Nonnull
	public List<String> getHomes(@Nonnull String player) {
		return API.offlineCache().lookupQuery(player) == null ? Collections.emptyList() : new ArrayList<>(API.getUser(player).getKeys("css.home"));
	}

	@Nullable
	public Position getHomePosition(@Nonnull String player, @Nonnull String home) {
		return API.getUser(player).getAs("css.home." + home, Position.class);
	}

	public void setHome(@Nonnull String player, @Nonnull String home, @Nonnull Position pos) {
		API.getUser(player).set("css.home." + home, pos);
	}

	public void load(@Nonnull Map<String, Integer> groups) {
		this.groups.clear();
		this.groups.putAll(groups);
	}

	@Nonnull
	public Map<String, Integer> getGroups() {
		return groups;
	}

	public void delHome(@Nonnull String player, @Nonnull String home) {
		API.getUser(player).remove("css.home." + home);
	}
}
