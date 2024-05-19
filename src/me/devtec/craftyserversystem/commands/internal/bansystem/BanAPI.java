package me.devtec.craftyserversystem.commands.internal.bansystem;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.database.DatabaseHandler.InsertQuery;
import me.devtec.shared.database.DatabaseHandler.Result;
import me.devtec.shared.database.DatabaseHandler.Row;
import me.devtec.shared.database.DatabaseHandler.SelectQuery;
import me.devtec.shared.database.DatabaseHandler.SelectQuery.Sorting;
import me.devtec.shared.database.DatabaseHandler.UpdateQuery;
import me.devtec.shared.database.SqlFieldType;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

@IgnoredClass
public class BanAPI {

	private static BanManagement management;
	private static SimpleDateFormat format;

	public interface BanManagement {
		int generateId();

		void saveEntry(Entry entry);

		List<Entry> retrieveHistory(String userName, String userIp, int limit);

		List<Entry> retrieveHistory(int limit);

		List<Entry> retrieveActivePunishments(String userName, String userIp);

		void save();
	}

	public static void init() {
		if (management != null)
			return;
		format = new SimpleDateFormat(API.get().getConfigManager().getMain().getString("bansystem.timeFormat"));
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
						if (API.get().getSqlConnection().exists(SelectQuery.table("banlist").where("id", entry.getId() + "")))
							API.get().getSqlConnection().update(UpdateQuery.table("banlist").where("id", entry.getId() + "").value("type", entry.getType().name()).value("user", entry.getUser())
									.value("reason", entry.getReason() + "").value("admin", entry.getAdmin() + "").value("duration", entry.getDuration() == 0L ? "null" : entry.getDuration() + "")
									.value("startDate", entry.getStartDate() + "").value("cancelled", entry.isCancelled() ? "1" : "0"));
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
				public List<Entry> retrieveHistory(String userName, String userIp, int limit) {
					List<Entry> history = new ArrayList<>();
					try {
						Result result = API.get().getSqlConnection().select(SelectQuery.table("banlist").where("user", userName).or().where("user", userIp).limit(limit));
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
				public List<Entry> retrieveActivePunishments(String userName, String userIp) {
					List<Entry> history = new ArrayList<>();
					try {
						Result result = API.get().getSqlConnection()
								.select(SelectQuery.table("banlist").where("user", userName).where("cancelled", "0").or().where("user", userIp).where("cancelled", "0"));
						while (result != null) {
							Entry entry = Entry.fromQuery(result);
							if ((entry.getType() == BanType.BAN || entry.getType() == BanType.MUTE)
									&& (entry.getDuration() == 0 || entry.getStartDate() + entry.getDuration() - System.currentTimeMillis() / 1000 > 0))
								history.add(entry);
							result = result.next();
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return history;
				}
			};
		} else {
			List<Entry> history = new ArrayList<>();
			Config config = API.get().getConfigManager().getBansStorage();
			for (String key : config.getKeys()) {
				if (key.equals("id"))
					continue;
				history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
						config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
			}
			management = new BanManagement() {

				@Override
				public void saveEntry(Entry entry) {
					if (!history.contains(entry))
						history.add(entry);
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
					if (limit == 0)
						return new ArrayList<>(history);
					int i = 0;
					List<Entry> result = new ArrayList<>();
					for (Entry key : history) {
						if (i++ == limit)
							break;
						result.add(key);
					}
					return result;
				}

				@Override
				public List<Entry> retrieveHistory(String user, String userIp, int limit) {
					if (limit == 0) {
						List<Entry> result = new ArrayList<>();
						for (Entry key : history) {
							if (!key.getUser().equals(user) && !key.getUser().equals(userIp))
								continue;
							result.add(key);
						}
						return result;
					}
					int i = 0;
					List<Entry> history = new ArrayList<>();
					for (Entry key : history) {
						if (!key.getUser().equals(user) && !key.getUser().equals(userIp))
							continue;
						if (i++ == limit)
							break;
						history.add(key);
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
				public List<Entry> retrieveActivePunishments(String user, String userIp) {
					List<Entry> result = new ArrayList<>();
					for (Entry key : history) {
						long dur;
						if (!key.getUser().equals(user) && !key.getUser().equals(userIp) || key.isCancelled() || key.getType() != BanType.BAN && key.getType() != BanType.MUTE
								|| (dur = key.getDuration()) != 0 && key.getStartDate() + dur - System.currentTimeMillis() / 1000 <= 0)
							continue;
						result.add(key);
					}
					return result;
				}
			};
		}
	}

	public static void shutdown() {
		if (management == null)
			return;
		management.save();
		management = null;
	}

	// String can be IP address or username
	public static Entry ban(String user, String administrator, String reason) {
		if (management == null)
			return null;
		Entry entry = new Entry(management.generateId(), BanType.BAN, user, reason, administrator, 0);
		management.saveEntry(entry);
		PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("user", entry.getUser()).add("reason", entry.getReason() == null ? "Uknown" : entry.getReason())
				.add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
				.add("startDate", format.format(Date.from(Instant.ofEpochMilli(entry.getStartDate()))));
		List<CommandSender> admins = new ArrayList<>();
		admins.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(API.get().getConfigManager().getCommands().getString("ban.perms.broadcast")))
				admins.add(player);
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.banned.perm", executor, admins);
		String kickMessage = executor.apply(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.banned"), "\n"));
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getAddress().getHostAddress().equals(user))
					player.kickPlayer(kickMessage);
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.kickPlayer(kickMessage);
		}
		return entry;
	}

	public static Entry tempBan(String user, String administrator, long time, String reason) {
		if (management == null)
			return null;
		if (time < 0)
			time = 0;
		Entry entry = new Entry(management.generateId(), BanType.BAN, user, reason, administrator, time);
		management.saveEntry(entry);
		PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("user", entry.getUser()).add("reason", entry.getReason() == null ? "Uknown" : entry.getReason())
				.add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
				.add("startDate", format.format(Date.from(Instant.ofEpochMilli(entry.getStartDate())))).add("expireAfter", TimeUtils.timeToString(entry.getDuration()))
				.add("expireDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + time))));
		List<CommandSender> admins = new ArrayList<>();
		admins.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(API.get().getConfigManager().getCommands().getString("tempban.perms.broadcast")))
				admins.add(player);
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.banned.temp", executor, admins);
		String kickMessage = executor.apply(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.temp-banned"), "\n"));
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getAddress().getHostAddress().equals(user))
					player.kickPlayer(kickMessage);
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.kickPlayer(kickMessage);
		}
		return entry;
	}

	public static Entry mute(String user, String administrator, String reason) {
		if (management == null)
			return null;
		Entry entry = new Entry(management.generateId(), BanType.MUTE, user, reason, administrator, 0);
		management.saveEntry(entry);
		PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("user", entry.getUser()).add("reason", entry.getReason() == null ? "Uknown" : entry.getReason())
				.add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
				.add("startDate", format.format(Date.from(Instant.ofEpochMilli(entry.getStartDate()))));
		List<CommandSender> admins = new ArrayList<>();
		admins.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(API.get().getConfigManager().getCommands().getString("mute.perms.broadcast")))
				admins.add(player);
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.muted.perm", executor, admins);
		if (isIPv4(user) || isIPv6(user)) {
			List<Player> players = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getAddress().getHostAddress().equals(user))
					players.add(player);
			if (!players.isEmpty())
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.mute", executor, players);
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.mute", executor, target);
		}
		return entry;
	}

	public static Entry tempMute(String user, String administrator, long time, String reason) {
		if (management == null)
			return null;
		if (time < 0)
			time = 0;
		Entry entry = new Entry(management.generateId(), BanType.MUTE, user, reason, administrator, time);
		management.saveEntry(entry);
		PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("user", entry.getUser()).add("reason", entry.getReason() == null ? "Uknown" : entry.getReason())
				.add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
				.add("startDate", format.format(Date.from(Instant.ofEpochMilli(entry.getStartDate())))).add("expireAfter", TimeUtils.timeToString(entry.getDuration()))
				.add("expireDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + time))));
		List<CommandSender> admins = new ArrayList<>();
		admins.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(API.get().getConfigManager().getCommands().getString("tempmute.perms.broadcast")))
				admins.add(player);
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.muted.temp", executor, admins);
		if (isIPv4(user) || isIPv6(user)) {
			List<Player> players = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getAddress().getHostAddress().equals(user))
					players.add(player);
			if (!players.isEmpty())
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.temp-muted", executor, players);
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.temp-muted", executor, target);
		}
		return entry;
	}

	public static Entry kick(String user, String administrator, String reason) {
		if (management == null)
			return null;
		Entry entry = new Entry(management.generateId(), BanType.KICK, user, reason, administrator, 0);
		management.saveEntry(entry);
		PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("user", entry.getUser()).add("reason", entry.getReason() == null ? "Uknown" : entry.getReason())
				.add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
				.add("startDate", format.format(Date.from(Instant.ofEpochMilli(entry.getStartDate()))));
		List<CommandSender> admins = new ArrayList<>();
		admins.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(API.get().getConfigManager().getCommands().getString("kick.perms.broadcast")))
				admins.add(player);
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.kicked", executor, admins);
		String kickMessage = executor.apply(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.kicked"), "\n"));
		if (isIPv4(user) || isIPv6(user)) {
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getAddress().getHostAddress().equals(user))
					player.kickPlayer(kickMessage);
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				target.kickPlayer(kickMessage);
		}
		return entry;
	}

	public static Entry warn(String user, String administrator, String reason) {
		if (management == null)
			return null;
		Entry entry = new Entry(management.generateId(), BanType.WARN, user, reason, administrator, 0);
		management.saveEntry(entry);
		PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("user", entry.getUser()).add("reason", entry.getReason() == null ? "Uknown" : entry.getReason())
				.add("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).add("id", entry.getId() + "")
				.add("startDate", format.format(Date.from(Instant.ofEpochMilli(entry.getStartDate()))));
		List<CommandSender> admins = new ArrayList<>();
		admins.add(Bukkit.getConsoleSender());
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.hasPermission(API.get().getConfigManager().getCommands().getString("warn.perms.broadcast")))
				admins.add(player);
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.warned", executor, admins);
		if (isIPv4(user) || isIPv6(user)) {
			List<Player> players = new ArrayList<>();
			for (Player player : BukkitLoader.getOnlinePlayers())
				if (player.getAddress().getAddress().getHostAddress().equals(user))
					players.add(player);
			if (!players.isEmpty())
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.warned", executor, players);
		} else {
			Player target = Bukkit.getPlayer(user);
			if (target != null)
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.warned", executor, target);
		}
		return entry;
	}

	public static List<Entry> getHistory(String userName, String userIp, int limit) {
		if (management == null)
			return null;
		return management.retrieveHistory(userName, userIp, limit);
	}

	public static List<Entry> getHistory(int limit) {
		if (management == null)
			return null;
		return management.retrieveHistory(limit);
	}

	public static List<Entry> getActivePunishments(String userName, String userIp) {
		if (management == null)
			return null;
		return management.retrieveActivePunishments(userName, userIp);
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

	public static SimpleDateFormat getTimeFormat() {
		return format;
	}

}
