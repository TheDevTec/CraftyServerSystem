package me.devtec.craftyserversystem.utils.tablist;

import java.util.function.Function;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagManagerAPI;
import me.devtec.craftyserversystem.utils.tablist.nametag.NametagPlayer;
import me.devtec.shared.components.Component;
import me.devtec.shared.components.ComponentAPI;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class UserTablistData extends TablistData {
	private static final Object emptyTabPacket = BukkitLoader.getNmsProvider().packetPlayerListHeaderFooter(Component.EMPTY_COMPONENT, Component.EMPTY_COMPONENT);

	private Player player;

	public UserTablistData(Player player) {
		this.player = player;
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
		return this;
	}

	private Function<Player, String> generateFunction(String format) {
		return player -> ColorUtils
				.colorize(PlaceholderAPI.apply(format.replace("{prefix}", getTagPrefix()).replace("{suffix}", getTagSuffix()).replace("{player}", player.getName()), player.getUniqueId()));
	}

	public void removeTablist() {
		BukkitLoader.getPacketHandler().send(player, emptyTabPacket);
		player.setPlayerListName(null);
		NametagManagerAPI.get().getPlayer(player).remove();
	}

	public Player getPlayer() {
		return player;
	}

}
