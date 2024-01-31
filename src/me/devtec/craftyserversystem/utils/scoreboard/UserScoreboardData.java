package me.devtec.craftyserversystem.utils.scoreboard;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
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
		if (updateTitleMode != 0) {
			if (updateTitleMode != 2)
				updateTitleMode = 0;
			score.setTitle(placeholders.applyWithoutColors(getTitle()));
		}
		for (String line : getLines())
			score.addLine(placeholders.applyWithoutColors(line));
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
