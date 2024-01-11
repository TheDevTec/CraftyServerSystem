package me.devtec.craftyserversystem;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.BalanceTop;
import me.devtec.craftyserversystem.economy.CssEconomy;
import me.devtec.craftyserversystem.economy.CssEconomyHook;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.economy.EmptyEconomyHook;
import me.devtec.craftyserversystem.economy.VaultEconomyHook;
import me.devtec.craftyserversystem.managers.CommandManager;
import me.devtec.craftyserversystem.managers.ConfigurationManager;
import me.devtec.craftyserversystem.managers.CooldownManager;
import me.devtec.craftyserversystem.managers.MessageManager;
import me.devtec.craftyserversystem.permission.EmptyPermissionHook;
import me.devtec.craftyserversystem.permission.PermissionHook;
import me.devtec.shared.Ref;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.json.modern.ModernJsonReader;
import me.devtec.shared.json.modern.ModernJsonWriter;
import me.devtec.shared.utility.ColorUtils;

public class API {

	private static API instance = new API();

	private ConfigurationManager cfgManager;
	private CommandManager cmdManager;
	private CooldownManager cdManager;
	private MessageManager msgManager;

	private PermissionHook permissionHook = new EmptyPermissionHook();
	private EconomyHook economyHook = new EmptyEconomyHook();
	private CssEconomy economy;

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

	protected void init() {
		cfgManager = new ConfigurationManager().initFromJar();
		cmdManager = new CommandManager(cfgManager);
		cdManager = new CooldownManager();
		msgManager = new MessageManager();
		if (Json.reader().getClass() == ModernJsonReader.class || Json.writer().getClass() == ModernJsonWriter.class) {
			Bukkit.getConsoleSender().sendMessage("");
			Bukkit.getConsoleSender().sendMessage(ColorUtils.colorize(
					"&cCraftyServerSystem &8» &aWe recommend to change &2Json reader & writer &afrom &e&nGuava&r&a to our own &2&nTheAPI&r&a in the &cplugins/TheAPI/config.yml &aon line &c\"default-json-handler\""));
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
	}
}
