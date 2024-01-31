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

import com.google.common.collect.ImmutableList;

import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
import me.devtec.shared.Ref;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.cache.ConcurrentSet;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;

public class NametagHologram extends Hologram {

	private static final Object entityTypeArmorStand = findEntityType();
	private static Constructor<?> spawnEntityPacket = Ref.constructor(Ref.nms("network.protocol.game", "PacketPlayOutSpawnEntity"), int.class, UUID.class, double.class, double.class, double.class,
			float.class, float.class, Ref.nms("world.entity", "EntityTypes"), int.class, Ref.nms("world.phys", "Vec3D"), double.class);
	private static final Object zero = Ref.isNewerThan(18) ? Ref.getStatic(Ref.nms("world.phys", "Vec3D"), "b") : Ref.getStatic(Ref.nms("world.phys", "Vec3D"), "a");

	private static Object findEntityType() {
		Class<?> entityTypes = Ref.nms("world.entity", "EntityTypes");
		Class<?> nmsHuman = Ref.nms("world.entity.decoration", "EntityArmorStand");
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
		id = integer.incrementAndGet();
		if (owner.getVehicle() != null) {
			double additionalY = owner.getVehicle().getLocation().getY();
			EntityType type = owner.getVehicle().getType();
			if (type == EntityType.HORSE || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE)
				additionalY += 0.85;
			else if (type == EntityType.DONKEY)
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
		metadataPacket = HologramHolder.packetMetadata(id, metadataListValue = Arrays.asList(makeItemInstance(data, owner.isSneaking() ? (byte) 34 : (byte) 32), makeItemInstance(showName, true),
				makeItemInstance(name, Optional.of(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(text, true, false)))), makeItemInstance(properties, (byte) (16 | 1))));
		spawnPacket = Ref.newInstance(spawnEntityPacket, id, uuid, prevX, prevY + height, prevZ, 0, 0, entityTypeArmorStand, 0, zero, 0);
		despawnPacket = BukkitLoader.getNmsProvider().packetEntityDestroy(id);
	}

	private static Constructor<?> dataWatcherItem = Ref.constructor(Ref.nms("network.syncher", "DataWatcher$Item"), Ref.nms("network.syncher", "DataWatcherObject"), Object.class);
	private static Method dataWatcherMakeInstance = Ref.method(Ref.nms("network.syncher", "DataWatcher$Item"), "e");

	private static Object makeItemInstance(Object dataIndex, Object value) {
		return Ref.isNewerThan(19) || Ref.serverVersionInt() == 19 && Ref.serverVersionRelease() >= 2 ? Ref.invoke(Ref.newInstance(dataWatcherItem, dataIndex, value), dataWatcherMakeInstance)
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
				spawnPacket = Ref.newInstance(spawnEntityPacket, id, uuid, prevX, prevY + height, prevZ, 0, 0, entityTypeArmorStand, 0, zero, 0);
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

		metadataListValue.set(2, makeItemInstance(name, Optional.of(BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(text, true, false)))));
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
			if (type == EntityType.HORSE || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE)
				additionalY += 0.85;
			else if (type == EntityType.DONKEY)
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
			if (type == EntityType.HORSE || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE)
				additionalY += 0.85;
			else if (type == EntityType.DONKEY)
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
			if (type == EntityType.HORSE || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE)
				additionalY += 0.85;
			else if (type == EntityType.DONKEY)
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
			if (type == EntityType.HORSE || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE)
				additionalY += 0.85;
			else if (type == EntityType.DONKEY)
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

	public static Object createTeamPacket(int mode, String holderName, String teamName) {
		Object packet = BukkitLoader.getNmsProvider().packetScoreboardTeam();
		Object nameList = ImmutableList.of(holderName);
		String always = "always";
		if (Ref.isNewerThan(16)) {
			Ref.set(packet, "i", teamName);
			try {
				Object o = Ref.newUnsafeInstance(TeamUtils.sbTeam);
				Ref.set(o, "a", BukkitLoader.getNmsProvider().chatBase("{\"text\":\"\"}"));
				Ref.set(o, "b", BukkitLoader.getNmsProvider().toIChatBaseComponent((Component) null));
				Ref.set(o, "c", BukkitLoader.getNmsProvider().toIChatBaseComponent((Component) null));
				Ref.set(o, "d", "never");
				Ref.set(o, "e", always);
				Ref.set(o, "f", TeamUtils.white);
				Ref.set(packet, "k", Optional.of(o));
			} catch (Exception var10) {
			}
			Ref.set(packet, "h", mode);
			Ref.set(packet, "j", nameList);
		} else {
			Ref.set(packet, "a", teamName);
			Ref.set(packet, "b", Ref.isNewerThan(12) ? BukkitLoader.getNmsProvider().chatBase("{\"text\":\"\"}") : "");
			Ref.set(packet, "c", Ref.isNewerThan(12) ? BukkitLoader.getNmsProvider().toIChatBaseComponent((Component) null) : "");
			Ref.set(packet, "d", Ref.isNewerThan(12) ? BukkitLoader.getNmsProvider().toIChatBaseComponent((Component) null) : "");
			if (Ref.isNewerThan(7)) {
				Ref.set(packet, "e", "never");
				Ref.set(packet, "f", Ref.isNewerThan(8) ? always : -1);
				if (Ref.isNewerThan(8))
					Ref.set(packet, "g", Ref.isNewerThan(12) ? TeamUtils.white : -1);
				Ref.set(packet, Ref.isNewerThan(8) ? "i" : "h", mode);
				Ref.set(packet, Ref.isNewerThan(8) ? "h" : "g", nameList);
			} else {
				Ref.set(packet, "f", mode);
				Ref.set(packet, "e", nameList);
			}
		}
		return packet;
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
			if (type == EntityType.HORSE || type == EntityType.SKELETON_HORSE || type == EntityType.ZOMBIE_HORSE)
				additionalY += 0.85;
			else if (type == EntityType.DONKEY)
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
