package me.devtec.craftyserversystem.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.json.custom.CustomJsonWriter;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.NmsProvider.ChatType;

public class MessageManager {

	private final Logger logger = JavaPlugin.getPlugin(Loader.class).getLogger();

	public class Action {
		Config config;
		String path;
		PlaceholdersExecutor executor;
		CommandSender[] receivers;
		CompletableFuture<String> result;

		public Action(Config config, String path, PlaceholdersExecutor executor, CommandSender[] receivers) {
			this.config = config;
			this.path = path;
			this.executor = executor;
			this.receivers = receivers;
		}

		public Action(Config config, String path, PlaceholdersExecutor executor, CommandSender[] receivers, CompletableFuture<String> result) {
			this.config = config;
			this.path = path;
			this.executor = executor;
			this.receivers = receivers;
			this.result = result;
		}

		public void process() {
			if (result != null) {
				if (!config.existsKey(path)) {
					JavaPlugin.getPlugin(Loader.class).getLogger().severe("Missing translation path '" + path + "', please report this bug to the DevTec team.");
					result.complete(null);
					return;
				}

				Object chatBase;
				boolean collection = false;
				if (config.isJson(path) && (config.get(path) instanceof Collection || config.get(path) instanceof Map)) {
					if (config.get(path) instanceof Collection && ((Collection<?>) config.get(path)).isEmpty() || config.get(path) instanceof Map && ((Map<?, ?>) config.get(path)).isEmpty()) {
						result.complete(null);
						return;
					}
					chatBase = BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(executor.apply(config.get(path)))); // Json
				} else if (config.get(path) instanceof Collection) {
					List<Object> components = new ArrayList<>();
					for (Object value : config.getList(path))
						if (value instanceof Collection)
							components.add(BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(executor.apply(value)))); // Json
						else
							components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(executor.applyAfterColorize(value.toString()))));
					chatBase = components;
					if (components.isEmpty()) {
						result.complete(null);
						return;
					}
					collection = true;
				} else {
					String value = config.getString(path);
					if (value == null || value.isEmpty()) {
						result.complete(null);
						return;
					}
					chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(executor.applyAfterColorize(value))); // String
				}
				if (!collection) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, chatBase);
					String inString = BukkitLoader.getNmsProvider().fromIChatBaseComponent(chatBase).toString();
					for (CommandSender player : receivers)
						if (player instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) player, packet);
						else
							player.sendMessage(inString);
					result.complete(inString);
					return;
				}
				StringContainer container = new StringContainer(64);
				for (Object component : (List<?>) chatBase) {
					Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);
					String inString = BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString();
					for (CommandSender player : receivers)
						if (player instanceof Player)
							BukkitLoader.getPacketHandler().send((Player) player, packet);
						else
							player.sendMessage(inString);
					container.append(inString);
				}
				result.complete(container.toString());
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

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, Collection<? extends CommandSender> receivers) {
		actions.add(new Action(transFile, pathToTranslation, ex, receivers.toArray(new CommandSender[0])));
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, CommandSender... receivers) {
		actions.add(new Action(transFile, pathToTranslation, ex, receivers));
	}

	public String sendMessageFromFileWithResult(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, Collection<? extends CommandSender> receivers) {
		CompletableFuture<String> future = new CompletableFuture<>();
		actions.add(0, new Action(transFile, pathToTranslation, ex, receivers.toArray(new CommandSender[0]), future)); // Inserts on first priority
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