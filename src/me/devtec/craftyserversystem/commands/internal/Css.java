package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.command.CommandSender;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;

public class Css extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// reload
		cmd.argument("reload", (sender, structure, args) -> {
			long millisStart = System.currentTimeMillis();
			API.get().reload();
			long millisEnd = System.currentTimeMillis();
			long time = millisEnd - millisStart;
			msg(sender, "reload", PlaceholdersExecutor.i().add("time", time));
		});
		// version
		cmd.argument("version", (sender, structure, args) -> {
			msg(sender, "version", PlaceholdersExecutor.i().add("version", Loader.getPlugin().getDescription().getVersion()).add("version", Loader.getPlugin().getDescription().getVersion())
					.add("authors", StringUtils.join(Loader.getPlugin().getDescription().getAuthors(), ", ")));
		}, "ver");

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
