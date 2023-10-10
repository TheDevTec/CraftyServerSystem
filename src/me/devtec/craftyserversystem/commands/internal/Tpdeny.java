package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.commands.internal.tprequest.TpaManager;
import me.devtec.craftyserversystem.commands.internal.tprequest.TpaRequest;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;

public class Tpdeny extends CssCommand {

	@Override
	public String section() {
		return "tpdeny";
	}

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<Player> cmd = CommandStructure.create(Player.class, P_DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			List<TpaRequest> requests = TpaManager.getProvider().getFilteredRequests(sender.getUniqueId(), true);
			if (requests == null) {
				msg(sender, "no-requests", PlaceholdersExecutor.EMPTY);
				return;
			}
			TpaRequest request = requests.get(0);
			TpaManager.getProvider().removeRequest(request);
			Player target = Bukkit.getPlayer(request.getTarget());
			if (target != null) {
				PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msg(sender, "sender", executor);
				msg(target, "target", executor);
			}
		}).permission(getPerm("cmd")).argument("-s", (sender, structure, args) -> { // silent
			List<TpaRequest> requests = TpaManager.getProvider().getFilteredRequests(sender.getUniqueId(), true);
			if (requests == null) {
				msg(sender, "no-requests", PlaceholdersExecutor.EMPTY);
				return;
			}
			TpaRequest request = requests.get(0);
			TpaManager.getProvider().removeRequest(request);
			Player target = Bukkit.getPlayer(request.getTarget());
			if (target != null) {
				PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", target.getName());
				msg(target, "target", executor);
			}
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

}
