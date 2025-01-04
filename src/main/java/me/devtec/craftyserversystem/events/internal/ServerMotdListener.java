package me.devtec.craftyserversystem.events.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.events.Event;
import me.devtec.shared.events.EventListener;
import me.devtec.shared.events.ListenerHolder;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.MathUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.events.ServerListPingEvent;
import me.devtec.theapi.bukkit.nms.GameProfileHandler;

public class ServerMotdListener implements CssListener {

	private ListenerHolder listener;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getServerMotd();
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("enabled");
	}

	@Override
	public void reload() {
		if (listener != null)
			unregister();

		List<Motd> motds = new ArrayList<>();
		for (String key : getConfig().getKeys("motds")) {
			List<GameProfileHandler> list = null;
			if (getConfig().getBoolean("motds." + key + ".slots.modified-list")) {
				list = new ArrayList<>();
				for (String value : getConfig().getStringList("motds." + key + ".slots.list"))
					list.add(GameProfileHandler.of(value, UUID.randomUUID()));
			}
			Motd motd = new Motd(getConfig().getString("motds." + key + ".slots.online"), getConfig().getString("motds." + key + ".slots.max-online"), list,
					getConfig().getString("motds." + key + ".motd").replace("\\n", "\n"), getConfig().getString("motds." + key + ".icon"),
					getConfig().getString("motds." + key + ".version") == null || getConfig().getString("motds." + key + ".version").isEmpty() ? null
							: getConfig().getString("motds." + key + ".version"));
			motds.add(motd);
		}
		listener = new EventListener() {

			@Override
			public void listen(Event event) {
				ServerListPingEvent e = (ServerListPingEvent) event;
				Motd motd = StringUtils.randomFromList(motds);
				if (motd.hasFavicon())
					e.setFavicon(motd.getFavicon());
				if (motd.hasMotd())
					e.setMotd(motd.getMotd());
				if (motd.hasSlots())
					e.setPlayersText(motd.getSlots());
				if (motd.hasVersion()) {
					e.setProtocol(-1);
					e.setVersion(motd.getVersion());
				}
				e.setOnlinePlayers(motd.getOnlinePlayers());
				e.setMaxPlayers(motd.getMaxPlayers());
			}
		}.build().listen(ServerListPingEvent.class);
	}

	@Override
	public void unregister() {
		listener.unregister();
		listener = null;
	}

	public static class Motd {
		private String online;
		private String max;
		private List<GameProfileHandler> playersText;
		private String motd;
		private String favicon;
		private String version;

		public Motd(String online, String max, List<GameProfileHandler> playersText, String motd, String favicon, String version) {
			this.motd = motd;
			this.online = online;
			this.max = max;
			this.playersText = playersText;
			this.favicon = favicon;
			this.version = version;
		}

		public int getOnlinePlayers() {
			return online == null ? BukkitLoader.getOnlinePlayers().size()
					: (int) MathUtils
							.calculate(PlaceholderAPI.apply(online.replace("{online}", BukkitLoader.getOnlinePlayers().size() + "").replace("{max-players}", Bukkit.getMaxPlayers() + ""), null));
		}

		public int getMaxPlayers() {
			return max == null ? Bukkit.getMaxPlayers()
					: (int) MathUtils.calculate(PlaceholderAPI.apply(max.replace("{online}", BukkitLoader.getOnlinePlayers().size() + "").replace("{max-players}", Bukkit.getMaxPlayers() + ""), null));
		}

		public boolean hasMotd() {
			return motd != null;
		}

		@Nullable
		public String getMotd() {
			return motd == null ? null
					: PlaceholderAPI.apply(ColorUtils.colorize(motd).replace("{online}", BukkitLoader.getOnlinePlayers().size() + "").replace("{max-players}", Bukkit.getMaxPlayers() + ""), null);
		}

		public boolean hasFavicon() {
			return favicon != null;
		}

		@Nullable
		public String getFavicon() {
			return favicon == null ? null : PlaceholderAPI.apply(favicon, null);
		}

		public boolean hasVersion() {
			return version != null;
		}

		@Nullable
		public String getVersion() {
			return version == null ? null
					: PlaceholderAPI.apply(ColorUtils.colorize(version).replace("{online}", BukkitLoader.getOnlinePlayers().size() + "").replace("{max-players}", Bukkit.getMaxPlayers() + ""), null);
		}

		public boolean hasSlots() {
			return playersText != null;
		}

		@Nullable
		public List<GameProfileHandler> getSlots() {
			if (playersText == null)
				return null;
			List<GameProfileHandler> slots = new ArrayList<>(playersText.size());
			for (GameProfileHandler handler : playersText)
				slots.add(GameProfileHandler.of(PlaceholderAPI
						.apply(ColorUtils.colorize(handler.getUsername()).replace("{online}", BukkitLoader.getOnlinePlayers().size() + "").replace("{max-players}", Bukkit.getMaxPlayers() + ""), null),
						handler.getUUID()));
			return slots;
		}
	}
}
