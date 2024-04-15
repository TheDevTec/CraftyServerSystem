package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class PlayerWeather extends CssCommand {

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
			setSun(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setSun(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setSun(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setSun(sender, player, false);
		});

		cmd.argument("rain", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setRain(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setRain(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setRain(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setRain(sender, player, false);
		});

		cmd.argument("reset", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			reset(sender, (Player) sender, true);
		}).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			reset(sender, (Player) sender, false);
		}).parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				reset(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				reset(sender, player, false);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void reset(CommandSender sender, Player target, boolean sendMessages) {
		target.resetPlayerWeather();
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playerweather-reset.other.sender", PLACEHOLDERS);
				msgOut(target, "playerweather-reset.other.target", PLACEHOLDERS);
			} else
				msgOut(target, "playerweather-reset.self");
	}

	public void setSun(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerWeather(WeatherType.CLEAR);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playersun.other.sender", PLACEHOLDERS);
				msgOut(target, "playersun.other.target", PLACEHOLDERS);
			} else
				msgOut(sender, "playersun.self");
	}

	public void setRain(CommandSender sender, Player target, boolean sendMessages) {
		target.setPlayerWeather(WeatherType.DOWNFALL);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msgOut(sender, "playerrain.other.sender", PLACEHOLDERS);
				msgOut(target, "playerrain.other.target", PLACEHOLDERS);
			} else
				msgOut(sender, "playerrain.self");
	}

}
