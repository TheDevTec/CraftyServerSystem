package me.devtec.craftyserversystem.commands.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Thor extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			Block block = getLookingBlock((Player) sender, 15);
			if (block == null)
				msgUsage(sender, "usage");
			else
				smite(block.getLocation(), true, true, sender);
		}).permission(getPerm("cmd"));
		// attack / silent
		cmd.argument("-attack", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			Block block = getLookingBlock((Player) sender, 15);
			if (block == null)
				msgUsage(sender, "usage");
			else
				smite(block.getLocation(), true, false, sender);
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("-attack") : Collections.emptyList()).argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			Block block = getLookingBlock((Player) sender, 15);
			if (block == null)
				msgUsage(sender, "usage");
			else
				smite(block.getLocation(), false, false, sender);
		});
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			Block block = getLookingBlock((Player) sender, 15);
			if (block == null)
				msgUsage(sender, "usage");
			else
				smite(block.getLocation(), false, true, sender);
		});
		// other
		CommandStructure<CommandSender> target = cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			smite(player, true, true, sender);
		});
		// attack / silent
		target.argument("-attack", (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			smite(player, true, false, sender);
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("-attack") : Collections.emptyList()).argument("-s", (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			smite(player, false, false, sender);
		});
		target.argument("-s", (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			smite(player, false, true, sender);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public static Block getLookingBlock(Player player, int range) {
		BlockIterator iter = new BlockIterator(player, range);
		Block lastBlock = iter.next();
		while (iter.hasNext()) {
			lastBlock = iter.next();
			if (lastBlock.getType() == Material.AIR || lastBlock.getType().name().equals("CAVE_AIR") || lastBlock.isLiquid() || !lastBlock.getType().isSolid())
				continue;
			break;
		}
		return lastBlock;
	}

	public void smite(Player target, boolean sendMessage, boolean onlyEffect, CommandSender sender) {
		if (onlyEffect)
			target.getWorld().strikeLightningEffect(target.getLocation());
		else
			target.getWorld().strikeLightning(target.getLocation());
		if (sendMessage)
			if (onlyEffect) {
				if (!sender.equals(target)) {
					PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
					msg(target, "other.effect.target", placeholders);
					msg(sender, "other.effect.sender", placeholders);
				} else
					msg(target, "self.effect", PlaceholdersExecutor.i().add("target", target.getName()));
			} else if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msg(target, "other.attack.target", placeholders);
				msg(sender, "other.attack.sender", placeholders);
			} else
				msg(target, "self.attack", PlaceholdersExecutor.i().add("target", target.getName()));
	}

	public void smite(Location looking, boolean sendMessage, boolean onlyEffect, CommandSender sender) {
		if (onlyEffect)
			looking.getWorld().strikeLightningEffect(looking);
		else
			looking.getWorld().strikeLightning(looking);
		if (sendMessage)
			if (onlyEffect)
				msg(sender, "block.effect", PlaceholdersExecutor.i().add("x", looking.getX()).add("y", looking.getY()).add("z", looking.getZ()).add("world", looking.getWorld().getName()));
			else
				msg(sender, "block.attack", PlaceholdersExecutor.i().add("x", looking.getX()).add("y", looking.getY()).add("z", looking.getZ()).add("world", looking.getWorld().getName()));

	}

}
