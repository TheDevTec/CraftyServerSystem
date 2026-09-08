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
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.TabAPI.SimpleTeam;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer;
import me.devtec.craftyserversystem.utils.tablist.nametag.classic.ClassicTabPlayer.Display;
import me.devtec.craftyserversystem.utils.tablist.nametag.hologram.ArmorStandHologram;
import me.devtec.shared.Ref;
import me.devtec.shared.components.base.Component;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.utility.MathUtils;
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

	public UserTablistData process(TextRenderer renderer) {
		renderer.target(player.getUniqueId());

		for (String placeholder : API.get().getConfigManager().getPlaceholders().getKeys()) {
			String replaced = PlaceholderAPI.apply(
					API.get().getConfigManager().getPlaceholders().getString(placeholder + ".placeholder"),
					player.getUniqueId());

			renderer.placeholder(placeholder,
					API.get().getConfigManager().getPlaceholders()
							.getString(placeholder + ".replace." + replaced,
									API.get().getConfigManager().getPlaceholders()
											.getString(placeholder + ".replace._DEFAULT", ""))
							.replace("{placeholder}", replaced));
		}

		ClassicTabPlayer nametag = TabAPI.getHolder(player);

		StringContainer header = new StringContainer(64);
		int linePos = 0;

		for (String text : getHeader()) {
			if (linePos++ != 0)
				header.append('\n');

			header.append(render(text, renderer));
		}

		StringContainer footer = new StringContainer(64);
		linePos = 0;

		for (String text : getFooter()) {
			if (linePos++ != 0)
				footer.append('\n');

			footer.append(render(text, renderer));
		}

		nametag.setHeader(Component.fromString(header.toString(), true, false));

		nametag.setFooter(Component.fromString(footer.toString(), true, false));

		nametag.setTablistFormat(getTabNameFormat());

		nametag.setPrefix(Display.TABLIST, Component.fromString(render(getTabPrefix(), renderer), true, false));

		nametag.setSuffix(Display.TABLIST, Component.fromString(render(getTabSuffix(), renderer), true, false));

		nametag.setPrefix(Display.NAMETAG, Component.fromString(render(getTagPrefix(), renderer), true, false));

		nametag.setSuffix(Display.NAMETAG, Component.fromString(render(getTagSuffix(), renderer), true, false));

		int index = 0;
		boolean addingMode = false;
		boolean hidePlayerNickname = false;

		List<ArmorStandHologram> lines = nametag.getAdditionalLines();

		for (String line : getNametagLines()) {
			if (line.contains("{player}"))
				hidePlayerNickname = true;

			String text = render(line.replace("{prefix}", getTagPrefix()).replace("{suffix}", getTagSuffix())
					.replace("{player}", player.getName()), renderer);

			if (addingMode || lines.size() <= index) {
				Location loc = player.getLocation();

				ArmorStandHologram stand;

				lines.add(stand = new ArmorStandHologram(nametag, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(),
						0.25 * index++, text));

				addingMode = true;

				for (ClassicTabPlayer viewer : nametag.getWhoSeeAdditionalLines())
					stand.show(viewer);
			} else {
				ArmorStandHologram lineAtIndex = lines.get(index++);
				lineAtIndex.setText(text);
			}
		}

		if (hidePlayerNickname) {
			SimpleTeam team = nametag.getPrimaryTeam();

			if (team.getNametagVisibility() != Visibility.NEVER) {
				team.setNametagVisibility(Visibility.NEVER);

				Object packet = TeamUtils.createTeamPacket(TeamUtils.METHOD_CHANGE, team.getTeam(), team.getColor(),
						team.getPrefix(), team.getSuffix(), team.getDisplayName(), team.getNametagVisibility(),
						team.getCollisionRule(), team.getFriendlyFlags(), team.getPlayers());

				for (ClassicTabPlayer holder : TabAPI.getPlayers())
					if (holder.getTeams().contains(team))
						holder.sendPacket(packet);
					else if (holder.getPlayer().equals(getPlayer()) || holder.getPlayer().canSee(getPlayer()))
						holder.createTeam(team);
			}
		}

		if (lines.size() > index && !addingMode)
			for (int i = lines.size() - 1; i > index; --i)
				lines.remove(i).hideAll();

		if (previous != null && previous != getYellowNumberDisplayMode()) {

			if (!yellowNumber.isEmpty())
				BukkitLoader.getPacketHandler().send(yellowNumber.keySet(), BukkitLoader.getNmsProvider()
						.packetScoreboardScore(Action.REMOVE, "yn_ping_css", player.getName(), 0));

			BukkitLoader.getPacketHandler().send(player,
					createObjectivePacket(0, "yn_ping_css", "", previous == YellowNumberDisplayMode.INTEGER));
		}

		if (previous == null && getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {

			BukkitLoader.getPacketHandler().send(player, createObjectivePacket(0, "yn_ping_css", player.getName(),
					getYellowNumberDisplayMode() == YellowNumberDisplayMode.INTEGER));

			Object packet = BukkitLoader.getNmsProvider().packetScoreboardDisplayObjective(0, null);

			Ref.set(packet, BukkitLoader.NO_OBFUSCATED_NMS_MODE ? "objectiveName" : "b", "yn_ping_css");

			BukkitLoader.getPacketHandler().send(player, packet);
		}

		if (getYellowNumberDisplayMode() != YellowNumberDisplayMode.NONE) {
			int updateValue = (int) MathUtils.calculate(renderPlain(getYellowNumberText(), renderer));

			Collection<Player> requiredUpdate = whoRequireUpdate(player, updateValue);

			if (!requiredUpdate.isEmpty())
				BukkitLoader.getPacketHandler().send(requiredUpdate, BukkitLoader.getNmsProvider()
						.packetScoreboardScore(Action.CHANGE, "yn_ping_css", player.getName(), updateValue));
		}

		previous = getYellowNumberDisplayMode();

		return this;
	}

	private String render(String text, TextRenderer renderer) {
		return renderer.render(PlaceholderAPI.apply(text, player.getUniqueId()), player.getUniqueId());
	}

	private String renderPlain(String text, TextRenderer renderer) {
		return renderer.renderPlain(PlaceholderAPI.apply(text, player.getUniqueId()), player.getUniqueId());
	}

	private Collection<Player> whoRequireUpdate(Player target, int value) {
		Collection<Player> list = new ArrayList<>();

		for (Player player : BukkitLoader.getOnlinePlayers()) {
			Integer previousValue = yellowNumber.get(player);

			if (player.canSee(target) && (previousValue == null || previousValue != value)) {

				list.add(player);
				yellowNumber.put(player, value);
			}
		}

		return list;
	}

	private Object createObjectivePacket(int mode, String scoreboardName, String displayName,
			boolean displayAsInteger) {

		return TeamUtils.createObjectivePacket(mode, scoreboardName, Component.fromString(displayName),
				Optional.empty(), displayAsInteger ? DisplayType.INTEGER : DisplayType.HEARTS);
	}

	public void removeTablist() {
		BukkitLoader.getPacketHandler().send(yellowNumber.keySet(),
				BukkitLoader.getNmsProvider().packetScoreboardScore(Action.REMOVE, "yn_ping_css", player.getName(), 0));

		yellowNumber.clear();

		BukkitLoader.getPacketHandler().send(player,
				createObjectivePacket(1, "yn_ping_css", "", previous == YellowNumberDisplayMode.INTEGER));

		ClassicTabPlayer nametag = TabAPI.removeHolder(player.getUniqueId());

		if (nametag != null)
			nametag.onDisconnect();
	}

	public Player getPlayer() {
		return player;
	}
}