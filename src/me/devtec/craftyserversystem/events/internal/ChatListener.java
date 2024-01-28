package me.devtec.craftyserversystem.events.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.ChatHandlers;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.dataholder.cache.TempList;
import me.devtec.shared.dataholder.cache.TempMap;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class ChatListener implements Listener, CssListener {

	// AntiSpam
	private List<String> ignoredPlaceholders = Arrays.asList("[item]", "[inv]", "[ec]");
	private boolean antiSpamEnabled;
	@Nonnull
	private TempMap<UUID, Object[]> prevMsgs;
	private int maxMessages;
	private double minSimilarity;
	private boolean bypassAntiSpam;

	// AntiSpam - Cooldown
	@Nonnull
	private TempList<UUID> cdMsgs;
	private boolean antiSpamCooldownEnabled;
	private boolean bypassAntiSpamCooldown;

	// AntiFlood
	private boolean antiFloodEnabled;
	private int floodMaxChars;
	private int floodMaxCapsChars;
	private int floodMaxNumbers;
	private int floodMaxSameWords;
	private int floodMinWordsBetweenSameToIgnore;
	private boolean bypassAntiFlood;

	// AntiSwear
	private boolean antiSwearEnabled;
	@Nonnull
	private String replacement;
	private boolean addColors;
	@Nonnull
	private List<String> words;
	private boolean bypassAntiSwear;
	private List<String[]> allowedPhrases;

	// AntiAd pattern
	private boolean antiAdEnabled;
	private boolean bypassAntiAd;
	@Nonnull
	private List<String> antiAdWhitelist;
	// Chat placeholders (emojis, etc.)
	@Nonnull
	private Map<String, String> chatPlaceholders;
	@Nullable
	private Set<Entry<String, String>> entrySetOfChatPlaceholders;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getChat();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {
		antiSpamEnabled = getConfig().getBoolean("antiSpam.enabled");
		if (prevMsgs == null)
			prevMsgs = new TempMap<>(TimeUtils.timeFromString(getConfig().getString("antiSpam.cache")) * 20);
		else {
			prevMsgs.clear();
			prevMsgs.setCacheTime(TimeUtils.timeFromString(getConfig().getString("antiSpam.cache")) * 20);
		}
		antiSpamCooldownEnabled = getConfig().getBoolean("antiSpam.cooldown-per-message.enabled");
		bypassAntiSpamCooldown = getConfig().getBoolean("antiSpam.cooldown-per-message.bypass-enabled");
		if (cdMsgs == null)
			cdMsgs = new TempList<>(TimeUtils.timeFromString(getConfig().getString("antiSpam.cooldown-per-message.time")) * 20);
		else {
			cdMsgs.clear();
			cdMsgs.setCacheTime(TimeUtils.timeFromString(getConfig().getString("antiSpam.cooldown-per-message.time")) * 20);
		}
		maxMessages = getConfig().getInt("antiSpam.maximum-messages") + 1;
		minSimilarity = getConfig().getDouble("antiSpam.minimal-similarity");
		bypassAntiSpam = getConfig().getBoolean("antiSpam.bypass-enabled");
		antiFloodEnabled = getConfig().getBoolean("antiFlood.enabled");
		floodMaxChars = getConfig().getInt("antiFlood.maximum-chars");
		floodMaxCapsChars = getConfig().getInt("antiFlood.maximum-caps-chars");
		floodMaxNumbers = getConfig().getInt("antiFlood.maximum-numbers");
		floodMaxSameWords = getConfig().getInt("antiFlood.maximum-same-words-in-row");
		floodMinWordsBetweenSameToIgnore = getConfig().getInt("antiFlood.words-between-same-to-ignore");
		bypassAntiFlood = getConfig().getBoolean("antiFlood.bypass-enabled");
		antiSwearEnabled = getConfig().getBoolean("antiSwear.enabled");
		replacement = getConfig().getString("antiSwear.replacement");
		addColors = replacement.indexOf('§') != -1;
		words = getConfig().getStringList("antiSwear.words");
		allowedPhrases = new ArrayList<>();
		for (String phrase : getConfig().getStringList("antiSwear.allowed-phrases")) {
			if (phrase.indexOf(":") == -1) {
				Loader.getPlugin().getLogger().warning("Failed loading allowed phrase '" + phrase + "' - Incorrect format! Format must be: 'swearWord:allowedPhrase'");
				continue;
			}
			String[] info = new String[3];
			String[] split = phrase.split(":");
			info[0] = split[0];
			info[1] = split[1].replace(" ", "");
			info[2] = split[1];
			allowedPhrases.add(info);
		}
		bypassAntiSwear = getConfig().getBoolean("antiSwear.bypass-enabled");
		antiAdEnabled = getConfig().getBoolean("antiAd.enabled");
		bypassAntiAd = getConfig().getBoolean("antiAd.bypass-enabled");
		antiAdWhitelist = getConfig().getStringList("antiAd.whitelist");
		entrySetOfChatPlaceholders = null;
		if (chatPlaceholders == null)
			chatPlaceholders = new HashMap<>();
		else
			chatPlaceholders.clear();
		for (String key : getConfig().getKeys("chat-placeholders"))
			chatPlaceholders.put(getConfig().getString("chat-placeholders." + key + ".text"), getConfig().getString("chat-placeholders." + key + ".replacement"));
		entrySetOfChatPlaceholders = chatPlaceholders.entrySet();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChat(AsyncPlayerChatEvent e) {
		if (e.isCancelled())
			return;
		List<String> playerNames = playerNames(e.getPlayer());

		String modifiedMessage = antiFloodEnabled && (bypassAntiFlood ? !e.getPlayer().hasPermission("css.chat.bypass.antiflood") : true) ? ChatHandlers.antiFlood(e.getMessage(),
				ChatHandlers.match(e.getMessage(), playerNames), floodMaxNumbers, floodMaxChars, floodMaxCapsChars, floodMaxSameWords, floodMinWordsBetweenSameToIgnore) : e.getMessage();

		if (antiAdEnabled && (bypassAntiAd ? !e.getPlayer().hasPermission("css.chat.bypass.antiad") : true) && ChatHandlers.antiAd(modifiedMessage, antiAdWhitelist)) {
			e.setCancelled(true);
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd-admin", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("message", modifiedMessage),
					"css.chat.antiad");
			return;
		}
		boolean addIgnorePlaceholders = e.getPlayer().hasPermission("css.chat.placeholders");
		ItemStack itemInHand;
		if (antiAdEnabled && addIgnorePlaceholders && (bypassAntiAd ? !e.getPlayer().hasPermission("css.chat.bypass.antiad") : true)
				&& (itemInHand = e.getPlayer().getItemInHand()).getType() != Material.AIR && modifiedMessage.indexOf("[item]") != -1
				&& ChatHandlers.antiAd(itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName() ? itemInHand.getItemMeta().getDisplayName() : null, antiAdWhitelist)) {
			e.setCancelled(true);
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd-admin", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("message",
					modifiedMessage.replace("[item]", getConfig().getString("placeholders.item.replace").replace("{itemName}", itemInHand.getItemMeta().getDisplayName()))), "css.chat.antiad");
			return;
		}

		if (antiSpamEnabled && (bypassAntiSpam ? !e.getPlayer().hasPermission("css.chat.bypass.antispam") : true)) {
			if (antiSpamCooldownEnabled && (bypassAntiSpamCooldown ? !e.getPlayer().hasPermission("css.chat.bypass.anticooldown") : true)) {
				if (cdMsgs.contains(e.getPlayer().getUniqueId())) {
					e.setCancelled(true);
					API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiSpam-Cooldown",
							PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("time",
									TimeUtils.timeToString(Math.max(1, (cdMsgs.getTimeOf(e.getPlayer().getUniqueId()) - System.currentTimeMillis() / 50L + cdMsgs.getCacheTime()) / 20))),
							e.getPlayer());
					return;
				}
				cdMsgs.add(e.getPlayer().getUniqueId());
			}
			if (ChatHandlers.processAntiSpam(e.getPlayer().getUniqueId(), modifiedMessage, prevMsgs, maxMessages, minSimilarity)) {
				e.setCancelled(true);
				API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiSpam", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
				return;
			}
		}
		if (antiSwearEnabled && (bypassAntiSwear ? !e.getPlayer().hasPermission("css.chat.bypass.antiswear") : true))
			if (getConfig().getBoolean("antiSwear.block-event")) {
				if (ChatHandlers.antiSwear(modifiedMessage, words, allowedPhrases, ChatHandlers.match(modifiedMessage, playerNames))) {
					e.setCancelled(true);
					API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiSwear", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
					return;
				}
			} else
				modifiedMessage = ChatHandlers.antiSwearReplace(modifiedMessage, words, allowedPhrases, ChatHandlers.match(modifiedMessage, playerNames), replacement, addColors);

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());

		String userGroup = API.get().getPermissionHook().getGroup(e.getPlayer());
		if (!getConfig().exists("formats." + userGroup))
			userGroup = "default";

		placeholders.add("player", placeholders.apply(getConfig().getString("formats." + userGroup + ".name")));
		placeholders.add("message", modifiedMessage);
		placeholders.add("message", placeholders.applyAfterColorize(getConfig().getString("formats." + userGroup + ".message")));

		Player player = e.getPlayer();

		// Chat render type
		Iterator<Player> targets = e.getRecipients().iterator();
		String type = getConfig().getString("options.type").toUpperCase();
		double distance = getConfig().getDouble("options.distance");

		List<String> worlds = null;
		List<String> ignoredStrings = new ArrayList<>();
		if (type.equals("PER_WORLD")) {
			worlds = new ArrayList<>();
			worlds.add(player.getWorld().getName());
			for (String groupName : getConfig().getKeys("options.per_world")) {
				List<String> worldsInGroup = getConfig().getStringList("options.per_world." + groupName);
				if (worldsInGroup.contains(player.getWorld().getName())) {
					worlds = worldsInGroup;
					break;
				}
			}
		}

		// Removing players which can't see message
		while (targets.hasNext()) {
			Player target = targets.next();

			if (target.equals(player))
				continue;

			// PER_WORLD type
			if (worlds != null) {
				if (!worlds.contains(target.getWorld().getName())) { // If group of worlds contains target world
					targets.remove();
					continue;
				}
			} else if (type.equals("DISTANCE") && (!player.getWorld().equals(target.getWorld()) || target.getLocation().distance(player.getLocation()) > distance)) { // If they are not in same world
				// or distance is too high
				targets.remove();
				continue;
			}
			ignoredStrings.add(target.getName());
		}

		ignoredStrings.add(e.getPlayer().getName());
		if (addColors)
			ignoredStrings.add(replacement + "§g");

		if (addIgnorePlaceholders)
			ignoredStrings.addAll(ignoredPlaceholders);

		// Chat notigications & colors
		StringContainer container = new StringContainer(placeholders.get("{message}"), 0, 16);
		for (Entry<String, String> entry : entrySetOfChatPlaceholders)
			container.replace(entry.getKey(), entry.getValue());

		placeholders.add("message", notificationReplace(player, colorize(player, container, ignoredStrings), e.getRecipients()));
		e.setMessage(placeholders.get("{message}")); // For other boring plugins
		e.setFormat(API.get().getMsgManager().sendMessageFromFileWithResult(getConfig(), "formats." + userGroup + ".chat", placeholders, BukkitLoader.getOnlinePlayers(), e.getPlayer()).replace("%",
				"%%"));
		e.getRecipients().clear(); // We have our own json format (see above)
	}

	public String notificationReplace(Player pinger, StringContainer container, Set<Player> targets) {
		String notificationColor = API.get().getConfigManager().getChat().getString("notification.color", "§c");
		for (Player player : targets) {
			if (pinger.equals(player))
				continue;
			int startAt = container.indexOfIgnoreCase(player.getName());
			if (startAt != -1) {
				notify(pinger, player);
				int length = player.getName().length() + notificationColor.length();
				String lastColors = buildLastColors(ColorUtils.getLastColors(container.substring(0, startAt)));
				String addedColors = lastColors.equals(notificationColor) ? "" : lastColors;
				if (lastColors.equals(notificationColor)) {
					container.insert(startAt + player.getName().length(), addedColors);
					startAt += length - notificationColor.length() + addedColors.length();
				} else {
					container.insert(startAt, notificationColor).insert(startAt + length, addedColors);
					startAt += length + addedColors.length();
				}
				int prev = startAt;
				while ((startAt = container.indexOfIgnoreCase(player.getName(), startAt)) != -1) {
					lastColors = buildLastColors(ColorUtils.getLastColors(lastColors + container.substring(prev, startAt)));
					addedColors = lastColors.equals(notificationColor) ? "" : lastColors;
					if (lastColors.equals(notificationColor)) {
						container.insert(startAt + player.getName().length(), addedColors);
						startAt += length - notificationColor.length() + addedColors.length();
					} else {
						container.insert(startAt, notificationColor).insert(startAt + length, addedColors);
						startAt += length + addedColors.length();
					}
					prev = startAt;
				}
			}
		}
		return container.toString();
	}

	private String buildLastColors(String colors) {
		if (colors.isEmpty())
			colors = "§f";
		else {
			char[] chars = colors.toCharArray();
			colors = "";
			for (char c : chars)
				colors += "§" + c;
		}
		return colors;
	}

	private void notify(Player pinger, Player target) {
		Config chat = API.get().getConfigManager().getChat();
		if (!chat.getString("notification.sound.name").isEmpty()) {
			Sound sound = Sound.valueOf(chat.getString("notification.sound.name"));
			if (sound != null) // Sound
				target.playSound(target.getLocation(), sound, chat.getFloat("notification.sound.volume"), chat.getFloat("notification.sound.pitch"));
		}

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().papi(target.getUniqueId()).add("pinger", pinger.getName());

		// CMDS
		Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Loader.class), () -> {
			for (String cmd : chat.getStringList("notification.commands"))
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders.apply(cmd));
		});
		// MSGS
		API.get().getMsgManager().sendMessageFromFile(chat, "notification.messages", placeholders, target);
	}

	public StringContainer colorize(Player sender, final StringContainer container, final List<String> protectedStrings) {
		if (container.isEmpty()
				|| !sender.hasPermission("css.chat.colors") && !sender.hasPermission("css.chat.gradient") && !sender.hasPermission("css.chat.hex") && !sender.hasPermission("css.chat.rainbow"))
			return container;
		if (sender.hasPermission("css.chat.colors"))
			for (int i = 0; i < container.length(); ++i) {
				char c = container.charAt(i);
				switch (c) {
				case '&':
					if (container.length() > i + 1) {
						char next = container.charAt(++i);
						if (isColorChar(next)) {
							container.setCharAt(i - 1, '§');
							container.setCharAt(i, Character.toLowerCase(next));
						}
					}
				}
			}
		if (sender.hasPermission("css.chat.gradient"))
			ColorUtils.gradient(container, protectedStrings);
		if (sender.hasPermission("css.chat.hex"))
			ColorUtils.color.replaceHex(container);
		if (sender.hasPermission("css.chat.rainbow")) {
			int startAt;
			if ((startAt = container.indexOf("&u")) != -1)
				ColorUtils.color.rainbow(container, startAt, container.length(), null, null, protectedStrings);
		}
		if (addColors) {
			int pos;
			while ((pos = container.indexOf("§g")) != -1) {
				String addedColors = ColorUtils.getLastColors(container.substring(0, pos - replacement.length()));
				if (addedColors.isEmpty())
					addedColors = "f";
				StringBuilder fixedColors = new StringBuilder();
				for (int i = 0; i < addedColors.length(); ++i) {
					fixedColors.append('§');
					fixedColors.append(addedColors.charAt(i));
				}
				container.replace(pos, pos + 2, fixedColors.toString());
			}
		}
		return container;
	}

	private boolean isColorChar(final int c) {
		return c <= 102 && c >= 97 || c <= 57 && c >= 48 || c <= 70 && c >= 65 || c <= 79 && c >= 75 || c <= 111 && c >= 107 || c == 114 || c == 82 || c == 120;
	}

	private List<String> playerNames(Player player) {
		List<String> names = new ArrayList<>();
		for (Player target : BukkitLoader.getOnlinePlayers())
			if (player.canSee(target))
				names.add(target.getName());
		return names;
	}
}
