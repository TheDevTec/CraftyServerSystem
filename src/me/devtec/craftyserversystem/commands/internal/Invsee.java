package me.devtec.craftyserversystem.commands.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.gui.EmptyItemGUI;
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.gui.ItemGUI;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class Invsee extends CssCommand {

	private Listener listener;
	private Map<UUID, GUI> guiHandler = new HashMap<>();
	private final int HEAD_SLOT = 0;
	private final int CHESTPLATE_SLOT = 1;
	private final int LEGGINGS_SLOT = 2;
	private final int BOOTS_SLOT = 3;

	private final int CURSOR_SLOT = 10;
	private final int OFFHAND_SLOT = 11;

	private final int CRAFT_0_SLOT = 5;
	private final int CRAFT_1_SLOT = 6;
	private final int CRAFT_2_SLOT = 14;
	private final int CRAFT_3_SLOT = 15;

	private static ItemGUI EMPTY = new EmptyItemGUI(ItemMaker.of(XMaterial.BLACK_STAINED_GLASS_PANE).displayName("&7").build());

	@Override
	public String section() {
		return "invsee";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		listener = new Listener() {

			@EventHandler(ignoreCancelled = true)
			public void onClick(InventoryDragEvent e) {
				GUI gui = guiHandler.get(e.getWhoClicked().getUniqueId());
				if (gui != null) {
					for (Entry<Integer, ItemStack> items : e.getNewItems().entrySet()) {
						int slot = items.getKey() + 18;
						if (items.getKey() >= 36)
							slot -= 36;
						Inventory top = e.getWhoClicked().getOpenInventory().getTopInventory();
						if (e.getWhoClicked().getOpenInventory().getBottomInventory().equals(e.getWhoClicked().getInventory()) && top.getType() == InventoryType.CRAFTING) {
							if (items.getKey() == 0) { // result
								BukkitLoader.getNmsProvider().postToMainThread(() -> {
									gui.setItem(CRAFT_0_SLOT, new EmptyItemGUI(top.getItem(1)).setUnstealable(false));
									gui.setItem(CRAFT_1_SLOT, new EmptyItemGUI(top.getItem(2)).setUnstealable(false));
									gui.setItem(CRAFT_2_SLOT, new EmptyItemGUI(top.getItem(3)).setUnstealable(false));
									gui.setItem(CRAFT_3_SLOT, new EmptyItemGUI(top.getItem(4)).setUnstealable(false));
									gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
								});
								return;
							}
							if (items.getKey() == 1)
								slot = CRAFT_0_SLOT;
							else if (items.getKey() == 2)
								slot = CRAFT_1_SLOT;
							else if (items.getKey() == 3)
								slot = CRAFT_2_SLOT;
							else if (items.getKey() == 4)
								slot = CRAFT_3_SLOT;
						}

						if (items.getKey() == 45)
							slot = OFFHAND_SLOT;
						if (items.getKey() == 8)
							slot = BOOTS_SLOT;
						if (items.getKey() == 7)
							slot = LEGGINGS_SLOT;
						if (items.getKey() == 6)
							slot = CHESTPLATE_SLOT;
						if (items.getKey() == 5)
							slot = HEAD_SLOT;
						gui.setItem(slot, new EmptyItemGUI(items.getValue()).setUnstealable(false));
					}
					if (e.getCursor() == null)
						gui.remove(CURSOR_SLOT);
					else
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
				}
			}

			@EventHandler(ignoreCancelled = true)
			public void onClick(InventoryClickEvent e) {
				GUI gui = guiHandler.get(e.getWhoClicked().getUniqueId());
				if (gui != null) {
					int slot = e.getSlot() + 18;
					Inventory top = e.getWhoClicked().getOpenInventory().getTopInventory();
					if (e.getClickedInventory() != null && e.getClickedInventory().getType() == InventoryType.CRAFTING
							&& e.getWhoClicked().getOpenInventory().getBottomInventory().equals(e.getWhoClicked().getInventory()) && top.getType() == InventoryType.CRAFTING) {
						if (e.getSlot() == 0) { // result
							BukkitLoader.getNmsProvider().postToMainThread(() -> {
								gui.setItem(CRAFT_0_SLOT, new EmptyItemGUI(top.getItem(1)).setUnstealable(false));
								gui.setItem(CRAFT_1_SLOT, new EmptyItemGUI(top.getItem(2)).setUnstealable(false));
								gui.setItem(CRAFT_2_SLOT, new EmptyItemGUI(top.getItem(3)).setUnstealable(false));
								gui.setItem(CRAFT_3_SLOT, new EmptyItemGUI(top.getItem(4)).setUnstealable(false));
								gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
							});
							return;
						}
						if (e.getSlot() == 1)
							slot = CRAFT_0_SLOT;
						else if (e.getSlot() == 2)
							slot = CRAFT_1_SLOT;
						else if (e.getSlot() == 3)
							slot = CRAFT_2_SLOT;
						else if (e.getSlot() == 4)
							slot = CRAFT_3_SLOT;
					}

					if (e.getSlot() == 40)
						slot = OFFHAND_SLOT;
					if (e.getSlot() == 36)
						slot = BOOTS_SLOT;
					if (e.getSlot() == 37)
						slot = LEGGINGS_SLOT;
					if (e.getSlot() == 38)
						slot = CHESTPLATE_SLOT;
					if (e.getSlot() == 39)
						slot = HEAD_SLOT;

					switch (e.getAction()) {
					case COLLECT_TO_CURSOR:
					case MOVE_TO_OTHER_INVENTORY:
						BukkitLoader.getNmsProvider().postToMainThread(() -> updateinv(gui, (Player) e.getWhoClicked()));
						break;
					case SWAP_WITH_CURSOR:
						gui.setItem(slot, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCurrentItem()).setUnstealable(false));
						break;
					case DROP_ONE_CURSOR:
					case DROP_ONE_SLOT:
					case PLACE_ONE: {
						gui.setItem(slot, new EmptyItemGUI(e.getCurrentItem().getType().isAir() ? e.getCursor().asOne() : e.getCurrentItem().asQuantity(e.getCurrentItem().getAmount() + 1))
								.setUnstealable(false));
						ItemStack cursor = e.getCursor().clone().subtract(1);
						if (cursor.getAmount() == 0)
							gui.remove(CURSOR_SLOT);
						else
							gui.setItem(CURSOR_SLOT, new EmptyItemGUI(cursor).setUnstealable(false));
						break;
					}
					case PLACE_SOME:
						int remaining = e.getCurrentItem().getAmount() + e.getCursor().getAmount() - e.getCurrentItem().getMaxStackSize();
						gui.setItem(slot, new EmptyItemGUI(e.getCurrentItem().clone().add(e.getCursor().getAmount())).setUnstealable(false));
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCursor().asQuantity(remaining)).setUnstealable(false));
						break;
					case PICKUP_HALF:
						gui.setItem(slot, new EmptyItemGUI(e.getCurrentItem().asQuantity(e.getCurrentItem().getAmount() / 2)).setUnstealable(false));
						ItemStack cursor = e.getCursor().clone().subtract(e.getCursor().getAmount() / 2);
						if (cursor.getAmount() == 0)
							gui.remove(CURSOR_SLOT);
						else
							gui.setItem(CURSOR_SLOT, new EmptyItemGUI(cursor).setUnstealable(false));
						break;
					case PICKUP_ONE:
						gui.setItem(slot, new EmptyItemGUI(e.getCurrentItem().asOne()).setUnstealable(false));
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
						break;
					case PICKUP_SOME:
						gui.setItem(slot, new EmptyItemGUI(e.getCurrentItem()).setUnstealable(false));
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
						break;
					case PICKUP_ALL:
						gui.removeItem(slot);
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCurrentItem()).setUnstealable(false));
						break;
					case DROP_ALL_CURSOR:
						gui.removeItem(CURSOR_SLOT);
						break;
					case DROP_ALL_SLOT:
						gui.setItem(slot, new EmptyItemGUI(e.getCursor()).setUnstealable(false));
						gui.removeItem(CURSOR_SLOT);
						break;
					case HOTBAR_MOVE_AND_READD:
					case HOTBAR_SWAP:
						ItemStack currentItem = e.getClick() == ClickType.NUMBER_KEY ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : e.getCurrentItem();
						if (e.getCurrentItem().getType().isAir())
							gui.remove(e.getHotbarButton() + 18);
						else
							gui.setItem(e.getHotbarButton() + 18, new EmptyItemGUI(e.getCurrentItem()).setUnstealable(false));
						if (currentItem == null)
							gui.remove(slot);
						else
							gui.setItem(slot, new EmptyItemGUI(currentItem).setUnstealable(false));
						break;
					case PLACE_ALL:
						gui.setItem(slot,
								new EmptyItemGUI(e.getCursor().isSimilar(e.getCurrentItem())
										? e.getCursor().asQuantity(Math.min(e.getCursor().getAmount() + e.getCurrentItem().getAmount(), e.getCursor().getMaxStackSize()))
										: e.getCursor()).setUnstealable(false));
						gui.removeItem(CURSOR_SLOT);
						break;
					case CLONE_STACK:
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(e.getCurrentItem()).setUnstealable(false));
						break;
					default:
						break;
					}
				}
			}

			@EventHandler(ignoreCancelled = true)
			public void onDrop(PlayerDropItemEvent e) {
				GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
				if (gui != null)
					updateinv(gui, e.getPlayer());
			}

			@EventHandler
			public void onClose(InventoryCloseEvent e) {
				GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
				if (gui != null)
					new Tasker() {

						@Override
						public void run() {
							updateinv(gui, (Player) e.getPlayer());
						}
					}.runLater(1);
			}

			@EventHandler(ignoreCancelled = true)
			public void onPickup(EntityPickupItemEvent e) {
				if (e.getEntity().getType() == EntityType.PLAYER) {
					GUI gui = guiHandler.get(e.getEntity().getUniqueId());
					if (gui != null)
						new Tasker() {

							@Override
							public void run() {
								updateinv(gui, (Player) e.getEntity());
							}
						}.runLater(1);
				}
			}

			@EventHandler(ignoreCancelled = true)
			public void onSwap(PlayerSwapHandItemsEvent e) {
				GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
				if (gui != null) {
					gui.setItem(18 + e.getPlayer().getInventory().getHeldItemSlot(), new EmptyItemGUI(e.getMainHandItem()).setUnstealable(false));
					gui.setItem(OFFHAND_SLOT, new EmptyItemGUI(e.getOffHandItem()).setUnstealable(false));
				}
			}

			@EventHandler(ignoreCancelled = true)
			public void onDispense(BlockDispenseArmorEvent e) {
				if (e.getTargetEntity().getType() == EntityType.PLAYER) {
					GUI gui = guiHandler.get(e.getTargetEntity().getUniqueId());
					if (gui != null)
						new Tasker() {

							@Override
							public void run() {
								updateinv(gui, (Player) e.getTargetEntity());
							}
						}.runLater(1);
				}
			}

			@EventHandler
			public void onUse(PlayerInteractEvent e) {
				if (e.hasItem() && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
					GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
					if (gui != null)
						new Tasker() {

							@Override
							public void run() {
								updateinv(gui, e.getPlayer());
							}
						}.runLater(1);
				}
			}

			@EventHandler(ignoreCancelled = true)
			public void onEat(PlayerItemConsumeEvent e) {
				GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
				if (gui != null)
					if (e.getReplacement() == null) {
						if (e.getItem().getAmount() == 1)
							gui.remove(18 + e.getPlayer().getInventory().getHeldItemSlot());
						else
							gui.setItem(18 + e.getPlayer().getInventory().getHeldItemSlot(), new EmptyItemGUI(e.getItem().asQuantity(e.getItem().getAmount() - 1)).setUnstealable(false));
					} else
						BukkitLoader.getNmsProvider().postToMainThread(() -> updateinv(gui, e.getPlayer()));
			}

		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		cmd.selector(Selector.PLAYER, (sender, structure, args) -> {
			Player player = Bukkit.getPlayer(args[0]);
			if (player.getUniqueId().equals(sender.getUniqueId())) {
				msg(sender, "self");
				return;
			}
			invsee(sender, player);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	@Override
	public void unregister() {
		super.unregister();
		if (listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

	private void invsee(Player sender, Player target) {
		PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("target", target.getName());
		msg(sender, "open", ex);
		GUI gui = guiHandler.get(target.getUniqueId());
		if (gui == null) {
			PlayerInventory inv = target.getInventory();
			gui = new GUI("&7Inventory of &e" + target.getName(), 54) {

				GUI thisInstance = this;

				int task = new Tasker() {

					@Override
					public void run() {
						updateinv(thisInstance, target);
					}
				}.runRepeating(40, 40);

				@Override
				public void onClose(Player player) {
					if (getPlayers().isEmpty()) {
						guiHandler.remove(target.getUniqueId());
						Scheduler.cancelTask(task);
					}
				}

				@Override
				public void onMultipleIteract(Player player, Map<Integer, ItemStack> guiSlots, Map<Integer, ItemStack> playerSlots) {
					Inventory top = target.getOpenInventory().getTopInventory();
					boolean isOpenPlayerInv = target.getOpenInventory().getBottomInventory().equals(target.getInventory()) && top.getType() == InventoryType.CRAFTING;
					for (Entry<Integer, ItemStack> slot : guiSlots.entrySet())
						switch (slot.getKey()) {
						case HEAD_SLOT:
							inv.setHelmet(slot.getValue());
							break;
						case CHESTPLATE_SLOT:
							inv.setChestplate(slot.getValue());
							break;
						case LEGGINGS_SLOT:
							inv.setLeggings(slot.getValue());
							break;
						case BOOTS_SLOT:
							inv.setBoots(slot.getValue());
							break;
						case CURSOR_SLOT:
							target.setItemOnCursor(slot.getValue());
							break;
						case OFFHAND_SLOT:
							inv.setItemInOffHand(slot.getValue());
							break;
						case CRAFT_0_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(1, slot.getValue()));
							break;
						case CRAFT_1_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(2, slot.getValue()));
							break;
						case CRAFT_2_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(3, slot.getValue()));
							break;
						case CRAFT_3_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(4, slot.getValue()));
							break;
						default:
							if (slot.getKey() >= 18)
								inv.setItem(slot.getKey() - 18, slot.getValue());
						}
				}

				@Override
				public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean gui) {
					if (gui) {
						Inventory top = target.getOpenInventory().getTopInventory();
						boolean isOpenPlayerInv = target.getOpenInventory().getBottomInventory().equals(target.getInventory()) && top.getType() == InventoryType.CRAFTING;
						switch (slot) {
						case HEAD_SLOT:
							inv.setHelmet(newItem);
							break;
						case CHESTPLATE_SLOT:
							inv.setChestplate(newItem);
							break;
						case LEGGINGS_SLOT:
							inv.setLeggings(newItem);
							break;
						case BOOTS_SLOT:
							inv.setBoots(newItem);
							break;
						case CURSOR_SLOT:
							target.setItemOnCursor(newItem);
							break;
						case OFFHAND_SLOT:
							inv.setItemInOffHand(newItem);
							break;
						case CRAFT_0_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(1, newItem));
							break;
						case CRAFT_1_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(2, newItem));
							break;
						case CRAFT_2_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(3, newItem));
							break;
						case CRAFT_3_SLOT:
							if (isOpenPlayerInv)
								BukkitLoader.getNmsProvider().postToMainThread(() -> top.setItem(4, newItem));
							break;
						default:
							if (slot >= 18)
								inv.setItem(slot - 18, newItem);
						}
					}
					return false;
				}
			};
			updateinv(gui, target);
			guiHandler.put(target.getUniqueId(), gui);
			gui.setInsertable(true);
		}
		gui.open(sender);
	}

	private void updateinv(GUI gui, Player target) {
		PlayerInventory inv = target.getInventory();
		// contents
		for (int i = 0; i < inv.getContents().length; ++i)
			if (!Objects.equals(inv.getItem(i), gui.getItem(i)))
				if (inv.getItem(i) == null)
					gui.removeItem(18 + i);
				else
					gui.setItem(18 + i, new EmptyItemGUI(inv.getItem(i)).setUnstealable(false));

		Inventory top = target.getOpenInventory().getTopInventory();
		boolean isOpenPlayerInv = target.getOpenInventory().getBottomInventory().equals(target.getInventory()) && top.getType() == InventoryType.CRAFTING;
		for (int i = 0; i < 18; ++i)
			switch (i) {
			case HEAD_SLOT:
				if (!Objects.equals(inv.getHelmet(), gui.getItem(i)))
					if (isEmpty(inv.getHelmet()))
						gui.removeItem(i);
					else
						gui.setItem(i, new EmptyItemGUI(inv.getHelmet()).setUnstealable(false));
				break;
			case CHESTPLATE_SLOT:
				if (!Objects.equals(inv.getChestplate(), gui.getItem(i)))
					if (isEmpty(inv.getChestplate()))
						gui.removeItem(i);
					else
						gui.setItem(i, new EmptyItemGUI(inv.getChestplate()).setUnstealable(false));
				break;
			case LEGGINGS_SLOT:
				if (!Objects.equals(inv.getLeggings(), gui.getItem(i)))
					if (isEmpty(inv.getLeggings()))
						gui.removeItem(i);
					else
						gui.setItem(i, new EmptyItemGUI(inv.getLeggings()).setUnstealable(false));
				break;
			case BOOTS_SLOT:
				if (!Objects.equals(inv.getBoots(), gui.getItem(i)))
					if (isEmpty(inv.getBoots()))
						gui.removeItem(i);
					else
						gui.setItem(i, new EmptyItemGUI(inv.getBoots()).setUnstealable(false));
				break;
			case CURSOR_SLOT:
				if (!Objects.equals(target.getItemOnCursor(), gui.getItem(i)))
					if (isEmpty(target.getItemOnCursor()))
						gui.remove(i);
					else
						gui.setItem(i, new EmptyItemGUI(target.getItemOnCursor()).setUnstealable(false));
				break;
			case OFFHAND_SLOT:
				if (!Objects.equals(inv.getItemInOffHand(), gui.getItem(i)))
					if (isEmpty(inv.getItemInOffHand()))
						gui.remove(i);
					else
						gui.setItem(i, new EmptyItemGUI(inv.getItemInOffHand()).setUnstealable(false));
				break;
			case CRAFT_0_SLOT:
				if (isOpenPlayerInv)
					if (!Objects.equals(top.getItem(1), gui.getItem(i)))
						if (isEmpty(top.getItem(1)))
							gui.remove(i);
						else
							gui.setItem(i, new EmptyItemGUI(top.getItem(1)).setUnstealable(false));
				break;
			case CRAFT_1_SLOT:
				if (isOpenPlayerInv)
					if (!Objects.equals(top.getItem(2), gui.getItem(i)))
						if (isEmpty(top.getItem(2)))
							gui.remove(i);
						else
							gui.setItem(i, new EmptyItemGUI(top.getItem(2)).setUnstealable(false));
				break;
			case CRAFT_2_SLOT:
				if (isOpenPlayerInv)
					if (!Objects.equals(top.getItem(3), gui.getItem(i)))
						if (isEmpty(top.getItem(3)))
							gui.remove(i);
						else
							gui.setItem(i, new EmptyItemGUI(top.getItem(3)).setUnstealable(false));
				break;
			case CRAFT_3_SLOT:
				if (isOpenPlayerInv)
					if (!Objects.equals(top.getItem(4), gui.getItem(i)))
						if (isEmpty(top.getItem(4)))
							gui.remove(i);
						else
							gui.setItem(i, new EmptyItemGUI(top.getItem(4)).setUnstealable(false));
				break;
			default:
				if (!gui.isInsertable())
					gui.setItem(i, EMPTY);
				break;
			}
	}

	private static boolean isEmpty(ItemStack stack) {
		return stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0;
	}

}
