package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.API;
import me.devtec.craftyserversystem.annotations.Nullable;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.warp.WarpInfo;
import me.devtec.craftyserversystem.commands.internal.warp.WarpManager;
import me.devtec.craftyserversystem.commands.internal.warp.WarpResult;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.holder.CommandExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.gui.EmptyItemGUI;
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.gui.GUI.ClickType;
import me.devtec.theapi.bukkit.gui.HolderGUI;
import me.devtec.theapi.bukkit.gui.ItemGUI;

public class Warp extends CssCommand {

	@Nullable
	public static GUI warpMenu;

	protected static boolean requireUpdateMenu;

	public static void callMenuUpdate() {
		requireUpdateMenu = true;
	}

	public interface MenuItem {
		ItemGUI makeItem(GUI prev, GUI next, int currentPage, int totalPages);
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandExecutor<CommandSender> main;

		Config config = API.get().getConfigManager().getMain();
		if (config.getBoolean("warp.enable-menu")) {
			updateMenu(config);
			main = (sender, structure, args) -> {
				if (!(sender instanceof Player)) {
					msgUsage(sender, "other");
					return;
				}
				if (requireUpdateMenu) {
					requireUpdateMenu = false;
					updateMenu(config);
				}
				warpMenu.open((Player) sender);
			};
		} else
			main = (sender, structure, args) -> {
				if (!(sender instanceof Player)) {
					msgUsage(sender, "other");
					return;
				}
				msgUsage(sender, "cmd");
			};

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, main).permission(getPerm("cmd"))
				.callableArgument((sender, structure, args) -> StringUtils.copyPartialMatches(args[0], WarpManager.getProvider().getWarps()), (sender, structure, args) -> {
					if (!(sender instanceof Player)) {
						msgUsage(sender, "other");
						return;
					}
					warp((Player) sender, args[0].toLowerCase(), true, false, sender);
				});
		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), false, false, sender);
		});
		// instant
		cmd.argument("-i", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), true, true, sender);
		}).permission(getPerm("instant"));

		// silent & instant
		cmd.argument("-si", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "other");
				return;
			}
			warp((Player) sender, args[0].toLowerCase(), false, true, sender);
		}).permission(getPerm("instant"));

		// other
		cmd = cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(), true, false, sender);
		}).permission(getPerm("other"));

		// silent
		cmd.argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(), false, false, sender);
		});
		// instant
		cmd.argument("-i", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(), true, true, sender);
		}).permission(getPerm("other-instant"));

		// silent & instant
		cmd.argument("-si", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				warp(player, args[0].toLowerCase(), false, true, sender);
		}).permission(getPerm("other-instant"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void updateMenu(Config config) {
		Map<Character, ItemGUI> items = new HashMap<>();
		Map<Character, MenuItem> menuItems = new HashMap<>();

		char warpChar = ' ';
		int totalSpacesPerPage = countWarpSpaces(config.getStringList("warp.slots"), warpChar);
		int totalPages = 1 + WarpManager.getProvider().getWarps().size() / totalSpacesPerPage;

		for (String key : config.getKeys("warp.items")) {
			String action;
			if ((action = config.getString("warp.items." + key + ".action")) != null)
				switch (action.toUpperCase()) {
				case "CLOSE":
					menuItems.put(key.charAt(0), (prev, next, currentPage, totalPages1) -> {
						ItemMaker maker = ItemMaker.loadMakerFromConfig(config, "warp.items." + key);
						PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("page", currentPage).add("totalPages", totalPages1).add("nextPage", currentPage + 1).add("previousPage",
								currentPage - 1);
						maker.displayName(ex.apply(maker.getDisplayName()));
						maker.lore(ex.apply(maker.getLore()));
						return currentPage == totalPages1 ? new EmptyItemGUI(maker.build()) : new ItemGUI(maker.build()) {
							@Override
							public void onClick(Player s, HolderGUI menu, ClickType arg2) {
								menu.close(s);
							}
						};
					});
					break;
				case "NEXT":
				case "NEXT_PAGE":
					menuItems.put(key.charAt(0), (prev, next, currentPage, totalPages1) -> {
						ItemMaker maker = ItemMaker.loadMakerFromConfig(config, "warp.items." + key + "." + (currentPage == totalPages1 ? "empty" : "available"));
						PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("page", currentPage).add("totalPages", totalPages1).add("nextPage", currentPage + 1).add("previousPage",
								currentPage - 1);
						maker.displayName(ex.apply(maker.getDisplayName()));
						maker.lore(ex.apply(maker.getLore()));
						return currentPage == totalPages1 ? new EmptyItemGUI(maker.build()) : new ItemGUI(maker.build()) {
							@Override
							public void onClick(Player s, HolderGUI menu, ClickType arg2) {
								next.open(s);
							}
						};
					});
					break;
				case "PREVIOUS":
				case "PREVIOUS_PAGE":
					menuItems.put(key.charAt(0), (prev, next, currentPage, totalPages1) -> {
						ItemMaker maker = ItemMaker.loadMakerFromConfig(config, "warp.items." + key + "." + (currentPage == 1 ? "empty" : "available"));
						PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("page", currentPage).add("totalPages", totalPages1).add("nextPage", currentPage + 1).add("previousPage",
								currentPage - 1);
						maker.displayName(ex.apply(maker.getDisplayName()));
						maker.lore(ex.apply(maker.getLore()));
						return currentPage == 1 ? new EmptyItemGUI(maker.build()) : new ItemGUI(maker.build()) {
							@Override
							public void onClick(Player s, HolderGUI menu, ClickType arg2) {
								prev.open(s);
							}
						};
					});
					break;
				case "WARP":
				case "PLACE":
					warpChar = key.charAt(0);
					break;
				}
			else
				items.put(key.charAt(0), new EmptyItemGUI(ItemMaker.loadMakerFromConfig(config, "warp.items." + key).build())); // Implement placeholders?
		}

		List<Integer> places = new ArrayList<>();
		Map<Integer, MenuItem> menuItemPlaces = new HashMap<>();
		boolean initLists = true;

		int menuSize = Math.min(config.getStringList("warp.slots").size() * 9, 54);
		PlaceholdersExecutor menuPlaceholders = PlaceholdersExecutor.i().add("page", 1).add("totalPages", totalPages);

		warpMenu = new GUI(menuPlaceholders.apply(config.getString("warp.title")), menuSize);
		List<String> warps = new ArrayList<>(WarpManager.getProvider().getWarps());

		int currentPage = 1;
		GUI prevBuild = warpMenu;
		GUI currentBuild = warpMenu;

		while (!warps.isEmpty()) {
			int pos = 0;
			for (String slot : config.getStringList("warp.slots"))
				for (int i = 0; i < slot.length(); ++i) {
					char c = slot.charAt(i);
					if (c == warpChar) {
						if (initLists)
							places.add(pos);
						continue;
					}
					ItemGUI menuItem = items.get(c);
					if (menuItem == null) {
						MenuItem actionitem;
						if ((actionitem = menuItems.get(c)) != null)
							if (initLists)
								menuItemPlaces.put(pos, actionitem);
						continue;
					}
					currentBuild.setItem(pos, menuItem);
					++pos;
				}
			initLists = false;

			for (int i = 0; i < places.size() && !warps.isEmpty(); ++i) {
				String warp = warps.remove(0); // pull
				WarpInfo warpInfo = WarpManager.getProvider().get(warp);
				if (!warpInfo.isValid())
					continue; // Skip invalid warp
				int warpPlace = places.get(i);
				currentBuild.setItem(warpPlace,
						new ItemGUI(ItemMaker.of(warpInfo.getIcon())
								.lore(PlaceholdersExecutor.i().add("warp", warp).add("cost", warpInfo.getCost()).add("permission", warpInfo.getPermission() + "")
										.apply(config.getStringList("warp.warp_lore." + (warpInfo.getCost() > 0 && warpInfo.getPermission() != null ? "withBoth"
												: warpInfo.getCost() > 0 ? "withCost" : warpInfo.getPermission() != null ? "withPerm" : "clear"))))
								.build()) {
							@Override
							public void onClick(Player s, HolderGUI menu, ClickType arg2) {
								warp(s, warp, true, false, s);
							}
						});
			}
			if (!warps.isEmpty()) { // next page
				++currentPage;
				prevBuild = currentBuild;
				currentBuild = new GUI(menuPlaceholders.add("page", currentPage + 1).apply(config.getString("warp.title")), menuSize);
				for (Entry<Integer, MenuItem> entry : menuItemPlaces.entrySet())
					prevBuild.setItem(entry.getKey(), entry.getValue().makeItem(prevBuild, currentBuild, currentPage, totalPages));
				++currentPage;

			} else
				for (Entry<Integer, MenuItem> entry : menuItemPlaces.entrySet())
					prevBuild.setItem(entry.getKey(), entry.getValue().makeItem(prevBuild, currentBuild, currentPage, totalPages));
		}
	}

	private int countWarpSpaces(List<String> slots, char warpChar) {
		int totalSpaces = 0;
		for (String slot : slots)
			for (int i = 0; i < slot.length(); ++i)
				if (slot.charAt(i) == warpChar)
					++totalSpaces;
		return totalSpaces;
	}

	private void warp(Player target, String warpName, boolean sendMessage, boolean instant, CommandSender sender) {
		WarpInfo warp = WarpManager.getProvider().get(warpName);
		WarpResult result = warp.warp(target, instant);
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("warp", warpName).add("cost", warp.getCost())
						.add("permission", warp.getPermission() + "");
				switch (result) {
				case FAILED_NO_MONEY:
					msg(target, "other.failed.money.target", placeholders);
					msg(sender, "other.failed.money.sender", placeholders);
					break;
				case FAILED_NO_PERMISSION:
					msg(target, "other.failed.perm.target", placeholders);
					msg(sender, "other.failed.perm.sender", placeholders);
					break;
				case SUCCESS:
					msg(target, "other.success.target", placeholders);
					msg(sender, "other.success.sender", placeholders);
					break;
				}
			} else {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("warp", warpName).add("cost", warp.getCost()).add("permission",
						warp.getPermission() + "");
				switch (result) {
				case FAILED_NO_MONEY:
					msg(target, "self.failed.money", placeholders);
					break;
				case FAILED_NO_PERMISSION:
					msg(target, "self.failed.perm", placeholders);
					break;
				case SUCCESS:
					msg(target, "self.success", placeholders);
					break;
				}
			}
	}

}
