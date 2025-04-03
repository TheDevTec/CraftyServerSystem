package me.devtec.craftyserversystem.commands.internal.bansystem;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.database.DatabaseHandler.Result;
import me.devtec.shared.utility.ParseUtils;

@IgnoredClass
public class Entry {
	private final int id;
	private final BanType type;
	private final String user;
	@Nullable
	private final String reason;
	@Nullable
	private final String admin;
	private final long duration;
	private final long startDate;
	private boolean cancelled;

	public Entry(int id, BanType type, String user, String reason, String admin, long duration, long startDate,
			boolean cancelled) {
		this.id = id;
		this.type = type;
		this.user = user;
		this.reason = reason;
		this.admin = admin;
		this.duration = duration;
		this.startDate = startDate;
		this.cancelled = cancelled;
	}

	public Entry(int id, BanType type, String user, String reason, String admin, long duration) {
		this(id, type, user, reason, admin, duration, System.currentTimeMillis() / 1000, false);
	}

	public Entry(int id, BanType type, String user, String reason, long duration) {
		this(id, type, user, reason, null, duration, System.currentTimeMillis() / 1000, false);
	}

	public BanType getType() {
		return type;
	}

	public String getUser() {
		return user;
	}

	@Nullable
	public String getReason() {
		return reason;
	}

	@Nullable
	public String getAdmin() {
		return admin;
	}

	public long getDuration() {
		return duration;
	}

	public long getStartDate() {
		return startDate;
	}

	public int getId() {
		return id;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public static Entry fromQuery(Result result) {
		return new Entry(ParseUtils.getInt(result.getValue()[0]), BanType.valueOf(result.getValue()[1].toUpperCase()),
				result.getValue()[2], result.getValue()[3], result.getValue()[4],
				ParseUtils.getLong(result.getValue()[5]), ParseUtils.getLong(result.getValue()[6]),
				ParseUtils.getInt(result.getValue()[7]) == 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Entry) {
			Entry second = (Entry) obj;
			return second.getId() == getId() && second.getType().equals(getType());
		}
		return false;
	}
}
