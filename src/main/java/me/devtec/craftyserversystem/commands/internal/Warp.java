package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.warp.WarpInfo;
import me.devtec.craftyserversystem.commands.internal.warp.WarpManager;
import me.devtec.craftyserversystem.commands.internal.warp.WarpResult;
import me.devtec.shared.commands.holder.CommandExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.gui.GUI.ClickType;
import me.devtec.theapi.bukkit.gui.HolderGUI;
import me.devtec.theapi.bukkit.gui.ItemGUI;
import me.devtec.theapi.bukkit.gui.expansion.GuiCreator;
import me.devtec.theapi.bukkit.gui.expansion.actions.ActionManager;
import me.devtec.theapi.bukkit.gui.expansion.guis.LoopGuiCreator;
import me.devtec.theapi.bukkit.gui.expansion.items.ConditionItem;
import me.devtec.theapi.bukkit.gui.expansion.items.ItemPackage;
import me.devtec.theapi.bukkit.gui.expansion.loop.LoopManager;
import me.devtec.theapi.bukkit.gui.expansion.utils.Utils;

public class Warp extends CssCommand {

	private static final String LOOP_NAME = "warp";
	private static final String GUI_NAME = "warp";

	public static void callMenuUpdate() {
		// LoopManager generates warp items dynamically every time the menu is opened.
	}

