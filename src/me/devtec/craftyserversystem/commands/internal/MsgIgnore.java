package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.msgsystem.MsgManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.theapi.bukkit.BukkitLoader;

public class MsgIgnore extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// offlinePlayer
		cmd.argument(null, 1, (sender, structure, args) -> {
			Query query = me.devtec.shared.API.offlineCache().lookupQuery(args[0]);
			if (query != null) {
				PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("target", query.getName());
				if (MsgManager.get().removeIgnore(sender.getName(), query.getName()))
					msg(sender, "accepting", ex);
				else {
					MsgManager.get().addIgnore(sender.getName(), query.getName());
					msg(sender, "ignoring", ex);
				}
			} else
				msg(sender, "not-exist", PlaceholdersExecutor.i().add("target", args[0]));
		}, (sender, structure, args) -> { // Tab completer
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size() + 1);
			players.add("{offlinePlayer}");
			for (Player player : onlinePlayers)
				if (!player.equals(sender))
					players.add(player.getName());
			players.addAll(MsgManager.get().getIgnoredPlayers(sender.getName()));
			return players;
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
