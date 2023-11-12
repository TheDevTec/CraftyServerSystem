package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

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
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Vanish extends CssCommand {

	private Listener listener;

	@Override
	public String section() {
		return "vanish";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		listener = new Listener() {

			@EventHandler
			public void login(PlayerLoginEvent e) {
				if (API.getUser(e.getPlayer().getUniqueId()).getBoolean("css.vanish"))
					setVanish(e.getPlayer(), e.getPlayer(), true, false); // Hide connected player before online players
				// Hide vanished players before this player

				for (Player player : BukkitLoader.getOnlinePlayers())
					if (!player.equals(e.getPlayer()) && getVanish(player) && !player.hasPermission(getPerm("see")))
						e.getPlayer().hidePlayer(Loader.getPlugin(), player);
			}

		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			setVanish(sender, (Player) sender, getVanish((Player) sender), true);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			setVanish(sender, (Player) sender, getVanish((Player) sender), false);
		});
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			setVanish(sender, player, getVanish(player), true);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					Player player = Bukkit.getPlayer(args[0]);
					setVanish(sender, player, getVanish(player), false);
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

	private boolean getVanish(Player target) {
		if (target.hasMetadata("css-vanish"))
			return !target.getMetadata("css-vanish").isEmpty();
		return false;
	}

	private void setVanish(CommandSender sender, Player target, boolean status, boolean sendMessages) {
		if (status) { // enable
			if (!getVanish(target))
				target.setMetadata("css-vanish", new FixedMetadataValue(Loader.getPlugin(), true));
		} else if (getVanish(target))
			for (MetadataValue value : target.getMetadata("css-vanish"))
				target.removeMetadata("css-vanish", value.getOwningPlugin());
		if (status)
			API.getUser(target.getUniqueId()).set("css.vanish", true);
		else
			API.getUser(target.getUniqueId()).remove("css.vanish");
		// TODO bungeecord bridge?

		for (Player player : BukkitLoader.getOnlinePlayers())
			if (!player.equals(target) && !player.hasPermission(getPerm("see")))
				player.hidePlayer(Loader.getPlugin(), target);

		if (sendMessages) {
			String statusPath = status ? "enabled" : "disabled";
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
