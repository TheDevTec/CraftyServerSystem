package me.devtec.craftyserversystem.utils.bossbar;

import java.util.HashMap;
import java.util.Map;

public class PerWorldBossBarData extends BossBarData {
	public final Map<String, BossBarData> perGroup = new HashMap<>();
	public final Map<String, BossBarData> perPlayer = new HashMap<>();
}
