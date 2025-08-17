package me.devtec.craftyserversystem.api;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.BalanceTop;
import me.devtec.craftyserversystem.commands.internal.Vanish;
import me.devtec.craftyserversystem.commands.internal.bansystem.BanAPI;
import me.devtec.craftyserversystem.economy.CssEconomy;
import me.devtec.craftyserversystem.economy.CssEconomyHook;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.economy.EmptyEconomyHook;
import me.devtec.craftyserversystem.economy.VaultEconomyHook;
import me.devtec.craftyserversystem.events.internal.BossBarListener;
import me.devtec.craftyserversystem.events.internal.ScoreboardListener;
import me.devtec.craftyserversystem.managers.AnimationManager;
import me.devtec.craftyserversystem.managers.CommandManager;
import me.devtec.craftyserversystem.managers.ConfigurationManager;
import me.devtec.craftyserversystem.managers.CooldownManager;
import me.devtec.craftyserversystem.managers.ListenerManager;
import me.devtec.craftyserversystem.managers.MessageManager;
import me.devtec.craftyserversystem.permission.EmptyPermissionHook;
import me.devtec.craftyserversystem.permission.PermissionHook;
import me.devtec.craftyserversystem.utils.bossbar.UserBossBarData;
import me.devtec.craftyserversystem.utils.scoreboard.UserScoreboardData;
import me.devtec.shared.Ref;
import me.devtec.shared.Ref.ServerType;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.database.DatabaseAPI;
import me.devtec.shared.database.DatabaseAPI.DatabaseType;
import me.devtec.shared.database.DatabaseAPI.SqlDatabaseSettings;
import me.devtec.shared.database.DatabaseHandler;
import me.devtec.shared.database.DatabaseHandler.Row;
import me.devtec.shared.database.SqlFieldType;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.mcmetrics.Metrics;
import me.devtec.shared.placeholders.PlaceholderExpansion;
import me.devtec.shared.sorting.SortingAPI.ComparableObject;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.theapi.bukkit.BukkitLoader;

public class API {

	private static API instance = new API();

	private ConfigurationManager cfgManager;
	private CommandManager cmdManager;
	private CooldownManager cdManager;
	private MessageManager msgManager;
	private ListenerManager listenerManager;
	private AnimationManager anmManager;
	private CommandsAPI commandsApi;

	private PermissionHook permissionHook = new EmptyPermissionHook();
	private EconomyHook economyHook = new EmptyEconomyHook();
	private CssEconomy economy;
	private Metrics metrics;
	@Nullable
	private DatabaseHandler sqlDatabase;

	// Private constructor
	private API() {
	}

	/**
	 * Created instance of API class
	 *
	 * @return API
	 */
	public static API get() {
		return instance;
	}

	/**
	 * ConfigurationManager with all our configurations.
	 *
	 * @return ConfigurationManager
	 */
	public ConfigurationManager getConfigManager() {
		return cfgManager;
	}

	/**
	 * Our own BanAPI with which you can manage bans, mutes, history and active
	 * punishments
	 *
	 * @return BanAPI
	 */
	public CommandsAPI getCommandsAPI() {
		return commandsApi;
	}

	/**
	 * Our own CommandManager with all registered commands, you can also register
	 * your own commands into it by getting a map of registered commands and adding
	 * your own. <br>
	 * When plugin is disabling, all commands are automatically unregistered.
	 *
	 * @return CommandManager
	 */
	public CommandManager getCommandManager() {
		return cmdManager;
	}

	/**
	 * Our own ListenerManager with all listeners, you can also register your own
	 * listeners into it by getting a list of listeners and adding your own. <br>
	 * When plugin is disabling, all listeners are automatically unregistered.
	 *
	 * @return ListenerManager
	 */
	public ListenerManager getListenerManager() {
		return listenerManager;
	}

