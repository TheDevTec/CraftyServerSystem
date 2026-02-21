package me.devtec.craftyserversystem.utils.tablist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI.SimpleTeam;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer.Display;
import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.ArmorStandHologram;
import me.devtec.shared.Ref;
import me.devtec.shared.components.Component;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.MathUtils;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.NmsProvider.Action;
import me.devtec.theapi.bukkit.nms.NmsProvider.DisplayType;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils.Visibility;

public class UserTablistData extends TablistData {
	private Player player;
	private YellowNumberDisplayMode previous;
	private Map<Player, Integer> yellowNumber = new HashMap<>();

	public UserTablistData(Player p) {
		player = p;
	}

	public UserTablistData(Player p, UserTablistData previousData) {
		player = p;
		previous = previousData.previous;
		yellowNumber.putAll(previousData.yellowNumber);
	}

	public UserTablistData process(PlaceholdersExecutor placeholders) {
		for (String placeholder : API.get().getConfigManager().getPlaceholders().getKeys()) {
			String replaced = PlaceholderAPI.apply(API.get().getConfigManager().getPlaceholders().getString(placeholder + ".placeholder"), player.getUniqueId());
			placeholders.add(placeholder,
					API.get().getConfigManager().getPlaceholders()
					.getString(placeholder + ".replace." + replaced, API.get().getConfigManager().getPlaceholders().getString(placeholder + ".replace._DEFAULT", ""))
					.replace("{placeholder}", replaced));
		}
		ClassicTabPlayer nametag = TabAPI.getHolder(player);

		StringContainer header = new StringContainer(64);
		for (String text : getHeader()) {
			if (!header.isEmpty())
				header.append('\n');
			header.append(placeholders.apply(text));
		}
		StringContainer footer = new StringContainer(64);
		for (String text : getFooter()) {
			if (!footer.isEmpty())
				footer.append('\n');
			footer.append(placeholders.apply(text));
		}
		nametag.setHeader(Component.fromString(header.toString(), true, false));
		nametag.setFooter(Component.fromString(footer.toString(), true, false));
		nametag.setTablistFormat(getTabNameFormat());
		nametag.setPrefix(Display.TABLIST, Component.fromString(placeholders.apply(getTabPrefix()), true, false));
		nametag.setSuffix(Display.TABLIST, Component.fromString(placeholders.apply(getTabSuffix()), true, false));
		nametag.setPrefix(Display.NAMETAG, Component.fromString(placeholders.apply(getTagPrefix()), true, false));
		nametag.setSuffix(Display.NAMETAG, Component.fromString(placeholders.apply(getTagSuffix()), true, false));
		int index = 0;
		boolean addingMode = false;
		boolean hidePlayerNickname = false;
		List<ArmorStandHologram> lines = nametag.getAdditionalLines();
		for(String line : getNametagLines()) {
			if(line.contains("{player}"))hidePlayerNickname=true;
			String text = placeholders.apply(line.replace("{prefix}", getTagPrefix()).replace("{suffix}", getTagSuffix()).replace("{player}", player.getName()));
			if(addingMode || lines.size()<=index) {
				//add new!
				Location loc = player.getLocation();
				ArmorStandHologram stand;
				lines.add(stand=new ArmorStandHologram(nametag, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), 0.25*index++, text));
				addingMode=true;
				for(ClassicTabPlayer player : nametag.getWhoSeeAdditionalLines())
					stand.show(player);
			}else {
				ArmorStandHologram lineAtIndex = lines.get(index++);
				lineAtIndex.setText(text);
			}
		}
		if(hidePlayerNickname) {
			SimpleTeam team = nametag.getPrimaryTeam();
			if(team.getNametagVisibility()!=Visibility.NEVER) {
				team.setNametagVisibility(Visibility.NEVER);
				Object packet = TeamUtils.createTeamPacket(TeamUtils.METHOD_CHANGE, team.getTeam(), team.getColor(), team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(), team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers());
				for(ClassicTabPlayer holder : TabAPI.getPlayers())
					if(holder.getTeams().contains(team))
						holder.sendPacket(packet);
					else if(holder.getPlayer().canSee(getPlayer()))
						holder.createTeam(team);
			}
		}
		if(lines.size()>index && !addingMode)
			for(int i = lines.size()-1; i > index; --i)
				lines.remove(i).hideAll();

