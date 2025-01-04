package me.devtec.craftyserversystem.placeholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;

public class PlaceholdersExecutor {

	public static final PlaceholdersExecutor EMPTY = new PlaceholdersExecutor() {
		@Override
		public String apply(String text) {
			return ColorUtils.colorize(PlaceholderAPI.apply(text.replace("{prefix}", API.get().getConfigManager().getPrefix()), null));
		}
	};

	public static PlaceholdersExecutor i() {
		return new PlaceholdersExecutor().add("prefix", API.get().getConfigManager().getPrefix());
	}

	private final Map<String, String> placeholders = new HashMap<>();
	private UUID target;

	public PlaceholdersExecutor add(String sequence, String replacement) {
		return addSpec('{' + sequence + '}', replacement);
	}

	public PlaceholdersExecutor add(String sequence, Number replacement) {
		return addSpec('{' + sequence + '}', StringUtils.formatDouble(FormatType.NORMAL, replacement.doubleValue()));
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
			return ColorUtils.colorize(PlaceholderAPI.apply(text, target)); // Doesn't contains any (our) placeholders

		StringContainer container = new StringContainer(text); // Much faster than using String#replace method
		for (Entry<String, String> entry : placeholders.entrySet())
			container.replace(entry.getKey(), entry.getValue());
		return ColorUtils.colorize(PlaceholderAPI.apply(container.toString(), target));
	}

	public String applyWithoutColors(String text) {
		if (placeholders.isEmpty() || text.indexOf('{') == -1)
			return PlaceholderAPI.apply(text, target); // Doesn't contains any (our) placeholders

		StringContainer container = new StringContainer(text); // Much faster than using String#replace method
		for (Entry<String, String> entry : placeholders.entrySet())
			container.replace(entry.getKey(), entry.getValue());
		return PlaceholderAPI.apply(container.toString(), target);
	}

	public String applyAfterColorize(String text) {
		if (placeholders.isEmpty() || text.indexOf('{') == -1)
			return PlaceholderAPI.apply(ColorUtils.colorize(text), target); // Doesn't contains any (our) placeholders

		StringContainer container = new StringContainer(ColorUtils.colorize(text, new ArrayList<>(placeholders.keySet()))); // Much faster than using String#replace method
		for (Entry<String, String> entry : placeholders.entrySet())
			container.replace(entry.getKey(), entry.getValue());
		return PlaceholderAPI.apply(container.toString(), target);
	}

	public List<String> apply(List<String> lore) {
		lore.replaceAll(this::apply);
		return lore;
	}

	public String get(String placeholder) {
		return placeholders.get(placeholder);
	}

	@SuppressWarnings("unchecked")
	public Object apply(Object object) {
		if (object instanceof Map) {
			Map<String, Object> map = new HashMap<>(((Map<String, Object>) object).size());
			for (Entry<String, Object> entry : ((Map<String, Object>) object).entrySet())
				if (entry.getKey().equals("color"))
					map.put(entry.getKey(), entry.getValue());
				else
					map.put(entry.getKey(), apply(entry.getValue(), entry.getKey().equals("hoverEvent")));
			return map;
		}
		if (object instanceof Collection) {
			List<Object> rewritten = new ArrayList<>(((Collection<?>) object).size());
			for (Object obj : (Collection<?>) object)
				rewritten.add(apply(obj));
			return rewritten;
		}
		return object instanceof String ? apply(object.toString()) : object;
	}

	@SuppressWarnings("unchecked")
	private Object apply(Object object, boolean shouldConvertToComponent) {
		if (object instanceof Map) {
			Map<String, Object> map = new HashMap<>(((Map<String, Object>) object).size());
			for (Entry<String, Object> entry : ((Map<String, Object>) object).entrySet())
				if (entry.getKey().equals("color"))
					map.put(entry.getKey(), entry.getValue());
				else
					map.put(entry.getKey(),
							apply(entry.getValue(), shouldConvertToComponent ? entry.getKey().equals("value") || entry.getKey().equals("content") || entry.getKey().equals("contents") : false));
			return map;
		}
		if (object instanceof Collection) {
			List<Object> rewritten = new ArrayList<>(((Collection<?>) object).size());
			for (Object obj : (Collection<?>) object)
				rewritten.add(apply(obj, shouldConvertToComponent));
			return rewritten;
		}
		if (object instanceof String)
			return shouldConvertToComponent ? ComponentAPI.toJsonList(ComponentAPI.fromString(apply(object.toString()))) : apply(object.toString());
		return object;
	}
}
