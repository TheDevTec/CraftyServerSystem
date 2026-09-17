package me.devtec.craftyserversystem.events.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.api.events.AfkToggleEvent;
import me.devtec.craftyserversystem.commands.internal.afk.AfkManager;
import me.devtec.craftyserversystem.events.CssListener;
import me.devtec.shared.Pair;
import me.devtec.shared.Ref;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.events.EventManager;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.utility.MathUtils;
import me.devtec.shared.utility.TimeUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.packetlistener.ChannelContainer;
import me.devtec.theapi.bukkit.packetlistener.PacketContainer;
import me.devtec.theapi.bukkit.packetlistener.PacketListener;

public class AfkListener implements CssListener {

	public static Map<UUID, Long> autoAfk;

	private Map<UUID, int[]> positions;
	private Map<UUID, Pair> movementLocs;

	private boolean invClickEvent;
	private boolean commandEvent;
	private boolean blockPlace;

	private PacketListener packetListener;
	private int task;

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getMain();
	}

	@Override
	public boolean isEnabled() {
		return API.get().getCommandManager().getRegistered().containsKey("afk");
	}

	@Override
	public void reload() {
		if(task != 0) {
			Scheduler.cancelTask(task);
			task = 0;

			if(packetListener != null)
				packetListener.unregister();

			packetListener = null;
		}

		invClickEvent = getConfig().getBoolean("afk.inventory-click-reset-afk");
		commandEvent = getConfig().getBoolean("afk.command-reset-afk");
		blockPlace = getConfig().getBoolean("afk.block-place-reset-afk");

		boolean movementEvent = getConfig().getBoolean("afk.movement-reset-afk");
		boolean sameMovementPattern = getConfig().getBoolean("afk.check-same-pattern-movement");

		long afkTime = Math.max(TimeUtils.timeFromString(getConfig().getString("afk.time")), 0);

		if(afkTime != 0) {
			autoAfk = new ConcurrentHashMap<>();

			task = new Tasker() {

				@Override
				public void run() {
					for(Entry<UUID, Long> entry : autoAfk.entrySet()) {
						if(entry.getValue() + afkTime - System.currentTimeMillis() / 1000 > 0)
							continue;

						Config user = me.devtec.shared.API.getUser(entry.getKey());

						if(user.getBoolean("afk"))
							continue;

						AfkToggleEvent event = new AfkToggleEvent(entry.getKey(), true);
						EventManager.call(event);

						if(event.isCancelled())
							return;

						user.set("afk", true);

						TextRenderer renderer = TextRenderer.forTarget(entry.getKey()).placeholder("prefix", API.get().getConfigManager().getPrefix())
						        .placeholder("player", me.devtec.shared.API.offlineCache().lookupNameById(entry.getKey())).colorize();

						API.get().getMsgManager().sendMessageFromFile(getConfig(), "afk.start.broadcast", renderer, BukkitLoader.getOnlinePlayers());

						BukkitLoader.getNmsProvider().postToMainThread(() -> {
							for(String command : getConfig().getStringList("afk.start.commands"))
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), render(command, renderer, entry.getKey()));
						});
					}
				}
			}.runRepeating(20, 20);
		}

		if(movementEvent) {
			positions = new ConcurrentHashMap<>();

			if(sameMovementPattern)
				movementLocs = new ConcurrentHashMap<>();

			Class<?> movementPacketClass = Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ServerboundMovePlayerPacket" : "PacketPlayInFlying");

			Field xField;
			Field yField;
			Field zField;
			Field yawField;
			Field pitchField;
			Field changedHead;
			Field changedPosition;

			if(BukkitLoader.NO_OBFUSCATED_NMS_MODE) {
				xField = Ref.field(movementPacketClass, "x");
				yField = Ref.field(movementPacketClass, "y");
				zField = Ref.field(movementPacketClass, "z");
				yawField = Ref.field(movementPacketClass, "yRot");
				pitchField = Ref.field(movementPacketClass, "xRot");
				changedHead = Ref.field(movementPacketClass, "hasRot");
				changedPosition = Ref.field(movementPacketClass, "hasPos");
			} else if(Ref.isBefore(17, 0)) {
				xField = Ref.field(movementPacketClass, "x");
				yField = Ref.field(movementPacketClass, "y");
				zField = Ref.field(movementPacketClass, "z");
				yawField = Ref.field(movementPacketClass, "yaw");
				pitchField = Ref.field(movementPacketClass, "pitch");
				changedHead = Ref.field(movementPacketClass, "hasLook");
				changedPosition = Ref.field(movementPacketClass, "hasPos");
			} else {
				xField = Ref.field(movementPacketClass, "a");
				yField = Ref.field(movementPacketClass, "b");
				zField = Ref.field(movementPacketClass, "c");
				yawField = Ref.field(movementPacketClass, "d");
				pitchField = Ref.field(movementPacketClass, "e");
				changedHead = Ref.field(movementPacketClass, "h");
				changedPosition = Ref.field(movementPacketClass, "g");
			}

			packetListener = new PacketListener() {

				@Override
				public void playOut(String player, PacketContainer container, ChannelContainer channel) {
				}

				@Override
				public void playIn(String player, PacketContainer container, ChannelContainer channel) {
					Object packet = container.getPacket();

					if(!movementPacketClass.isAssignableFrom(packet.getClass()) && !movementPacketClass.equals(packet.getClass()))
						return;

					int x = MathUtils.floor((double) Ref.get(packet, xField));
					int y = MathUtils.floor((double) Ref.get(packet, yField));
					int z = MathUtils.floor((double) Ref.get(packet, zField));

					UUID uuid = me.devtec.shared.API.offlineCache().lookupId(player);

					if((boolean) Ref.get(packet, changedHead) && sameMovementPattern) {
						Pair pair = movementLocs.get(uuid);

						if(pair != null) {
							Pair sub = (Pair) pair.getValue();

							float yaw = (float) Ref.get(packet, yawField);
							float pitch = (float) Ref.get(packet, pitchField);

							float[] rotation = (float[]) sub.getValue();

							if(rotation[0] != yaw || rotation[1] != pitch) {
								rotation[0] = yaw;
								rotation[1] = pitch;
								sub.setKey(0);
							}
						}
					}

					if((boolean) Ref.get(packet, changedPosition) && (!sameMovementPattern || !checkIfInsideWaterFlow(uuid, x, y, z))) {

						int[] previous = positions.computeIfAbsent(uuid, id -> new int[]{x, z});

						if(x != previous[0] || z != previous[1]) {
							positions.put(uuid, new int[]{x, z});
							AfkManager.getProvider().stopAfk(uuid, true);
						}
					}
				}

				private boolean checkIfInsideWaterFlow(UUID uuid, int x, int y, int z) {
					Pair pair = movementLocs.computeIfAbsent(uuid, id -> Pair.of(new ArrayList<>(), Pair.of(0, new float[2])));

					@SuppressWarnings("unchecked")
					List<int[]> movements = (List<int[]>) pair.getKey();

					int[] start = {x, y, z};
					Pair sub = (Pair) pair.getValue();

					if(movements.size() > 1)
						for(int[] currentMovement : movements) {
							if(!equals(currentMovement, start))
								continue;

							if((int) sub.getKey() >= 2)
								return true;

							sub.setKey((int) sub.getKey() + 1);
							return false;
						}

					movements.add(start);

					if(movements.size() > 10)
						movements.remove(0);

					return false;
				}

				private boolean equals(int[] first, int[] second) {
					return first[0] == second[0] && first[1] == second[1] && first[2] == second[2];
				}
			};

			packetListener.register();
		}
	}

	private String render(String text, TextRenderer renderer, UUID target) {
		return renderer.render(PlaceholderAPI.apply(text, target), target);
	}

	@Override
	public void unregister() {
		if(task != 0) {
			Scheduler.cancelTask(task);
			task = 0;
		}

		if(packetListener != null)
			packetListener.unregister();

		packetListener = null;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		AfkManager.getProvider().stopAfk(event.getPlayer().getUniqueId(), false);
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		AfkManager.getProvider().stopAfk(event.getPlayer().getUniqueId(), true);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if(blockPlace)
			AfkManager.getProvider().stopAfk(event.getPlayer().getUniqueId(), true);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();

		AfkManager.getProvider().stopAfk(uuid, false);

		if(autoAfk != null)
			autoAfk.remove(uuid);

		if(positions != null)
			positions.remove(uuid);

		if(movementLocs != null)
			movementLocs.remove(uuid);
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		if(invClickEvent)
			AfkManager.getProvider().stopAfk(event.getWhoClicked().getUniqueId(), true);
	}

	@EventHandler
	public void onInvDrag(InventoryDragEvent event) {
		if(invClickEvent)
			AfkManager.getProvider().stopAfk(event.getWhoClicked().getUniqueId(), true);
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if(commandEvent && !isAfkCommand(event.getMessage().substring(1).toLowerCase().split(" ")[0]))
			AfkManager.getProvider().stopAfk(event.getPlayer().getUniqueId(), true);
	}

	private boolean isAfkCommand(String cmd) {
		for(String afkCommand : API.get().getConfigManager().getCommands().getStringList("afk.cmd"))
			if(cmd.equals(afkCommand.toLowerCase()))
				return true;

		return false;
	}
}
