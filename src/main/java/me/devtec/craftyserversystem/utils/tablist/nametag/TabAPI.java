package me.devtec.craftyserversystem.utils.tablist.nametag;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.Setter;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer;
import me.devtec.shared.API;
import me.devtec.shared.Ref;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.components.Component;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.CollisionRule;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.Visibility;
import me.devtec.theapi.bukkit.packetlistener.ChannelContainer;
import me.devtec.theapi.bukkit.packetlistener.PacketContainer;
import me.devtec.theapi.bukkit.packetlistener.PacketListener;

public class TabAPI {
	protected static Map<UUID, ClassicTabPlayer> data = new ConcurrentHashMap<>();

	@Getter
	@Setter
	public static class SimpleTeam {
		private String team;
		private Component prefix;
		private Component suffix;
		private Component displayName;
		private ChatColor color;
		private int friendlyFlags;
		private CollisionRule collisionRule;
		private Visibility nametagVisibility;
		private Set<String> players = new HashSet<>();

		public SimpleTeam(String name, Component prefix, Component suffix, Component displayName, ChatColor color, int friendlyFlags,
				CollisionRule collisionRule, Visibility nametagVisibility) {
			this.team = name;
			this.prefix = prefix;
			this.suffix = suffix;
			this.displayName = displayName;
			this.color = color;
			this.friendlyFlags=friendlyFlags;
			this.collisionRule = collisionRule;
			this.nametagVisibility = nametagVisibility;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof SimpleTeam ? ((SimpleTeam)obj).team.equals(this.team) : false;
		}

		@Override
		public int hashCode() {
			return team.hashCode();
		}

		public SimpleTeam leavePlayer(String name) {
			players.remove(name);
			SimpleTeam t = new SimpleTeam(team, null, null, null, null, 0, null, null);
			t.players.add(name);
			return t;
		}

		public SimpleTeam remove() {
			return this;
		}

		public SimpleTeam joinPlayer(String name) {
			players.add(name);
			return this;
		}

		@Override
		public String toString() {
			return "Team[name="+team+",prefix="+prefix+",suffix="+suffix+",displayName="+displayName+",color="+color.name()+",friendlyFlags="+friendlyFlags+",collosions="+collisionRule+",visibility="+nametagVisibility+"]";
		}
	}

