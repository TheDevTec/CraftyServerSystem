package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Night extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNight(sender, ((Player) sender).getWorld(), true);
		}).permission(getPerm("cmd")).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setNight(sender, ((Player) sender).getWorld(), false);
		}).parent();
		cmd.selector(Selector.WORLD, (sender, structure, args) -> {
			setNight(sender, Bukkit.getWorld(args[0]), true);
		}).argument("-s", (sender, structure, args) -> {
			setNight(sender, Bukkit.getWorld(args[0]), false);
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void setNight(CommandSender sender, World world, boolean sendMessages) {
		world.setTime(13000);
		if (sendMessages)
			msgOut(sender, "time.night", PlaceholdersExecutor.i().add("world", world.getName()));
	}

}
