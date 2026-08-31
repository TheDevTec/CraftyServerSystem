package me.devtec.craftyserversystem.commands.internal;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.nbt.NbtCompound;
import me.devtec.craftyserversystem.nbt.NbtList;
import me.devtec.craftyserversystem.nbt.NbtReader;
import me.devtec.craftyserversystem.nbt.NbtTag;
import me.devtec.craftyserversystem.nbt.NbtWriter;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.Ref;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.json.Json;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.utility.OfflineCache.Query;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.gui.EmptyItemGUI;
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.gui.ItemGUI;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class Invsee extends CssCommand {

	private Listener listener;
	private Listener swapItemListener;
	private Map<UUID, GUI> guiHandler = new HashMap<>();
	private final Map<UUID, OfflineInventory> offlineInventories = new HashMap<>();
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
	public void register() {
		if (isRegistered())
			return;

		if (Ref.isNewerThan(9) && Ref.isOlderThan(13)) // 1.10 - 1.12
			swapItemListener = new Listener() {

			@EventHandler(ignoreCancelled = true)
			public void onSwap(PlayerSwapHandItemsEvent e) {
				GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
				if (gui != null) {
					gui.setItem(18 + e.getPlayer().getInventory().getHeldItemSlot(), new EmptyItemGUI(e.getMainHandItem()).setUnstealable(false));
					gui.setItem(OFFHAND_SLOT, new EmptyItemGUI(e.getOffHandItem()).setUnstealable(false));
				}
			}
		};
		else if (Ref.isNewerThan(13)) // 1.14+
			swapItemListener = new Listener() {

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
		};

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
						gui.setItem(slot, new EmptyItemGUI(e.getCurrentItem().getType() == Material.AIR ? asOne(e.getCursor()) : asQuantity(e.getCurrentItem(), e.getCurrentItem().getAmount() + 1))
								.setUnstealable(false));
						ItemStack cursor = subtract(e.getCursor(), 1);
						if (cursor.getAmount() == 0)
							gui.remove(CURSOR_SLOT);
						else
							gui.setItem(CURSOR_SLOT, new EmptyItemGUI(cursor).setUnstealable(false));
						break;
					}
					case PLACE_SOME:
						int remaining = e.getCurrentItem().getAmount() + e.getCursor().getAmount() - e.getCurrentItem().getMaxStackSize();
						gui.setItem(slot, new EmptyItemGUI(add(e.getCurrentItem(), e.getCursor().getAmount())).setUnstealable(false));
						gui.setItem(CURSOR_SLOT, new EmptyItemGUI(asQuantity(e.getCursor(), remaining)).setUnstealable(false));
						break;
					case PICKUP_HALF:
						gui.setItem(slot, new EmptyItemGUI(asQuantity(e.getCurrentItem(), e.getCurrentItem().getAmount() / 2)).setUnstealable(false));
						ItemStack cursor = subtract(e.getCursor(), e.getCursor().getAmount() / 2);
						if (cursor.getAmount() == 0)
							gui.remove(CURSOR_SLOT);
						else
							gui.setItem(CURSOR_SLOT, new EmptyItemGUI(cursor).setUnstealable(false));
						break;
					case PICKUP_ONE:
						gui.setItem(slot, new EmptyItemGUI(asOne(e.getCurrentItem())).setUnstealable(false));
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
						if (e.getCurrentItem().getType() == Material.AIR)
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
										? asQuantity(e.getCursor(), Math.min(e.getCursor().getAmount() + e.getCurrentItem().getAmount(), e.getCursor().getMaxStackSize()))
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

			private ItemStack asQuantity(ItemStack origin, int amount) {
				ItemStack stack = origin.clone();
				stack.setAmount(amount);
				return stack;
			}

			private ItemStack add(ItemStack origin, int amount) {
				ItemStack stack = origin.clone();
				stack.setAmount(stack.getAmount() + amount);
				return stack;
			}

			private ItemStack subtract(ItemStack origin, int amount) {
				ItemStack stack = origin.clone();
				stack.setAmount(stack.getAmount() - amount);
				return stack;
			}

			private ItemStack asOne(ItemStack origin) {
				ItemStack stack = origin.clone();
				stack.setAmount(1);
				return stack;
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
			public void onPickup(PlayerPickupItemEvent e) {
				GUI gui = guiHandler.get(e.getPlayer().getUniqueId());
				if (gui != null)
					new Tasker() {

					@Override
					public void run() {
						updateinv(gui, e.getPlayer());
					}
				}.runLater(1);
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
					BukkitLoader.getNmsProvider().postToMainThread(() -> updateinv(gui, e.getPlayer()));
			}

			@EventHandler
			public void onJoin(PlayerJoinEvent e) {
				switchToLive(e.getPlayer());
			}

			@EventHandler
			public void onQuit(PlayerQuitEvent e) {
				UUID target = e.getPlayer().getUniqueId();
				if (!guiHandler.containsKey(target))
					return;

				new Tasker() {
					@Override
					public void run() {
						if (Bukkit.getPlayer(target) != null || !guiHandler.containsKey(target))
							return;
						try {
							OfflineInventory offline = loadOfflineInventory(target);
							if (offline != null)
								offlineInventories.put(target, offline);
						} catch (IOException err) {
							err.printStackTrace();
						}
					}
				}.runLater(2);
			}

		};
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());
		if (swapItemListener != null)
			Bukkit.getPluginManager().registerEvents(swapItemListener, Loader.getPlugin());

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		cmd.callableArgument((player, structure, args) -> {
			List<String> match = new ArrayList<>();
			if(args[0].isEmpty()) {
				for(Query query : API.offlineCache().getQueries())
					match.add(query.getName());
				return match;
			}
			String arg = args[0].toLowerCase();
			for(Query query : API.offlineCache().getQueries())
				if(query.getName().toLowerCase().contains(arg))
					match.add(query.getName());
			return match;
		}, (sender, structure, args) -> {
			UUID found = API.offlineCache().lookupId(args[0]);
			if(found==null) {
				msg(sender, "no-match");
				return;
			}
			if (found.equals(sender.getUniqueId())) {
				msg(sender, "self");
				return;
			}
			invsee(sender, found);
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
			if (swapItemListener != null)
				HandlerList.unregisterAll(swapItemListener);
			swapItemListener = null;
		}
	}


	private static class OfflineInventory {
		private final Path file;
		private final NbtCompound root;

		private OfflineInventory(Path file, NbtCompound root) {
			this.file = file;
			this.root = root;
		}
	}

	public void invsee(Player sender, UUID target) {
		Player online = Bukkit.getPlayer(target);
		if (online != null) {
			invsee(sender, online);
			return;
		}

		String name = API.offlineCache().lookupNameById(target);
		if (name == null)
			name = target.toString();

		PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("target", name);
		GUI gui = guiHandler.get(target);

		if (gui == null)
			try {
				OfflineInventory offline = loadOfflineInventory(target);
				if (offline == null)
					return;

				Map<Integer, ItemStack> inventory = readOfflineInventory(offline.root);
				gui = createInvseeGui(target, name);
				offlineInventories.put(target, offline);
				guiHandler.put(target, gui);
				updateinv(gui, inventory);
				gui.setInsertable(true);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

		msg(sender, "open", ex);
		gui.open(sender);
	}

	public void invsee(Player sender, Player target) {
		UUID targetId = target.getUniqueId();
		PlaceholdersExecutor ex = PlaceholdersExecutor.i().add("target", target.getName());
		GUI gui = guiHandler.get(targetId);

		if (gui == null) {
			gui = createInvseeGui(targetId, target.getName());
			guiHandler.put(targetId, gui);
			updateinv(gui, target);
			gui.setInsertable(true);
		} else {
			OfflineInventory offline = offlineInventories.remove(targetId);
			if (offline != null)
				syncGuiToPlayer(gui, target);
			updateinv(gui, target);
		}

		msg(sender, "open", ex);
		gui.open(sender);
	}

	private GUI createInvseeGui(UUID targetId, String name) {
		return new GUI("&7Inventory of &e" + name, 54) {
			GUI thisInstance = this;

			int task = new Tasker() {
				@Override
				public void run() {
					Player target = Bukkit.getPlayer(targetId);
					if (target == null)
						return;

					OfflineInventory offline = offlineInventories.remove(targetId);
					if (offline != null)
						syncGuiToPlayer(thisInstance, target);

					updateinv(thisInstance, target);
				}
			}.runRepeating(40, 40);

			@Override
			public void onClose(Player player) {
				if (!getPlayers().isEmpty())
					return;

				guiHandler.remove(targetId);
				Scheduler.cancelTask(task);

				Player target = Bukkit.getPlayer(targetId);
				if (target != null) {
					offlineInventories.remove(targetId);
					return;
				}

				OfflineInventory offline = offlineInventories.remove(targetId);
				if (offline != null) {
					try {
						saveOfflineInventory(thisInstance, offline);
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;
				}

				new Tasker() {
					@Override
					public void run() {
						if (Bukkit.getPlayer(targetId) != null)
							return;
						try {
							OfflineInventory loaded = loadOfflineInventory(targetId);
							if (loaded != null)
								saveOfflineInventory(thisInstance, loaded);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}.runLater(2);
			}

			@Override
			public void onMultipleIteract(Player player, Map<Integer, ItemStack> guiSlots, Map<Integer, ItemStack> playerSlots) {
				Player target = Bukkit.getPlayer(targetId);
				if (target == null)
					return;

				for (Entry<Integer, ItemStack> slot : guiSlots.entrySet())
					applyLiveSlot(target, slot.getKey(), slot.getValue());
			}

			@Override
			public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean gui) {
				if (!gui)
					return false;

				Player target = Bukkit.getPlayer(targetId);
				if (target != null)
					applyLiveSlot(target, slot, newItem);
				return false;
			}
		};
	}

	private void applyLiveSlot(Player target, int slot, ItemStack item) {
		PlayerInventory inv = target.getInventory();

		switch (slot) {
		case HEAD_SLOT:
			inv.setHelmet(item);
			break;
		case CHESTPLATE_SLOT:
			inv.setChestplate(item);
			break;
		case LEGGINGS_SLOT:
			inv.setLeggings(item);
			break;
		case BOOTS_SLOT:
			inv.setBoots(item);
			break;
		case CURSOR_SLOT:
			target.setItemOnCursor(item);
			break;
		case OFFHAND_SLOT:
			if (Ref.isNewerThan(8))
				inv.setItemInOffHand(item);
			break;
		case CRAFT_0_SLOT:
		case CRAFT_1_SLOT:
		case CRAFT_2_SLOT:
		case CRAFT_3_SLOT: {
			Inventory top = target.getOpenInventory().getTopInventory();
			boolean crafting = target.getOpenInventory().getBottomInventory().equals(target.getInventory()) && top.getType() == InventoryType.CRAFTING;
			if (!crafting)
				break;

			int craftSlot;
			switch (slot) {
			case CRAFT_0_SLOT:
				craftSlot = 1;
				break;
			case CRAFT_1_SLOT:
				craftSlot = 2;
				break;
			case CRAFT_2_SLOT:
				craftSlot = 3;
				break;
			default:
				craftSlot = 4;
				break;
			}
			top.setItem(craftSlot, item);
			break;
		}
		default:
			if (slot >= 18 && slot < 54)
				inv.setItem(slot - 18, item);
			break;
		}
	}

	private void switchToLive(Player target) {
		UUID targetId = target.getUniqueId();
		GUI gui = guiHandler.get(targetId);
		if (gui == null)
			return;

		OfflineInventory offline = offlineInventories.remove(targetId);
		if (offline == null)
			return;

		syncGuiToPlayer(gui, target);
		updateinv(gui, target);
	}

	private void syncGuiToPlayer(GUI gui, Player target) {
		PlayerInventory inv = target.getInventory();

		for (int i = 0; i < 36; ++i)
			inv.setItem(i, normalize(gui.getItem(18 + i)));

		inv.setHelmet(normalize(gui.getItem(HEAD_SLOT)));
		inv.setChestplate(normalize(gui.getItem(CHESTPLATE_SLOT)));
		inv.setLeggings(normalize(gui.getItem(LEGGINGS_SLOT)));
		inv.setBoots(normalize(gui.getItem(BOOTS_SLOT)));

		if (Ref.isNewerThan(8))
			inv.setItemInOffHand(normalize(gui.getItem(OFFHAND_SLOT)));

		Inventory top = target.getOpenInventory().getTopInventory();
		boolean crafting = target.getOpenInventory().getBottomInventory().equals(target.getInventory()) && top.getType() == InventoryType.CRAFTING;
		if (crafting) {
			top.setItem(1, normalize(gui.getItem(CRAFT_0_SLOT)));
			top.setItem(2, normalize(gui.getItem(CRAFT_1_SLOT)));
			top.setItem(3, normalize(gui.getItem(CRAFT_2_SLOT)));
			top.setItem(4, normalize(gui.getItem(CRAFT_3_SLOT)));
		}

		target.updateInventory();
	}

	private void updateinv(GUI gui, Player target) {
		PlayerInventory inv = target.getInventory();

		for (int i = 0; i < 36; ++i)
			setGuiItem(gui, 18 + i, inv.getItem(i));

		Inventory top = target.getOpenInventory().getTopInventory();
		boolean isOpenPlayerInv = target.getOpenInventory().getBottomInventory().equals(target.getInventory()) && top.getType() == InventoryType.CRAFTING;

		setGuiItem(gui, HEAD_SLOT, inv.getHelmet());
		setGuiItem(gui, CHESTPLATE_SLOT, inv.getChestplate());
		setGuiItem(gui, LEGGINGS_SLOT, inv.getLeggings());
		setGuiItem(gui, BOOTS_SLOT, inv.getBoots());
		setGuiItem(gui, CURSOR_SLOT, target.getItemOnCursor());

		if (Ref.isNewerThan(8))
			setGuiItem(gui, OFFHAND_SLOT, inv.getItemInOffHand());
		else
			gui.setItem(OFFHAND_SLOT, EMPTY);

		if (isOpenPlayerInv) {
			setGuiItem(gui, CRAFT_0_SLOT, top.getItem(1));
			setGuiItem(gui, CRAFT_1_SLOT, top.getItem(2));
			setGuiItem(gui, CRAFT_2_SLOT, top.getItem(3));
			setGuiItem(gui, CRAFT_3_SLOT, top.getItem(4));
		} else {
			gui.setItem(CRAFT_0_SLOT, EMPTY);
			gui.setItem(CRAFT_1_SLOT, EMPTY);
			gui.setItem(CRAFT_2_SLOT, EMPTY);
			gui.setItem(CRAFT_3_SLOT, EMPTY);
		}

		fillUnused(gui);
	}

	private void updateinv(GUI gui, Map<Integer, ItemStack> inventory) {
		for (int i = 0; i < 36; ++i)
			setGuiItem(gui, 18 + i, inventory.get(i));

		setGuiItem(gui, BOOTS_SLOT, inventory.get(100));
		setGuiItem(gui, LEGGINGS_SLOT, inventory.get(101));
		setGuiItem(gui, CHESTPLATE_SLOT, inventory.get(102));
		setGuiItem(gui, HEAD_SLOT, inventory.get(103));

		if (Ref.isNewerThan(8))
			setGuiItem(gui, OFFHAND_SLOT, inventory.get(-106));
		else
			gui.setItem(OFFHAND_SLOT, EMPTY);

		gui.setItem(CURSOR_SLOT, EMPTY);
		gui.setItem(CRAFT_0_SLOT, EMPTY);
		gui.setItem(CRAFT_1_SLOT, EMPTY);
		gui.setItem(CRAFT_2_SLOT, EMPTY);
		gui.setItem(CRAFT_3_SLOT, EMPTY);
		fillUnused(gui);
	}

	private void fillUnused(GUI gui) {
		for (int i = 0; i < 18; ++i)
			switch (i) {
			case HEAD_SLOT:
			case CHESTPLATE_SLOT:
			case LEGGINGS_SLOT:
			case BOOTS_SLOT:
			case CURSOR_SLOT:
			case OFFHAND_SLOT:
			case CRAFT_0_SLOT:
			case CRAFT_1_SLOT:
			case CRAFT_2_SLOT:
			case CRAFT_3_SLOT:
				break;
			default:
				gui.setItem(i, EMPTY);
				break;
			}
	}

	private OfflineInventory loadOfflineInventory(UUID target) throws IOException {

		Path file = (Ref.isNewerThan(25) ? Path.of("world", "players", "data") : Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("playerdata")).resolve(target + ".dat");
		if (!Files.exists(file))
			return null;

		NbtTag tag = NbtReader.read(file);
		if (!(tag instanceof NbtCompound))
			throw new IOException("Player NBT root isn't TAG_Compound: " + target);
		return new OfflineInventory(file, (NbtCompound) tag);
	}

	private Map<Integer, ItemStack> readOfflineInventory(NbtCompound root) {
		Map<Integer, ItemStack> result = new HashMap<>();
		for (Object object : root.getListValue("Inventory")) {
			if (!(object instanceof NbtCompound))
				continue;

			NbtCompound item = (NbtCompound) object;
			int slot = item.getByte("Slot", (byte) -1);
			try {
				ItemStack stack = deserializeOfflineItem(item);
				if (!isEmpty(stack))
					result.put(slot, stack);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private ItemStack deserializeOfflineItem(NbtCompound item) {
		Material material = findMaterial(item.getString("id", ""));
		if (material == null || material == Material.AIR)
			return null;

		ItemMaker maker = ItemMaker.of(material).amount(Math.max(1, getNumber(item.getAny("count"), getNumber(item.getAny("Count"), 1))));
		NbtCompound components = item.getCompound("components");
		NbtCompound legacyTag = item.getCompound("tag");
		if (components != null)
			applyComponents(maker, components);
		if (legacyTag != null)
			applyLegacyTag(maker, legacyTag);
		return maker.build();
	}

	private void applyComponents(ItemMaker maker, NbtCompound components) {
		Object name = components.getAny("minecraft:custom_name");
		if (name == null)
			name = components.getAny("minecraft:item_name");
		if (name != null)
			maker.rawDisplayName(componentText(name));
		applyLore(maker, components.getAny("minecraft:lore"));
		applyEnchantments(maker, components.getAny("minecraft:enchantments"));
		applyEnchantments(maker, components.getAny("minecraft:stored_enchantments"));
		maker.unbreakable(components.contains("minecraft:unbreakable"));
		maker.damage(getNumber(components.getAny("minecraft:damage"), 0));
	}

	private void applyLegacyTag(ItemMaker maker, NbtCompound tag) {
		NbtCompound display = tag.getCompound("display");
		if (display != null) {
			if (display.contains("Name"))
				maker.rawDisplayName(componentText(display.getAny("Name")));
			applyLore(maker, display.getAny("Lore"));
		}
		applyEnchantments(maker, tag.getAny("Enchantments"));
		applyEnchantments(maker, tag.getAny("StoredEnchantments"));
		maker.unbreakable(isEnabled(tag.getAny("Unbreakable")));
		maker.damage(getNumber(tag.getAny("Damage"), 0));
	}

	private void applyLore(ItemMaker maker, Object value) {
		if (!(value instanceof NbtList))
			return;
		List<String> lore = new ArrayList<>();
		for (Object line : ((NbtList) value).values())
			lore.add(componentText(line));
		maker.rawLore(lore);
	}

	private void applyEnchantments(ItemMaker maker, Object value) {
		if (value instanceof NbtCompound) {
			NbtCompound compound = (NbtCompound) value;
			NbtCompound levels = compound.getCompound("levels");
			for (Entry<String, Object> entry : (levels == null ? compound : levels).values().entrySet())
				applyEnchantment(maker, entry.getKey(), getNumber(entry.getValue(), 1));
			return;
		}
		if (!(value instanceof NbtList))
			return;
		for (Object entry : ((NbtList) value).values())
			if (entry instanceof NbtCompound) {
				NbtCompound enchantment = (NbtCompound) entry;
				applyEnchantment(maker, enchantment.getString("id", ""), getNumber(enchantment.getAny("lvl"), 1));
			}
	}

	private void applyEnchantment(ItemMaker maker, String id, int level) {
		Enchantment enchantment = findEnchantment(id);
		if (enchantment != null)
			maker.enchant(enchantment, level);
	}

	private Material findMaterial(String id) {
		if (id == null || id.isEmpty())
			return null;
		Material material = Material.matchMaterial(id);
		if (material != null)
			return material;
		String key = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		return Material.matchMaterial(key.toUpperCase(java.util.Locale.ROOT));
	}

	private Enchantment findEnchantment(String id) {
		if (id == null || id.isEmpty())
			return null;
		NamespacedKey key = NamespacedKey.fromString(id);
		if (key == null)
			key = NamespacedKey.minecraft(id);
		return Enchantment.getByKey(key);
	}

	private int getNumber(Object value, int def) {
		return value instanceof Number ? ((Number) value).intValue() : def;
	}

	private boolean isEnabled(Object value) {
		return value instanceof Boolean ? (Boolean) value : getNumber(value, 0) != 0;
	}

	@SuppressWarnings("unchecked")
	private String componentText(Object component) {
		if (component == null)
			return "";
		if (component instanceof String) {
			String text = (String) component;
			if ((text.startsWith("{") || text.startsWith("[")) && (text.endsWith("}") || text.endsWith("]")))
				try {
					return componentText(Json.reader().simpleRead(text));
				} catch (Exception ignored) {
				}
			return text;
		}
		if (component instanceof NbtList) {
			StringBuilder output = new StringBuilder();
			for (Object entry : ((NbtList) component).values())
				output.append(componentText(entry));
			return output.toString();
		}
		Map<String, Object> values;
		if (component instanceof NbtCompound)
			values = ((NbtCompound) component).values();
		else if (component instanceof Map)
			values = (Map<String, Object>) component;
		else
			return String.valueOf(component);

		StringBuilder output = new StringBuilder(styleCodes(values));
		Object text = values.get("text");
		if (text != null)
			output.append(text);
		else if (values.get("translate") != null)
			output.append(values.get("translate"));
		Object extra = values.get("extra");
		if (extra != null)
			output.append(componentText(extra));
		return output.toString();
	}

	private String styleCodes(Map<String, Object> values) {
		StringBuilder codes = new StringBuilder(ChatColor.RESET.toString());
		Object color = values.get("color");
		if (color != null)
			try {
				codes.append(ChatColor.valueOf(String.valueOf(color).toUpperCase(java.util.Locale.ROOT)));
			} catch (IllegalArgumentException ignored) {
			}
		if (isEnabled(values.get("bold")))
			codes.append(ChatColor.BOLD);
		if (isEnabled(values.get("italic")))
			codes.append(ChatColor.ITALIC);
		if (isEnabled(values.get("underlined")))
			codes.append(ChatColor.UNDERLINE);
		if (isEnabled(values.get("strikethrough")))
			codes.append(ChatColor.STRIKETHROUGH);
		if (isEnabled(values.get("obfuscated")))
			codes.append(ChatColor.MAGIC);
		return codes.toString();
	}

	/* Legacy NMS item deserializer removed. */
	/*
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ItemStack deserializeItem(NbtCompound item, Path temp) throws Exception {
		Object unsafe = Bukkit.getUnsafe();
		Method deserialize = findMethod(unsafe.getClass(), "deserializeItem", byte[].class);

		if (deserialize != null) {
			Map copy = new java.util.LinkedHashMap(item.values());
			Method dataVersion = findMethod(unsafe.getClass(), "getDataVersion");
			if (dataVersion != null)
				copy.put("DataVersion", ((Number) dataVersion.invoke(unsafe)).intValue());

			NbtCompound serialized = new NbtCompound("", copy);
			NbtWriter.write(temp, serialized, true);
			return (ItemStack) deserialize.invoke(unsafe, Files.readAllBytes(temp));
		}

		Object compound = toNmsCompound(item);
		Object nmsItem = createLegacyNmsItem(compound);
		return nmsItem == null ? null : BukkitLoader.getNmsProvider().asBukkitItem(nmsItem);
	}

	private Object toNmsCompound(NbtCompound source) throws Exception {
		Object compound = BukkitLoader.getNmsProvider().parseNBT("{}");
		for (Entry<String, Object> entry : source.values().entrySet())
			setNmsValue(compound, entry.getKey(), entry.getValue());
		return compound;
	}

	private void setNmsValue(Object compound, String key, Object value) throws Exception {
		if (value instanceof NbtValue)
			value = ((NbtValue) value).value();
		if (value == null)
			return;

		if (value instanceof NbtCompound) {
			BukkitLoader.getNmsProvider().setNBTBase(compound, key, toNmsCompound((NbtCompound) value));
			return;
		}
		if (value instanceof NbtList) {
			BukkitLoader.getNmsProvider().setNBTBase(compound, key, toNmsList((NbtList) value));
			return;
		}
		if (value instanceof String || value instanceof Character) {
			BukkitLoader.getNmsProvider().setString(compound, key, String.valueOf(value));
			return;
		}
		if (value instanceof Boolean || value instanceof Byte) {
			BukkitLoader.getNmsProvider().setByte(compound, key,
					value instanceof Boolean ? (byte) ((Boolean) value ? 1 : 0) : (Byte) value);
			return;
		}
		if (value instanceof Short) {
			BukkitLoader.getNmsProvider().setShort(compound, key, (Short) value);
			return;
		}
		if (value instanceof Integer) {
			BukkitLoader.getNmsProvider().setInteger(compound, key, (Integer) value);
			return;
		}
		if (value instanceof Long) {
			BukkitLoader.getNmsProvider().setLong(compound, key, (Long) value);
			return;
		}
		if (value instanceof Float) {
			BukkitLoader.getNmsProvider().setFloat(compound, key, (Float) value);
			return;
		}
		if (value instanceof Double) {
			BukkitLoader.getNmsProvider().setDouble(compound, key, (Double) value);
			return;
		}
		if (value instanceof byte[]) {
			BukkitLoader.getNmsProvider().setByteArray(compound, key, (byte[]) value);
			return;
		}
		if (value instanceof int[]) {
			BukkitLoader.getNmsProvider().setIntArray(compound, key, (int[]) value);
			return;
		}
		if (!(value instanceof long[]))
			throw new IOException("Unsupported NBT value: " + value.getClass().getName());
		BukkitLoader.getNmsProvider().setNBTBase(compound, key, createNmsLongArray((long[]) value));
	}

	private Object createNmsLongArray(long[] values) {
		StringBuilder snbt = new StringBuilder("{value:[L;");
		for (int i = 0; i < values.length; ++i) {
			if (i != 0)
				snbt.append(',');
			snbt.append(values[i]).append('L');
		}
		return BukkitLoader.getNmsProvider().getNBTBase(BukkitLoader.getNmsProvider().parseNBT(snbt.append("]}").toString()),
				"value");
	}

	private Object toNmsList(NbtList source) throws Exception {
		Object holder = BukkitLoader.getNmsProvider().parseNBT("{value:[]}");
		Object list = BukkitLoader.getNmsProvider().getNBTBase(holder, "value");
		for (Object value : source.values())
			appendNmsListValue(list, createNmsTag(value));
		return list;
	}

	private Object createNmsTag(Object value) throws Exception {
		Object holder = BukkitLoader.getNmsProvider().parseNBT("{}");
		setNmsValue(holder, "value", value);
		return BukkitLoader.getNmsProvider().getNBTBase(holder, "value");
	}

	private void appendNmsListValue(Object list, Object value) throws Exception {
		for (Method method : list.getClass().getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers()) || !"add".equals(method.getName()))
				continue;
			Class<?>[] parameters = method.getParameterTypes();
			if (parameters.length == 1 && parameters[0].isAssignableFrom(value.getClass())) {
				method.setAccessible(true);
				method.invoke(list, value);
				return;
			}
			if (parameters.length == 2 && parameters[0] == int.class && parameters[1].isAssignableFrom(value.getClass())) {
				method.setAccessible(true);
				method.invoke(list, listSize(list), value);
				return;
			}
		}
		throw new IOException("Unable to append NBT list value on " + Bukkit.getBukkitVersion());
	}

	private int listSize(Object list) {
		try {
			Method size = list.getClass().getMethod("size");
			return ((Number) size.invoke(list)).intValue();
		} catch (Exception ignored) {
			return 0;
		}
	}

	static Object registries = Ref.invokeStatic(Ref.method(Ref.nms("server", "MinecraftServer"), "getDefaultRegistryAccess"));
	static {
		if(registries==null)registries = Ref.invoke(Ref.invokeStatic(Ref.method(Ref.nms("server", "MinecraftServer"), "getServer")), "registryAccess");
	}
	static Method parseItem = Ref.findMethodByName(Ref.nms("world.item", "ItemStack"), "parseOptional");
	static Method legacyCreateItem = Ref.method(Ref.nms("world.item", "ItemStack"), Ref.isOlderThan(9) ? "createStack":Ref.isOlderThan(19)?"a":"of", Ref.nms("nbt", Ref.isOlderThan(19)?"NBTTagCompound":"CompoundTag"));
	static Constructor<?> oldCreateItem = Ref.constructor(Ref.nms("world.item", "ItemStack"), Ref.nms("", "NBTTagCompound"));
	static Object CODEC = Ref.getStatic(Ref.field(Ref.nms("world.item", "ItemStack"), "CODEC"));
	static Method decode = CODEC == null ? null : Ref.method(CODEC.getClass(), "decode", Ref.getClass("com.mojang.serialization.DynamicOps"), Object.class);
	static Object NbtOps = Ref.getStatic(Ref.field(Ref.nms("nbt", "NbtOps"), "INSTANCE"));
	static Method result = CODEC == null ? null : Ref.method(Ref.getClass("com.mojang.serialization.DataResult"), "result");
	static Method getFirst = CODEC == null ? null : Ref.method(Ref.getClass("com.mojang.datafixers.util.Pair"), "getFirst");

	private Object createLegacyNmsItem(Object compound) throws Exception {
		if(compound==null)return null;
		if(oldCreateItem!=null)
			return Ref.newInstance(oldCreateItem, compound);
		if(legacyCreateItem!=null)
			return Ref.invokeStatic(legacyCreateItem, compound);
		if(parseItem!=null)
			return Ref.invokeStatic(parseItem, registries, compound);
		if(decode!=null) {
			Optional<?> decoded = (Optional<?>) Ref.invoke(Ref.invoke(CODEC, decode, NbtOps, compound), result);
			return decoded.isPresent() ? Ref.invoke(decoded.get(), getFirst) : null;
		}
		return null;
	}
	*/

	private void saveOfflineInventory(GUI gui, OfflineInventory offline) throws IOException {
		List<Object> inventory = new ArrayList<>();
		Path itemTemp = Files.createTempFile("css-invsee-item-", ".nbt");

		try {
			for (int i = 0; i < 36; ++i)
				addOfflineItem(inventory, gui.getItem(18 + i), i, itemTemp);

			addOfflineItem(inventory, gui.getItem(BOOTS_SLOT), 100, itemTemp);
			addOfflineItem(inventory, gui.getItem(LEGGINGS_SLOT), 101, itemTemp);
			addOfflineItem(inventory, gui.getItem(CHESTPLATE_SLOT), 102, itemTemp);
			addOfflineItem(inventory, gui.getItem(HEAD_SLOT), 103, itemTemp);

			if (Ref.isNewerThan(8))
				addOfflineItem(inventory, gui.getItem(OFFHAND_SLOT), -106, itemTemp);
		} finally {
			Files.deleteIfExists(itemTemp);
		}

		offline.root.values().put("Inventory", new NbtList("Inventory", 10, inventory));

		Path temporary = offline.file.resolveSibling(offline.file.getFileName().toString() + ".invsee.tmp");
		NbtWriter.write(temporary, offline.root, true);

		try {
			Files.move(temporary, offline.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temporary, offline.file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addOfflineItem(List<Object> inventory, ItemStack stack, int slot, Path temp) throws IOException {
		if (isEmpty(stack))
			return;

		NbtCompound compound;
		try {
			compound = serializeItem(stack, temp);
		} catch (Exception e) {
			throw new IOException("Unable to serialize ItemStack on " + Bukkit.getBukkitVersion(), e);
		}

		Map values = compound.values();
		values.remove("DataVersion");
		values.put("Slot", (byte) slot);
		inventory.add(compound);
	}

	private NbtCompound serializeItem(ItemStack stack, Path temp) throws Exception {
		Object unsafe = Bukkit.getUnsafe();
		Method serialize = findMethod(unsafe.getClass(), "serializeItem", ItemStack.class);

		if (serialize != null) {
			byte[] bytes = (byte[]) serialize.invoke(unsafe, stack);
			Files.write(temp, bytes);
			NbtTag tag = NbtReader.read(temp);
			if (tag instanceof NbtCompound)
				return (NbtCompound) tag;
		}

		Method paperSerialize = findMethod(stack.getClass(), "serializeAsBytes");
		if (paperSerialize != null) {
			byte[] bytes = (byte[]) paperSerialize.invoke(stack);
			Files.write(temp, bytes);
			NbtTag tag = NbtReader.read(temp);
			if (tag instanceof NbtCompound)
				return (NbtCompound) tag;
		}

		Object compound = saveLegacyNmsItem(stack, temp);
		NbtTag tag = NbtReader.read(temp);
		if (!(tag instanceof NbtCompound))
			throw new IOException("Serialized legacy ItemStack isn't TAG_Compound: " + compound);
		return (NbtCompound) tag;
	}

	private Object saveLegacyNmsItem(ItemStack stack, Path temp) throws Exception {
		Class<?> craft = getCraftItemStackClass();
		Method asNmsCopy = craft.getMethod("asNMSCopy", ItemStack.class);
		Object nmsItem = asNmsCopy.invoke(null, stack);
		Object compound = BukkitLoader.getNmsProvider().parseNBT("{}");
		Object saved = null;

		for (Method method : nmsItem.getClass().getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 1 || !compound.getClass().isAssignableFrom(method.getReturnType()))
				continue;
			if (!method.getParameterTypes()[0].isAssignableFrom(compound.getClass())
					&& !compound.getClass().isAssignableFrom(method.getParameterTypes()[0]))
				continue;

			try {
				method.setAccessible(true);
				saved = method.invoke(nmsItem, compound);
				if (saved != null)
					break;
			} catch (Throwable ignored) {
			}
		}

		if (saved == null)
			throw new IOException("Unable to find legacy ItemStack NBT save method on " + nmsItem.getClass().getName());

		writeLegacyNbt(saved, temp);
		return saved;
	}

	private void writeLegacyNbt(Object compound, Path temp) throws Exception {
		String pkg = compound.getClass().getPackage().getName();
		Class<?> tools = Class.forName(pkg + ".NBTCompressedStreamTools");

		try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
			for (Method method : tools.getDeclaredMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 2)
					continue;
				Class<?>[] params = method.getParameterTypes();
				if (!params[0].isAssignableFrom(compound.getClass()) && !compound.getClass().isAssignableFrom(params[0]) || !java.io.DataOutput.class.isAssignableFrom(params[1]))
					continue;

				method.setAccessible(true);
				method.invoke(null, compound, output);
				return;
			}
		}

		throw new IOException("Unable to find legacy NBT writer on " + Bukkit.getBukkitVersion());
	}

	private Class<?> getCraftItemStackClass() throws ClassNotFoundException {
		String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
		try {
			return Class.forName(craftPackage + ".inventory.CraftItemStack");
		} catch (ClassNotFoundException ignored) {
			return Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
		}
	}

	private Method findMethod(Class<?> type, String name, Class<?>... params) {
		Class<?> current = type;
		while (current != null)
			try {
				Method method = current.getDeclaredMethod(name, params);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException ignored) {
				current = current.getSuperclass();
			}
		try {
			Method method = type.getMethod(name, params);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException ignored) {
			return null;
		}
	}

	private void setGuiItem(GUI gui, int slot, ItemStack item) {
		ItemStack current = gui.getItem(slot);
		if (isEmpty(item)) {
			if (!isEmpty(current))
				gui.removeItem(slot);
			return;
		}

		if (!Objects.equals(item, current))
			gui.setItem(slot, new EmptyItemGUI(item.clone()).setUnstealable(false));
	}

	private ItemStack normalize(ItemStack stack) {
		return isEmpty(stack) ? null : stack.clone();
	}

	private boolean isEmpty(ItemStack stack) {
		return stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0;
	}
}
