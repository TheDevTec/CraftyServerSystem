package me.devtec.craftyserversystem.utils.bossbar;

import java.util.Objects;

import me.devtec.craftyserversystem.utils.bossbar.BossBarEmulator.Color;
import me.devtec.craftyserversystem.utils.bossbar.BossBarEmulator.Style;

public class BossBarData {

	private String text;
	private String progress;
	private Color color;
	private Style style;

	@Override
	public boolean equals(Object object) {
		if (object instanceof BossBarData)
			return ((BossBarData) object).text.equals(text) && ((BossBarData) object).progress.equals(progress) && Objects.equals(((BossBarData) object).color, color)
					&& Objects.equals(((BossBarData) object).style, style);
		return false;
	}

	public String getText() {
		return text;
	}

	public BossBarData setText(String text) {
		this.text = text;
		return this;
	}

	public String getProgress() {
		return progress;
	}

	public BossBarData setProgress(String progress) {
		this.progress = progress;
		return this;
	}

	public boolean isComplete() {
		return text != null && progress != null;
	}

	@Override
	public BossBarData clone() {
		return new BossBarData().setText(text).setProgress(progress).setColor(color).setStyle(style);
	}

	public BossBarData setColor(Color color) {
		this.color = color;
		return this;
	}

	public Color getColor() {
		return color;
	}

	public BossBarData setStyle(Style style) {
		this.style = style;
		return this;
	}

	public Style getStyle() {
		return style;
	}

	public BossBarData fillMissing(BossBarData additional) {
		if (text == null)
			text = additional.text;
		if (progress == null)
			progress = additional.progress;
		if (color == null)
			color = additional.color;
		if (style == null)
			style = additional.style;
		return this;
	}
}
