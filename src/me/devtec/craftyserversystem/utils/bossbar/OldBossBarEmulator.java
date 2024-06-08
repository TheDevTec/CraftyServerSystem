package me.devtec.craftyserversystem.utils.bossbar;

import org.bukkit.entity.Player;

import me.devtec.theapi.bukkit.bossbar.BossBar;

public class OldBossBarEmulator implements BossBarEmulator {

	private BossBar bar;
	private boolean removed;

	public OldBossBarEmulator(Player player, String text, double progress) {
		bar = new BossBar(player, text, progress);
	}

	@Override
	public String getText() {
		return bar.getTitle();
	}

	@Override
	public void setText(String text) {
		if (!removed && bar.getTitle().equals(text))
			return;
		removed = false;
		bar.setTitle(text);
	}

	@Override
	public double getProgress() {
		return bar.getProgress();
	}

	@Override
	public void setProgress(double progress) {
		progress = Math.min(100.0, Math.max(0.0, progress / 100));
		if (!removed && bar.getProgress() == progress)
			return;
		removed = false;
		bar.setProgress(progress);
	}

	@Override
	public void setStyle(Style style) {
		// Not supported
	}

	@Override
	public void setColor(Color color) {
		// Not supported
	}

	@Override
	public void remove() {
		bar.remove();
		removed = true;
	}

	@Override
	public boolean canSee(Player player) {
		return !removed;
	}

	@Override
	public void addPlayer(Player player) {

	}

}
