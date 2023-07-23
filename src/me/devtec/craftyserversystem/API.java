package me.devtec.craftyserversystem;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.economy.EconomyHook;
import me.devtec.craftyserversystem.economy.EmptyEconomyHook;
import me.devtec.craftyserversystem.managers.CommandManager;
import me.devtec.craftyserversystem.managers.ConfigurationManager;
import me.devtec.craftyserversystem.managers.CooldownManager;
import me.devtec.craftyserversystem.managers.MessageManager;
import me.devtec.craftyserversystem.permission.EmptyPermissionHook;
import me.devtec.craftyserversystem.permission.PermissionHook;
import me.devtec.shared.dataholder.Config;

public class API {

	private static API instance = new API();

	private ConfigurationManager cfgManager;
	private CommandManager cmdManager;
	private CooldownManager cdManager;
	private MessageManager msgManager;

	private PermissionHook permissionHook = new EmptyPermissionHook();
	private EconomyHook economyHook = new EmptyEconomyHook();

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
	}

	protected void init() {
		cfgManager = new ConfigurationManager("config.yml", "translations.yml", "commands.yml", "cooldowns.yml", "join.yml", "quit.yml", "chat.yml").initFromJar();
		cmdManager = new CommandManager(cfgManager);
		cdManager = new CooldownManager();
		msgManager = new MessageManager();
	}
}
