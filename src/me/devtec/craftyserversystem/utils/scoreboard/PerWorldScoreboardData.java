package me.devtec.craftyserversystem.utils.scoreboard;

import java.util.HashMap;
import java.util.Map;

public class PerWorldScoreboardData extends ScoreboardData {
	public final Map<String, ScoreboardData> perGroup = new HashMap<>();
	public final Map<String, ScoreboardData> perPlayer = new HashMap<>();
}
