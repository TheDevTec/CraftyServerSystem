package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Ref;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class God extends CssCommand {

	private Listener listener;

	@Override
	public void register() {
		if (isRegistered())
			return;

		if (Ref.isOlderThan(12))
			if (API.get().getConfigManager().getMain().getBoolean("god.anti-void-damage-listener"))
				listener = new Listener() {

					@EventHandler(ignoreCancelled = true)
					public void playerVoid(EntityDamageEvent e) {
						if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
							e.setCancelled(true);
					}

					@EventHandler(ignoreCancelled = true)
					public void playerFood(FoodLevelChangeEvent e) {
						if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
							e.setCancelled(true);
					}
				};
			else
				listener = new Listener() {

					@EventHandler(ignoreCancelled = true)
					public void playerVoid(EntityDamageEvent e) {
						if (e.getCause() != DamageCause.VOID && e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
							e.setCancelled(true);
					}

					@EventHandler(ignoreCancelled = true)
					public void playerFood(FoodLevelChangeEvent e) {
						if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
							e.setCancelled(true);
					}
				};
		else if (API.get().getConfigManager().getMain().getBoolean("god.anti-void-damage-listener"))
			listener = new Listener() {

				@EventHandler(ignoreCancelled = true)
				public void playerVoid(EntityDamageEvent e) {
					if (e.getCause() == DamageCause.VOID && e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
						e.setCancelled(true);
				}

				@EventHandler(ignoreCancelled = true)
				public void playerFood(FoodLevelChangeEvent e) {
					if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
						e.setCancelled(true);
				}

				@EventHandler(ignoreCancelled = true)
				public void playerAir(EntityAirChangeEvent e) {
					if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()) && ((Player) e.getEntity()).getRemainingAir() > e.getAmount())
						e.setCancelled(true);
				}
			};
		else
			listener = new Listener() {

				@EventHandler(ignoreCancelled = true)
				public void playerFood(FoodLevelChangeEvent e) {
					if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()))
						e.setCancelled(true);
				}

				@EventHandler(ignoreCancelled = true)
				public void playerAir(EntityAirChangeEvent e) {
					if (e.getEntityType() == EntityType.PLAYER && isAllowed((Player) e.getEntity()) && ((Player) e.getEntity()).getRemainingAir() > e.getAmount())
						e.setCancelled(true);
				}
			};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			setGod((Player) sender, !isAllowed((Player) sender), true, sender);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			setGod((Player) sender, !isAllowed((Player) sender), false, sender);
		});
		// other
		cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				setGod(player, !isAllowed(player), true, sender);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						setGod(player, !isAllowed(player), false, sender);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void setGod(Player target, boolean godStatus, boolean sendMessage, CommandSender sender) {
		if (godStatus) {
			if (Ref.isOlderThan(12))
				me.devtec.shared.API.getUser(target.getUniqueId()).set("css.god", true);
			else
				target.setInvulnerable(true);
			if (sendMessage)
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
					msg(target, "other.true.target", placeholders);
					msg(sender, "other.true.sender", placeholders);
				} else
					msg(target, "self.true", PlaceholdersExecutor.i().add("target", target.getName()));
		} else {
			if (Ref.isOlderThan(12))
				me.devtec.shared.API.getUser(target.getUniqueId()).set("css.god", false);
			else
				target.setInvulnerable(false);
			if (sendMessage)
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
					msg(target, "other.false.target", placeholders);
					msg(sender, "other.false.sender", placeholders);
				} else
					msg(target, "self.false", PlaceholdersExecutor.i().add("target", target.getName()));
		}
	}

	public boolean isAllowed(Player target) {
		if (Ref.isOlderThan(12))
			return me.devtec.shared.API.getUser(target.getUniqueId()).getBoolean("css.god");
		return target.isInvulnerable();
	}

	@Override
	public void unregister() {
		super.unregister();
		if (listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

}
