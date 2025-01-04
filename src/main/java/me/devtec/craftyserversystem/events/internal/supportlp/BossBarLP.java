package me.devtec.craftyserversystem.events.internal.supportlp;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.events.internal.BossBarListener;
import me.devtec.craftyserversystem.utils.bossbar.UserBossBarData;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;

@IgnoredClass
public class BossBarLP {
	private EventSubscription<UserDataRecalculateEvent> lpListener;

	public BossBarLP register(BossBarListener instance) {
		lpListener = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), UserDataRecalculateEvent.class, e -> {
			UserBossBarData userData = BossBarListener.data.get(e.getUser().getUniqueId());
			if (userData != null)
				BossBarListener.data.put(e.getUser().getUniqueId(), instance.generateData(userData.getPlayer()));
		});
		return this;
	}

	public void unregister() {
		lpListener.close();
	}

}
