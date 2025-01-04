package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.msgsystem.MsgManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class MsgSpy extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (sender instanceof Player) {
				boolean status = !MsgManager.get().getSpy(sender.getName());
				MsgManager.get().setSpy(sender.getName(), status);
				if (status)
					msg(sender, "self.enabled");
				else
					msg(sender, "self.disabled");
			} else
				msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// boolean
		cmd.selector(Selector.BOOLEAN, (sender, structure, args) -> {
			if (sender instanceof Player) {
				boolean status = ParseUtils.getBoolean(args[0]);
				boolean cStatus = !MsgManager.get().getSpy(sender.getName());
				if (status != cStatus)
					MsgManager.get().setSpy(sender.getName(), status);
				if (status)
					msg(sender, "self.enabled");
				else
					msg(sender, "self.disabled");
			} else
				msgUsage(sender, "cmd");
		});
		// offlinePlayer
		cmd.argument(null, 1, (sender, structure, args) -> { // Player argument (variable - required for console sender)
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[0]);
			if (query != null) {
				boolean status = !MsgManager.get().getSpy(query.getName());
				MsgManager.get().setSpy(query.getName(), status);
				PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", query.getName());
				if (status) {
					msg(sender, "other.enabled.sender", ex);
					Player target = Bukkit.getPlayer(query.getUUID());
					if (target != null)
						msg(target, "other.enabled.target", ex);
				} else {
					msg(sender, "other.disabled.sender", ex);
					Player target = Bukkit.getPlayer(query.getUUID());
					if (target != null)
						msg(target, "other.disabled.target", ex);
				}
			} else
				msg(sender, "not-exist", PlaceholdersExecutor.i().add("target", args[0]));
		}, (sender, structure, args) -> { // Tab completer
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size() + 1);
			players.add("{offlinePlayer}");
			for (Player player : onlinePlayers)
				players.add(player.getName());
			return players;
		}).permission(getPerm("other")).selector(Selector.BOOLEAN, (sender, structure, args) -> {
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[0]);
			if (query != null) {
				PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", query.getName());
				boolean status = ParseUtils.getBoolean(args[1]);
				boolean cStatus = !MsgManager.get().getSpy(query.getName());
				if (status == cStatus) {
					if (status)
						msg(sender, "other.enabled.sender", ex);
					else
						msg(sender, "other.disabled.sender", ex);
					return;
				}
				MsgManager.get().setSpy(query.getName(), status);
				if (status) {
					msg(sender, "other.enabled.sender", ex);
					Player target = Bukkit.getPlayer(query.getUUID());
					if (target != null)
						msg(target, "other.enabled.target", ex);
				} else {
					msg(sender, "other.disabled.sender", ex);
					Player target = Bukkit.getPlayer(query.getUUID());
					if (target != null)
						msg(target, "other.disabled.target", ex);
				}
			} else
				msg(sender, "not-exist", PlaceholdersExecutor.i().add("target", args[0]));
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
