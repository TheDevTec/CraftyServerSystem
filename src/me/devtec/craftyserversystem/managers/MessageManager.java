package me.devtec.craftyserversystem.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.custom.CustomJsonWriter;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.NmsProvider.ChatType;

public class MessageManager {

	public void sendMessageTrans(String pathToTranslation, PlaceholdersExecutor ex, CommandSender... receivers) {
		sendMessageFromFile(Loader.getPlugin().getConfigManager().getTranslations(), pathToTranslation, ex, receivers);
	}

	public void sendMessageFromFile(Config transFile, String pathToTranslation, PlaceholdersExecutor ex, CommandSender... receivers) {
		if (!transFile.existsKey(pathToTranslation)) {
			Loader.getPlugin().getLogger().severe("Missing translation path '" + pathToTranslation + "', please report this bug to the DevTec team.");
			return;
		}

		Object chatBase;
		boolean collection = false;
		if (transFile.isJson(pathToTranslation))
			chatBase = BukkitLoader.getNmsProvider().chatBase(ex.apply(CustomJsonWriter.toJson(transFile.get(pathToTranslation)))); // Json
		else if (transFile.get(pathToTranslation) instanceof Collection) {
			List<Object> components = new ArrayList<>();
			for (Object value : transFile.getList(pathToTranslation))
				if (value instanceof Collection)
					components.add(BukkitLoader.getNmsProvider().chatBase(ex.apply(CustomJsonWriter.toJson(value)))); // Json
				else
					components.add(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(value.toString()))));
			chatBase = components;
			collection = true;
		} else
			chatBase = BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(ex.apply(transFile.getString(pathToTranslation)))); // String
		if (collection)
			for (Object component : (List<?>) chatBase) {
				Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, component);
				for (CommandSender player : receivers)
					if (player instanceof Player)
						BukkitLoader.getPacketHandler().send((Player) player, packet);
			}
		else {
			Object packet = BukkitLoader.getNmsProvider().packetChat(ChatType.SYSTEM, chatBase);
			for (CommandSender player : receivers)
				if (player instanceof Player)
					BukkitLoader.getPacketHandler().send((Player) player, packet);
		}
	}
}
