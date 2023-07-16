package me.devtec.craftyserversystem.managers;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.command.CommandSender;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.API;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.utility.StringUtils;
import me.devtec.shared.utility.StringUtils.FormatType;
import me.devtec.shared.utility.TimeUtils;

public class CooldownManager {
	private Map<String, CooldownHolder> map = new HashMap<>();

	@Nullable
	public CooldownHolder getCooldown(String id) {
		return map.get(id);
	}

	@Nullable
	public CooldownHolder getOrPrepare(String id) {
		CooldownHolder cd = map.get(id);
		if (cd == null) {
			Config cdConfig = Loader.getPlugin().getConfigManager().getCooldowns();
			if (cdConfig.exists(id)) {
				String timeInString = cdConfig.getString(id + ".time", "0");
				if (timeInString.equalsIgnoreCase("per-group")) {
					Map<String, Long> timePerGroup = new HashMap<>();
					for (String group : cdConfig.getKeys(id + ".per-group"))
						timePerGroup.put(group, TimeUtils.timeFromString(cdConfig.getString(id + ".per-group." + group)));
					if (!timePerGroup.containsKey("default")) {
						Loader.getPlugin().getLogger().severe("In cooldown '" + id + "' in the per-group section the group 'default' is missing, set the time to 5min.");
						timePerGroup.put("default", 300L);
					}
					String bypassPermPre = cdConfig.getString(id + ".bypass-perm");
					if (bypassPermPre == null || bypassPermPre.trim().isEmpty())
						bypassPermPre = null;
					final String bypassPerm = bypassPermPre;

					String msgCdPre = cdConfig.getString(id + ".cooldown-message");
					boolean sendMessage = msgCdPre != null && !msgCdPre.trim().isEmpty() ? true : false;

					cd = new CooldownHolder(id) {

						@Override
						public boolean accept(CommandSender sender) {
							if (bypassPerm != null && sender.hasPermission(bypassPerm))
								return true; // Skip whole cooldown checker

							long currentTime = System.currentTimeMillis() / 1000;
							Config file = API.getUser(sender.getName());

							String userGroup = Loader.getPlugin().getPermissionHook().getGroup(sender);

							long lastUsedTime = file.getLong("css.cd." + id());
							long nextUsageIn = lastUsedTime - currentTime;
							if (nextUsageIn <= 0) {
								Long time = timePerGroup.get(userGroup);
								if (time == null)
									time = timePerGroup.get("default");
								file.set("css.cd." + id(), currentTime + time);
								return true;
							}
							if (sendMessage)
								Loader.getPlugin().getMsgManager().sendMessageFromFile(cdConfig, id + ".cooldown-message",
										PlaceholdersExecutor.i().add("time", StringUtils.formatDouble(FormatType.NORMAL, nextUsageIn)), sender);
							return false;
						}
					};
				} else {
					long time = TimeUtils.timeFromString(timeInString);
					boolean isGlobal = cdConfig.getBoolean(id + ".isGlobal");
					String bypassPermPre = cdConfig.getString(id + ".bypass-perm");
					if (bypassPermPre == null || bypassPermPre.trim().isEmpty())
						bypassPermPre = null;
					final String bypassPerm = bypassPermPre;

					String msgCdPre = cdConfig.getString(id + ".cooldown-message");
					boolean sendMessage = msgCdPre != null && !msgCdPre.trim().isEmpty() ? true : false;

					if (isGlobal)
						cd = new CooldownHolder(id) {
							long lastUsedTime;

							@Override
							public boolean accept(CommandSender sender) {
								if (bypassPerm != null && sender.hasPermission(bypassPerm))
									return true; // Skip whole cooldown checker

								long currentTime = System.currentTimeMillis() / 1000;
								long nextUsageIn = lastUsedTime - currentTime;
								if (nextUsageIn <= 0) {
									lastUsedTime = currentTime + time;
									return true;
								}
								if (sendMessage)
									Loader.getPlugin().getMsgManager().sendMessageFromFile(cdConfig, id + ".cooldown-message",
											PlaceholdersExecutor.i().add("time", StringUtils.formatDouble(FormatType.NORMAL, nextUsageIn)), sender);
								return false;
							}
						};
					else
						cd = new CooldownHolder(id) {

							@Override
							public boolean accept(CommandSender sender) {
								if (bypassPerm != null && sender.hasPermission(bypassPerm))
									return true; // Skip whole cooldown checker

								long currentTime = System.currentTimeMillis() / 1000;
								Config file = API.getUser(sender.getName());
								long lastUsedTime = file.getLong("css.cd." + id());
								long nextUsageIn = lastUsedTime - currentTime;
								if (nextUsageIn <= 0) {
									file.set("css.cd." + id(), currentTime + time);
									return true;
								}
								if (sendMessage)
									Loader.getPlugin().getMsgManager().sendMessageFromFile(cdConfig, id + ".cooldown-message",
											PlaceholdersExecutor.i().add("time", StringUtils.formatDouble(FormatType.NORMAL, nextUsageIn)), sender);
								return false;
							}
						};
				}
				register(cd);
			}
		}
		return cd;
	}

	public void register(CooldownHolder cooldown) {
		map.put(cooldown.id(), cooldown);
	}

	public void unregister(String id) {
		map.remove(id);
	}
}
