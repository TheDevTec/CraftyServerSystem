package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.game.ItemMaker;

public class Skull extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			giveHead(sender, sender, sender.getName(), true);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			giveHead(sender, sender, sender.getName(), false);
		}).priority(0);
		// Head owner
		CommandStructure<Player> headValue = cmd.argument(null, (sender, structure, args) -> {
			giveHead(sender, sender, args[0], true);
		}).priority(1)
				// silent
				.argument("-s", (sender, structure, args) -> {
					giveHead(sender, sender, args[0], false);
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

	private void giveHead(Player sender, Player target, String owner, boolean sendMessages) {
		ItemStack head;
		if (owner.startsWith("http://") || owner.startsWith("https://"))
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