	/**
	 * Our own CooldownManager, which stores cooldowns from the cooldows.yml file in
	 * RAM in the map under the specified ids from the file. They are used in the
	 * commands. <br>
	 * See
	 * {@link CssCommand#addBypassSettings(me.devtec.shared.commands.structures.CommandStructure)}
	 *
	 * @return CooldownManager
	 */
	public CooldownManager getCooldownManager() {
		return cdManager;
	}

	/**
	 * Sends Json messages from the {@link Config} from the specified path to the
	 * specified players with placeholder replacement
	 *
	 * @return MessageManager
	 */
	public MessageManager getMsgManager() {
		return msgManager;
	}

	/**
	 * Creates animations from String list
	 *
	 * @return AnimationManager
	 */
	public AnimationManager getAnimationManager() {
		return anmManager;
	}

	/**
	 * Economy hook via Vault Economy provider (Essentials, CMIEconomy, etc.) <br>
	 * Or return our empty instance {@link EmptyPermissionHook}
	 *
	 * @return PermissionHook
	 */
	public PermissionHook getPermissionHook() {
		return permissionHook;
	}

	public void setPermissionHook(PermissionHook hook) {
		permissionHook = hook;
	}

	/**
	 * Economy hook via Vault Economy provider (Essentials, CMIEconomy, etc.) <br>
	 * Or return our empty instance {@link EmptyEconomyHook}
	 *
	 * @return EconomyHook
	 */
	public EconomyHook getEconomyHook() {
		return economyHook;
	}

	public void setEconomyHook(EconomyHook hook) {
		economyHook = hook;
		CssCommand balanceTop = getCommandManager().getRegistered().get("balancetop");
		if (balanceTop != null)
			((BalanceTop) balanceTop).calculate();
	}

	@Nullable
	public DatabaseHandler getSqlConnection() {
		return sqlDatabase;
	}

	private PlaceholderExpansion placeholder;

