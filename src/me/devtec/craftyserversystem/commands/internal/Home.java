package me.devtec.craftyserversystem.commands.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.game.Position;

public class Home extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		Map<String, Integer> map = new HashMap<>();
		map.put("default", 1);
		for (String group : API.get().getConfigManager().getMain().getKeys("homes"))
			map.put(group, API.get().getConfigManager().getMain().getString("homes." + group).equals("UNLIMITED") ? Integer.MAX_VALUE
					: Math.max(1, API.get().getConfigManager().getMain().getInt("homes." + group)));
		HomeManager.get().load(map);

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (HomeManager.get().getHomes(sender.getName()).isEmpty()) {
				msg(sender, "no-home", PlaceholdersExecutor.EMPTY);
				return;
			}
			String home = HomeManager.get().getHomes(sender.getName()).contains("home") ? "home" : HomeManager.get().getHomes(sender.getName()).get(0);
			Position pos = HomeManager.get().getHomePosition(sender.getName(), home);
			sender.teleport(pos.toLocation());
			msg(sender, "teleport", PlaceholdersExecutor.i().add("home", home));
		}).permission(getPerm("cmd"));
		// home
		cmd.callableArgument((sender, structure, args) -> HomeManager.get().getHomes(sender.getName()), 1, (sender, structure, args) -> {
			String home = args[0].toLowerCase();
			Position pos = HomeManager.get().getHomePosition(sender.getName(), home);
			sender.teleport(pos.toLocation());
			msg(sender, "teleport", PlaceholdersExecutor.i().add("home", home));
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
