package me.devtec.craftyserversystem.utils.scoreboard;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.theapi.bukkit.scoreboard.ScoreboardAPI;
import me.devtec.theapi.bukkit.scoreboard.SimpleScore;

public class UserScoreboardData extends ScoreboardData {
	private static SimpleScore score = new SimpleScore();

	// 1 = only when needed
	// 2 = every time (bcs placeholders in title)
	private byte updateTitleMode = 1;
	private Player player;
	private String group;
	private volatile boolean hidden;

	public UserScoreboardData(Player player, String vaultGroup, boolean hidden) {
		this.player = player;
		group = vaultGroup;
		this.hidden = hidden;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hide) {
		if (hide) {
			hidden = true;
			removeScoreboard();
		} else
			hidden = false;
	}

	public void process(PlaceholdersExecutor placeholders) {
		if (hidden)
			return;
		for (String placeholder : API.get().getConfigManager().getPlaceholders().getKeys()) {
			String replaced = PlaceholderAPI.apply(API.get().getConfigManager().getPlaceholders().getString(placeholder + ".placeholder"), player.getUniqueId());
			placeholders.add(placeholder,
					API.get().getConfigManager().getPlaceholders()
							.getString(placeholder + ".replace." + replaced, API.get().getConfigManager().getPlaceholders().getString(placeholder + ".replace._DEFAULT", ""))
							.replace("{placeholder}", replaced));
		}
		if (updateTitleMode != 0) {
			if (updateTitleMode != 2)
				updateTitleMode = 0;
			score.setTitle(placeholders.apply(getTitle()));
		}
		for (String line : getLines())
			score.addLine(placeholders.apply(line));
		score.send(player);
	}

	public UserScoreboardData markModified() {
		updateTitleMode = (byte) (getTitle().indexOf('{') != -1 || getTitle().indexOf('%') != -1 ? 2 : 1);
		return this;
	}

	public void removeScoreboard() {
		ScoreboardAPI scoreboard = SimpleScore.scores.remove(player.getUniqueId());
		if (scoreboard != null)
			scoreboard.destroy();
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
