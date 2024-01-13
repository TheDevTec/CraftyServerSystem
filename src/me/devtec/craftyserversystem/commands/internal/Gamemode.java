package me.devtec.craftyserversystem.commands.internal;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.Ref;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Gamemode extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));

		// survival
		CommandStructure<CommandSender> survival = cmd.argument("survival", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "survival");
				return;
			}
			setGameMode((Player) sender, GameMode.SURVIVAL, true, sender);
		}, "s", "0").permission(getPerm("survival"));
		// silent
		survival.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "survival");
				return;
			}
			setGameMode((Player) sender, GameMode.SURVIVAL, false, sender);
		});
		// other
		survival.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setGameMode(player, GameMode.SURVIVAL, true, sender);
		}).permission(getPerm("other.survival"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[1]))
						setGameMode(player, GameMode.SURVIVAL, false, sender);
				});

		// creative
		CommandStructure<CommandSender> creative = cmd.argument("creative", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "creative");
				return;
			}
			setGameMode((Player) sender, GameMode.CREATIVE, true, sender);
		}, "c", "1").permission(getPerm("creative"));
		// silent
		creative.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "creative");
				return;
			}
			setGameMode((Player) sender, GameMode.CREATIVE, false, sender);
		});
		// other
		creative.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setGameMode(player, GameMode.CREATIVE, true, sender);
		}).permission(getPerm("other.creative"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[1]))
						setGameMode(player, GameMode.CREATIVE, false, sender);
				});

		// adventure
		CommandStructure<CommandSender> adventure = cmd.argument("adventure", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "adventure");
				return;
			}
			setGameMode((Player) sender, GameMode.ADVENTURE, true, sender);
		}, "a", "2").permission(getPerm("adventure"));
		// silent
		adventure.argument("-s", (sender, structure, args) -> {
			if (!(sender instanceof Player)) {
				msgUsage(sender, "adventure");
				return;
			}
			setGameMode((Player) sender, GameMode.ADVENTURE, false, sender);
		});
		// other
		adventure.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			for (Player player : selector(sender, args[1]))
				setGameMode(player, GameMode.ADVENTURE, true, sender);
		}).permission(getPerm("other.adventure"))
				// silent
				.argument("-s", (sender, structure, args) -> {
					for (Player player : selector(sender, args[1]))
						setGameMode(player, GameMode.ADVENTURE, false, sender);
				});

		if (Ref.isNewerThan(7)) { // 1.8+
			// spectator
			CommandStructure<CommandSender> spectator = cmd.argument("spectator", (sender, structure, args) -> {
				if (!(sender instanceof Player)) {
					msgUsage(sender, "spectator");
					return;
				}
				setGameMode((Player) sender, GameMode.SPECTATOR, true, sender);
			}, "sp", "3").permission(getPerm("spectator"));
			// silent
			spectator.argument("-s", (sender, structure, args) -> {
				if (!(sender instanceof Player)) {
					msgUsage(sender, "spectator");
					return;
				}
				setGameMode((Player) sender, GameMode.SPECTATOR, false, sender);
			});
			// other
			spectator.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
				for (Player player : selector(sender, args[1]))
					setGameMode(player, GameMode.SPECTATOR, true, sender);
			}).permission(getPerm("other.spectator"))
					// silent
					.argument("-s", (sender, structure, args) -> {
						for (Player player : selector(sender, args[1]))
							setGameMode(player, GameMode.SPECTATOR, false, sender);
					});
		}

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void setGameMode(Player target, GameMode mode, boolean sendMessage, CommandSender sender) {
		if (sendMessage)
			if (target.equals(sender)) {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("gamemode", mode.name().toLowerCase());
				msg(sender, "self", placeholders);
			} else {
				PlaceholdersExecutor placeholders = PlaceholdersExecutor.i().add("target", target.getName()).add("sender", sender.getName()).add("gamemode", mode.name().toLowerCase());
				msg(target, "other.target", placeholders);
				msg(sender, "other.sender", placeholders);
			}
		// You can change gamemode only in primary thread
		if (!Bukkit.isPrimaryThread())
			BukkitLoader.getNmsProvider().postToMainThread(() -> target.setGameMode(mode));
		else
			target.setGameMode(mode);
	}

}
