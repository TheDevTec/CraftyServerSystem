package me.devtec.craftyserversystem.utils.tablist.nametag;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer;
import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.ArmorStandHologram;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.DefaultTeamManager;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.LuckPermsTeamManager;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.TeamManager;
import me.devtec.craftyserversystem.utils.tablist.nametag.support.VaultTeamManager;
import me.devtec.shared.API;
import me.devtec.shared.Ref;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.theapi.bukkit.BukkitLoader;
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

	public volatile Map<Integer, List<ClassicTabPlayer>> watchingEntityMove = new ConcurrentHashMap<>();
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

	@Nonnull
	public TeamManager getTeamManager() {
		return teamManager;
	}

	public void setTeamManager(@Nonnull TeamManager manager) {
		teamManager = manager == null ? new DefaultTeamManager() : manager;
	}

	public void load() {
		isLoaded = true;
		switch (me.devtec.craftyserversystem.api.API.get().getConfigManager().getTab().getString("sorting.by")
				.toUpperCase()) {
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
		Class<?> entityTypes, outVelocity, outMount, outMetadata, outDestroy, clientBundle, outTeleport, inVehicleMove,
		outSpawnEntity, clientPlayerInfoUpdate, clientPlayerInfoRemove, entityPose, outBed, outAnimation;
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
			playerInfoUuidField = Ref.field(Ref.nms("network.protocol.game", "ClientboundPlayerInfoUpdatePacket$Entry"),
					UUID.class);
			valueField = Ref.field(Ref.nms("network.syncher", "SynchedEntityData$DataValue"), "value");
			animationEntityId = null;
			animationId = null;
		} else {
			entityTypes = Ref.nms("world.entity", "EntityTypes");
			outVelocity = Ref.nms("network.protocol.game", "PacketPlayOutEntityVelocity");
			outMount = Ref.nms("network.protocol.game", "PacketPlayOutMount") == null
					? Ref.nms("network.protocol.game", "PacketPlayOutAttachEntity")
							: Ref.nms("network.protocol.game", "PacketPlayOutMount");
			outMetadata = Ref.nms("network.protocol.game", "PacketPlayOutEntityMetadata");
			outDestroy = Ref.nms("network.protocol.game", "PacketPlayOutEntityDestroy");
			clientBundle = Ref.nms("network.protocol.game", "ClientboundBundlePacket");
			outTeleport = Ref.nms("network.protocol.game", "PacketPlayOutEntityTeleport");
			inVehicleMove = Ref.nms("network.protocol.game", "PacketPlayInVehicleMove");
			outSpawnEntity = Ref.nms("network.protocol.game", "PacketPlayOutNamedEntitySpawn") == null
					? Ref.nms("network.protocol.game", "PacketPlayOutSpawnEntity")
							: Ref.nms("network.protocol.game", "PacketPlayOutNamedEntitySpawn");
			clientPlayerInfoUpdate = Ref.isNewerThan(19)
					|| Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2
					? Ref.nms("network.protocol.game", "ClientboundPlayerInfoUpdatePacket")
							: Ref.nms("network.protocol.game", "PacketPlayOutPlayerInfo");
					clientPlayerInfoRemove = Ref.nms("network.protocol.game", "ClientboundPlayerInfoRemovePacket");
					entityPose = Ref.nms("world.entity", "EntityPose");
					outBed = Ref.nms("", "PacketPlayOutBed");
					outAnimation = Ref.nms("network.protocol.game", "ClientboundAnimatePacket") == null
							? Ref.nms("network.protocol.game", "PacketPlayOutAnimation")
									: Ref.nms("network.protocol.game", "ClientboundAnimatePacket");
					idField = Ref.isOlderThan(12) ? Ref.field(outMount, "c") : Ref.field(outMount, int.class);
					mobsField = Ref.field(outMount, int[].class) == null ? Ref.field(outMount, "b")
							: Ref.field(outMount, int[].class);
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
									: Ref.field(Ref.nms("network.protocol.game", "PacketPlayOutPlayerInfo$PlayerInfoData"),
											Ref.getClass("com.mojang.authlib.GameProfile"));
					valueField = Ref.isNewerThan(19)
							|| Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2
							? Ref.field(Ref.nms("network.syncher", "DataWatcher$b"), "c")
									: Ref.field(
											Ref.isOlderThan(12) ? Ref.nms("network.syncher", "DataWatcher$WatchableObject")
													: Ref.nms("network.syncher", "DataWatcher$Item"),
													Ref.isOlderThan(12) ? "c" : "b");
							animationId = Ref.field(outAnimation, "b");
							animationEntityId = Ref.field(outAnimation, "a");
		}
		Class<?> vec3D = Ref.nms("world.phys", "Vec3") == null ? Ref.nms("world.phys", "Vec3D")
				: Ref.nms("world.phys", "Vec3");
		Field bedId = Ref.field(outBed, int.class);
		Field[] xyz = new Field[3];
		Field moveRot = Ref.field(outTeleport, Ref.nms("world.entity", "PositionMoveRotation"));
		Field positionVec = Ref.field(Ref.nms("world.entity", "PositionMoveRotation"), vec3D);
		Field positionVecVehicle = Ref.field(inVehicleMove, vec3D);
		if (positionVec != null) {
			if (BukkitLoader.NO_OBFUSCATED_NMS_MODE) {
				xyz[0] = Ref.field(vec3D, "x");
				xyz[1] = Ref.field(vec3D, "y");
				xyz[2] = Ref.field(vec3D, "z");
			} else
				for (Field field : Ref.getDeclaredFields(vec3D))
					if (field.getType() == double.class)
						if (xyz[0] == null)
							xyz[0] = field;
						else if (xyz[1] == null)
							xyz[1] = field;
						else if (xyz[2] == null) {
							xyz[2] = field;
							break;
						}
		} else if (Ref.isOlderThan(12)) {
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
		} else
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
		if (inVehicleMove != null) {
			if (positionVecVehicle == null)
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
			if (positionVecVehicle != null)
				if (BukkitLoader.NO_OBFUSCATED_NMS_MODE) {
					mxyz[0] = Ref.field(vec3D, "x");
					mxyz[1] = Ref.field(vec3D, "y");
					mxyz[2] = Ref.field(vec3D, "z");
				} else
					for (Field field : Ref.getDeclaredFields(vec3D))
						if (field.getType() == double.class)
							if (mxyz[0] == null)
								mxyz[0] = field;
							else if (mxyz[1] == null)
								mxyz[1] = field;
							else if (mxyz[2] == null) {
								mxyz[2] = field;
								break;
							}
		}
		Class<?> actionPacketClass = Ref.nms("network.protocol.game",
				BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ServerboundPlayerCommandPacket" : "PacketPlayInEntityAction");
		Class<?> movementPacketClass = Ref.nms("network.protocol.game",
				BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ServerboundMovePlayerPacket" : "PacketPlayInFlying");
		Class<?> inputPacket = Ref.nms("network.protocol.game", "ServerboundPlayerInputPacket");
		Field changedPosition;
		Field input = Ref.field(inputPacket, "input");
		Field shift = Ref.field(Ref.nms("world.entity.player", "Input"), "shift");
		Field actionField;
		if (BukkitLoader.NO_OBFUSCATED_NMS_MODE || Ref.isOlderThan(17)) {
			changedPosition = Ref.field(movementPacketClass, "hasPos");
			actionField = Ref.field(actionPacketClass, Ref.isOlderThan(17) ? "animation" : "action");
		} else {
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
					ClassicTabPlayer player = TabAPI.getHolder((int) Ref.get(packet, velocityEntityId));
					if (player != null) {
						ClassicTabPlayer receiver = TabAPI.await(name);
						if (receiver == null)
							return;
						Location location = player.getPlayer().getLocation();
						for(ArmorStandHologram line : player.getAdditionalLines())
							line.shouldTeleport(location);
					}
					return;
				}
				// 1.19.4+
				if ((Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() == 3)
						&& packet.getClass().isAssignableFrom(clientBundle)) {
					for (Object inBundle : (Iterable<?>) Ref.get(packet, iterableField))
						processPacket(name, inBundle);
					return;
				}
				processPacket(name, packet);
			}

			private Object findEntityType() {
				Class<?> nmsHuman = Ref.nms("world.entity.player",
						BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "Player" : "EntityHuman");
				for (Field field : Ref.getAllFields(entityTypes))
					try {
						if (field.getType().equals(entityTypes) && field.getGenericType() instanceof ParameterizedType
								&& ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
										.equals(nmsHuman))
							return Ref.get(null, field);
					} catch (Exception ignored) {

					}
				return null;
			}

			@Override
			public void playIn(String name, PacketContainer packetContainer, ChannelContainer channel) {
				Object packet = packetContainer.getPacket();
				if (movementPacketClass.isAssignableFrom(packet.getClass())) {
					if ((boolean) Ref.get(packet, changedPosition)) {
						ClassicTabPlayer player = TabAPI.await(name);
						if (player != null) {
							Location location = player.getPlayer().getLocation();
							for(ArmorStandHologram line : player.getAdditionalLines())
								line.shouldTeleport(location);
						}
					}
					return;
				}
				if (inputPacket!=null  && inputPacket.isAssignableFrom(packet.getClass())) {
					ClassicTabPlayer player = TabAPI.await(name);
					if (player != null) {
						Location loc = player.getPlayer().getLocation();
						for(ArmorStandHologram line : player.getAdditionalLines()) {
							line.setPosWithoutUpdate(loc);
							line.updateHeight((boolean)Ref.get(Ref.get(packet, input), shift), false, false, false);
						}
					}

					return;
				}
				if (packet.getClass().equals(actionPacketClass)) {
					ClassicTabPlayer player = TabAPI.await(name);
					if (player != null)
						switch (Ref.get(packet, actionField).toString()) {
						case "PRESS_SHIFT_KEY":
						case "1":
							Location loc = player.getPlayer().getLocation();
							for(ArmorStandHologram line : player.getAdditionalLines()) {
								line.setPosWithoutUpdate(loc);
								line.updateHeight(true, false, false, false);
							}
							break;
						case "2":
						case "RELEASE_SHIFT_KEY":
							loc = player.getPlayer().getLocation();
							for(ArmorStandHologram line : player.getAdditionalLines()) {
								line.setPosWithoutUpdate(loc);
								line.updateHeight(false, false, false, false);
							}
							break;
						case "START_FALL_FLYING":
							loc = player.getPlayer().getLocation();
							for(ArmorStandHologram line : player.getAdditionalLines()) {
								line.setPosWithoutUpdate(loc);
								line.updateHeight(false, false, false, false);
							}
							break;
						}
					return;
				}
				if (inVehicleMove != null && packet.getClass().isAssignableFrom(inVehicleMove)) {
					ClassicTabPlayer spawned = TabAPI.await(name);
					if (positionVecVehicle != null) {
						Object pos = Ref.get(packet, positionVecVehicle);
						double x=(double) Ref.get(pos, movexField);
						double y=(double) Ref.get(pos, moveyField);
						double z=(double) Ref.get(pos, movezField);
						for(ArmorStandHologram line : spawned.getAdditionalLines())
							line.shouldTeleport(x,y,z);
					} else {
						double x=(double) Ref.get(packet, movexField);
						double y=(double) Ref.get(packet, moveyField);
						double z=(double) Ref.get(packet, movezField);
						for(ArmorStandHologram line : spawned.getAdditionalLines())
							line.shouldTeleport(x,y,z);
					}
				}
			}

			@SuppressWarnings("unchecked")
			private void processPacket(String name, Object packet) {
				if (Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2) {
					if (packet.getClass().isAssignableFrom(clientPlayerInfoUpdate)) {
						boolean shouldContinue = false;
						for(Object obj : (EnumSet<?>)Ref.get(packet, "actions"))
							if("ADD_PLAYER".equals(obj.toString())) {
								shouldContinue=true;
								break;
							}
						if(!shouldContinue)return;
						ClassicTabPlayer receiverPlayer = TabAPI.await(name);
						for (Object entity : (List<?>) Ref.get(packet, listBField)) {
							UUID uuid = (UUID) Ref.get(entity, playerInfoUuidField);
							if (uuid.equals(receiverPlayer.getPlayer().getUniqueId()))
								continue;
							ClassicTabPlayer spawned = TabAPI.await(uuid);
							Player joinedPlayer = spawned.getPlayer();
							long start = System.currentTimeMillis();
							while (!joinedPlayer.isOnline()
									|| joinedPlayer.getWorld() == null)
								if (System.currentTimeMillis() - start >= 5)
									return;
							if(spawned.getPrimaryTeam() == null || receiverPlayer.getTeams().contains(spawned.getPrimaryTeam()))return;
							receiverPlayer.getPlayer().sendMessage("Displaying team "+spawned.getPrimaryTeam()+" of player "+spawned.getPlayer().getName());
							receiverPlayer.createTeam(spawned.getPrimaryTeam());
						}
						return;
					}
					if (packet.getClass().isAssignableFrom(clientPlayerInfoRemove)) {
						UUID offlineUuid = API.offlineCache().lookupId(name);
						ClassicTabPlayer receiverPlayer = TabAPI.await(offlineUuid);
						if(receiverPlayer==null)return;
						for (UUID entity : (List<UUID>) Ref.get(packet, listUuidsField)) {
							if (entity.equals(offlineUuid))
								continue;
							ClassicTabPlayer removed = TabAPI.data.get(entity);
							if (removed != null && removed.getPrimaryTeam()!=null) {
								receiverPlayer.getPlayer().sendMessage("Removing team "+removed.getPrimaryTeam()+" of player "+removed.getPlayer().getName());
								receiverPlayer.removeTeam(removed.getPrimaryTeam().getTeam());
							}
						}
						return;
					}
				} else if (packet.getClass().isAssignableFrom(clientPlayerInfoUpdate)) { // These two packets are
					// combined into one in the
					// earlier versions
					UUID offlineUuid = API.offlineCache().lookupId(name);
					if ("REMOVE_PLAYER".equals(Ref.get(packet, clientPlayerInfoLegacyAction).toString())) { // Remove
						ClassicTabPlayer receiverPlayer = TabAPI.getHolder(offlineUuid);
						if (receiverPlayer==null)
							return;
						for (Object entity : (List<?>) Ref.get(packet, listUuidsFieldUpdate)) {
							UUID uuid = BukkitLoader.getNmsProvider()
									.fromGameProfile(Ref.get(entity, playerInfoUuidField)).getUUID();
							if (uuid.equals(offlineUuid))
								continue;
							ClassicTabPlayer removed = TabAPI.data.get(uuid);
							if (removed != null && removed.getPrimaryTeam()!=null)
								receiverPlayer.removeTeam(removed.getPrimaryTeam().getTeam());
						}
					} else { // Add or update
						ClassicTabPlayer receiverPlayer = TabAPI.await(offlineUuid);
						for (Object entity : (List<?>) Ref.get(packet, listBField)) {
							UUID uuid = BukkitLoader.getNmsProvider().fromGameProfile(Ref.get(entity, playerInfoUuidField)).getUUID();
							if (uuid.equals(offlineUuid))
								continue;
							ClassicTabPlayer spawned = TabAPI.await(uuid);
							Player joinedPlayer = spawned.getPlayer();
							long start = System.currentTimeMillis();
							while (!joinedPlayer.isOnline()
									|| joinedPlayer.getWorld() == null)
								if (System.currentTimeMillis() - start >= 5)
									return;
							receiverPlayer.createTeam(spawned.getPrimaryTeam());
						}
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outSpawnEntity)) {
					if (entityTypeField == null || Ref.get(packet, entityTypeField).equals(entityTypePlayer)) {
						UUID uuid = (UUID) Ref.get(packet, uuidField);
						UUID offlineUuid = API.offlineCache().lookupId(name);
						if (uuid.equals(offlineUuid))
							return;
						ClassicTabPlayer receiverPlayer = TabAPI.await(offlineUuid);
						ClassicTabPlayer spawned = TabAPI.getHolder(uuid);
						Player joinedPlayer = spawned.getPlayer();
						long start = System.currentTimeMillis();
						while (joinedPlayer.getWorld() == null || !joinedPlayer.isOnline())
							if (System.currentTimeMillis() - start >= 5)
								return;
						spawned.showLines(receiverPlayer);
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outMetadata)) {
					ClassicTabPlayer player = TabAPI.getHolder((int) Ref.get(packet, metaEntityId));
					if (player != null)
						for (Object item : (List<?>) Ref.get(packet, metaData)) {
							Object pose = Ref.get(item, valueField);
							if (pose == null)
								continue;
							if (Ref.isOlderThan(13)) {
								if (pose instanceof Byte)
									switch ((byte) pose) {
									case (byte) 0x02:
										Location loc = player.getPlayer().getLocation();
									for(ArmorStandHologram line : player.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(true, false, false, false);
									}
									break;
									case (byte) 0x80:
										loc = player.getPlayer().getLocation();
									for(ArmorStandHologram line : player.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(false, true, false, false);
									}
									break;
									default:
										loc = player.getPlayer().getLocation();
										for(ArmorStandHologram line : player.getAdditionalLines()) {
											line.setPosWithoutUpdate(loc);
											line.updateHeight(false, false, false, false);
										}
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
									Location loc = player.getPlayer().getLocation();
									for(ArmorStandHologram line : player.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(false, true, false, false);
									}
									break;
								default:
									loc = player.getPlayer().getLocation();
									for(ArmorStandHologram line : player.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(false, false, false, false);
									}
									break;
								}
						}
					return;
				}
				if (outBed != null && packet.getClass().isAssignableFrom(outBed)) {
					ClassicTabPlayer player = TabAPI.getHolder((int) Ref.get(packet, bedId));
					if (player != null) {
						Location loc = player.getPlayer().getLocation();
						for(ArmorStandHologram line : player.getAdditionalLines()) {
							line.setPosWithoutUpdate(loc);
							line.updateHeight(false, true, false, false);
						}
					}
					return;
				}
				if (outBed != null && packet.getClass().isAssignableFrom(outAnimation)) {
					if ((int) Ref.get(packet, animationId) == 2) {
						ClassicTabPlayer player = TabAPI.getHolder((int) Ref.get(packet, animationEntityId));
						if (player != null) {
							Location loc = player.getPlayer().getLocation();
							for(ArmorStandHologram line : player.getAdditionalLines()) {
								line.setPosWithoutUpdate(loc);
								line.updateHeight(false, false, false, false);
							}
						}
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outDestroy)) { // Used only when player switch world/gamemode or dies
					ClassicTabPlayer receiver = TabAPI.await(name);
					Object obj = Ref.get(packet, integersField);
					if (obj instanceof List)
						for (int i : (List<Integer>) obj) { // IntList
							ClassicTabPlayer despawning = TabAPI.getHolder(i);
							if (despawning != null)
								despawning.hideLines(receiver);
						}
					else
						for (int i : (int[]) obj) { // int[]
							ClassicTabPlayer despawning = TabAPI.getHolder(i);
							if (despawning != null)
								despawning.hideLines(receiver);
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
							List<ClassicTabPlayer> ridingBefore = watchingEntityMove.get(id);
							synchronized (ridingBefore) {
								for (ClassicTabPlayer player : ridingBefore)
									// Dismount
									for(ArmorStandHologram line : player.getAdditionalLines())
										line.updateHeight(false, false, false, false);
								watchingEntityMove.remove(id);
								return;
							}
						} else {
							List<ClassicTabPlayer> players = Collections.synchronizedList(new ArrayList<>());
							ClassicTabPlayer riding = TabAPI.getHolder(attachedMob);
							if (riding != null && riding.getPlayer().getVehicle() != null) {
								players.add(riding);
								// Mount
								Location loc = riding.getPlayer().getVehicle().getLocation();
								for(ArmorStandHologram line : riding.getAdditionalLines()) {
									line.setPosWithoutUpdate(loc);
									line.updateHeight(false, false, true, false);
								}
							}
							if (players.isEmpty())
								watchingEntityMove.remove(id);
							else
								watchingEntityMove.put(id, players);
						}
					} else {
						Object obj = Ref.get(packet, mobsField);
						int[] mobs;
						if(obj instanceof int[])
							mobs=(int[])obj;
						else
							mobs=new int[] {(int)obj};
						List<ClassicTabPlayer> ridingBefore = watchingEntityMove.get(id);
						if (ridingBefore == null) { // mount
							List<ClassicTabPlayer> players = mobs.length == 0 ? Collections.emptyList()
									: Collections.synchronizedList(new ArrayList<>());
							for (int i : mobs) {
								ClassicTabPlayer riding = TabAPI.getHolder(i);
								if (riding != null) {
									players.add(riding);
									// Mount
									Location loc = riding.getPlayer().getVehicle().getLocation();
									for(ArmorStandHologram line : riding.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(false, false, true, false);
									}
								}
							}
							watchingEntityMove.put(id, players);
						} else {// mount & dismount
							List<ClassicTabPlayer> players = mobs.length == 0 ? Collections.emptyList()
									: new ArrayList<>();
							for (int i : mobs) {
								ClassicTabPlayer riding = TabAPI.getHolder(i);
								if (riding != null) {
									players.add(riding);
									// Mount
									Location loc = riding.getPlayer().getVehicle().getLocation();
									for(ArmorStandHologram line : riding.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(false, false, true, false);
									}
								}
							}
							if (players.isEmpty())
								watchingEntityMove.remove(id);
							else {
								watchingEntityMove.put(id, players);
								ridingBefore.removeAll(players);
								for (ClassicTabPlayer dismounted : ridingBefore) {
									Location loc = dismounted.getPlayer().getLocation();
									for(ArmorStandHologram line : dismounted.getAdditionalLines()) {
										line.setPosWithoutUpdate(loc);
										line.updateHeight(false, false, false, false);
									}
								}
							}
						}
					}
					return;
				}
				if (packet.getClass().isAssignableFrom(outTeleport)) {
					ClassicTabPlayer player = TabAPI.getHolder((int) Ref.get(packet, entityIdField));
					if (player != null)
						if (Ref.isOlderThan(12)) {
							double x= (int) Ref.get(packet, xField) / 32.0;
							double y= (int) Ref.get(packet, yField) / 32.0;
							double z= (int) Ref.get(packet, zField) / 32.0;
							for(ArmorStandHologram line : player.getAdditionalLines())
								line.shouldTeleport(x,y,z);
						}
						else if (moveRot != null) {
							Object pos = Ref.get(Ref.get(packet, moveRot), positionVec);
							double x= (double) Ref.get(pos, xField);
							double y= (double) Ref.get(pos, yField);
							double z= (double) Ref.get(pos, zField);
							for(ArmorStandHologram line : player.getAdditionalLines())
								line.shouldTeleport(x,y,z);
						} else {
							double x= (double) Ref.get(packet, xField);
							double y= (double) Ref.get(packet, yField);
							double z= (double) Ref.get(packet, zField);
							for(ArmorStandHologram line : player.getAdditionalLines())
								line.shouldTeleport(x,y,z);
						}
				}
			}
		};
		listener.register();
		for (Player online : BukkitLoader.getOnlinePlayers())
			TabAPI.getHolder(online).afterConnection();

		// Init online players
		for (ClassicTabPlayer player : TabAPI.data.values())
			for (ClassicTabPlayer other : TabAPI.data.values()) {
				if (player.equals(other))
					continue;
				// Tablist sorting
				if (player.getPlayer().canSee(other.getPlayer()))
					player.showLines(other);
				if (other.getPlayer().canSee(player.getPlayer()))
					other.showLines(player);
			}
	}

	public void unload() {
		isLoaded = false;
		teamManager.disable();
		listener.unregister();
		TabAPI.unload();
	}

	public boolean isLoaded() {
		return isLoaded;
	}

}
