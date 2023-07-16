package me.devtec.craftyserversystem.placeholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import me.devtec.craftyserversystem.Loader;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.ColorUtils;

public class PlaceholdersExecutor {

	public static final PlaceholdersExecutor EMPTY = new PlaceholdersExecutor() {
		@Override
		public String apply(String text) {
			return PlaceholderAPI.apply(ColorUtils.colorize(text.replace("{prefix}", Loader.getPlugin().getConfigManager().getPrefix())), null);
		}
	};

	public static PlaceholdersExecutor i() {
		return new PlaceholdersExecutor().add("prefix", Loader.getPlugin().getConfigManager().getPrefix());
	}

	private final Map<String, String> placeholders = new HashMap<>();
	private UUID target;

	public PlaceholdersExecutor add(String sequence, String replacement) {
		return addSpec('{' + sequence + '}', replacement);
	}

	public PlaceholdersExecutor add(String sequence, Number replacement) {
		return addSpec('{' + sequence + '}', replacement.toString());
	}

	public PlaceholdersExecutor addSpec(String sequence, String replacement) {
		placeholders.put(sequence, replacement);
		return this;
	}

	public PlaceholdersExecutor addSpec(String sequence, Number replacement) {
		return addSpec(sequence, replacement.toString());
	}

	public PlaceholdersExecutor papi(UUID playerUuid) {
		target = playerUuid;
		return this;
	}

	public String apply(String text) {
		if (placeholders.isEmpty() || text.indexOf('{') == -1)
			return PlaceholderAPI.apply(ColorUtils.colorize(text), target); // Doesn't contains any (our) placeholders

		StringContainer container = new StringContainer(text); // Much faster than using String#replace method
		for (Entry<String, String> entry : placeholders.entrySet())
			container.replace(entry.getKey(), entry.getValue());
		return PlaceholderAPI.apply(ColorUtils.colorize(container.toString()), target);
	}

	public List<String> apply(List<String> lore) {
		lore.replaceAll(this::apply);
		return lore;
	}
}
