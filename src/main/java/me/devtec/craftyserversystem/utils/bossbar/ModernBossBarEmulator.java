package me.devtec.craftyserversystem.utils.bossbar;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class ModernBossBarEmulator implements BossBarEmulator {

	private BossBar bar;

	public ModernBossBarEmulator(Player player, String text, double progress) {
		bar = Bukkit.createBossBar(text, BarColor.PURPLE, BarStyle.SOLID);
		setProgress(progress);
		bar.addPlayer(player);
	}

	@Override
	public String getText() {
		return bar.getTitle();
	}

	@Override
	public void setText(String text) {
		if (bar.getTitle().equals(text))
			return;
		bar.setTitle(text);
	}

	@Override
	public double getProgress() {
		return bar.getProgress();
	}

	@Override
	public void setProgress(double progress) {
		progress = Math.min(1.0, Math.max(0.0, progress / 100));
		if (bar.getProgress() == progress)
			return;
		bar.setProgress(progress);
	}

	@Override
	public void setStyle(Style style) {
		bar.setStyle(BarStyle.valueOf(style.name()));
	}

	@Override
	public void setColor(Color color) {
		bar.setColor(BarColor.valueOf(color.name()));
	}

	@Override
	public void remove() {
		bar.removeAll();
	}

	@Override
	public boolean canSee(Player player) {
		return bar.getPlayers().contains(player);
	}

	@Override
	public void addPlayer(Player player) {
		bar.addPlayer(player);
	}

}
