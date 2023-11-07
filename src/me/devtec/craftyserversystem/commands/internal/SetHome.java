package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.game.Position;

public class SetHome extends CssCommand {

	@Override
	public String section() {
		return "sethome";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			List<String> homes = HomeManager.get().getHomes(sender.getName());
			homes.remove("home");
			if (homes.size() >= HomeManager.get().getMaximumHomes(sender.getName())) {
				msgUsage(sender, "cmd");
				return;
			}
			Position pos;
			HomeManager.get().setHome(sender.getName(), "home", pos = Position.fromEntity(sender));
			msg(sender, "set", PlaceholdersExecutor.i().add("home", "home").add("x", pos.getX()).add("y", pos.getY()).add("z", pos.getZ()).add("yaw", pos.getYaw()).add("pitch", pos.getPitch())
					.add("world", pos.getWorldName()));
		}).permission(getPerm("cmd"));
		// home
		cmd.argument(null, 1, (sender, structure, args) -> {
			List<String> homes = HomeManager.get().getHomes(sender.getName());
			homes.remove(args[0].toLowerCase());
			int maxHomes = HomeManager.get().getMaximumHomes(sender.getName());
			if (homes.size() >= maxHomes) {
				msg(sender, "overlimit", PlaceholdersExecutor.i().add("totalHomes", homes.size()).add("maxHomes", maxHomes));
				return;
			}
			Position pos;
			HomeManager.get().setHome(sender.getName(), args[0].toLowerCase(), pos = Position.fromEntity(sender));
			msg(sender, "set", PlaceholdersExecutor.i().add("home", args[0].toLowerCase()).add("x", pos.getX()).add("y", pos.getY()).add("z", pos.getZ()).add("yaw", pos.getYaw())
					.add("pitch", pos.getPitch()).add("world", pos.getWorldName()));
		}, (sender, structure, args) -> {
			List<String> homes = HomeManager.get().getHomes(sender.getName());
			homes.add("{homeName}");
			return homes;
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
