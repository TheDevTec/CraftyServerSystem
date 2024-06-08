package me.devtec.craftyserversystem.utils.bossbar;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.utility.MathUtils;

public class UserBossBarData extends BossBarData {

	// 1 = only when needed
	// 2 = every time (bcs placeholders)
	private byte updateTitleMode = 1;
	private Player player;
	private String group;
	private volatile boolean hidden;
	private BossBarEmulator bossbar;

	public UserBossBarData(Player player, String vaultGroup, boolean hidden, BossBarEmulator bossbar) {
		this.player = player;
		group = vaultGroup;
		this.hidden = hidden;
		this.bossbar = bossbar;
	}

	public BossBarEmulator getBossBar() {
		return bossbar;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hide) {
		if (hide) {
			hidden = true;
			removeBossBar();
		} else
			hidden = false;
	}

	public UserBossBarData process(PlaceholdersExecutor placeholders) {
		if (hidden)
			return this;
		for (String placeholder : API.get().getConfigManager().getPlaceholders().getKeys()) {
			String replaced = PlaceholderAPI.apply(API.get().getConfigManager().getPlaceholders().getString(placeholder + ".placeholder"), player.getUniqueId());
			placeholders.add(placeholder,
					API.get().getConfigManager().getPlaceholders()
							.getString(placeholder + ".replace." + replaced, API.get().getConfigManager().getPlaceholders().getString(placeholder + ".replace._DEFAULT", ""))
							.replace("{placeholder}", replaced));
		}
		BossBarEmulator bar = bossbar;
		if (bar == null)
			bar = bossbar = BossBarEmulator.createInstance(player, getText(), MathUtils.calculate(placeholders.apply(getProgress())));
		else {
			if (!bar.canSee(player))
				bar.addPlayer(player);
			if (updateTitleMode != 0) {
				if (updateTitleMode != 2)
					updateTitleMode = 0;
				bar.setText(placeholders.apply(getText()));
			}
			bar.setProgress(MathUtils.calculate(placeholders.apply(getProgress())));
		}
		if (getStyle() != null)
			bar.setStyle(getStyle());
		if (getColor() != null)
			bar.setColor(getColor());
		return this;
	}

	public UserBossBarData markModified() {
		updateTitleMode = (byte) (getText().indexOf('{') != -1 || getText().indexOf('%') != -1 ? 2 : 1);
		return this;
	}

	public void removeBossBar() {
		bossbar.remove();
	}

	public Player getPlayer() {
		return player;
	}

	public boolean shouldUpdateData(String group) {
		if (group.equals(this.group))
			return false;
		return true;
	}

}
