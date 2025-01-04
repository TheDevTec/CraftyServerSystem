package me.devtec.craftyserversystem.utils.tablist;

import java.util.HashMap;
import java.util.Map;

public class PerWorldTablistData extends TablistData {
	public final Map<String, TablistData> perGroup = new HashMap<>();
	public final Map<String, TablistData> perPlayer = new HashMap<>();
}
