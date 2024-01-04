package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Hat extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			ItemStack inHand = sender.getItemInHand();
			if (inHand.getType() == Material.AIR) {
				msg(sender, "empty-hand");
				return;
			}
			ItemStack helmet = sender.getEquipment().getHelmet();
			sender.getEquipment().setHelmet(inHand);
			sender.setItemInHand(helmet);
			msg(sender, "set.self");
		}).permission(getPerm("cmd"));
		// other
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player target = Bukkit.getPlayer(args[0]);
			ItemStack inHand = sender.getItemInHand();
			if (inHand.getType() == Material.AIR) {
				msg(sender, "empty-hand");
				return;
			}
			ItemStack helmet = target.getEquipment().getHelmet();
			target.getEquipment().setHelmet(inHand);
			sender.setItemInHand(helmet);
			PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
			msg(sender, "set.other.sender", placeholders);
			msg(target, "set.other.targer", placeholders);
		}).permission(getPerm("other"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
