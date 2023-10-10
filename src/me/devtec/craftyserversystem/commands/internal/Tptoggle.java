package me.devtec.craftyserversystem.commands.internal;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.tprequest.TpaManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.OfflineCache.Query;

public class Tptoggle extends CssCommand {

	@Override
	public String section() {
		return "tptoggle";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			boolean globalToggle = TpaManager.getProvider().hasGlobalToggle(sender.getUniqueId());
			if (globalToggle) {
				TpaManager.getProvider().setGlobalToggle(sender.getUniqueId(), false);
				msg(sender, "global.disabled");
			} else {
				TpaManager.getProvider().setGlobalToggle(sender.getUniqueId(), true);
				msg(sender, "global.enabled");
			}
		}).permission(getPerm("cmd")).argument(null, 1, (sender, structure, args) -> {
			Query query = API.offlineCache().lookupQuery(args[0]);
			if (query == null) {
				msg(sender, "never-joined", PlaceholdersExecutor.i().add("target", args[0]));
				return;
			}
			List<UUID> toggled = TpaManager.getProvider().getToggledPlayers(sender.getUniqueId());
			if (toggled.contains(query.getUUID())) {
				TpaManager.getProvider().removeFromToggledPlayers(sender.getUniqueId(), query.getUUID());
				msg(sender, "user.removed", PlaceholdersExecutor.i().add("target", query.getName()));
			} else {
				TpaManager.getProvider().addToToggledPlayers(sender.getUniqueId(), query.getUUID());
				msg(sender, "user.added", PlaceholdersExecutor.i().add("target", query.getName()));
			}
		}).argument("-s", (sender, structure, args) -> { // silent
			Query query = API.offlineCache().lookupQuery(args[0]);
			if (query == null) {
				msg(sender, "never-joined", PlaceholdersExecutor.i().add("target", args[0]));
				return;
			}
			List<UUID> toggled = TpaManager.getProvider().getToggledPlayers(sender.getUniqueId());
			if (toggled.contains(query.getUUID()))
				TpaManager.getProvider().removeFromToggledPlayers(sender.getUniqueId(), query.getUUID());
			else
				TpaManager.getProvider().addToToggledPlayers(sender.getUniqueId(), query.getUUID());
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
