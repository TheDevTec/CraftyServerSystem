package me.devtec.craftyserversystem.utils.tablist.nametag.hologram;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.devtec.shared.Ref;
import me.devtec.shared.utility.MathUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public interface HologramHolder {

	AtomicInteger integer = (AtomicInteger) Ref.getStatic(Ref.field(Ref.nms("world.entity", "Entity"), AtomicInteger.class));
	Field entityCount = Ref.field(Ref.nms("world.entity", "Entity"), "entityCount");

	Class<?> metadataClass = Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ClientboundSetEntityDataPacket" : "PacketPlayOutEntityMetadata");
	Constructor<?> metadataConstructor = Ref.constructor(metadataClass, int.class, List.class);
	Class<?> entityTeleport = Ref.nms("network.protocol.game", BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "ClientboundTeleportEntityPacket" : "PacketPlayOutEntityTeleport");
	Field entityId = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "id" : "a"), posX = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "x" : "b"),
			posY = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "y" : "c"), posZ = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "z" : "d"),
			onGround = Ref.field(entityTeleport, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "onGround" : "g");
	Field metadataId = Ref.isNewerThan(20) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? null : Ref.field(metadataClass, int.class);
	Field metadataList = Ref.field(metadataClass, List.class);

	static Object packetMetadata(int id, List<?> list) {
		try {
			return Ref.isNewerThan(20) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? Ref.newInstance(metadataConstructor, id, list)
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
