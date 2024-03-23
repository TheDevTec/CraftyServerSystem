package me.devtec.craftyserversystem.commands.internal;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Tphere extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			Collection<? extends Player> collection = selector(sender, args[0]);
			if (collection.isEmpty() || collection.size() == 1 && collection.contains(sender)) {
				teleport(sender, true, sender);
				return;
			}
			for (Player target : collection)
				if (!target.getUniqueId().equals(sender.getUniqueId()))
					teleport(target, true, sender);
		}).argument("-s", (sender, structure, args) -> { // silent
			Collection<? extends Player> collection = selector(sender, args[0]);
			if (collection.isEmpty() || collection.size() == 1 && collection.contains(sender)) {
				teleport(sender, true, sender);
				return;
			}
			for (Player target : collection)
				if (!target.getUniqueId().equals(sender.getUniqueId()))
					teleport(target, false, sender);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void teleport(Player target, boolean sendMessage, Player sender) {
		if (target.equals(sender)) {
			msg(sender, "failed.self", PlaceholdersExecutor.EMPTY);
			return;
		}
		target.teleport(sender);
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
		if (sendMessage) {
			msg(target, "success.target", placeholders);
			msg(sender, "success.sender", placeholders);
		}
	}

}
