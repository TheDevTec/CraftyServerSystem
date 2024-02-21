package me.devtec.craftyserversystem.menubuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.devtec.craftyserversystem.Loader;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.gui.GUI;

public abstract class MenuBuilder {
	private final String id;
	private int menuSize;
	private String title;
	private Map<Integer, ItemBuilder> items = new HashMap<>();
	private Map<Character, ItemBuilder> charMap = new HashMap<>();
	private Map<ItemBuilder, List<Integer>> tickableItems = new HashMap<>();

	public MenuBuilder(String id) {
		this.id = id;
	}

	public abstract ItemBuilder loadItem(char character, String path, Config config);

	public MenuBuilder buildClassic(Config config) {
		setTitle(config.getString(id + ".title"));

		// init char map
		charMap.clear();
		for (String line : config.getKeys(id + ".items"))
			charMap.put(line.charAt(0), loadItem(line.charAt(0), id + ".items." + line, config));

		setMenuSize(Math.min(config.getStringList(id + ".lines").size() * 9, GUI.LINES_6));

		int slot = 0;
		for (String line : config.getStringList(id + ".lines"))
			for (char character : line.toCharArray())
				if (character != ' ') {
					ItemBuilder item = charMap.get(character);
					if (item == null)
						Loader.getPlugin().getLogger().warning("Not found item with id '" + character + "' in the menu with id '" + id + "'");
					else {
						if (item.getRefleshInterval() != 0) {
							List<Integer> list = getTickableItems().get(item);
							if (list == null)
								getTickableItems().put(item, list = new ArrayList<>());
							list.add(slot);
						}
						setItem(slot++, item);
					}
				} else
					slot++;
		return this;
	}

	public Map<ItemBuilder, List<Integer>> getTickableItems() {
		return tickableItems;
	}

	public long getMinimumRefleshTickSpeed() {
		List<Long> tickableItems = new ArrayList<>();
		for (ItemBuilder builder : getTickableItems().keySet())
			tickableItems.add(builder.getRefleshInterval());
		return tickableItems.isEmpty() ? 0 : tickableItems.size() == 1 ? tickableItems.get(0) : smallestTicks(tickableItems.toArray(new Long[0]));
	}

	private static long greatestCommonDivisor(long a, long b) {
		while (b != 0) {
			long t = b;
			b = a % b;
			a = t;
		}
		return a;
	}

	private static long smallestTicks(Long[] numbers) {
		long result = numbers[0];
		for (int i = 1; i < numbers.length; i++)
			result = greatestCommonDivisor(result, numbers[i]);
		return result;
	}

	public MenuBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public MenuBuilder setItem(int slot, ItemBuilder itemBuilder) {
		items.put(slot, itemBuilder);
		return this;
	}

	public int getMenuSize() {
		return menuSize;
	}

	public MenuBuilder setMenuSize(int size) {
		menuSize = size;
		return this;
	}

	public Map<Integer, ItemBuilder> getItems() {
		return items;
	}

	public Map<Character, ItemBuilder> getCharMap() {
		return charMap;
	}

	public String getTitle() {
		return title;
	}

	public String getId() {
		return id;
	}
}