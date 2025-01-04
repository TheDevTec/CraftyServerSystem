package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.menubuilder.DefaultMenuBuilder;
import me.devtec.craftyserversystem.menubuilder.ItemBuilder;
import me.devtec.craftyserversystem.menubuilder.ItemResult;
import me.devtec.craftyserversystem.menubuilder.MenuBuilder;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.gui.GUI.ClickType;
import me.devtec.theapi.bukkit.gui.HolderGUI;
import me.devtec.theapi.bukkit.gui.ItemGUI;

public class CssGui extends CssCommand {

	public static Map<String, MenuBuilder> menus = new HashMap<>();
	private static final AtomicInteger MENU_ID = new AtomicInteger(1);

	@Override
	public void register() {
		if (isRegistered())
			return;

		for (String key : API.get().getConfigManager().getCustomGuis().getKeys())
			menus.put(key, new DefaultMenuBuilder(key, API.get().getConfigManager().getCustomGuis()));

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// menu
		cmd.callableArgument((sender, structure, args) -> {
			List<String> guis = new ArrayList<>();
			for (String gui : menus.keySet())
				if (sender.hasPermission(getPerm("per-id").replace("{id}", gui.toLowerCase())) || sender.hasPermission(getPerm("per-id-other").replace("{id}", gui.toLowerCase())))
					guis.add(gui);
			return guis;
		}, (sender, structure, args) -> {
			if (sender instanceof Player)
				openMenu(sender, (Player) sender, args[0], true);
			else
				msgUsage(sender, "cmd");
		})
				// silent
				.argument("-s", (sender, structure, args) -> {
					if (sender instanceof Player)
						openMenu(sender, (Player) sender, args[0], false);
					else
						msgUsage(sender, "cmd");
				})
				// other
				.parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
					for (Player player : selector(sender, args[1]))
						openMenu(sender, player, args[0], true);
				}).permission(getPerm("other"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[1]))
						openMenu(sender, player, args[0], false);
				});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void openMenu(CommandSender sender, Player target, String id, boolean sendMessage) {
		int menuId = MENU_ID.getAndIncrement();
		MenuBuilder builder = menus.get(id.toLowerCase());
		GUI gui;

		Map<ItemBuilder, ItemResult> result = new HashMap<>();
		if (!builder.getTickableItems().isEmpty()) {
			AtomicInteger atomicInteger = new AtomicInteger();
			gui = new GUI(builder.getTitle(), builder.getMenuSize()) {

				@Override
				public void onClose(Player player) {
					int i = atomicInteger.get();
					if (i != 0)
						Scheduler.cancelTask(i);

					for (ItemBuilder entry : builder.getTickableItems().keySet())
						entry.clearTick(menuId);
				}
			};
			long minSchedulerSpeed;
			atomicInteger.set(new Tasker() {

				@Override
				public void run() {
					for (Entry<ItemBuilder, List<Integer>> entry : builder.getTickableItems().entrySet())
						if (entry.getKey().canTick(menuId)) {
							entry.getKey().tick(menuId);
							ItemResult itemResult = entry.getKey().getItemResult(target);
							ItemStack itemStack = itemResult.getItem().build();
							result.put(entry.getKey(), itemResult);
							for (int slot : entry.getValue()) {
								ItemGUI item = gui.getItemGUI(slot);
								if (item.getItem().isSimilar(itemStack))
									break;
								gui.setItem(slot, item.setItem(itemStack));
							}
						}
				}
			}.runRepeating(minSchedulerSpeed = builder.getMinimumRefleshTickSpeed(), minSchedulerSpeed));
		} else
			gui = new GUI(builder.getTitle(), builder.getMenuSize());
		for (Entry<Integer, ItemBuilder> entry : builder.getItems().entrySet()) {
			entry.getValue().tick(menuId);
			ItemResult itemResult = entry.getValue().getItemResult(target);
			result.put(entry.getValue(), itemResult);
			gui.setItem(entry.getKey(), new ItemGUI(itemResult.getItem().build()) {

				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					ItemResult itemResult = result.get(entry.getValue());
					switch (itemResult.getButtonType()) {
					case CLOSE:
						itemResult.onClick(player);
						gui.close(player);
						break;
					case OPEN_MENU:
						itemResult.onClick(player);
						if (!id.equals(itemResult.getOpenMenuId()))
							openMenu(null, target, itemResult.getOpenMenuId(), false);
						else
							for (Entry<Integer, ItemBuilder> entry : builder.getItems().entrySet()) {
								entry.getValue().tick(menuId);
								itemResult = entry.getValue().getItemResult(player);
								result.put(entry.getValue(), itemResult);
								ItemGUI item = gui.getItemGUI(entry.getKey());
								ItemStack itemStack = itemResult.getItem().build();
								if (!item.getItem().isSimilar(itemStack))
									gui.setItem(entry.getKey(), item.setItem(itemStack));
							}
						break;
					case USE_ITEM:
						itemResult.onClick(player);
						if (itemResult.updateGuiAfterUse())
							for (Entry<Integer, ItemBuilder> entry : builder.getItems().entrySet()) {
								entry.getValue().tick(menuId);
								itemResult = entry.getValue().getItemResult(player);
								result.put(entry.getValue(), itemResult);
								ItemGUI item = gui.getItemGUI(entry.getKey());
								ItemStack itemStack = itemResult.getItem().build();
								if (!item.getItem().isSimilar(itemStack))
									gui.setItem(entry.getKey(), item.setItem(itemStack));
							}
						else if (itemResult.updateItemAfterUse()) {
							ItemBuilder currentBuilder = entry.getValue();
							for (Entry<Integer, ItemBuilder> entry : builder.getItems().entrySet())
								if (entry.getValue().equals(currentBuilder)) {
									entry.getValue().tick(menuId);
									itemResult = entry.getValue().getItemResult(player);
									result.put(entry.getValue(), itemResult);
									ItemGUI item = gui.getItemGUI(entry.getKey());
									ItemStack itemStack = itemResult.getItem().build();
									if (item.getItem().isSimilar(itemStack))
										break;
									gui.setItem(entry.getKey(), item.setItem(itemStack));
								}
						}
						break;
					default:
						break;
					}
				}
			});
		}
		gui.open(target);
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("id", id);
				msg(target, "other.target", placeholders);
				msg(sender, "other.sender", placeholders);
			} else
				msg(target, "self", PlaceholdersExecutor.i().add("target", target.getName()).add("id", id));
	}

}
