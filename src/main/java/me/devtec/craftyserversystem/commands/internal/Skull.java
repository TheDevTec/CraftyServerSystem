package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;

public class Skull extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "usage");
		}).permission(getPerm("cmd"));
		// Head owner
		CommandStructure<CommandSender> headValue = cmd.argument(null, (sender, structure, args) -> {
			if (sender instanceof Player)
				giveHead(sender, (Player) sender, args[0], true);
			else
				msgUsage(sender, "usage");
		}, (sender, structure, args) -> {
			List<String> tabCompleter = new ArrayList<>();
			tabCompleter.add("{skullUrl.png}");
			tabCompleter.add("{skullValues}");
			if (Bukkit.getPluginManager().isPluginEnabled("HeadDatabase"))
				tabCompleter.add("{HDB:id}");
			tabCompleter.add("{skullOwner}");
			for (Player player : BukkitLoader.getOnlinePlayers())
				tabCompleter.add(player.getName());
			return tabCompleter;
		}).priority(1)
				// silent
				.argument("-s", (sender, structure, args) -> {
					if (sender instanceof Player)
						giveHead(sender, (Player) sender, args[0], false);
					else
						msgUsage(sender, "usage");
				}).parent();
		// Other
		headValue.selector(Selector.PLAYER, (sender, structure, args) -> {
			giveHead(sender, Bukkit.getPlayer(args[1]), args[0], true);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					giveHead(sender, Bukkit.getPlayer(args[1]), args[0], false);
				});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void giveHead(CommandSender sender, Player target, String owner, boolean sendMessages) {
		ItemStack head;
		if (owner.endsWith(".png"))
			head = ItemMaker.ofHead().skinUrl(owner).displayName("URL Generated Head").build();
		else if (owner.toLowerCase().startsWith("hdb:"))
			head = ItemMaker.ofHead().skinHDB(owner.substring(4)).displayName("HDB Generated Head").build();
		else if (owner.length() > 16)
			head = ItemMaker.ofHead().skinValues(owner).displayName("Values Generated Head").build();
		else
			head = ItemMaker.ofHead().skinName(owner).displayName(owner + "'s head").build();
		if (!target.getInventory().addItem(head).isEmpty())
			target.getWorld().dropItem(target.getLocation(), head);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("owner", owner);
				msg(sender, "other.sender", placeholders);
				msg(target, "other.target", placeholders);
			} else
				msg(target, "self", PlaceholdersExecutor.i().add("owner", owner));
	}

}
