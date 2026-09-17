package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Banlist extends CssCommand {

	@Override
	public void register() {
		if(isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, (sender, structure, args) -> {
			String player = args[0];
			sendList(sender, player, API.get().getCommandsAPI().getBanAPI().getHistory(player, null, 0), 1);
		}, (sender, structure, args) -> {
			List<String> list = new ArrayList<>();
			for(Player player : BukkitLoader.getOnlinePlayers())
				list.add(player.getName());
			list.add("{offlinePlayer}");
			return list;
		}).selector(Selector.INTEGER, (sender, structure, args) -> {
			String player = args[0];
			sendList(sender, player, API.get().getCommandsAPI().getBanAPI().getHistory(player, null, 0), ParseUtils.getInt(args[1]));
		});
		// register
		List<String> cmds = getCommands();
		if(!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void sendList(CommandSender sender, String user, List<Entry> entries, int page) {
		if(entries.isEmpty()) {
			msg(sender, "empty", renderer().placeholder("user", user));
			return;
		}
		int totalPages = entries.size() / 10 + (entries.size() % 10 == 0 ? 0 : 1);
		if(page <= 0)
			page = 1;
		if(page > totalPages)
			page = totalPages;
		TextRenderer placeholders = renderer().placeholder("page", page).placeholder("totalPages", totalPages).placeholder("previousPage", Math.max(1, page - 1))
		        .placeholder("nextPage", Math.min(totalPages, page + 1)).placeholder("user", user);
		msg(sender, "header", placeholders);
		for(int i = page * 10 - 10; i < page * 10 && i < entries.size(); ++i) {
			Entry entry = entries.get(i);
			TextRenderer executor;
			if(entry.getDuration() == 0)
				executor = renderer().placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
				        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId() + "")
				        .placeholder("startDate", API.get().getCommandsAPI().getBanAPI().getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))));
			else
				executor = renderer().placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
				        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId() + "")
				        .placeholder("startDate", API.get().getCommandsAPI().getBanAPI().getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))))
				        .placeholder("expireAfter", TimeUtils.timeToString(entry.getStartDate() + entry.getDuration() - System.currentTimeMillis() / 1000))
				        .placeholder("expireDate", API.get().getCommandsAPI().getBanAPI().getTimeFormat().format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + entry.getDuration()))));
			String statusPath = entry.isCancelled()
			        ? "cancelled"
			        : entry.getDuration() == 0 ? "active" : entry.getStartDate() + entry.getDuration() - System.currentTimeMillis() / 1000 <= 0 ? "inactive" : "active";
			switch(entry.getType()) {
				case BAN :
					msg(sender, "entry." + statusPath + ".ban." + (entry.getDuration() == 0 ? "perm" : "temp"), executor.placeholder("position", i + 1));
					break;
				case MUTE :
					msg(sender, "entry." + statusPath + ".mute." + (entry.getDuration() == 0 ? "perm" : "temp"), executor.placeholder("position", i + 1));
					break;
				case KICK :
					msg(sender, "entry." + statusPath + ".kick", executor.placeholder("position", i + 1));
					break;
				case WARN :
					msg(sender, "entry." + statusPath + ".warn", executor.placeholder("position", i + 1));
					break;
			}
			msg(sender, "footer", placeholders);
		}
	}

}
