package me.devtec.craftyserversystem.events.internal.supportlp;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.events.internal.TablistListener;
import me.devtec.craftyserversystem.utils.tablist.UserTablistData;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;

@IgnoredClass
public class TablistLP {
	private EventSubscription<UserDataRecalculateEvent> lpListener;

	public TablistLP register(TablistListener instance) {
		lpListener = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), UserDataRecalculateEvent.class, e -> {
			UserTablistData userData = TablistListener.data.get(e.getUser().getUniqueId());
			TablistListener.data.put(e.getUser().getUniqueId(), instance.generateData(userData.getPlayer()));
		});
		return this;
	}

	public void unregister() {
		lpListener.close();
	}

}
