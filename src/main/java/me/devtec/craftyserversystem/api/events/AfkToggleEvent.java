package me.devtec.craftyserversystem.api.events;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.devtec.shared.events.Cancellable;
import me.devtec.shared.events.Event;
import me.devtec.shared.events.ListenerHolder;

public class AfkToggleEvent extends Event implements Cancellable {
	private static List<ListenerHolder> listeners = new ArrayList<>();

	private final UUID uuid;
	private final boolean status;
	private boolean cancelled;

	public AfkToggleEvent(UUID uuid, boolean status) {
		this.uuid = uuid;
		this.status = status;
	}

	public UUID getUUID() {
		return uuid;
	}

	public boolean getStatus() {
		return status;
	}

	@Override
	public List<ListenerHolder> getHandlers() {
		return listeners;
	}

	public static List<ListenerHolder> getHandlerList() {
		return listeners;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

}
