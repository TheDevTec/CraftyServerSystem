package me.devtec.craftyserversystem.utils.bossbar;

import org.bukkit.entity.Player;

import me.devtec.shared.Ref;

public interface BossBarEmulator {
	String getText();

	void setText(String text);

	double getProgress();

	void setProgress(double progress);

	void setStyle(Style style);

	void setColor(Color color);

	public enum Color {
		PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
	}

	public enum Style {
		SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
	}

	static BossBarEmulator createInstance(Player player, String text, double progress) {
		if (Ref.isOlderThan(9))
			return new OldBossBarEmulator(player, text, progress);
		return new ModernBossBarEmulator(player, text, progress);
	}

	void remove();

	boolean canSee(Player player);

	void addPlayer(Player player);
}
