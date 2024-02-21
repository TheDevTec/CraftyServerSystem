package me.devtec.craftyserversystem.menubuilder;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.craftyserversystem.menubuilder.ItemBuilder.ButtonType;
import me.devtec.shared.annotations.Comment;
import me.devtec.shared.utility.ColorUtils;
import me.devtec.theapi.bukkit.BukkitLoader;
import me.devtec.theapi.bukkit.game.ItemMaker;
import me.devtec.theapi.bukkit.game.ItemMaker.HeadItemMaker;

public interface ItemResult {
	public ItemMaker getItem();

	public List<String> getCommands();

	public List<String> getMessages();

	public CooldownHolder getCooldown();

	public default void onClick(Player player) {
		for (String message : getMessages())
			player.sendMessage(ColorUtils.colorize(message.replace("{player}", player.getName())));
		if (!getCommands().isEmpty())
			BukkitLoader.getNmsProvider().postToMainThread(() -> {
				for (String command : getCommands())
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
			});
		if (getCooldown() != null)
			getCooldown().accept(player);
	}

	@Comment(comment = "This ItemResult cannot be cached")
	public default boolean containPlaceholders() {
		int firstAt;
		if (getItem().getDisplayName() != null)
			if (getItem().getDisplayName().indexOf('{') != -1 && getItem().getDisplayName().indexOf('}') != -1
					|| (firstAt = getItem().getDisplayName().indexOf('%')) != -1 && getItem().getDisplayName().indexOf('%', firstAt) != -1)
				return true;
		if (getItem().getLore() != null)
			for (String line : getItem().getLore())
				if (line.indexOf('{') != -1 && line.indexOf('}') != -1 || (firstAt = line.indexOf('%')) != -1 && line.indexOf('%', firstAt) != -1)
					return true;
		if (getItem() instanceof HeadItemMaker) {
			HeadItemMaker maker = (HeadItemMaker) getItem();
			if (maker.getHeadOwner() != null)
				if (maker.getHeadOwner().indexOf('{') != -1 && maker.getHeadOwner().indexOf('}') != -1
						|| (firstAt = maker.getHeadOwner().indexOf('%')) != -1 && maker.getHeadOwner().indexOf('%', firstAt) != -1)
					return true;
		}
		return false;
	}

	public ButtonType getButtonType();

	public default String getOpenMenuId() {
		return null;
	}

	public boolean updateItemAfterUse();

	public boolean updateGuiAfterUse();

	public static ItemResult of(ItemMaker maker, List<String> commands, List<String> messages, CooldownHolder cooldown, ButtonType type, String openMenuId, boolean updateItem,
			boolean updateWholeGui) {
		return new ItemResult() {
			boolean containPlaceholders = ItemResult.super.containPlaceholders();

			@Override
			public List<String> getMessages() {
				return messages;
			}

			@Override
			public ItemMaker getItem() {
				return maker;
			}

			@Override
			public CooldownHolder getCooldown() {
				return cooldown;
			}

			@Override
			public List<String> getCommands() {
				return commands;
			}

			@Override
			public boolean containPlaceholders() {
				return containPlaceholders;
			}

			@Override
			public ButtonType getButtonType() {
				return type;
			}

			@Override
			public String getOpenMenuId() {
				return openMenuId;
			}

			@Override
			public boolean updateItemAfterUse() {
				return updateItem;
			}

			@Override
			public boolean updateGuiAfterUse() {
				return updateWholeGui;
			}
		};
	}
}
