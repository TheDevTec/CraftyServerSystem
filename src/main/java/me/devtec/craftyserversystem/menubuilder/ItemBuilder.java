package me.devtec.craftyserversystem.menubuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.InternalPlaceholders;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.game.itemmakers.HeadItemMaker;

public class ItemBuilder {

	private final char character;
	private Function<Player, ItemResult> itemResult;
	private String path;
	private long refleshInterval;

	private Map<Integer, Long> nextTickAt;

	public ItemBuilder(long refleshInterval, Function<Player, ItemResult> itemResult, char character, String path) {
		this.character = character;
		this.refleshInterval = refleshInterval;
		this.itemResult = itemResult;
		this.path = path;

	}

	public boolean canTick(int menuId) {
		return nextTickAt == null || System.currentTimeMillis() / 50 - nextTickAt.getOrDefault(menuId, 0L) <= 0;
	}

	public void clearTick(int menuId) {
		if (nextTickAt != null)
			nextTickAt.remove(menuId);
	}

	public void tick(int menuId) {
		if (nextTickAt == null || refleshInterval == 0)
			return;
		nextTickAt.put(menuId, System.currentTimeMillis() / 50 + refleshInterval);
	}

	public String getPath() {
		return path;
	}

	public char getId() {
		return character;
	}

	public Function<Player, ItemResult> getItemResultFunction() {
		return itemResult;
	}

	public ItemResult getItemResult(Player target) {
		ItemResult result = itemResult.apply(target);
		if (result.containPlaceholders()) {
			PlaceholdersExecutor placeholders = InternalPlaceholders.generatePlaceholders(target);
			if (result.getCooldown() != null)
				placeholders.add("time", TimeUtils.timeToString(result.getCooldown().remainingTime(target)));
			if (result.getItem().getDisplayName() != null)
				result.getItem().displayName(placeholders.apply(result.getItem().getDisplayName()));
			if (result.getItem().getLore() != null)
				result.getItem().getLore().replaceAll(lore -> placeholders.apply(lore));
			if (result.getItem() instanceof HeadItemMaker) {
				HeadItemMaker maker = (HeadItemMaker) result.getItem();
				if (maker.getHeadOwner() != null && maker.getHeadOwnerType() == 0)
					maker.skinName(PlaceholderAPI.apply(maker.getHeadOwner().replace("{player}", target.getName()),
							target.getUniqueId()));
			}
		}
		return result;
	}

	public void setItemResultFunction(Function<Player, ItemResult> itemResult) {
		this.itemResult = itemResult;
	}

	public long getRefleshInterval() {
		return refleshInterval;
	}

	public void setRefleshInterval(long refleshInterval) {
		this.refleshInterval = refleshInterval;
		if (refleshInterval == 0)
			nextTickAt = null;
		else
			nextTickAt = new ConcurrentHashMap<>();
	}

	@Override
	public int hashCode() {
		return 8 + 8 * path.hashCode() + 8 * (int) refleshInterval + 8 * character;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ItemBuilder
				? ((ItemBuilder) obj).path.equals(path) && ((ItemBuilder) obj).refleshInterval == refleshInterval
						&& ((ItemBuilder) obj).character == character
				: false;
	}

