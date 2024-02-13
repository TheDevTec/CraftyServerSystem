package me.devtec.craftyserversystem.api.events;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.devtec.shared.events.Event;
import me.devtec.shared.events.ListenerHolder;

public class VanishToggleEvent extends Event {
	private static List<ListenerHolder> listeners = new ArrayList<>();

	private final UUID uuid;
	private boolean status;

	public VanishToggleEvent(UUID uuid, boolean status) {
		this.uuid = uuid;
		this.status = status;
	}

	public UUID getUUID() {
		return uuid;
	}

	public boolean getStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	@Override
	public List<ListenerHolder> getHandlers() {
		return listeners;
	}

	public static List<ListenerHolder> getHandlerList() {
		return listeners;
	}

}
