package me.devtec.craftyserversystem.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.bukkit.command.BlockCommandSender;
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
import me.devtec.shared.Ref;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.components.base.Component;
import me.devtec.shared.components.base.ComponentItem;
import me.devtec.shared.components.decorations.ClickEvent;
import me.devtec.shared.components.decorations.HoverEvent;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.json.Json;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.text.TextRenderer.ColorMode;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.nms.NmsProvider.ChatType;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class MessageManager {

	private static final ItemStack EMPTY;

	static {
		EMPTY = ItemMaker.of(XMaterial.BLACK_STAINED_GLASS_PANE).displayName("&f").build();
	}

	private final Logger logger = JavaPlugin.getPlugin(Loader.class).getLogger();

	public class Action {

		Config config;
		String path;
		List<String> messages;
		TextRenderer renderer;
		CommandSender[] receivers;
		CompletableFuture<String> result;
		Player owner;

		public Action(Config config, String path, TextRenderer renderer, CommandSender[] receivers) {
			this.config = config;
			this.path = path;
			this.renderer = prepareRenderer(renderer);
			this.receivers = receivers;
		}

		public Action(List<String> messages, TextRenderer renderer, CommandSender[] receivers) {
			this.messages = messages;
			this.renderer = prepareRenderer(renderer);
			this.receivers = receivers;
		}

		public Action(Config config, String path, TextRenderer renderer, CommandSender[] receivers, CompletableFuture<String> result, Player owner) {
			this.config = config;
			this.path = path;
			this.renderer = prepareRenderer(renderer);
			this.receivers = receivers;
			this.result = result;
			this.owner = owner;
		}

		public void process() {
			if(path == null && messages != null) {
				List<Object> components = new ArrayList<>();

				for(String value : messages)
					if(value.startsWith("[") && value.endsWith("]") || value.startsWith("{") && value.endsWith("}")) {

						Object json = Json.reader().simpleRead(value);
						Component component = ComponentAPI.fromJson(renderJson(json, renderer));

						components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(component));
					} else
						components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(render(value, renderer))));

				if(components.isEmpty())
					return;

				for(Object component : components) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);

					for(CommandSender receiver : receivers)
						if(receiver instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) receiver, packet);
						else if(receiver instanceof BlockCommandSender)
							BukkitLoader.getNmsProvider().postToMainThread(() -> receiver.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString()));
						else
							receiver.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString());
				}
				return;
			}

			if(result != null) {
				if(!config.existsKey(path)) {
					logger.severe("Missing translation path '" + path + "', please report this bug to the DevTec team.");

					result.complete(null);
					return;
				}

				List<Component> components;
				String inString;
				boolean collection = false;

				Object valueAtPath = config.get(path);

				if(config.isJson(path) && (valueAtPath instanceof Collection || valueAtPath instanceof Map)) {

					if(valueAtPath instanceof Collection && ((Collection<?>) valueAtPath).isEmpty() || valueAtPath instanceof Map && ((Map<?, ?>) valueAtPath).isEmpty()) {

						result.complete(null);
						return;
					}

					components = new ArrayList<>();

					Component component = ComponentAPI.fromJson(renderJson(valueAtPath, renderer));

					components.add(component);
					inString = convertToReadableStringForConsole(component);

				} else if(valueAtPath instanceof Collection) {
					components = new ArrayList<>();
					StringContainer container = new StringContainer(64);

					for(Object value : config.getList(path)) {
						Component component;

						if(value instanceof Collection || value instanceof Map)
							component = ComponentAPI.fromJson(renderJson(value, renderer));
						else
							component = Component.fromString(renderBeforePlaceholders(value.toString(), renderer), true, true);

						components.add(component);

						if(!container.isEmpty())
							container.append('\n');

						container.append(convertToReadableStringForConsole(component));
					}

					if(components.isEmpty()) {
						result.complete(null);
						return;
					}

					inString = container.toString();
					collection = true;

				} else {
					String value = config.getString(path);

					if(value == null || value.isEmpty()) {
						result.complete(null);
						return;
					}

					components = new ArrayList<>();

					Component component = Component.fromString(renderBeforePlaceholders(value, renderer), true, true);

					components.add(component);
					inString = convertToReadableStringForConsole(component);
				}

				if(owner.hasPermission("css.chat.placeholders"))
					replaceChatPlaceholders(API.get().getConfigManager().getChat(), components, new AtomicInteger(0), API.get().getConfigManager().getChat().getInt("placeholders.limit-per-message"));

				if(!collection) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, BukkitLoader.getNmsProvider().toIChatBaseComponent(components));

					for(CommandSender receiver : receivers)
						if(receiver instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) receiver, packet);
						else if(receiver instanceof BlockCommandSender)
							BukkitLoader.getNmsProvider().postToMainThread(() -> receiver.sendMessage(inString));
						else
							receiver.sendMessage(inString);

					result.complete(inString);
					return;
				}

				for(Component component : components) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);

					for(CommandSender receiver : receivers)
						if(receiver instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) receiver, packet);
						else if(receiver instanceof BlockCommandSender)
							BukkitLoader.getNmsProvider().postToMainThread(() -> receiver.sendMessage(convertToReadableStringForConsole(component)));
						else
							receiver.sendMessage(convertToReadableStringForConsole(component));
				}

				result.complete(inString);
				return;
			}

			if(!config.existsKey(path)) {
				logger.severe("Missing translation path '" + path + "', please report this bug to the DevTec team.");
				return;
			}

			Object chatBase;
			boolean collection = false;
			Object valueAtPath = config.get(path);
			if(config.isJson(path) && (valueAtPath instanceof Collection || valueAtPath instanceof Map)) {

				if(valueAtPath instanceof Collection && ((Collection<?>) valueAtPath).isEmpty() || valueAtPath instanceof Map && ((Map<?, ?>) valueAtPath).isEmpty())
					return;

				Component component = ComponentAPI.fromJson(renderJson(valueAtPath, renderer));

				chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(component);

			} else if(valueAtPath instanceof Collection) {
				List<Object> components = new ArrayList<>();

				for(Object value : config.getList(path))
					if(value instanceof Collection || value instanceof Map) {
						Component component = ComponentAPI.fromJson(renderJson(value, renderer));

						components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(component));
					} else
						components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(render(value.toString(), renderer))));

				if(components.isEmpty())
					return;

				chatBase = components;
				collection = true;

			} else {
				String value = config.getString(path);

				if(value == null || value.isEmpty())
					return;

				chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(render(value, renderer)));
			}

			if(collection)
				for(Object component : (List<?>) chatBase) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);

					for(CommandSender receiver : receivers)
						if(receiver instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) receiver, packet);
						else if(receiver instanceof BlockCommandSender)
							BukkitLoader.getNmsProvider().postToMainThread(() -> receiver.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString()));
						else
							receiver.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString());
				}
			else {
				Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, chatBase);

				for(CommandSender receiver : receivers)
					if(receiver instanceof Player)
						BukkitLoader.getPacketHandler().send((Player) receiver, packet);
					else if(receiver instanceof BlockCommandSender)
						BukkitLoader.getNmsProvider().postToMainThread(() -> receiver.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(chatBase).toString()));
					else
						receiver.sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(chatBase).toString());
			}
		}

		private String getNbtOf(ItemStack itemInHand) {
			if(!itemInHand.hasItemMeta() || itemInHand.getType() == Material.AIR)
				return null;

			ItemStack item = itemInHand.clone();
			ItemMeta meta = Bukkit.getItemFactory().getItemMeta(itemInHand.getType());

			meta.setLore(itemInHand.getItemMeta().getLore());

			for(Entry<Enchantment, Integer> enchantment : itemInHand.getEnchantments().entrySet())
				meta.addEnchant(enchantment.getKey(), enchantment.getValue(), true);

			meta.addItemFlags(itemInHand.getItemMeta().getItemFlags().toArray(new ItemFlag[0]));

			if(itemInHand.getItemMeta().getDisplayName() != null)
				meta.setDisplayName("§e");

			item.setItemMeta(meta);

			Object nbt = BukkitLoader.getNmsProvider().getNBT(item);

			return nbt == null ? null : nbt.toString();
		}

		private void replaceChatPlaceholders(Config config, List<Component> components, AtomicInteger totalPlaceholders, int limitPlaceholders) {

			ListIterator<Component> iterator = components.listIterator();

			while(iterator.hasNext()) {
				Component component = iterator.next();

				int[] find;
				int previousPosition = 0;

				while((find = find(previousPosition, component.getText())) != null) {
					if(totalPlaceholders.getAndIncrement() >= limitPlaceholders)
						break;

					String prefix = component.getText().substring(previousPosition, find[0]);

					String suffix = component.getText().substring(find[0] + find[1]);

					component.setText(prefix);

					previousPosition = find[0] + find[1];

					ItemStack itemInHand = owner.getItemInHand();

					if(itemInHand.getType() != Material.AIR && (find[1] == 6 || find[1] == 3)) {

						String itemName = itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName() ? itemInHand.getItemMeta().getDisplayName() : null;

						if(itemName == null) {
							StringContainer container = new StringContainer(itemInHand.getType().name().length());

							boolean first = true;

							for(String split : itemInHand.getType().name().split("_")) {
								if(first) {
									container.append(split.charAt(0)).append(split.substring(1).toLowerCase());

									first = false;
									continue;
								}

								container.append(' ');

								if("OF".equals(split) || "THE".equals(split))
									container.append(split.toLowerCase());
								else
									container.append(split.charAt(0)).append(split.substring(1).toLowerCase());
							}

							itemName = container.toString();
						}

						String id = createShortId();

						GUI inventory = new GUI(TextRenderer.create().placeholder("player", owner.getName()).renderPlain(config.getString("placeholders.item.inv-title")), 27);

						for(int i = 0; i < 27; ++i)
							if(i == 13)
								inventory.getInventory().setItem(i, itemInHand);
							else
								inventory.getInventory().setItem(i, EMPTY);

						synchronized(PreCommandListener.guis) {
							PreCommandListener.guis.put(id, inventory);
						}

						String value = TextRenderer.create().placeholder("player", owner.getName()).placeholder("itemName", itemName).colorizeBeforePlaceholders()
						        .render(config.getString("placeholders.item.replace"));

						Component item = ComponentAPI.fromString(value);

						item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/css-openinv " + id));

						item.setHoverEvent(
						        new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentItem(itemInHand.getType().name().toLowerCase(), itemInHand.getAmount()).setNbt(getNbtOf(itemInHand))));

						if(item.getExtra() != null)
							for(Component extra : item.getExtra()) {
								extra.setClickEvent(item.getClickEvent());
								extra.setHoverEvent(item.getHoverEvent());
							}

						iterator.add(item);

					} else if(find[1] == 5) {
						String id = createShortId();

						GUI inventory = new GUI(TextRenderer.create().placeholder("player", owner.getName()).renderPlain(config.getString("placeholders.inventory.inv-title")), 36);

						if(Ref.isAtLeast(9, 0))
							inventory.getInventory().setContents(owner.getInventory().getStorageContents());
						else
							inventory.getInventory().setContents(owner.getInventory().getContents());

						synchronized(PreCommandListener.guis) {
							PreCommandListener.guis.put(id, inventory);
						}

						String value = TextRenderer.create().placeholder("player", owner.getName()).colorize().render(config.getString("placeholders.inventory.replace"));

						Component item = ComponentAPI.fromString(value);

						item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/css-openinv " + id));

						String hover = config.getString("placeholders.inventory.hoverEvent");

						if(!hover.isEmpty())
							item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							        ComponentAPI.fromString(TextRenderer.create().placeholder("player", owner.getName()).colorize().render(hover), true, false)));

						if(item.getExtra() != null)
							for(Component extra : item.getExtra()) {
								extra.setClickEvent(item.getClickEvent());
								extra.setHoverEvent(item.getHoverEvent());
							}

						iterator.add(item);

					} else if(find[1] == 4) {
						String id = createShortId();

						GUI inventory = new GUI(TextRenderer.create().placeholder("player", owner.getName()).renderPlain(config.getString("placeholders.enderchest.inv-title")),
						        owner.getEnderChest().getSize());

						inventory.getInventory().setContents(owner.getEnderChest().getContents());

						synchronized(PreCommandListener.guis) {
							PreCommandListener.guis.put(id, inventory);
						}

						String value = TextRenderer.create().placeholder("player", owner.getName()).colorize().render(config.getString("placeholders.enderchest.replace"));

						Component item = ComponentAPI.fromString(value);

						String hover = config.getString("placeholders.enderchest.hoverEvent");

						if(!hover.isEmpty())
							item.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
							        ComponentAPI.fromString(TextRenderer.create().placeholder("player", owner.getName()).colorize().render(hover), true, false)));

						item.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/css-openinv " + id));

						if(item.getExtra() != null)
							for(Component extra : item.getExtra()) {
								extra.setClickEvent(item.getClickEvent());
								extra.setHoverEvent(item.getHoverEvent());
							}

						iterator.add(item);
					}

					iterator.add(new Component(suffix).copyOf(component).setClickEvent(component.getClickEvent()).setHoverEvent(component.getHoverEvent()).setInsertion(component.getInsertion()));

					iterator.previous();
				}
			}

			for(Component component : components)
				if(component.getExtra() != null)
					replaceChatPlaceholders(config, component.getExtra(), totalPlaceholders, limitPlaceholders);
		}

		private String createShortId() {
			final char[] chars = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

			while(true) {
				char[] id = new char[6];

				for(int i = 0; i < id.length; ++i)
					id[i] = chars[StringUtils.random.nextInt(chars.length)];

				String result = new String(id);

				synchronized(PreCommandListener.guis) {
					if(!PreCommandListener.guis.containsKey(result))
						return result;
				}
			}
		}

		private int[] find(int startAt, String value) {
			if(value != null)
				for(int i = startAt; i < value.length(); ++i) {
					char c = value.charAt(i);
					char endChar = c == '[' ? ']' : '%';

					if(c != '[' && c != '%')
						continue;

					if(value.length() > i + 5 && value.charAt(i + 1) == 'i' && value.charAt(i + 2) == 't' && value.charAt(i + 3) == 'e' && value.charAt(i + 4) == 'm' && value.charAt(i + 5) == endChar)
						return new int[]{i, 6};

					if(value.length() > i + 2 && value.charAt(i + 1) == 'i' && value.charAt(i + 2) == endChar)
						return new int[]{i, 3};

					if(value.length() > i + 4 && value.charAt(i + 1) == 'i' && value.charAt(i + 2) == 'n' && value.charAt(i + 3) == 'v' && value.charAt(i + 4) == endChar)
						return new int[]{i, 5};

					if(value.length() > i + 3 && value.charAt(i + 1) == 'e' && value.charAt(i + 2) == 'c' && value.charAt(i + 3) == endChar)
						return new int[]{i, 4};
				}

			return null;
		}
	}

	static final List<Action> actions = Collections.synchronizedList(new ArrayList<>());

	public MessageManager() {
		new Tasker() {

			@Override
			public void run() {
				while(!actions.isEmpty())
					actions.remove(0).process();
			}
		}.runRepeating(1, 1);
	}

	private TextRenderer prepareRenderer(TextRenderer renderer) {
		if(renderer == null)
			renderer = TextRenderer.create();

		if(!renderer.tokens().contains("{prefix}"))
			renderer.placeholder("prefix", API.get().getConfigManager().getPrefix());

		renderer.colorize();
		return renderer;
	}

	private String render(String text, TextRenderer renderer) {
		if(text == null)
			return null;

		return renderer.render(PlaceholderAPI.apply(text, renderer.target()));
	}

	private String renderBeforePlaceholders(String text, TextRenderer renderer) {
		if(text == null)
			return null;

		ColorMode previous = renderer.colorMode();

		renderer.colorizeBeforePlaceholders();

		try {
			return renderer.render(PlaceholderAPI.apply(text, renderer.target()));
		} finally {
			renderer.colorMode(previous);
		}
	}

	private Object renderJson(Object object, TextRenderer renderer) {
		return ComponentAPI.renderJson(applyPlaceholderApi(object, renderer.target()), renderer);
	}

	@SuppressWarnings("unchecked")
	private Object applyPlaceholderApi(Object object, UUID target) {
		if(object instanceof Map) {
			Map<String, Object> source = (Map<String, Object>) object;
			Map<String, Object> result = new HashMap<>(source.size());

			for(Entry<String, Object> entry : source.entrySet())
				if("color".equals(entry.getKey()))
					result.put(entry.getKey(), entry.getValue());
				else
					result.put(entry.getKey(), applyPlaceholderApi(entry.getValue(), target));

			return result;
		}

		if(object instanceof Collection) {
			Collection<?> source = (Collection<?>) object;
			List<Object> result = new ArrayList<>(source.size());

			for(Object value : source)
				result.add(applyPlaceholderApi(value, target));

			return result;
		}

		if(object instanceof String)
			return PlaceholderAPI.apply((String) object, target);

		return object;
	}

	public String convertToReadableStringForConsole(Component component) {
		StringContainer builder = new StringContainer(component.getText().length() + 8);

		String colorBefore = null;

		if(component.getColor() != null) {
			if(component.getColor().charAt(0) == '#')
				colorBefore = hexToReadableFormat(component.getColor());
			else
				colorBefore = "§" + component.colorToChar();

			builder.append(colorBefore);
		}

		String formatsBefore = component.getFormats();
		builder.append(formatsBefore);

		builder.append(component.getText());

		if(component.getExtra() != null)
			for(Component child : component.getExtra()) {
				toString(child, builder, colorBefore, formatsBefore);

				if(child.getColor() != null)
					if(child.getColor().charAt(0) == '#')
						colorBefore = hexToReadableFormat(child.getColor());
					else
						colorBefore = "§" + child.colorToChar();

				String formats = child.getFormats();

				if(!formats.isEmpty())
					formatsBefore = formats;
			}

		return builder.toString();
	}

	private void toString(Component component, StringContainer builder, String parentColorBefore, String parentFormatsBefore) {

		String colorBefore = parentColorBefore;
		String formatsBefore = component.getFormats();

		if(component.getColor() != null) {
			if(component.getColor().charAt(0) == '#')
				colorBefore = hexToReadableFormat(component.getColor());
			else
				colorBefore = "§" + component.colorToChar();

			if(!colorBefore.equals(parentColorBefore) || !formatsBefore.equals(parentFormatsBefore))
				builder.append(colorBefore);
		}

		if(!formatsBefore.equals(parentFormatsBefore))
			builder.append(formatsBefore);

		builder.append(component.getText());

		if(component.getExtra() != null)
			for(Component child : component.getExtra())
				toString(child, builder, colorBefore, formatsBefore);
	}

	private String hexToReadableFormat(String color) {
		return new String(new char[]{'§', 'x', '§', color.charAt(1), '§', color.charAt(2), '§', color.charAt(3), '§', color.charAt(4), '§', color.charAt(5), '§', color.charAt(6)});
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, TextRenderer renderer, Collection<? extends CommandSender> receivers) {

		actions.add(new Action(transFile, pathToTranslation, renderer, receivers.toArray(new CommandSender[0])));
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, TextRenderer renderer, CommandSender... receivers) {

		actions.add(new Action(transFile, pathToTranslation, renderer, receivers));
	}

	public void sendMessageFromFile(List<String> messages, TextRenderer renderer, CommandSender... receivers) {

		actions.add(new Action(messages, renderer, receivers));
	}

	public String sendMessageFromFileWithResult(Config transFile, String pathToTranslation, TextRenderer renderer, Collection<? extends CommandSender> receivers, Player player) {

		CompletableFuture<String> future = new CompletableFuture<>();

		actions.add(0, new Action(transFile, pathToTranslation, renderer, receivers.toArray(new CommandSender[0]), future, player));

		try {
			return future.get();
		} catch(Exception e) {
			return null;
		}
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, TextRenderer renderer, String permission) {

		List<CommandSender> receivers = new ArrayList<>();

		receivers.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(permission == null || player.hasPermission(permission))
				receivers.add(player);

		actions.add(new Action(transFile, pathToTranslation, renderer, receivers.toArray(new CommandSender[0])));
	}
}
