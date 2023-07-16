package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Ref;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;

public class Heal implements CssCommand {

	private CommandHolder<CommandSender> cmd;

	@Override
	public String section() {
		return "heal";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			heal((Player) sender, true, sender);
		}).permission(getPerm("cmd"));
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			heal((Player) sender, false, sender);
		});
		// other
		cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[0]))
				heal(player, true, sender);
		}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[0]))
						heal(player, false, sender);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void heal(Player target, boolean sendMessage, CommandSender sender) {
		target.setHealth(target.getMaxHealth());
		target.setFoodLevel(20);
		target.setSaturation(10);
		target.setExhaustion(0);
		target.setFireTicks(0);
		target.setRemainingAir(target.getMaximumAir());
		if (Ref.isNewerThan(18)) // TODO 1.19+
			target.setFreezeTicks(0);
		for (PotionEffect effect : target.getActivePotionEffects())
			target.removePotionEffect(effect.getType());
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("admin", sender.getName()).add("target", target.getName());
				msg(target, "other.target", placeholders);
				msg(sender, "other.admin", placeholders);
			} else
				msg(target, "self", PlaceholdersExecutor.i().add("target", target.getName()));
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
