package me.devtec.craftyserversystem.commands.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.events.VanishToggleEvent;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.database.DatabaseHandler.InsertQuery;
import me.devtec.shared.database.DatabaseHandler.RemoveQuery;
import me.devtec.shared.database.DatabaseHandler.SelectQuery;
import me.devtec.shared.events.EventManager;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Vanish extends CssCommand {

	private Listener listener;
	private boolean storeVanishInDb;

	@Override
	public void register() {
		if (isRegistered())
			return;

		storeVanishInDb = me.devtec.craftyserversystem.api.API.get().getConfigManager().getMain().getBoolean("vanish.store-in-sql")
				&& me.devtec.craftyserversystem.api.API.get().getSqlConnection() != null;

		listener = new Listener() {

			@EventHandler
			public void login(PlayerLoginEvent e) {
				if (hasVanishEnabled(e.getPlayer().getUniqueId())) {
					VanishToggleEvent event = new VanishToggleEvent(e.getPlayer().getUniqueId(), true);
					EventManager.call(event);
					if (!event.isCancelled()) { // Hide connected player before online players
						// Legacy support
						if (event.getStatus()) {
							if (!getVanish(e.getPlayer()))
								e.getPlayer().setMetadata("vanish", new FixedMetadataValue(Loader.getPlugin(), true));
						} else if (getVanish(e.getPlayer()))
							for (MetadataValue value : e.getPlayer().getMetadata("vanish"))
								e.getPlayer().removeMetadata("vanish", value.getOwningPlugin());
						for (Player player : BukkitLoader.getOnlinePlayers())
							if (!player.equals(e.getPlayer()) && !player.hasPermission(getPerm("see")))
								player.hidePlayer(e.getPlayer());
					} else if (storeVanishInDb)
						try {
							me.devtec.craftyserversystem.api.API.get().getSqlConnection().remove(RemoveQuery.table("css_vanish").where("id", e.getPlayer().getUniqueId().toString()));
						} catch (SQLException e1) {
							e1.printStackTrace();
						}
					else
						API.getUser(e.getPlayer().getUniqueId()).remove("css.vanish");
				}
				// Hide vanished players before this player
				for (Player player : BukkitLoader.getOnlinePlayers())
					if (!player.equals(e.getPlayer()) && getVanish(player) && !player.hasPermission(getPerm("see")))
						e.getPlayer().hidePlayer(player);
			}

			private boolean hasVanishEnabled(UUID uuid) {
				if (storeVanishInDb)
					try {
						return me.devtec.craftyserversystem.api.API.get().getSqlConnection().exists(SelectQuery.table("css_vanish").where("id", uuid.toString()));
					} catch (SQLException e) {
						e.printStackTrace();
					}
				return API.getUser(uuid).getBoolean("css.vanish");
			}

		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			setVanish(sender, (Player) sender, !getVanish((Player) sender), true);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			setVanish(sender, (Player) sender, !getVanish((Player) sender), false);
		});
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			setVanish(sender, player, !getVanish(player), true);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					Player player = Bukkit.getPlayer(args[0]);
					setVanish(sender, player, !getVanish(player), false);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	@Override
	public void unregister() {
		super.unregister();
		if (listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

	public static boolean getVanish(Player target) {
		if (target.hasMetadata("vanish"))
			return !target.getMetadata("vanish").isEmpty();
		return false;
	}

	public void setVanish(CommandSender sender, Player target, boolean status, boolean sendMessages) {
		VanishToggleEvent event = new VanishToggleEvent(target.getUniqueId(), status);
		EventManager.call(event);
		if (event.isCancelled())
			return;

		// Legacy support
		if (event.getStatus()) {
			if (!getVanish(target))
				target.setMetadata("vanish", new FixedMetadataValue(Loader.getPlugin(), true));
		} else if (getVanish(target))
			for (MetadataValue value : target.getMetadata("vanish"))
				target.removeMetadata("vanish", value.getOwningPlugin());

		// Store in the player's data / sql
		if (storeVanishInDb) {
			if (event.getStatus())
				try {
					me.devtec.craftyserversystem.api.API.get().getSqlConnection().insert(InsertQuery.table("css_vanish", target.getUniqueId().toString()));
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			else
				try {
					me.devtec.craftyserversystem.api.API.get().getSqlConnection().remove(RemoveQuery.table("css_vanish").where("id", target.getUniqueId().toString()));
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
		} else if (event.getStatus())
			API.getUser(target.getUniqueId()).set("css.vanish", true);
		else
			API.getUser(target.getUniqueId()).remove("css.vanish");

		if (event.getStatus()) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (!player.equals(target) && !player.hasPermission(getPerm("see")))
					player.hidePlayer(target);
		} else
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (!player.equals(target) && !player.canSee(target))
					player.showPlayer(target);

		if (sendMessages) {
			String statusPath = event.getStatus() ? "enabled" : "disabled";
			if (sender.equals(target))
				msg(sender, "self." + statusPath);
			else {
				PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msg(sender, "other." + statusPath + ".sender", ex);
				msg(target, "other." + statusPath + ".target", ex);
			}
		}
	}

}
