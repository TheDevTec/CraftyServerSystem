package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.text.TextRenderer;

public class PlayerNight extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure
				.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
					if (!(sender instanceof Player)) {
						msgUsage(sender, "usage");
						return;
					}
					setNight(sender, (Player) sender, true);
				}).permission(getPerm("cmd")).argument("-s", (sender, structure, args) -> {
					if (!(sender instanceof Player)) {
						msgUsage(sender, "usage");
						return;
					}
					setNight(sender, (Player) sender, false);
				}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						setNight(sender, player, true);
				}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						setNight(sender, player, false);
				});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void setNight(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerTime(13000, false);
		if (sendMessages)
			if (!sender.equals(target)) {
				TextRenderer PLACEHOLDERS = renderer().placeholder("sender", sender.getName()).placeholder("target",
						target.getName());
				msg(sender, "other.sender", PLACEHOLDERS);
				msg(target, "other.target", PLACEHOLDERS);
			} else
				msg(sender, "self");
	}

}
