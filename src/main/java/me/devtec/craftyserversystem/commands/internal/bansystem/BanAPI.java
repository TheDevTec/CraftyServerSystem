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
import me.devtec.shared.database.DatabaseHandler.Row;
import me.devtec.shared.database.Sql;
import me.devtec.shared.database.SqlFieldType;
import me.devtec.shared.database.SqlRow;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

@IgnoredClass
public class BanAPI {

	private static BanAPI instance;

	private BanManagement management;
	private SimpleDateFormat format;

	private BanAPI() {
		init();
	}

	public static BanAPI get() {
		if(instance == null)
			instance = new BanAPI();

		return instance;
	}

	public static boolean isInit() {
		return instance != null && instance.management != null;
	}

	public SimpleDateFormat getTimeFormat() {
		return format;
	}

	public BanManagement getManagement() {
		return management;
	}

	public void setBanManagement(BanManagement management) {
		this.management = management;
	}

	private TextRenderer renderer() {
		return TextRenderer.create().placeholder("prefix", API.get().getConfigManager().getPrefix()).colorize();
	}

	private Sql.Select selectEntries() {
		return Sql.select("id", "type", "user", "reason", "admin", "duration", "startDate", "cancelled").from("css_banlist");
	}

	public void init() {
		if(management != null)
			return;

		format = new SimpleDateFormat(API.get().getConfigManager().getMain().getString("bansystem.timeFormat"));

		if(API.get().getSqlConnection() != null) {
			try {
				API.get().getSqlConnection().createTable("css_banlist",
				        new Row[]{new Row("id", SqlFieldType.INT, 255), new Row("type", SqlFieldType.VARCHAR, 255), new Row("user", SqlFieldType.VARCHAR, 255),
				                new Row("reason", SqlFieldType.VARCHAR, 255, true), new Row("admin", SqlFieldType.VARCHAR, 255, true), new Row("duration", SqlFieldType.LONG),
				                new Row("startDate", SqlFieldType.LONG), new Row("cancelled", SqlFieldType.TINYINT, 1)});
			} catch(SQLException exception) {
				exception.printStackTrace();
			}

			management = new BanManagement() {

				@Override
				public void saveEntry(Entry entry) {
					try {
						if(API.get().getSqlConnection().exists(Sql.select("id").from("css_banlist").where("id", entry.getId()).limit(1)))
							API.get().getSqlConnection()
							        .update(Sql.update("css_banlist").set("type", entry.getType().name()).set("user", entry.getUser()).set("reason", entry.getReason()).set("admin", entry.getAdmin())
							                .set("duration", entry.getDuration()).set("startDate", entry.getStartDate()).set("cancelled", entry.isCancelled() ? 1 : 0).where("id", entry.getId()));
						else
							API.get().getSqlConnection().update(Sql.insertInto("css_banlist", "id", "type", "user", "reason", "admin", "duration", "startDate", "cancelled").values(entry.getId(),
							        entry.getType().name(), entry.getUser(), entry.getReason(), entry.getAdmin(), entry.getDuration(), entry.getStartDate(), entry.isCancelled() ? 1 : 0));
					} catch(SQLException exception) {
						exception.printStackTrace();
					}
				}

				@Override
				public List<Entry> retrieveHistory(int limit) {
					List<Entry> history = new ArrayList<>();

					try {
						Sql.Select query = selectEntries();

						if(limit > 0)
							query.limit(limit);

						for(SqlRow row : API.get().getSqlConnection().query(query))
							history.add(Entry.fromQuery(row));
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return history;
				}

				@Override
				public List<Entry> retrieveHistory(String userName, String userIp, int limit) {
					List<Entry> history = new ArrayList<>();

					try {
						Sql.Select query = selectEntries();

						if(userName == null || userIp == null) {
							String onlyOneName = userIp == null ? userName : userIp;

							query.where("user", onlyOneName);
						} else
							query.where(Sql.column("user").eq(userName).or(Sql.column("user").eq(userIp)));

						if(limit > 0)
							query.limit(limit);

						for(SqlRow row : API.get().getSqlConnection().query(query))
							history.add(Entry.fromQuery(row));
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return history;
				}

				@Override
				public void save() {
				}

				@Override
				public int generateId() {
					try {
						List<SqlRow> result = API.get().getSqlConnection().query(Sql.select("id").from("css_banlist").orderBy(Sql.column("id").desc()).limit(1));

						if(result.isEmpty())
							return 0;

						Object id = result.get(0).get("id");

						if(id instanceof Number)
							return ((Number) id).intValue() + 1;

						return ParseUtils.getInt(String.valueOf(id)) + 1;
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return 0;
				}

				@Override
				public List<Entry> retrieveActivePunishments(String userName, String userIp) {
					List<Entry> history = new ArrayList<>();

					try {
						Sql.Select query = selectEntries();

						if(userName == null || userIp == null) {
							String onlyOneName = userIp == null ? userName : userIp;

							query.where("user", onlyOneName).where("cancelled", 0);
						} else
							query.where(Sql.column("user").eq(userName).and(Sql.column("cancelled").eq(0)).or(Sql.column("user").eq(userIp).and(Sql.column("cancelled").eq(0))));

						for(SqlRow row : API.get().getSqlConnection().query(query)) {
							Entry entry = Entry.fromQuery(row);

							if((entry.getType() == BanType.BAN || entry.getType() == BanType.MUTE) && isActive(entry))
								history.add(entry);
						}
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return history;
				}

				@Override
				public List<Entry> retrieveActivePunishments(String userName, String userIp, BanType type) {

					List<Entry> history = new ArrayList<>();

					try {
						Sql.Select query = selectEntries();

						if(userName == null || userIp == null) {
							String onlyOneName = userIp == null ? userName : userIp;

							query.where("user", onlyOneName).where("type", type.name()).where("cancelled", 0);
						} else
							query.where(Sql.column("user").eq(userName).and(Sql.column("type").eq(type.name())).and(Sql.column("cancelled").eq(0))
							        .or(Sql.column("user").eq(userIp).and(Sql.column("type").eq(type.name())).and(Sql.column("cancelled").eq(0))));

						for(SqlRow row : API.get().getSqlConnection().query(query)) {
							Entry entry = Entry.fromQuery(row);

							if(isActive(entry))
								history.add(entry);
						}
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return history;
				}

				@Override
				public List<Entry> retrieveActivePunishments() {
					List<Entry> history = new ArrayList<>();

					try {
						for(SqlRow row : API.get().getSqlConnection().query(selectEntries().where("cancelled", 0))) {

							Entry entry = Entry.fromQuery(row);

							if((entry.getType() == BanType.BAN || entry.getType() == BanType.MUTE) && isActive(entry))
								history.add(entry);
						}
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return history;
				}

				@Override
				public List<Entry> retrieveActivePunishments(BanType type) {
					List<Entry> history = new ArrayList<>();

					try {
						for(SqlRow row : API.get().getSqlConnection().query(selectEntries().where("cancelled", 0).where("type", type.name()))) {

							Entry entry = Entry.fromQuery(row);

							if(isActive(entry))
								history.add(entry);
						}
					} catch(SQLException exception) {
						exception.printStackTrace();
					}

					return history;
				}
			};
		} else {
			List<Entry> history = new ArrayList<>();
			Config config = API.get().getConfigManager().getBansStorage();

			for(String key : config.getKeys()) {
				if("id".equals(key))
					continue;

				history.add(new Entry(ParseUtils.getInt(key), BanType.valueOf(config.getString(key + ".type")), config.getString(key + ".user"), config.getString(key + ".reason"),
				        config.getString(key + ".admin"), config.getLong(key + ".duration"), config.getLong(key + ".startDate"), config.getBoolean(key + ".cancelled")));
			}

			management = new BanManagement() {

				@Override
				public void saveEntry(Entry entry) {
					if(!history.contains(entry))
						history.add(entry);

					config.set(entry.getId() + ".type", entry.getType().name());
					config.set(entry.getId() + ".user", entry.getUser());
					config.set(entry.getId() + ".admin", entry.getAdmin());
					config.set(entry.getId() + ".reason", entry.getReason());

					if(entry.getDuration() != 0)
						config.set(entry.getId() + ".duration", entry.getDuration());
					else
						config.remove(entry.getId() + ".duration");

					config.set(entry.getId() + ".startDate", entry.getStartDate());
					config.set(entry.getId() + ".cancelled", entry.isCancelled());
				}

				@Override
				public List<Entry> retrieveHistory(int limit) {
					if(limit == 0)
						return new ArrayList<>(history);

					int i = 0;
					List<Entry> result = new ArrayList<>();

					for(Entry key : history) {
						if(i++ == limit)
							break;

						result.add(key);
					}

					return result;
				}

				@Override
				public List<Entry> retrieveHistory(String userName, String userIp, int limit) {
					if(limit == 0) {
						List<Entry> result = new ArrayList<>();

						if(userName == null || userIp == null) {
							String onlyOneName = userIp == null ? userName : userIp;

							for(Entry key : history) {
								if(!key.getUser().equals(onlyOneName))
									continue;

								result.add(key);
							}
						} else
							for(Entry key : history) {
								if(!key.getUser().equals(userName) && !key.getUser().equals(userIp))
									continue;

								result.add(key);
							}

						return result;
					}

					int i = 0;
					List<Entry> result = new ArrayList<>();

					if(userName == null || userIp == null) {
						String onlyOneName = userIp == null ? userName : userIp;

						for(Entry key : history) {
							if(!key.getUser().equals(onlyOneName))
								continue;

							if(i++ == limit)
								break;

							result.add(key);
						}
					} else
						for(Entry key : history) {
							if(!key.getUser().equals(userName) && !key.getUser().equals(userIp))
								continue;

							if(i++ == limit)
								break;

							result.add(key);
						}

					return result;
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
				public List<Entry> retrieveActivePunishments(String userName, String userIp) {
					List<Entry> result = new ArrayList<>();

					if(userName == null || userIp == null) {
						String onlyOneName = userIp == null ? userName : userIp;

						for(Entry key : history) {
							if(!key.getUser().equals(onlyOneName) || key.isCancelled() || key.getType() != BanType.BAN && key.getType() != BanType.MUTE || !isActive(key))
								continue;

							result.add(key);
						}
					} else
						for(Entry key : history) {
							if(!key.getUser().equals(userName) && !key.getUser().equals(userIp) || key.isCancelled() || key.getType() != BanType.BAN && key.getType() != BanType.MUTE || !isActive(key))
								continue;

							result.add(key);
						}

					return result;
				}

				@Override
				public List<Entry> retrieveActivePunishments(String userName, String userIp, BanType type) {

					List<Entry> result = new ArrayList<>();

					if(userName == null || userIp == null) {
						String onlyOneName = userIp == null ? userName : userIp;

						for(Entry key : history) {
							if(!key.getUser().equals(onlyOneName) || key.isCancelled() || key.getType() != type || !isActive(key))
								continue;

							result.add(key);
						}
					} else
						for(Entry key : history) {
							if(!key.getUser().equals(userName) && !key.getUser().equals(userIp) || key.isCancelled() || key.getType() != type || !isActive(key))
								continue;

							result.add(key);
						}

					return result;
				}

				@Override
				public List<Entry> retrieveActivePunishments() {
					List<Entry> result = new ArrayList<>();

					for(Entry key : history) {
						if(key.isCancelled() || key.getType() != BanType.BAN && key.getType() != BanType.MUTE || !isActive(key))
							continue;

						result.add(key);
					}

					return result;
				}

				@Override
				public List<Entry> retrieveActivePunishments(BanType type) {
					List<Entry> result = new ArrayList<>();

					for(Entry key : history) {
						if(key.isCancelled() || key.getType() != type || !isActive(key))
							continue;

						result.add(key);
					}

					return result;
				}
			};
		}
	}

	private static boolean isActive(Entry entry) {
		return entry.getDuration() == 0 || entry.getStartDate() + entry.getDuration() - System.currentTimeMillis() / 1000 > 0;
	}

	public void shutdown() {
		if(management == null)
			return;

		management.save();
		management = null;
	}

	// String can be IP address or username
	public Entry ban(String user, String administrator, String reason) {
		BanManagement management = getManagement();

		Entry entry = new Entry(management.generateId(), BanType.BAN, user, reason, administrator, 0);

		management.saveEntry(entry);

		TextRenderer renderer = renderer().placeholder("user", entry.getUser())
		        .placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
		        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId())
		        .placeholder("startDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))));

		List<CommandSender> admins = new ArrayList<>();

		admins.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.hasPermission(API.get().getConfigManager().getCommands().getString("ban.perms.broadcast")))
				admins.add(player);

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.banned.perm", renderer, admins);

		String kickMessage = renderer.render(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.banned"), "\n"));

		if(isIPv4(user) || isIPv6(user)) {
			for(Player player : BukkitLoader.getOnlinePlayers())
				if(player.getAddress().getAddress().getHostAddress().equals(user))
					player.kickPlayer(kickMessage);
		} else {
			Player target = Bukkit.getPlayer(user);

			if(target != null)
				target.kickPlayer(kickMessage);
		}

		return entry;
	}

	public Entry tempBan(String user, String administrator, long time, String reason) {
		if(time < 0)
			time = 0;

		BanManagement management = getManagement();

		Entry entry = new Entry(management.generateId(), BanType.BAN, user, reason, administrator, time);

		management.saveEntry(entry);

		TextRenderer renderer = renderer().placeholder("user", entry.getUser())
		        .placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
		        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId())
		        .placeholder("startDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate())))).placeholder("expireAfter", TimeUtils.timeToString(entry.getDuration()))
		        .placeholder("expireDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + time))));

		List<CommandSender> admins = new ArrayList<>();

		admins.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.hasPermission(API.get().getConfigManager().getCommands().getString("tempban.perms.broadcast")))
				admins.add(player);

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.banned.temp", renderer, admins);

		String kickMessage = renderer.render(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.temp-banned"), "\n"));

		if(isIPv4(user) || isIPv6(user)) {
			for(Player player : BukkitLoader.getOnlinePlayers())
				if(player.getAddress().getAddress().getHostAddress().equals(user))
					player.kickPlayer(kickMessage);
		} else {
			Player target = Bukkit.getPlayer(user);

			if(target != null)
				target.kickPlayer(kickMessage);
		}

		return entry;
	}

	public Entry mute(String user, String administrator, String reason) {
		BanManagement management = getManagement();

		Entry entry = new Entry(management.generateId(), BanType.MUTE, user, reason, administrator, 0);

		management.saveEntry(entry);

		TextRenderer renderer = renderer().placeholder("user", entry.getUser())
		        .placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
		        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId())
		        .placeholder("startDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))));

		List<CommandSender> admins = new ArrayList<>();

		admins.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.hasPermission(API.get().getConfigManager().getCommands().getString("mute.perms.broadcast")))
				admins.add(player);

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.muted.perm", renderer, admins);

		if(isIPv4(user) || isIPv6(user)) {
			List<Player> players = new ArrayList<>();

			for(Player player : BukkitLoader.getOnlinePlayers())
				if(player.getAddress().getAddress().getHostAddress().equals(user))
					players.add(player);

			if(!players.isEmpty())
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.muted", renderer, players);
		} else {
			Player target = Bukkit.getPlayer(user);

			if(target != null)
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.muted", renderer, target);
		}

		return entry;
	}

	public Entry tempMute(String user, String administrator, long time, String reason) {
		if(time < 0)
			time = 0;

		BanManagement management = getManagement();

		Entry entry = new Entry(management.generateId(), BanType.MUTE, user, reason, administrator, time);

		management.saveEntry(entry);

		TextRenderer renderer = renderer().placeholder("user", entry.getUser())
		        .placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
		        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId())
		        .placeholder("startDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate())))).placeholder("expireAfter", TimeUtils.timeToString(entry.getDuration()))
		        .placeholder("expireDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate() + time))));

		List<CommandSender> admins = new ArrayList<>();

		admins.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.hasPermission(API.get().getConfigManager().getCommands().getString("tempmute.perms.broadcast")))
				admins.add(player);

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.muted.temp", renderer, admins);

		if(isIPv4(user) || isIPv6(user)) {
			List<Player> players = new ArrayList<>();

			for(Player player : BukkitLoader.getOnlinePlayers())
				if(player.getAddress().getAddress().getHostAddress().equals(user))
					players.add(player);

			if(!players.isEmpty())
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.temp-muted", renderer, players);
		} else {
			Player target = Bukkit.getPlayer(user);

			if(target != null)
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.temp-muted", renderer, target);
		}

		return entry;
	}

	public Entry kick(String user, String administrator, String reason) {
		BanManagement management = getManagement();

		Entry entry = new Entry(management.generateId(), BanType.KICK, user, reason, administrator, 0);

		management.saveEntry(entry);

		TextRenderer renderer = renderer().placeholder("user", entry.getUser())
		        .placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
		        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId())
		        .placeholder("startDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))));

		List<CommandSender> admins = new ArrayList<>();

		admins.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.hasPermission(API.get().getConfigManager().getCommands().getString("kick.perms.broadcast")))
				admins.add(player);

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.kicked", renderer, admins);

		String kickMessage = renderer.render(StringUtils.join(API.get().getConfigManager().getMain().getStringList("bansystem.kicked"), "\n"));

		if(isIPv4(user) || isIPv6(user)) {
			for(Player player : BukkitLoader.getOnlinePlayers())
				if(player.getAddress().getAddress().getHostAddress().equals(user))
					player.kickPlayer(kickMessage);
		} else {
			Player target = Bukkit.getPlayer(user);

			if(target != null)
				target.kickPlayer(kickMessage);
		}

		return entry;
	}

	public Entry warn(String user, String administrator, String reason) {
		BanManagement management = getManagement();

		Entry entry = new Entry(management.generateId(), BanType.WARN, user, reason, administrator, 0);

		management.saveEntry(entry);

		TextRenderer renderer = renderer().placeholder("user", entry.getUser())
		        .placeholder("reason", entry.getReason() == null ? API.get().getConfigManager().getMain().getString("bansystem.not-specified-reason") : entry.getReason())
		        .placeholder("admin", entry.getAdmin() == null ? "Console" : entry.getAdmin()).placeholder("id", entry.getId())
		        .placeholder("startDate", format.format(Date.from(Instant.ofEpochSecond(entry.getStartDate()))));

		List<CommandSender> admins = new ArrayList<>();

		admins.add(Bukkit.getConsoleSender());

		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.hasPermission(API.get().getConfigManager().getCommands().getString("warn.perms.broadcast")))
				admins.add(player);

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), "bansystem.warned", renderer, admins);

		if(isIPv4(user) || isIPv6(user)) {
			List<Player> players = new ArrayList<>();

			for(Player player : BukkitLoader.getOnlinePlayers())
				if(player.getAddress().getAddress().getHostAddress().equals(user))
					players.add(player);

			if(!players.isEmpty())
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.warned", renderer, players);
		} else {
			Player target = Bukkit.getPlayer(user);

			if(target != null)
				API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(), "bansystem.warned", renderer, target);
		}

		return entry;
	}

	public List<Entry> getHistory(String userName, String userIp, int limit) {
		return getManagement().retrieveHistory(userName, userIp, limit);
	}

	public List<Entry> getHistory(int limit) {
		return getManagement().retrieveHistory(limit);
	}

	public List<Entry> getActivePunishments(String userName, String userIp) {
		return getManagement().retrieveActivePunishments(userName, userIp);
	}

	public List<Entry> getActivePunishments(String userName, String userIp, BanType type) {
		return getManagement().retrieveActivePunishments(userName, userIp, type);
	}

	public List<Entry> getActivePunishments(BanType type) {
		return getManagement().retrieveActivePunishments(type);
	}

	public List<Entry> getActivePunishments() {
		return getManagement().retrieveActivePunishments();
	}

	public void saveModifiedEntry(Entry entry) {
		getManagement().saveEntry(entry);
	}

	public static boolean isIPv6(String user) {
		byte mode = 0;
		int count = 0;
		int groups = 0;

		for(int i = 0; i < user.length(); ++i) {
			char c = user.charAt(i);

			switch(mode) {
				case 0 :
					if(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {

						++count;
						mode = 1;
						continue;
					}

					return false;

				case 1 :
					if(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {

						if(++count > 4)
							return false;

						continue;
					}

					if(c == ':') {
						count = 0;

						if(++groups == 7)
							mode = 2;
						else
							mode = 0;

						continue;
					}

					return false;

				case 2 :
					if(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {

						if(++count > 4)
							return false;

						continue;
					}

					if(c == ':' && user.length() - 1 == i)
						return true;

					return false;
			}
		}

		return mode == 2 && count < 5 && count > 0;
	}

	public static boolean isIPv4(String input) {
		if(input.length() < 7)
			return false;

		byte mode = 0;
		int group = 0;
		int count = 0;

		char first = 0;
		char second = 0;
		char third = 0;

		for(int i = 0; i < input.length(); ++i) {
			char c = input.charAt(i);

			switch(mode) {
				case 0 :
					if(c >= '0' && c <= '9') {
						switch(++count) {
							case 1 :
								first = c;
								break;

							case 2 :
								second = c;
								break;

							case 3 :
								third = c;
								break;

							default :
								return false;
						}

						mode = 1;
						continue;
					}

					return false;

				case 1 :
					if(c == '.') {
						if(count == 0)
							return false;

						++group;

						count = 0;
						first = 0;
						second = 0;
						third = 0;
						mode = 0;

						continue;
					}

					if(c >= '0' && c <= '9') {
						switch(++count) {
							case 1 :
								first = c;
								break;

							case 2 :
								second = c;
								break;

							case 3 :
								third = c;
								break;

							default :
								return false;
						}

						if(count == 3 && (first > '2' || first == '2' && second > '5' || first == '2' && second == '5' && third > '5'))
							return false;

						continue;
					}

					return false;
			}
		}

		return group == 3 && count != 0;
	}

	public interface BanManagement {

		int generateId();

		void saveEntry(Entry entry);

		List<Entry> retrieveHistory(String userName, String userIp, int limit);

		List<Entry> retrieveHistory(int limit);

		List<Entry> retrieveActivePunishments(String userName, String userIp);

		List<Entry> retrieveActivePunishments(String userName, String userIp, BanType type);

		List<Entry> retrieveActivePunishments(BanType type);

		List<Entry> retrieveActivePunishments();

		void save();
	}
}
