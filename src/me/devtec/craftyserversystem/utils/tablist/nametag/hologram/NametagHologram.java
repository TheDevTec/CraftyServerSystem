package me.devtec.craftyserversystem.utils.tablist.nametag.hologram;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
import me.devtec.shared.Ref;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.cache.ConcurrentSet;
import me.devtec.shared.utility.MathUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class NametagHologram extends Hologram {

	private static final Object entityTypeArmorStand = findEntityType();
	private static Constructor<?> spawnEntityPacket = Ref.constructor(Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ClientboundAddEntityPacket" : "PacketPlayOutSpawnEntity"),
			int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, Ref.nms("world.entity", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "EntityType" : "EntityTypes"),
			int.class, Ref.nms("world.phys", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "Vec3" : "Vec3D"), double.class);
	private static byte LEGACY_SPAWN_PACKET;
	static {
		if (spawnEntityPacket == null) {
			spawnEntityPacket = Ref.constructor(Ref.nms("network.protocol.game", "PacketPlayOutSpawnEntity"), int.class, UUID.class, double.class, double.class, double.class, float.class, float.class,
					Ref.nms("world.entity", "EntityTypes"), int.class, Ref.nms("world.phys", "Vec3D"));
			LEGACY_SPAWN_PACKET = 1;
		}
		if (spawnEntityPacket == null) {
			spawnEntityPacket = Ref.constructor(Ref.nms("network.protocol.game", "PacketPlayOutSpawnEntity"));
			LEGACY_SPAWN_PACKET = 2;
		}
	}
	private static final Object zero = Ref.isNewerThan(18)
			? BukkitLoader.NO_OBFUSCATED_NMS_MODE ? Ref.getStatic(Ref.nms("world.phys", "Vec3"), "ZERO") : Ref.getStatic(Ref.nms("world.phys", "Vec3D"), "b")
			: Ref.getStatic(Ref.nms("world.phys", "Vec3D"), "a");

	private static Object findEntityType() {
		Class<?> entityTypes = Ref.nms("world.entity", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "EntityType" : "EntityTypes");
		Class<?> nmsHuman = Ref.nms("world.entity.decoration", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ArmorStand" : "EntityArmorStand");
		for (Field field : Ref.getAllFields(entityTypes))
			try {
				if (field.getType().equals(entityTypes) && field.getGenericType() instanceof ParameterizedType
						&& ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].equals(nmsHuman))
					return Ref.get(null, field);
			} catch (Exception e) {

			}
		return null;
	}

	private Set<NametagPlayer> whichCanSee = new ConcurrentSet<>();
	private World prevWorld;
	private double prevX, prevY, prevZ;
	private boolean shouldUpdate;
	private Player owner;

	private String nametag;

	private double height;
	private boolean sneaking;

	public NametagHologram(Player owner, World world, double x, double y, double z, String text) {
		this.owner = owner;
		nametag = text;
		uuid = UUID.randomUUID();
		prevX = x;
		prevY = y;
		prevZ = z;
		prevWorld = world;
		updateHeight(owner.isSneaking(), owner.isSleeping(), owner.getVehicle() != null, true);
		id = HologramHolder.increaseAndGetId();
		if (owner.getVehicle() != null) {
			double additionalY = owner.getVehicle().getLocation().getY();
			EntityType type = owner.getVehicle().getType();
			if (type == EntityType.HORSE || type.name().equals("SKELETON_HORSE") || type.name().equals("ZOMBIE_HORSE"))
				additionalY += 0.85;
			else if (type.name().equals("DONKEY"))
				additionalY += 0.525;
			else if (type.name().equals("CAMEL"))
				additionalY += 1.15;
			else if (type == EntityType.PIG)
				additionalY += 0.325;
			else if (type.name().equals("STRIDER"))
				additionalY += 1.15;
			else if (type == EntityType.BOAT || type.name().equals("CHEST_BOAT"))
				additionalY += -0.45;
			prevY = additionalY;
		}
		metadataPacket = HologramHolder.packetMetadata(id,
				metadataListValue = Arrays.asList(makeItemInstance(data, owner.isSneaking() ? (byte) 34 : (byte) 32), makeItemInstance(showName, true),
						makeItemInstance(name, LEGACY_SPAWN_PACKET == 2 ? text : Optional.of(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(text, true, false)))),
						makeItemInstance(properties, (byte) (16 | 1))));
		switch (LEGACY_SPAWN_PACKET) {
		case 0:
			spawnPacket = Ref.newInstance(spawnEntityPacket, id, uuid, prevX, prevY + height, prevZ, 0, 0, entityTypeArmorStand, 0, zero, 0);
			break;
		case 1:
			spawnPacket = Ref.newInstance(spawnEntityPacket, id, uuid, prevX, prevY + height, prevZ, 0, 0, entityTypeArmorStand, 0, zero);
			break;
		case 2:
			spawnPacket = Ref.newInstance(spawnEntityPacket);
			Ref.set(spawnPacket, "a", id);
			if (Ref.isOlderThan(12)) {
				Ref.set(spawnPacket, "b", MathUtils.floor(prevX * 32.0));
				Ref.set(spawnPacket, "c", MathUtils.floor((prevY + height) * 32.0));
				Ref.set(spawnPacket, "d", MathUtils.floor(prevZ * 32.0));
				Ref.set(spawnPacket, "j", 78);
			} else {
				Ref.set(spawnPacket, "b", uuid);
				Ref.set(spawnPacket, "c", prevX);
				Ref.set(spawnPacket, "d", prevY + height);
				Ref.set(spawnPacket, "e", prevZ);
				Ref.set(spawnPacket, "k", 78);
			}
			break;
		}
		despawnPacket = BukkitLoader.getNmsProvider().packetEntityDestroy(id);
	}

	private static Constructor<?> dataWatcherItem = Ref.constructor(Ref.nms("network.syncher", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "SynchedEntityData$DataItem" : "DataWatcher$Item"),
			Ref.nms("network.syncher", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "EntityDataAccessor" : "DataWatcherObject"), Object.class);
	private static Method dataWatcherMakeInstance = Ref.method(Ref.nms("network.syncher", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "SynchedEntityData$DataItem" : "DataWatcher$Item"),
			BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "value" : "e");
	static {
		if (dataWatcherMakeInstance == null)
			dataWatcherMakeInstance = Ref.method(Ref.nms("network.syncher", "DataWatcher$Item"), "a");
		if (Ref.isOlderThan(12)) {
			dataWatcherItem = Ref.constructor(Ref.nms("network.syncher", "DataWatcher$WatchableObject"), int.class, int.class, Object.class);
			dataWatcherMakeInstance = Ref.method(Ref.nms("network.syncher", "DataWatcher$WatchableObject"), "a");
		}

	}

	private static Object makeItemInstance(Object dataIndex, Object value) {
		return Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? Ref.invoke(Ref.newInstance(dataWatcherItem, dataIndex, value), dataWatcherMakeInstance)
				: Ref.isOlderThan(12)
						? Ref.newInstance(dataWatcherItem, value.getClass() == String.class ? 4 : 0, (int) dataIndex, value instanceof Boolean ? (boolean) value ? (byte) 1 : (byte) 0 : value)
						: Ref.newInstance(dataWatcherItem, dataIndex, value);
	}

	public Player getPlayer() {
		return owner;
	}

	public void show(NametagPlayer to) {
		if (!Objects.equals(owner.getWorld(), prevWorld)) {
			hideAll();
			prevWorld = owner.getWorld();
			Location playerLoc = owner.getLocation();
			prevX = playerLoc.getX();
			prevY = playerLoc.getY();
			prevZ = playerLoc.getZ();
			shouldUpdate = true;
		}
		if (whichCanSee.add(to)) {
			if (shouldUpdate) {
				switch (LEGACY_SPAWN_PACKET) {
				case 0:
					spawnPacket = Ref.newInstance(spawnEntityPacket, id, uuid, prevX, prevY + height, prevZ, 0, 0, entityTypeArmorStand, 0, zero, 0);
					break;
				case 1:
					spawnPacket = Ref.newInstance(spawnEntityPacket, id, uuid, prevX, prevY + height, prevZ, 0, 0, entityTypeArmorStand, 0, zero);
					break;
				case 2:
					spawnPacket = Ref.newInstance(spawnEntityPacket);
					Ref.set(spawnPacket, "a", id);
					if (Ref.isOlderThan(12)) {
						Ref.set(spawnPacket, "b", MathUtils.floor(prevX * 32.0));
						Ref.set(spawnPacket, "c", MathUtils.floor((prevY + height) * 32.0));
						Ref.set(spawnPacket, "d", MathUtils.floor(prevZ * 32.0));
						Ref.set(spawnPacket, "j", 78);
					} else {
						Ref.set(spawnPacket, "b", uuid);
						Ref.set(spawnPacket, "c", prevX);
						Ref.set(spawnPacket, "d", prevY + height);
						Ref.set(spawnPacket, "e", prevZ);
						Ref.set(spawnPacket, "k", 78);
					}
					break;
				}
				shouldUpdate = false;
			}
			super.show(to.getPlayer());
		}
	}

	public void hide(NametagPlayer to) {
		if (whichCanSee.remove(to))
			super.hide(to.getPlayer());
	}

	public void setText(String text) {
		if (nametag.equals(text))
			return;
		nametag = text;

		metadataListValue.set(2, makeItemInstance(name, LEGACY_SPAWN_PACKET == 2 ? text : Optional.of(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(text, true, false)))));
		for (NametagPlayer asPlayer : whichCanSee) {
			if (!asPlayer.getPlayer().isOnline())
				continue;
			BukkitLoader.getPacketHandler().send(asPlayer.getPlayer(), metadataPacket);
		}

	}

	public void shouldTeleport(Location playerLoc) {
		if (owner.getVehicle() != null) {
			double additionalY = owner.getVehicle().getLocation().getY();
			EntityType type = owner.getVehicle().getType();
			if (type == EntityType.HORSE || type.name().equals("SKELETON_HORSE") || type.name().equals("ZOMBIE_HORSE"))
				additionalY += 0.85;
			else if (type.name().equals("DONKEY"))
				additionalY += 0.525;
			else if (type.name().equals("CAMEL"))
				additionalY += 1.15;
			else if (type == EntityType.PIG)
				additionalY += 0.325;
			else if (type.name().equals("STRIDER"))
				additionalY += 1.15;
			else if (type == EntityType.BOAT || type.name().equals("CHEST_BOAT"))
				additionalY += -0.45;
			playerLoc.setY(additionalY);
		}
		if (!prevWorld.equals(playerLoc.getWorld())) {
			hideAll();
			prevWorld = playerLoc.getWorld();
			prevX = playerLoc.getX();
			prevY = playerLoc.getY();
			prevZ = playerLoc.getZ();
			shouldUpdate = true;
			return;
		}
		if (!areSame(playerLoc)) {
			prevX = playerLoc.getX();
			prevY = playerLoc.getY();
			prevZ = playerLoc.getZ();
			Object teleportPacket = HologramHolder.packetTeleport(id, prevX, prevY + height, prevZ);
			for (NametagPlayer player : whichCanSee)
				if (player.getPlayer().isOnline())
					BukkitLoader.getPacketHandler().send(player.getPlayer(), teleportPacket);
			shouldUpdate = true;
		}
	}

	public void shouldTeleport(double x, double y, double z) {
		if (owner.getVehicle() != null) {
			double additionalY = owner.getVehicle().getLocation().getY();
			EntityType type = owner.getVehicle().getType();
			if (type == EntityType.HORSE || type.name().equals("SKELETON_HORSE") || type.name().equals("ZOMBIE_HORSE"))
				additionalY += 0.85;
			else if (type.name().equals("DONKEY"))
				additionalY += 0.525;
			else if (type.name().equals("CAMEL"))
				additionalY += 1.15;
			else if (type == EntityType.PIG)
				additionalY += 0.325;
			else if (type.name().equals("STRIDER"))
				additionalY += 1.15;
			else if (type == EntityType.BOAT || type.name().equals("CHEST_BOAT"))
				additionalY += -0.45;
			y = additionalY;
		}
		if (!areSame(x, y, z)) {
			prevX = x;
			prevY = y;
			prevZ = z;
			Object teleportPacket = HologramHolder.packetTeleport(id, prevX, prevY + height, prevZ);
			for (NametagPlayer player : whichCanSee)
				if (player.getPlayer().isOnline())
					BukkitLoader.getPacketHandler().send(player.getPlayer(), teleportPacket);
			shouldUpdate = true;
		}
	}

	public void shouldTeleport(NametagPlayer player, Location playerLoc) {
		if (owner.getVehicle() != null) {
			double additionalY = owner.getVehicle().getLocation().getY();
			EntityType type = owner.getVehicle().getType();
			if (type == EntityType.HORSE || type.name().equals("SKELETON_HORSE") || type.name().equals("ZOMBIE_HORSE"))
				additionalY += 0.85;
			else if (type.name().equals("DONKEY"))
				additionalY += 0.525;
			else if (type.name().equals("CAMEL"))
				additionalY += 1.15;
			else if (type == EntityType.PIG)
				additionalY += 0.325;
			else if (type.name().equals("STRIDER"))
				additionalY += 1.15;
			else if (type == EntityType.BOAT || type.name().equals("CHEST_BOAT"))
				additionalY += -0.45;
			playerLoc.setY(additionalY);
		}
		if (!prevWorld.equals(playerLoc.getWorld())) {
			hideAll();
			prevWorld = playerLoc.getWorld();
			prevX = playerLoc.getX();
			prevY = playerLoc.getY();
			prevZ = playerLoc.getZ();
			shouldUpdate = true;
			return;
		}
		if (!areSame(playerLoc)) {
			prevX = playerLoc.getX();
			prevY = playerLoc.getY();
			prevZ = playerLoc.getZ();
			Object teleportPacket = HologramHolder.packetTeleport(id, prevX, prevY + height, prevZ);
			BukkitLoader.getPacketHandler().send(player.getPlayer(), teleportPacket);
			shouldUpdate = true;
		}
	}

	public void shouldTeleport(NametagPlayer player, double x, double y, double z) {
		if (owner.getVehicle() != null) {
			double additionalY = owner.getVehicle().getLocation().getY();
			EntityType type = owner.getVehicle().getType();
			if (type == EntityType.HORSE || type.name().equals("SKELETON_HORSE") || type.name().equals("ZOMBIE_HORSE"))
				additionalY += 0.85;
			else if (type.name().equals("DONKEY"))
				additionalY += 0.525;
			else if (type.name().equals("CAMEL"))
				additionalY += 1.15;
			else if (type == EntityType.PIG)
				additionalY += 0.325;
			else if (type.name().equals("STRIDER"))
				additionalY += 1.15;
			else if (type == EntityType.BOAT || type.name().equals("CHEST_BOAT"))
				additionalY += -0.45;
			y = additionalY;
		}
		if (!areSame(x, y, z)) {
			prevX = x;
			prevY = y;
			prevZ = z;
			Object teleportPacket = HologramHolder.packetTeleport(id, prevX, prevY + height, prevZ);
			BukkitLoader.getPacketHandler().send(player.getPlayer(), teleportPacket);
			shouldUpdate = true;
		}
	}

	private boolean areSame(Location loc) {
		return prevX == loc.getX() && prevY == loc.getY() && prevZ == loc.getZ();
	}

	private boolean areSame(double x, double y, double z) {
		return prevX == x && prevY == y && prevZ == z;
	}

	public void hideAll() {
		for (NametagPlayer asPlayer : whichCanSee) {
			if (!asPlayer.getPlayer().isOnline())
				continue;
			super.hide(asPlayer.getPlayer());
		}
		whichCanSee.clear();
	}

	public void sneak(boolean sneaking) {
		if (metadataListValue == null)
			return;
		if (this.sneaking != sneaking) {
			this.sneaking = sneaking;
			if (sneaking)
				metadataListValue.set(0, makeItemInstance(data, (byte) 34));
			else
				metadataListValue.set(0, makeItemInstance(data, (byte) 32));
			for (NametagPlayer asPlayer : whichCanSee) {
				if (!asPlayer.getPlayer().isOnline())
					continue;
				BukkitLoader.getPacketHandler().send(asPlayer.getPlayer(), metadataPacket);
			}
		}
	}

	public void updateHeight(boolean sneaking, boolean sleeping, boolean riding, boolean ignore) {
		height = 2 + -0.12;
		if (riding) {
			height -= 0.12;
			sneak(false);
		} else if (sleeping) {
			height -= 1.44;
			sneak(false);
		} else {
			if (sneaking)
				height -= 0.44;
			sneak(sneaking);
		}
		if (ignore)
			return;
		Object teleportPacket = HologramHolder.packetTeleport(id, prevX, prevY + height, prevZ);
		for (NametagPlayer asPlayer : whichCanSee) {
			if (!asPlayer.getPlayer().isOnline())
				continue;
			BukkitLoader.getPacketHandler().send(asPlayer.getPlayer(), teleportPacket);
		}
	}

	public void setPosWithoutUpdate(Location location) {
		prevX = location.getX();
		prevZ = location.getZ();
		Entity vehicle = owner.getVehicle();
		if (vehicle != null) {
			double additionalY = vehicle.getLocation().getY();
			EntityType type = vehicle.getType();
			if (type == EntityType.HORSE || type.name().equals("SKELETON_HORSE") || type.name().equals("ZOMBIE_HORSE"))
				additionalY += 0.85;
			else if (type.name().equals("DONKEY"))
				additionalY += 0.525;
			else if (type.name().equals("CAMEL"))
				additionalY += 1.15;
			else if (type == EntityType.PIG)
				additionalY += 0.325;
			else if (type.name().equals("STRIDER"))
				additionalY += 1.15;
			else if (type == EntityType.BOAT || type.name().equals("CHEST_BOAT"))
				additionalY += -0.45;
			prevY = additionalY;
		} else
			prevY = location.getY();
	}
}
