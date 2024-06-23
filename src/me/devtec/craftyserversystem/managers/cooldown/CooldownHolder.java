package me.devtec.craftyserversystem.managers.cooldown;

import org.bukkit.command.CommandSender;

import me.devtec.shared.annotations.Nullable;

public abstract class CooldownHolder {

	private final String id;
	private String perm;
	private long time;

	public CooldownHolder(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public abstract boolean accept(CommandSender sender);

	public abstract boolean tryWithoutWriting(CommandSender sender);

	public abstract long remainingTime(CommandSender sender);

	@Nullable
	public String getBypassPerm() {
		return perm;
	}

	public void setBypassPerm(@Nullable String perm) {
		this.perm = perm;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

}
