package me.devtec.craftyserversystem.commands.internal.tprequest;

import java.util.UUID;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.annotations.IgnoredClass;

@IgnoredClass
public class TpaRequest {
	private UUID sender;
	private UUID target;
	private long expireAt;
	private boolean teleportToTarget;

	public TpaRequest(UUID sender, UUID target, long expireAt, boolean teleportToTarget) {
		this.target = target;
		this.expireAt = expireAt;
		this.teleportToTarget = teleportToTarget;
	}

	public UUID getSender() {
		return sender;
	}

	public UUID getTarget() {
		return target;
	}

	public long getExpireAt() {
		return expireAt;
	}

	public boolean shouldTeleportToTarget() {
		return teleportToTarget;
	}

	public boolean isValid() {
		return getExpireAt() - System.currentTimeMillis() / 1000 > 0 && Bukkit.getPlayer(getSender()) != null && Bukkit.getPlayer(getTarget()) != null;
	}

	public void teleport() {
		if (teleportToTarget)
			Bukkit.getPlayer(sender).teleport(Bukkit.getPlayer(target));
		else
			Bukkit.getPlayer(target).teleport(Bukkit.getPlayer(sender));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TpaRequest) {
			TpaRequest req = (TpaRequest) obj;
			return req.sender.equals(sender) && req.target.equals(target) || req.sender.equals(target) && req.target.equals(sender);
		}
		return false;
	}
}
