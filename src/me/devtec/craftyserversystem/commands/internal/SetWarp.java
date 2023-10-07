package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.warp.WarpInfo;
import me.devtec.craftyserversystem.commands.internal.warp.WarpManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.theapi.bukkit.game.Position;

public class SetWarp extends CssCommand {

	@Override
	public String section() {
		return "setwarp";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, 1, (sender, structure, args) -> {
			setWarp(args[0].toLowerCase(), Position.fromEntity(sender), sender);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void setWarp(String warpName, Position pos, CommandSender sender) {
		WarpInfo info;
		if ((info = WarpManager.getProvider().get(warpName)) != null) {
			info.setPosition(pos);
			PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("warp", warpName).add("world", pos.getWorldName()).add("x", StringUtils.formatDouble(FormatType.NORMAL, pos.getX()))
					.add("y", StringUtils.formatDouble(FormatType.NORMAL, pos.getY())).add("z", StringUtils.formatDouble(FormatType.NORMAL, pos.getZ()))
					.add("yaw", StringUtils.formatDouble(FormatType.NORMAL, pos.getYaw())).add("pitch", StringUtils.formatDouble(FormatType.NORMAL, pos.getPitch()));
			msg(sender, "moved", placeholders);
		} else {
			info = WarpManager.getProvider().create(warpName, pos);
			PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("warp", warpName).add("world", pos.getWorldName()).add("x", StringUtils.formatDouble(FormatType.NORMAL, pos.getX()))
					.add("y", StringUtils.formatDouble(FormatType.NORMAL, pos.getY())).add("z", StringUtils.formatDouble(FormatType.NORMAL, pos.getZ()))
					.add("yaw", StringUtils.formatDouble(FormatType.NORMAL, pos.getYaw())).add("pitch", StringUtils.formatDouble(FormatType.NORMAL, pos.getPitch()));
			msg(sender, "created", placeholders);
		}
	}

}
