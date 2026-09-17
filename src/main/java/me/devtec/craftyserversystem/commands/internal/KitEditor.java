package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.kits.KitSample;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.gui.AnvilGUI;
import me.devtec.theapi.bukkit.gui.EmptyItemGUI;
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
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class KitEditor extends CssCommand {

	private static final String GUI_MAIN = "kit-editor/main";
	private static final String GUI_DETAIL = "kit-editor/detail";
	private static final String GUI_SETTINGS = "kit-editor/settings";
	private static final String GUI_MESSAGES = "kit-editor/messages";
	private static final String GUI_COMMANDS = "kit-editor/commands";
	private static final String GUI_CONTENTS = "kit-editor/contents";

	private static final String LOOP_KITS = "kit_editor_kits";
	private static final String LOOP_MESSAGES = "kit_editor_messages";
	private static final String LOOP_COMMANDS = "kit_editor_commands";
	private static final String LOOP_SLOTS = "kit_editor_slots";

	private static final String DATA_KIT = "kit_editor_kit";

	/*
	 * Pořadí # slotů v contents.yml.
	 *
	 * Prvních 5:
	 *
	 * Helmet / Chestplate / Leggings / Boots / Offhand
	 *
	 * potom:
	 *
	 * 9-35 main inventory 0-8 hotbar
	 */
	private static final int[] INVENTORY_SLOT_ORDER = createInventorySlotOrder();

	@Override
	public void register() {
		if(isRegistered() || !API.get().getConfigManager().getCommands().getBoolean("kit.enabled", true))
			return;

		registerActions();
		registerLoops();

		/*
		 * GUI soubory už načítá CssGui.
		 *
		 * Loop GUI po registraci providerů reloadneme, stejně jako Warp.
		 */
		reloadLoopGui(GUI_MAIN);
		reloadLoopGui(GUI_MESSAGES);
		reloadLoopGui(GUI_COMMANDS);
		reloadLoopGui(GUI_CONTENTS);

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> openMain(sender)).permission(getPerm("cmd"));

		List<String> cmds = getCommands();

		if(!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	@Override
	public void unregister() {
		super.unregister();

		LoopManager.unregister(LOOP_KITS);
		LoopManager.unregister(LOOP_MESSAGES);
		LoopManager.unregister(LOOP_COMMANDS);
		LoopManager.unregister(LOOP_SLOTS);

		ActionManager.unregister("kit_editor_select");
		ActionManager.unregister("kit_editor_create");
		ActionManager.unregister("kit_editor_delete");

		ActionManager.unregister("kit_editor_edit_cost");
		ActionManager.unregister("kit_editor_edit_permission");
		ActionManager.unregister("kit_editor_edit_cooldown_permission");
		ActionManager.unregister("kit_editor_edit_cooldown_time");

		ActionManager.unregister("kit_editor_toggle_override");
		ActionManager.unregister("kit_editor_toggle_drop");

		ActionManager.unregister("kit_editor_add_message");
		ActionManager.unregister("kit_editor_add_command");

		ActionManager.unregister("kit_editor_import_inventory");
		ActionManager.unregister("kit_editor_clear_contents");
	}

	/*
	 * ============================================================ ACTIONS
	 * ============================================================
	 */

	private void registerActions() {

		/*
		 * ============================================================ SELECT KIT
		 * ============================================================
		 */

		ActionManager.register("kit_editor_select", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			String kitName = resolve(player, placeholders, values);

			KitSample kit = findKit(kitName);

			if(kit == null)
				return;

			syncKitData(sharedData, kit);
		});

		/*
		 * ============================================================ CREATE
		 * ============================================================
		 */

		ActionManager.register("kit_editor_create", (holder, values) -> (gui, player, sharedData, placeholders) -> openKitCreator(player, sharedData));

		/*
		 * ============================================================ DELETE
		 * ============================================================
		 */

		ActionManager.register("kit_editor_delete", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			deleteKit(kit);

			clearKitData(sharedData);
		});

		/*
		 * ============================================================ COST
		 * ============================================================
		 */

		ActionManager.register("kit_editor_edit_cost", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			openTextInput(player,

			        "&fKit Cost",

			        String.valueOf(kit.getCost()),

			        XMaterial.GOLD_INGOT,

			        value -> {

				        double cost = value == null || value.trim().isEmpty() ? 0 : Economy.multipleByMoneyFormat(ParseUtils.getDouble(value), value);

				        kit.setCost(cost);

				        saveAndSync(sharedData, kit);
			        },

			        GUI_SETTINGS);
		});

		/*
		 * ============================================================ PERMISSION
		 * ============================================================
		 */

		ActionManager.register("kit_editor_edit_permission", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			openTextInput(player,

			        "&fKit Permission",

			        kit.getPermission() == null ? "css.kit." + kit.getName() : kit.getPermission(),

			        XMaterial.NAME_TAG,

			        value -> {

				        kit.setPermission(value == null || value.trim().isEmpty() ? null : value.trim());

				        saveAndSync(sharedData, kit);
			        },

			        GUI_SETTINGS);
		});

		/*
		 * ============================================================ COOLDOWN BYPASS
		 * PERMISSION ============================================================
		 */

		ActionManager.register("kit_editor_edit_cooldown_permission", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			openTextInput(player,

			        "&fCooldown Bypass Permission",

			        kit.getCooldown().getBypassPerm() == null ? "css.cooldown.kits" : kit.getCooldown().getBypassPerm(),

			        XMaterial.NAME_TAG,

			        value -> {

				        kit.getCooldown().setBypassPerm(value == null || value.trim().isEmpty() ? null : value.trim());

				        saveAndSync(sharedData, kit);
			        },

			        GUI_SETTINGS);
		});

		/*
		 * ============================================================ COOLDOWN TIME
		 * ============================================================
		 */

		ActionManager.register("kit_editor_edit_cooldown_time", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			openTextInput(player,

			        "&fCooldown Time",

			        TimeUtils.timeToString(kit.getCooldown().getTime()),

			        XMaterial.CLOCK,

			        value -> {

				        kit.getCooldown().setTime(value == null || value.trim().isEmpty() ? 0 : TimeUtils.timeFromString(value.trim()));

				        saveAndSync(sharedData, kit);
			        },

			        GUI_SETTINGS);
		});

		/*
		 * ============================================================ OVERRIDE
		 * CONTENTS ============================================================
		 */

		ActionManager.register("kit_editor_toggle_override", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			kit.setOverrideContents(!kit.isOverrideContents());

			saveAndSync(sharedData, kit);
		});

		/*
		 * ============================================================ DROP ITEMS
		 * ============================================================
		 */

		ActionManager.register("kit_editor_toggle_drop", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			kit.setDropItems(!kit.isDropItems());

			saveAndSync(sharedData, kit);
		});

		/*
		 * ============================================================ ADD MESSAGE
		 * ============================================================
		 */

		ActionManager.register("kit_editor_add_message", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			openTextInput(player,

			        "&fAdd Message",

			        "Message Here",

			        XMaterial.PAPER,

			        value -> {

				        kit.getMessages().add(value == null ? "" : value);

				        saveAndSync(sharedData, kit);
			        },

			        GUI_MESSAGES);
		});

		/*
		 * ============================================================ ADD COMMAND
		 * ============================================================
		 */

		ActionManager.register("kit_editor_add_command", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			openTextInput(player,

			        "&fAdd Command",

			        "Command Here",

			        XMaterial.COMMAND_BLOCK,

			        value -> {

				        if(value == null || value.trim().isEmpty())
					        return;

				        String command = value.trim();

				        if(command.startsWith("/"))
					        command = command.substring(1);

				        kit.getCommands().add(command);

				        saveAndSync(sharedData, kit);
			        },

			        GUI_COMMANDS);
		});

		/*
		 * ============================================================ IMPORT PLAYER
		 * INVENTORY
		 *
		 * Přesně 0-40.
		 *
		 * Tzn:
		 *
		 * hotbar main inventory boots leggings chestplate helmet offhand
		 * ============================================================
		 */

		ActionManager.register("kit_editor_import_inventory", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			kit.getContents().clear();

			for(int slot = 0; slot <= 40; ++slot) {

				ItemStack item = player.getInventory().getItem(slot);

				if(isEmpty(item))
					continue;

				kit.getContents().put(slot, item.clone());
			}

			saveAndSync(sharedData, kit);
		});

		/*
		 * ============================================================ CLEAR CONTENTS
		 * ============================================================
		 */

		ActionManager.register("kit_editor_clear_contents", (holder, values) -> (gui, player, sharedData, placeholders) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return;

			kit.getContents().clear();

			saveAndSync(sharedData, kit);
		});
	}

	/*
	 * ============================================================ LOOPS
	 * ============================================================
	 */

	private void registerLoops() {

		registerKitLoop();
		registerMessageLoop();
		registerCommandLoop();
		registerSlotLoop();
	}

	/*
	 * ============================================================ KIT LIST
	 * ============================================================
	 */

	private void registerKitLoop() {

		LoopManager.register(LOOP_KITS, () -> (holder, player, sharedData, conditions, defaultItem) -> {

			List<ItemGUI> result = new ArrayList<>();

			for(KitSample kit : kits().values()) {

				Map<String, Object> placeholders = new LinkedHashMap<>();

				placeholders.put("kit", kit.getName());

				placeholders.put("kit_name", kit.getName());

				placeholders.put("kit_permission", kit.getPermission() == null ? "none" : kit.getPermission());

				placeholders.put("kit_cost", kit.getCost());

				placeholders.put("kit_contents", kit.getContents().size());

				placeholders.put("kit_messages", kit.getMessages().size());

				placeholders.put("kit_commands", kit.getCommands().size());

				addLoopItem(result, player, sharedData, placeholders, conditions, defaultItem);
			}

			return result;
		});
	}

	/*
	 * ============================================================ MESSAGES
	 * ============================================================
	 */

	private void registerMessageLoop() {

		LoopManager.register(LOOP_MESSAGES, () -> (holder, player, sharedData, conditions, defaultItem) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return Collections.emptyList();

			List<ItemGUI> result = new ArrayList<>();

			for(int i = 0; i < kit.getMessages().size(); ++i) {

				String message = kit.getMessages().get(i);

				int index = i;

				Map<String, Object> placeholders = new LinkedHashMap<>();

				placeholders.put("message", message);

				placeholders.put("message_index", index);

				placeholders.put("message_number", index + 1);

				ItemPackage itemPackage = findLoopItem(player, sharedData, placeholders, conditions, defaultItem);

				if(itemPackage == null)
					continue;

				result.add(createRemovableTextItem(itemPackage, player, sharedData, placeholders,

				        () -> {

					        if(index < 0 || index >= kit.getMessages().size())
						        return;

					        kit.getMessages().remove(index);

					        saveAndSync(sharedData, kit);

					        openGui(player, GUI_MESSAGES);
				        }));
			}

			return result;
		});
	}

	/*
	 * ============================================================ COMMANDS
	 * ============================================================
	 */

	private void registerCommandLoop() {

		LoopManager.register(LOOP_COMMANDS, () -> (holder, player, sharedData, conditions, defaultItem) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return Collections.emptyList();

			List<ItemGUI> result = new ArrayList<>();

			for(int i = 0; i < kit.getCommands().size(); ++i) {

				String command = kit.getCommands().get(i);

				int index = i;

				Map<String, Object> placeholders = new LinkedHashMap<>();

				placeholders.put("command", command);

				placeholders.put("command_index", index);

				placeholders.put("command_number", index + 1);

				ItemPackage itemPackage = findLoopItem(player, sharedData, placeholders, conditions, defaultItem);

				if(itemPackage == null)
					continue;

				result.add(createRemovableTextItem(itemPackage, player, sharedData, placeholders,

				        () -> {

					        if(index < 0 || index >= kit.getCommands().size())
						        return;

					        kit.getCommands().remove(index);

					        saveAndSync(sharedData, kit);

					        openGui(player, GUI_COMMANDS);
				        }));
			}

			return result;
		});
	}

	/*
	 * ============================================================ EXACT INVENTORY
	 * SLOT EDITOR ============================================================
	 */

	private void registerSlotLoop() {

		LoopManager.register(LOOP_SLOTS, () -> (holder, player, sharedData, conditions, defaultItem) -> {

			KitSample kit = selectedKit(sharedData);

			if(kit == null)
				return Collections.emptyList();

			List<ItemGUI> items = new ArrayList<>(41);

			for(int slot : INVENTORY_SLOT_ORDER)
				items.add(createInventorySlotItem(player, sharedData, kit, slot));

			return items;
		});
	}

	/*
	 * ============================================================ INVENTORY SLOT
	 * ITEM ============================================================
	 */

	private ItemGUI createInventorySlotItem(Player viewer, Config sharedData, KitSample kit, int inventorySlot) {

		ItemStack stored = kit.getContents().get(inventorySlot);

		ItemStack display = createSlotDisplay(stored, inventorySlot);

		return new ItemGUI(display) {

			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {

				/*
				 * Pokud hráč drží item na cursoru, zkopírujeme ho přímo do tohoto přesného
				 * inventory slotu kitu.
				 *
				 * Item hráči NEODEBÍRÁME.
				 */
				ItemStack cursor = player.getItemOnCursor();

				if(!isEmpty(cursor)) {

					kit.getContents().put(inventorySlot, cursor.clone());

					saveAndSync(sharedData, kit);

					openGui(player, GUI_CONTENTS);

					return;
				}

				ItemStack current = kit.getContents().get(inventorySlot);

				if(isEmpty(current))
					return;

				/*
				 * SHIFT + LEFT = remove
				 */
				if(click.isLeftClick() && click.isShiftClick()) {

					kit.getContents().remove(inventorySlot);

					saveAndSync(sharedData, kit);

					openGui(player, GUI_CONTENTS);

					return;
				}

				/*
				 * RIGHT = copy do vlastního inventáře
				 */
				if(click.isRightClick())
					player.getInventory().addItem(current.clone());
			}
		};
	}

	private ItemStack createSlotDisplay(ItemStack stored, int slot) {

		if(isEmpty(stored))
			return ItemMaker.of(emptySlotMaterial(slot)).displayName("&7" + slotDisplayName(slot))
			        .lore("", "&8» &7Player inventory slot: &e" + slot, "", "&8» &7Place an item on your cursor", "&8» &7and click this slot to assign it.", "").build();

		ItemMaker maker = ItemMaker.of(stored.clone());

		List<String> lore = new ArrayList<>();

		if(maker.getLore() != null)
			lore.addAll(maker.getLore());

		lore.add("");
		lore.add("&8» &7Kit slot: &e" + slotDisplayName(slot));

		lore.add("&8» &7Player inventory slot: &e" + slot);

		lore.add("");
		lore.add("&8» &7Cursor item: &eReplace");

		lore.add("&8» &7Right click: &eCopy item");

		lore.add("&8» &7Shift + Left: &cRemove");

		lore.add("");

		maker.lore(lore);

		return maker.build();
	}

	private static XMaterial emptySlotMaterial(int slot) {

		switch(slot) {

			case 39 :
				return XMaterial.LEATHER_HELMET;

			case 38 :
				return XMaterial.LEATHER_CHESTPLATE;

			case 37 :
				return XMaterial.LEATHER_LEGGINGS;

			case 36 :
				return XMaterial.LEATHER_BOOTS;

			case 40 :
				return XMaterial.SHIELD;

			default :
				return XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE;
		}
	}

	private static String slotDisplayName(int slot) {

		switch(slot) {

			case 39 :
				return "Helmet";

			case 38 :
				return "Chestplate";

			case 37 :
				return "Leggings";

			case 36 :
				return "Boots";

			case 40 :
				return "Offhand";

			default : {

				if(slot >= 0 && slot <= 8)

					return "Hotbar " + (slot + 1);

				return "Inventory " + slot;
			}
		}
	}

	/*
	 * ============================================================ LOOP UTILITIES
	 * ============================================================
	 */

	private static void addLoopItem(List<ItemGUI> items, Player player, Config sharedData, Map<String, Object> placeholders, List<ConditionItem> conditions, ItemPackage defaultItem) {

		ItemPackage result = findLoopItem(player, sharedData, placeholders, conditions, defaultItem);

		if(result == null)
			return;

		items.add(createLoopItem(result, player, sharedData, placeholders));
	}

	private static ItemPackage findLoopItem(Player player, Config sharedData, Map<String, Object> placeholders, List<ConditionItem> conditions, ItemPackage defaultItem) {

		for(ConditionItem condition : conditions) {

			ItemPackage result = condition.test(player, sharedData, placeholders);

			if(result != null && result.getItem() != null)

				return result;
		}

		return defaultItem != null && defaultItem.getItem() != null ? defaultItem : null;
	}

	private static ItemGUI createLoopItem(ItemPackage itemPackage, Player player, Config sharedData, Map<String, Object> placeholders) {

		return new ItemGUI(Utils.applyPlaceholders(itemPackage.getTypePlaceholder(), itemPackage.getItem(), placeholders, player)) {

			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {

				itemPackage.runActions(gui, player, sharedData, placeholders);
			}
		};
	}

	/*
	 * Messages / Commands potřebují ClickType, proto nemůžeme remove dát pouze do
	 * ActionManager.
	 */
	private static ItemGUI createRemovableTextItem(ItemPackage itemPackage, Player player, Config sharedData, Map<String, Object> placeholders, Runnable remove) {

		return new ItemGUI(Utils.applyPlaceholders(itemPackage.getTypePlaceholder(), itemPackage.getItem(), placeholders, player)) {

			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {

				if(click.isLeftClick() && click.isShiftClick())

					remove.run();
			}
		};
	}

	/*
	 * ============================================================ CREATE KIT
	 * ============================================================
	 */

	private void openKitCreator(Player player, Config sharedData) {

		AnvilGUI anvil = new AnvilGUI("&fType kit name") {

			@Override
			public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {

				if(!guiClick)
					return false;

				String name = getRenameText();

				if(name == null)
					return false;

				name = name.replace(" ", "").trim();

				if(name.isEmpty() || "kitnamehere".equalsIgnoreCase(name))
					return false;

				KitSample existing = findKit(name);

				if(existing != null) {

					syncKitData(sharedData, existing);

					close(player);

					return false;
				}

				KitSample kit = new KitSample(name);

				kits().put(name.toLowerCase(Locale.ROOT), kit);

				saveKit(kit);

				syncKitData(sharedData, kit);

				close(player);

				return false;
			}

			@Override
			public void onClose(Player player, CloseReason reason) {

				if(selectedKit(sharedData) != null)

					openGui(player, GUI_DETAIL);
				else
					openGui(player, GUI_MAIN);
			}
		};

		anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.NAME_TAG).displayName("Kit Name Here").build()));

		anvil.open(player);
	}

	/*
	 * ============================================================ GENERIC ANVIL
	 * INPUT ============================================================
	 */

	private void openTextInput(Player player, String title, String initialValue, XMaterial material, Consumer<String> consumer, String returnGui) {

		AnvilGUI anvil = new AnvilGUI(title) {

			@Override
			public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {

				if(!guiClick)
					return false;

				consumer.accept(getRenameText());

				close(player);

				return false;
			}

			@Override
			public void onClose(Player player, CloseReason reason) {

				openGui(player, returnGui);
			}
		};

		String display = initialValue == null || initialValue.isEmpty() ? " " : initialValue;

		anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(material).displayName(display).build()));

		anvil.open(player);
	}

	/*
	 * ============================================================ GUI
	 * ============================================================
	 */

	private void openMain(Player player) {

		Config data = sharedData(player);

		clearKitData(data);

		openGui(player, GUI_MAIN);
	}

	private static void openGui(Player player, String id) {

		GuiCreator creator = GuiCreator.guis.get(id);

		if(creator != null)
			creator.open(player);
	}

	private static void reloadLoopGui(String id) {

		GuiCreator creator = GuiCreator.guis.get(id);

		if(creator instanceof LoopGuiCreator)
			((LoopGuiCreator) creator).reload();
	}

	/*
	 * ============================================================ KIT STATE
	 * ============================================================
	 */

	private static Config sharedData(Player player) {

		return GuiCreator.sharedData.computeIfAbsent(player.getUniqueId(), ignored -> new Config());
	}

	private KitSample selectedKit(Config sharedData) {

		String name = sharedData.getString(DATA_KIT);

		return findKit(name);
	}

	private void syncKitData(Config data, KitSample kit) {

		data.set(DATA_KIT, kit.getName());

		data.set("kit_name", kit.getName());

		data.set("kit_permission", kit.getPermission() == null ? "none" : kit.getPermission());

		data.set("kit_cost", kit.getCost());

		data.set("kit_override_contents", kit.isOverrideContents());

		data.set("kit_drop_items", kit.isDropItems());

		data.set("kit_cooldown_permission", kit.getCooldown().getBypassPerm() == null ? "none" : kit.getCooldown().getBypassPerm());

		data.set("kit_cooldown_time", TimeUtils.timeToString(kit.getCooldown().getTime()));

		data.set("kit_contents_count", kit.getContents().size());

		data.set("kit_messages_count", kit.getMessages().size());

		data.set("kit_commands_count", kit.getCommands().size());
	}

	private static void clearKitData(Config data) {

		data.remove(DATA_KIT);

		data.remove("kit_name");
		data.remove("kit_permission");
		data.remove("kit_cost");

		data.remove("kit_override_contents");
		data.remove("kit_drop_items");

		data.remove("kit_cooldown_permission");
		data.remove("kit_cooldown_time");

		data.remove("kit_contents_count");
		data.remove("kit_messages_count");
		data.remove("kit_commands_count");
	}

	/*
	 * ============================================================ KIT LOOKUP
	 * ============================================================
	 */

	private Map<String, KitSample> kits() {

		return ((Kit) API.get().getCommandManager().getRegistered().get("kit")).getKits();
	}

	private KitSample findKit(String name) {

		if(name == null || name.trim().isEmpty())
			return null;

		KitSample direct = kits().get(name.toLowerCase(Locale.ROOT));

		if(direct != null)
			return direct;

		for(KitSample kit : kits().values())

			if(kit.getName().equalsIgnoreCase(name))

				return kit;

		return null;
	}

	/*
	 * ============================================================ SAVE
	 * ============================================================
	 */

	private void saveAndSync(Config data, KitSample kit) {

		saveKit(kit);

		syncKitData(data, kit);
	}

	private void saveKit(KitSample kit) {

		Config config = API.get().getConfigManager().getKits();

		String key = kit.getName();

		config.remove(key);

		config.set(key + ".permission", kit.getPermission());

		config.set(key + ".settings.cost", kit.getCost());

		config.set(key + ".settings.override-contents-in-slots", kit.isOverrideContents());

		config.set(key + ".settings.drop-items-when-full-inv", kit.isDropItems());

		config.set(key + ".settings.cooldown.bypass-perm", kit.getCooldown().getBypassPerm());

		config.set(key + ".settings.cooldown.time", TimeUtils.timeToString(kit.getCooldown().getTime()));

		config.set(key + ".messages", kit.getMessages());

		config.set(key + ".commands", kit.getCommands());

		List<Entry<Integer, ItemStack>> entries = new ArrayList<>(kit.getContents().entrySet());

		entries.sort(Entry.comparingByKey());

		List<String> contents = new ArrayList<>();

		for(Entry<Integer, ItemStack> entry : entries) {

			ItemStack stack = entry.getValue();

			if(isEmpty(stack))
				continue;

			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) Json.writer().writeWithoutParse(stack);

			String type = map.remove("type").toString();

			if(map.isEmpty())

				contents.add(entry.getKey() + ":" + type);

			else
				contents.add(entry.getKey() + ":" + type + Json.writer().simpleWrite(map));
		}

		config.set(key + ".contents", contents);

		config.save("yaml");
	}

	/*
	 * ============================================================ DELETE
	 * ============================================================
	 */

	private void deleteKit(KitSample kit) {

		kits().remove(kit.getName().toLowerCase(Locale.ROOT));

		API.get().getCooldownManager().unregister(kit.getCooldown().id());

		API.get().getConfigManager().getKits().remove(kit.getName()).save("yaml");
	}

	/*
	 * ============================================================ UTILS
	 * ============================================================
	 */

	private static String resolve(Player player, Map<String, Object> placeholders, String values) {

		if(values == null || values.isEmpty())
			return "";

		return Utils.replacePlaceholders(values, placeholders, player.getUniqueId());
	}

	private static boolean isEmpty(ItemStack item) {

		return item == null || item.getType() == Material.AIR;
	}

	private static int[] createInventorySlotOrder() {

		int[] slots = new int[41];

		int index = 0;

		/*
		 * Armor + offhand
		 */
		slots[index++] = 39;
		slots[index++] = 38;
		slots[index++] = 37;
		slots[index++] = 36;
		slots[index++] = 40;

		/*
		 * Main inventory
		 */
		for(int slot = 9; slot <= 35; ++slot)

			slots[index++] = slot;

		/*
		 * Hotbar
		 */
		for(int slot = 0; slot <= 8; ++slot)

			slots[index++] = slot;

		return slots;
	}
}
