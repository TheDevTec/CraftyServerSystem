package me.devtec.craftyserversystem.events.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.gui.expansion.GuiCreator;
import me.devtec.theapi.bukkit.gui.expansion.guis.LoopGuiCreator;

@IgnoredClass
public class ProxyBridgeListener implements PluginMessageListener {

	public static final String CHANNEL = "craftyserversystem:bridge";

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if(!CHANNEL.equalsIgnoreCase(channel))return;
		ByteArrayDataInput input = ByteStreams.newDataInput(message);
		String action = input.readUTF();
		switch(action) {
		case "open_gui": {
			String guiWithJson = input.readUTF();
			String playerTarget = input.readUTF();
			int splitAt = guiWithJson.indexOf('{');
			if(splitAt!=-1) {
				String id = guiWithJson.substring(0, splitAt-1);
				GuiCreator gui = GuiCreator.guis.get(id);
				if(gui==null) {
					Loader.getPlugin().getLogger().warning("[GuiExpansion] Not found menu with id " + id + "!");
					return;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> placeholders = (Map<String, Object>) Json.reader().simpleRead(guiWithJson.substring(splitAt));

				if(gui instanceof LoopGuiCreator && placeholders.containsKey("page")) {
					LoopGuiCreator loop = (LoopGuiCreator) gui;
					Object page = placeholders.get("page");
					for(Player target : selector(Bukkit.getConsoleSender(), playerTarget)) {
						Config data = GuiCreator.sharedData.computeIfAbsent(target.getUniqueId(), t -> new Config());
						for(Entry<String, Object> value : placeholders.entrySet())
							data.set(value.getKey(), value.getValue());
						loop.open(target, page instanceof Number ? ((Number)page).intValue() : 1);
					}
				}else
					for(Player target : selector(Bukkit.getConsoleSender(), playerTarget)) {
						Config data = GuiCreator.sharedData.computeIfAbsent(target.getUniqueId(), t -> new Config());
						for(Entry<String, Object> value : placeholders.entrySet())
							data.set(value.getKey(), value.getValue());
						gui.open(target);
					}
			}else {
				GuiCreator gui = GuiCreator.guis.get(guiWithJson);
				if(gui==null) {
					Loader.getPlugin().getLogger().warning("[GuiExpansion] Not found menu with id " + guiWithJson + "!");
					return;
				}
				for(Player target : selector(Bukkit.getConsoleSender(), playerTarget))
					gui.open(target);
			}
			break;
		}
		default:
			Loader.getPlugin().getLogger().warning("Proxy tried to call unsupported action named '"+action+"', skipped.");
			break;
		}
	}

	public Collection<? extends Player> selector(CommandSender sender, String selector) {
		char lowerCase = selector.length() == 1 && selector.charAt(0) == '*' ? '*'
				: selector.length() == 2 && selector.charAt(0) == '@' ? Character.toLowerCase(selector.charAt(1)) : 0;
		if (lowerCase != 0)
			switch (lowerCase) {
			case 'a':
			case 'e':
			case '*':
				return BukkitLoader.getOnlinePlayers();
			case 'r':
				return Collections.singleton(StringUtils.randomFromCollection(BukkitLoader.getOnlinePlayers()));
			case 's':
			case 'p':
				Location pos = null;
				if (sender instanceof Player)
					pos = ((Player) sender).getLocation();
				else if (sender instanceof BlockCommandSender)
					pos = ((BlockCommandSender) sender).getBlock().getLocation();
				else
					pos = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
				double distance = -1;
				Player nearestPlayer = null;
				for (Player sameWorld : pos.getWorld().getPlayers()) {
					double distanceRange = sameWorld.getLocation().distance(pos);
					if (distance == -1 || distance < distanceRange) {
						distance = distanceRange;
						nearestPlayer = sameWorld;
					}
				}
				Collection<? extends Player> players = BukkitLoader.getOnlinePlayers();
				return players.isEmpty() ? Collections.emptyList()
						: Collections.singleton(nearestPlayer == null ? players.iterator().next() : nearestPlayer);
			}
		Player target = Bukkit.getPlayer(selector);
		return target == null ? Collections.emptyList() : Collections.singleton(target);
	}

}
