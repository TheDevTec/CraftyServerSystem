package me.devtec.craftyserversystem.events.internal.supportlp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.events.internal.BossBarListener;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;

@IgnoredClass
public class BossBarLP {

	private EventSubscription<UserDataRecalculateEvent> lpListener;

	public BossBarLP register(BossBarListener instance) {
		lpListener = LuckPermsProvider.get().getEventBus().subscribe(Loader.getPlugin(), UserDataRecalculateEvent.class, event -> {
			Player player = Bukkit.getPlayer(event.getUser().getUniqueId());

			if(player == null || !player.isOnline())
				return;

			instance.refreshPermissionData(player, event.getData().getMetaData().getPrimaryGroup());
		});

		return this;
	}

	public void unregister() {
		if(lpListener != null) {
			lpListener.close();
			lpListener = null;
		}
	}
}
