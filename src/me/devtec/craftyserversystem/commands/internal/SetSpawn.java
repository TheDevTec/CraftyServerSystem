package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.theapi.bukkit.game.Position;

public class SetSpawn extends CssCommand {

	@Override
	public String section() {
		return "setspawn";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			setSpawn(Position.fromEntity(sender), sender);
		}).permission(getPerm("cmd"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void setSpawn(Position pos, CommandSender sender) {
		API.get().getConfigManager().setSpawn(pos);
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("world", pos.getWorldName()).add("x", StringUtils.formatDouble(FormatType.NORMAL, pos.getX()))
				.add("y", StringUtils.formatDouble(FormatType.NORMAL, pos.getY())).add("z", StringUtils.formatDouble(FormatType.NORMAL, pos.getZ()))
				.add("yaw", StringUtils.formatDouble(FormatType.NORMAL, pos.getYaw())).add("pitch", StringUtils.formatDouble(FormatType.NORMAL, pos.getPitch()));
		msg(sender, "set", placeholders);
	}

}
