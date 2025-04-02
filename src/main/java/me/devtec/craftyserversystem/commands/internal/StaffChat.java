package me.devtec.craftyserversystem.commands.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.devtec.craftyserversystem.Loader;
import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.StringUtils;

public class StaffChat extends CssCommand {

	private boolean canBeToggled;
	private Listener listener;
	private List<UUID> toggled;

	@Override
	public void register() {
		if (isRegistered())
			return;

		canBeToggled = API.get().getConfigManager().getMain().getBoolean("staff-chat.can-be-toggled");

		if (canBeToggled) {
			toggled = new ArrayList<>();
			listener = new Listener() {

				@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
				public void playerChat(AsyncPlayerChatEvent e) {
					if (toggled.contains(e.getPlayer().getUniqueId())) {
						e.setCancelled(true);
						staffChat(e.getPlayer(), e.getMessage());
					}
				}

				@EventHandler
				public void onQuit(PlayerQuitEvent e) {
					toggled.remove(e.getPlayer().getUniqueId());
				}
			};
			Bukkit.getPluginManager().registerEvents(listener, Loader.getPlugin());
		}

		CommandStructure<CommandSender> cmd = CommandStructure
				.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
					if (canBeToggled && sender instanceof Player) {
						if (toggled.remove(((Player) sender).getUniqueId()))
							msg(sender, "toggle.off");
						else {
							toggled.add(((Player) sender).getUniqueId());
							msg(sender, "toggle.on");
						}
						return;
					}
					msgUsage(sender, "cmd");
				}).permission(getPerm("cmd"));
		cmd.argument(null, -1, (sender, structure, args) -> {
			staffChat(sender, StringUtils.buildString(0, args));
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	public void staffChat(CommandSender player, String message) {
		String path = "default";
		if (player instanceof Player) {
			String group = API.get().getPermissionHook().getGroup(player);
			if (API.get().getConfigManager().getMain().existsKey("staff-chat.formats." + group))
				;
		} else
			path = "console";
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getMain(),
				"staff-chat.formats." + path,
				PlaceholdersExecutor.i().add("sender", player.getName()).add("message", message), getPerm("cmd"));
	}

	@Override
	public void unregister() {
		super.unregister();
		if (toggled != null)
			toggled.clear();
		toggled = null;
		if (listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

}
