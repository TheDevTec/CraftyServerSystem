package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.kits.KitSample;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class Kit extends CssCommand {

	private static Map<String, KitSample> kits = new HashMap<>();

	@Override
	public String section() {
		return "kit";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		Config config = API.get().getConfigManager().getKits();
		for (String key : config.getKeys()) {
			KitSample sample = new KitSample(key);
			sample.setCost(config.getDouble(key + ".settings.cost"));
			sample.setOverrideContents(config.getBoolean(key + ".settings.override-contents-in-slots"));
			sample.setDropItems(config.getBoolean(key + ".settings.drop-items-when-full-inv"));
			sample.setCommands(config.getStringList(key + ".commands"));
			for (String content : config.getStringList(key + ".contents")) {
				int index = content.indexOf('{');
				if (index == -1) {
					Optional<XMaterial> material = XMaterial.matchXMaterial(content.toUpperCase());
					if (material.isPresent())
						sample.getContents().add(material.get().parseItem());
					else {
						Material bukkitType = Material.getMaterial(content);
						if (bukkitType != null)
							sample.getContents().add(new ItemStack(bukkitType));
						else
							Loader.getPlugin().getLogger().warning("An error occurred while building kit '" + key + "'. Material '" + content + "' is invalid. '");
					}
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Object> json = new HashMap<>((Map<String, Object>) Json.reader().simpleRead(content.substring(index)));
					json.put("type", content.substring(0, index));
					ItemStack stack = ItemMaker.loadFromJson(json);
					if (stack == null)
						Loader.getPlugin().getLogger().warning("An error occurred while building kit '" + key + "'. Material '" + json.get("type") + "' is invalid. '");
					else
						sample.getContents().add(stack);
				}
			}
			kits.put(key.toLowerCase(), sample);
		}
		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "console");
				return;
			}
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// kit
		CommandStructure<CommandSender> kitCmd = cmd.callableArgument((sender, structure, args) -> {
			List<String> value = new ArrayList<>();
			for (KitSample sample : kits.values())
				if (sample.getCooldown().tryWithoutWriting(sender))
					value.add(sample.getName());
			return value;
		}, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "console");
				return;
			}
			useKit((Player) sender, kits.get(args[0].toLowerCase()), false, false, true, sender);
		});

		kitCmd.callableArgument((sender, structure, args) -> {
			if (args[1].isEmpty())
				return perm(sender, "no-cost") && perm(sender, "no-cooldown") ? Arrays.asList("-c", "-w", "-s")
						: perm(sender, "no-cost") ? Arrays.asList("-c", "-s") : perm(sender, "no-cooldown") ? Arrays.asList("-w", "-s") : Arrays.asList("-s");
			if (args[1].indexOf('-') != -1) {
				List<String> result = new ArrayList<>();
				result.add(args[1]);
				if (args[1].indexOf('c') == -1 && perm(sender, "no-cost"))
					result.add(args[1] + 'c');
				if (args[1].indexOf('w') == -1 && perm(sender, "no-cooldown"))
					result.add(args[1] + 'w');
				if (args[1].indexOf('s') == -1)
					result.add(args[1] + 's');
				return result;
			}
			return Collections.emptyList();
		}, 1, (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "console");
				return;
			}
			useKit((Player) sender, kits.get(args[0].toLowerCase()), perm(sender, "no-cost") ? args[1].indexOf('c') != -1 : false, perm(sender, "no-cooldown") ? args[1].indexOf('w') != -1 : false,
					args[1].indexOf('s') == -1, sender);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	@Override
	public void unregister() {
		super.unregister();
		for (KitSample sample : kits.values())
			API.get().getCooldownManager().unregister(sample.getCooldown().id());
		kits.clear();
	}

	private void useKit(Player target, KitSample kit, boolean ignoreCost, boolean ignoreCooldown, boolean sendMessage, CommandSender sender) {
		PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().papi(target.getUniqueId()).add("kit", kit.getName()).add("cost", kit.getCost()).add("admin", sender.getName())
				.add("target", target.getName()).add("player", target.getName());
		if (!ignoreCooldown && !kit.getCooldown().tryWithoutWriting(target)) {
			long currentTime = System.currentTimeMillis() / 1000;
			Config file = me.devtec.shared.API.getUser(target.getName());
			long lastUsedTime = file.getLong("css.cd." + kit.getCooldown().id());
			long nextUsageIn = lastUsedTime - currentTime;
			kit.getCooldown().accept(target);
			placeholders.add("time", StringUtils.formatDouble(FormatType.NORMAL, nextUsageIn));
			if (sender.equals(target)) {
				if (sendMessage)
					msg(target, "in-cooldown.self", placeholders);
			} else {
				if (sendMessage)
					msg(target, "in-cooldown.target", placeholders);
				msg(sender, "in-cooldown.sender", placeholders);
			}
			return;
		}
		if (!ignoreCost && kit.getCost() > 0 && API.get().getEconomyHook().has(target.getName(), kit.getCost())) {
			if (sender.equals(target)) {
				if (sendMessage)
					msg(target, "enough-money.self", placeholders);
			} else {
				if (sendMessage)
					msg(target, "enough-money.target", placeholders);
				msg(sender, "enough-money.sender", placeholders);
			}
			return;
		}
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getKits(), kit.getName() + ".messages", placeholders, target);
		BukkitLoader.getNmsProvider().postToMainThread(() -> {
			for (ItemStack stack : kit.getContents()) {
				if (target.getInventory().firstEmpty() == -1)
					target.getWorld().dropItem(target.getLocation(), stack);
				target.getInventory().addItem(stack);
			}
			for (String cmd : kit.getCommands())
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders.applyAfterColorize(cmd));
		});
		if (sender.equals(target)) {
			if (sendMessage)
				msg(target, "used.self", placeholders);
		} else {
			if (sendMessage)
				msg(target, "used.target", placeholders);
			msg(sender, "used.sender", placeholders);
		}
	}

}
