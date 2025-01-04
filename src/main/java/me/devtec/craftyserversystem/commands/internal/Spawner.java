package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class Spawner extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "usage");
		}).permission(getPerm("cmd"))
				// Entity type
				.selector(Selector.ENTITY_TYPE, (sender, structure, args) -> {
					spawner(getLookingBlock(sender, 15), true, sender, EntityType.valueOf(args[0].toUpperCase()));
				})
				// silent
				.argument("-s", (sender, structure, args) -> {
					spawner(getLookingBlock(sender, 15), false, sender, EntityType.valueOf(args[0].toUpperCase()));
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

	public void spawner(Block target, boolean sendMessage, CommandSender sender, EntityType type) {
		if (XMaterial.matchXMaterial(target.getType()) != XMaterial.SPAWNER) {
			msg(sender, "not-spawner");
			return;
		}
		BukkitLoader.getNmsProvider().postToMainThread(() -> {
			CreatureSpawner spawner = (CreatureSpawner) target.getState();
			spawner.setSpawnedType(type);
			spawner.update(false, false);
		});
		if (sendMessage)
			msg(sender, "changed", PlaceholdersExecutor.i().add("type", getFormattedNameOf(type)));
	}

	public String getFormattedNameOf(EntityType type) {
		StringContainer container = new StringContainer(type.name().length());
		boolean first = true;
		for (String split : type.name().split("_")) {
			if (first) {
				container.append(split.charAt(0)).append(split.substring(1).toLowerCase());
				first = false;
				continue;
			}
			container.append(' ');
			if (split.equals("OF") || split.equals("THE"))
				container.append(split.toLowerCase());
			else
				container.append(split.charAt(0)).append(split.substring(1).toLowerCase());
		}
		return container.toString();
	}

}
