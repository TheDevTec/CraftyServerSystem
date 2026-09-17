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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.events.VanishToggleEvent;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.API;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.database.Sql;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.events.EventManager;
import me.devtec.shared.text.TextRenderer;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Vanish extends CssCommand {

	private Listener listener;
	private boolean storeVanishInDb;
	private boolean fakeJoin;
	private boolean fakeLeave;
	private boolean isSpigotPurgingFiles;

	@SuppressWarnings("resource")
	@Override
	public void register() {
		if(isRegistered())
			return;

		storeVanishInDb = me.devtec.craftyserversystem.api.API.get().getConfigManager().getMain().getBoolean("vanish.store-in-sql")
		        && me.devtec.craftyserversystem.api.API.get().getSqlConnection() != null;

		fakeJoin = me.devtec.craftyserversystem.api.API.get().getConfigManager().getMain().getBoolean("vanish.broadcast-join-and-leave")
		        && me.devtec.craftyserversystem.api.API.get().getConfigManager().getJoin().getBoolean("enabled");

		fakeLeave = me.devtec.craftyserversystem.api.API.get().getConfigManager().getMain().getBoolean("vanish.broadcast-join-and-leave")
		        && me.devtec.craftyserversystem.api.API.get().getConfigManager().getQuit().getBoolean("enabled");

		if(fakeJoin || fakeLeave)
			isSpigotPurgingFiles = new Config("spigot.yml").getBoolean("players.disable-saving");

		listener = new Listener() {

			@EventHandler
			public void login(PlayerJoinEvent event) {
				Player player = event.getPlayer();

				if(hasVanishEnabled(player.getUniqueId())) {
					VanishToggleEvent vanishEvent = new VanishToggleEvent(player.getUniqueId(), true);
					EventManager.call(vanishEvent);

					if(!vanishEvent.isCancelled()) {
						if(vanishEvent.getStatus()) {
							if(!getVanish(player))
								player.setMetadata("vanish", new FixedMetadataValue(Loader.getPlugin(), true));
						} else if(getVanish(player))
							for(MetadataValue value : player.getMetadata("vanish"))
								player.removeMetadata("vanish", value.getOwningPlugin());

						for(Player online : BukkitLoader.getOnlinePlayers())
							if(!online.equals(player) && !online.hasPermission(getPerm("see")))
								online.hidePlayer(player);
					} else if(storeVanishInDb)
						try {
							me.devtec.craftyserversystem.api.API.get().getSqlConnection().update(Sql.deleteFrom("css_vanish").where("id", player.getUniqueId().toString()));
						} catch(SQLException exception) {
							exception.printStackTrace();
						}
					else
						API.getUser(player.getUniqueId()).set("css.vanish", null);
				}

				for(Player online : BukkitLoader.getOnlinePlayers())
					if(!online.equals(player) && getVanish(online) && !player.hasPermission(getPerm("see")))
						player.hidePlayer(online);
			}

			@EventHandler
			public void quit(PlayerQuitEvent event) {
				for(MetadataValue value : event.getPlayer().getMetadata("vanish"))
					event.getPlayer().removeMetadata("vanish", value.getOwningPlugin());
			}

			private boolean hasVanishEnabled(UUID uuid) {
				if(storeVanishInDb)
					try {
						return me.devtec.craftyserversystem.api.API.get().getSqlConnection().exists(Sql.select("id").from("css_vanish").where("id", uuid.toString()).limit(1));
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

				return API.getUser(uuid).getBoolean("css.vanish");
			}
		};

		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if(!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}

			Player player = (Player) sender;
			setVanish(sender, player, !getVanish(player), true);
		}).permission(getPerm("cmd"));

		cmd.argument("-s", (sender, structure, args) -> {
			if(!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}

			Player player = (Player) sender;
			setVanish(sender, player, !getVanish(player), false);
		});

		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);

			if(player != null)
				setVanish(sender, player, !getVanish(player), true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);

			if(player != null)
				setVanish(sender, player, !getVanish(player), false);
		});

		List<String> commands = getCommands();

		if(!commands.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(commands.remove(0), commands.toArray(new String[0]));
	}

	@Override
	public void unregister() {
		super.unregister();

		if(listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

	public static boolean getVanish(Player target) {
		return target.hasMetadata("vanish") && !target.getMetadata("vanish").isEmpty();
	}

	public void setVanish(CommandSender sender, Player target, boolean status, boolean sendMessages) {
		VanishToggleEvent event = new VanishToggleEvent(target.getUniqueId(), status);
		EventManager.call(event);

		if(event.isCancelled())
			return;

		if(event.getStatus()) {
			if(!getVanish(target))
				target.setMetadata("vanish", new FixedMetadataValue(Loader.getPlugin(), true));
		} else if(getVanish(target))
			for(MetadataValue value : target.getMetadata("vanish"))
				target.removeMetadata("vanish", value.getOwningPlugin());

		if(storeVanishInDb) {
			if(event.getStatus())
				try {
					me.devtec.craftyserversystem.api.API.get().getSqlConnection().update(Sql.insertInto("css_vanish", "id").values(target.getUniqueId().toString()));
				} catch(SQLException exception) {
					exception.printStackTrace();
				}
			else
				try {
					me.devtec.craftyserversystem.api.API.get().getSqlConnection().update(Sql.deleteFrom("css_vanish").where("id", target.getUniqueId().toString()));
				} catch(SQLException exception) {
					exception.printStackTrace();
				}
		} else if(event.getStatus())
			API.getUser(target.getUniqueId()).set("css.vanish", true);
		else
			API.getUser(target.getUniqueId()).set("css.vanish", null);

		if(event.getStatus()) {
			for(Player player : BukkitLoader.getOnlinePlayers())
				if(!player.equals(target) && !player.hasPermission(getPerm("see"))) {
					player.hidePlayer(target);

					if(fakeLeave) {
						TextRenderer renderer = renderer().target(target.getUniqueId()).placeholder("player", target.getName());

						Config config = me.devtec.craftyserversystem.api.API.get().getConfigManager().getQuit();

						me.devtec.craftyserversystem.api.API.get().getMsgManager().sendMessageFromFile(config, "quit.text", renderer, player);
					}
				}
		} else
			for(Player player : BukkitLoader.getOnlinePlayers())
				if(!player.equals(target) && !player.canSee(target)) {
					player.showPlayer(target);

					if(fakeJoin) {
						TextRenderer renderer = renderer().target(target.getUniqueId()).placeholder("player", target.getName());

						String time = isSpigotPurgingFiles ? "first" : "normal";

						Config config = me.devtec.craftyserversystem.api.API.get().getConfigManager().getJoin();

						me.devtec.craftyserversystem.api.API.get().getMsgManager().sendMessageFromFile(config, "join." + time + ".text", renderer, player);
					}
				}

		if(sendMessages) {
			String statusPath = event.getStatus() ? "enabled" : "disabled";

			if(sender.equals(target))
				msg(sender, "self." + statusPath);
			else {
				TextRenderer senderRenderer = renderer(sender).placeholder("sender", sender.getName()).placeholder("target", target.getName());

				TextRenderer targetRenderer = renderer(target).placeholder("sender", sender.getName()).placeholder("target", target.getName());

				msg(sender, "other." + statusPath + ".sender", senderRenderer);
				msg(target, "other." + statusPath + ".target", targetRenderer);
			}
		}
	}
}
