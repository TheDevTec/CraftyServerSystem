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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
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

public class ChatListener implements CssListener {

	// AntiSpam
	private List<String> ignoredPlaceholders = Arrays.asList("[item]", "[inv]", "[ec]");
	private boolean antiSpamEnabled;
	@Nonnull
	private TempMap<UUID, Object[]> prevMsgs;
	private int maxMessages;
	private double minSimilarity;
	private boolean bypassAntiSpam;
	private boolean randomLettersAntiSpamEnabled;
	private int randomLettersMinimumLength;
	private int randomLettersMinimumWordLength;
	private int randomLettersMinimumSuspiciousWords;
	private double randomLettersMinimumSuspiciousRatio;
	private int randomLettersRepeatedSequenceThreshold;

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
	private int floodMaxPatternRepeats;
	private int floodMinWordsBetweenSameToIgnore;
	private boolean bypassAntiFlood;

	// AntiSwear
	private boolean antiSwearEnabled;
	@Nonnull
	private String replacement;
	private boolean addColors;
	@Nonnull
	private List<ChatHandlers.BadWordRule> profanityRules;
	private boolean bypassAntiSwear;
	private boolean antiSwearBlockEvent;
	private boolean antiSwearHistoryEnabled;
	private int antiSwearHistoryMessages;
	@Nonnull
	private TempMap<UUID, List<String>> antiSwearHistory;
	@Nonnull
	private TempMap<UUID, ChatHandlers.LanguageProfile> antiSwearLanguageProfiles;
	private List<ChatHandlers.AllowedPhraseRule> allowedPhrases;
	private List<String> contextualPhrases;
	private boolean autoModDebugEnabled;
	private boolean autoModDebugConsole;
	@Nonnull
	private String autoModDebugPermission;

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
	private boolean enabledChatIgnore;
	private boolean chatIgnoreOnlyPings;

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
		enabledChatIgnore = API.get().getCommandManager().getRegistered().containsKey("chatignore");
		chatIgnoreOnlyPings = API.get().getConfigManager().getMain().getBoolean("chatIgnore.only-pings-in-chat");
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
			cdMsgs = new TempList<>(
					TimeUtils.timeFromString(getConfig().getString("antiSpam.cooldown-per-message.time")) * 20);
		else {
			cdMsgs.clear();
			cdMsgs.setCacheTime(
					TimeUtils.timeFromString(getConfig().getString("antiSpam.cooldown-per-message.time")) * 20);
		}
		maxMessages = getConfig().getInt("antiSpam.maximum-messages") + 1;
		minSimilarity = getConfig().getDouble("antiSpam.minimal-similarity");
		bypassAntiSpam = getConfig().getBoolean("antiSpam.bypass-enabled");
		randomLettersAntiSpamEnabled = getConfig().getBoolean("antiSpam.random-letters.enabled");
		randomLettersMinimumLength = getConfig().getInt("antiSpam.random-letters.minimum-length");
		if (randomLettersMinimumLength <= 0)
			randomLettersMinimumLength = 18;
		randomLettersMinimumWordLength = getConfig().getInt("antiSpam.random-letters.minimum-word-length");
		if (randomLettersMinimumWordLength <= 0)
			randomLettersMinimumWordLength = 6;
		randomLettersMinimumSuspiciousWords = getConfig()
				.getInt("antiSpam.random-letters.minimum-suspicious-words");
		if (randomLettersMinimumSuspiciousWords <= 0)
			randomLettersMinimumSuspiciousWords = 2;
		randomLettersMinimumSuspiciousRatio = getConfig()
				.getDouble("antiSpam.random-letters.minimum-suspicious-ratio");
		if (randomLettersMinimumSuspiciousRatio <= 0)
			randomLettersMinimumSuspiciousRatio = 0.6;
		randomLettersRepeatedSequenceThreshold = getConfig()
				.getInt("antiSpam.random-letters.repeated-sequence-threshold");
		if (randomLettersRepeatedSequenceThreshold <= 0)
			randomLettersRepeatedSequenceThreshold = 4;
		antiFloodEnabled = getConfig().getBoolean("antiFlood.enabled");
		floodMaxChars = getConfig().getInt("antiFlood.maximum-chars");
		floodMaxCapsChars = getConfig().getInt("antiFlood.maximum-caps-chars");
		floodMaxNumbers = getConfig().getInt("antiFlood.maximum-numbers");
		floodMaxSameWords = getConfig().getInt("antiFlood.maximum-same-words-in-row");
		floodMaxPatternRepeats = getConfig().getInt("antiFlood.maximum-pattern-repeats", 2);
		if (floodMaxPatternRepeats < 1)
			floodMaxPatternRepeats = 2;
		floodMinWordsBetweenSameToIgnore = getConfig().getInt("antiFlood.words-between-same-to-ignore");
		bypassAntiFlood = getConfig().getBoolean("antiFlood.bypass-enabled");
		antiSwearEnabled = getConfig().getBoolean("antiSwear.enabled");
		replacement = getConfig().getString("antiSwear.replacement");
		addColors = replacement.indexOf('§') != -1;
		profanityRules = new ArrayList<>();
		loadProfanityRules("antiSwear.words");
		loadProfanityRules("antiSwear.language-rules");
		allowedPhrases = new ArrayList<>();
		for (String phrase : getConfig().getStringList("antiSwear.allowed-phrases")) {
			if (phrase.indexOf(":") == -1) {
				Loader.getPlugin().getLogger().warning("Failed loading allowed phrase '" + phrase
						+ "' - Incorrect format! Format must be: 'swearWord:allowedPhrase'");
				continue;
			}
			String[] split = phrase.split(":", 2);
			ChatHandlers.AllowedPhraseRule allowed = new ChatHandlers.AllowedPhraseRule(split[0], split[1].trim());
			if (!allowed.isValid()) {
				Loader.getPlugin().getLogger().warning("Failed loading allowed phrase '" + phrase
						+ "' - Invalid normalized value");
				continue;
			}
			allowedPhrases.add(allowed);
		}
		contextualPhrases = new ArrayList<>();
		for (String phrase : getConfig().getStringList("antiSwear.contextual-phrases")) {
			String normalizedPhrase = ChatHandlers.normalizeAntiSwearPhrase(phrase);
			if (!normalizedPhrase.isEmpty())
				contextualPhrases.add(normalizedPhrase);
		}
		bypassAntiSwear = getConfig().getBoolean("antiSwear.bypass-enabled");
		antiSwearBlockEvent = getConfig().getBoolean("antiSwear.block-event");
		antiSwearHistoryEnabled = getConfig().getBoolean("antiSwear.history.enabled");
		autoModDebugEnabled = getConfig().getBoolean("antiSwear.automod-debug.enabled");
		autoModDebugConsole = getConfig().getBoolean("antiSwear.automod-debug.console");
		autoModDebugPermission = getConfig().getString("antiSwear.automod-debug.permission",
				"css.chat.automod.debug");
		antiSwearHistoryMessages = getConfig().getInt("antiSwear.history.maximum-messages");
		if (antiSwearHistoryMessages <= 0)
			antiSwearHistoryMessages = 4;
		long antiSwearHistoryCache = TimeUtils.timeFromString(
				getConfig().getString("antiSwear.history.cache", "20s")) * 20;
		if (antiSwearHistory == null)
			antiSwearHistory = new TempMap<>(antiSwearHistoryCache);
		else {
			antiSwearHistory.clear();
			antiSwearHistory.setCacheTime(antiSwearHistoryCache);
		}
		long antiSwearLanguageProfileCache = TimeUtils.timeFromString(
				getConfig().getString("antiSwear.language-profile.cache", "30m")) * 20;
		if (antiSwearLanguageProfiles == null)
			antiSwearLanguageProfiles = new TempMap<>(antiSwearLanguageProfileCache);
		else {
			antiSwearLanguageProfiles.clear();
			antiSwearLanguageProfiles.setCacheTime(antiSwearLanguageProfileCache);
		}
		antiAdEnabled = getConfig().getBoolean("antiAd.enabled");
		bypassAntiAd = getConfig().getBoolean("antiAd.bypass-enabled");
		antiAdWhitelist = getConfig().getStringList("antiAd.whitelist");
		entrySetOfChatPlaceholders = null;
		if (chatPlaceholders == null)
			chatPlaceholders = new HashMap<>();
		else
			chatPlaceholders.clear();
		for (String key : getConfig().getKeys("chat-placeholders"))
			chatPlaceholders.put(getConfig().getString("chat-placeholders." + key + ".text"),
					getConfig().getString("chat-placeholders." + key + ".replacement"));
		entrySetOfChatPlaceholders = chatPlaceholders.entrySet();
	}

	private void loadProfanityRules(String path) {
		for (String value : getConfig().getStringList(path)) {
			ChatHandlers.BadWordRule rule = ChatHandlers.parseProfanityRule(value);
			if (rule != null && !rule.getWord().isEmpty())
				profanityRules.add(rule);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncPlayerChatEvent e) {
		if (e.isCancelled())
			return;
		List<String> playerNames = playerNames(e.getPlayer());
		String modifiedMessage = antiFloodEnabled
				&& (bypassAntiFlood ? !e.getPlayer().hasPermission("css.chat.bypass.antiflood") : true)
				? ChatHandlers.antiFlood(e.getMessage(), ChatHandlers.match(e.getMessage(), playerNames),
						floodMaxNumbers, floodMaxChars, floodMaxCapsChars, floodMaxSameWords,
						floodMinWordsBetweenSameToIgnore, floodMaxPatternRepeats)
						: e.getMessage();

		if (antiAdEnabled && (bypassAntiAd ? !e.getPlayer().hasPermission("css.chat.bypass.antiad") : true)
				&& ChatHandlers.antiAd(modifiedMessage, antiAdWhitelist)) {
			e.setCancelled(true);
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd",
					PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd-admin",
					PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("message", modifiedMessage),
					"css.chat.antiad");
			return;
		}
		boolean addIgnorePlaceholders = e.getPlayer().hasPermission("css.chat.placeholders");
		ItemStack itemInHand;
		if (antiAdEnabled && addIgnorePlaceholders
				&& (bypassAntiAd ? !e.getPlayer().hasPermission("css.chat.bypass.antiad") : true)
				&& (itemInHand = e.getPlayer().getItemInHand()).getType() != Material.AIR
				&& modifiedMessage.indexOf("[item]") != -1
				&& ChatHandlers.antiAd(itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()
						? itemInHand.getItemMeta().getDisplayName()
								: null, antiAdWhitelist)) {
			e.setCancelled(true);
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd",
					PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
			API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiAd-admin",
					PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("message",
							modifiedMessage.replace("[item]", getConfig().getString("placeholders.item.replace")
									.replace("{itemName}", itemInHand.getItemMeta().getDisplayName()))),
					"css.chat.antiad");
			return;
		}

		if (antiSpamEnabled && (bypassAntiSpam ? !e.getPlayer().hasPermission("css.chat.bypass.antispam") : true)) {
			if (antiSpamCooldownEnabled
					&& (bypassAntiSpamCooldown ? !e.getPlayer().hasPermission("css.chat.bypass.anticooldown") : true)) {
				if (cdMsgs.contains(e.getPlayer().getUniqueId())) {
					e.setCancelled(true);
					API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiSpam-Cooldown",
							PlaceholdersExecutor.i().add("player", e.getPlayer().getName()).add("time",
									TimeUtils.timeToString(Math.max(1,
											(cdMsgs.getTimeOf(e.getPlayer().getUniqueId())
													- System.currentTimeMillis() / 50L + cdMsgs.getCacheTime()) / 20))),
							e.getPlayer());
					return;
				}
				cdMsgs.add(e.getPlayer().getUniqueId());
			}
			if (randomLettersAntiSpamEnabled && ChatHandlers.randomLettersSpam(modifiedMessage,
					randomLettersMinimumLength, randomLettersMinimumWordLength, randomLettersMinimumSuspiciousWords,
					randomLettersMinimumSuspiciousRatio, randomLettersRepeatedSequenceThreshold) || ChatHandlers.processAntiSpam(e.getPlayer().getUniqueId(), modifiedMessage, prevMsgs, maxMessages,
							minSimilarity)) {
				e.setCancelled(true);
				API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiSpam",
						PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
				return;
			}
		}
		if (antiSwearEnabled && (bypassAntiSwear ? !e.getPlayer().hasPermission("css.chat.bypass.antiswear") : true)) {
			int[][] ignoredSections = ChatHandlers.match(modifiedMessage, playerNames);
			Set<String> profileLanguages = getProfileLanguages(e.getPlayer().getUniqueId());
			ChatHandlers.ProfanityResult currentResult = ChatHandlers.checkProfanity(modifiedMessage, profanityRules,
					allowedPhrases, ignoredSections, profileLanguages);
			boolean currentMessageContainsSwear = currentResult.hasMatch();
			String currentContextMatch = ChatHandlers.findContextualProfanity(modifiedMessage, contextualPhrases);
			boolean currentContextContainsSwear = currentContextMatch != null;
			boolean historyContainsSwear = false;
			ChatHandlers.ProfanityResult historyResult = null;
			String historyContextMatch = null;
			if (!currentMessageContainsSwear && !currentContextContainsSwear && antiSwearHistoryEnabled) {
				historyResult = ChatHandlers.checkSplitProfanity(antiSwearHistory.get(e.getPlayer().getUniqueId()),
						modifiedMessage, profanityRules, allowedPhrases, profileLanguages);
				String historyMessage = buildAntiSwearHistoryMessage(e.getPlayer().getUniqueId(), modifiedMessage);
				historyContextMatch = ChatHandlers.findContextualProfanity(historyMessage, contextualPhrases);
				historyContainsSwear = historyResult.hasMatch() || historyContextMatch != null;
			}
			debugAutoMod(e.getPlayer(), "message", currentResult, currentContextMatch);
			if (historyContainsSwear)
				debugAutoMod(e.getPlayer(), "history", historyResult, historyContextMatch);
			if (antiSwearBlockEvent && (currentMessageContainsSwear || historyResult != null && historyResult.hasMatch())
					|| currentContextContainsSwear || historyContextMatch != null) {
				e.setCancelled(true);
				clearAntiSwearHistory(e.getPlayer().getUniqueId());
				API.get().getMsgManager().sendMessageFromFile(getConfig(), "translations.antiSwear",
						PlaceholdersExecutor.i().add("player", e.getPlayer().getName()), e.getPlayer());
				return;
			}
			if (currentMessageContainsSwear) {
				clearAntiSwearHistory(e.getPlayer().getUniqueId());
				modifiedMessage = currentResult.replace(modifiedMessage, replacement, addColors);
			} else if (historyResult != null && historyResult.hasMatch()) {
				clearAntiSwearHistory(e.getPlayer().getUniqueId());
				modifiedMessage = historyResult.replace(modifiedMessage, replacement, addColors);
			} else if (antiSwearHistoryEnabled)
				addAntiSwearHistory(e.getPlayer().getUniqueId(), modifiedMessage);
		}
		learnProfile(e.getPlayer().getUniqueId(), modifiedMessage);

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getPlayer().getName())
				.add("player_name", e.getPlayer().getName()).papi(e.getPlayer().getUniqueId());

		String userGroup = API.get().getPermissionHook().getGroup(e.getPlayer().getName());
		if (!getConfig().exists("formats." + userGroup)) {
			userGroup = "__OTHER__";
			if (!getConfig().exists("formats." + userGroup))
				userGroup = "default";
		}

		placeholders.add("player", placeholders.apply(getConfig().getString("formats." + userGroup + ".name",
				getConfig().getString("formats.__OTHER__.name"))));
		placeholders.add("message",modifiedMessage);

		placeholders.add("message", modifiedMessage = placeholders.apply(getConfig()
				.getString("formats." + userGroup + ".message", getConfig().getString("formats.__OTHER__.message"))));

		Player player = e.getPlayer();

		// Chat render type
		Iterator<Player> targets = e.getRecipients().iterator();
		String type = getConfig().getString("options.type").toUpperCase();
		double distance = getConfig().getDouble("options.distance");

		List<String> worlds = null;
		List<String> ignoredStrings = new ArrayList<>();
		if ("PER_WORLD".equals(type)) {
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
			} else if ("DISTANCE".equals(type) && (!player.getWorld().equals(target.getWorld())
					|| target.getLocation().distance(player.getLocation()) > distance)) { // If they are not in same
				// world
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

		List<Player> pinged = enabledChatIgnore && chatIgnoreOnlyPings ? new ArrayList<>() : null;
		placeholders.add("message",
				notificationReplace(player, pinged, colorize(player, container, ignoredStrings), e.getRecipients()));
		e.setMessage(placeholders.get("{message}")); // For other boring plugins
		if (enabledChatIgnore) {
			Iterator<Player> receivers = e.getRecipients().iterator();
			while (receivers.hasNext()) {
				Player target = receivers.next();
				if (target.equals(e.getPlayer()))
					continue;
				if (me.devtec.shared.API.getUser(target.getUniqueId()).getBoolean("css.chatignore")) {
					if (pinged!=null && chatIgnoreOnlyPings && pinged.contains(target))
						continue;
					receivers.remove();
				}
			}
		}
		e.setFormat(API.get().getMsgManager()
				.sendMessageFromFileWithResult(getConfig(),
						getConfig().existsKey("formats." + userGroup + ".chat") ? "formats." + userGroup + ".chat"
								: "formats.default.chat",
								placeholders, e.getRecipients(), e.getPlayer())
				.replace("%", "%%"));
		e.getRecipients().clear(); // We have our own json format (see above)
	}

	private String buildAntiSwearHistoryMessage(UUID uniqueId, String message) {
		List<String> history = antiSwearHistory.get(uniqueId);
		if (history == null || history.isEmpty())
			return message;
		StringBuilder builder = new StringBuilder(message.length() + history.size() * 16);
		for (String previousMessage : history) {
			if (builder.length() != 0)
				builder.append(' ');
			builder.append(previousMessage);
		}
		if (builder.length() != 0)
			builder.append(' ');
		builder.append(message);
		return builder.toString();
	}

	private void addAntiSwearHistory(UUID uniqueId, String message) {
		if (message == null || message.trim().isEmpty())
			return;
		List<String> history = antiSwearHistory.get(uniqueId);
		if (history == null)
			history = new ArrayList<>(antiSwearHistoryMessages);
		history.add(message);
		while (history.size() > antiSwearHistoryMessages)
			history.remove(0);
		antiSwearHistory.put(uniqueId, history);
	}

	private void clearAntiSwearHistory(UUID uniqueId) {
		if (antiSwearHistory != null)
			antiSwearHistory.remove(uniqueId);
	}

	private Set<String> getProfileLanguages(UUID uniqueId) {
		ChatHandlers.LanguageProfile profile = antiSwearLanguageProfiles.get(uniqueId);
		return profile == null ? java.util.Collections.<String>emptySet() : profile.getLanguages();
	}

	private void learnProfile(UUID uniqueId, String message) {
		if (message == null || message.length() < 3)
			return;
		ChatHandlers.LanguageProfile profile = antiSwearLanguageProfiles.get(uniqueId);
		if (profile == null)
			profile = new ChatHandlers.LanguageProfile();
		profile.learn(message);
		antiSwearLanguageProfiles.put(uniqueId, profile);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID uniqueId = event.getPlayer().getUniqueId();
		antiSwearHistory.remove(uniqueId);
		antiSwearLanguageProfiles.remove(uniqueId);
	}

	private void debugAutoMod(Player player, String source, ChatHandlers.ProfanityResult result,
			String contextualPhrase) {
		if (!autoModDebugEnabled)
			return;
		if (result != null)
			for (ChatHandlers.ProfanityMatch match : result.getMatches())
				if (match.getDecision() == ChatHandlers.ProfanityDecision.MATCH)
					sendAutoModDebug("player=" + player.getName() + " source=" + source + " original="
							+ cleanAutoModDebug(match.getOriginal()) + " rule=" + match.getRule().getWord() + " type="
							+ match.getType() + " language=" + (match.getLanguage() == null ? "unknown" : match.getLanguage()));
		if (contextualPhrase != null)
			sendAutoModDebug("player=" + player.getName() + " source=" + source + " contextual="
					+ cleanAutoModDebug(contextualPhrase));
	}

	private void sendAutoModDebug(String details) {
		String message = "[AutoMod] " + details;
		if (autoModDebugConsole)
			Bukkit.getConsoleSender().sendMessage(message);
		for (Player target : Bukkit.getOnlinePlayers())
			if (target.hasPermission(autoModDebugPermission))
				target.sendMessage(message);
	}

	private String cleanAutoModDebug(String value) {
		return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
	}

	public String notificationReplace(Player pinger, List<Player> pinged, StringContainer container,
			Set<Player> targets) {
		String notificationColor = API.get().getConfigManager().getChat().getString("notification.color", "§c");
		for (Player player : targets) {
			if (pinger.equals(player))
				continue;
			int startAt = container.indexOfIgnoreCase(player.getName());
			if (startAt != -1) {
				notify(pinger, player);
				if (pinged != null)
					pinged.add(player);
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
					lastColors = buildLastColors(
							ColorUtils.getLastColors(lastColors + container.substring(prev, startAt)));
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
				target.playSound(target.getLocation(), sound, chat.getFloat("notification.sound.volume"),
						chat.getFloat("notification.sound.pitch"));
		}

		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().papi(target.getUniqueId()).add("pinger",
				pinger.getName());

		// CMDS
		Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Loader.class), () -> {
			for (String cmd : chat.getStringList("notification.commands"))
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders.apply(cmd));
		});
		// MSGS
		API.get().getMsgManager().sendMessageFromFile(chat, "notification.messages", placeholders, target);
	}

	public StringContainer colorize(Player sender, final StringContainer container,
			final List<String> protectedStrings) {
		if (container.isEmpty()
				|| !sender.hasPermission("css.chat.colors") && !sender.hasPermission("css.chat.gradient")
				&& !sender.hasPermission("css.chat.hex") && !sender.hasPermission("css.chat.rainbow"))
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
		return c <= 102 && c >= 97 || c <= 57 && c >= 48 || c <= 70 && c >= 65 || c <= 79 && c >= 75
				|| c <= 111 && c >= 107 || c == 114 || c == 82 || c == 120;
	}

	private List<String> playerNames(Player player) {
		List<String> names = new ArrayList<>();
		for (Player target : BukkitLoader.getOnlinePlayers())
			if (player.canSee(target))
				names.add(target.getName());
		return names;
	}
}