	public void start() {
		if (metrics != null)
			return;
		BukkitLoader.getApiRelease();
		metrics = new Metrics(Loader.getPlugin().getDescription().getVersion(), 20204);
		cfgManager = new ConfigurationManager().initFromJar();

		// Login to the database
		Config config = getConfigManager().getMain();
		if (config.getBoolean("sql.enabled")) {
			DatabaseType databaseType = DatabaseType.valueOf(config.getString("sql.type").toUpperCase());
			try {
				SqlDatabaseSettings sqlSettings = new SqlDatabaseSettings(databaseType, config.getString("sql.ip"),
						3306, config.getString("sql.database"), config.getString("sql.username"),
						config.getString("sql.password"));
				if (!config.getString("sql.attributes").trim().isEmpty())
					sqlSettings.attributes(config.getString("sql.attributes"));
				sqlDatabase = DatabaseAPI.openConnection(databaseType, sqlSettings);
				// Create tables
				if (config.getBoolean("vanish.store-in-sql"))
					sqlDatabase.createTable("css_vanish", new Row[] { new Row("id", SqlFieldType.VARCHAR, 48) });
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		cmdManager = new CommandManager(cfgManager);
		cdManager = new CooldownManager();
		msgManager = new MessageManager();
		anmManager = new AnimationManager();
		listenerManager = new ListenerManager();
		commandsApi = new CommandsAPI();

		// Register our economy hook
		Config economy = getConfigManager().getEconomy();
		if (economy.getBoolean("enabled")) {
			Map<String, List<String>> map = null;
			if (economy.getBoolean("settings.per-world-economy")) {
				map = new HashMap<>();
				for (String key : economy.getKeys("per-world-groups"))
					map.put(key, economy.getStringList("per-world-groups." + key));
			}
			if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
				this.economy = new CssEconomy(economy.getDouble("settings.startup-money"),
						"UNLIMITED".equals(economy.getString("settings.minimum-money")) ? Double.NEGATIVE_INFINITY
								: economy.getDouble("settings.minimum-money"),
								"UNLIMITED".equals(economy.getString("settings.maximum-money")) ? Double.POSITIVE_INFINITY
										: economy.getDouble("settings.maximum-money"),
										map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
			} else {
				Constructor<?> cons = Ref.constructor(
						Ref.getClass("me.devtec.craftyserversystem.economy.CssEconomyVaultImplementation"),
						double.class, double.class, double.class, boolean.class, Map.class);
				this.economy = (CssEconomy) Ref.newInstance(cons, economy.getDouble("settings.startup-money"),
						"UNLIMITED".equals(economy.getString("settings.minimum-money")) ? Double.NEGATIVE_INFINITY
								: economy.getDouble("settings.minimum-money"),
								"UNLIMITED".equals(economy.getString("settings.maximum-money")) ? Double.POSITIVE_INFINITY
										: economy.getDouble("settings.maximum-money"),
										map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
				VaultEconomyHook.registerOurEconomy();
			}
		}
	}

	public void registerPlaceholders() {
		if (API.get().getConfigManager().getEconomy().getBoolean("settings.balance-top.enable-global-placeholder")) {
			Loader.getPlugin().getLogger().info("Registering Placeholder for BalanceTop (%css_baltop_{name/balance}_{position}%)");
			EconomyHook hook = API.get().getEconomyHook();
			placeholder = new PlaceholderExpansion("css") {

				@Override
				public String apply(String text, UUID player) {
					if (text.startsWith("css_"))
						text = text.substring(4);
					if (text.startsWith("baltop_name_")) {
						int pos = Math.max(0, ParseUtils.getInt(text, 12, text.length())-1);
						String worldGroup = "default";
						if (hook instanceof CssEconomyHook) {
							CssEconomyHook css = (CssEconomyHook) hook;
							if (css.economy.isEnabledPerWorldEconomy())
								if (player != null && Bukkit.getPlayer(player) != null)
									worldGroup = css.economy.getWorldGroup(Bukkit.getPlayer(player).getWorld().getName());
						}
						ComparableObject<String, Double>[] comp = BalanceTop.balanceTop.get(worldGroup);
						return comp != null && comp.length > pos ? comp[pos].getKey() : "-";
					}
					if (text.startsWith("baltop_balance_")) {
						int pos = Math.max(0, ParseUtils.getInt(text, 15, text.length())-1);
						String worldGroup = "default";
						if (hook instanceof CssEconomyHook) {
							CssEconomyHook css = (CssEconomyHook) hook;
							if (css.economy.isEnabledPerWorldEconomy())
								if (player != null && Bukkit.getPlayer(player) != null)
									worldGroup = css.economy.getWorldGroup(Bukkit.getPlayer(player).getWorld().getName());
						}
						ComparableObject<String, Double>[] comp = BalanceTop.balanceTop.get(worldGroup);
						return comp != null && comp.length > pos ? hook.format(comp[pos].getValue()) : "-";
					}
					if ("baltop_rank".equalsIgnoreCase(text) && player!=null) {
						String name = me.devtec.shared.API.offlineCache().lookupNameById(player);
						String worldGroup = "default";
						if (hook instanceof CssEconomyHook) {
							CssEconomyHook css = (CssEconomyHook) hook;
							if (css.economy.isEnabledPerWorldEconomy())
								if (player != null && Bukkit.getPlayer(player) != null)
									worldGroup = css.economy.getWorldGroup(Bukkit.getPlayer(player).getWorld().getName());
						}
						ComparableObject<String, Double>[] comp = BalanceTop.balanceTop.get(worldGroup);
						if(comp!=null) {
							int pos = 1;
							for(ComparableObject<String, Double> line : comp)
								if(line.getKey().equalsIgnoreCase(name))
									break;
								else ++pos;
							return StringUtils.formatDouble(FormatType.NORMAL, pos);
						}
						return null;
					}
					if (player == null)
						return null;
					if (text.startsWith("user_"))
						return me.devtec.shared.API.getUser(player).get(text.substring(5)) + "";
					if (text != null)
						switch (text) {
						case "balance": {
							Player online = Bukkit.getPlayer(player);
							return ""
							+ API.get().getEconomyHook().getBalance(
									online == null ? me.devtec.shared.API.offlineCache().lookupNameById(player)
											: online.getName(),
											online == null ? null : online.getWorld().getName());
						}
						case "formatted_balance": {
							Player online = Bukkit.getPlayer(player);
							return API.get().getEconomyHook()
									.format(API.get().getEconomyHook().getBalance(
											online == null ? me.devtec.shared.API.offlineCache().lookupNameById(player)
													: online.getName(),
													online == null ? null : online.getWorld().getName()));
						}
						case "vanish": {
							Player online = Bukkit.getPlayer(player);
							return online != null ? "" + Vanish.getVanish(online) : null;
						}
						case "ping": {
							Player online = Bukkit.getPlayer(player);
							return online != null ? "" + BukkitLoader.getNmsProvider().getPing(online) : null;
						}
						case "scoreboard": {
							UserScoreboardData online = ScoreboardListener.data.get(player);
							return online != null ? "" + online.isHidden() : null;
						}
						case "bossbar": {
							UserBossBarData online = BossBarListener.data.get(player);
							return online != null ? "" + online.isHidden() : null;
						}
						case "afk":
							return me.devtec.shared.API.getUser(player).getBoolean("afk") + "";
						case "chatignore":
							return me.devtec.shared.API.getUser(player).getBoolean("css.chatignore") + "";
						default:
							break;
						}
					return null;
				}
			}.register();
		} else
			placeholder = new PlaceholderExpansion("css") {

			@Override
			public String apply(String text, UUID player) {
				if (player == null)
					return null;
				if (text.startsWith("css_"))
					text = text.substring(4);
				if (text.startsWith("user_"))
					return me.devtec.shared.API.getUser(player).get(text.substring(5)) + "";
				if (text != null)
					switch (text) {
					case "balance": {
						Player online = Bukkit.getPlayer(player);
						return ""
						+ API.get().getEconomyHook().getBalance(
								online == null ? me.devtec.shared.API.offlineCache().lookupNameById(player)
										: online.getName(),
										online == null ? null : online.getWorld().getName());
					}
					case "formatted_balance": {
						Player online = Bukkit.getPlayer(player);
						return API.get().getEconomyHook()
								.format(API.get().getEconomyHook().getBalance(
										online == null ? me.devtec.shared.API.offlineCache().lookupNameById(player)
												: online.getName(),
												online == null ? null : online.getWorld().getName()));
					}
					case "vanish": {
						Player online = Bukkit.getPlayer(player);
						return online != null ? "" + Vanish.getVanish(online) : null;
					}
					case "ping": {
						Player online = Bukkit.getPlayer(player);
						return online != null ? "" + BukkitLoader.getNmsProvider().getPing(online) : null;
					}
					case "scoreboard": {
						UserScoreboardData online = ScoreboardListener.data.get(player);
						return online != null ? "" + online.isHidden() : null;
					}
					case "bossbar": {
						UserBossBarData online = BossBarListener.data.get(player);
						return online != null ? "" + online.isHidden() : null;
					}
					case "afk":
						return me.devtec.shared.API.getUser(player).getBoolean("afk") + "";
					case "chatignore":
						return me.devtec.shared.API.getUser(player).getBoolean("css.chatignore") + "";
					default:
						break;
					}
				return null;
			}
		}.register();
	}

	public void shutdown() {
		if (BanAPI.isInit())
			getCommandsAPI().getBanAPI().shutdown();
		placeholder.unregister();
		metrics.shutdown();
		getListenerManager().unregister();
		if (Bukkit.getPluginManager().getPlugin("Vault") != null)
			if ("CssEconomyVaultImplementation".equals(getEconomyHook().getClass().getSimpleName()))
				VaultEconomyHook.unregisterOurEconomy();
		setEconomyHook(new EmptyEconomyHook());
	}

	public void reload() {
		// Unload
		if(Ref.serverType()!=ServerType.PAPER)
			getCommandManager().unregister();
		getListenerManager().unregister();
		getAnimationManager().unload();
		if (BanAPI.isInit())
			getCommandsAPI().getBanAPI().shutdown();
		if (getSqlConnection() != null)
			try {
				getSqlConnection().close();
				sqlDatabase = null;
			} catch (SQLException e) {
			}
		if (Bukkit.getPluginManager().getPlugin("Vault") != null)
			if ("CssEconomyVaultImplementation".equals(getEconomyHook().getClass().getSimpleName()))
				VaultEconomyHook.unregisterOurEconomy();
		setEconomyHook(new EmptyEconomyHook());
		CssCommand gui = getCommandManager().getRegistered().get("cssgui");
		if(gui!=null)gui.reload();
		// Load
		getConfigManager().reloadAll();
		getAnimationManager().load();
		// Login to the database
		Config config = getConfigManager().getMain();
		if (config.getBoolean("sql.enabled")) {
			DatabaseType databaseType = DatabaseType.valueOf(config.getString("sql.type").toUpperCase());
			try {
				SqlDatabaseSettings sqlSettings = new SqlDatabaseSettings(databaseType, config.getString("sql.ip"),
						3306, config.getString("sql.database"), config.getString("sql.username"),
						config.getString("sql.password"));
				if (!config.getString("sql.attributes").trim().isEmpty())
					sqlSettings.attributes(config.getString("sql.attributes"));
				sqlDatabase = DatabaseAPI.openConnection(databaseType, sqlSettings);
				// Create tables
				if (config.getBoolean("vanish.store-in-sql"))
					sqlDatabase.createTable("css_vanish", new Row[] { new Row("id", SqlFieldType.VARCHAR, 48) });
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		// Register our economy hook
		Config economy = getConfigManager().getEconomy();
		if (economy.getBoolean("enabled")) {
			Map<String, List<String>> map = null;
			if (economy.getBoolean("settings.per-world-economy")) {
				map = new HashMap<>();
				for (String key : economy.getKeys("per-world-groups"))
					map.put(key, economy.getStringList("per-world-groups." + key));
			}
			if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
				this.economy = new CssEconomy(economy.getDouble("settings.startup-money"),
						"UNLIMITED".equals(economy.getString("settings.minimum-money")) ? Double.NEGATIVE_INFINITY
								: economy.getDouble("settings.minimum-money"),
								"UNLIMITED".equals(economy.getString("settings.maximum-money")) ? Double.POSITIVE_INFINITY
										: economy.getDouble("settings.maximum-money"),
										map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
			} else {
				Constructor<?> cons = Ref.constructor(
						Ref.getClass("me.devtec.craftyserversystem.economy.CssEconomyVaultImplementation"),
						double.class, double.class, double.class, boolean.class, Map.class);
				this.economy = (CssEconomy) Ref.newInstance(cons, economy.getDouble("settings.startup-money"),
						"UNLIMITED".equals(economy.getString("settings.minimum-money")) ? Double.NEGATIVE_INFINITY
								: economy.getDouble("settings.minimum-money"),
								"UNLIMITED".equals(economy.getString("settings.maximum-money")) ? Double.POSITIVE_INFINITY
										: economy.getDouble("settings.maximum-money"),
										map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
				VaultEconomyHook.registerOurEconomy();
			}
		}
		getCommandsAPI().getBanAPI().init();

		if(Ref.serverType()!=ServerType.PAPER)
			getCommandManager().register();
		getListenerManager().register();
	}
}
