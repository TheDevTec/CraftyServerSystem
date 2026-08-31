package me.devtec.craftyserversystem.events.internal;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.Position;

public class DeathListener implements CssListener {
	boolean hideMessage;
	boolean keepInventory;
	boolean keepExp;
	SpawnPriority[] priorities;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getDeath();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {
		hideMessage = getConfig().getBoolean("death.hide-death-message");
		keepInventory = getConfig().getBoolean("death.keep-inventory");
		keepExp = getConfig().getBoolean("death.keep-exp");
		priorities=new SpawnPriority[getConfig().getStringList("respawn-priority").size()];
		int pos = 0;
		for(String line : getConfig().getStringList("respawn-priority"))
			try{
				priorities[pos++]=SpawnPriority.valueOf(line.toUpperCase());
			}catch(NoSuchFieldError | Exception e){
				Loader.getPlugin().getLogger().warning("SpawnPriority named "+line+" doesn't exist! Check your death.yml file.");
			}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		if (hideMessage)
			e.setDeathMessage(null);
		if (keepInventory || e.getEntity().hasPermission("css.death.keep-inventory")) {
			e.setKeepInventory(true);
			e.getDrops().clear();
		}
		if (keepExp || e.getEntity().hasPermission("css.death.keep-exp")) {
			e.setKeepLevel(true);
			e.setDroppedExp(0);
		}
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", e.getEntity().getName()).papi(e.getEntity().getUniqueId());
		// Send json message
		API.get().getMsgManager().sendMessageFromFile(getConfig(), "death.broadcast", placeholders, BukkitLoader.getOnlinePlayers());
		for (String cmd : placeholders.apply(getConfig().getStringList("death.commands")))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		if(priorities==null)return;
		Location found = null;
		for(SpawnPriority priority : priorities){
			if(priority!=null)
				switch(priority){
				case BED:
					found=e.getPlayer().getRespawnLocation();
					break;
				case HOME:
					Set<String> homes = HomeManager.get().getHomes(e.getPlayer().getName());
					if(homes.isEmpty())continue;
					if(homes.contains("home"))
						found=HomeManager.get().getHomePosition(e.getPlayer().getName(), "home").toLocation();
					else
						found=HomeManager.get().getHomePosition(e.getPlayer().getName(), homes.iterator().next()).toLocation();
					break;
				case SPAWN:
					Position spawn = API.get().getConfigManager().getSpawn();
					if(spawn!=null && spawn.getWorld()!=null)
						found=spawn.toLocation();
					break;
				case WORLD:
					found=e.getPlayer().getWorld().getSpawnLocation();
					break;
				}
			if(found!=null)break;
		}
		if(found!=null)
			e.setRespawnLocation(found);
	}

	public enum SpawnPriority {
		HOME,
		BED,
		SPAWN,
		WORLD
	}
}
