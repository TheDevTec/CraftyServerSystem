package me.devtec.craftyserversystem.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.internal.PreCommandListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Ref;
import me.devtec.shared.components.ClickEvent;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.components.ComponentItem;
import me.devtec.shared.components.HoverEvent;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.json.custom.CustomJsonWriter;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.nms.NmsProvider.ChatType;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class MessageManager {

	private static ItemStack EMPTY;
	static {
		EMPTY = ItemMaker.of(XMaterial.BLACK_STAINED_GLASS_PANE).displayName("&f").build();
	}

	private final Logger logger = JavaPlugin.getPlugin(Loader.class).getLogger();

	public class Action {
		Config config;
		String path;
		PlaceholdersExecutor executor;
		CommandSender[] receivers;
		CompletableFuture<String> result;
		Player owner;

		public Action(Config config, String path, PlaceholdersExecutor executor, CommandSender[] receivers) {
			this.config = config;
			this.path = path;
			this.executor = executor;
			this.receivers = receivers;
		}

		public Action(Config config, String path, PlaceholdersExecutor executor, CommandSender[] receivers, CompletableFuture<String> result, Player owner) {
			this.config = config;
			this.path = path;
			this.executor = executor;
			this.receivers = receivers;
			this.result = result;
			this.owner = owner;
		}

		public void process() {
			if (result != null) {
				if (!config.existsKey(path)) {
					JavaPlugin.getPlugin(Loader.class).getLogger().severe("Missing translation path '" + path + "', please report this bug to the DevTec team.");
					result.complete(null);
					return;
				}

				List<Component> components;
				String inString;
				boolean collection = false;
				if (config.isJson(path) && (config.get(path) instanceof Collection || config.get(path) instanceof Map)) {
					if (config.get(path) instanceof Collection && ((Collection<?>) config.get(path)).isEmpty() || config.get(path) instanceof Map && ((Map<?, ?>) config.get(path)).isEmpty()) {
						result.complete(null);
						return;
					}
					components = new ArrayList<>();
					components.add(ComponentAPI.fromJson(CustomJsonWriter.toJson(executor.apply(config.get(path))))); // Json
					inString = convertToReadableStringForConsole(components.get(0));
				} else if (config.get(path) instanceof Collection) {
					components = new ArrayList<>();
					StringContainer container = new StringContainer(64);
					for (Object value : config.getList(path))
						if (value instanceof Collection) {
							Component component = ComponentAPI.fromJson(CustomJsonWriter.toJson(executor.apply(value)));
							components.add(component); // Json
							if (!container.isEmpty())
								container.append('\n');
							container.append(convertToReadableStringForConsole(component));
						} else {
							Component component = ComponentAPI.fromString(executor.applyAfterColorize(value.toString()));
							components.add(component); // String
							if (!container.isEmpty())
								container.append('\n');
							container.append(convertToReadableStringForConsole(component));
						}
					if (components.isEmpty()) {
						result.complete(null);
						return;
					}
					inString = container.toString();
					collection = true;
				} else {
					String value = config.getString(path);
					if (value == null || value.isEmpty()) {
						result.complete(null);
						return;
					}
					components = new ArrayList<>();
					components.add(ComponentAPI.fromString(executor.applyAfterColorize(value))); // String
					inString = convertToReadableStringForConsole(components.get(0));
				}
				if (owner.hasPermission("css.chat.placeholders"))
					replaceChatPlaceholders(API.get().getConfigManager().getChat(), components, new AtomicInteger(0), API.get().getConfigManager().getChat().getInt("placeholders.limit-per-message"));
				if (!collection) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, BukkitLoader.getNmsProvider().toIChatBaseComponent(components));
					for (CommandSender player : receivers)
						if (player instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) player, packet);
						else
							player.sendMessage(inString);
					result.complete(inString);
					return;
				}
				for (Component component : components) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);
					for (CommandSender player : receivers)
						if (player instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) player, packet);
						else
							player.sendMessage(convertToReadableStringForConsole(component));
				}
				result.complete(inString);
				return;
			}
			if (!config.existsKey(path)) {
				logger.severe("Missing translation path '" + path + "', please report this bug to the DevTec team.");
				return;
			}

			Object chatBase;
			boolean collection = false;
			if (config.isJson(path) && (config.get(path) instanceof Collection || config.get(path) instanceof Map)) {
				if (config.get(path) instanceof Collection && ((Collection<?>) config.get(path)).isEmpty() || config.get(path) instanceof Map && ((Map<?, ?>) config.get(path)).isEmpty())
					return;
				chatBase = BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(executor.apply(config.get(path)))); // Json
			} else if (config.get(path) instanceof Collection) {
				List<Object> components = new ArrayList<>();
				for (Object value : config.getList(path))
					if (value instanceof Collection)
						components.add(BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(executor.apply(value)))); // Json
					else
						components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(executor.apply(value.toString()))));
				chatBase = components;
				if (components.isEmpty())
					return;
				collection = true;
			} else {
				String result = config.getString(path);
				if (result == null || result.isEmpty())
					return;
				chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(executor.apply(result))); // String
			}
			if (collection)
				for (Object component : (List<?>) chatBase) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);
					for (CommandSender player : receivers)
						if (player instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) player, packet);
						else
							player.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString());
				}
			else {
				Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, chatBase);
				for (CommandSender player : receivers)
					if (player instanceof Player)
						BukkitLoader.getPacketHandler().send((Player) player, packet);
					else
						player.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(chatBase).toString());
			}
		}

		private String getNbtOf(ItemStack itemInHand) {
			if (!itemInHand.hasItemMeta() || itemInHand.getType() == Material.AIR)
				return null;
			ItemStack item = itemInHand.clone();
			ItemMeta meta = Bukkit.getItemFactory().getItemMeta(itemInHand.getType());
			meta.setLore(itemInHand.getItemMeta().getLore());
			for (Entry<Enchantment, Integer> ench : itemInHand.getEnchantments().entrySet())
				meta.addEnchant(ench.getKey(), ench.getValue(), true);
			meta.addItemFlags(itemInHand.getItemMeta().getItemFlags().toArray(new ItemFlag[0]));
			if (itemInHand.getItemMeta().getDisplayName() != null)
				meta.setDisplayName("§e");
			item.setItemMeta(meta);
			Object nbt = BukkitLoader.getNmsProvider().getNBT(item);
			return nbt == null ? null : nbt + "";
		}

		private void replaceChatPlaceholders(Config config, List<Component> components, AtomicInteger totalPlaceholders, int limitPlaceholders) {
			ListIterator<Component> itr = components.listIterator();
			while (itr.hasNext()) {
				Component comp = itr.next();
				int[] find;
				int prevPos = 0;
				while ((find = find(prevPos, comp.getText())) != null) {
					if (totalPlaceholders.getAndIncrement() >= limitPlaceholders)
						break;
					String prefix = comp.getText().substring(prevPos, find[0]);
					String suffix = comp.getText().substring(find[0] + find[1]);
					comp.setText(prefix);
					prevPos = find[0] + find[1];
					ItemStack itemInHand = owner.getItemInHand();
					if (itemInHand.getType() != Material.AIR && find[1] == 6) {
						String itemName = itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName() ? itemInHand.getItemMeta().getDisplayName() : null;
						if (itemName == null) {
							StringContainer container = new StringContainer(itemInHand.getType().name().length());
							boolean first = true;
							for (String split : itemInHand.getType().name().split("_")) {
								if (first) {
									container.append(split.charAt(0)).append(split.substring(1).toLowerCase());
									first = false;
									continue;
								}
								container.append(' ');
								if (split.equals("OF") || split.equals("THE"))
									container.append(split.toLowerCase());
								else
									container.append(split.charAt(0)).append(split.substring(1).toLowerCase());
							}
							itemName = container.toString();
						}
						String id = UUID.randomUUID().toString();
						GUI inv = new GUI(config.getString("placeholders.item.inv-title").replace("{player}", owner.getName()), 27);
						for (int i = 0; i < 27; ++i)
							if (i == 13)
								inv.getInventory().setItem(i, itemInHand);
							else
								inv.getInventory().setItem(i, EMPTY);
						synchronized (PreCommandListener.guis) {
							PreCommandListener.guis.put(id, inv);
						}
						String value = ColorUtils.colorize(config.getString("placeholders.item.replace"), Arrays.asList("{itemName}", "{player}")).replace("{player}", owner.getName())
								.replace("{itemName}", itemName);
						Component item = ComponentAPI.fromString(value);
						item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/css-openinv " + id));
						item.setHoverEvent(
								new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentItem(itemInHand.getType().name().toLowerCase(), itemInHand.getAmount()).setNbt(getNbtOf(itemInHand))));
						if (item.getExtra() != null)
							for (Component extra : item.getExtra()) {
								extra.setClickEvent(item.getClickEvent());
								extra.setHoverEvent(item.getHoverEvent());
							}
						itr.add(item);
					} else if (find[1] == 5) {
						String id = UUID.randomUUID().toString();
						GUI inv = new GUI(config.getString("placeholders.inventory.inv-title").replace("{player}", owner.getName()), 36);
						if (Ref.isNewerThan(8))
							inv.getInventory().setContents(owner.getInventory().getStorageContents());
						else
							inv.getInventory().setContents(owner.getInventory().getContents());
						synchronized (PreCommandListener.guis) {
							PreCommandListener.guis.put(id, inv);
						}

						String value = ColorUtils.colorize(config.getString("placeholders.inventory.replace").replace("{player}", owner.getName()));
						Component item = ComponentAPI.fromString(value);
						item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/css-openinv " + id));
						if (!config.getString("placeholders.inventory.hoverEvent").isEmpty())
							item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
									ComponentAPI.fromString(ColorUtils.colorize(config.getString("placeholders.inventory.hoverEvent").replace("{player}", owner.getName())), true, false)));
						if (item.getExtra() != null)
							for (Component extra : item.getExtra()) {
								extra.setClickEvent(item.getClickEvent());
								extra.setHoverEvent(item.getHoverEvent());
							}
						itr.add(item);
					} else if (find[1] == 4) {
						String id = UUID.randomUUID().toString();
						GUI inv = new GUI(config.getString("placeholders.enderchest.inv-title").replace("{player}", owner.getName()), owner.getEnderChest().getSize());
						inv.getInventory().setContents(owner.getEnderChest().getContents());
						synchronized (PreCommandListener.guis) {
							PreCommandListener.guis.put(id, inv);
						}
						String value = ColorUtils.colorize(config.getString("placeholders.enderchest.replace").replace("{player}", owner.getName()));
						Component item = ComponentAPI.fromString(value);
						if (!config.getString("placeholders.enderchest.hoverEvent").isEmpty())
							item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
									ComponentAPI.fromString(ColorUtils.colorize(config.getString("placeholders.enderchest.hoverEvent").replace("{player}", owner.getName())), true, false)));
						item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/css-openinv " + id));
						if (item.getExtra() != null)
							for (Component extra : item.getExtra()) {
								extra.setClickEvent(item.getClickEvent());
								extra.setHoverEvent(item.getHoverEvent());
							}
						itr.add(item);
					}
					itr.add(new Component(suffix).copyOf(comp).setClickEvent(comp.getClickEvent()).setHoverEvent(comp.getHoverEvent()).setInsertion(comp.getInsertion()));
					itr.previous();
				}
			}

			for (Component component : components)
				if (component.getExtra() != null)
					replaceChatPlaceholders(config, component.getExtra(), totalPlaceholders, limitPlaceholders);
		}

		private int[] find(int startAt, String value) {
			for (int i = startAt; i < value.length(); ++i) {
				char c = value.charAt(i);
				if (c == '[') {
					if (value.length() > i + 5)
						if (value.charAt(i + 1) == 'i' && value.charAt(i + 2) == 't' && value.charAt(i + 3) == 'e' && value.charAt(i + 4) == 'm' && value.charAt(i + 5) == ']')
							return new int[] { i, 6 };
					if (value.length() > i + 4)
						if (value.charAt(i + 1) == 'i' && value.charAt(i + 2) == 'n' && value.charAt(i + 3) == 'v' && value.charAt(i + 4) == ']')
							return new int[] { i, 5 };
					if (value.length() > i + 3)
						if (value.charAt(i + 1) == 'e' && value.charAt(i + 2) == 'c' && value.charAt(i + 3) == ']')
							return new int[] { i, 4 };
				}
			}
			return null;
		}
	}

	static List<Action> actions = Collections.synchronizedList(new ArrayList<>());

	public MessageManager() {
		new Tasker() {

			@Override
			public void run() {
				if (actions.isEmpty())
					return;
				for (int i = 0; i < actions.size(); ++i)
					actions.remove(0).process();
			}
		}.runRepeating(1, 1);
	}

	public String convertToReadableStringForConsole(Component component) {
		StringContainer builder = new StringContainer(component.getText().length() + 8);

		String colorBefore = null;

		// COLOR
		if (component.getColor() != null) {
			if (component.getColor().charAt(0) == '#')
				colorBefore = hexToReadableFormat(component.getColor());
			else
				colorBefore = "§" + component.colorToChar();
			builder.append(colorBefore);
		}

		// FORMATS
		String formatsBefore = component.getFormats();
		builder.append(formatsBefore);

		builder.append(component.getText());

		if (component.getExtra() != null)
			for (Component c : component.getExtra()) {
				toString(c, builder, colorBefore, formatsBefore);
				if (c.getColor() != null)
					if (c.getColor().charAt(0) == '#')
						colorBefore = hexToReadableFormat(c.getColor());
					else
						colorBefore = "§" + c.colorToChar();
				String formats = c.getFormats();
				if (!formats.isEmpty())
					formatsBefore = formats;
			}
		return builder.toString();
	}

	// Deeper toString with "anti" copying of colors & formats
	private void toString(Component component, StringContainer builder, String parentColorBefore, String parentFormatsBefore) {
		String colorBefore = parentColorBefore;

		// FORMATS
		String formatsBefore = component.getFormats();
		// COLOR
		if (component.getColor() != null) {
			if (component.getColor().charAt(0) == '#')
				colorBefore = hexToReadableFormat(component.getColor());
			else
				colorBefore = "§" + component.colorToChar();
			if (!colorBefore.equals(parentColorBefore) || !formatsBefore.equals(parentFormatsBefore))
				builder.append(colorBefore);
		}

		// FORMATS
		if (!formatsBefore.equals(parentFormatsBefore))
			builder.append(formatsBefore);

		builder.append(component.getText());

		if (component.getExtra() != null)
			for (Component c : component.getExtra())
				toString(c, builder, colorBefore, formatsBefore);
	}

	private String hexToReadableFormat(String color) {
		return new String(new char[] { '§', 'x', '§', color.charAt(1), '§', color.charAt(2), '§', color.charAt(3), '§', color.charAt(4), '§', color.charAt(5), '§', color.charAt(6) });
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, Collection<? extends CommandSender> receivers) {
		actions.add(new Action(transFile, pathToTranslation, ex, receivers.toArray(new CommandSender[0])));
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, CommandSender... receivers) {
		actions.add(new Action(transFile, pathToTranslation, ex, receivers));
	}

	public String sendMessageFromFileWithResult(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, Collection<? extends CommandSender> receivers, Player player) {
		CompletableFuture<String> future = new CompletableFuture<>();
		actions.add(0, new Action(transFile, pathToTranslation, ex, receivers.toArray(new CommandSender[0]), future, player)); // Inserts on first priority
		try {
			return future.get(); // Freeze current thread and await result from another thread
		} catch (Exception e) {
			return null;
		}
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, String permission) {
		List<CommandSender> receivers = new ArrayList<>();
		receivers.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(permission))
				receivers.add(player);
		actions.add(new Action(transFile, pathToTranslation, ex, receivers.toArray(new CommandSender[0])));
	}
}