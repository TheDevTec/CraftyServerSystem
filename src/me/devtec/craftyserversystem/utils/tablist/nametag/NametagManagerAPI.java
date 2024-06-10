package me.devtec.craftyserversystem.utils.tablist.nametag;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.NametagHologram;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.DefaultTeamManager;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.LuckPermsTeamManager;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.TeamManager;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.VaultTeamManager;
import me.devtec.shared.API;
import me.devtec.shared.Ref;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.dataholder.cache.ConcurrentSet;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;
import me.devtec.theapi.bukkit.packetlistener.ChannelContainer;
import me.devtec.theapi.bukkit.packetlistener.PacketContainer;
import me.devtec.theapi.bukkit.packetlistener.PacketListener;

public class NametagManagerAPI {

	private static NametagManagerAPI instance;

	public static NametagManagerAPI get() {
		if (instance == null)
			instance = new NametagManagerAPI();
		return instance;
	}

	private boolean isLoaded;
	private PacketListener listener;
	private volatile Set<NametagPlayer> players = new ConcurrentSet<>();

	protected volatile Map<Integer, List<NametagPlayer>> watchingEntityMove = new ConcurrentHashMap<>();
	private TeamManager teamManager = new DefaultTeamManager();

	private NametagManagerAPI() {

	}

