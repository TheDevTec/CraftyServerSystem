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
import me.devtec.craftyserversystem.events.internal.ScoreboardListener;
import me.devtec.craftyserversystem.events.internal.TablistListener;
import me.devtec.craftyserversystem.managers.CommandManager;
import me.devtec.craftyserversystem.managers.ConfigurationManager;
import me.devtec.craftyserversystem.managers.CooldownManager;
import me.devtec.craftyserversystem.managers.ListenerManager;
import me.devtec.craftyserversystem.managers.MessageManager;
import me.devtec.craftyserversystem.permission.EmptyPermissionHook;
import me.devtec.craftyserversystem.permission.PermissionHook;
import me.devtec.craftyserversystem.utils.scoreboard.UserScoreboardData;
import me.devtec.craftyserversystem.utils.tablist.UserTablistData;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.shared.Ref;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.database.DatabaseAPI;
import me.devtec.shared.database.DatabaseAPI.DatabaseType;
import me.devtec.shared.database.DatabaseAPI.SqlDatabaseSettings;
import me.devtec.shared.database.DatabaseHandler;
import me.devtec.shared.database.DatabaseHandler.Row;
import me.devtec.shared.database.SqlFieldType;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.json.modern.ModernJsonReader;
import me.devtec.shared.json.modern.ModernJsonWriter;
import me.devtec.shared.mcmetrics.Metrics;
import me.devtec.shared.placeholders.PlaceholderExpansion;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class API {

	private static API instance = new API();

	private ConfigurationManager cfgManager;
	private CommandManager cmdManager;
	private CooldownManager cdManager;
	private MessageManager msgManager;
	private ListenerManager listenerManager;

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
		metrics = new Metrics(Loader.getPlugin().getDescription().getVersion(), 20204);
		cfgManager = new ConfigurationManager().initFromJar();

		// Login to the database
		Config config = getConfigManager().getMain();
		if (config.getBoolean("sql.enabled")) {
			DatabaseType databaseType = DatabaseType.valueOf(config.getString("sql.type").toUpperCase());
			try {
				SqlDatabaseSettings sqlSettings = new SqlDatabaseSettings(databaseType, config.getString("sql.ip"), 3306, config.getString("sql.database"), config.getString("sql.username"),
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
		listenerManager = new ListenerManager();
		if (Json.reader().getClass() == ModernJsonReader.class || Json.writer().getClass() == ModernJsonWriter.class) {
			Bukkit.getConsoleSender().sendMessage("");
			Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
					"&cCraftyServerSystem &8» &aWe recommend to change &2Json reader & writer &afrom &e&nGuava&r&a to our own &2&nTheAPI&r&a in the &cplugins/TheAPI/config.yml &afile on line &c\"default-json-handler\""));
			Bukkit.getConsoleSender().sendMessage("");
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
						economy.getString("settings.minimum-money").equals("UNLIMITED") ? Double.NEGATIVE_INFINITY : economy.getDouble("settings.minimum-money"),
						economy.getString("settings.maximum-money").equals("UNLIMITED") ? Double.POSITIVE_INFINITY : economy.getDouble("settings.maximum-money"), map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
			} else {
				Constructor<?> cons = Ref.constructor(Ref.getClass("me.devtec.craftyserversystem.economy.CssEconomyVaultImplementation"), double.class, double.class, double.class, boolean.class,
						Map.class);
				this.economy = (CssEconomy) Ref.newInstance(cons, economy.getDouble("settings.startup-money"),
						economy.getString("settings.minimum-money").equals("UNLIMITED") ? Double.NEGATIVE_INFINITY : economy.getDouble("settings.minimum-money"),
						economy.getString("settings.maximum-money").equals("UNLIMITED") ? Double.POSITIVE_INFINITY : economy.getDouble("settings.maximum-money"), map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
				VaultEconomyHook.registerOurEconomy();
			}
		}

		placeholder = new PlaceholderExpansion("css") {

			@Override
			public String apply(String text, UUID player) {
				if (player == null || !text.startsWith("css_"))
					return null;
				if (text.startsWith("css_user_"))
					return me.devtec.shared.API.getUser(player).get(text.substring(9)) + "";
				if (text.equals("css_balance")) {
					Player online = Bukkit.getPlayer(player);
					return "" + API.get().getEconomyHook().getBalance(online == null ? me.devtec.shared.API.offlineCache().lookupNameById(player) : online.getName(),
							online == null ? null : online.getWorld().getName());
				}
				if (text.equals("css_formatted_balance")) {
					Player online = Bukkit.getPlayer(player);
					return API.get().getEconomyHook().format(API.get().getEconomyHook().getBalance(online == null ? me.devtec.shared.API.offlineCache().lookupNameById(player) : online.getName(),
							online == null ? null : online.getWorld().getName()));
				}
				if (text.equals("css_vanish")) {
					Player online = Bukkit.getPlayer(player);
					return online != null ? "" + Vanish.getVanish(online) : null;
				}
				if (text.equals("css_ping")) {
					Player online = Bukkit.getPlayer(player);
					return online != null ? "" + BukkitLoader.getNmsProvider().getPing(online) : null;
				}
				if (text.equals("css_scoreboard")) {
					UserScoreboardData online = ScoreboardListener.data.get(player);
					return online != null ? "" + online.isHidden() : null;
				}
				return null;
			}
		}.register();
	}

	public void shutdown() {
		BanAPI.shutdown();
		placeholder.unregister();
		metrics.shutdown();
		if (NametagManagerAPI.get().isLoaded())
			NametagManagerAPI.get().unload();
		if (!TablistListener.data.isEmpty())
			for (UserTablistData data : TablistListener.data.values())
				data.removeTablist();
		if (!ScoreboardListener.data.isEmpty())
			for (UserScoreboardData data : ScoreboardListener.data.values())
				data.removeScoreboard();
		if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
			Config economy = getConfigManager().getEconomy();
			if (economy.getBoolean("enabled"))
				VaultEconomyHook.unregisterOurEconomy();
		}
		setEconomyHook(new EmptyEconomyHook());
	}

	public void reload() {
		// Unload
		BanAPI.shutdown();
		if (getSqlConnection() != null)
			try {
				getSqlConnection().close();
				sqlDatabase = null;
			} catch (SQLException e) {
			}
		getCommandManager().unregister();
		getListenerManager().unregister();
		if (NametagManagerAPI.get().isLoaded())
			NametagManagerAPI.get().unload();
		if (!TablistListener.data.isEmpty())
			for (UserTablistData data : TablistListener.data.values())
				data.removeTablist();
		if (!ScoreboardListener.data.isEmpty())
			for (UserScoreboardData data : ScoreboardListener.data.values())
				data.removeScoreboard();
		if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
			Config economy = getConfigManager().getEconomy();
			if (economy.getBoolean("enabled"))
				VaultEconomyHook.unregisterOurEconomy();
		}
		setEconomyHook(new EmptyEconomyHook());
		// Load
		getConfigManager().reloadAll();
		// Login to the database
		Config config = getConfigManager().getMain();
		if (config.getBoolean("sql.enabled")) {
			DatabaseType databaseType = DatabaseType.valueOf(config.getString("sql.type").toUpperCase());
			try {
				SqlDatabaseSettings sqlSettings = new SqlDatabaseSettings(databaseType, config.getString("sql.ip"), 3306, config.getString("sql.database"), config.getString("sql.username"),
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
						economy.getString("settings.minimum-money").equals("UNLIMITED") ? Double.NEGATIVE_INFINITY : economy.getDouble("settings.minimum-money"),
						economy.getString("settings.maximum-money").equals("UNLIMITED") ? Double.POSITIVE_INFINITY : economy.getDouble("settings.maximum-money"), map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
			} else {
				Constructor<?> cons = Ref.constructor(Ref.getClass("me.devtec.craftyserversystem.economy.CssEconomyVaultImplementation"), double.class, double.class, double.class, boolean.class,
						Map.class);
				this.economy = (CssEconomy) Ref.newInstance(cons, economy.getDouble("settings.startup-money"),
						economy.getString("settings.minimum-money").equals("UNLIMITED") ? Double.NEGATIVE_INFINITY : economy.getDouble("settings.minimum-money"),
						economy.getString("settings.maximum-money").equals("UNLIMITED") ? Double.POSITIVE_INFINITY : economy.getDouble("settings.maximum-money"), map != null, map);
				setEconomyHook(new CssEconomyHook(this.economy));
				VaultEconomyHook.registerOurEconomy();
			}
		}

		getCommandManager().register();
		getListenerManager().register();
	}
}
