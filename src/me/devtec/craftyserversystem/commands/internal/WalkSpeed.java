package me.devtec.craftyserversystem.commands.internal;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ParseUtils;

public class WalkSpeed extends CssCommand {

	@Override
	public String section() {
		return "walkspeed";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			msgUsage(sender, "usage");
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			msgUsage(sender, "other");
		}).permission(getPerm("other"))
				// Speed
				.selector(Selector.NUMBER, (sender, structure, args) -> {
					speed(Bukkit.getPlayer(args[0]), ParseUtils.getFloat(args[1]), true, sender);
				})
				// silent
				.argument("-s", (sender, structure, args) -> {
					speed(Bukkit.getPlayer(args[0]), ParseUtils.getFloat(args[1]), false, sender);
				});
		cmd.selector(Selector.NUMBER, (sender, structure, args) -> {
			speed((Player) sender, ParseUtils.getFloat(args[0]), true, sender);
		}, (sender, structure, args) -> sender instanceof Player ? API.selectorUtils.build(args[0], Selector.NUMBER) : Collections.emptyList())
				// silent
				.argument("-s", (sender, structure, args) -> {
					speed((Player) sender, ParseUtils.getFloat(args[1]), false, sender);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void speed(Player target, float speed, boolean sendMessage, CommandSender sender) {
		target.setWalkSpeed(Math.max(0, Math.min(1, speed / 10)));
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("value", Math.max(0, Math.min(10, speed)));
				msg(target, "other.target", placeholders);
				msg(sender, "other.sender", placeholders);
			} else
				msg(target, "self", PlaceholdersExecutor.i().add("target", target.getName()));
	}

}
