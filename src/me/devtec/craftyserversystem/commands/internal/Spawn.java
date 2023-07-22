package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Spawn implements CssCommand {

	private CommandHolder<CommandSender> cmd;

	@Override
	public String section() {
		return "spawn";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			spawn((Player) sender, true, sender);
		}).permission(getPerm("cmd"));

		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			spawn((Player) sender, false, sender);
		});

		// other
		cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				spawn(player, true, sender);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						spawn(player, false, sender);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void spawn(Player target, boolean sendMessage, CommandSender sender) {
		if (sendMessage)
			if (target.equals(sender)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName());
				msg(sender, "self", placeholders);
			} else {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("admin", sender.getName());
				msg(sender, "other.target", placeholders);
				msg(sender, "other.admin", placeholders);
			}
		// You can teleport entity only in primary thread
		if (!Bukkit.isPrimaryThread())
			BukkitLoader.getNmsProvider().postToMainThread(() -> target.teleport(API.get().getConfigManager().getSpawn().toLocation()));
		else
			target.teleport(API.get().getConfigManager().getSpawn().toLocation());
	}

	@Override
	public void unregister() {
		if (!isRegistered())
			return;
		cmd.unregister();
		cmd = null;
	}

	@Override
	public boolean isRegistered() {
		return cmd != null;
	}

}
