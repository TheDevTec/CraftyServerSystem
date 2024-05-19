package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class TempMute extends CssCommand {

	private Listener listener;

	@Override
	public void register() {
		if (isRegistered())
			return;
		BanAPI.init();

		CooldownHolder cd;
		if (API.get().getCooldownManager().getCooldown("banmanager.mute") == null)
			API.get().getCooldownManager().register(cd = new CooldownHolder("banmanager.mute") {

				Map<UUID, Long> cd = new HashMap<>();

				@Override
				public boolean accept(CommandSender sender) {
					long currentTime = System.currentTimeMillis() / 1000;
					long lastUsedTime = cd.getOrDefault(((Player) sender).getUniqueId(), 0L);
					long nextUsageIn = lastUsedTime - currentTime;
					if (nextUsageIn <= 0) {
						cd.put(((Player) sender).getUniqueId(), currentTime + 5);
						return true;
					}
					return false;
				}

				@Override
				public boolean tryWithoutWriting(CommandSender sender) {
					return remainingTime(sender) <= 0;
				}

				@Override
				public long remainingTime(CommandSender sender) {
					long currentTime = System.currentTimeMillis() / 1000;
					long lastUsedTime = cd.getOrDefault(((Player) sender).getUniqueId(), 0L);
					long nextUsageIn = lastUsedTime - currentTime;
					return Math.max(0, nextUsageIn);
				}
			});
		else
			cd = API.get().getCooldownManager().getCooldown("banmanager.mute");
		listener = new Listener() {

			@EventHandler(ignoreCancelled = true)
			public void asyncChat(AsyncPlayerChatEvent e) {
				for (Entry entry : BanAPI.getActivePunishments(e.getPlayer().getName(), e.getPlayer().getAddress().getAddress().getHostAddress()))
					if (entry.getType() == BanType.MUTE) {
						e.setCancelled(true);
						if (cd.accept(e.getPlayer())) {
							PlaceholdersExecutor executor;
							if (entry.getDuration() == 0)
								executor = PlaceholdersExecutor.i().add("reason", entry.getReason()).add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
										.add("startDate", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochMilli(entry.getStartDate()))));
							else
								executor = PlaceholdersExecutor.i().add("reason", entry.getReason()).add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
										.add("startDate", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochMilli(entry.getStartDate()))))
										.add("expireAfter", TimeUtils.timeToString(System.currentTimeMillis() / 1000 - entry.getStartDate() + entry.getDuration()))
										.add("expireDate", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + entry.getDuration()))));

							API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), entry.getDuration() == 0 ? "bansystem.muted" : "bansystem.temp-muted", executor,
									e.getPlayer());
						}
						return;
					}
			}
		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).argument(null, (sender, structure, args) -> {
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
			BanAPI.tempMute(player, sender.getName(), TimeUtils.timeFromString(args[1]), reason);
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
			BanAPI.tempMute(player, sender.getName(), TimeUtils.timeFromString(args[1]), reason);
		}, (sender, structure, args) -> Arrays.asList("{reason}")).permission(getPerm("cmd"));
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
