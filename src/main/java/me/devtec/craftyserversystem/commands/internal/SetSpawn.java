package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.game.Position;

public class SetSpawn extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure
				.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
					setSpawn(Position.fromEntity(sender), sender);
				}).permission(getPerm("cmd"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void setSpawn(Position pos, CommandSender sender) {
		API.get().getConfigManager().setSpawn(pos);
		if (sender != null)
			msg(sender, "set",
					renderer().placeholder("world", pos.getWorldName()).placeholder("x", pos.getX())
							.placeholder("y", pos.getY()).placeholder("z", pos.getZ()).placeholder("yaw", pos.getYaw())
							.placeholder("pitch", pos.getPitch()));
	}

}
