package me.devtec.craftyserversystem.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.events.CssListener;

public class ListenerManager {

	protected List<CssListener> registered;

	public ListenerManager() {
		registered = new ArrayList<>();
	}

	public void register() {
		try {
			List<CssListener> lookup = lookupForCssListeners();
			for (CssListener listener : lookup) {
				if (!listener.isEnabled())
					continue;
				register(listener);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @return List<CssListener instance>
	 * @throws Exception
	 */
	public List<CssListener> lookupForCssListeners() throws Exception {
		List<CssListener> lookup = new ArrayList<>();
		try (JarFile file = new JarFile(new File(Loader.getPlugin().getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class") && entry.getName().startsWith("me/devtec/craftyserversystem/events/internal/") && entry.getName().indexOf('$') == -1) {
					String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
					Class<?> clazz = Class.forName(className);
					if (clazz.getAnnotation(IgnoredClass.class) != null)
						continue;
					lookup.add((CssListener) clazz.newInstance());
				}
			}
		}
		return lookup;
	}

	public void reloadAll() {
		Iterator<CssListener> itr = registered.iterator();
		while (itr.hasNext()) {
			CssListener listener = itr.next();
			if (!listener.isEnabled()) {
				itr.remove();
				HandlerList.unregisterAll(listener);
				continue;
			}
			listener.reload();
			if (!listener.isEnabled()) { // This is probably not our listener
				itr.remove();
				HandlerList.unregisterAll(listener);
			}
		}
	}

	public void register(CssListener listener) {
		listener.reload();
		Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());
		registered.add(listener);
	}

	public void unregister(CssListener listener) {
		registered.remove(listener);
		HandlerList.unregisterAll(listener);
		listener.unregister();
	}

	public void unregister() {
		for (CssListener listener : registered) {
			HandlerList.unregisterAll(listener);
			listener.unregister();
		}
		registered.clear();
	}

	public List<CssListener> getRegistered() {
		return registered;
	}

}
