package me.devtec.craftyserversystem.commands.internal;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.msgsystem.MsgManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Reply extends CssCommand {

	@Override
	public String section() {
		return "reply";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		cmd.argument(null, (sender, structure, args) -> {
			String replyTo = MsgManager.get().getReply(sender instanceof Player ? sender.getName() : null);
			sendMessage(sender, replyTo, StringUtils.buildString(1, args));
		}, (sender, structure, args) -> Arrays.asList("{message}"));
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void sendMessage(CommandSender sender, String replyTo, String message) {
		if (replyTo == null) {
			msg(sender, "noone");
			return;
		}
		CommandSender target = replyTo.equals("$CONSOLE") ? Bukkit.getConsoleSender() : Bukkit.getPlayerExact(replyTo);
		if (target == null) {
			msg(sender, "not-online", PlaceholdersExecutor.i().add("target", replyTo));
			return;
		}
		String senderName = sender instanceof Player ? sender.getName() : "$CONSOLE";
		String targetName = replyTo;
		if (!MsgManager.get().trySendMessage(senderName, targetName)) {
			msgOut(sender, "msg.not-accepting", PlaceholdersExecutor.i().add("target", targetName));
			return;
		}
		PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", senderName).add("target", targetName).add("message", message);
		msgOut(sender, "msg.receive.sender", ex);
		msgOut(target, "msg.receive.target", ex);
		MsgManager.get().setReply(target instanceof Player ? targetName : null, sender instanceof Player ? senderName : null);
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (MsgManager.get().getSpy(player.getName()) && !player.equals(sender) && !player.equals(target))
				msgOut(player, "msg.receive.spy", ex);
		if (!Bukkit.getConsoleSender().equals(sender) && !Bukkit.getConsoleSender().equals(target))
			msgOut(Bukkit.getConsoleSender(), "msg.receive.spy", ex);
	}
}
