package me.devtec.craftyserversystem.utils.tablist;

import org.bukkit.entity.Player;

import me.devtec.shared.placeholders.PlaceholderAPI;

public class ConditionTablistData extends TablistData {
	private String placeholder;
	private String requestValue;

	public String getPlaceholder() {
		return placeholder;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public String getRequestValue() {
		return requestValue;
	}

	public void setRequestValue(String requestValue) {
		this.requestValue = requestValue;
	}

	public boolean canBeApplied(Player player) {
		return requestValue.equalsIgnoreCase(PlaceholderAPI.apply(placeholder, player.getUniqueId()));
	}
}
