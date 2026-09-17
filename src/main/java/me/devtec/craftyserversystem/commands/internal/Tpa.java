package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.tprequest.Result;
import me.devtec.craftyserversystem.commands.internal.tprequest.TpaManager;
import me.devtec.craftyserversystem.commands.internal.tprequest.TpaRequest;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.text.TextRenderer;

public class Tpa extends CssCommand {

	@Override
	public void register() {
		if(isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd")).selector(Selector.PLAYER, (sender, structure, args) -> {
			sendRequest(Bukkit.getPlayer(args[0]), true, sender);
		}).argument("-s", (sender, structure, args) -> { // silent
			sendRequest(Bukkit.getPlayer(args[0]), false, sender);
		});

		// register
		List<String> cmds = getCommands();
		if(!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void sendRequest(Player target, boolean sendMessage, Player sender) {
		if(target.equals(sender)) {
			msg(sender, "failed.self");
			return;
		}
		Result result = TpaManager.getProvider()
		        .sendRequest(new TpaRequest(sender.getUniqueId(), target.getUniqueId(), System.currentTimeMillis() / 1000 + API.get().getConfigManager().getTeleportRequestTime(), true));
		switch(result) {
			case SUCCESS :
				TextRenderer placeholders = renderer().placeholder("sender", sender.getName()).placeholder("target", target.getName());
				msg(target, "success.target", placeholders);
				if(sendMessage)
					msg(sender, "success.sender", placeholders);
				break;
			case DENIED_BY_TARGET :
				if(sendMessage)
					msg(sender, "failed.denied_toggled", renderer().placeholder("target", target.getName()));
				break;
			case INVALID :
				if(sendMessage)
					msg(sender, "failed.invalid", renderer().placeholder("target", target.getName()));
				break;
			case FAILED_SENDER :
				if(sendMessage)
					msg(sender, "failed.sender", renderer().placeholder("target", target.getName()));
				break;
			case FAILED_TARGET :
				if(sendMessage)
					msg(sender, "failed.target", renderer().placeholder("target", target.getName()));
				break;
		}
	}

}
