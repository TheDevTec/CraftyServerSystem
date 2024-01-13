package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;

public class GamemodeAdventure extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			changeGameMode((Player) sender, true, sender);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			changeGameMode((Player) sender, false, sender);
		});
		// other
		cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				changeGameMode(player, true, sender);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[00]))
						changeGameMode(player, false, sender);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void changeGameMode(Player target, boolean sendMessage, CommandSender sender) {
		if (sendMessage)
			if (target.equals(sender)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("gamemode", "adventure");
				msg(sender, "self", placeholders);
			} else {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("sender", sender.getName()).add("gamemode", "adventure");
				msg(target, "other.target", placeholders);
				msg(sender, "other.sender", placeholders);
			}
		// You can change gamemode only in primary thread
		if (!Bukkit.isPrimaryThread())
			BukkitLoader.getNmsProvider().postToMainThread(() -> target.setGameMode(GameMode.ADVENTURE));
		else
			target.setGameMode(GameMode.ADVENTURE);
	}

}
