package me.devtec.craftyserversystem.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.Nonnull;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.ChatHandlers;
import me.devtec.shared.Ref;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.dataholder.cache.TempMap;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class ChatListener implements Listener {

	// AntiSpam
	private boolean antiSpamEnabled;
	@Nonnull
	private TempMap<UUID, Object[]> prevMsgs;
	private int maxMessages;
	private boolean bypassAntiSpam;

	// AntiFlood
	private boolean antiFloodEnabled;
	private int floodMaxChars;
	private int floodMaxNumbers;
	private boolean bypassAntiFlood;

	// AntiSwear
	private boolean antiSwearEnabled;
	@Nonnull
	private String replacement;
	@Nonnull
	private List<String> words;
	private boolean bypassAntiSwear;
	private List<String> allowedPhrases;

	// AntiAd pattern
	private boolean antiAdEnabled;
	private boolean bypassAntiAd;
	@Nonnull
	private List<String> antiAdWhitelist;

	public ChatListener() {
		reload();
	}

	public void reload() {
		Config chat = API.get().getConfigManager().getChat();
		antiSpamEnabled = chat.getBoolean("antiSpam.enabled");
		if (prevMsgs == null)
			prevMsgs = new TempMap<>(TimeUtils.timeFromString(chat.getString("antiSpam.cache")) * 20);
		else {
			prevMsgs.clear();
			prevMsgs.setCacheTime(TimeUtils.timeFromString(chat.getString("antiSpam.cache")) * 20);
		}
		maxMessages = chat.getInt("antiSpam.maximum-messages") + 1;
		bypassAntiSpam = chat.getBoolean("antiSpam.bypass-enabled");
		antiFloodEnabled = chat.getBoolean("antiFlood.enabled");
		floodMaxChars = chat.getInt("antiFlood.maximum-chars");
		floodMaxNumbers = chat.getInt("antiFlood.maximum-numbers");
		bypassAntiFlood = chat.getBoolean("antiFlood.bypass-enabled");
		antiSwearEnabled = chat.getBoolean("antiSwear.enabled");
		replacement = chat.getString("antiSwear.replacement");
		words = chat.getStringList("antiSwear.words");
		allowedPhrases = chat.getStringList("antiSwear.allowed-phrases");
		bypassAntiSwear = chat.getBoolean("antiSwear.bypass-enabled");
		antiAdEnabled = chat.getBoolean("antiAd.enabled");
		bypassAntiAd = chat.getBoolean("antiAd.bypass-enabled");
		antiAdWhitelist = chat.getStringList("antiAd.whitelist");
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		Config chat = API.get().getConfigManager().getChat();
		String[] playerNames = playerNames(e.getPlayer());

		String modifiedMessage = antiFloodEnabled && (bypassAntiFlood ? !e.getPlayer().hasPermission("gk.chat.bypass.antiflood") : true)
				? ChatHandlers.antiFlood(e.getMessage(), ChatHandlers.match(e.getMessage(), playerNames), floodMaxNumbers, floodMaxChars)
				: e.getMessage();

		if (antiSpamEnabled && (bypassAntiSpam ? !e.getPlayer().hasPermission("gk.chat.bypass.antispam") : true)
				&& ChatHandlers.processAntiSpam(e.getPlayer().getUniqueId(), modifiedMessage, prevMsgs, maxMessages)) {
			e.setCancelled(true);
			API.get().getMsgManager().sendMessageFromFile(chat, "translations.antiSpam", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
			return;

		}
		if (antiSwearEnabled && (bypassAntiSwear ? !e.getPlayer().hasPermission("gk.chat.bypass.antiswear") : true))
			if (chat.getBoolean("antiSwear.block-event")) {
				if (ChatHandlers.antiSwear(modifiedMessage, words, allowedPhrases, ChatHandlers.match(modifiedMessage, playerNames))) {
					e.setCancelled(true);
					API.get().getMsgManager().sendMessageFromFile(chat, "translations.antiSwear", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
					return;
				}
			} else
				modifiedMessage = ChatHandlers.antiSwearReplace(modifiedMessage, words, allowedPhrases, ChatHandlers.match(modifiedMessage, playerNames), replacement);
		if (antiAdEnabled && (bypassAntiAd ? !e.getPlayer().hasPermission("gk.chat.bypass.antiad") : true) && ChatHandlers.antiAd(modifiedMessage, antiAdWhitelist)) {
			e.setCancelled(true);
			API.get().getMsgManager().sendMessageFromFile(chat, "translations.antiAd", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
			API.get().getMsgManager().sendMessageFromFile(chat, "translations.antiAd-admin", PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("message", modifiedMessage),
					"gh.chat.antiad");
			return;
		}

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());

		String userGroup = API.get().getPermissionHook().getGroup(e.getPlayer());
		if (!chat.exists("formats." + userGroup))
			userGroup = "default";

		placeholders.add("player", placeholders.apply(chat.getString("formats." + userGroup + ".name")));
		placeholders.add("message", modifiedMessage);
		placeholders.add("message", placeholders.applyAfterColorize(chat.getString("formats." + userGroup + ".message")));

		Player player = e.getPlayer();

		// Chat render type
		Iterator<Player> targets = e.getRecipients().iterator();
		String type = chat.getString("options.type").toUpperCase();
		double distance = chat.getDouble("options.distance");

		List<String> worlds = null;
		List<String> players = new ArrayList<>();
		if (type.equals("PER_WORLD")) {
			worlds = new ArrayList<>();
			for (String groupName : chat.getKeys("options.per_world"))
				if (chat.getStringList("options.per_world." + groupName).contains(player.getLocation().getWorld().getName()))
					worlds = chat.getStringList("options.per_world." + groupName);
			if (worlds.isEmpty()) // Invalid or empty group
				worlds.add(player.getLocation().getWorld().getName());
		}

		// Removing players which can't see message
		while (targets.hasNext()) {
			Player target = targets.next();

			if (target.equals(player))
				continue;

			// PER_WORLD type
			if (worlds != null) {
				if (!worlds.contains(target.getLocation().getWorld().getName())) // If group of worlds contains target world
					targets.remove();
				continue;
			}

			// DISTANCE
			if (type.equals("DISTANCE")) {
				if (!player.getWorld().equals(target.getWorld()) || target.getLocation().distance(player.getLocation()) > distance) // If they are not in same world or distance is too high
					targets.remove();
				continue;
			}
			players.add(target.getName());
		}

		players.add(e.getPlayer().getName());

		// Chat notigications & colors

		placeholders.add("message", notificationReplace(player, colorize(player, placeholders.get("{message}"), players), e.getRecipients())); // notifikace!
		e.setMessage(placeholders.get("{message}")); // pro pluginy
		e.setFormat(API.get().getMsgManager().sendMessageFromFileWithResult(chat, "formats." + userGroup + ".chat", placeholders, BukkitLoader.getOnlinePlayers()).replace("%", "%%")); // pro konzoli a
																																														// hrace
		e.getRecipients().clear(); // mame vlastni json zpravy (viz metoda vyse)
	}

	public String notificationReplace(Player pinger, String msg, Set<Player> targets) {
		StringContainer builder = new StringContainer(msg);
		String notificationColor = API.get().getConfigManager().getChat().getString("notification.color", "§c");
		for (Player player : targets)
			if (!player.getUniqueId().equals(pinger.getUniqueId()) && pinger.canSee(player)) {
				int startAt = builder.indexOfIgnoreCase(player.getName());
				if (startAt != -1) {
					notify(pinger, player);
					if (msg.length() == player.getName().length()) // Just Player
						return notificationColor + player.getName();
					String lastColors = ColorUtils.getLastColors(builder.substring(0, startAt));
					if (lastColors.isEmpty())
						lastColors = "§f";
					else {
						char[] chars = lastColors.toCharArray();
						lastColors = "";
						for (char c : chars)
							lastColors += "§" + c;
					}
					while ((startAt = builder.indexOfIgnoreCase(player.getName(), startAt)) != -1) {
						String nextColors = ColorUtils.getLastColors(lastColors + builder.substring(startAt + player.getName().length()));
						builder.replace(startAt, startAt + player.getName().length(), notificationColor + player.getName() + lastColors);
						lastColors = nextColors;
						if (lastColors.isEmpty())
							lastColors = "§f";
						else {
							char[] chars = lastColors.toCharArray();
							lastColors = "";
							for (char c : chars)
								lastColors += "§" + c;
						}
						startAt += player.getName().length();
					}
				}
			}
		return builder.toString();
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

	public String colorize(Player sender, final String original, final List<String> protectedStrings) {
		if (original == null || original.trim().isEmpty()
				|| !sender.hasPermission("css.chat.colors") && !sender.hasPermission("css.chat.gradient") && !sender.hasPermission("css.chat.hex") && !sender.hasPermission("css.chat.rainbow"))
			return original;
		String msg = original;
		if (sender.hasPermission("css.chat.colors")) {
			final StringContainer builder = new StringContainer(original.length() + 16);
			for (int i = 0; i < original.length(); ++i) {
				final char c = original.charAt(i);
				if (c == '&' && original.length() > i + 1) {
					final char next = original.charAt(++i);
					if (isColorChar(next))
						builder.append('§').append(toLowerCase(next));
					else
						builder.append(c).append(next);
				} else
					builder.append(c);
			}
			msg = builder.toString();
		}
		if (ColorUtils.color != null) {
			if (!Ref.serverType().isBukkit() || Ref.isNewerThan(15)) {
				if (sender.hasPermission("css.chat.gradient"))
					msg = ColorUtils.gradient(msg, protectedStrings);
				if (sender.hasPermission("css.chat.hex"))
					if (msg.indexOf(35) != -1)
						msg = ColorUtils.color.replaceHex(msg);
			}
			if (sender.hasPermission("css.chat.rainbow"))
				if (msg.indexOf("&u") != -1)
					msg = ColorUtils.color.rainbow(msg, null, null, protectedStrings);
		}
		return msg;
	}

	private boolean isColorChar(final int c) {
		return c <= 102 && c >= 97 || c <= 57 && c >= 48 || c <= 70 && c >= 65 || c <= 79 && c >= 75 || c <= 111 && c >= 107 || c == 114 || c == 82 || c == 120;
	}

	private char toLowerCase(final int c) {
		if (c >= 65 && c <= 79 || c == 82 || c == 85 || c == 88)
			return (char) (c + 32);
		return (char) c;
	}

	private String[] playerNames(Player player) {
		List<String> names = new ArrayList<>();
		for (Player target : BukkitLoader.getOnlinePlayers())
			if (player.canSee(target))
				names.add(target.getName());
		return names.toArray(new String[0]);
	}
}
