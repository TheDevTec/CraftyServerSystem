package me.devtec.craftyserversystem.utils.tablist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
import me.devtec.shared.Ref;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.shared.utility.MathUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.NmsProvider.Action;
import me.devtec.theapi.bukkit.nms.NmsProvider.DisplayType;

public class UserTablistData extends TablistData {
	private static final Object emptyTabPacket = BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT);

	private Player player;
	private YellowNumberDisplayMode previous;

	public UserTablistData(Player player) {
		this.player = player;
	}

	public UserTablistData(Player player, UserTablistData previousData) {
		this.player = player;
		previous = previousData.previous;
	}

	public UserTablistData process(PlaceholdersExecutor placeholders) {
		StringContainer headerContainer = new StringContainer(64);
		for (String text : getHeader()) {
			if (!headerContainer.isEmpty())
				headerContainer.append('\n');
			headerContainer.append(placeholders.apply(text));
		}
		StringContainer footerContainer = new StringContainer(64);
		for (String text : getFooter()) {
			if (!footerContainer.isEmpty())
				footerContainer.append('\n');
			footerContainer.append(placeholders.apply(text));
		}
		Object tabPacket = BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(ComponentAPI.fromString(headerContainer.toString()), ComponentAPI.fromString(footerContainer.toString()));
		BukkitLoader.getPacketHandler().send(player, tabPacket);
		player.setPlayerListName(placeholders.apply(getTabNameFormat().replace("{player}", player.getName()).replace("{prefix}", getTabPrefix()).replace("{suffix}", getTabSuffix())));
		NametagPlayer nametag = NametagManagerAPI.get().getPlayer(player);
		nametag.setNametagGenerator(generateFunction(getTagNameFormat()));
		nametag.getNametag().setText(nametag.getNametagGenerator().apply(player));

		Collection<Player> whoCanSee = null;
		if (previous != null && previous != getYellowNumberDisplayMode()) {
			BukkitLoader.getPacketHandler().send(whoCanSee = whoCanSee(player), BukkitLoader.getNmsProvider().packetScoreboardScore(Action.REMOVE, "css_yn", player.getName(), 0));
			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(1, "css_yn", "", previous == YellowNumberDisplayMode.INTEGER));
			// remove
		}
		if (previous == null && previous != getYellowNumberDisplayMode() && getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {
			// create
			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(0, "css_yn", player.getName(), getYellowNumberDisplayMode() == YellowNumberDisplayMode.INTEGER));
			Object packet = BukkitLoader.getNmsProvider().packetScoreboardDisplayObjective(0, null);
			Ref.set(packet, "b", "css_yn");
			BukkitLoader.getPacketHandler().send(player, packet);
		}
		if (getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE)
			// update
			BukkitLoader.getPacketHandler().send(whoCanSee == null ? whoCanSee(player) : whoCanSee,
					BukkitLoader.getNmsProvider().packetScoreboardScore(Action.CHANGE, "css_yn", player.getName(), (int) MathUtils.calculate(placeholders.applyWithoutColors(getYellowNumberText()))));
		previous = getYellowNumberDisplayMode();
		return this;
	}

	private Collection<Player> whoCanSee(Player target) {
		Collection<Player> list = new ArrayList<>();
		for (Player player : BukkitLoader.getOnlinePlayers())
			if (player.canSee(target))
				list.add(player);
		return list;
	}

	private Object createObjectivePacket(int mode, String sbname, String displayName, boolean displayAsInteger) {
		Object packet = BukkitLoader.getNmsProvider().packetScoreboardObjective();
		if (Ref.isNewerThan(16)) {
			Ref.set(packet, "d", sbname);
			Ref.set(packet, "e", BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(displayName)));
			Ref.set(packet, "f", BukkitLoader.getNmsProvider().getEnumScoreboardHealthDisplay(displayAsInteger ? DisplayType.INTEGER : DisplayType.HEARTS));
			Ref.set(packet, "g", mode);
		} else {
			Ref.set(packet, "a", sbname);
			Ref.set(packet, "b", Ref.isNewerThan(12) ? BukkitLoader.getNmsProvider().toIChatBaseComponent(ComponentAPI.fromString(displayName)) : displayName);
			if (Ref.isNewerThan(7)) {
				Ref.set(packet, "c", BukkitLoader.getNmsProvider().getEnumScoreboardHealthDisplay(displayAsInteger ? DisplayType.INTEGER : DisplayType.HEARTS));
				Ref.set(packet, "d", mode);
			} else
				Ref.set(packet, "c", mode);
		}
		return packet;
	}

	private Function<Player, String> generateFunction(String format) {
		return player -> ColorUtils
				.colorize(PlaceholderAPI.apply(format.replace("{prefix}", getTagPrefix()).replace("{suffix}", getTagSuffix()).replace("{player}", player.getName()), player.getUniqueId()));
	}

	public void removeTablist() {
		BukkitLoader.getPacketHandler().send(player, emptyTabPacket);
		player.setPlayerListName(null);
		NametagPlayer nametag = NametagManagerAPI.get().getPlayer(player);
		nametag.remove();
		BukkitLoader.getPacketHandler().send(player, createObjectivePacket(1, "css_yn", "", previous == YellowNumberDisplayMode.INTEGER));
		if (NametagManagerAPI.get().getPlayers().size() == 1)
			NametagManagerAPI.get().getPlayers().remove(nametag);
		else
			new Tasker() {

				@Override
				public void run() {
					NametagManagerAPI.get().getPlayers().remove(nametag);
				}
			}.runLater(5);
	}

	public Player getPlayer() {
		return player;
	}

}
