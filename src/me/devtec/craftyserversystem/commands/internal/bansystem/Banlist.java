package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Banlist extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;
		BanAPI.init();

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, (sender, structure, args) -> {
			String player = args[0];
			sendList(sender, player, BanAPI.getHistory(player, null, 0), 1);
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				list.add(player.getName());
			list.add("{offlinePlayer}");
			return list;
		}).selector(Selector.INTEGER, (sender, structure, args) -> {
			String player = args[0];
			sendList(sender, player, BanAPI.getHistory(player, null, 0), ParseUtils.getInt(args[1]));
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void sendList(CommandSender sender, String user, List<Entry> entries, int page) {
		if (entries.isEmpty()) {
			msg(sender, "empty", PlaceholdersExecutor.i().add("user", user));
			return;
		}
		int totalPages = entries.size() / 10 + (entries.size() % 10 == 0 ? 0 : 1);
		if (page <= 0)
			page = 1;
		if (page > totalPages)
			page = totalPages;
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("page", page).add("totalPages", totalPages).add("previousPage", Math.max(1, page - 1))
				.add("nextPage", Math.min(totalPages, page + 1)).add("user", user);
		msg(sender, "header", placeholders);
		for (int i = page * 10 - 10; i < page * 10 && i < entries.size(); ++i) {
			Entry entry = entries.get(i);
			PlaceholdersExecutor executor;
			if (entry.getDuration() == 0)
				executor = PlaceholdersExecutor.i().add("reason", entry.getReason()).add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
						.add("startDate", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))));
			else
				executor = PlaceholdersExecutor.i().add("reason", entry.getReason()).add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
						.add("startDate", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))))
						.add("expireAfter", TimeUtils.timeToString(System.currentTimeMillis() / 1000 - entry.getStartDate() + entry.getDuration()))
						.add("expireDate", BanAPI.getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + entry.getDuration()))));
			String statusPath = entry.isCancelled() ? "cancelled"
					: entry.getDuration() == 0 ? "active" : System.currentTimeMillis() / 1000 - entry.getStartDate() + entry.getDuration() <= 0 ? "inactive" : "active";
			switch (entry.getType()) {
			case BAN:
				msg(sender, "entry." + statusPath + ".ban." + (entry.getDuration() == 0 ? "perm" : "temp"), executor.add("position", i + 1));
				break;
			case MUTE:
				msg(sender, "entry." + statusPath + ".mute." + (entry.getDuration() == 0 ? "perm" : "temp"), executor.add("position", i + 1));
				break;
			case KICK:
				msg(sender, "entry." + statusPath + ".kick", executor.add("position", i + 1));
				break;
			case WARN:
				msg(sender, "entry." + statusPath + ".warn", executor.add("position", i + 1));
				break;
			}
			msg(sender, "footer", placeholders);
		}
	}

}
