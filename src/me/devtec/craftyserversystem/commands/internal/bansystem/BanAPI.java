package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.database.DatabaseHandler.InsertQuery;
import me.devtec.shared.database.DatabaseHandler.Result;
import me.devtec.shared.database.DatabaseHandler.Row;
import me.devtec.shared.database.DatabaseHandler.SelectQuery;
import me.devtec.shared.database.DatabaseHandler.SelectQuery.Sorting;
import me.devtec.shared.database.SqlFieldType;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

@IgnoredClass
public class BanAPI {

	private static BanManagement management;

	public interface BanManagement {
		int generateId();

		void saveEntry(Entry entry);

		List<Entry> retrieveHistory(String user, int limit);

		List<Entry> retrieveHistory(int limit);

		List<Entry> retrieveActivePunishments(String user);

		void save();
	}

	public static void init() {
		if (management != null)
			return;
		if (API.get().getSqlConnection() != null) {
			try {
				API.get().getSqlConnection().createTable("banlist",
						new Row[] { new Row("id", SqlFieldType.INT, 255), new Row("type", SqlFieldType.VARCHAR, 255), new Row("user", SqlFieldType.VARCHAR, 255),
								new Row("reason", SqlFieldType.VARCHAR, 255, true), new Row("admin", SqlFieldType.VARCHAR, 255, true), new Row("duration", SqlFieldType.LONG, 255, true),
								new Row("startDate", SqlFieldType.LONG, 255), new Row("cancelled", SqlFieldType.TINYINT, 1) });
			} catch (SQLException e) {
				e.printStackTrace();
			}
			management = new BanManagement() {

				@Override
				public void saveEntry(Entry entry) {
					try {
						API.get().getSqlConnection().insert(InsertQuery.table("banlist", entry.getId() + "", entry.getType().name(), entry.getUser(), entry.getReason() + "", entry.getAdmin() + "",
								entry.getDuration() == 0L ? "null" : entry.getDuration() + "", entry.getStartDate() + "", entry.isCancelled() ? "1" : "0"));
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

				@Override
				public List<Entry> retrieveHistory(int limit) {
					List<Entry> history = new ArrayList<>();
					try {
						Result result = API.get().getSqlConnection().select(SelectQuery.table("banlist").limit(limit));
						while (result != null) {
							history.add(Entry.fromQuery(result));
							result = result.next();
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return history;
				}

				@Override
				public List<Entry> retrieveHistory(String user, int limit) {
					List<Entry> history = new ArrayList<>();
					try {
						Result result = API.get().getSqlConnection().select(SelectQuery.table("banlist").where("user", user).limit(limit));
						while (result != null) {
							history.add(Entry.fromQuery(result));
							result = result.next();
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return history;
				}

				@Override
				public void save() {

				}

				@Override
				public int generateId() {
					try {
						Result result = API.get().getSqlConnection().select(SelectQuery.table("banlist", "id").sortBy("id").sortType(Sorting.UP).limit(1));
						if (result == null)
							return 0;
						return ParseUtils.getInt(result.getValue()[0]) + 1;
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return 0;
				}

				@Override
				public List<Entry> retrieveActivePunishments(String user) {
					List<Entry> history = new ArrayList<>();
					try {
						Result result = API.get().getSqlConnection().select(SelectQuery.table("banlist").where("user", user).where("cancelled", "0"));
						while (result != null) {
							Entry entry = Entry.fromQuery(result);
							if (entry.getDuration() == 0 || entry.getStartDate() + entry.getDuration() - System.currentTimeMillis() / 1000 > 0)
								history.add(entry);
							result = result.next();
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return history;
				}
			};
		} else
			management = new BanManagement() {
				Config config = API.get().getConfigManager().getBansStorage();

				@Override
				public void saveEntry(Entry entry) {
					config.set(entry.getId() + ".type", entry.getType().name());
					config.set(entry.getId() + ".user", entry.getUser());
					config.set(entry.getId() + ".admin", entry.getAdmin());
					config.set(entry.getId() + ".reason", entry.getReason());
					if (entry.getDuration() != 0)
						config.set(entry.getId() + ".duration", entry.getDuration());
					config.set(entry.getId() + ".startDate", entry.getStartDate());
					config.set(entry.getId() + ".cancelled", entry.isCancelled());
				}

				@Override
				public List<Entry> retrieveHistory(int limit) {
					if (limit == 0) {
						List<Entry> history = new ArrayList<>();
						for (String key : config.getKeys()) {
							if (key.equals("id"))
								continue;
							history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
									config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
						}
						return history;
					}
					int i = 0;
					List<Entry> history = new ArrayList<>();
					for (String key : config.getKeys()) {
						if (key.equals("id"))
							continue;
						if (i++ == limit)
							break;
						history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
								config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
					}
					return history;
				}

				@Override
				public List<Entry> retrieveHistory(String user, int limit) {
					if (limit == 0) {
						List<Entry> history = new ArrayList<>();
						for (String key : config.getKeys()) {
							if (key.equals("id") || !config.getString(key + ".user").equals(user))
								continue;
							history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
									config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
						}
						return history;
					}
					int i = 0;
					List<Entry> history = new ArrayList<>();
					for (String key : config.getKeys()) {
						if (key.equals("id") || !config.getString(key + ".user").equals(user))
							continue;
						if (i++ == limit)
							break;
						history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
								config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
					}
					return history;
				}

				@Override
				public void save() {
					config.save("yaml");
				}

				@Override
				public int generateId() {
					int id = config.getInt("id");
					config.set("id", id + 1);
					return id;
				}

				@Override
				public List<Entry> retrieveActivePunishments(String user) {
					List<Entry> history = new ArrayList<>();
					for (String key : config.getKeys()) {
						long dur;
						if (key.equals("id") || !config.getString(key + ".user").equals(user) || config.getBoolean(key + ".cancelled")
								|| (dur = config.getLong(key + ".duration")) != 0 && config.getLong(key + ".startDate") + dur - System.currentTimeMillis() / 1000 <= 0)
							continue;
						history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
								config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
					}
					return history;
				}
			};
	}

	public static void shutdown() {
		if (management == null)
			return;
		management.save();
		management = null;
	}

	// String can be IP address or username
	public static Entry ban(String user, String administrator, String reason) {
		Entry entry = new Entry(management.generateId(), BanType.BAN, user, reason, administrator, 0);
		saveEntry(entry);
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getHostName().substring(1).equals(user))
					player.kickPlayer("Banned");
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.kickPlayer("Banned");
		}
		return entry;
	}

	public static Entry tempBan(String user, String administrator, long time, String reason) {
		if (time < 0)
			time = 0;
		Entry entry = new Entry(management.generateId(), BanType.BAN, user, reason, administrator, time);
		saveEntry(entry);
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getHostName().substring(1).equals(user))
					player.kickPlayer("Banned");
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.kickPlayer("Banned");
		}
		return entry;
	}

	public static Entry mute(String user, String administrator, String reason) {
		Entry entry = new Entry(management.generateId(), BanType.MUTE, user, reason, administrator, 0);
		saveEntry(entry);
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getHostName().substring(1).equals(user))
					player.sendMessage("Muted");
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.sendMessage("Muted");
		}
		return entry;
	}

	public static Entry tempMute(String user, String administrator, long time, String reason) {
		if (time < 0)
			time = 0;
		Entry entry = new Entry(management.generateId(), BanType.MUTE, user, reason, administrator, time);
		saveEntry(entry);
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getHostName().substring(1).equals(user))
					player.sendMessage("Muted");
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.sendMessage("Muted");
		}
		return entry;
	}

	public static Entry kick(String user, String administrator, String reason) {
		Entry entry = new Entry(management.generateId(), BanType.KICK, user, reason, administrator, 0);
		saveEntry(entry);
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getHostName().substring(1).equals(user))
					player.kickPlayer("Kicked");
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.kickPlayer("Kicked");
		}
		return entry;
	}

	public static Entry warn(String user, String administrator, String reason) {
		Entry entry = new Entry(management.generateId(), BanType.WARN, user, reason, administrator, 0);
		saveEntry(entry);
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getHostName().substring(1).equals(user))
					player.sendMessage("Warn");
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.sendMessage("Warn");
		}
		return entry;
	}

	public static List<Entry> getHistory(String user, int limit) {
		return management.retrieveHistory(user, limit);
	}

	public static List<Entry> getHistory(int limit) {
		return management.retrieveHistory(limit);
	}

	public static List<Entry> getActivePunishments(String user) {
		return management.retrieveActivePunishments(user);
	}

	private static void saveEntry(Entry entry) {
		if (API.get().getSqlConnection() != null)
			try {
				API.get().getSqlConnection().insert(
						InsertQuery.table("banlist", entry.getType().name(), entry.getUser(), entry.getAdmin() + "", entry.getReason() + "", entry.getDuration() + "", entry.getStartDate() + ""));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		else
			API.get().getConfigManager().getBansStorage();
	}

	public static boolean isIPv6(String user) {
		byte mode = 0;
		int count = 0;
		int groups = 0;
		for (int i = 0; i < user.length(); ++i) {
			char c = user.charAt(i);
			switch (mode) {
			case 0:
				if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
					++count;
					mode = 1;
					continue;
				}
				return false;
			case 1:
				if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
					if (++count > 4)
						return false;
					continue;
				}
				if (c == ':') {
					count = 0;
					if (++groups == 7)
						mode = 2;
					else
						mode = 0;
					continue;
				}
				return false;
			case 2:
				if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
					if (++count > 4)
						return false;
					continue;
				}
				if (c == ':')
					if (user.length() - 1 == i)
						return true;
				return false;
			}
		}
		return mode == 2 && count < 5 && count > 0;
	}

	private static boolean isIPv4(String input) {
		if (input.length() < 7)
			return false;
		byte mode = 0;
		int group = 0;

		int count = 0;
		char first = 0;
		char second = 0;
		char third = 0;
		for (int i = 0; i < input.length(); ++i) {
			char c = input.charAt(i);
			switch (mode) {
			case 0:
				if (c >= '0' && c <= '9') {
					switch (++count) {
					case 1:
						first = c;
						break;
					case 2:
						second = c;
						break;
					case 3:
						third = c;
						break;
					default:
						return false;
					}
					mode = 1;
					continue;
				}
				return false;
			case 1:
				if (c == '.') {
					if (count == 0)
						return false;
					++group;
					count = 0;
					first = 0;
					second = 0;
					third = 0;
					mode = 0;
					continue;
				}
				if (c >= '0' && c <= '9') {
					switch (++count) {
					case 1:
						first = c;
						break;
					case 2:
						second = c;
						break;
					case 3:
						third = c;
						break;
					default:
						return false;
					}
					if (count == 3)
						if (first > '2' || first == '2' && second > '5' || first == '2' && second == '5' && third > '5')
							return false;
					continue;
				}
				return false;
			}
		}
		return group == 3 && count != 0;
	}

}
