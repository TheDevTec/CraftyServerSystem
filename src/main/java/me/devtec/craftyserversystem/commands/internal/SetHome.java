package me.devtec.craftyserversystem.commands.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.game.Position;

public class SetHome extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure
				.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
					Set<String> homes = new HashSet<>(HomeManager.get().getHomes(sender.getName()));
					if (!homes.isEmpty())
						homes.remove("home");
					if (homes.size() >= HomeManager.get().getMaximumHomes(sender.getName())) {
						msgUsage(sender, "cmd");
						return;
					}
					Position pos;
					HomeManager.get().setHome(sender.getName(), "home", pos = Position.fromEntity(sender));
					msg(sender, "set", renderer().placeholder("home", "home").placeholder("x", pos.getX())
							.placeholder("y", pos.getY()).placeholder("z", pos.getZ()).placeholder("yaw", pos.getYaw())
							.placeholder("pitch", pos.getPitch()).placeholder("world", pos.getWorldName()));
				}).permission(getPerm("cmd"));
		// home
		cmd.argument(null, 1, (sender, structure, args) -> {
			Set<String> homes = new HashSet<>(HomeManager.get().getHomes(sender.getName()));
			if (!homes.isEmpty())
				homes.remove(args[0].toLowerCase());
			int maxHomes = HomeManager.get().getMaximumHomes(sender.getName());
			if (homes.size() >= maxHomes) {
				msg(sender, "overlimit",
						renderer().placeholder("totalHomes", homes.size()).placeholder("maxHomes", maxHomes));
				return;
			}
			Position pos;
			HomeManager.get().setHome(sender.getName(), args[0].toLowerCase(), pos = Position.fromEntity(sender));
			msg(sender, "set",
					renderer().placeholder("home", args[0].toLowerCase()).placeholder("x", pos.getX())
							.placeholder("y", pos.getY()).placeholder("z", pos.getZ()).placeholder("yaw", pos.getYaw())
							.placeholder("pitch", pos.getPitch()).placeholder("world", pos.getWorldName()));
		}, (sender, structure, args) -> {
			Set<String> homes = new HashSet<>(HomeManager.get().getHomes(sender.getName()));
			homes.add("{homeName}");
			return homes;
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