		if (previous != null && previous != getYellowNumberDisplayMode()) {
			// remove
			if (!yellowNumber.isEmpty())
				BukkitLoader.getPacketHandler().send(yellowNumber.keySet(), BukkitLoader.getNmsProvider().packetScoreboardScore(Action.REMOVE, "yn_ping_css", player.getName(), 0));
			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(0, "yn_ping_css", "", previous == YellowNumberDisplayMode.INTEGER));
		}
		if (previous == null && previous != getYellowNumberDisplayMode() && getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {
			// create
			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(0, "yn_ping_css", player.getName(), getYellowNumberDisplayMode() == YellowNumberDisplayMode.INTEGER));
			Object packet = BukkitLoader.getNmsProvider().packetScoreboardDisplayObjective(0, null);
			Ref.set(packet, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "objectiveName" : "b", "yn_ping_css");
			BukkitLoader.getPacketHandler().send(player, packet);
		}
		if (getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {
			// update
			int updateValue = (int) MathUtils.calculate(placeholders.applyWithoutColors(getYellowNumberText()));
			Collection<Player> requiredUpdate = whoRequireUpdate(player, updateValue);
			if (!requiredUpdate.isEmpty())
				BukkitLoader.getPacketHandler().send(requiredUpdate, BukkitLoader.getNmsProvider().packetScoreboardScore(Action.CHANGE, "yn_ping_css", player.getName(), updateValue));
		}
		previous = getYellowNumberDisplayMode();
		return this;
	}


	private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	public String generateSimpleHash() {
		long timestamp = System.currentTimeMillis();
		long random = StringUtils.random.nextLong();
		long combined = timestamp ^ random;

		// Použijeme jen část čísla, abychom dostali 11 znaků
		return toBase62(Math.abs(combined) & 0x3FFFFFFFFFL, 11);
	}

	private String toBase62(long value, int length) {
		char[] result = new char[length];

		for (int i = length - 1; i >= 0; i--) {
			int remainder = (int)(value % 62);
			result[i] = BASE62.charAt(remainder);
			value = value / 62;
		}

		return new String(result);
	}

	private Collection<Player> whoRequireUpdate(Player target, int value) {
		Collection<Player> list = new ArrayList<>();
		for (Player player : BukkitLoader.getOnlinePlayers()) {
			Integer val;
			if (player.canSee(target) && ((val = yellowNumber.get(player)) == null || val != value)) {
				list.add(player);
				yellowNumber.put(player, value);
			}
		}
		return list;
	}

	private Object createObjectivePacket(int mode, String sbname, String displayName, boolean displayAsInteger) {
		return TeamUtils.createObjectivePacket(mode, sbname, Component.fromString(displayName), Optional.ofNullable(null), displayAsInteger ? DisplayType.INTEGER : DisplayType.HEARTS);
	}

	public void removeTablist() {
		ClassicTabPlayer nametag = TabAPI.removeHolder(player.getUniqueId());
		if(nametag!=null)
			nametag.onDisconnect(); // With cooldown 5 ticks?
		BukkitLoader.getPacketHandler().send(yellowNumber.keySet(), BukkitLoader.getNmsProvider().packetScoreboardScore(Action.REMOVE, "yn_ping_css", player.getName(), 0));
		yellowNumber.clear();
		BukkitLoader.getPacketHandler().send(player, createObjectivePacket(1, "yn_ping_css", "", previous == YellowNumberDisplayMode.INTEGER));
	}

	public Player getPlayer() {
		return player;
	}

}
