package me.devtec.craftyserversystem.commands.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ParseUtils;

public class Near extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "usage");
		}).permission(getPerm("cmd")).selector(Selector.NUMBER, (sender, structure, args) -> {
			double distance = ParseUtils.getDouble(args[0]);
			Map<String, Double> near = new HashMap<>();
			Location loc = sender.getLocation();
			for (Player player : sender.getWorld().getPlayers()) {
				if (player.equals(sender))
					continue;
				double pDistance = player.getLocation().distance(loc);
				if (pDistance <= distance)
					near.put(player.getName(), pDistance);
			}
			msg(sender, "result", PlaceholdersExecutor.i().add("amount", near.size()));
			for (Entry<String, Double> entry : near.entrySet())
				msg(sender, "item", PlaceholdersExecutor.i().add("target", entry.getKey()).add("distance", entry.getValue()));
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
