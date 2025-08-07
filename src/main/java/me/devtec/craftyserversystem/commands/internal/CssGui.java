package me.devtec.craftyserversystem.commands.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.dataholder.Config;
import me.devtec.theapi.bukkit.gui.expansion.GuiCreator;
import me.devtec.theapi.bukkit.gui.expansion.guis.AnvilGuiCreator;
import me.devtec.theapi.bukkit.gui.expansion.guis.ClassicGuiCreator;
import me.devtec.theapi.bukkit.gui.expansion.guis.LoopGuiCreator;

public class CssGui extends CssCommand {

	public static List<String> guisByCss = new ArrayList<>();

	@Override
	public void reload() {
		for(String id : guisByCss)
			GuiCreator.guis.remove(id);
		guisByCss.clear();
		loadGuis(new File("plugins/CraftyServerSystem/guis"), "");
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		loadGuis(new File("plugins/CraftyServerSystem/guis"), "");

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));
		// menu
		cmd.callableArgument((sender, structure, args) -> {
			List<String> guis = new ArrayList<>();
			for (String gui : GuiCreator.guis.keySet())
				if (sender.hasPermission(getPerm("per-id").replace("{id}", gui.toLowerCase())) || sender.hasPermission(getPerm("per-id-other").replace("{id}", gui.toLowerCase())))
					guis.add(gui);
			return guis;
		}, (sender, structure, args) -> {
			if (sender instanceof Player)
				openMenu(sender, (Player) sender, args[0], true);
			else
				msgUsage(sender, "cmd");
		})
		// silent
		.argument("-s", (sender, structure, args) -> {
			if (sender instanceof Player)
				openMenu(sender, (Player) sender, args[0], false);
			else
				msgUsage(sender, "cmd");
		})
		// other
		.parent().selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				openMenu(sender, player, args[0], true);
		}).permission(getPerm("other"))
		// silent
		.argument("-s", (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				openMenu(sender, player, args[0], false);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void loadGuis(File folder, String prefix) {
		for (File file : folder.listFiles())
			if (file.isDirectory())
				loadGuis(file, prefix.isEmpty() ? file.getName() : prefix + "/" + file.getName());
			else if (file.getName().endsWith(".yml")) {
				Config config = new Config(file);
				String fileName = file.getName().substring(0, file.getName().length() - 4);
				String name = prefix.isEmpty() ? fileName : prefix + "/" + fileName;
				guisByCss.add(name);
				if("anvil".equalsIgnoreCase(config.getString("type", "NORMAL"))) new AnvilGuiCreator(name,config);
				else if(config.exists("loop")) new LoopGuiCreator(name,config);
				else new ClassicGuiCreator(name,config);
			}
	}

	public void openMenu(CommandSender sender, Player target, String id, boolean sendMessage) {
		GuiCreator.guis.get(id).open(target);
		if (sendMessage)
			if (!sender.equals(target)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName()).add("id", id);
				msg(target, "other.target", placeholders);
				msg(sender, "other.sender", placeholders);
			} else
				msg(target, "self", PlaceholdersExecutor.i().add("target", target.getName()).add("id", id));
	}

}
