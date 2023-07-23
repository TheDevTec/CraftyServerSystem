package me.devtec.craftyserversystem.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Ref;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class ChatListener implements Listener {

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());

		Config chat = API.get().getConfigManager().getChat();

		String userGroup = API.get().getPermissionHook().getGroup(e.getPlayer());
		if (!chat.exists("formats." + userGroup))
			userGroup = "default";

		placeholders.add("player", placeholders.apply(chat.getString("formats." + userGroup + ".name")));
		placeholders.add("message", e.getMessage());
		placeholders.add("message", placeholders.applyWithoutColors(chat.getString("formats." + userGroup + ".message")));

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
			// TODO - ignore

			if (target.equals(player) || target.hasPermission("SCR.Other.ChatTypeBypass") || !player.canSee(target))
				continue;

			// PER_WORLD type
			if (type.equals("PER_WORLD")) {
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
		// TODO - flood, antispam, ...

		// Chat notigications & colors
		placeholders.add("message", notificationReplace(player, colorize(player, placeholders.get("{message}"), players), e.getRecipients()));

		e.setMessage(placeholders.get("{message}")); // for console
		API.get().getMsgManager().sendMessageFromFile(chat, "formats." + userGroup + ".chat", placeholders, BukkitLoader.getOnlinePlayers());
		e.getRecipients().clear();
	}

	public static String notificationReplace(Player pinger, String msg, Set<Player> targets) {
		for (Player player : targets)
			if (player != pinger && msg.contains(player.getName()) && pinger.canSee(player)) {
				boolean endsWithName = msg.endsWith(player.getName());

				String notificationColor = API.get().getConfigManager().getChat().getString("notification.color", "§c");

				notify(pinger, player);

				String[] split = Pattern.compile(player.getName(), Pattern.CASE_INSENSITIVE).split(msg);
				if (split.length == 0) // Just Player
					return notificationColor + player.getName();

				String lastColors = ColorUtils.getLastColors(split[0]);
				if (lastColors.isEmpty())
					lastColors = "§f";
				else {
					char[] chars = lastColors.toCharArray();
					lastColors = "";
					for (char c : chars)
						lastColors += "§" + c;
				}
				StringContainer builder = new StringContainer(split[0]);
				for (int i = 1; i < split.length; ++i) {
					builder.append(notificationColor).append(player.getName()).append(lastColors).append(split[i]);
					lastColors = ColorUtils.getLastColors(lastColors + split[i]);
					if (lastColors.isEmpty())
						lastColors = "&f";
					else {
						char[] chars = lastColors.toCharArray();
						lastColors = "";
						for (char c : chars)
							lastColors += "§" + c;
					}
				}
				if (endsWithName)
					builder.append(notificationColor).append(player.getName());
				msg = builder.toString();
			}
		return msg;
	}

	private static void notify(Player pinger, Player target) {
		Config chat = API.get().getConfigManager().getChat();
		if (!chat.getString("notification.sound.name").isEmpty()) {
			Sound sound = Sound.valueOf(chat.getString("notification.sound.name"));
			if (sound != null) // Sound
				target.playSound(target.getLocation(), sound, chat.getFloat("notification.sound.volume"), chat.getFloat("notification.sound.pitch"));
		}

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().papi(target.getUniqueId()).add("pinger", pinger.getName());

		// CMDS
		BukkitLoader.getNmsProvider().postToMainThread(() -> {
			for (String cmd : chat.getStringList("notification.commands"))
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders.apply(cmd));
		});
		// MSGS
		API.get().getMsgManager().sendMessageFromFile(chat, "notification.messages", placeholders, target);
	}

	public static String colorize(Player sender, final String original, final List<String> protectedStrings) {
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

	private static boolean isColorChar(final int c) {
		return c <= 102 && c >= 97 || c <= 57 && c >= 48 || c <= 70 && c >= 65 || c <= 79 && c >= 75 || c <= 111 && c >= 107 || c == 114 || c == 82 || c == 120;
	}

	private static char toLowerCase(final int c) {
		switch (c) {
		case 65:
		case 66:
		case 67:
		case 68:
		case 69:
		case 70:
		case 75:
		case 76:
		case 77:
		case 78:
		case 79:
		case 82:
		case 85:
		case 88: {
			return (char) (c + 32);
		}
		default: {
			return (char) c;
		}
		}
	}
}
