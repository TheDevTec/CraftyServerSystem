package me.devtec.craftyserversystem.menubuilder;

import me.devtec.shared.dataholder.Config;

public class DefaultMenuBuilder extends MenuBuilder {

	public DefaultMenuBuilder(String id, Config config) {
		super(id);
		buildClassic(config);
	}

	@Override
	public ItemBuilder loadItem(char character, String path, Config config) {
		return ItemBuilder.build(character, path, config);
	}
}