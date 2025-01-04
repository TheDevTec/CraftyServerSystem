package me.devtec.craftyserversystem.utils.scoreboard;

import java.util.List;

public class ScoreboardData {

	private String title;
	private List<String> lines;

	@Override
	public boolean equals(Object object) {
		if (object instanceof ScoreboardData)
			return ((ScoreboardData) object).title.equals(title) && ((ScoreboardData) object).lines.equals(lines);
		return false;
	}

	public String getTitle() {
		return title;
	}

	public ScoreboardData setTitle(String title) {
		this.title = title;
		return this;
	}

	public List<String> getLines() {
		return lines;
	}

	public ScoreboardData setLines(List<String> lines) {
		this.lines = lines;
		return this;
	}

	public boolean isComplete() {
		return title != null && lines != null;
	}

	@Override
	public ScoreboardData clone() {
		return new ScoreboardData().setTitle(title).setLines(lines);
	}

	public ScoreboardData fillMissing(ScoreboardData additional) {
		if (title == null)
			title = additional.title;
		if (lines == null)
			lines = additional.lines;
		return this;
	}
}
