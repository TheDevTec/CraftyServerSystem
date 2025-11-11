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
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Pair;
import me.devtec.shared.Ref;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.events.EventManager;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
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

	@Override
	public Config getConfig() {
		return API.get().getConfigManager().getMain();
	}

	@Override
	public boolean isEnabled() {
		return API.get().getCommandManager().getRegistered().containsKey("afk");
	}

	private int task;

	@Override
	public void reload() {
		if (task != 0) {
			Scheduler.cancelTask(task);
			task = 0;
			if (packetListener != null)
				packetListener.unregister();
			packetListener = null;
		}
		invClickEvent = getConfig().getBoolean("afk.inventory-click-reset-afk");
		commandEvent = getConfig().getBoolean("afk.command-reset-afk");
		blockPlace = getConfig().getBoolean("afk.block-place-reset-afk");
		boolean movementEvent = getConfig().getBoolean("afk.movement-reset-afk");
		boolean sameMovementPattern = getConfig().getBoolean("afk.check-same-pattern-movement");
		long afkTime = Math.max(TimeUtils.timeFromString(getConfig().getString("afk.time")), 0);
		if (afkTime != 0) {
			autoAfk = new ConcurrentHashMap<>();
			task = new Tasker() {

				@Override
				public void run() {
					for (Entry<UUID, Long> entry : autoAfk.entrySet())
						if (entry.getValue() + afkTime - System.currentTimeMillis() / 1000 <= 0) {
							Config user = me.devtec.shared.API.getUser(entry.getKey());
							if (!user.getBoolean("afk")) {
								AfkToggleEvent event = new AfkToggleEvent(entry.getKey(), true);
								EventManager.call(event);
								if(event.isCancelled())return;
								user.set("afk", true);
								PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("player", me.devtec.shared.API.offlineCache().lookupNameById(entry.getKey())).papi(entry.getKey());
								// Send json message
								API.get().getMsgManager().sendMessageFromFile(getConfig(), "afk.start.broadcast", placeholders, BukkitLoader.getOnlinePlayers());
								BukkitLoader.getNmsProvider().postToMainThread(() -> {
									for (String cmd : placeholders.apply(getConfig().getStringList("afk.start.commands")))
										Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
								});
							}
						}
				}
			}.runRepeating(20, 20);
		}
		if (movementEvent) {
			positions = new ConcurrentHashMap<>();
			if (sameMovementPattern)
				movementLocs = new ConcurrentHashMap<>();
			Class<?> movementPacketClass = Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ServerboundMovePlayerPacket" : "PacketPlayInFlying");
			Field xField;
			Field yField;
			Field zField;
			Field yawField;
			Field pitchField;
			Field changedHead;
			Field changedPosition;
			if (BukkitLoader.NO_OBFUSCATED_NMS_MODE) {
				xField = Ref.field(movementPacketClass, "x");
				yField = Ref.field(movementPacketClass, "y");
				zField = Ref.field(movementPacketClass, "z");
				yawField = Ref.field(movementPacketClass, "yRot");
				pitchField = Ref.field(movementPacketClass, "xRot");
				changedHead = Ref.field(movementPacketClass, "hasRot");
				changedPosition = Ref.field(movementPacketClass, "hasPos");
			} else if (Ref.isOlderThan(17)) {
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
					if (movementPacketClass.isAssignableFrom(container.getPacket().getClass()) || movementPacketClass.equals(container.getPacket().getClass())) {
						Object pos = container.getPacket();
						int x = MathUtils.floor((double) Ref.get(pos, xField));
						int y = MathUtils.floor((double) Ref.get(pos, yField));
						int z = MathUtils.floor((double) Ref.get(pos, zField));
						UUID uuid = me.devtec.shared.API.offlineCache().lookupId(player);
						if ((boolean) Ref.get(pos, changedHead) && sameMovementPattern) {
							Pair pair = movementLocs.get(uuid);
							if (pair != null) {
								Pair sub = (Pair) pair.getValue();
								float yaw = (float) Ref.get(pos, yawField);
								float pitch = (float) Ref.get(pos, pitchField);
								if (((float[]) sub.getValue())[0] != yaw || ((float[]) sub.getValue())[1] != pitch) {
									((float[]) sub.getValue())[0] = yaw;
									((float[]) sub.getValue())[1] = pitch;
									sub.setKey(0);
								}
							}
						}
						if ((boolean) Ref.get(pos, changedPosition) && (sameMovementPattern ? !checkIfInsideWaterFlow(uuid, x, y, z) : true)) {
							int[] previous = positions.computeIfAbsent(uuid, id -> new int[] { x, z });
							if (x != previous[0] || z != previous[1]) {
								positions.put(uuid, new int[] { x, z });
								AfkManager.getProvider().stopAfk(uuid, true);
							}
						}
					}
				}

				private boolean checkIfInsideWaterFlow(UUID uuid, int x, int y, int z) {
					Pair pair = movementLocs.computeIfAbsent(uuid, i -> Pair.of(new ArrayList<>(), Pair.of(0, new float[2])));
					@SuppressWarnings("unchecked")
					List<int[]> movements = (List<int[]>) pair.getKey();
					int[] start = { x, y, z };

					Pair sub = (Pair) pair.getValue();

					if (movements.size() > 1)
						for (int[] currentMovement : movements)
							if (equals(currentMovement, start)) {
								if ((int) sub.getKey() >= 2)
									return true;
								sub.setKey((int) sub.getKey() + 1);
								return false;
							}
					movements.add(start);
					if (movements.size() > 10)
						movements.remove(0);
					return false;
				}

				private boolean equals(int[] is, int[] is2) {
					return is[0] == is2[0] && is[1] == is2[1] && is[2] == is2[2];
				}
			};
			packetListener.register();
		}
	}

	@Override
	public void unregister() {
		if (task != 0) {
			Scheduler.cancelTask(task);
			task = 0;
		}
		if (packetListener != null)
			packetListener.unregister();
		packetListener = null;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		AfkManager.getProvider().stopAfk(e.getPlayer().getUniqueId(), false);
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		AfkManager.getProvider().stopAfk(e.getPlayer().getUniqueId(), true);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if (blockPlace)
			AfkManager.getProvider().stopAfk(e.getPlayer().getUniqueId(), true);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		AfkManager.getProvider().stopAfk(e.getPlayer().getUniqueId(), false);
		if (autoAfk != null)
			autoAfk.remove(e.getPlayer().getUniqueId());
		if (positions != null)
			positions.remove(e.getPlayer().getUniqueId());
		if (movementLocs != null)
			movementLocs.remove(e.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent e) {
		if (invClickEvent)
			AfkManager.getProvider().stopAfk(e.getWhoClicked().getUniqueId(), true);
	}

	@EventHandler
	public void onInvDrag(InventoryDragEvent e) {
		if (invClickEvent)
			AfkManager.getProvider().stopAfk(e.getWhoClicked().getUniqueId(), true);
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (commandEvent && !isAfkCommand(e.getMessage().substring(1).toLowerCase().split(" ")[0]))
			AfkManager.getProvider().stopAfk(e.getPlayer().getUniqueId(), true);
	}

	private boolean isAfkCommand(String cmd) {
		for (String afkCommand : API.get().getConfigManager().getCommands().getStringList("afk.cmd"))
			if (cmd.equals(afkCommand.toLowerCase()))
				return true;
		return false;
	}
}
