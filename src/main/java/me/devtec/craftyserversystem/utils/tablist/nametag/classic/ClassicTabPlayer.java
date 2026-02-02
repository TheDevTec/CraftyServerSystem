package me.devtec.craftyserversystem.utils.tablist.nametag.classic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.Setter;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI.SimpleTeam;
import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.ArmorStandHologram;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentEntity;
import me.devtec.shared.components.ComponentItem;
import me.devtec.shared.dataholder.cache.ConcurrentSet;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.CollisionRule;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.Visibility;

public class ClassicTabPlayer {
	private final static AtomicInteger counter = new AtomicInteger(0);
	private final static Object emptyTablistHeaderFooterPacket = BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT);

	public enum Display {
		TABLIST, NAMETAG
	}

	@Getter
	private Player player;

	@Getter
	private final int id = counter.incrementAndGet();

	@Getter
	private final Set<SimpleTeam> teams = new HashSet<>();

	//INTERNAL
	private boolean ready = false;
	private List<Object> packets = new ArrayList<>();

	@Getter
	private final List<ArmorStandHologram> additionalLines = new ArrayList<>();

	@Getter
	private final Set<ClassicTabPlayer> whoSeeAdditionalLines = new ConcurrentSet<>();

	@Getter
	private Component header;
	@Getter
	private Component footer;
	// Tabname
	@Getter
	@Setter
	private String tablistFormat = "{prefix}{player}{suffix}";
	private Component tabPrefix;
	private Component tabSuffix;
	// Nametag
	private Component tagPrefix;
	private Component tagSuffix;
	@Getter
	private SimpleTeam primaryTeam;

	public ClassicTabPlayer(Player player) {
		Bukkit.broadcastMessage("creating data for "+player.getName());
		this.player=player;
	}

	public void sendPacket(Object packet) {
		if (!ready)
			packets.add(packet);
		else
			BukkitLoader.getPacketHandler().send(getPlayer(), packet);
	}

	public void changePrimaryTeam(SimpleTeam newTeam) {
		if(newTeam == null || primaryTeam!=null && primaryTeam.getTeam().equals(newTeam.getTeam()))return;
		if(primaryTeam!=null) {
			for(ClassicTabPlayer player : TabAPI.getPlayers())
				if(player.getTeams().contains(primaryTeam)) {
					player.removeTeam(primaryTeam.getTeam());
					player.createTeam(newTeam);
				} else if(player.getPlayer().canSee(getPlayer()))
					player.createTeam(newTeam);
		} else
			for(ClassicTabPlayer player : TabAPI.getPlayers())
				if(player.getPlayer().canSee(getPlayer()))
					player.createTeam(newTeam);
		primaryTeam = newTeam;
	}

	public void createTeam(SimpleTeam team) {
		sendPacket(TeamUtils.createTeamPacket(TeamUtils.METHOD_ADD, team.getTeam(), team.getColor(), team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(), team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers()));
	}

	public void joinTeam(SimpleTeam team) {
		sendPacket(TeamUtils.createTeamPacket(TeamUtils.METHOD_JOIN, team.getTeam(), team.getColor(), team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(), team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers()));
	}


	public void updateTeam(SimpleTeam team) {
		sendPacket(TeamUtils.createTeamPacket(TeamUtils.METHOD_CHANGE, team.getTeam(), team.getColor(), team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(), team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers()));
	}


	public void leaveTeam(SimpleTeam team) {
		sendPacket(TeamUtils.createTeamPacket(TeamUtils.METHOD_LEAVE, team.getTeam(), null, null, null, null, null, null, 0, team.getPlayers()));
	}


	public void removeTeam(String team) {
		sendPacket(TeamUtils.createTeamPacket(TeamUtils.METHOD_REMOVE, team, null, null, null, null, null, null, 0, null));
	}


	@Override
	public boolean equals(Object obj) {
		return obj instanceof ClassicTabPlayer && ((ClassicTabPlayer)obj).getPlayer().equals(getPlayer());
	}


	@Override
	public int hashCode() {
		return player.hashCode();
	}


	public void showLines(ClassicTabPlayer holder) {
		if(getWhoSeeAdditionalLines().add(holder))
			for(ArmorStandHologram hologram : getAdditionalLines())
				hologram.show(holder);
	}


	public void hideLines(ClassicTabPlayer holder) {
		if(getWhoSeeAdditionalLines().remove(holder))
			for(ArmorStandHologram hologram : getAdditionalLines())
				hologram.hide(holder);
	}


	public void afterConnection() {
		ready=true;
		for(Object packet : packets)
			BukkitLoader.getPacketHandler().send(getPlayer(), packet);
		packets=null;
	}


	public void onDisconnect() {
		player.setPlayerListName(null);
		sendPacket(emptyTablistHeaderFooterPacket);
		for(ArmorStandHologram hologram : getAdditionalLines())
			hologram.hideAll();
		additionalLines.clear();
		whoSeeAdditionalLines.clear();
		if (getPlayer().getVehicle() != null)
			NametagManagerAPI.get().watchingEntityMove.remove(getPlayer().getVehicle().getEntityId());
	}


	public Component getPrefix(Display display) {
		if(display==Display.NAMETAG)
			return tagPrefix;
		return tabPrefix;
	}


	public Component getSuffix(Display display) {
		if(display==Display.NAMETAG)
			return tagSuffix;
		return tabSuffix;
	}


	public void setPrefix(Display display, Component value) {
		if(display==Display.NAMETAG){
			if(!areSame(tagPrefix,value)) {
				tagPrefix=value;
				if(getPrimaryTeam()==null)
					changePrimaryTeam(new SimpleTeam(getPlayer().getName(), getPrefix(Display.NAMETAG), getSuffix(Display.NAMETAG), null, ChatColor.WHITE, 0, CollisionRule.ALWAYS, Visibility.ALWAYS).joinPlayer(getPlayer().getName()));
				else{
					SimpleTeam team = getPrimaryTeam();
					team.setPrefix(value);
					Object packet = TeamUtils.createTeamPacket(TeamUtils.METHOD_CHANGE, team.getTeam(), team.getColor(), team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(), team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers());
					for(ClassicTabPlayer holder : TabAPI.getPlayers())
						if(holder.getTeams().contains(team))
							holder.sendPacket(packet);
				}
			}
			return;
		}
		if(!areSame(tabPrefix,value)) {
			tabPrefix=value;
			player.setPlayerListName(getTablistFormat().replace("{prefix}", value==null?"":value.toString()).replace("{player}", getPlayer().getName()).replace("{suffix}", tabSuffix==null?"":tabSuffix.toString()));
		}
	}

	public void setSuffix(Display display, Component value) {
		if(display==Display.NAMETAG){
			if(!areSame(tagPrefix,value)) {
				tagSuffix=value;
				if(getPrimaryTeam()==null)
					changePrimaryTeam(new SimpleTeam(getPlayer().getName(), getPrefix(Display.NAMETAG), getSuffix(Display.NAMETAG), null, ChatColor.WHITE, 0, CollisionRule.ALWAYS, Visibility.ALWAYS).joinPlayer(getPlayer().getName()));
				else{
					SimpleTeam team = getPrimaryTeam();
					team.setSuffix(value);
					Object packet = TeamUtils.createTeamPacket(TeamUtils.METHOD_CHANGE, team.getTeam(), team.getColor(), team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(), team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers());
					for(ClassicTabPlayer holder : TabAPI.getPlayers())
						if(holder.getTeams().contains(team))
							holder.sendPacket(packet);
				}
			}
			return;
		}
		if(!areSame(tabSuffix,value)) {
			tabSuffix=value;
			player.setPlayerListName(getTablistFormat().replace("{prefix}", tabPrefix==null?"":tabPrefix.toString()).replace("{player}", getPlayer().getName()).replace("{suffix}", value==null?"":value.toString()));
		}
	}

	private boolean areSame(Component a, Component b) {
		if(a==null == (b!=null) || a!=null && b !=null && !a.getClass().equals(b.getClass()))return false;
		if(a==null && b == null)return true;
		if(a instanceof ComponentItem) {
			ComponentItem item = (ComponentItem)a;
			ComponentItem secondItem = (ComponentItem)b;
			if(!Objects.equals(item.getId(), secondItem.getId()) || item.getCount()!=secondItem.getCount() || !Objects.equals(item.getNbt(), secondItem.getNbt()))return false;
			return true;
		}
		if(a instanceof ComponentEntity) {
			ComponentEntity item = (ComponentEntity)a;
			ComponentEntity secondItem = (ComponentEntity)b;
			if(!Objects.equals(item.getId(), secondItem.getId()) || !Objects.equals(item.getType(), secondItem.getType()) || !areSame(item.getName(), secondItem.getName()))return false;
			return true;
		}
		if(a.getText()==null ? b.getText()==null : a.getText().equals(b.getText())) {
			if(a.getExtra()==null && b.getExtra()==null || a.getExtra()!=null && b.getExtra()!=null && a.getExtra().size()==b.getExtra().size() || a.getExtra()==null && b.getExtra()!=null && b.getExtra().isEmpty()
					|| b.getExtra()==null && a.getExtra()!=null && a.getExtra().isEmpty()) {
				int pos = 0;
				if(a.getExtra()!=null)
					for(Component extra : a.getExtra())
						if(!areSame(extra,b.getExtra().get(pos++)))
							return false;
			}
			if(!Objects.equals(a.getColor(), b.getColor()) || !Objects.equals(a.getFont(), b.getFont()) || !Objects.equals(a.getInsertion(), b.getInsertion()) ||
					a.isBold()!=b.isBold() || a.isItalic()!=b.isItalic()
					|| a.isObfuscated()!=b.isObfuscated()
					|| a.isStrikethrough()!=b.isStrikethrough()
					|| a.isUnderlined()!=b.isUnderlined())
				return false;
			if(a.getClickEvent()!=null && b.getClickEvent()!=null) {
				if(a.getClickEvent().getAction()!=b.getClickEvent().getAction() || !Objects.equals(a.getClickEvent().getValue(), b.getClickEvent().getValue()))
					return false;
			}else if(a.getClickEvent() == null ? b.getClickEvent() != null : b.getClickEvent() == null)
				return false;
			if(a.getHoverEvent()!=null && b.getHoverEvent()!=null) {
				if(a.getHoverEvent().getAction()!=b.getHoverEvent().getAction() || !areSame(a.getHoverEvent().getValue(), b.getHoverEvent().getValue()))
					return false;
			}else if(a.getHoverEvent() == null ? b.getHoverEvent() != null : b.getHoverEvent() == null)
				return false;
			return true;
		}
		return false;
	}


	public void setHeader(Component value) {
		if(value==null && (this.header==null || this.header.isEmpty()) || !value.isEmpty() && header!=null && areSame(value, header))return;
		this.header=value;
		sendPacket(BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(header, footer));
	}


	public void setFooter(Component value) {
		if(value==null && (this.footer==null || this.footer.isEmpty()) || !value.isEmpty() && footer!=null && areSame(value, footer))return;
		this.footer=value;
		sendPacket(BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(header, footer));
	}

	@Override
	public String toString() {
		return "TabPlayer[name="+player.getName()+",ready="+ready+"]";
	}

}
