package me.devtec.craftyserversystem.commands.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.ParseUtils;

public class ChatIgnore extends CssCommand {

	private Listener listener;

	@Override
	public void register() {
		if (isRegistered())
			return;

		boolean onlyPings = me.devtec.craftyserversystem.api.API.get().getConfigManager().getMain().getBoolean("chatIgnore.only-pings-in-chat");
		if (!me.devtec.craftyserversystem.api.API.get().getConfigManager().getChat().getBoolean("enabled")) {
			listener = new Listener() {
				@EventHandler
				public void onChat(AsyncPlayerChatEvent e) {
					Iterator<Player> itr = e.getRecipients().iterator();
					if (itr.hasNext())
						for (Player player = itr.next(); itr.hasNext(); player = itr.next())
							if (API.getUser(player.getUniqueId()).getBoolean("css.chatignore") && (!onlyPings || onlyPings && notContainsName(e.getMessage(), player.getName())))
								itr.remove();
				}

				private boolean notContainsName(String message, String name) {
					return !message.contains(name);
				}
			};
			Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());
		}

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (sender instanceof Player) {
				Config user = API.getUser(((Player) sender).getUniqueId());
				if (user.getBoolean("css.chatignore")) {
					user.set("css.chatignore", false);
					msg(sender, "accepting.self");
					return;
				}
				user.set("css.chatignore", true);
				msg(sender, "ignoring.self");
			} else
				msgUsage(sender, "other");
		}).permission(getPerm("cmd"));
		cmd.argument("-s", (sender, structure, args) -> {
			Config user = API.getUser(((Player) sender).getUniqueId());
			if (user.getBoolean("css.chatignore")) {
				user.set("css.chatignore", false);
				return;
			}
			user.set("css.chatignore", true);
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("-s") : Collections.emptyList());
		cmd.selector(Selector.BOOLEAN, (sender, structure, args) -> {
			Config user = API.getUser(((Player) sender).getUniqueId());
			if (!ParseUtils.getBoolean(args[0])) {
				user.set("css.chatignore", false);
				msg(sender, "accepting.self");
				return;
			}
			user.set("css.chatignore", true);
			msg(sender, "ignoring.self");
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("true", "false") : Collections.emptyList()).argument("-s", (sender, structure, args) -> {
			Config user = API.getUser(((Player) sender).getUniqueId());
			if (!ParseUtils.getBoolean(args[0])) {
				user.set("css.chatignore", false);
				return;
			}
			user.set("css.chatignore", true);
		});
		// Other
		CommandStructure<CommandSender> other = cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			Config user = API.getUser(player.getUniqueId());
			PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("target", player.getName()).add("sender", sender.getName());
			if (user.getBoolean("css.chatignore")) {
				user.set("css.chatignore", false);
				msg(sender, "accepting.other.sender", ex);
				msg(player, "accepting.other.target", ex);
				return;
			}
			user.set("css.chatignore", true);
			msg(sender, "ignoring.other.sender", ex);
			msg(player, "ignoring.other.target", ex);
		}).permission(getPerm("other"));
		other.argument("-s", (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			Config user = API.getUser(player.getUniqueId());
			if (user.getBoolean("css.chatignore")) {
				user.set("css.chatignore", false);
				return;
			}
			user.set("css.chatignore", true);
		});
		other.selector(Selector.BOOLEAN, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			Config user = API.getUser(player.getUniqueId());
			PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("target", player.getName()).add("sender", sender.getName());
			if (!ParseUtils.getBoolean(args[1])) {
				user.set("css.chatignore", false);
				msg(sender, "accepting.other.sender", ex);
				msg(player, "accepting.other.target", ex);
				return;
			}
			user.set("css.chatignore", true);
			msg(sender, "ignoring.other.sender", ex);
			msg(player, "ignoring.other.target", ex);
		}).argument("-s", (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			Config user = API.getUser(player.getUniqueId());
			if (!ParseUtils.getBoolean(args[1])) {
				user.set("css.chatignore", false);
				return;
			}
			user.set("css.chatignore", true);
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
}
