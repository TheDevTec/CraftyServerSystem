package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class Repair extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if(sender instanceof Player)
				repairItemInHand(sender, (Player)sender, false);
			else
				msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			repairItemInHand(sender, Bukkit.getPlayer(args[0]), false);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			repairItemInHand(sender, Bukkit.getPlayer(args[0]), true);
		});
		cmd.argument("-s", (sender, structure, args) -> {
			if(sender instanceof Player)
				repairItemInHand(sender, (Player)sender, true);
			else
				msgUsage(sender, "cmd");
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void repairItemInHand(CommandSender sender, Player player, boolean silent) {
		ItemStack item = player.getEquipment().getItemInHand();
		if (item == null || item.getType()==Material.AIR || item.getAmount() <= 0) {
			if(!silent)
				if(sender.equals(player))
					msg(sender, "self.empty-hand");
				else
					msg(sender, "other.empty-hand", PlaceholdersExecutor.i().add("target", player.getName()));
			return;
		}
		if(item.getType().getMaxDurability()<=0) {
			if(!silent) {
				PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", player.getName()).add("item", XMaterial.matchXMaterial(item).getFormattedName());
				if(sender.equals(player))
					msg(sender, "self.cannot-be-fixed", ex);
				else
					msg(sender, "other.cannot-be-fixed", ex);
			}
			return;
		}
		if(!silent) {
			PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", player.getName()).add("item", XMaterial.matchXMaterial(item).getFormattedName());
			if(sender.equals(player))
				msg(sender, "self.fixed", ex);
			else {
				msg(sender, "other.fixed.sender", ex);
				msg(player, "other.fixed.target", ex);
			}
		}
		item.setDurability((short)0);
		player.getEquipment().setItemInHand(item);
	}
}
