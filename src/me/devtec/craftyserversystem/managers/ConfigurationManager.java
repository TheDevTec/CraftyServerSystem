package me.devtec.craftyserversystem.managers;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.Loader;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.merge.MergeStandards;
import me.devtec.shared.utility.StreamUtils;
import me.devtec.theapi.bukkit.game.Position;

public class ConfigurationManager {
	private static final String FILES_PATH = "plugins/CraftyServerSystem/";

	private Config main;
	private Config translations;
	private Config commands;
	private Config cooldowns;
	private String prefix = "";
	private Position spawn;

	public ConfigurationManager(String main, String translations, String commands, String cooldowns) {
		this.main = new Config(FILES_PATH + main);
		this.translations = new Config(FILES_PATH + translations);
		this.commands = new Config(FILES_PATH + commands);
		this.cooldowns = new Config(FILES_PATH + cooldowns);
	}

	public ConfigurationManager initFromJar() {
		if (main.merge(new Config().reload(StreamUtils.fromStream(Loader.getPlugin().getClass().getClassLoader().getResourceAsStream("files/config.yml"))), MergeStandards.DEFAULT))
			main.save("yaml");
		if (translations.merge(new Config().reload(StreamUtils.fromStream(Loader.getPlugin().getClass().getClassLoader().getResourceAsStream("files/translations.yml"))), MergeStandards.DEFAULT))
			translations.save("yaml");
		if (commands.merge(new Config().reload(StreamUtils.fromStream(Loader.getPlugin().getClass().getClassLoader().getResourceAsStream("files/commands.yml"))), MergeStandards.DEFAULT))
			commands.save("yaml");
		if (cooldowns.merge(new Config().reload(StreamUtils.fromStream(Loader.getPlugin().getClass().getClassLoader().getResourceAsStream("files/cooldowns.yml"))), MergeStandards.DEFAULT))
			cooldowns.save("yaml");
		prefix = main.getString("prefix", "");

		Config data = new Config(FILES_PATH + "spawn.yml");
		spawn = data.getAs("spawn", Position.class, Position.fromLocation(Bukkit.getWorlds().get(0).getSpawnLocation()));
		return this;
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

	public Config getMain() {
		return main;
	}

	public Config getTranslations() {
		return translations;
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
	}

	public String getPrefix() {
		return prefix;
	}
}
