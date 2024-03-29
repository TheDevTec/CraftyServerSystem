package me.devtec.craftyserversystem.utils.tablist.nametag.hologram;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.devtec.shared.Ref;
import me.devtec.shared.utility.MathUtils;

public interface HologramHolder {

	static AtomicInteger integer = (AtomicInteger) Ref.getStatic(Ref.field(Ref.nms("world.entity", "Entity"), AtomicInteger.class));
	static Field entityCount = Ref.field(Ref.nms("world.entity", "Entity"), "entityCount");

	static Class<?> metadataClass = Ref.nms("network.protocol.game", "PacketPlayOutEntityMetadata");
	static Constructor<?> metadataConstructor = Ref.constructor(metadataClass, int.class, List.class);
	static Class<?> entityTeleport = Ref.nms("network.protocol.game", "PacketPlayOutEntityTeleport");
	static Field entityId = Ref.field(entityTeleport, "a"), posX = Ref.field(entityTeleport, "b"), posY = Ref.field(entityTeleport, "c"), posZ = Ref.field(entityTeleport, "d"),
			onGround = Ref.field(entityTeleport, "g");
	static Field metadataId = Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? null : Ref.field(metadataClass, int.class);
	static Field metadataList = Ref.field(metadataClass, List.class);

	static Object packetMetadata(int id, List<?> list) {
		try {
			return Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? Ref.newInstance(metadataConstructor, id, list)
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

	public static int increaseAndGetId() {
		if (integer != null)
			return integer.incrementAndGet();
		int count = (int) Ref.getStatic(entityCount);
		Ref.setStatic(entityCount, count + 1);
		return count;
	}
}
