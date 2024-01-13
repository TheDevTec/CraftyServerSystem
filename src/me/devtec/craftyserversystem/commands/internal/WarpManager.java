package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class WarpManager extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		// Ugh, horrible
		me.devtec.craftyserversystem.commands.internal.warp.WarpManager provider = me.devtec.craftyserversystem.commands.internal.warp.WarpManager.getProvider();

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).callableArgument((sender, structure, args) -> StringUtils.copyPartialMatches(args[0], provider.getWarps()), (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		});

		// icon
		if (API.get().getConfigManager().getMain().getBoolean("warp.enable-menu"))
			cmd.argument("icon", (sender, structure, args) -> {
				ItemStack icon = ((Player) sender).getItemInHand();
				if (icon.getType() == Material.AIR) {
					msg(sender, "icon.empty-hand");
					return;
				}
				Warp.callMenuUpdate();
				provider.get(args[0]).setIcon(icon);
				msg(sender, "icon.set", PlaceholdersExecutor.i().add("warp", args[0]).add("type", XMaterial.matchXMaterial(icon).getFormattedName()));
			}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("icon") : Collections.emptyList());

		cmd.argument("cost", (sender, structure, args) -> {
			msgUsage(sender, "cmd-cost");
		}).selector(Selector.NUMBER, (sender, structure, args) -> {
			double cost = ParseUtils.getDouble(args[2]);
			if (cost < 0) {
				msg(sender, "cost.is-under-zero");
				return;
			}
			Warp.callMenuUpdate();
			provider.get(args[0]).setCost(cost);
			msg(sender, "cost.set", PlaceholdersExecutor.i().add("warp", args[0]).add("cost", cost));
		});

		cmd.argument("perm", (sender, structure, args) -> {
			msgUsage(sender, "cmd-perm");
		}).argument(null, 1, (sender, structure, args) -> {
			String permission = args[2].toLowerCase();
			if (permission.equals("none")) {
				provider.get(args[0]).setPermission(null);
				Warp.callMenuUpdate();
				msg(sender, "perm.none", PlaceholdersExecutor.i().add("warp", args[0]));
				return;
			}
			Warp.callMenuUpdate();
			provider.get(args[0]).setPermission("css.cmd.warp." + permission);
			msg(sender, "perm.set", PlaceholdersExecutor.i().add("warp", args[0]).add("permission", "css.cmd.warp." + permission));
		}, (sender, structure, args) -> {
			List<String> tabCompleter = new ArrayList<>();
			tabCompleter.add("none");
			tabCompleter.add(args[0].toLowerCase());
			tabCompleter.add("{perm}");
			return tabCompleter;
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}
}
