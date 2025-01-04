package me.devtec.craftyserversystem.commands.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.json.Json;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class CssUser extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}, (sender, structure, args) -> {
			List<String> tablist = new ArrayList<>();
			tablist.add("{offlinePlayer}");
			tablist.add("{uuid}");
			for (Player player : BukkitLoader.getOnlinePlayers())
				tablist.add(player.getName());
			return tablist;
		});
		// get [path]
		cmd.argument("get", (sender, structure, args) -> {
			msgUsage(sender, "get");
		}).argument(null, 1, (sender, structure, args) -> {
			Config user;
			String name = null;
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			if (!user.existsKey(args[2]))
				msg(sender, "does-not-exist", PlaceholdersExecutor.i().add("user", name).add("path", args[2]));
			else
				msg(sender, "get", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", "" + user.get(args[2])));
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		});
		// keys {path}
		cmd.argument("keys", (sender, structure, args) -> {
			Config user;
			String name = null;
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			if (user.getKeys().isEmpty())
				msg(sender, "keys.empty-primary", PlaceholdersExecutor.i().add("user", name));
			else
				msg(sender, "keys.primary", PlaceholdersExecutor.i().add("user", name).add("keys", StringUtils.join(user.getKeys(), ", ")));
		}).argument(null, 1, (sender, structure, args) -> {
			Config user;
			String name = null;
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			if (!user.exists(args[2]))
				msg(sender, "does-not-exist", PlaceholdersExecutor.i().add("user", name).add("path", args[2]));
			else {
				Set<String> keys = user.getKeys(args[2]);
				if (keys.isEmpty())
					msg(sender, "keys.empty", PlaceholdersExecutor.i().add("user", name).add("path", args[2]));
				else
					msg(sender, "keys.sub", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("keys", StringUtils.join(keys, ", ")));
			}
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		});
		// addNumber [path] [number]
		cmd.argument("addNumber", (sender, structure, args) -> {
			msgUsage(sender, "addNumber");
		}).argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "addNumber");
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		}).selector(Selector.NUMBER, (sender, structure, args) -> {
			Config user;
			String name = null;
			Number number = ParseUtils.getNumber(args[3]);
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			Object setValue = user.get(args[2]);
			Number finalValue = number;
			if (setValue == null)
				user.set(args[2], number);
			else if (setValue.getClass() == int.class || setValue.getClass() == Integer.class)
				user.set(args[2], finalValue = ((Integer) setValue).intValue() + number.intValue());
			else if (setValue.getClass() == double.class || setValue.getClass() == Double.class)
				user.set(args[2], finalValue = ((Double) setValue).doubleValue() + number.doubleValue());
			else if (setValue.getClass() == float.class || setValue.getClass() == Float.class)
				user.set(args[2], finalValue = ((Float) setValue).floatValue() + number.floatValue());
			else if (setValue.getClass() == long.class || setValue.getClass() == Long.class)
				user.set(args[2], finalValue = ((Long) setValue).longValue() + number.longValue());
			else if (setValue.getClass() == byte.class || setValue.getClass() == Byte.class)
				user.set(args[2], finalValue = ((Byte) setValue).byteValue() + number.byteValue());
			else if (setValue.getClass() == short.class || setValue.getClass() == Short.class)
				user.set(args[2], finalValue = ((Short) setValue).shortValue() + number.shortValue());
			else if (setValue.getClass() == BigDecimal.class) {
				BigDecimal decimal = (BigDecimal) setValue;
				decimal.add(BigDecimal.valueOf(number.doubleValue()));
				user.set(args[2], decimal);
				finalValue = decimal.doubleValue();
			} else if (setValue.getClass() == BigInteger.class) {
				BigInteger decimal = (BigInteger) setValue;
				decimal.add(BigInteger.valueOf(number.longValue()));
				user.set(args[2], decimal);
				finalValue = decimal.longValue();
			} else
				user.set(args[2], number); // uknown number format - So we override value
			msg(sender, "addNumber", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", finalValue + ""));
		});
		// addToList [path] [value]
		cmd.argument("addToList", (sender, structure, args) -> {
			msgUsage(sender, "addToList");
		}, "atolist").argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "addToList");
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		}).argument(null, -1, (sender, structure, args) -> {
			Config user;
			String name = null;
			Object value = Json.reader().read(StringUtils.buildString(3, args));
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			Collection<Object> list = user.getList(args[2]);
			if (list == null)
				user.set(args[2], list = new ArrayList<>());
			boolean added = list.add(value);
			if (added) {
				user.set(args[2], list);
				msg(sender, "addToList.success", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", value + ""));
			} else
				msg(sender, "addToList.failed", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", value + ""));
		}, (sender, structure, args) -> args[args.length - 1].isEmpty() ? Arrays.asList("\"string\"", "number", "true", "false", "null", "[0,1,2]", "{\"key\":\"value\"}")
				: Arrays.asList(args[args.length - 1], "\"string\"", "number", "true", "false", "null", "[0,1,2]", "{\"key\":\"value\"}"));
		// showList {page}
		cmd.argument("showList", (sender, structure, args) -> {
			msgUsage(sender, "showList");
		}, "listvalues").argument(null, 1, (sender, structure, args) -> {
			Config user;
			String name = null;
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			Collection<Object> list = user.getList(args[2]);
			showList(sender, PlaceholdersExecutor.i().add("user", name).add("path", args[2]), list, 0);
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		}).selector(Selector.INTEGER, (sender, structure, args) -> {
			Config user;
			String name = null;
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			Collection<Object> list = user.getList(args[2]);
			showList(sender, PlaceholdersExecutor.i().add("user", name).add("path", args[2]), list, ParseUtils.getInt(args[3]));
		});
		// removeFromList [path] [value/-pos:{listPos}]
		cmd.argument("removeFromList", (sender, structure, args) -> {
			msgUsage(sender, "removeFromList");
		}, "rfromlist").argument(null, 1, (sender, structure, args) -> {
			Config user;
			String name = null;
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			Collection<Object> list = user.getList(args[2]);
			showList(sender, PlaceholdersExecutor.i().add("user", name).add("path", args[2]), list, 0);
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		}).argument(null, -1, (sender, structure, args) -> {
			Config user;
			String name = null;
			Object value = Json.reader().read(StringUtils.buildString(3, args));
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			Collection<Object> list = user.getList(args[2]);
			if (list == null) {
				msg(sender, "does-not-exist", PlaceholdersExecutor.i().add("user", name).add("path", args[2]));
				return;
			}
			boolean modified = false;
			if (list != null)
				if (value != null && value.toString().startsWith("-pos:")) {
					int pos = Math.max(0, Math.min(ParseUtils.getInt(value.toString(), 5, value.toString().length()), list.size() - 1));
					if (list instanceof List) {
						((List<Object>) list).remove(pos);
						modified = true;
					} else {
						Iterator<Object> itr = list.iterator();
						while (itr.hasNext() && pos != -1) {
							itr.next();
							if (pos-- == 0) {
								itr.remove();
								modified = true;
								break;
							}
						}
					}
				} else
					modified = list.remove(value);
			if (modified) {
				user.set(args[2], list);
				msg(sender, "removeFromList.success", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", value + ""));
			} else
				msg(sender, "removeFromList.failed", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", value + ""));
		}, (sender, structure, args) -> args[args.length - 1].isEmpty() ? Arrays.asList("\"string\"", "number", "true", "false", "null", "[0,1,2]", "{\"key\":\"value\"}")
				: Arrays.asList(args[args.length - 1], "\"string\"", "number", "true", "false", "null", "[0,1,2]", "{\"key\":\"value\"}"));
		// Set [path] [value]
		cmd.argument("set", (sender, structure, args) -> {
			msgUsage(sender, "set");
		}).argument(null, 1, (sender, structure, args) -> {
			msgUsage(sender, "set");
		}, (sender, structure, args) -> {
			Config user;
			try {
				if (args[0].indexOf('-') != -1)
					user = me.devtec.shared.API.getUser(UUID.fromString(args[0]));
				else
					user = me.devtec.shared.API.getUser(args[0]);
			} catch (Exception e) {
				user = me.devtec.shared.API.getUser(args[0]);
			}
			List<String> keys = new ArrayList<>();
			if (args[2].isEmpty() || args[2].indexOf('.') == -1)
				keys.addAll(user.getKeys());
			else if (args[2].endsWith(".")) {
				String path = args[2].substring(0, args[2].length() - 1);
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			} else {
				String path = args[2].substring(0, args[2].lastIndexOf('.'));
				for (String subkey : user.getKeys(path))
					keys.add(path + "." + subkey);
			}
			return keys;
		}).argument(null, -1, (sender, structure, args) -> {
			Config user;
			String name = null;
			Object value = Json.reader().read(StringUtils.buildString(3, args));
			try {
				UUID uuid = UUID.fromString(args[0]);
				name = me.devtec.shared.API.offlineCache().lookupNameById(uuid);
				if (name == null)
					name = "" + uuid;
				user = me.devtec.shared.API.getUser(uuid);
			} catch (Exception e) {
				name = args[0];
				user = me.devtec.shared.API.getUser(args[0]);
			}
			user.set(args[2], value);
			msg(sender, "set", PlaceholdersExecutor.i().add("user", name).add("path", args[2]).add("value", value + ""));
		}, (sender, structure, args) -> args[args.length - 1].isEmpty() ? Arrays.asList("\"string\"", "number", "true", "false", "null", "[0,1,2]", "{\"key\":\"value\"}")
				: Arrays.asList(args[args.length - 1], "\"string\"", "number", "true", "false", "null", "[0,1,2]", "{\"key\":\"value\"}"));

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void showList(CommandSender sender, PlaceholdersExecutor executor, Collection<Object> list, int page) {
		if (list == null) {
			msg(sender, "does-not-exist", executor);
			return;
		}
		if (list.isEmpty()) {
			msg(sender, "showList.empty", executor);
			return;
		}
		int totalPages = list.size() / 10 + (list.size() % 10 == 0 ? 0 : 1) - 1;
		if (page >= totalPages)
			page = totalPages;
		if (page < 0)
			page = 0;
		msg(sender, "showList.header", executor.add("nextPage", page + 1 <= totalPages ? "" + (page + 1) : page + "").add("previousPage", page != 0 ? "" + (page - 1) : "" + page)
				.add("size", list.size() + "").add("page", page + 1).add("totalPages", totalPages + 1));
		if (list instanceof List) {
			List<Object> listObj = (List<Object>) list;
			int multiplier = (page + 1) * 10;
			for (int i = multiplier - 10; i < multiplier && list.size() > i; ++i)
				msg(sender, "showList.entry", PlaceholdersExecutor.i().add("user", executor.get("{user}")).add("path", executor.get("{path}")).add("value", listObj.get(i) + "").add("position", i + 1)
						.add("listPosition", i + ""));
		} else {
			Iterator<Object> itr = list.iterator();
			int pos = (page + 1) * 10 - 10;
			while (itr.hasNext() && pos != -1) {
				itr.next();
				if (pos-- == 0)
					break;
			}
			int multiplier = (page + 1) * 10;
			while (itr.hasNext() && pos != 10) {
				Object obj = itr.next();
				msg(sender, "showList.entry", PlaceholdersExecutor.i().add("user", executor.get("{user}")).add("path", executor.get("{path}")).add("value", obj + "")
						.add("position", (multiplier - 10 + pos + 1) * page + "").add("listPosition", multiplier - 10 + pos + ""));
			}
		}
		msg(sender, "showList.footer", executor);
	}

}
