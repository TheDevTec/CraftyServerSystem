package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class PlayerRain extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			setRain(sender, (Player) sender);
		}).permission(getPerm("cmd")).selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				setRain(sender, player);
		}).permission(getPerm("other"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void setRain(CommandSender sender, Player target) {
		target.setPlayerWeather(WeatherType.DOWNFALL);
		if (!sender.equals(target)) {
			PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
			msgOut(sender, "playerrain.other.sender", PLACEHOLDERS);
			msgOut(target, "playerrain.other.target", PLACEHOLDERS);
		} else
			msgOut(sender, "playerrain.self", PlaceholdersExecutor.EMPTY);
	}

}
