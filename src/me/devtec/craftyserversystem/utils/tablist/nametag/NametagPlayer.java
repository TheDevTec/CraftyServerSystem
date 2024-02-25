package me.devtec.craftyserversystem.utils.tablist.nametag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.NametagHologram;
import me.devtec.shared.annotations.Comment;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.dataholder.cache.ConcurrentSet;
import me.devtec.theapi.bukkit.BukkitLoader;

public class NametagPlayer {
	private static final AtomicInteger id = new AtomicInteger(0);
	private static final Function<Player, String> DEFAULT_NAMETAG = Player::getName;

	private final int privateId;
	// Player username
	private final String name;
	// Player UUID
	private final UUID uuid;
	// Nametag
	private NametagHologram nametag;

	private final Set<NametagPlayer> withProfile = new ConcurrentSet<>();
	// Team name
	private String teamName;
	// Nametag name generator
	private Function<Player, String> nametagGenerator = DEFAULT_NAMETAG;

	public NametagPlayer(@Nonnull String name, @Nonnull UUID uuid, @Nonnull String teamname) {
		privateId = id.incrementAndGet();
		this.name = name;
		this.uuid = uuid;
		teamName = teamname;
	}

	@Nullable
	public Player getPlayer() {
		return nametag == null ? null : nametag.getPlayer();
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public UUID getUUID() {
		return uuid;
	}

	public NametagPlayer initNametag(@Nonnull Player player) {
		Location loc = player.getLocation();
		nametag = new NametagHologram(player, player.getWorld(), loc.getX(), loc.getY(), loc.getZ(), nametagGenerator.apply(player));
		BukkitLoader.getPacketHandler().send(player, NametagHologram.createTeamPacket(0, name, "z_nametag"));
		BukkitLoader.getPacketHandler().send(player, NametagHologram.createTeamPacket(0, name, teamName));
		return this;
	}

	@Nullable
	public NametagHologram getNametag() {
		return nametag;
	}

	public void setTeamName(@Nonnull String newTeamName) {
		if (newTeamName.equals(teamName))
			return;
		List<Player> targets = new ArrayList<>(withProfile.size());
		for (NametagPlayer player : withProfile)
			targets.add(player.getPlayer());

		if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
			BukkitLoader.getPacketHandler().send(targets, NametagHologram.createTeamPacket(4, name, teamName));
		else
			BukkitLoader.getPacketHandler().send(targets, NametagHologram.createTeamPacket(1, name, teamName));
		BukkitLoader.getPacketHandler().send(targets, NametagHologram.createTeamPacket(0, name, newTeamName));

		if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
			BukkitLoader.getPacketHandler().send(getPlayer(), NametagHologram.createTeamPacket(4, name, teamName));
		else
			BukkitLoader.getPacketHandler().send(getPlayer(), NametagHologram.createTeamPacket(1, name, teamName));
		BukkitLoader.getPacketHandler().send(getPlayer(), NametagHologram.createTeamPacket(0, name, newTeamName));
		teamName = newTeamName;
	}

	@Nonnull
	public String getTeamName() {
		return teamName;
	}

	@Nonnull
	public Function<Player, String> getNametagGenerator() {
		return nametagGenerator;
	}

	public void setNametagGenerator(@Nullable Function<Player, String> function) {
		nametagGenerator = function == null ? DEFAULT_NAMETAG : function;
	}

	public void addTabSorting(@Nonnull NametagPlayer player) {
		if (withProfile.add(player)) {
			BukkitLoader.getPacketHandler().send(player.getPlayer(), NametagHologram.createTeamPacket(0, name, "z_nametag"));
			BukkitLoader.getPacketHandler().send(player.getPlayer(), NametagHologram.createTeamPacket(0, name, teamName));
		}
	}

	public void removeTabSorting(@Nonnull NametagPlayer player) {
		if (withProfile.remove(player))
			if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
				BukkitLoader.getPacketHandler().send(player.getPlayer(), NametagHologram.createTeamPacket(4, name, teamName));
			else
				BukkitLoader.getPacketHandler().send(player.getPlayer(), NametagHologram.createTeamPacket(1, name, teamName));
	}

	@Nonnull
	public Set<NametagPlayer> getWhoSeeTabSorting() {
		return withProfile;
	}

	public void showNametag(@Nonnull NametagPlayer player) {
		nametag.show(player);
	}

	public void hideNametag(@Nonnull NametagPlayer player) {
		nametag.hide(player);
	}

	public void remove() {
		setNametagGenerator(null);
		onQuit();
	}

	@Comment(comment = "Internal code - Called only when player quit server in the PlayerQuitEvent event")
	public void onQuit() {
		BukkitLoader.getPacketHandler().send(getPlayer(), NametagHologram.createTeamPacket(1, name, teamName));
		if (getPlayer().getVehicle() != null)
			NametagManagerAPI.get().watchingEntityMove.remove(getPlayer().getVehicle().getEntityId());
		synchronized (NametagManagerAPI.get().getPlayers()) {
			for (NametagPlayer online : NametagManagerAPI.get().getPlayers()) {
				online.removeTabSorting(this);
				online.hideNametag(this);
			}
			nametag.hideAll();
		}
		withProfile.clear();
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NametagPlayer ? privateId == ((NametagPlayer) obj).privateId : false;
	}

	@Override
	public String toString() {
		return "NametagPlayer{name=" + name + ", uuid=" + uuid + ", team=" + teamName + "}";
	}
}
