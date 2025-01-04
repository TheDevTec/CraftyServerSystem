package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class GetPos extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			getPos((Player) sender, sender);
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			getPos(Bukkit.getPlayer(args[0]), sender);
		}).permission(getPerm("other"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void getPos(Player target, CommandSender sender) {
		Location loc = target.getLocation();
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("x", loc.getX()).add("y", loc.getY()).add("z", loc.getZ()).add("yaw", loc.getYaw())
				.add("pitch", loc.getPitch()).add("world", loc.getWorld().getName());
		if (!sender.equals(target))
			msg(sender, "other", placeholders);
		else
			msg(sender, "self", placeholders);
	}

}
