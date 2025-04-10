package me.devtec.craftyserversystem.utils.tablist.nametag.hologram;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import me.devtec.shared.Ref;
import me.devtec.shared.utility.MathUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public interface HologramHolder {

	AtomicInteger integer = (AtomicInteger) Ref
			.getStatic(Ref.field(Ref.nms("world.entity", "Entity"), AtomicInteger.class));
	Field entityCount = Ref.field(Ref.nms("world.entity", "Entity"), "entityCount");

	Class<?> metadataClass = Ref.nms("network.protocol.game",
			BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ClientboundSetEntityDataPacket" : "PacketPlayOutEntityMetadata");
	Constructor<?> metadataConstructor = Ref.constructor(metadataClass, int.class, List.class);
	Class<?> entityTeleport = Ref.nms("network.protocol.game",
			BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ClientboundTeleportEntityPacket" : "PacketPlayOutEntityTeleport");
	Field entityId = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "id" : "a"),
			posX = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "x" : "b"),
			posY = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "y" : "c"),
			posZ = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "z" : "d"),
			onGround = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "onGround" : "g");
	Field metadataId = Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? null
			: Ref.field(metadataClass, int.class);
	Field metadataList = Ref.field(metadataClass, List.class);
	Class<?> vec3D = Ref.nms("world.phys", "Vec3") == null ? Ref.nms("world.phys", "Vec3D")
			: Ref.nms("world.phys", "Vec3");
	Constructor<?> vec3DConstructor = Ref.constructor(vec3D, double.class, double.class, double.class);
	boolean modernPaper = Ref.field(entityTeleport, "f") == null;
	Constructor<?> modernTeleportPacket = Ref.constructor(entityTeleport, int.class,
			Ref.nms("world.entity", "PositionMoveRotation"), Set.class, boolean.class);
	Constructor<?> positionMoveRotation = Ref.constructor(Ref.nms("world.entity", "PositionMoveRotation"), vec3D, vec3D,
			float.class, float.class);

	static Object packetMetadata(int id, List<?> list) {
		try {
			return Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2
					? Ref.newInstance(metadataConstructor, id, list)
					: initMetadataPacket(Ref.newUnsafeInstance(metadataClass), id, list);
		} catch (Exception e) {
			return null;
		}
	}

	static Object initMetadataPacket(Object instance, int id, List<?> list) {
		Ref.set(instance, metadataId, id);
		Ref.set(instance, metadataList, list);
		return instance;
	}

	static Object packetTeleport(int id, double x, double y, double z) {
		if (modernPaper)
			return Ref.newInstance(modernTeleportPacket, id, Ref.newInstance(positionMoveRotation,
					Ref.newInstance(vec3DConstructor, x, y, z), Ref.newInstance(vec3DConstructor, 0, 0, 0), 0f, 0f),
					Collections.emptySet(), false);
		try {
			Object teleportPacket = Ref.newUnsafeInstance(entityTeleport);
			Ref.set(teleportPacket, entityId, id);
			if (Ref.isOlderThan(12)) {
				Ref.set(teleportPacket, posX, MathUtils.floor(x * 32));
				Ref.set(teleportPacket, posY, MathUtils.floor(y * 32));
				Ref.set(teleportPacket, posZ, MathUtils.floor(z * 32));
			} else {
				Ref.set(teleportPacket, posX, x);
				Ref.set(teleportPacket, posY, y);
				Ref.set(teleportPacket, posZ, z);
			}
			Ref.set(teleportPacket, onGround, false);
			return teleportPacket;
		} catch (Exception e) {
			return null;
		}
	}

	static int increaseAndGetId() {
		if (integer != null)
			return integer.incrementAndGet();
		int count = (int) Ref.getStatic(entityCount);
		Ref.setStatic(entityCount, count + 1);
		return count;
	}
}
