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

public class Weather extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "usage");
		}).permission(getPerm("cmd"));
		cmd.argument("sun", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setSun(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setSun(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setSun(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setSun(sender, Bukkit.getWorld(args[1]), false);
		});

		cmd.argument("rain", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setRain(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setRain(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setRain(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setRain(sender, Bukkit.getWorld(args[1]), false);
		});

		cmd.argument("thunder", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setThunder(sender, ((Player) sender).getWorld(), true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setThunder(sender, ((Player) sender).getWorld(), false);
		}).parent().selector(Selector.WORLD, (sender, structure, args) -> {
			setThunder(sender, Bukkit.getWorld(args[1]), true);
		}).argument("-s", (sender, structure, args) -> {
			setThunder(sender, Bukkit.getWorld(args[1]), false);
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void setSun(CommandSender sender, World world, boolean sendMessages) {
		world.setStorm(false);
		world.setThundering(false);
		world.setWeatherDuration(36000);
		if (sendMessages)
			msgOut(sender, "weather.sun", PlaceholdersExecutor.i().add("world", world.getName()));
	}

	public void setRain(CommandSender sender, World world, boolean sendMessages) {
		world.setStorm(true);
		world.setThundering(false);
		world.setWeatherDuration(36000);
		if (sendMessages)
			msgOut(sender, "weather.rain", PlaceholdersExecutor.i().add("world", world.getName()));
	}

	public void setThunder(CommandSender sender, World world, boolean sendMessages) {
		world.setStorm(true);
		world.setThundering(false);
		world.setWeatherDuration(36000);
		if (sendMessages)
			msgOut(sender, "weather.thunder", PlaceholdersExecutor.i().add("world", world.getName()));
	}

}
