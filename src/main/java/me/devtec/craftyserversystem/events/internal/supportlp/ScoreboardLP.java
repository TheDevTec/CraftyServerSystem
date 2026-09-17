package me.devtec.craftyserversystem.events.internal.supportlp;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.events.internal.ScoreboardListener;
import me.devtec.craftyserversystem.utils.scoreboard.UserScoreboardData;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;

@IgnoredClass
public class ScoreboardLP {
	private EventSubscription<UserDataRecalculateEvent> lpListener;

	public ScoreboardLP register(ScoreboardListener instance) {
		lpListener = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), UserDataRecalculateEvent.class, event -> {
			UserScoreboardData userData = ScoreboardListener.data.get(event.getUser().getUniqueId());

			if(userData == null)
				return;

			instance.refreshPermissionData(userData.getPlayer(), event.getData().getMetaData().getPrimaryGroup());
		});

		return this;
	}

	public void unregister() {
		lpListener.close();
	}
}
