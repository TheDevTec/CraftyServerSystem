package me.devtec.craftyserversystem.utils.bossbar;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.text.TextRenderer;
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

	@Override
	public BossBarData setText(String text) {
		try {
			return super.setText(text);
		} finally {
			markModified();
		}
	}

	public BossBarEmulator getBossBar() {
		return bossbar;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hide) {
		if(hide) {
			hidden = true;
			removeBossBar();
		} else
			hidden = false;
	}

	public UserBossBarData process(TextRenderer renderer) {
		if(hidden)
			return this;

		BossBarEmulator bar = bossbar;

		if(getText() == null || getText().isEmpty()) {
			if(bar == null)
				return this;
			bar.remove();
			bar = null;
			return this;
		}

		renderer.target(player.getUniqueId()).colorize();

		for(String placeholder : API.get().getConfigManager().getPlaceholders().getKeys()) {
			String replaced = PlaceholderAPI.apply(API.get().getConfigManager().getPlaceholders().getString(placeholder + ".placeholder"), player.getUniqueId());

			renderer.placeholder(placeholder,
			        API.get().getConfigManager().getPlaceholders()
			                .getString(placeholder + ".replace." + replaced, API.get().getConfigManager().getPlaceholders().getString(placeholder + ".replace._DEFAULT", ""))
			                .replace("{placeholder}", replaced));
		}

		if(bar == null)
			bar = bossbar = BossBarEmulator.createInstance(player, render(getText(), renderer), MathUtils.calculate(renderPlain(getProgress(), renderer)));
		else {
			if(!bar.canSee(player))
				bar.addPlayer(player);
			if(updateTitleMode != 0) {
				bar.setText(render(getText(), renderer));
				if(updateTitleMode != 2)
					updateTitleMode = 0;
			}

			bar.setProgress(MathUtils.calculate(renderPlain(getProgress(), renderer)));
		}

		if(getStyle() != null)
			bar.setStyle(getStyle());

		if(getColor() != null)
			bar.setColor(getColor());

		return this;
	}

	private String render(String text, TextRenderer renderer) {
		return renderer.render(PlaceholderAPI.apply(text, player.getUniqueId()), player.getUniqueId());
	}

	private String renderPlain(String text, TextRenderer renderer) {
		return renderer.renderPlain(PlaceholderAPI.apply(text, player.getUniqueId()), player.getUniqueId());
	}

	public UserBossBarData markModified() {
		updateTitleMode = (byte) (getText().indexOf('{') != -1 || getText().indexOf('%') != -1 ? 2 : 1);
		return this;
	}

	public void removeBossBar() {
		if(bossbar != null)
			bossbar.remove();
	}

	public Player getPlayer() {
		return player;
	}

	public boolean shouldUpdateData(String group) {
		return !group.equals(this.group);
	}
}
