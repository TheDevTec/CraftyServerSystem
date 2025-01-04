package me.devtec.craftyserversystem.events.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.internal.bansystem.ConsoleBanFilter;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.dataholder.Config;

public class ConsoleFilterListener implements CssListener {

	private Predicate<String> predicate;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getConsoleFilter();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {
		ConsoleBanFilter.init();
		List<Pattern> list = new ArrayList<>();
		for (String regex : getConfig().getStringList("list"))
			try {
				list.add(Pattern.compile(regex));
			} catch (Exception e) {
				JavaPlugin.getPlugin(Loader.class).getLogger().warning("Failed to compile regex Pattern '" + regex + "', skipping..");
			}
		if (!list.isEmpty())
			ConsoleBanFilter.registerFilter(predicate = t -> {
				try {
					for (Pattern pattern : list)
						if (pattern.matcher(t).find())
							return true;
				} catch (Exception e) {
				}
				return false;
			});
	}

	@Override
	public void unregister() {
		if (predicate != null) {
			ConsoleBanFilter.unregisterFilter(predicate);
			predicate = null;
		}
	}
}
