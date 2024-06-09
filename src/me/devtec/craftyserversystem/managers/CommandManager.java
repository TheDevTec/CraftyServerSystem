package me.devtec.craftyserversystem.managers;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.BukkitLoader;

public class CommandManager {

	protected ConfigurationManager cfgManager;
	protected Map<String, CssCommand> registered;

	public CommandManager(ConfigurationManager cfgManager) {
		this.cfgManager = cfgManager;
		registered = new HashMap<>();
	}

	public void register() {
		try {
			Map<String, CssCommand> lookup = lookupForCssCommands();
			Config commandsFile = cfgManager.getCommands();
			BukkitLoader.getNmsProvider().postToMainThread(() -> {
				for (String commandKey : commandsFile.getKeys())
					if (commandsFile.getBoolean(commandKey + ".enabled", true)) {
						CssCommand cmd = lookup.get(commandKey);
						if (cmd == null)
							continue;
						cmd.register();
						registered.put(cmd.section(), cmd);
					}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @return Map<command name in the commands.yml, CssCommand instance>
	 * @throws Exception
	 */
	public Map<String, CssCommand> lookupForCssCommands() throws Exception {
		Map<String, CssCommand> lookup = new HashMap<>();
		try (JarFile file = new JarFile(new File(Loader.getPlugin().getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class") && entry.getName().startsWith("me/devtec/craftyserversystem/commands/internal/") && entry.getName().indexOf('$') == -1) {
					String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
					Class<?> clazz = Class.forName(className);
					if (clazz.getAnnotation(IgnoredClass.class) != null)
						continue;
					CssCommand css = (CssCommand) clazz.newInstance();
					lookup.put(css.section(), css);
				}
			}
		}
		return lookup;
	}

	public void unregister() {
		BukkitLoader.getNmsProvider().postToMainThread(() -> {
			for (CssCommand register : registered.values())
				register.unregister();
			registered.clear();
		});
	}

	public Map<String, CssCommand> getRegistered() {
		return registered;
	}

}
