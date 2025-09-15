package me.devtec.craftyserversystem.utils.tablist.nametag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.NametagHologram;
import me.devtec.shared.annotations.Comment;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.components.Component;
import me.devtec.shared.dataholder.cache.ConcurrentSet;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.CollisionRule;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.Visibility;

public class NametagPlayer {
	private static final Function<Player, String> DEFAULT_NAMETAG = Player::getName;

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
	private volatile boolean afterJoin;
	private final List<Object> packets = new ArrayList<>();

	public NametagPlayer(@Nonnull String name, @Nonnull UUID uuid, @Nonnull String teamname) {
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
		return this;
	}

	public NametagPlayer afterJoin() {
		BukkitLoader.getPacketHandler().send(getPlayer(),
				TeamUtils.createTeamPacket(0, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
		for (Object packet : packets)
			BukkitLoader.getPacketHandler().send(getPlayer(), packet);
		packets.clear();
		afterJoin = true;
		return this;
	}

	public void sendTeamPacket(Object packet) {
		if (!afterJoin)
			packets.add(packet);
		else
			BukkitLoader.getPacketHandler().send(getPlayer(), packet);
	}

	@Nullable
	public NametagHologram getNametag() {
		return nametag;
	}

	public void setTeamName(@Nonnull String newTeamName) {
		if (newTeamName.equals(teamName))
			return;

		if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
			for (NametagPlayer target : withProfile)
				target.sendTeamPacket(TeamUtils.createTeamPacket(4, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
		else
			for (NametagPlayer target : withProfile)
				target.sendTeamPacket(TeamUtils.createTeamPacket(1, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
		for (NametagPlayer target : withProfile)
			target.sendTeamPacket(TeamUtils.createTeamPacket(0, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, newTeamName, Visibility.NEVER, CollisionRule.ALWAYS));

		if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
			sendTeamPacket(TeamUtils.createTeamPacket(4, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
		else
			sendTeamPacket(TeamUtils.createTeamPacket(1, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
		sendTeamPacket(TeamUtils.createTeamPacket(0, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, newTeamName, Visibility.NEVER, CollisionRule.ALWAYS));
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
		if (withProfile.add(player))
			if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
				player.sendTeamPacket(TeamUtils.createTeamPacket(3, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
			else
				player.sendTeamPacket(TeamUtils.createTeamPacket(0, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
	}

	public void removeTabSorting(@Nonnull NametagPlayer player) {
		if (withProfile.remove(player))
			if (NametagManagerAPI.get().getPlayerCountInTeam(teamName) > 1)
				player.sendTeamPacket(TeamUtils.createTeamPacket(4, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
			else
				player.sendTeamPacket(TeamUtils.createTeamPacket(1, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName, Visibility.NEVER, CollisionRule.ALWAYS));
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
		for (NametagPlayer whoSee : getWhoSeeTabSorting())
			BukkitLoader.getPacketHandler().send(getPlayer(), TeamUtils.createTeamPacket(1, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, whoSee.name, whoSee.teamName));
		withProfile.clear();
		BukkitLoader.getPacketHandler().send(getPlayer(), TeamUtils.createTeamPacket(1, TeamUtils.white, Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT, name, teamName));
		if (getPlayer().getVehicle() != null)
			NametagManagerAPI.get().watchingEntityMove.remove(getPlayer().getVehicle().getEntityId());
		synchronized (NametagManagerAPI.get().getPlayers()) {
			for (NametagPlayer online : NametagManagerAPI.get().getPlayers()) {
				online.removeTabSorting(this);
				online.hideNametag(this);
			}
			nametag.hideAll();
		}
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NametagPlayer ? uuid.equals(((NametagPlayer) obj).uuid) : false;
	}

	@Override
	public String toString() {
		return "NametagPlayer{name=" + name + ", uuid=" + uuid + ", team=" + teamName + "}";
	}
}
