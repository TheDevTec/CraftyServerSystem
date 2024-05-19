package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class TempBan extends CssCommand {

	private Listener listener;

	@Override
	public void register() {
		if (isRegistered())
			return;
		BanAPI.init();
		ConsoleBanFilter.init();
		listener = new Listener() {

			@EventHandler
			public void asyncLogin(AsyncPlayerPreLoginEvent e) {
				if (e.getLoginResult() != Result.ALLOWED)
					return;
				for (Entry entry : BanAPI.getActivePunishments(e.getName(), e.getAddress().getHostAddress()))
					if (entry.getType() == BanType.BAN) {
						e.setLoginResult(Result.KICK_BANNED);
						String banMessage;
						if (entry.getDuration() == 0)
							banMessage = ColorUtils.colorize(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.banned"), "\n").replace("{reason}", entry.getReason())
									.replace("{admin}", entry.getAdmin() == null ? "Console" : entry.getAdmin()).replace("id", entry.getId() + "")
									.replace("{startDate}", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate())))));
						else
							banMessage = ColorUtils.colorize(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.temp-banned"), "\n")
									.replace("{reason}", entry.getReason()).replace("{admin}", entry.getAdmin() == null ? "Console" : entry.getAdmin()).replace("id", entry.getId() + "")
									.replace("{expireAfter}", TimeUtils.timeToString(System.currentTimeMillis() / 1000 - entry.getStartDate() + entry.getDuration()))
									.replace("{expireDate}", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + entry.getDuration()))))
									.replace("{startDate}", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate())))));
						String stripped = ColorUtils.strip(banMessage);
						ConsoleBanFilter.addMessage("UUID of player " + e.getName() + " is " + e.getUniqueId(), "");
						ConsoleBanFilter.addMessage("Disconnecting " + e.getName() + " (" + e.getAddress().toString(), stripped);
						ConsoleBanFilter.addMessage(e.getName() + " (" + e.getAddress().toString(), ") lost connection: " + stripped);
						e.setKickMessage(banMessage);
						return;
					}
			}
		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				list.add(player.getName());
			list.add("{offlinePlayer}");
			return list;
		}).argument(null, (sender, structure, args) -> {
			String player = args[0];
			String reason = null;
			BanAPI.tempBan(player, sender.getName(), TimeUtils.timeFromString(args[1]), reason);
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			if (args[1].isEmpty()) {
				list.add("1h");
				list.add("6h");
				list.add("7d");
				list.add("14d");
				list.add("1mon");
			} else if (Character.isDigit(args[1].charAt(args[1].length() - 1))) {
				list.add(args[1]);
				list.add(args[1] + "h");
				list.add(args[1] + "d");
				list.add(args[1] + "mon");
			} else
				list.add(args[1]);
			return list;
		}).argument(null, (sender, structure, args) -> {
			String player = args[0];
			String reason = StringUtils.buildString(2, args);
			BanAPI.tempBan(player, sender.getName(), TimeUtils.timeFromString(args[1]), reason);
		}, (sender, structure, args) -> Arrays.asList("{reason}"));
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
