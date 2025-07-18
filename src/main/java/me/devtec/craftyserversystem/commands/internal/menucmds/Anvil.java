package me.devtec.craftyserversystem.commands.internal.menucmds;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.gui.AnvilGUI;

public class Anvil extends CssCommand {

	@Override
	public String section() {
		return "anvil";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure
				.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
					if (!(sender instanceof Player)) {
						msgUsage(sender, "cmd");
						return;
					}
					openInv(sender, (Player) sender, true);
				}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "cmd");
				return;
			}
			openInv(sender, (Player) sender, false);
		});
		// other
		cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				openInv(sender, player, true);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						openInv(sender, player, false);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void openInv(CommandSender sender, Player target, boolean sendMessages) {
		new AnvilGUI("Anvil", target) {
			@Override
			public void onClose(Player player) {
				ItemStack item = getItem(0);
				if (item != null)
					target.getInventory().addItem(item);
				item = getItem(1);
				if (item != null)
					target.getInventory().addItem(item);
				item = player.getItemOnCursor();
				player.setItemOnCursor(null);
				if (item != null)
					target.getInventory().addItem(item);
			}
		}.setInsertable(true);
		if (sendMessages)
			if (sender.equals(target))
				msg(sender, "self");
			else {
				PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target",
						target.getName());
				msg(sender, "other.sender", ex);
				msg(target, "other.target", ex);
			}
	}

}
