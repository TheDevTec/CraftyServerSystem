package me.devtec.craftyserversystem.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.NmsProvider.ChatType;

public class MessageManager {

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, Collection<? extends CommandSender> receivers) {
		if (!transFile.existsKey(pathToTranslation)) {
			JavaPlugin.getPlugin(Loader.class).getLogger().severe("Missing translation path '" + pathToTranslation + "', please report this bug to the DevTec team.");
			return;
		}

		Object chatBase;
		boolean collection = false;
		if (transFile.isJson(pathToTranslation) && (transFile.get(pathToTranslation) instanceof Collection || transFile.get(pathToTranslation) instanceof Map)) {
			if (transFile.get(pathToTranslation) instanceof Collection && ((Collection<?>) transFile.get(pathToTranslation)).isEmpty()
					|| transFile.get(pathToTranslation) instanceof Map && ((Map<?, ?>) transFile.get(pathToTranslation)).isEmpty())
				return;
			chatBase = BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(transFile.get(pathToTranslation)))); // Json
		} else if (transFile.get(pathToTranslation) instanceof Collection) {
			List<Object> components = new ArrayList<>();
			for (Object value : transFile.getList(pathToTranslation))
				if (value instanceof Collection)
					components.add(BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(value)))); // Json
				else
					components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(value.toString()))));
			chatBase = components;
			if (components.isEmpty())
				return;
			collection = true;
		} else {
			String result = transFile.getString(pathToTranslation);
			if (result == null || result.isEmpty())
				return;
			chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(result))); // String
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

	public String sendMessageFromFileWithResult(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, Collection<? extends CommandSender> receivers) {
		if (!transFile.existsKey(pathToTranslation)) {
			JavaPlugin.getPlugin(Loader.class).getLogger().severe("Missing translation path '" + pathToTranslation + "', please report this bug to the DevTec team.");
			return null;
		}

		Object chatBase;
		boolean collection = false;
		if (transFile.isJson(pathToTranslation) && (transFile.get(pathToTranslation) instanceof Collection || transFile.get(pathToTranslation) instanceof Map)) {
			if (transFile.get(pathToTranslation) instanceof Collection && ((Collection<?>) transFile.get(pathToTranslation)).isEmpty()
					|| transFile.get(pathToTranslation) instanceof Map && ((Map<?, ?>) transFile.get(pathToTranslation)).isEmpty())
				return null;
			chatBase = BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(transFile.get(pathToTranslation)))); // Json
		} else if (transFile.get(pathToTranslation) instanceof Collection) {
			List<Object> components = new ArrayList<>();
			for (Object value : transFile.getList(pathToTranslation))
				if (value instanceof Collection)
					components.add(BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(value)))); // Json
				else
					components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.applyAfterColorize(value.toString()))));
			chatBase = components;
			if (components.isEmpty())
				return null;
			collection = true;
		} else {
			String result = transFile.getString(pathToTranslation);
			if (result == null || result.isEmpty())
				return null;
			chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.applyAfterColorize(result))); // String
		}
		if (!collection) {
			Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, chatBase);
			String inString = BukkitLoader.getNmsProvider().fromIChatBaseComponent(chatBase).toString();
			for (CommandSender player : receivers)
				if (player instanceof Player)
					BukkitLoader.getPacketHandler().send((Player) player, packet);
				else
					player.sendMessage(inString);
			return inString;
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
		return container.toString();
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, String permission) {
		if (!transFile.existsKey(pathToTranslation)) {
			JavaPlugin.getPlugin(Loader.class).getLogger().severe("Missing translation path '" + pathToTranslation + "', please report this bug to the DevTec team.");
			return;
		}

		Object chatBase;
		boolean collection = false;
		if (transFile.isJson(pathToTranslation) && (transFile.get(pathToTranslation) instanceof Collection || transFile.get(pathToTranslation) instanceof Map)) {
			if (transFile.get(pathToTranslation) instanceof Collection && ((Collection<?>) transFile.get(pathToTranslation)).isEmpty()
					|| transFile.get(pathToTranslation) instanceof Map && ((Map<?, ?>) transFile.get(pathToTranslation)).isEmpty())
				return;
			chatBase = BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(transFile.get(pathToTranslation)))); // Json
		} else if (transFile.get(pathToTranslation) instanceof Collection) {
			List<Object> components = new ArrayList<>();
			for (Object value : transFile.getList(pathToTranslation))
				if (value instanceof Collection)
					components.add(BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(value)))); // Json
				else
					components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(value.toString()))));
			chatBase = components;
			if (components.isEmpty())
				return;
			collection = true;
		} else {
			String result = transFile.getString(pathToTranslation);
			if (result == null || result.isEmpty())
				return;
			chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(result))); // String
		}
		if (collection)
			for (Object component : (List<?>) chatBase) {
				Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);
				for (Player player : BukkitLoader.getOnlinePlayers())
					if (player.hasPermission(permission))
						BukkitLoader.getPacketHandler().send(player, packet);
				Bukkit.getConsoleSender().sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(component).toString());
			}
		else {
			Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, chatBase);
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.hasPermission(permission))
					BukkitLoader.getPacketHandler().send(player, packet);
			Bukkit.getConsoleSender().sendMessage(BukkitLoader.getNmsProvider().fromIChatBaseComponent(chatBase).toString());
		}
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, CommandSender... receivers) {
		if (!transFile.existsKey(pathToTranslation)) {
			JavaPlugin.getPlugin(Loader.class).getLogger().severe("Missing translation path '" + pathToTranslation + "', please report this bug to the DevTec team.");
			return;
		}

		Object chatBase;
		boolean collection = false;
		if (transFile.isJson(pathToTranslation) && (transFile.get(pathToTranslation) instanceof Collection || transFile.get(pathToTranslation) instanceof Map)) {
			if (transFile.get(pathToTranslation) instanceof Collection && ((Collection<?>) transFile.get(pathToTranslation)).isEmpty()
					|| transFile.get(pathToTranslation) instanceof Map && ((Map<?, ?>) transFile.get(pathToTranslation)).isEmpty())
				return;
			chatBase = BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(transFile.get(pathToTranslation)))); // Json
		} else if (transFile.get(pathToTranslation) instanceof Collection) {
			List<Object> components = new ArrayList<>();
			for (Object value : transFile.getList(pathToTranslation))
				if (value instanceof Collection)
					components.add(BukkitLoader.getNmsProvider().chatBase(CustomJsonWriter.toJson(ex.apply(value)))); // Json
				else
					components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(value.toString()))));
			chatBase = components;
			if (components.isEmpty())
				return;
			collection = true;
		} else {
			String result = transFile.getString(pathToTranslation);
			if (result == null || result.isEmpty())
				return;
			chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(result))); // String
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