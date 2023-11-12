package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.msgsystem.MsgManager;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Msg extends CssCommand {

	@Override
	public String section() {
		return "msg";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// console
		cmd.argument("$CONSOLE", 1, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("$CONSOLE") : Collections.emptyList()) // Create tab completer without console option, if sender isn't player
				.argument(null, (sender, structure, args) -> { // Message argument
					sendMessage(sender, Bukkit.getConsoleSender(), StringUtils.buildString(1, args));
				}, (sender, structure, args) -> Arrays.asList("{message}"));
		// player
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> { // Player argument
			msgUsage(sender, "cmd");
		}, (sender, structure, args) -> { // Tab completer
			Collection<? extends Player> onlinePlayers = BukkitLoader.getOnlinePlayers();
			List<String> players = new ArrayList<>(onlinePlayers.size());
			for (Player player : onlinePlayers)
				if (!player.equals(sender))
					players.add(player.getName());
			return players; // Create tab completer without sender's name
		}).argument(null, (sender, structure, args) -> { // Message argument
			sendMessage(sender, Bukkit.getPlayer(args[0]), StringUtils.buildString(1, args));
		}, (sender, structure, args) -> Arrays.asList("{message}"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void sendMessage(CommandSender sender, CommandSender target, String message) {
		String senderName = sender instanceof Player ? sender.getName() : "$CONSOLE";
		String targetName = target instanceof Player ? target.getName() : "$CONSOLE";
		if (senderName.equals(targetName)) {
			msg(sender, "self");
			return;
		}
		if (!MsgManager.get().trySendMessage(senderName, targetName)) {
			msg(sender, "not-accepting", PlaceholdersExecutor.i().add("target", targetName));
			return;
		}
		PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", senderName).add("target", targetName).add("message", message);
		msg(sender, "receive.sender", ex);
		msg(target, "receive.target", ex);
		MsgManager.get().setReply(sender instanceof Player ? senderName : null, target instanceof Player ? targetName : null);
		MsgManager.get().setReply(target instanceof Player ? targetName : null, sender instanceof Player ? senderName : null);
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (MsgManager.get().getSpy(player.getName()) && !player.equals(sender) && !player.equals(target))
				msg(player, "receive.spy", ex);
		if (!Bukkit.getConsoleSender().equals(sender) && !Bukkit.getConsoleSender().equals(target))
			msg(Bukkit.getConsoleSender(), "receive.spy", ex);
	}
}
