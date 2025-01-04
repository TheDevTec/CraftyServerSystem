package me.devtec.craftyserversystem.commands.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.events.internal.BossBarListener;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.bossbar.UserBossBarData;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.ParseUtils;

public class BossBarHide extends CssCommand {

	@Override
	public void register() {
		if (isRegistered() || !API.get().getConfigManager().getBossBar().getBoolean("enabled"))
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "usage");
				return;
			}
			toggle(sender, (Player) sender, true);
		}).permission(getPerm("cmd")).argument("-s", (sender, structure, args) -> {
			toggle(sender, (Player) sender, false);
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("-s") : Collections.emptyList()).firstParent().selector(Selector.BOOLEAN, (sender, structure, args) -> {
			setStatus(sender, (Player) sender, true, ParseUtils.getBoolean(args[0]));
		}, (sender, structure, args) -> sender instanceof Player ? Arrays.asList("true", "false") : Collections.emptyList()).argument("-s", (sender, structure, args) -> {
			setStatus(sender, (Player) sender, false, ParseUtils.getBoolean(args[0]));
		}).firstParent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				toggle(sender, player, true);
		}).permission(getPerm("other")).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				toggle(sender, player, false);
		}).parent().selector(Selector.BOOLEAN, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				setStatus(sender, player, true, ParseUtils.getBoolean(args[1]));
		}).argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				setStatus(sender, player, false, ParseUtils.getBoolean(args[1]));
		});
		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void toggle(CommandSender sender, Player target, boolean sendMessages) {
		UserBossBarData data = BossBarListener.data.get(target.getUniqueId());
		if (data == null)
			return;
		boolean status = !data.isHidden();
		data.setHidden(status);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msg(sender, "other." + status + ".sender", PLACEHOLDERS);
				msg(target, "other." + status + ".target", PLACEHOLDERS);
			} else
				msg(sender, "self." + status, PlaceholdersExecutor.EMPTY);
	}

	public void setStatus(CommandSender sender, Player target, boolean sendMessages, boolean status) {
		UserBossBarData data = BossBarListener.data.get(target.getUniqueId());
		if (data == null)
			return;
		if (data.isHidden() == status) {
			if (!sender.equals(target))
				msg(sender, "other.already-set-to." + status, PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()));
			else
				msg(sender, "self.already-set-to." + status, PlaceholdersExecutor.EMPTY);
			return;
		}
		data.setHidden(status);
		if (sendMessages)
			if (!sender.equals(target)) {
				PlaceholdersExecutor PLACEHOLDERS = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msg(sender, "other." + status + ".sender", PLACEHOLDERS);
				msg(target, "other." + status + ".target", PLACEHOLDERS);
			} else
				msg(sender, "self." + status, PlaceholdersExecutor.EMPTY);
	}

}
