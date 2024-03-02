package me.devtec.craftyserversystem.utils.tablist.nametag.hologram;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.devtec.shared.Ref;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Hologram implements HologramHolder {
	static Object data = Ref.isOlderThan(12) ? 0 : findDataWatcherObject(Ref.nms("world.entity", "Entity"), Byte.class);
	static Object name = Ref.isOlderThan(12) ? 2 : findDataWatcherObject(Ref.nms("world.entity", "Entity"), Optional.class);
	static {
		if (name == null)
			name = findDataWatcherObject(Ref.nms("world.entity", "Entity"), String.class);
	}
	static Object showName = Ref.isOlderThan(12) ? 3 : findDataWatcherObject(Ref.nms("world.entity", "Entity"), Boolean.class);
	static Object properties = Ref.isOlderThan(12) ? 10 : findDataWatcherObject(Ref.nms("world.entity.decoration", "EntityArmorStand"), Byte.class);

	protected int id;
	protected UUID uuid;
	protected Object spawnPacket;
	protected Object despawnPacket;
	protected Object metadataPacket;
	protected List<Object> metadataListValue;

	protected void show(Player to) {
		BukkitLoader.getPacketHandler().send(to, spawnPacket);
		BukkitLoader.getPacketHandler().send(to, metadataPacket);
	}

	protected void hide(Player to) {
		BukkitLoader.getPacketHandler().send(to, despawnPacket);
	}

	private static final Object findDataWatcherObject(Class<?> inside, Class<?> holder) {
		for (Field field : Ref.getAllFields(inside))
			try {
				if (field.getType().equals(Ref.nms("network.syncher", "DataWatcherObject")) && field.getGenericType() instanceof ParameterizedType) {
					if (((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] instanceof ParameterizedType)
						if (((ParameterizedType) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]).getRawType().equals(holder))
							return Ref.get(null, field);
					if (((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].equals(holder))
						return Ref.get(null, field);
				}
			} catch (Exception e) {

			}
		return null;
	}
}
