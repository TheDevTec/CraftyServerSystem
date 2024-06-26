package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.cache.TempList;

public class Fly extends CssCommand {

	private Listener listener;
	private List<UUID> fallDamageCancel = null;

	@Override
	public void register() {
		if (isRegistered())
			return;

		if (API.get().getConfigManager().getMain().getBoolean("fly.anti-fall-damage-listener")) {
			fallDamageCancel = new ArrayList<>();
			listener = new Listener() {

				@EventHandler
				public void playerFall(EntityDamageEvent e) {
					if (e.getCause() == DamageCause.FALL && e.getEntityType() == EntityType.PLAYER)
						if (fallDamageCancel.remove(e.getEntity().getUniqueId()))
							e.setCancelled(true);
				}

				@EventHandler
				public void quit(PlayerQuitEvent e) {
					fallDamageCancel.remove(e.getPlayer().getUniqueId());
				}

				private List<UUID> tempList = new TempList<>(20 * 5);

				@EventHandler
				public void onPreWorldChange(PlayerTeleportEvent e) {
					if (!e.getFrom().getWorld().equals(e.getTo().getWorld()) && isAllowed(e.getPlayer()))
						tempList.add(e.getPlayer().getUniqueId());
				}

				@EventHandler
				public void onWorldChange(PlayerChangedWorldEvent e) {
					if (tempList.remove(e.getPlayer().getUniqueId()))
						setFly(e.getPlayer(), true, false, e.getPlayer());
				}
			};
		} else
			listener = new Listener() {
				private List<UUID> tempList = new TempList<>(20 * 5);

				@EventHandler
				public void onPreWorldChange(PlayerTeleportEvent e) {
					if (!e.getFrom().getWorld().equals(e.getTo().getWorld()) && isAllowed(e.getPlayer()))
						tempList.add(e.getPlayer().getUniqueId());
				}

				@EventHandler
				public void onWorldChange(PlayerChangedWorldEvent e) {
					if (tempList.remove(e.getPlayer().getUniqueId()))
						setFly(e.getPlayer(), true, false, e.getPlayer());
				}
			};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			setFly((Player) sender, !isAllowed((Player) sender), true, sender);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			setFly((Player) sender, !isAllowed((Player) sender), false, sender);
		});
		// other
		cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				setFly(player, !isAllowed(player), true, sender);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						setFly(player, !isAllowed(player), false, sender);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void setFly(Player target, boolean flyStatus, boolean sendMessage, CommandSender sender) {
		if (flyStatus) {
			target.setAllowFlight(true);
			if (target.getLocation().add(0, -0.5, 0).getBlock().isEmpty())
				target.setFlying(true);
			if (sendMessage)
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
					msg(target, "other.true.target", placeholders);
					msg(sender, "other.true.sender", placeholders);
				} else
					msg(target, "self.true", PlaceholdersExecutor.i().add("target", target.getName()));
		} else {
			if (fallDamageCancel != null && listener != null && target.getLocation().add(0, -0.5, 0).getBlock().isEmpty())
				fallDamageCancel.add(target.getUniqueId());
			target.setFlying(false);
			target.setAllowFlight(false);
			if (sendMessage)
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
					msg(target, "other.false.target", placeholders);
					msg(sender, "other.false.sender", placeholders);
				} else
					msg(target, "self.false", PlaceholdersExecutor.i().add("target", target.getName()));
		}
	}

	public boolean isAllowed(Player sender) {
		return sender.getAllowFlight();
	}

	@Override
	public void unregister() {
		super.unregister();
		if (listener != null) {
			HandlerList.unregisterAll(listener);
			fallDamageCancel.clear();
			fallDamageCancel = null;
			listener = null;
		}
	}

}
