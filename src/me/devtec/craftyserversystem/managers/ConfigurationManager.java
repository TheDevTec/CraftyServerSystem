package me.devtec.craftyserversystem.managers;

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
	private Config chat;
	private Config kits;
	private Config economy;
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
		chat = new Config(FILES_PATH + "events/chat.yml");
		kits = new Config(FILES_PATH + "kits.yml");
		economy = new Config(FILES_PATH + "economy.yml");
	}

	public ConfigurationManager initFromJar() {
		merge(main, "config.yml");
		merge(translations, "translations.yml");
		merge(commands, "commands.yml");
		merge(cooldowns, "cooldowns.yml");
		merge(join, "join.yml");
		merge(quit, "quit.yml");
		merge(chat, "chat.yml");
		merge(kits, "kits.yml");
		merge(economy, "economy.yml");
		prefix = getMain().getString("prefix", "");
		teleportRequestTime = TimeUtils.timeFromString(getMain().getString("teleport-request-time"));
		return this;
	}

	private void merge(Config origin, String path) {
		ClassLoader classLoader = Loader.getPlugin().getClass().getClassLoader();
		if (origin.merge(new Config().reload(StreamUtils.fromStream(classLoader.getResourceAsStream("files/" + path))), MergeStandards.DEFAULT))
			origin.save("yaml");
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
		spawn = data.getAs("spawn", Position.class, Position.fromLocation(Bukkit.getWorlds().get(0).getSpawnLocation()));
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

	public void reloadAll() {
		getMain().reload();
		getTranslations().reload();
		getCommands().reload();
		getKits().reload();
		getCooldowns().reload();
		prefix = getMain().getString("prefix", "");
		teleportRequestTime = TimeUtils.timeFromString(getMain().getString("teleport-request-time"));
	}

	public String getPrefix() {
		return prefix;
	}

	public Config getJoin() {
		return join;
	}

	public Config getQuit() {
		return quit;
	}

	public Config getChat() {
		return chat;
	}
}