	public boolean isPlayer(int id) {
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.getEntityId() == id)
				return true;
		return false;
	}

	public boolean isPlayer(String name) {
		return Bukkit.getPlayerExact(name) != null;
	}

	public Set<NametagPlayer> getPlayers() {
		return players;
	}

	public NametagPlayer getPlayer(Player player) {
		return lookupByUuidOrCreate(player, player.getUniqueId());
	}

	public NametagPlayer lookupById(int id) {
		synchronized (players) {
			for (NametagPlayer player : players)
				if (player.getPlayer().getEntityId() == id)
					return player;
		}
		return null;
	}

	public NametagPlayer lookupByName(String name) {
		synchronized (players) {
			for (NametagPlayer player : players)
				if (player.getName().equals(name))
					return player;
		}
		return null;
	}

	public NametagPlayer lookupByUuid(UUID uuid) {
		synchronized (players) {
			for (NametagPlayer player : players)
				if (player.getUUID().equals(uuid))
					return player;
		}
		return null;
	}

	public NametagPlayer lookupByUuidOrCreate(Player online, UUID uuid) {
		synchronized (players) {
			for (NametagPlayer player : players)
				if (player.getUUID().equals(uuid))
					return player;
			NametagPlayer as = new NametagPlayer(online.getName(), online.getUniqueId(), teamManager.getTeam(online.getUniqueId())).initNametag(online);
			if (online.getVehicle() != null) {
				List<NametagPlayer> players = new ArrayList<>();
				players.add(as);
				watchingEntityMove.put(online.getVehicle().getEntityId(), players);
			}
			players.add(as);
			return as;

		}
	}

	@Nonnull
	public TeamManager getTeamManager() {
		return teamManager;
	}

	public void setTeamManager(@Nonnull TeamManager manager) {
		teamManager = manager == null ? new DefaultTeamManager() : manager;
	}

	public static Object modifyTeamPacket(Object packet) {
		if (Ref.isNewerThan(16)) {
			Optional<?> optional = (Optional<?>) Ref.get(packet, TeamUtils.parameters);
			if (optional.isPresent())
				Ref.set(optional.get(), TeamUtils.nametagVisibility, "never");
		} else
			Ref.set(packet, TeamUtils.nametagVisibility, "never");
		return packet;

	}

	public void load() {
		isLoaded = true;
		switch (me.devtec.craftyserversystem.api.API.get().getConfigManager().getTab().getString("sorting.by").toUpperCase()) {
		case "GROUP_WEIGHT":
			if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
				teamManager = new LuckPermsTeamManager();
				break;
			}
		case "GROUP":
			if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
				teamManager = new VaultTeamManager();
				break;
			}
		default:
			teamManager = new DefaultTeamManager();
			break;
		}
		teamManager.reload();
		Class<?> entityTypes, outVelocity, outMount, outMetadata, outDestroy, clientBundle, outTeleport, inVehicleMove, outSpawnEntity, clientPlayerInfoUpdate, clientPlayerInfoRemove, entityPose,
				outBed, outAnimation;
		// mount
		Field idField;
		Field mobsField;
		Field isLeashed;
		// destroy
		Field integersField;
		// teleport
		Field entityIdField;
		Field uuidField;
		Field entityTypeField;
		Field listBField;
		Field clientPlayerInfoLegacyAction;
		Field listUuidsField;
		Field listUuidsFieldUpdate;
		Field iterableField;
		Field metaEntityId;
		Field velocityEntityId;
		Field metaData;
		Field playerInfoUuidField;
		Field valueField;
		Field animationEntityId, animationId;
		if (BukkitLoader.NO_OBFUSCATED_NMS_MODE) {
			entityTypes = Ref.nms("world.entity", "EntityType");
			outVelocity = Ref.nms("network.protocol.game", "ClientboundSetEntityMotionPacket");
			outMount = Ref.nms("network.protocol.game", "ClientboundSetEntityLinkPacket");
			outMetadata = Ref.nms("network.protocol.game", "ClientboundSetEntityDataPacket");
			outDestroy = Ref.nms("network.protocol.game", "ClientboundRemoveEntitiesPacket");
			clientBundle = Ref.nms("network.protocol.game", "ClientboundBundlePacket");
			outTeleport = Ref.nms("network.protocol.game", "ClientboundTeleportEntityPacket");
			inVehicleMove = Ref.nms("network.protocol.game", "ServerboundMoveVehiclePacket");
			outSpawnEntity = Ref.nms("network.protocol.game", "ClientboundAddEntityPacket");
			clientPlayerInfoUpdate = Ref.nms("network.protocol.game", "ClientboundPlayerInfoUpdatePacket");
			clientPlayerInfoRemove = Ref.nms("network.protocol.game", "ClientboundPlayerInfoRemovePacket");
			entityPose = Ref.nms("world.entity", "Pose");
			outBed = null;
			outAnimation = null;
			idField = Ref.field(outMount, "sourceId");
			mobsField = Ref.field(outMount, "destId");
			isLeashed = null;
			integersField = Ref.field(outDestroy, "entityIds");
			entityIdField = Ref.field(outTeleport, int.class);
			uuidField = Ref.field(outSpawnEntity, UUID.class);
			entityTypeField = Ref.field(outSpawnEntity, entityTypes);
			listBField = Ref.field(clientPlayerInfoUpdate, List.class);
			clientPlayerInfoLegacyAction = null;
			listUuidsField = Ref.field(clientPlayerInfoRemove, List.class);
			listUuidsFieldUpdate = Ref.field(clientPlayerInfoUpdate, List.class);
			iterableField = Ref.field(clientBundle, Iterable.class);
			metaEntityId = Ref.field(outMetadata, int.class);
			velocityEntityId = Ref.field(outVelocity, int.class);
			metaData = Ref.field(outMetadata, List.class);
			playerInfoUuidField = Ref.field(Ref.nms("network.protocol.game", "ClientboundPlayerInfoUpdatePacket$Entry"), UUID.class);
			valueField = Ref.field(Ref.nms("network.syncher", "SynchedEntityData$DataValue"), "value");
			animationEntityId = null;
			animationId = null;
		} else {
			entityTypes = Ref.nms("world.entity", "EntityTypes");
			outVelocity = Ref.nms("network.protocol.game", "PacketPlayOutEntityVelocity");
			outMount = Ref.nms("network.protocol.game", "PacketPlayOutMount") == null ? Ref.nms("network.protocol.game", "PacketPlayOutAttachEntity")
					: Ref.nms("network.protocol.game", "PacketPlayOutMount");
			outMetadata = Ref.nms("network.protocol.game", "PacketPlayOutEntityMetadata");
			outDestroy = Ref.nms("network.protocol.game", "PacketPlayOutEntityDestroy");
			clientBundle = Ref.nms("network.protocol.game", "ClientboundBundlePacket");
			outTeleport = Ref.nms("network.protocol.game", "PacketPlayOutEntityTeleport");
			inVehicleMove = Ref.nms("network.protocol.game", "PacketPlayInVehicleMove");
			outSpawnEntity = Ref.nms("network.protocol.game", "PacketPlayOutNamedEntitySpawn") == null ? Ref.nms("network.protocol.game", "PacketPlayOutSpawnEntity")
					: Ref.nms("network.protocol.game", "PacketPlayOutNamedEntitySpawn");
			clientPlayerInfoUpdate = Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? Ref.nms("network.protocol.game", "ClientboundPlayerInfoUpdatePacket")
					: Ref.nms("network.protocol.game", "PacketPlayOutPlayerInfo");
			clientPlayerInfoRemove = Ref.nms("network.protocol.game", "ClientboundPlayerInfoRemovePacket");
			entityPose = Ref.nms("world.entity", "EntityPose");
			outBed = Ref.nms("", "PacketPlayOutBed");
			outAnimation = Ref.nms("network.protocol.game", "ClientboundAnimatePacket") == null ? Ref.nms("network.protocol.game", "PacketPlayOutAnimation")
					: Ref.nms("network.protocol.game", "ClientboundAnimatePacket");
			idField = Ref.isOlderThan(12) ? Ref.field(outMount, "c") : Ref.field(outMount, int.class);
			mobsField = Ref.field(outMount, int[].class) == null ? Ref.field(outMount, "b") : Ref.field(outMount, int[].class);
			isLeashed = Ref.field(outMount, "a");
			integersField = Ref.field(outDestroy, "a");
			entityIdField = Ref.field(outTeleport, int.class);
			uuidField = Ref.field(outSpawnEntity, UUID.class);
			entityTypeField = Ref.field(outSpawnEntity, entityTypes);
			listBField = Ref.field(clientPlayerInfoUpdate, List.class);
			clientPlayerInfoLegacyAction = Ref.field(clientPlayerInfoUpdate, "a");
			listUuidsField = Ref.field(clientPlayerInfoRemove, List.class);
			listUuidsFieldUpdate = Ref.field(clientPlayerInfoUpdate, List.class);
			iterableField = Ref.field(clientBundle, Iterable.class);
			metaEntityId = Ref.field(outMetadata, int.class);
			velocityEntityId = Ref.field(outVelocity, int.class);
			metaData = Ref.field(outMetadata, List.class);
			playerInfoUuidField = Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2
					? Ref.field(Ref.nms("network.protocol.game", "ClientboundPlayerInfoUpdatePacket$b"), UUID.class)
					: Ref.field(Ref.nms("network.protocol.game", "PacketPlayOutPlayerInfo$PlayerInfoData"), Ref.getClass("com.mojang.authlib.GameProfile"));
			valueField = Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? Ref.field(Ref.nms("network.syncher", "DataWatcher$b"), "c")
					: Ref.field(Ref.isOlderThan(12) ? Ref.nms("network.syncher", "DataWatcher$WatchableObject") : Ref.nms("network.syncher", "DataWatcher$Item"), Ref.isOlderThan(12) ? "c" : "b");
			animationId = Ref.field(outAnimation, "b");
			animationEntityId = Ref.field(outAnimation, "a");
		}

		Field bedId = Ref.field(outBed, int.class);
		Field[] xyz = new Field[3];
		if (Ref.isOlderThan(12)) {
			boolean firstInt = true;
			for (Field field : Ref.getDeclaredFields(outTeleport))
				if (field.getType() == int.class) {
					if (firstInt) {
						firstInt = false;
						continue;
					}
					if (xyz[0] == null)
						xyz[0] = field;
					else if (xyz[1] == null)
						xyz[1] = field;
					else if (xyz[2] == null) {
						xyz[2] = field;
						break;
					}
				}
		}
		for (Field field : Ref.getDeclaredFields(outTeleport))
			if (field.getType() == double.class)
				if (xyz[0] == null)
					xyz[0] = field;
				else if (xyz[1] == null)
					xyz[1] = field;
				else if (xyz[2] == null) {
					xyz[2] = field;
					break;
				}
		Field[] mxyz = new Field[3];
		if (inVehicleMove != null)
			for (Field field : Ref.getDeclaredFields(inVehicleMove))
				if (field.getType() == double.class)
					if (mxyz[0] == null)
						mxyz[0] = field;
					else if (mxyz[1] == null)
						mxyz[1] = field;
					else if (mxyz[2] == null) {
						mxyz[2] = field;
						break;
					}
		Class<?> actionPacketClass = Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ServerboundPlayerCommandPacket" : "PacketPlayInEntityAction");
		Class<?> movementPacketClass = Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ServerboundMovePlayerPacket" : "PacketPlayInFlying");
		Field changedPosition;
		Field actionField;
		if (BukkitLoader.NO_OBFUSCATED_NMS_MODE || Ref.isOlderThan(17)) {
			Ref.field(movementPacketClass, "x");
			Ref.field(movementPacketClass, "y");
			Ref.field(movementPacketClass, "z");
			changedPosition = Ref.field(movementPacketClass, "hasPos");
			actionField = Ref.field(actionPacketClass, Ref.isOlderThan(17) ? "animation" : "action");
		} else {
			Ref.field(movementPacketClass, "a");
			Ref.field(movementPacketClass, "b");
			Ref.field(movementPacketClass, "c");
			changedPosition = Ref.field(movementPacketClass, "g");
			actionField = Ref.field(actionPacketClass, "b");
		}
		listener = new PacketListener() {
			Field xField = xyz[0];
			Field yField = xyz[1];
			Field zField = xyz[2];
			Field movexField = mxyz[0];
			Field moveyField = mxyz[1];
			Field movezField = mxyz[2];
			Object entityTypePlayer = findEntityType();

			@Override
			public void playOut(String name, PacketContainer packetContainer, ChannelContainer channel) {
				Object packet = packetContainer.getPacket();
				if (packet.getClass().isAssignableFrom(outVelocity)) {
					NametagPlayer player = lookupById((int) Ref.get(packet, velocityEntityId));
					if (player != null) {
						NametagPlayer receiver = lookupByName(name);
						if (receiver == null)
							return;
						player.getNametag().shouldTeleport(player.getPlayer().getLocation());
					}
					return;
				}
				// 1.19.4+
				if ((Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() == 3) && packet.getClass().isAssignableFrom(clientBundle)) {
					for (Object inBundle : (Iterable<?>) Ref.get(packet, iterableField))
						processPacket(name, inBundle);
					return;
				}
				processPacket(name, packet);
			}

			private Object findEntityType() {
				Class<?> nmsHuman = Ref.nms("world.entity.player", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "Player" : "EntityHuman");
				for (Field field : Ref.getAllFields(entityTypes))
					try {
						if (field.getType().equals(entityTypes) && field.getGenericType() instanceof ParameterizedType
								&& ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].equals(nmsHuman))
							return Ref.get(null, field);
					} catch (Exception e) {

					}
				return null;
			}

			@Override
			public void playIn(String name, PacketContainer packetContainer, ChannelContainer channel) {
				Object packet = packetContainer.getPacket();
				if (movementPacketClass.isAssignableFrom(packet.getClass())) {
					if ((boolean) Ref.get(packet, changedPosition)) {
						NametagPlayer player = lookupByName(name);
						if (player != null) {
							NametagHologram hologram = player.getNametag();
							hologram.shouldTeleport(player.getPlayer().getLocation());
						}
					}
					return;
				}
				if (packet.getClass().equals(actionPacketClass)) {
					NametagPlayer player = lookupByName(name);
					if (player != null)
						switch (Ref.get(packet, actionField).toString()) {
						case "PRESS_SHIFT_KEY":
						case "1":
							player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
							player.getNametag().updateHeight(true, false, false, false);
							break;
						case "2":
						case "RELEASE_SHIFT_KEY":
							player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
							player.getNametag().updateHeight(false, false, false, false);
							break;
						case "START_FALL_FLYING":
							player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
							player.getNametag().updateHeight(false, false, false, false);
							break;
						}
					return;
				}
				if (inVehicleMove != null && packet.getClass().isAssignableFrom(inVehicleMove)) {
					NametagPlayer spawned = lookupByName(name);
					if (spawned == null) {
						UUID uuid = API.offlineCache().lookupId(name);
						Player player;
						while ((player = Bukkit.getPlayer(uuid)) == null)
							; // Await world change
						spawned = lookupByUuidOrCreate(player, uuid);
					}
					spawned.getNametag().shouldTeleport((double) Ref.get(packet, movexField), (double) Ref.get(packet, moveyField), (double) Ref.get(packet, movezField));
				}
			}

			@SuppressWarnings("unchecked")
			private void processPacket(String name, Object packet) {
				if (Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2) {
					if (packet.getClass().isAssignableFrom(clientPlayerInfoUpdate)) {
						UUID offlineUuid = API.offlineCache().lookupId(name);
						Player offlinePlayer;
						while ((offlinePlayer = Bukkit.getPlayer(offlineUuid)) == null)
							; // Await world change

						NametagPlayer receiverPlayer = lookupByUuidOrCreate(offlinePlayer, offlineUuid);
						for (Object entity : (List<?>) Ref.get(packet, listBField)) {
							UUID uuid = (UUID) Ref.get(entity, playerInfoUuidField);
							if (uuid.equals(offlineUuid))
								continue;

							new Tasker() {

								@Override
								public void run() {
									Player joinedPlayer = null;
									long start = System.currentTimeMillis();
									while (joinedPlayer == null && (joinedPlayer = Bukkit.getPlayer(uuid)) == null || joinedPlayer.getWorld() == null)
										if (System.currentTimeMillis() - start >= 20)
											return;
									// Await connection
									NametagPlayer spawned;
									spawned = lookupByUuidOrCreate(joinedPlayer, uuid);
									spawned.addTabSorting(receiverPlayer);
								}
							}.runTask();
						}
						return;
					}
					if (packet.getClass().isAssignableFrom(clientPlayerInfoRemove)) {
						UUID offlineUuid = API.offlineCache().lookupId(name);
						Player offlinePlayer;
						if ((offlinePlayer = Bukkit.getPlayer(offlineUuid)) == null)
							return;
						NametagPlayer receiverPlayer = lookupByUuidOrCreate(offlinePlayer, offlineUuid);
						for (UUID entity : (List<UUID>) Ref.get(packet, listUuidsField)) {
							if (entity.equals(offlineUuid))
								continue;
							NametagPlayer spawned = lookupByUuid(entity);
							if (spawned != null)
								spawned.removeTabSorting(receiverPlayer);
						}
						return;
					}
				} else if (packet.getClass().isAssignableFrom(clientPlayerInfoUpdate)) { // These two packets are combined into one in the earlier versions
					UUID offlineUuid = API.offlineCache().lookupId(name);
					Player offlinePlayer;
					if (Ref.get(packet, clientPlayerInfoLegacyAction).toString().equals("REMOVE_PLAYER")) { // Remove
						if ((offlinePlayer = Bukkit.getPlayer(offlineUuid)) == null)
							return;
						NametagPlayer receiverPlayer = lookupByUuidOrCreate(offlinePlayer, offlineUuid);
						for (Object entity : (List<?>) Ref.get(packet, listUuidsFieldUpdate)) {
							UUID uuid = BukkitLoader.getNmsProvider().fromGameProfile(Ref.get(entity, playerInfoUuidField)).getUUID();
							if (uuid.equals(offlineUuid))
								continue;
							NametagPlayer spawned = lookupByUuid(uuid);
							if (spawned != null)
								spawned.removeTabSorting(receiverPlayer);
						}
					} else { // Add or update
						while ((offlinePlayer = Bukkit.getPlayer(offlineUuid)) == null)
							; // Await world change

						NametagPlayer receiverPlayer = lookupByUuidOrCreate(offlinePlayer, offlineUuid);
						for (Object entity : (List<?>) Ref.get(packet, listBField)) {
							UUID uuid = BukkitLoader.getNmsProvider().fromGameProfile(Ref.get(entity, playerInfoUuidField)).getUUID();
							if (uuid.equals(offlineUuid))
								continue;
							new Tasker() {

								@Override
								public void run() {
									Player joinedPlayer = null;
									long start = System.currentTimeMillis();
									while (joinedPlayer == null && (joinedPlayer = Bukkit.getPlayer(uuid)) == null || joinedPlayer.getWorld() == null)
										if (System.currentTimeMillis() - start >= 20)
											return;
									NametagPlayer spawned;
									spawned = lookupByUuidOrCreate(joinedPlayer, uuid);
									spawned.addTabSorting(receiverPlayer);
								}
							}.runTask();
						}
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outSpawnEntity)) {
					if (entityTypeField == null ? true : Ref.get(packet, entityTypeField).equals(entityTypePlayer)) {
						UUID uuid = (UUID) Ref.get(packet, uuidField);
						UUID offlineUuid = API.offlineCache().lookupId(name);
						if (uuid.equals(offlineUuid))
							return;
						Player offlinePlayer;
						while ((offlinePlayer = Bukkit.getPlayer(offlineUuid)) == null)
							; // Await world change
						Player offlinePlayerFinal = offlinePlayer;
						NametagPlayer receiverPlayer = lookupByUuidOrCreate(offlinePlayerFinal, offlineUuid);

						new Tasker() {

							@Override
							public void run() {
								Player joinedPlayer = null;
								long start = System.currentTimeMillis();
								while (joinedPlayer == null && (joinedPlayer = Bukkit.getPlayer(uuid)) == null || joinedPlayer.getWorld() == null)
									if (System.currentTimeMillis() - start >= 20)
										return;

								NametagPlayer spawned = lookupByUuidOrCreate(joinedPlayer, uuid);
								spawned.showNametag(receiverPlayer);
							}
						}.runTask();
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outMetadata)) {
					NametagPlayer player = lookupById((int) Ref.get(packet, metaEntityId));
					if (player != null)
						for (Object item : (List<?>) Ref.get(packet, metaData)) {
							Object pose = Ref.get(item, valueField);
							if (pose == null)
								continue;
							if (Ref.isOlderThan(13)) {
								if (pose instanceof Byte)
									switch ((byte) pose) {
									case (byte) 0x02:
										player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
										player.getNametag().updateHeight(true, false, false, false);
										break;
									case (byte) 0x80:
										player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
										player.getNametag().updateHeight(false, true, false, false);
										break;
									default:
										player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
										player.getNametag().updateHeight(false, false, false, false);
										break;
									}
								return;
							}
							if (pose.getClass().isAssignableFrom(entityPose))
								switch (pose.toString()) {
								case "CROUCHING":
								case "SNEAKING":
								case "FALL_FLYING":
									break;
								case "SLEEPING":
								case "SWIMMING":
									player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
									player.getNametag().updateHeight(false, true, false, false);
									break;
								default:
									player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
									player.getNametag().updateHeight(false, false, false, false);
									break;
								}
						}
					return;
				}
				if (outBed != null && packet.getClass().isAssignableFrom(outBed)) {
					NametagPlayer player = lookupById((int) Ref.get(packet, bedId));
					if (player != null) {
						player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
						player.getNametag().updateHeight(false, true, false, false);
					}
					return;
				}
				if (outBed != null && packet.getClass().isAssignableFrom(outAnimation)) {
					if ((int) Ref.get(packet, animationId) == 2) {
						NametagPlayer player = lookupById((int) Ref.get(packet, animationEntityId));
						if (player != null) {
							player.getNametag().setPosWithoutUpdate(player.getPlayer().getLocation());
							player.getNametag().updateHeight(false, false, false, false);
						}
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outDestroy)) { // Used only when player switch world/gamemode or dies
					NametagPlayer receiver = lookupByName(name);
					if (receiver == null)
						return;
					Object obj = Ref.get(packet, integersField);
					if (obj instanceof List)
						for (int i : (List<Integer>) obj) { // IntList
							NametagPlayer despawning = lookupById(i);
							if (despawning != null)
								despawning.hideNametag(receiver);
						}
					else
						for (int i : (int[]) obj) { // int[]
							NametagPlayer despawning = lookupById(i);
							if (despawning != null)
								despawning.hideNametag(receiver);
						}
					return;
				}
				if (packet.getClass().isAssignableFrom(outMount)) { // Ride
					int id = (int) Ref.get(packet, idField);
					if (Ref.isOlderThan(12)) {
						boolean lead = (int) Ref.get(packet, isLeashed) == 1;
						if (lead)
							return;

						int attachedMob = (int) Ref.get(packet, mobsField);
						if (attachedMob == -1) { // Dismount
							List<NametagPlayer> ridingBefore = watchingEntityMove.get(id);
							synchronized (ridingBefore) {
								for (NametagPlayer player : ridingBefore)
									// Dismount
									player.getNametag().updateHeight(false, false, false, false);
								watchingEntityMove.remove(id);
								return;
							}
						} else {
							List<NametagPlayer> players = Collections.synchronizedList(new ArrayList<>());
							NametagPlayer riding = lookupById(attachedMob);
							if (riding != null && riding.getPlayer().getVehicle() != null) {
								players.add(riding);
								// Mount
								riding.getNametag().setPosWithoutUpdate(riding.getPlayer().getVehicle().getLocation());
								riding.getNametag().updateHeight(false, false, true, false);
							}
							if (players.isEmpty())
								watchingEntityMove.remove(id);
							else
								watchingEntityMove.put(id, players);
						}
					} else {
						int[] mobs = (int[]) Ref.get(packet, mobsField);
						List<NametagPlayer> ridingBefore = watchingEntityMove.get(id);
						if (ridingBefore == null) { // mount
							List<NametagPlayer> players = mobs.length == 0 ? Collections.emptyList() : Collections.synchronizedList(new ArrayList<>());
							for (int i : mobs) {
								NametagPlayer riding = lookupById(i);
								if (riding != null) {
									players.add(riding);
									// Mount
									riding.getNametag().setPosWithoutUpdate(riding.getPlayer().getVehicle().getLocation());
									riding.getNametag().updateHeight(false, false, true, false);
								}
							}
							watchingEntityMove.put(id, players);
						} else {// mount & dismount
							List<NametagPlayer> players = mobs.length == 0 ? Collections.emptyList() : new ArrayList<>();
							for (int i : mobs) {
								NametagPlayer riding = lookupById(i);
								if (riding != null) {
									players.add(riding);
									// Mount
									riding.getNametag().setPosWithoutUpdate(riding.getPlayer().getVehicle().getLocation());
									riding.getNametag().updateHeight(false, false, true, false);
								}
							}
							if (players.isEmpty())
								watchingEntityMove.remove(id);
							else {
								watchingEntityMove.put(id, players);
								ridingBefore.removeAll(players);
								for (NametagPlayer dismounted : ridingBefore) {
									dismounted.getNametag().setPosWithoutUpdate(dismounted.getPlayer().getLocation());
									dismounted.getNametag().updateHeight(false, false, false, false);
								}
							}
						}
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outTeleport)) {
					NametagPlayer player = lookupById((int) Ref.get(packet, entityIdField));
					if (player != null)
						if (Ref.isOlderThan(12))
							player.getNametag().shouldTeleport((int) Ref.get(packet, xField) / 32.0, (int) Ref.get(packet, yField) / 32.0, (int) Ref.get(packet, zField) / 32.0);
						else
							player.getNametag().shouldTeleport((double) Ref.get(packet, xField), (double) Ref.get(packet, yField), (double) Ref.get(packet, zField));
				}
			}
		};
		listener.register();
		for (Player online : BukkitLoader.getOnlinePlayers())
			lookupByUuidOrCreate(online, online.getUniqueId()).afterJoin();

		// Init online players
		for (NametagPlayer player : players)
			for (NametagPlayer other : players) {
				if (player.equals(other))
					continue;
				// Tablist sorting
				if (player.getPlayer().canSee(other.getPlayer()))
					player.addTabSorting(other);
				if (other.getPlayer().canSee(player.getPlayer()))
					other.addTabSorting(player);
				// Nametag
				if (player.getPlayer().getWorld().equals(other.getPlayer().getWorld())) {
					if (other.getPlayer().canSee(player.getPlayer()) && canSeeSpectatorLogin(other, player))
						player.getNametag().show(other);
					if (player.getPlayer().canSee(other.getPlayer()) && canSeeSpectatorLogin(player, other))
						other.getNametag().show(player);
				}
			}
	}

	private boolean canSeeSpectatorLogin(NametagPlayer player, NametagPlayer other) {
		return other.getPlayer().getGameMode() == GameMode.SPECTATOR ? player.getPlayer().getGameMode() == GameMode.SPECTATOR : true;
	}

	public void unload() {
		isLoaded = false;
		teamManager.disable();
		listener.unregister();
		for (NametagPlayer hologram : players)
			hologram.onQuit();
		players.clear();
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public int getPlayerCountInTeam(String teamName) {
		int amount = 0;
		synchronized (players) {
			for (NametagPlayer player : players)
				if (player.getTeamName().equals(teamName))
					++amount;
		}
		return amount;
	}

	public List<String> getPlayersInTeam(String teamName) {
		List<String> result = new ArrayList<>();
		synchronized (players) {
			for (NametagPlayer player : players)
				if (player.getTeamName().equals(teamName))
					result.add(player.getName());
		}
		return result;
	}

}
