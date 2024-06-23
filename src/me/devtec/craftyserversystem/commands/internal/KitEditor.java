package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import me.devtec.theapi.bukkit.gui.GUI;
import me.devtec.theapi.bukkit.gui.GUI.ClickType;
import me.devtec.theapi.bukkit.gui.HolderGUI;
import me.devtec.theapi.bukkit.gui.ItemGUI;
import me.devtec.theapi.bukkit.xseries.XMaterial;

public class KitEditor extends CssCommand {
	private static ItemGUI empty = new EmptyItemGUI(ItemMaker.of(XMaterial.BLACK_STAINED_GLASS_PANE).displayName("&c").build());

	@Override
	public void register() {
		if (isRegistered() || !API.get().getConfigManager().getCommands().getBoolean("kit.enabled", true))
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			openEditor(sender, 1);
		}).permission(getPerm("cmd"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void openEditor(Player player, int requestedPage) {
		Map<String, KitSample> kits = ((Kit) API.get().getCommandManager().getRegistered().get("kit")).getKits();
		int totalPages = kits.size() / 27 + (kits.size() % 27 == 0 ? 0 : 1);
		if (requestedPage > totalPages)
			requestedPage = totalPages;
		if (requestedPage < 1)
			requestedPage = 1;
		final int page = requestedPage;
		GUI gui = new GUI("&fKit Editor " + page + "/" + totalPages, 45);
		for (int i = 0; i < 9; ++i)
			gui.setItem(i, empty);
		for (int i = 36; i < 45; ++i)
			gui.setItem(i, empty);
		gui.setItem(40, new ItemGUI(ItemMaker.of(XMaterial.RED_STAINED_GLASS_PANE).displayName("&cClose").lore("", "&8» &7Click to close menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {
				gui.close(player);
			}
		});
		gui.setItem(4, new ItemGUI(ItemMaker.of(XMaterial.LIME_STAINED_GLASS_PANE).displayName("&aCreate new kit").lore("", "&8» &7Click to open kit creator", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {
				openKitCreator(player, page);
			}
		});
		KitSample[] kitSamples = kits.values().toArray(new KitSample[0]);
		for (int i = page * 27 - 27; i < page * 27; ++i) {
			if (kitSamples.length <= i || kitSamples[i] == null)
				break;
			KitSample kit = kitSamples[i];
			gui.addItem(new ItemGUI(ItemMaker.of(XMaterial.CHEST).displayName("&f" + kitSamples[i].getName()).lore("", "&8» &7Click to open kit editor", "").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					openEditorOf(player, page, kit);
				}
			});
		}
		if (page != totalPages)
			gui.setItem(42, new ItemGUI(ItemMaker.ofHead().skinValues(
					"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ5OTNiOGMxMzU4ODkxOWI5ZjhiNDJkYjA2NWQ1YWRmZTc4YWYxODI4MTViNGU2ZjBmOTFiYTY4M2RlYWM5In19fQ==")
					.displayName("&fNext page &e&l>>>").lore("", "&8» &7Click to continue to next page", "").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					openEditor(player, page + 1);
				}
			});
		if (page != 1)
			gui.setItem(38,
					new ItemGUI(ItemMaker.ofHead().skinValues(
							"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjRiZmVmMTRlODQyMGEyNTZlNDU3YTRhN2M4ODExMmUxNzk0ODVlNTIzNDU3ZTQzODUxNzdiYWQifX19")
							.displayName("&e&l<<< &fPrevious page").lore("", "&8» &7Click to return to previous page", "").build()) {
						@Override
						public void onClick(Player player, HolderGUI gui, ClickType click) {
							openEditor(player, page - 1);
						}
					});
		gui.open(player);
	}

	public void openEditorOf(Player player, int page, KitSample kit) {
		GUI gui = new GUI("&fKit Editor of " + kit.getName(), 54) {
			@Override
			public void onClose(Player player) {
				if (((Kit) API.get().getCommandManager().getRegistered().get("kit")).getKits().containsKey(kit.getName())) {
					// save
					Config config = API.get().getConfigManager().getKits();
					config.remove(kit.getName());
					config.set(kit.getName() + ".permission", kit.getPermission());
					config.set(kit.getName() + ".settings.cost", kit.getCost());
					config.set(kit.getName() + ".settings.override-contents-in-slots", kit.isOverrideContents());
					config.set(kit.getName() + ".settings.drop-items-when-full-inv", kit.isDropItems());
					config.set(kit.getName() + ".settings.cooldown.bypass-perm", kit.getCooldown().getBypassPerm());
					config.set(kit.getName() + ".settings.cooldown.time", TimeUtils.timeToString(kit.getCooldown().getTime()));
					config.set(kit.getName() + ".messages", kit.getMessages());
					config.set(kit.getName() + ".commands", kit.getCommands());
					List<String> contents = new ArrayList<>();
					for (ItemStack stack : kit.getContents()) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) Json.writer().writeWithoutParse(stack);
						String type = map.remove("type").toString();
						if (map.isEmpty())
							contents.add(type);
						else
							contents.add(type + Json.writer().simpleWrite(map));
					}
					config.set(kit.getName() + ".contents", contents);
					config.save("yaml");
				} else
					// remove
					API.get().getConfigManager().getKits().remove(kit.getName()).save("yaml");
			}
		};
		for (int i = 0; i < 9; ++i)
			gui.setItem(i, empty);
		for (int i = 45; i < 54; ++i)
			gui.setItem(i, empty);
		gui.setItem(22, new ItemGUI(ItemMaker.of(XMaterial.REPEATER).displayName("&cSettings").lore("", "&8» &7Kit settings", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openKitSettings(player, page, kit);
			}
		});
		gui.setItem(29, new ItemGUI(ItemMaker.of(XMaterial.COMMAND_BLOCK).displayName("&dCommands").lore("", "&8» &7Commands", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openCommands(player, page, kit, 1);
			}
		});
		gui.setItem(31, new ItemGUI(ItemMaker.of(XMaterial.CHEST).displayName("&6Contents").lore("", "&8» &7Kit contents", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openContents(player, page, kit, 1);
			}
		});
		gui.setItem(33, new ItemGUI(ItemMaker.of(XMaterial.WRITABLE_BOOK).displayName("&fMessages").lore("", "&8» &7Messages", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openMessages(player, page, kit, 1);
			}
		});
		gui.setItem(49, new ItemGUI(
				ItemMaker.of(XMaterial.LIME_STAINED_GLASS_PANE).displayName("&eSave & return back").lore("", "&8» &7Click to save changes into file & return back to main menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {
				openEditor(player, page);
			}
		});
		gui.setItem(52, new ItemGUI(ItemMaker.of(XMaterial.TNT).displayName("&4DELETE KIT").lore("", "&8» &7Click to delete kit", "&8» &7Click with SHIFT to confirm", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {
				if (click.isShiftClick()) {
					((Kit) API.get().getCommandManager().getRegistered().get("kit")).getKits().remove(kit.getName());
					openEditor(player, page);
				}
			}
		});
		gui.open(player);
	}

	protected void openMessages(Player player, int guiPage, KitSample kit, int requestedPage) {
		int totalPages = kit.getMessages().size() / 36 + (kit.getMessages().size() % 36 == 0 ? 0 : 1);
		if (requestedPage > totalPages)
			requestedPage = totalPages;
		if (requestedPage < 1)
			requestedPage = 1;
		final int page = requestedPage;
		GUI gui = new GUI("&fKit Messages of " + kit.getName() + " " + page + "/" + totalPages, 54);
		for (int i = 0; i < 9; ++i)
			gui.setItem(i, empty);
		for (int i = 45; i < 54; ++i)
			gui.setItem(i, empty);
		gui.setItem(49, new ItemGUI(ItemMaker.of(XMaterial.YELLOW_STAINED_GLASS_PANE).displayName("&eReturn back").lore("", "&8» &7Click to return back to previous menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openEditorOf(player, guiPage, kit);
			}
		});
		String[] items = kit.getMessages().toArray(new String[0]);
		for (int i = page * 36 - 36; i < page * 36; ++i) {
			if (items.length <= i || items[i] == null)
				break;
			String item = items[i];
			int id = i;
			gui.addItem(new ItemGUI(ItemMaker.of(XMaterial.PAPER).displayName("&fMessage #" + i).lore("", "&8» &f" + item, "", "&8» &7Left + Shift click to remove this message").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					if (click.isLeftClick() && click.isShiftClick()) {
						kit.getMessages().remove(id);
						openContents(player, guiPage, kit, page);
					}
				}
			});
		}
		if (page != totalPages)
			gui.setItem(41, new ItemGUI(ItemMaker.ofHead().skinValues(
					"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ5OTNiOGMxMzU4ODkxOWI5ZjhiNDJkYjA2NWQ1YWRmZTc4YWYxODI4MTViNGU2ZjBmOTFiYTY4M2RlYWM5In19fQ==")
					.displayName("&fNext page &e&l>>>").lore("", "&8» &7Click to continue to next page", "").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					openMessages(player, guiPage, kit, page + 1);
				}
			});
		if (page != 1)
			gui.setItem(37,
					new ItemGUI(ItemMaker.ofHead().skinValues(
							"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjRiZmVmMTRlODQyMGEyNTZlNDU3YTRhN2M4ODExMmUxNzk0ODVlNTIzNDU3ZTQzODUxNzdiYWQifX19")
							.displayName("&e&l<<< &fPrevious page").lore("", "&8» &7Click to return to previous page", "").build()) {
						@Override
						public void onClick(Player player, HolderGUI gui, ClickType click) {
							openMessages(player, guiPage, kit, page - 1);
						}
					});
		gui.setItem(52, new ItemGUI(ItemMaker.of(XMaterial.WRITABLE_BOOK).displayName("&aAdd message").lore("", "&8» &7Click to add new message", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				AnvilGUI anvil = new AnvilGUI("&fType message") {
					@Override
					public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
						if (guiClick) {
							if (getRenameText() == null || getRenameText().trim().isEmpty())
								kit.getMessages().add("");
							else
								kit.getMessages().add(getRenameText());
							close(player);
						}
						return false;
					}

					@Override
					public void onClose(Player player) {
						openMessages(player, guiPage, kit, page);
					}
				};
				anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.PAPER).displayName("Message Here").build()));
				anvil.open(player);
			}
		});
		gui.open(player);
	}

	protected void openContents(Player player, int guiPage, KitSample kit, int requestedPage) {
		int totalPages = kit.getContents().size() / 36 + (kit.getContents().size() % 36 == 0 ? 0 : 1);
		if (requestedPage > totalPages)
			requestedPage = totalPages;
		if (requestedPage < 1)
			requestedPage = 1;
		final int page = requestedPage;
		GUI gui = new GUI("&fKit Contents of " + kit.getName() + " " + page + "/" + totalPages, 54);
		for (int i = 0; i < 9; ++i)
			gui.setItem(i, empty);
		for (int i = 45; i < 54; ++i)
			gui.setItem(i, empty);
		gui.setItem(49, new ItemGUI(ItemMaker.of(XMaterial.YELLOW_STAINED_GLASS_PANE).displayName("&eReturn back").lore("", "&8» &7Click to return back to previous menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openEditorOf(player, guiPage, kit);
			}
		});
		ItemStack[] items = kit.getContents().toArray(new ItemStack[0]);
		for (int i = page * 36 - 36; i < page * 36; ++i) {
			if (items.length <= i || items[i] == null)
				break;
			ItemStack item = items[i];
			ItemMaker maker = ItemMaker.of(item);
			List<String> newLore = new ArrayList<>();
			if (maker.getLore() != null)
				newLore.addAll(maker.getLore());
			newLore.add("");
			newLore.add("&8» &7Left + Shift click to remove this item");
			newLore.add("&8» &7Middle click to copy item into your inventory");
			maker.lore(newLore);
			int id = i;
			gui.addItem(new ItemGUI(maker.build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					if (click.isLeftClick() && click.isShiftClick()) {
						kit.getContents().remove(id);
						openContents(player, guiPage, kit, page);
					} else if (click.isMiddleClick())
						if (player.getInventory().firstEmpty() != -1)
							player.getInventory().addItem(item);
				}
			});
		}
		if (page != totalPages)
			gui.setItem(41, new ItemGUI(ItemMaker.ofHead().skinValues(
					"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ5OTNiOGMxMzU4ODkxOWI5ZjhiNDJkYjA2NWQ1YWRmZTc4YWYxODI4MTViNGU2ZjBmOTFiYTY4M2RlYWM5In19fQ==")
					.displayName("&fNext page &e&l>>>").lore("", "&8» &7Click to continue to next page", "").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					openContents(player, guiPage, kit, page + 1);
				}
			});
		if (page != 1)
			gui.setItem(37,
					new ItemGUI(ItemMaker.ofHead().skinValues(
							"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjRiZmVmMTRlODQyMGEyNTZlNDU3YTRhN2M4ODExMmUxNzk0ODVlNTIzNDU3ZTQzODUxNzdiYWQifX19")
							.displayName("&e&l<<< &fPrevious page").lore("", "&8» &7Click to return to previous page", "").build()) {
						@Override
						public void onClick(Player player, HolderGUI gui, ClickType click) {
							openContents(player, guiPage, kit, page - 1);
						}
					});
		gui.setItem(52, new ItemGUI(ItemMaker.of(XMaterial.EMERALD).displayName("&aAdd items").lore("", "&8» &7Click to open insert menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI gui, ClickType click) {
				GUI gui2 = new GUI("&fInsert items", 54) {
					@Override
					public void onClose(Player player) {
						for (int i = 0; i < 45; ++i) {
							ItemStack item = gui.getItem(i);
							if (item != null && item.getType() != Material.AIR)
								kit.getContents().add(item);
						}
					}
				};
				for (int i = 45; i < 54; ++i)
					gui2.setItem(i, empty);
				gui2.setItem(49, new ItemGUI(ItemMaker.of(XMaterial.YELLOW_STAINED_GLASS_PANE).displayName("&eReturn back").lore("", "&8» &7Click to return back to previous menu", "").build()) {
					@Override
					public void onClick(Player player, HolderGUI guir, ClickType click) {
						openContents(player, guiPage, kit, page);
					}
				});
				gui2.open(player);
			}
		});
		gui.open(player);
	}

	protected void openCommands(Player player, int guiPage, KitSample kit, int requestedPage) {
		int totalPages = kit.getCommands().size() / 36 + (kit.getCommands().size() % 36 == 0 ? 0 : 1);
		if (requestedPage > totalPages)
			requestedPage = totalPages;
		if (requestedPage < 1)
			requestedPage = 1;
		final int page = requestedPage;
		GUI gui = new GUI("&fKit Commands of " + kit.getName() + " " + page + "/" + totalPages, 54);
		for (int i = 0; i < 9; ++i)
			gui.setItem(i, empty);
		for (int i = 45; i < 54; ++i)
			gui.setItem(i, empty);
		gui.setItem(49, new ItemGUI(ItemMaker.of(XMaterial.YELLOW_STAINED_GLASS_PANE).displayName("&eReturn back").lore("", "&8» &7Click to return back to previous menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openEditorOf(player, guiPage, kit);
			}
		});
		String[] items = kit.getCommands().toArray(new String[0]);
		for (int i = page * 36 - 36; i < page * 36; ++i) {
			if (items.length <= i || items[i] == null)
				break;
			String item = items[i];
			int id = i;
			gui.addItem(new ItemGUI(ItemMaker.of(XMaterial.PAPER).displayName("&fCommand #" + i).lore("", "&8» &f" + item, "", "&8» &7Left + Shift click to remove this command").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					if (click.isLeftClick() && click.isShiftClick()) {
						kit.getCommands().remove(id);
						openContents(player, guiPage, kit, page);
					}
				}
			});
		}
		if (page != totalPages)
			gui.setItem(41, new ItemGUI(ItemMaker.ofHead().skinValues(
					"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ5OTNiOGMxMzU4ODkxOWI5ZjhiNDJkYjA2NWQ1YWRmZTc4YWYxODI4MTViNGU2ZjBmOTFiYTY4M2RlYWM5In19fQ==")
					.displayName("&fNext page &e&l>>>").lore("", "&8» &7Click to continue to next page", "").build()) {
				@Override
				public void onClick(Player player, HolderGUI gui, ClickType click) {
					openCommands(player, guiPage, kit, page + 1);
				}
			});
		if (page != 1)
			gui.setItem(37,
					new ItemGUI(ItemMaker.ofHead().skinValues(
							"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjRiZmVmMTRlODQyMGEyNTZlNDU3YTRhN2M4ODExMmUxNzk0ODVlNTIzNDU3ZTQzODUxNzdiYWQifX19")
							.displayName("&e&l<<< &fPrevious page").lore("", "&8» &7Click to return to previous page", "").build()) {
						@Override
						public void onClick(Player player, HolderGUI gui, ClickType click) {
							openCommands(player, guiPage, kit, page - 1);
						}
					});
		gui.setItem(52, new ItemGUI(ItemMaker.of(XMaterial.WRITABLE_BOOK).displayName("&aAdd command")
				.lore("", "&8» &7Click to add new command", "&8» &7Type command without slash (/)", "&8» &7Use {player} as placeholder", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				AnvilGUI anvil = new AnvilGUI("&fType message") {
					@Override
					public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
						if (guiClick) {
							if (getRenameText() != null && !getRenameText().trim().isEmpty())
								kit.getCommands().add(getRenameText());
							close(player);
						}
						return false;
					}

					@Override
					public void onClose(Player player) {
						openCommands(player, guiPage, kit, page);
					}
				};
				anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.PAPER).displayName("Command Here").build()));
				anvil.open(player);
			}
		});
		gui.open(player);
	}

	public void openKitSettings(Player player, int page, KitSample kit) {
		GUI gui = new GUI("&fKit Settings of " + kit.getName(), 54);
		for (int i = 0; i < 9; ++i)
			gui.setItem(i, empty);
		for (int i = 45; i < 54; ++i)
			gui.setItem(i, empty);
		gui.setItem(49, new ItemGUI(ItemMaker.of(XMaterial.YELLOW_STAINED_GLASS_PANE).displayName("&eReturn back").lore("", "&8» &7Click to return back to previous menu", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				openEditorOf(player, page, kit);
			}
		});
		gui.setItem(19, new ItemGUI(ItemMaker.of(XMaterial.GOLD_INGOT).displayName("&6Cost").lore("", "&8» &7Cost: &e" + kit.getCost(), "", "&8» &7Click to change cost", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				ItemGUI item = this;
				AnvilGUI anvil = new AnvilGUI("&fTime") {
					@Override
					public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
						if (guiClick) {
							kit.setCost(getRenameText() == null || getRenameText().trim().isEmpty() ? 0 : Economy.multipleByMoneyFormat(ParseUtils.getDouble(getRenameText()), getRenameText()));
							item.setItem(ItemMaker.of(XMaterial.GOLD_INGOT).displayName("&6Cost").lore("", "&8» &7Cost: &e" + kit.getCost(), "", "&8» &7Click to change cost", "").build());
							gui.setItem(19, item);
							close(player);
						}
						return false;
					}

					@Override
					public void onClose(Player player) {
						gui.open(player);
					}
				};
				anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.CLOCK).displayName(TimeUtils.timeToString(kit.getCooldown().getTime())).build()));
				anvil.open(player);
			}
		});
		gui.setItem(22, new ItemGUI(ItemMaker.of(XMaterial.ENDER_PEARL).displayName("&6Cooldown").lore("", "&8» &7Bypass Permission: &e" + kit.getCooldown().getBypassPerm(),
				"&8» &7Time: &e" + kit.getCooldown().getTime(), "", "&8» &7Left click to change bypass permission", "&8» &7Right click to change time", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				ItemGUI item = this;
				if (click.isLeftClick()) {
					AnvilGUI anvil = new AnvilGUI("&fBypass Permission") {
						@Override
						public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
							if (guiClick) {
								kit.getCooldown().setBypassPerm(getRenameText() == null || getRenameText().trim().isEmpty() ? null : getRenameText());
								item.setItem(
										ItemMaker
												.of(XMaterial.ENDER_PEARL).displayName("&6Cooldown").lore("", "&8» &7Bypass Permission: &e" + kit.getCooldown().getBypassPerm(),
														"&8» &7Time: &e" + kit.getCooldown().getTime(), "", "&8» &7Left click to change bypass permission", "&8» &7Right click to change time", "")
												.build());
								gui.setItem(22, item);
								close(player);
							}
							return false;
						}

						@Override
						public void onClose(Player player) {
							gui.open(player);
						}
					};
					anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.NAME_TAG).displayName(kit.getCooldown().getBypassPerm() == null ? "css.cooldown.kits" : kit.getPermission()).build()));
					anvil.open(player);
				} else {
					AnvilGUI anvil = new AnvilGUI("&fTime") {
						@Override
						public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
							if (guiClick) {
								kit.getCooldown().setTime(getRenameText() == null || getRenameText().trim().isEmpty() ? 0 : TimeUtils.timeFromString(getRenameText()));
								item.setItem(
										ItemMaker
												.of(XMaterial.ENDER_PEARL).displayName("&6Cooldown").lore("", "&8» &7Bypass Permission: &e" + kit.getCooldown().getBypassPerm(),
														"&8» &7Time: &e" + kit.getCooldown().getTime(), "", "&8» &7Left click to change bypass permission", "&8» &7Right click to change time", "")
												.build());
								gui.setItem(22, item);
								close(player);
							}
							return false;
						}

						@Override
						public void onClose(Player player) {
							gui.open(player);
						}
					};
					anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.CLOCK).displayName(TimeUtils.timeToString(kit.getCooldown().getTime())).build()));
					anvil.open(player);
				}
			}
		});
		gui.setItem(24, new ItemGUI(
				ItemMaker.of(XMaterial.BARRIER).displayName("&cPermission").lore("", "&8» &7Permission: &e" + kit.getPermission(), "", "&8» &7Click to change kit permission", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				ItemGUI item = this;
				AnvilGUI anvil = new AnvilGUI("&fKit Permission") {
					@Override
					public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
						if (guiClick) {
							kit.setPermission(getRenameText() == null || getRenameText().trim().isEmpty() ? null : getRenameText());
							item.setItem(ItemMaker.of(XMaterial.BARRIER).displayName("&cPermission")
									.lore("", "&8» &7Permission: &e" + kit.getPermission(), "", "&8» &7Click to change kit permission", "").build());
							gui.setItem(24, item);
							close(player);
						}
						return false;
					}

					@Override
					public void onClose(Player player) {
						gui.open(player);
					}
				};
				anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.NAME_TAG).displayName(kit.getPermission() == null ? "css.kit." + kit.getName() : kit.getPermission()).build()));
				anvil.open(player);
			}
		});
		gui.setItem(30, new ItemGUI(
				ItemMaker.of(XMaterial.INK_SAC).displayName("&dOverride contents in slots").lore("", "&8» &7Status: &e" + kit.isOverrideContents(), "", "&8» &7Click to toggle status", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				kit.setOverrideContents(!kit.isOverrideContents());
				setItem(ItemMaker.of(XMaterial.INK_SAC).displayName("&dOverride contents in slots").lore("", "&8» &7Status: &e" + kit.isOverrideContents(), "", "&8» &7Click to toggle status", "")
						.build());
				gui.setItem(30, this);
			}
		});
		gui.setItem(32, new ItemGUI(ItemMaker.of(XMaterial.FEATHER).displayName("&fDrops items on the ground if inventory is full")
				.lore("", "&8» &7Status: &e" + kit.isDropItems(), "", "&8» &7Click to toggle status", "").build()) {
			@Override
			public void onClick(Player player, HolderGUI guir, ClickType click) {
				kit.setDropItems(!kit.isDropItems());
				setItem(ItemMaker.of(XMaterial.FEATHER).displayName("&fDrops items on the ground if inventory is full")
						.lore("", "&8» &7Status: &e" + kit.isDropItems(), "", "&8» &7Click to toggle status", "").build());
				gui.setItem(32, this);
			}
		});
		gui.open(player);
	}

	public void openKitCreator(Player player, int page) {
		AnvilGUI anvil = new AnvilGUI("&fType kit name") {
			@Override
			public boolean onInteractItem(Player player, ItemStack newItem, ItemStack oldItem, ClickType type, int slot, boolean guiClick) {
				if (guiClick && getRenameText() != null && !getRenameText().trim().isEmpty()) {
					KitSample kit = ((Kit) API.get().getCommandManager().getRegistered().get("kit")).getKits().get(getRenameText().replace(" ", ""));
					if (kit == null) {
						kit = new KitSample(getRenameText().replace(" ", ""));
						((Kit) API.get().getCommandManager().getRegistered().get("kit")).getKits().put(kit.getName(), kit);
						// save
						Config config = API.get().getConfigManager().getKits();
						config.set(kit.getName() + ".permission", kit.getPermission());
						config.set(kit.getName() + ".settings.cost", kit.getCost());
						config.set(kit.getName() + ".settings.override-contents-in-slots", kit.isOverrideContents());
						config.set(kit.getName() + ".settings.drop-items-when-full-inv", kit.isDropItems());
						config.set(kit.getName() + ".settings.cooldown.bypass-perm", kit.getCooldown().getBypassPerm());
						config.set(kit.getName() + ".settings.cooldown.time", TimeUtils.timeToString(kit.getCooldown().getTime()));
						config.set(kit.getName() + ".messages", kit.getMessages());
						config.set(kit.getName() + ".commands", kit.getCommands());
						List<String> contents = new ArrayList<>();
						for (ItemStack stack : kit.getContents()) {
							@SuppressWarnings("unchecked")
							Map<String, Object> map = (Map<String, Object>) Json.writer().writeWithoutParse(stack);
							String material = map.remove("type").toString();
							if (map.isEmpty())
								contents.add(material);
							else
								contents.add(material + Json.writer().simpleWrite(map));
						}
						config.set(kit.getName() + ".contents", contents);
						config.save("yaml");
					}
					openEditorOf(player, page, kit);
				}
				return false;
			}

		};
		anvil.setItem(0, new EmptyItemGUI(ItemMaker.of(XMaterial.NAME_TAG).displayName("Kit Name Here").build()));
		anvil.open(player);
	}
}
