package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.shared.dataholder.cache.TempList;

@IgnoredClass
public class ConsoleBanFilter {

	private static List<String[]> cancelledMessages;
	private static boolean init;
	private static List<Predicate<String>> predicates = new ArrayList<>();

	public static void init() {
		if (init)
			return;
		init = true;
		cancelledMessages = new TempList<>(100);
		((Logger) LogManager.getRootLogger()).addFilter(new AbstractFilter() {

			@Override
			public Result filter(LogEvent event) {
				String msg = event.getMessage().getFormattedMessage();
				synchronized (cancelledMessages) {
					if (!cancelledMessages.isEmpty()) {
						Iterator<String[]> itr = cancelledMessages.iterator();
						while (itr.hasNext()) {
							String[] string = itr.next();
							if (msg.startsWith(string[0]) && msg.endsWith(string[1])) {
								itr.remove();
								return Result.DENY;
							}
						}
					}
				}
				for (Predicate<String> predicate : predicates)
					if (predicate.test(msg))
						return Result.DENY;
				return super.filter(event);
			}
		});
	}

	public static void addMessage(String prefix, String suffix) {
		synchronized (cancelledMessages) {
			cancelledMessages.add(new String[] { prefix == null ? "" : prefix, suffix == null ? "" : suffix });
		}
	}

	public static void registerFilter(Predicate<String> predicate) {
		predicates.add(predicate);
	}

	public static void unregisterFilter(Predicate<String> predicate) {
		predicates.remove(predicate);
	}

}
