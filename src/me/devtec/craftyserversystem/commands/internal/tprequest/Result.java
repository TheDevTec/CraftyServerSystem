package me.devtec.craftyserversystem.commands.internal.tprequest;

public enum Result {
	SUCCESS, // Request sent!
	DENIED_BY_TARGET, // TpToggle <target> or global
	FAILED_SENDER, // Sender have any teleport request from target
	FAILED_TARGET, // Target already have any request from sender
	INVALID // Sender or target is offline or request expired
}
