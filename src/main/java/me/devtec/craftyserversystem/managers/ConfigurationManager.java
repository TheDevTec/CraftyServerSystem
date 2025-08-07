package me.devtec.craftyserversystem.managers;

import java.io.File;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.Loader;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.merge.MergeStandards;
import me.devtec.shared.utility.StreamUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.game.Position;

public class ConfigurationManager {
	protected static final String FILES_PATH = "plugins/CraftyServerSystem/";

	private Config main;
	private Config translations;
	private Config commands;
	private Config cooldowns;
	private Config join;
	private Config quit;
	private Config death;
	private Config tab;
	private Config bossbar;
	private Config scoreboard;
	private Config chat;
	private Config kits;
	private Config economy;
	private Config placeholders;
	private Config serverMotd;
	private Config consoleFilter;
	private Config animations;
	private String prefix = "";
	private Position spawn;
	private long teleportRequestTime;

	public ConfigurationManager() {
		main = new Config(FILES_PATH + "config.yml");
		translations = new Config(FILES_PATH + "translations.yml");
		commands = new Config(FILES_PATH + "commands.yml");
		cooldowns = new Config(FILES_PATH + "cooldowns.yml");
		join = new Config(FILES_PATH + "events/join.yml");
		quit = new Config(FILES_PATH + "events/quit.yml");
		death = new Config(FILES_PATH + "events/death.yml");
		chat = new Config(FILES_PATH + "events/chat.yml");
		scoreboard = new Config(FILES_PATH + "events/scoreboard.yml");
		tab = new Config(FILES_PATH + "events/tablist.yml");
		bossbar = new Config(FILES_PATH + "events/bossbar.yml");
		kits = new Config(FILES_PATH + "kits.yml");
		economy = new Config(FILES_PATH + "economy.yml");
		placeholders = new Config(FILES_PATH + "placeholders.yml");
		serverMotd = new Config(FILES_PATH + "events/server-motd.yml");
		consoleFilter = new Config(FILES_PATH + "events/console-filter.yml");
		animations = new Config(FILES_PATH + "animations.yml");
	}

	public ConfigurationManager initFromJar() {
		merge(main, "config.yml");
		merge(translations, "translations.yml");
		merge(commands, "commands.yml");
		merge(cooldowns, "cooldowns.yml");
		merge(join, "join.yml");
		merge(quit, "quit.yml");
		merge(death, "death.yml");
		if (!chat.exists("formats.default"))
			merge(chat, "chat.yml");
		if (!scoreboard.exists("title"))
			merge(scoreboard, "scoreboard.yml");
		if (!tab.exists("header"))
			merge(tab, "tablist.yml");
		if (!bossbar.exists("text"))
			merge(bossbar, "bossbar.yml");
		merge(kits, "kits.yml");
		merge(economy, "economy.yml");
		merge(placeholders, "placeholders.yml");
		if(!new File(FILES_PATH+"guis").exists()) {
			Config.loadFromPlugin(Loader.getPlugin().getClass(), "guis/heal/main.yml", FILES_PATH + "storage/guis/heal/main.yml").save("yaml");
			Config.loadFromPlugin(Loader.getPlugin().getClass(), "guis/shop/buy_menu.yml", FILES_PATH + "storage/guis/shop/buy_menu.yml").save("yaml");
			Config.loadFromPlugin(Loader.getPlugin().getClass(), "guis/shop/main.yml", FILES_PATH + "storage/guis/shop/main.yml").save("yaml");
		}
		if (!serverMotd.exists("motds"))
			merge(serverMotd, "server-motd.yml");
		merge(consoleFilter, "console-filter.yml");
		if (animations.getKeys().isEmpty())
			merge(animations, "animations.yml");
		prefix = getMain().getString("prefix", "");
		teleportRequestTime = TimeUtils.timeFromString(getMain().getString("teleport-request-time"));
		return this;
	}

	private void merge(Config origin, String path) {
		ClassLoader classLoader = Loader.getPlugin().getClass().getClassLoader();
		if (origin.merge(new Config().reload(StreamUtils.fromStream(classLoader.getResourceAsStream(path))),
				MergeStandards.DEFAULT))
			origin.save("yaml");
	}

	public String getPrefix() {
		return prefix;
	}

	public long getTeleportRequestTime() {
		return teleportRequestTime;
	}

	public void setTeleportRequestTime(long time) {
		teleportRequestTime = time;
	}

	public Config getWarpsStorage() {
		return new Config(FILES_PATH + "storage/warps.yml");
	}

	public Config getBansStorage() {
		return new Config(FILES_PATH + "storage/banlist.yml");
	}

	public Position getSpawn() {
		return spawn;
	}

	public void setSpawn(Position position) {
		spawn = position;
		Config data = new Config(FILES_PATH + "spawn.yml");
		data.set("spawn", position);
		data.save("yaml");
	}

	public void loadSpawn() {
		Config data = new Config(FILES_PATH + "spawn.yml");
		spawn = data.getAs("spawn", Position.class,
				Position.fromLocation(Bukkit.getWorlds().get(0).getSpawnLocation()));
	}

	public Config getMain() {
		return main;
	}

	public Config getTranslations() {
		return translations;
	}

	public Config getKits() {
		return kits;
	}

	public Config getEconomy() {
		return economy;
	}

	public Config getCommands() {
		return commands;
	}

	public Config getCooldowns() {
		return cooldowns;
	}

	public Config getPlaceholders() {
		return placeholders;
	}

	public Config getServerMotd() {
		return serverMotd;
	}

	public Config getJoin() {
		return join;
	}

	public Config getDeath() {
		return death;
	}

	public Config getScoreboard() {
		return scoreboard;
	}

	public Config getTab() {
		return tab;
	}

	public Config getBossBar() {
		return bossbar;
	}

	public Config getQuit() {
		return quit;
	}

	public Config getChat() {
		return chat;
	}

	public Config getConsoleFilter() {
		return consoleFilter;
	}

	public Config getAnimations() {
		return animations;
	}

	public void reloadAll() {
		getMain().reload();
		getTranslations().reload();
		getCommands().reload();
		getCooldowns().reload();
		getScoreboard().reload();
		getJoin().reload();
		getQuit().reload();
		getDeath().reload();
		getChat().reload();
		getTab().reload();
		getBossBar().reload();
		getKits().reload();
		getEconomy().reload();
		getPlaceholders().reload();
		getServerMotd().reload();
		getConsoleFilter().reload();
		getAnimations().reload();
		prefix = getMain().getString("prefix", "");
		teleportRequestTime = TimeUtils.timeFromString(getMain().getString("teleport-request-time"));
	}
}
