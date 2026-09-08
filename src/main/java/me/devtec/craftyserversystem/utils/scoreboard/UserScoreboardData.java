package me.devtec.craftyserversystem.utils.scoreboard;

import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.placeholders.PlaceholderAPI;
import me.devtec.shared.text.TextRenderer;
import me.devtec.theapi.bukkit.scoreboard.ScoreboardAPI;
import me.devtec.theapi.bukkit.scoreboard.SimpleScore;

public class UserScoreboardData extends ScoreboardData {

	private static final SimpleScore score = new SimpleScore();

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

	public void process(TextRenderer renderer) {
		if (hidden)
			return;

		renderer.target(player.getUniqueId()).colorize();

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

		if (updateTitleMode != 0) {
			if (updateTitleMode != 2)
				updateTitleMode = 0;

			score.setTitle(render(getTitle(), renderer));
		}

		for (String line : getLines())
			score.addLine(render(line, renderer));

		score.send(player);
	}

	private String render(String text, TextRenderer renderer) {
		return renderer.render(PlaceholderAPI.apply(text, player.getUniqueId()), player.getUniqueId());
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
		return !group.equals(this.group);
	}
}