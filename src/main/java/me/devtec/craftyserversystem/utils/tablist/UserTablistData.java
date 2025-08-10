package me.devtec.craftyserversystem.utils.tablist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
import me.devtec.shared.Ref;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.utility.MathUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.nms.NmsProvider.Action;
import me.devtec.theapi.bukkit.nms.NmsProvider.DisplayType;
import me.devtec.theapi.bukkit.nms.utils.TeamUtils;

public class UserTablistData extends TablistData {
	private static final Object emptyTabPacket = BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT);

	private Player player;
	private String previousName;
	private String previousHeader;
	private String previousFooter;
	private YellowNumberDisplayMode previous;
	private Map<Player, Integer> yellowNumber = new HashMap<>();

	public UserTablistData(Player p) {
		player = p;
	}

	public UserTablistData(Player p, UserTablistData previousData) {
		player = p;
		previous = previousData.previous;
		previousHeader = previousData.previousHeader;
		previousFooter = previousData.previousFooter;
		previousName = previousData.previousName;
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
		String header = headerContainer.toString();
		String footer = footerContainer.toString();
		if (!header.equals(previousHeader) || !footer.equals(previousFooter)) {
			previousHeader = header;
			previousFooter = footer;
			Object tabPacket = BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(ComponentAPI.fromString(header), ComponentAPI.fromString(footer));
			BukkitLoader.getPacketHandler().send(player, tabPacket);
		}
		String playerlistName = placeholders.apply(getTabNameFormat().replace("{player}", player.getName()).replace("{prefix}", getTabPrefix()).replace("{suffix}", getTabSuffix()));
		if (!playerlistName.equals(previousName)) {
			previousName = playerlistName;
			player.setPlayerListName(playerlistName);
		}
		NametagPlayer nametag = NametagManagerAPI.get().getPlayer(player);
		nametag.setNametagGenerator(generateFunction(getTagNameFormat(), placeholders));
		nametag.getNametag().setText(nametag.getNametagGenerator().apply(player));

		if (previous != null && previous != getYellowNumberDisplayMode()) {
			// remove
			if (!yellowNumber.isEmpty())
				BukkitLoader.getPacketHandler().send(yellowNumber.keySet(), BukkitLoader.getNmsProvider().packetScoreboardScore(Action.REMOVE, "css_yn", player.getName(), 0));
			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(0, "css_yn", "", previous == YellowNumberDisplayMode.INTEGER));
		}
		if (previous == null && previous != getYellowNumberDisplayMode() && getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {
			// create
			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(0, "css_yn", player.getName(), getYellowNumberDisplayMode() == YellowNumberDisplayMode.INTEGER));
			Object packet = BukkitLoader.getNmsProvider().packetScoreboardDisplayObjective(0, null);
			Ref.set(packet, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "objectiveName" : "b", "css_yn");
			BukkitLoader.getPacketHandler().send(player, packet);
		}
		if (getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {
			// update
			int updateValue = (int) MathUtils.calculate(placeholders.applyWithoutColors(getYellowNumberText()));
			Collection<Player> requiredUpdate = whoRequireUpdate(player, updateValue);
			if (!requiredUpdate.isEmpty())
				BukkitLoader.getPacketHandler().send(requiredUpdate, BukkitLoader.getNmsProvider().packetScoreboardScore(Action.CHANGE, "css_yn", player.getName(), updateValue));
		}
		previous = getYellowNumberDisplayMode();
		return this;
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

	private Function<Player, String> generateFunction(String format, PlaceholdersExecutor placeholders) {
		return player -> placeholders.apply(format.replace("{prefix}", getTagPrefix()).replace("{suffix}", getTagSuffix()).replace("{player}", player.getName()));
	}

	public void removeTablist() {
		BukkitLoader.getPacketHandler().send(player, emptyTabPacket);
		player.setPlayerListName(null);
		NametagPlayer nametag = NametagManagerAPI.get().getPlayer(player);
		nametag.remove();
		BukkitLoader.getPacketHandler().send(yellowNumber.keySet(), BukkitLoader.getNmsProvider().packetScoreboardScore(Action.REMOVE, "css_yn", player.getName(), 0));
		yellowNumber.clear();
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