	public static ItemBuilder build(char character, String path, Config config) {
		long refleshEvery = !config.existsKey(path + ".refleshEvery") ? 0
				: config.getString(path + ".refleshEvery").endsWith("t") ? config.getLong(path + ".refleshEvery")
						: TimeUtils.timeFromString(config.getString(path + ".refleshEvery")) * 20;
		if (config.getString(path + ".cooldown") != null) {
			CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(config.getString(path + ".cooldown"));
			if (cooldown != null) {
				Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

					private ItemResult[] cache = new ItemResult[2];

					@Override
					public ItemResult apply(Player player) {
						boolean status = cooldown.tryWithoutWriting(player);
						ItemResult result = cache[status ? 1 : 0];
						if (result == null) {
							result = createItemResult(status);
							if (!result.containPlaceholders())
								cache[status ? 1 : 0] = result;
						}
						return result;
					}

					private ItemResult createItemResult(boolean canUse) {
						if (canUse) {
							ItemMaker maker = ItemMaker.loadMakerFromConfig(config, path + ".item.canUse");
							if (maker == null)
								Loader.getPlugin().getLogger().warning(
										"[CssGui] Failed to load ItemMaker from config path " + path + ".item.canUse");
							return ItemResult.of(maker, config.getStringList(path + ".item.canUse.onUse.commands"),
									config.getStringList(path + ".item.canUse.onUse.messages"), cooldown,
									ButtonType.parse(config.getString(path + ".action")),
									parseId(config.getString(path + ".action", "")),
									config.getBoolean(path + ".item.canUse.onUse.updateItem"),
									config.getBoolean(path + ".item.canUse.onUse.updateInventory"));
						}
						ItemMaker maker = ItemMaker.loadMakerFromConfig(config, path + ".item.onCooldown");
						if (maker == null)
							Loader.getPlugin().getLogger().warning(
									"[CssGui] Failed to load ItemMaker from config path " + path + ".item.onCooldown");
						return ItemResult.of(maker, config.getStringList(path + ".item.onCooldown.onUse.commands"),
								config.getStringList(path + ".item.onCooldown.onUse.messages"), cooldown,
								ButtonType.parse(config.getString(path + ".action")),
								parseId(config.getString(path + ".action", "")),
								config.getBoolean(path + ".item.onCooldown.onUse.updateItem"),
								config.getBoolean(path + ".item.onCooldown.onUse.updateInventory"));
					}
				};
				return new ItemBuilder(refleshEvery, itemMaker, character, path);
			}
		}
		if (config.exists(path + ".predicate")) {
			String placeholder = config.getString(path + ".predicate.placeholder");
			String condition = config.getString(path + ".predicate.condition");
			if (condition != null)
				switch (condition.charAt(0)) {
				case '>':
					double number;
					switch (condition.charAt(1)) {
					case '=':
						number = ParseUtils.getDouble(condition, 2, condition.length());
						Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

							private ItemResult[] cache = new ItemResult[2];

							@Override
							public ItemResult apply(Player player) {
								boolean status = number >= ParseUtils
										.getDouble(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
								String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
										: status + "";
								ItemResult result = cache[status ? 1 : 0];
								if (result == null) {
									result = createItemResult(player, subpath);
									if (!result.containPlaceholders())
										cache[status ? 1 : 0] = result;
								}
								return result;
							}

							private ItemResult createItemResult(Player player, String subpath) {
								CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
										config.getString(path + ".predicate.result." + subpath + ".cooldown"));
								if (cooldown != null) {
									String finalPath = cooldown.tryWithoutWriting(player)
											? path + ".predicate.result." + subpath + ".item.canUse"
											: path + ".predicate.result." + subpath + ".item.onCooldown";
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
									if (maker == null)
										Loader.getPlugin().getLogger().warning(
												"[CssGui] Failed to load ItemMaker from config path " + finalPath);
									return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
											config.getStringList(finalPath + ".onUse.messages"), cooldown,
											ButtonType.parse(config.getString(finalPath + ".action")),
											parseId(config.getString(finalPath + ".action", "")),
											config.getBoolean(finalPath + ".onUse.updateItem"),
											config.getBoolean(finalPath + ".onUse.updateInventory"));
								}
								ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
										path + ".predicate.result." + subpath + ".item");
								if (maker == null)
									Loader.getPlugin().getLogger()
											.warning("[CssGui] Failed to load ItemMaker from config path " + path
													+ ".predicate.result." + subpath + ".item");
								return ItemResult.of(maker,
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"),
										cooldown,
										ButtonType.parse(
												config.getString(path + ".predicate.result." + subpath + ".action")),
										parseId(config.getString(path + ".predicate.result." + subpath + ".action",
												"")),
										config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
										config.getBoolean(
												path + ".predicate.result." + subpath + ".onUse.updateInventory"));
							}
						};
						return new ItemBuilder(refleshEvery, itemMaker, character, path);
					default:
						number = ParseUtils.getDouble(condition, 1, condition.length());
						itemMaker = new Function<Player, ItemResult>() {

							private ItemResult[] cache = new ItemResult[2];

							@Override
							public ItemResult apply(Player player) {
								boolean status = number > ParseUtils
										.getDouble(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
								player.sendMessage(number + ":"
										+ ParseUtils.getDouble(PlaceholderAPI.apply(placeholder, player.getUniqueId()))
										+ ":" + status);
								String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
										: status + "";
								ItemResult result = cache[status ? 1 : 0];
								if (result == null) {
									result = createItemResult(player, subpath);
									if (!result.containPlaceholders())
										cache[status ? 1 : 0] = result;
								}
								return result;
							}

							private ItemResult createItemResult(Player player, String subpath) {
								CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
										config.getString(path + ".predicate.result." + subpath + ".cooldown"));
								if (cooldown != null) {
									String finalPath = cooldown.tryWithoutWriting(player)
											? path + ".predicate.result." + subpath + ".item.canUse"
											: path + ".predicate.result." + subpath + ".item.onCooldown";
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
									if (maker == null)
										Loader.getPlugin().getLogger().warning(
												"[CssGui] Failed to load ItemMaker from config path " + finalPath);
									return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
											config.getStringList(finalPath + ".onUse.messages"), cooldown,
											ButtonType.parse(config.getString(finalPath + ".action")),
											parseId(config.getString(finalPath + ".action", "")),
											config.getBoolean(finalPath + ".onUse.updateItem"),
											config.getBoolean(finalPath + ".onUse.updateInventory"));
								}
								ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
										path + ".predicate.result." + subpath + ".item");
								if (maker == null)
									Loader.getPlugin().getLogger()
											.warning("[CssGui] Failed to load ItemMaker from config path " + path
													+ ".predicate.result." + subpath + ".item");
								return ItemResult.of(maker,
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"),
										cooldown,
										ButtonType.parse(
												config.getString(path + ".predicate.result." + subpath + ".action")),
										parseId(config.getString(path + ".predicate.result." + subpath + ".action",
												"")),
										config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
										config.getBoolean(
												path + ".predicate.result." + subpath + ".onUse.updateInventory"));
							}
						};
						return new ItemBuilder(refleshEvery, itemMaker, character, path);
					}
				case '<':
					switch (condition.charAt(1)) {
					case '=':
						number = ParseUtils.getDouble(condition, 2, condition.length());
						Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

							private ItemResult[] cache = new ItemResult[2];

							@Override
							public ItemResult apply(Player player) {
								boolean status = number <= ParseUtils
										.getDouble(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
								String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
										: status + "";
								ItemResult result = cache[status ? 1 : 0];
								if (result == null) {
									result = createItemResult(player, subpath);
									if (!result.containPlaceholders())
										cache[status ? 1 : 0] = result;
								}
								return result;
							}

							private ItemResult createItemResult(Player player, String subpath) {
								CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
										config.getString(path + ".predicate.result." + subpath + ".cooldown"));
								if (cooldown != null) {
									String finalPath = cooldown.tryWithoutWriting(player)
											? path + ".predicate.result." + subpath + ".item.canUse"
											: path + ".predicate.result." + subpath + ".item.onCooldown";
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
									if (maker == null)
										Loader.getPlugin().getLogger().warning(
												"[CssGui] Failed to load ItemMaker from config path " + finalPath);
									return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
											config.getStringList(finalPath + ".onUse.messages"), cooldown,
											ButtonType.parse(config.getString(finalPath + ".action")),
											parseId(config.getString(finalPath + ".action", "")),
											config.getBoolean(finalPath + ".onUse.updateItem"),
											config.getBoolean(finalPath + ".onUse.updateInventory"));
								}
								ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
										path + ".predicate.result." + subpath + ".item");
								if (maker == null)
									Loader.getPlugin().getLogger()
											.warning("[CssGui] Failed to load ItemMaker from config path " + path
													+ ".predicate.result." + subpath + ".item");
								return ItemResult.of(maker,
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"),
										cooldown,
										ButtonType.parse(
												config.getString(path + ".predicate.result." + subpath + ".action")),
										parseId(config.getString(path + ".predicate.result." + subpath + ".action",
												"")),
										config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
										config.getBoolean(
												path + ".predicate.result." + subpath + ".onUse.updateInventory"));
							}
						};
						return new ItemBuilder(refleshEvery, itemMaker, character, path);
					default:
						number = ParseUtils.getDouble(condition, 1, condition.length());
						itemMaker = new Function<Player, ItemResult>() {

							private ItemResult[] cache = new ItemResult[2];

							@Override
							public ItemResult apply(Player player) {
								boolean status = number < ParseUtils
										.getDouble(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
								String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
										: status + "";
								ItemResult result = cache[status ? 1 : 0];
								if (result == null) {
									result = createItemResult(player, subpath);
									if (!result.containPlaceholders())
										cache[status ? 1 : 0] = result;
								}
								return result;
							}

							private ItemResult createItemResult(Player player, String subpath) {
								CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
										config.getString(path + ".predicate.result." + subpath + ".cooldown"));
								if (cooldown != null) {
									String finalPath = cooldown.tryWithoutWriting(player)
											? path + ".predicate.result." + subpath + ".item.canUse"
											: path + ".predicate.result." + subpath + ".item.onCooldown";
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
									if (maker == null)
										Loader.getPlugin().getLogger().warning(
												"[CssGui] Failed to load ItemMaker from config path " + finalPath);
									return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
											config.getStringList(finalPath + ".onUse.messages"), cooldown,
											ButtonType.parse(config.getString(finalPath + ".action")),
											parseId(config.getString(finalPath + ".action", "")),
											config.getBoolean(finalPath + ".onUse.updateItem"),
											config.getBoolean(finalPath + ".onUse.updateInventory"));
								}
								ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
										path + ".predicate.result." + subpath + ".item");
								if (maker == null)
									Loader.getPlugin().getLogger()
											.warning("[CssGui] Failed to load ItemMaker from config path " + path
													+ ".predicate.result." + subpath + ".item");
								return ItemResult.of(maker,
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"),
										cooldown,
										ButtonType.parse(
												config.getString(path + ".predicate.result." + subpath + ".action")),
										parseId(config.getString(path + ".predicate.result." + subpath + ".action",
												"")),
										config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
										config.getBoolean(
												path + ".predicate.result." + subpath + ".onUse.updateInventory"));
							}
						};
						return new ItemBuilder(refleshEvery, itemMaker, character, path);
					}
				case '=':
					String finalCond = condition.substring(2);
					boolean isNumber = ParseUtils.isNumber(finalCond);
					number = isNumber ? ParseUtils.getDouble(finalCond) : 0;
					switch (condition.charAt(1)) {
					case '~':
						if (isNumber) {
							Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

								private ItemResult[] cache = new ItemResult[2];

								@Override
								public ItemResult apply(Player player) {
									boolean status = ParseUtils.getDouble(
											PlaceholderAPI.apply(placeholder, player.getUniqueId())) == number;
									String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
											: status + "";
									ItemResult result = cache[status ? 1 : 0];
									if (result == null) {
										result = createItemResult(player, subpath);
										if (!result.containPlaceholders())
											cache[status ? 1 : 0] = result;
									}
									return result;
								}

								private ItemResult createItemResult(Player player, String subpath) {
									CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
											config.getString(path + ".predicate.result." + subpath + ".cooldown"));
									if (cooldown != null) {
										String finalPath = cooldown.tryWithoutWriting(player)
												? path + ".predicate.result." + subpath + ".item.canUse"
												: path + ".predicate.result." + subpath + ".item.onCooldown";
										ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
										if (maker == null)
											Loader.getPlugin().getLogger().warning(
													"[CssGui] Failed to load ItemMaker from config path " + finalPath);
										return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
												config.getStringList(finalPath + ".onUse.messages"), cooldown,
												ButtonType.parse(config.getString(finalPath + ".action")),
												parseId(config.getString(finalPath + ".action", "")),
												config.getBoolean(finalPath + ".onUse.updateItem"),
												config.getBoolean(finalPath + ".onUse.updateInventory"));
									}
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
											path + ".predicate.result." + subpath + ".item");
									if (maker == null)
										Loader.getPlugin().getLogger()
												.warning("[CssGui] Failed to load ItemMaker from config path " + path
														+ ".predicate.result." + subpath + ".item");
									return ItemResult.of(maker,
											config.getStringList(
													path + ".predicate.result." + subpath + ".onUse.commands"),
											config.getStringList(
													path + ".predicate.result." + subpath + ".onUse.messages"),
											cooldown,
											ButtonType.parse(config
													.getString(path + ".predicate.result." + subpath + ".action")),
											parseId(config.getString(path + ".predicate.result." + subpath + ".action",
													"")),
											config.getBoolean(
													path + ".predicate.result." + subpath + ".onUse.updateItem"),
											config.getBoolean(
													path + ".predicate.result." + subpath + ".onUse.updateInventory"));
								}
							};
							return new ItemBuilder(refleshEvery, itemMaker, character, path);
						} else {
							Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

								private ItemResult[] cache = new ItemResult[2];

								@Override
								public ItemResult apply(Player player) {
									boolean status = finalCond
											.equalsIgnoreCase(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
									String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
											: status + "";
									ItemResult result = cache[status ? 1 : 0];
									if (result == null) {
										result = createItemResult(player, subpath);
										if (!result.containPlaceholders())
											cache[status ? 1 : 0] = result;
									}
									return result;
								}

								private ItemResult createItemResult(Player player, String subpath) {
									CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
											config.getString(path + ".predicate.result." + subpath + ".cooldown"));
									if (cooldown != null) {
										String finalPath = cooldown.tryWithoutWriting(player)
												? path + ".predicate.result." + subpath + ".item.canUse"
												: path + ".predicate.result." + subpath + ".item.onCooldown";
										ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
										if (maker == null)
											Loader.getPlugin().getLogger().warning(
													"[CssGui] Failed to load ItemMaker from config path " + finalPath);
										return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
												config.getStringList(finalPath + ".onUse.messages"), cooldown,
												ButtonType.parse(config.getString(finalPath + ".action")),
												parseId(config.getString(finalPath + ".action", "")),
												config.getBoolean(finalPath + ".onUse.updateItem"),
												config.getBoolean(finalPath + ".onUse.updateInventory"));
									}
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
											path + ".predicate.result." + subpath + ".item");
									if (maker == null)
										Loader.getPlugin().getLogger()
												.warning("[CssGui] Failed to load ItemMaker from config path " + path
														+ ".predicate.result." + subpath + ".item");
									return ItemResult.of(maker,
											config.getStringList(
													path + ".predicate.result." + subpath + ".onUse.commands"),
											config.getStringList(
													path + ".predicate.result." + subpath + ".onUse.messages"),
											cooldown,
											ButtonType.parse(config
													.getString(path + ".predicate.result." + subpath + ".action")),
											parseId(config.getString(path + ".predicate.result." + subpath + ".action",
													"")),
											config.getBoolean(
													path + ".predicate.result." + subpath + ".onUse.updateItem"),
											config.getBoolean(
													path + ".predicate.result." + subpath + ".onUse.updateInventory"));
								}
							};
							return new ItemBuilder(refleshEvery, itemMaker, character, path);
						}
					default:
						if (isNumber) {
							Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

								private ItemResult[] cache = new ItemResult[2];

								@Override
								public ItemResult apply(Player player) {
									boolean status = ParseUtils.getDouble(
											PlaceholderAPI.apply(placeholder, player.getUniqueId())) == number;
									String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
											: status + "";
									ItemResult result = cache[status ? 1 : 0];
									if (result == null) {
										result = createItemResult(player, subpath);
										if (!result.containPlaceholders())
											cache[status ? 1 : 0] = result;
									}
									return result;
								}

								private ItemResult createItemResult(Player player, String subpath) {
									CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
											config.getString(path + ".predicate.result." + subpath + ".cooldown"));
									if (cooldown != null) {
										String finalPath = cooldown.tryWithoutWriting(player)
												? path + ".predicate.result." + subpath + ".item.canUse"
												: path + ".predicate.result." + subpath + ".item.onCooldown";
										ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
										if (maker == null)
											Loader.getPlugin().getLogger().warning(
													"[CssGui] Failed to load ItemMaker from config path " + finalPath);
										return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
												config.getStringList(finalPath + ".onUse.messages"), cooldown,
												ButtonType.parse(config.getString(finalPath + ".action")),
												parseId(config.getString(finalPath + ".action", "")),
												config.getBoolean(finalPath + ".onUse.updateItem"),
												config.getBoolean(finalPath + ".onUse.updateInventory"));
									}
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
											path + ".predicate.result." + subpath + ".item");
									if (maker == null)
										Loader.getPlugin().getLogger()
												.warning("[CssGui] Failed to load ItemMaker from config path " + path
														+ ".predicate.result." + subpath + ".item");
									return ItemResult.of(maker,
											config.getStringList(
													path + ".predicate.result." + subpath + ".onUse.commands"),
											config.getStringList(
													path + ".predicate.result." + subpath + ".onUse.messages"),
											cooldown,
											ButtonType.parse(config
													.getString(path + ".predicate.result." + subpath + ".action")),
											parseId(config.getString(path + ".predicate.result." + subpath + ".action",
													"")),
											config.getBoolean(
													path + ".predicate.result." + subpath + ".onUse.updateItem"),
											config.getBoolean(
													path + ".predicate.result." + subpath + ".onUse.updateInventory"));
								}
							};
							return new ItemBuilder(refleshEvery, itemMaker, character, path);
						}
						Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

							private ItemResult[] cache = new ItemResult[2];

							@Override
							public ItemResult apply(Player player) {
								boolean status = finalCond
										.equals(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
								String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
										: status + "";
								ItemResult result = cache[status ? 1 : 0];
								if (result == null) {
									result = createItemResult(player, subpath);
									if (!result.containPlaceholders())
										cache[status ? 1 : 0] = result;
								}
								return result;
							}

							private ItemResult createItemResult(Player player, String subpath) {
								CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
										config.getString(path + ".predicate.result." + subpath + ".cooldown"));
								if (cooldown != null) {
									String finalPath = cooldown.tryWithoutWriting(player)
											? path + ".predicate.result." + subpath + ".item.canUse"
											: path + ".predicate.result." + subpath + ".item.onCooldown";
									ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
									if (maker == null)
										Loader.getPlugin().getLogger().warning(
												"[CssGui] Failed to load ItemMaker from config path " + finalPath);
									return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
											config.getStringList(finalPath + ".onUse.messages"), cooldown,
											ButtonType.parse(config.getString(finalPath + ".action")),
											parseId(config.getString(finalPath + ".action", "")),
											config.getBoolean(finalPath + ".onUse.updateItem"),
											config.getBoolean(finalPath + ".onUse.updateInventory"));
								}
								ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
										path + ".predicate.result." + subpath + ".item");
								if (maker == null)
									Loader.getPlugin().getLogger()
											.warning("[CssGui] Failed to load ItemMaker from config path " + path
													+ ".predicate.result." + subpath + ".item");
								return ItemResult.of(maker,
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
										config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"),
										cooldown,
										ButtonType.parse(
												config.getString(path + ".predicate.result." + subpath + ".action")),
										parseId(config.getString(path + ".predicate.result." + subpath + ".action",
												"")),
										config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
										config.getBoolean(
												path + ".predicate.result." + subpath + ".onUse.updateInventory"));
							}
						};
						return new ItemBuilder(refleshEvery, itemMaker, character, path);
					}
				case '!':
					finalCond = condition.substring(2);
					Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

						private ItemResult[] cache = new ItemResult[2];

						@Override
						public ItemResult apply(Player player) {
							boolean status = !finalCond.equals(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
							String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT"
									: status + "";
							ItemResult result = cache[status ? 1 : 0];
							if (result == null) {
								result = createItemResult(player, subpath);
								if (!result.containPlaceholders())
									cache[status ? 1 : 0] = result;
							}
							return result;
						}

						private ItemResult createItemResult(Player player, String subpath) {
							CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(
									config.getString(path + ".predicate.result." + subpath + ".cooldown"));
							if (cooldown != null) {
								String finalPath = cooldown.tryWithoutWriting(player)
										? path + ".predicate.result." + subpath + ".item.canUse"
										: path + ".predicate.result." + subpath + ".item.onCooldown";
								ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
								if (maker == null)
									Loader.getPlugin().getLogger()
											.warning("[CssGui] Failed to load ItemMaker from config path " + finalPath);
								return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
										config.getStringList(finalPath + ".onUse.messages"), cooldown,
										ButtonType.parse(config.getString(finalPath + ".action")),
										parseId(config.getString(finalPath + ".action", "")),
										config.getBoolean(finalPath + ".onUse.updateItem"),
										config.getBoolean(finalPath + ".onUse.updateInventory"));
							}
							ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
									path + ".predicate.result." + subpath + ".item");
							if (maker == null)
								Loader.getPlugin().getLogger()
										.warning("[CssGui] Failed to load ItemMaker from config path " + path
												+ ".predicate.result." + subpath + ".item");
							return ItemResult.of(maker,
									config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
									config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"),
									cooldown,
									ButtonType
											.parse(config.getString(path + ".predicate.result." + subpath + ".action")),
									parseId(config.getString(path + ".predicate.result." + subpath + ".action", "")),
									config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
									config.getBoolean(
											path + ".predicate.result." + subpath + ".onUse.updateInventory"));
						}
					};
					return new ItemBuilder(refleshEvery, itemMaker, character, path);
				}
			Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

				private Map<String, ItemResult> cache = new ConcurrentHashMap<>();

				@Override
				public ItemResult apply(Player player) {
					String status = PlaceholderAPI.apply(placeholder, player.getUniqueId());
					String subpath = !config.exists(path + ".predicate.result." + status) ? "_DEFAULT" : status + "";
					ItemResult result = cache.get(subpath);
					if (result == null) {
						result = createItemResult(player, subpath);
						if (!result.containPlaceholders())
							cache.put(subpath, result);
					}
					return result;
				}

				private ItemResult createItemResult(Player player, String subpath) {
					CooldownHolder cooldown = API.get().getCooldownManager()
							.getOrPrepare(config.getString(path + ".predicate.result." + subpath + ".cooldown"));
					if (cooldown != null) {
						String finalPath = cooldown.tryWithoutWriting(player)
								? path + ".predicate.result." + subpath + ".item.canUse"
								: path + ".predicate.result." + subpath + ".item.onCooldown";
						ItemMaker maker = ItemMaker.loadMakerFromConfig(config, finalPath);
						if (maker == null)
							Loader.getPlugin().getLogger()
									.warning("[CssGui] Failed to load ItemMaker from config path " + finalPath);
						return ItemResult.of(maker, config.getStringList(finalPath + ".onUse.commands"),
								config.getStringList(finalPath + ".onUse.messages"), cooldown,
								ButtonType.parse(config.getString(finalPath + ".action")),
								parseId(config.getString(finalPath + ".action", "")),
								config.getBoolean(finalPath + ".onUse.updateItem"),
								config.getBoolean(finalPath + ".onUse.updateInventory"));
					}
					ItemMaker maker = ItemMaker.loadMakerFromConfig(config,
							path + ".predicate.result." + subpath + ".item");
					if (maker == null)
						Loader.getPlugin().getLogger().warning("[CssGui] Failed to load ItemMaker from config path "
								+ path + ".predicate.result." + subpath + ".item");
					return ItemResult.of(maker,
							config.getStringList(path + ".predicate.result." + subpath + ".onUse.commands"),
							config.getStringList(path + ".predicate.result." + subpath + ".onUse.messages"), cooldown,
							ButtonType.parse(config.getString(path + ".predicate.result." + subpath + ".action")),
							parseId(config.getString(path + ".predicate.result." + subpath + ".action", "")),
							config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateItem"),
							config.getBoolean(path + ".predicate.result." + subpath + ".onUse.updateInventory"));
				}
			};
			return new ItemBuilder(refleshEvery, itemMaker, character, path);
		}
		Function<Player, ItemResult> itemMaker = new Function<Player, ItemResult>() {

			private ItemResult cache;

			@Override
			public ItemResult apply(Player player) {
				ItemResult result = cache;
				if (result == null) {
					ItemMaker maker = ItemMaker.loadMakerFromConfig(config, path + ".item");
					if (maker == null)
						Loader.getPlugin().getLogger()
								.warning("[CssGui] Failed to load ItemMaker from config path " + path + ".item");
					result = ItemResult.of(maker, config.getStringList(path + ".onUse.commands"),
							config.getStringList(path + ".onUse.messages"), null,
							ButtonType.parse(config.getString(path + ".action")),
							parseId(config.getString(path + ".action", "")),
							config.getBoolean(path + ".onUse.updateItem"),
							config.getBoolean(path + ".onUse.updateInventory"));
					if (!result.containPlaceholders())
						cache = result;
				}
				return result;
			}
		};
		return new ItemBuilder(refleshEvery, itemMaker, character, path);
	}

	private static String parseId(String value) {
		int pos;
		return (pos = value.indexOf('(')) != -1 ? value.substring(pos, value.length() - 1) : null;
	}

	public enum ButtonType {
		NONE, USE_ITEM, OPEN_MENU, CLOSE;

		public static ButtonType parse(String value) {
			value = value == null ? "NONE" : value.toUpperCase();
			if (value.startsWith("OPEN_MENU"))
				return OPEN_MENU;
			return valueOf(value);
		}
	}

}