	public static void register() {
		Class<?> teamPacket = Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket");
		Method getName = Ref.method(teamPacket, "getName");
		Method getParameters = Ref.method(teamPacket, "getParameters");
		Method getPlayers = Ref.method(teamPacket, "getPlayers");
		Method getCollisionRule = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getCollisionRule");
		Method getNametagVisibility = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getNametagVisibility");
		Method getPlayerPrefix = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getPlayerPrefix");
		Method getPlayerSuffix = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getPlayerSuffix");
		Method getDisplayName = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getDisplayName");
		Method getOptions = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getOptions");
		Method getColor = Ref.method(Ref.nms("network.protocol.game", "ClientboundSetPlayerTeamPacket$Parameters"), "getColor");
		new PacketListener() {

			@Override
			public void playOut(String name, PacketContainer packetContainer, ChannelContainer channel) {
				Object packet = packetContainer.getPacket();
				if (packet.getClass().equals(teamPacket)) {

					@SuppressWarnings("unchecked")
					Collection<String> players = (Collection<String>)Ref.invoke(packet, getPlayers);
					String teamName = (String)Ref.invoke(packet, getName);
					Optional<?> optional = (Optional<?>)Ref.invoke(packet, getParameters);
					Object parameters = optional==null?null:optional.orElse(null);
					Set<SimpleTeam> teams = getHolder(name).getTeams();
					SimpleTeam team = null;
					switch ((int) Ref.get(packet, TeamUtils.teamMethod)) {
					case TeamUtils.METHOD_ADD:
						teams.add(team=new SimpleTeam(
								teamName,
								parameters==null?null:BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerPrefix)),
										parameters==null?null:BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerSuffix)),
												parameters==null?null:BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getDisplayName)),
														parameters==null?ChatColor.WHITE:ChatColor.valueOf(((Enum<?>)Ref.invoke(parameters, getColor)).name()), (int)Ref.invoke(parameters, getOptions),
																parameters==null?CollisionRule.ALWAYS:CollisionRule.valueOf(Ref.invoke(parameters, getCollisionRule).toString().toUpperCase()),
																		parameters==null?Visibility.ALWAYS:Visibility.valueOf(Ref.invoke(parameters, getNametagVisibility).toString().toUpperCase())));
						team.getPlayers().addAll(players);
						break;
					case TeamUtils.METHOD_JOIN:
						for(SimpleTeam t : getHolder(name).getTeams())
							if(t.getTeam().equals(teamName)) {
								team=t;
								break;
							}
						if(team==null)
							teams.add(team=new SimpleTeam(
									teamName,
									parameters==null?null:BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerPrefix)),
											parameters==null?null:BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerSuffix)),
													parameters==null?null:BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getDisplayName)),
															parameters==null?ChatColor.WHITE:ChatColor.valueOf(((Enum<?>)Ref.invoke(parameters, getColor)).name()), (int)Ref.invoke(parameters, getOptions),
																	parameters==null?CollisionRule.ALWAYS:CollisionRule.valueOf(Ref.invoke(parameters, getCollisionRule).toString().toUpperCase()),
																			parameters==null?Visibility.ALWAYS:Visibility.valueOf(Ref.invoke(parameters, getNametagVisibility).toString().toUpperCase())));
						else if(parameters!=null){
							team.setPrefix(BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerPrefix)));
							team.setSuffix(BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerSuffix)));
							team.setDisplayName(BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getDisplayName)));
							team.setColor(ChatColor.valueOf(((Enum<?>)Ref.invoke(parameters, getColor)).name()));
							team.setCollisionRule(CollisionRule.valueOf(Ref.invoke(parameters, getCollisionRule).toString().toUpperCase()));
							team.setNametagVisibility(Visibility.valueOf(Ref.invoke(parameters, getNametagVisibility).toString().toUpperCase()));
						}
						team.getPlayers().addAll(players);
						break;
					case TeamUtils.METHOD_REMOVE:
						Iterator<SimpleTeam> itr = teams.iterator();
						while(itr.hasNext()) {
							SimpleTeam t = itr.next();
							if(t.getTeam().equals(teamName))itr.remove();
						}
						break;
					case TeamUtils.METHOD_CHANGE:
						for(SimpleTeam t : getHolder(name).getTeams())
							if(t.getTeam().equals(teamName)) {
								team=t;
								break;
							}
						if(team!=null && parameters!=null) {
							team.setPrefix(BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerPrefix)));
							team.setSuffix(BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getPlayerSuffix)));
							team.setDisplayName(BukkitLoader.getNmsProvider().fromIChatBaseComponent(Ref.invoke(parameters, getDisplayName)));
							team.setColor(ChatColor.valueOf(((Enum<?>)Ref.invoke(parameters, getColor)).name()));
							team.setCollisionRule(CollisionRule.valueOf(Ref.invoke(parameters, getCollisionRule).toString().toUpperCase()));
							team.setNametagVisibility(Visibility.valueOf(Ref.invoke(parameters, getNametagVisibility).toString().toUpperCase()));
						}
						break;
					case TeamUtils.METHOD_LEAVE:
						for(SimpleTeam t : teams)
							if(t.getTeam().equals(teamName)) {
								t.getPlayers().removeAll(players);
								break;
							}
						break;
					}
				}
			}

			@Override
			public void playIn(String name, PacketContainer packetContainer, ChannelContainer channel) {
			}
		}.register();
	}

	@Nonnull
	public static ClassicTabPlayer getHolder(Player player) {
		return data.computeIfAbsent(player.getUniqueId(), id->{
			ClassicTabPlayer result = new ClassicTabPlayer(player);
			if (player.getVehicle() != null) {
				List<ClassicTabPlayer> players = new ArrayList<>();
				players.add(result);
				NametagManagerAPI.get().watchingEntityMove.put(player.getVehicle().getEntityId(), players);
			}
			return result;});
	}

	@Nullable
	public static ClassicTabPlayer getHolder(UUID player) {
		Player online = Bukkit.getPlayer(player);
		return online == null ? data.get(player) : getHolder(online);
	}

	@Nullable
	public static ClassicTabPlayer getHolder(String playerName) {
		return getHolder(API.offlineCache().lookupId(playerName));
	}

	@Nullable
	public static ClassicTabPlayer await(String playerName) {
		return await(API.offlineCache().lookupId(playerName));
	}

	@Nullable
	public static ClassicTabPlayer await(UUID uuid) {
		Player online;
		while((online=Bukkit.getPlayer(uuid))==null);
		return getHolder(online);
	}

	@Nullable
	public static ClassicTabPlayer getHolder(int entityId) {
		for(Player player : BukkitLoader.getOnlinePlayers())
			if(player.isOnline() && player.getEntityId()==entityId)
				return data.get(player.getUniqueId());
		return null;
	}

	public static Collection<ClassicTabPlayer> getPlayers(){
		return data.values();
	}

	@Nullable
	public static ClassicTabPlayer removeHolder(UUID player) {
		ClassicTabPlayer holder = data.remove(player);
		if(holder!=null)
			for (ClassicTabPlayer active : TabAPI.getPlayers()) {
				active.getWhoSeeAdditionalLines().remove(holder);
				active.removeTeam(holder.getPrimaryTeam().getTeam());
			}
		return holder;
	}

	public static void unload() {
		for(ClassicTabPlayer player : data.values())
			player.onDisconnect();
		data.clear();
	}
}
