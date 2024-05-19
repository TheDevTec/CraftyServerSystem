package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.shared.dataholder.cache.TempList;

@IgnoredClass
public class ConsoleBanFilter {

	private static List<String[]> cancelledMessages;
	private static volatile boolean init;

	public static void init() {
		if (init)
			return;
		init = true;
		cancelledMessages = new TempList<>(100);
		((Logger) LogManager.getRootLogger()).addFilter(new AbstractFilter() {

			@Override
			public Result filter(LogEvent event) {
				synchronized (cancelledMessages) {
					if (!cancelledMessages.isEmpty()) {
						Iterator<String[]> itr = cancelledMessages.iterator();
						while (itr.hasNext()) {
							String[] string = itr.next();
							String msg = event.getMessage().getFormattedMessage();
							if (msg.startsWith(string[0]) && msg.endsWith(string[1])) {
								itr.remove();
								return Result.DENY;
							}
						}
					}
				}
				return super.filter(event);
			}
		});
	}

	public static void addMessage(String prefix, String suffix) {
		synchronized (cancelledMessages) {
			cancelledMessages.add(new String[] { prefix == null ? "" : prefix, suffix == null ? "" : suffix });
		}
	}

}