	@Override
	public void unregister() {
		super.unregister();

		LoopManager.unregister(LOOP_NAME);
		ActionManager.unregister("warp");
		ActionManager.unregister("warp_silent");
		ActionManager.unregister("warp_instant");
		ActionManager.unregister("warp_silent_instant");

		WarpManager.getProvider().unload(true);
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		WarpManager.getProvider().load();

		registerLoop();
		registerActions();

		GuiCreator gui = GuiCreator.guis.get(GUI_NAME);
		if (gui instanceof LoopGuiCreator)
			((LoopGuiCreator) gui).reload();

		CommandExecutor<CommandSender> main;
		Config config = API.get().getConfigManager().getMain();

		if (config.getBoolean("warp.enable-menu"))
			main = (sender, structure, args) -> {
				if (!(sender instanceof Player)) {
					msgUsage(sender, "other");
					return;
				}

				GuiCreator creator = GuiCreator.guis.get(GUI_NAME);
				if (creator == null) {
					msgUsage(sender, "cmd");
					return;
				}

				creator.open((Player) sender);
			};
		else
			main = (sender, structure, args) -> {
				if (!(sender instanceof Player)) {
					msgUsage(sender, "other");
					return;
				}

				msgUsage(sender, "cmd");
			};

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, main)
				.permission(getPerm("cmd")).callableArgument((sender, structure, args) -> StringUtils
						.copyPartialMatches(args[0], WarpManager.getProvider().getWarps()),
						(sender, structure, args) -> {
							if (!(sender instanceof Player)) {
								msgUsage(sender, "other");
								return;
							}

							warp((Player) sender, args[0].toLowerCase(Locale.ROOT), true, false, sender);
						});

		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}

			warp((Player) sender, args[0].toLowerCase(Locale.ROOT), false, false, sender);
		});

		cmd.argument("-i", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}

			warp((Player) sender, args[0].toLowerCase(Locale.ROOT), true, true, sender);
		}).permission(getPerm("instant"));

		cmd.argument("-si", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}

			warp((Player) sender, args[0].toLowerCase(Locale.ROOT), false, true, sender);
		}).permission(getPerm("instant"));

		cmd = cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(Locale.ROOT), true, false, sender);
		}).permission(getPerm("other"));

		cmd.argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(Locale.ROOT), false, false, sender);
		});

		cmd.argument("-i", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(Locale.ROOT), true, true, sender);
		}).permission(getPerm("other-instant"));

		cmd.argument("-si", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(Locale.ROOT), false, true, sender);
		}).permission(getPerm("other-instant"));

		List<String> commands = getCommands();

		if (!commands.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(commands.remove(0), commands.toArray(new String[0]));
	}

	private void registerLoop() {
		LoopManager.register(LOOP_NAME, () -> (holder, player, sharedData, conditions, defaultItem) -> {
			List<ItemGUI> items = new ArrayList<>();

			for (String warpName : WarpManager.getProvider().getWarps()) {
				WarpInfo warp = WarpManager.getProvider().get(warpName);

				if (warp == null || !warp.isValid())
					continue;

				Map<String, Object> placeholders = createWarpPlaceholders(player, warpName, warp);

				ItemPackage result = findWarpItem(player, sharedData, placeholders, conditions, defaultItem);

				if (result != null)
					items.add(createWarpItem(result, player, sharedData, placeholders));
			}

			return items;
		});
	}

	private void registerActions() {
		registerWarpAction("warp", true, false);
		registerWarpAction("warp_silent", false, false);
		registerWarpAction("warp_instant", true, true);
		registerWarpAction("warp_silent_instant", false, true);
	}

	private void registerWarpAction(String actionName, boolean sendMessages, boolean instant) {
		ActionManager.register(actionName, (holder, values) -> (gui, player, sharedData, placeholders) -> {
			String warpName = resolveWarpName(player, placeholders, values);

			if (warpName == null)
				return;

			warp(player, warpName, sendMessages, instant, player);
		});
	}

	private Map<String, Object> createWarpPlaceholders(Player player, String warpName, WarpInfo warp) {
		Map<String, Object> placeholders = new HashMap<>(24);

		String permission = warp.getPermission() == null ? "" : warp.getPermission();

		boolean requiresPermission = !permission.isEmpty();
		boolean hasPermission = !requiresPermission || player.hasPermission(permission);
		boolean hasCost = warp.getCost() > 0;
		boolean hasMoney = !hasCost
				|| API.get().getEconomyHook().has(player.getName(), player.getWorld().getName(), warp.getCost());

		Material material = warp.getIcon() == null ? Material.STONE : warp.getIcon().getType();

		placeholders.put("warp", warpName);
		placeholders.put("warp_name", warpName);

		placeholders.put("cost", warp.getCost());
		placeholders.put("warp_cost", warp.getCost());

		placeholders.put("permission", permission);
		placeholders.put("warp_permission", permission);

		placeholders.put("world", warp.getPosition().getWorld().getName());
		placeholders.put("warp_world", warp.getPosition().getWorld().getName());

		placeholders.put("warp_material", material.name());

		placeholders.put("warp_valid", warp.isValid());

		placeholders.put("warp_requires_permission", requiresPermission);
		placeholders.put("warp_has_permission", hasPermission);

		placeholders.put("warp_has_cost", hasCost);
		placeholders.put("warp_has_money", hasMoney);

		placeholders.put("warp_can_use", hasPermission && hasMoney);

		placeholders.put("warp_has_cooldown", warp.getCooldown() != null);
		placeholders.put("warp_cooldown", warp.getCooldown() == null ? "" : warp.getCooldown().id());

		return placeholders;
	}

	private static ItemPackage findWarpItem(Player player, Config sharedData, Map<String, Object> placeholders,
			List<ConditionItem> conditions, ItemPackage defaultItem) {

		for (ConditionItem condition : conditions) {
			ItemPackage result = condition.test(player, sharedData, placeholders);

			if (result != null && result.getItem() != null)
				return result;
		}

		return defaultItem != null && defaultItem.getItem() != null ? defaultItem : null;
	}

	private static ItemGUI createWarpItem(ItemPackage result, Player placeholderPlayer, Config sharedData,
			Map<String, Object> placeholders) {

		return new ItemGUI(Utils.applyPlaceholders(result.getTypePlaceholder(), result.getItem(), placeholders,
				placeholderPlayer)) {

			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {
				result.runActions(gui, player, sharedData, placeholders);
			}
		};
	}

	private static String resolveWarpName(Player player, Map<String, Object> placeholders, String values) {

		String warpName = values;

		if (warpName == null || warpName.trim().isEmpty()) {
			Object value = placeholders.get("warp");

			if (value == null)
				return null;

			warpName = value.toString();
		} else
			warpName = Utils.replacePlaceholders(warpName, placeholders, player.getUniqueId());

		if (warpName == null)
			return null;

		warpName = warpName.trim();

		return warpName.isEmpty() ? null : warpName.toLowerCase(Locale.ROOT);
	}

	public WarpResult warp(Player target, String warpName, boolean sendMessages, boolean instant,
			CommandSender sender) {

		WarpInfo warp = WarpManager.getProvider().get(warpName);

		if (warp == null || !warp.isValid())
			return null;

		WarpResult result = warp.warp(target, instant);

		if (!sendMessages)
			return result;

		if (!sender.equals(target)) {
			TextRenderer targetRenderer = createWarpRenderer(target, sender, target, warpName, warp);

			TextRenderer senderRenderer = createWarpRenderer(sender, sender, target, warpName, warp);

			switch (result) {
			case FAILED_NO_MONEY:
				msg(target, "other.failed.money.target", targetRenderer);

				msg(sender, "other.failed.money.sender", senderRenderer);
				break;

			case FAILED_NO_PERMISSION:
				msg(target, "other.failed.perm.target", targetRenderer);

				msg(sender, "other.failed.perm.sender", senderRenderer);
				break;

			case SUCCESS:
				msg(target, "other.success.target", targetRenderer);

				msg(sender, "other.success.sender", senderRenderer);
				break;
			}
		} else {
			TextRenderer renderer = createWarpRenderer(target, sender, target, warpName, warp);

			switch (result) {
			case FAILED_NO_MONEY:
				msg(target, "self.failed.money", renderer);
				break;

			case FAILED_NO_PERMISSION:
				msg(target, "self.failed.perm", renderer);
				break;

			case SUCCESS:
				msg(target, "self.success", renderer);
				break;
			}
		}

		return result;
	}

	private TextRenderer createWarpRenderer(CommandSender viewer, CommandSender sender, Player target, String warpName,
			WarpInfo warp) {

		return renderer(viewer).placeholder("sender", sender.getName()).placeholder("target", target.getName())
				.placeholder("warp", warpName).placeholder("cost", warp.getCost())
				.placeholder("permission", warp.getPermission() == null ? "" : warp.getPermission());
	}
}