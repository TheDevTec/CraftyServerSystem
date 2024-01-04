package me.devtec.craftyserversystem.commands.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.commands.CssCommand;
import me.devtec.craftyserversystem.placeholders.PlaceholdersExecutor;
import me.devtec.shared.commands.selectors.Selector;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.utility.MathUtils;
import me.devtec.shared.utility.ParseUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public class Tp extends CssCommand {

	@Override
	public void register() {
		if (isRegistered())
			return;

		CommandStructure<CommandSender> cmd = CommandStructure.create(CommandSender.class, DEFAULT_PERMS_CHECKER, (sender, structure, args) -> {
			msgUsage(sender, "cmd");
		}).permission(getPerm("cmd"));

		CommandStructure<CommandSender> selectorCmd = cmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> { // tp <to player>
			if (sender instanceof LivingEntity) {
				Collection<? extends Player> collection = selector(sender, args[0]);
				if (collection.isEmpty())
					msgUsage(sender, "cmd");
				else {
					Player target = collection.iterator().next();
					((LivingEntity) sender).teleport(target);
					msg(sender, "players.self", PlaceholdersExecutor.i().add("destination", target.getName()));
				}
			} else
				msgUsage(sender, "cmd");
		}).argument("-s", (sender, structure, args) -> { // silent
			if (sender instanceof LivingEntity) {
				Collection<? extends Player> collection = selector(sender, args[0]);
				if (collection.isEmpty())
					msgUsage(sender, "cmd");
				else {
					Player target = collection.iterator().next();
					((LivingEntity) sender).teleport(target);
				}
			} else
				msgUsage(sender, "cmd");
		}).parent();
		// tp <selected players> <to player>
		selectorCmd.selector(Selector.ENTITY_SELECTOR, (sender, structure, args) -> {
			Collection<? extends Player> collection = selector(sender, args[0]);
			if (collection.isEmpty())
				msgUsage(sender, "player");
			else {
				Collection<? extends Player> targets = selector(sender, args[1]);
				if (targets.isEmpty()) {
					msgUsage(sender, "player");
					return;
				}
				Player target = targets.iterator().next();
				for (Player player : collection) {
					player.teleport(target);
					PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("destination", target.getName()).add("sender", sender.getName()).add("target", target.getName());
					msg(sender, "players.sender", executor);
					msg(player, "players.target", executor);
				}
			}
		}).argument("-s", (sender, structure, args) -> { // silent
			Collection<? extends Player> collection = selector(sender, args[0]);
			if (collection.isEmpty())
				msgUsage(sender, "player");
			else {
				Collection<? extends Player> targets = selector(sender, args[1]);
				if (targets.isEmpty()) {
					msgUsage(sender, "player");
					return;
				}
				Player target = targets.iterator().next();
				for (Player player : collection)
					player.teleport(target);
			}
		});
		// tp <selected players> <x y z> {yaw} {pitch}
		selectorCmd.selector(Selector.POSITION, (sender, structure, arg2) -> { // x
			msgUsage(sender, "loc");
		}).selector(Selector.POSITION, (sender, structure, arg2) -> { // y
			msgUsage(sender, "loc");
		}).selector(Selector.POSITION, (sender, structure, args) -> { // z
			Collection<? extends Player> players = selector(sender, args[0]);
			String x = args[1];
			String y = args[2];
			String z = args[3];
			boolean individual = x.indexOf('~') != -1 || y.indexOf('~') != -1 || z.indexOf('~') != -1;
			boolean useMath = x.indexOf('+') != -1 || x.indexOf('-') != -1 || y.indexOf('+') != -1 || y.indexOf('-') != -1 || z.indexOf('+') != -1 || z.indexOf('-') != -1;
			teleport(sender,
					sender instanceof ConsoleCommandSender ? null : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : ((LivingEntity) sender).getWorld(),
					players, x, y, z, null, null, individual, useMath);
		}).selector(Selector.POSITION, (sender, structure, args) -> { // yaw
			Collection<? extends Player> players = selector(sender, args[0]);
			String x = args[1];
			String y = args[2];
			String z = args[3];
			String yaw = args[4];
			boolean individual = x.indexOf('~') != -1 || y.indexOf('~') != -1 || z.indexOf('~') != -1 || yaw.indexOf('~') != -1;
			boolean useMath = x.indexOf('+') != -1 || x.indexOf('-') != -1 || y.indexOf('+') != -1 || y.indexOf('-') != -1 || z.indexOf('+') != -1 || z.indexOf('-') != -1 || yaw.indexOf('+') != -1
					|| yaw.indexOf('-') != -1;
			teleport(sender,
					sender instanceof ConsoleCommandSender ? null : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : ((LivingEntity) sender).getWorld(),
					players, x, y, z, yaw, null, individual, useMath);
		}).selector(Selector.POSITION, (sender, structure, args) -> { // pitch
			Collection<? extends Player> players = selector(sender, args[0]);
			String x = args[1];
			String y = args[2];
			String z = args[3];
			String yaw = args[4];
			String pitch = args[5];
			boolean individual = x.indexOf('~') != -1 || y.indexOf('~') != -1 || z.indexOf('~') != -1 || yaw.indexOf('~') != -1 || pitch.indexOf('~') != -1;
			boolean useMath = x.indexOf('+') != -1 || x.indexOf('-') != -1 || y.indexOf('+') != -1 || y.indexOf('-') != -1 || z.indexOf('+') != -1 || z.indexOf('-') != -1 || yaw.indexOf('+') != -1
					|| yaw.indexOf('-') != -1 || pitch.indexOf('+') != -1 || pitch.indexOf('-') != -1;
			teleport(sender,
					sender instanceof ConsoleCommandSender ? null : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : ((LivingEntity) sender).getWorld(),
					players, x, y, z, yaw, pitch, individual, useMath);
		});

		// tp <x> <y> <z> {yaw} {pitch}

		cmd.selector(Selector.POSITION, (sender, structure, arg2) -> { // x
			if (!(sender instanceof Player)) {
				msgUsage(sender, "loc");
				return;
			}
			msgUsage(sender, "self-loc");
		}).selector(Selector.POSITION, (sender, structure, arg2) -> { // y
			if (!(sender instanceof Player)) {
				msgUsage(sender, "loc");
				return;
			}
			msgUsage(sender, "self-loc");
		}).selector(Selector.POSITION, (sender, structure, args) -> { // z
			if (!(sender instanceof Player)) {
				msgUsage(sender, "loc");
				return;
			}
			String x = args[0];
			String y = args[1];
			String z = args[2];
			boolean individual = x.indexOf('~') != -1 || y.indexOf('~') != -1 || z.indexOf('~') != -1;
			boolean useMath = x.indexOf('+') != -1 || x.indexOf('-') != -1 || y.indexOf('+') != -1 || y.indexOf('-') != -1 || z.indexOf('+') != -1 || z.indexOf('-') != -1;
			teleport(sender,
					sender instanceof ConsoleCommandSender ? null : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : ((LivingEntity) sender).getWorld(),
					Arrays.asList((Player) sender), x, y, z, null, null, individual, useMath);
		}).selector(Selector.POSITION, (sender, structure, args) -> { // yaw
			if (!(sender instanceof Player)) {
				msgUsage(sender, "loc");
				return;
			}
			String x = args[0];
			String y = args[1];
			String z = args[2];
			String yaw = args[3];
			boolean individual = x.indexOf('~') != -1 || y.indexOf('~') != -1 || z.indexOf('~') != -1 || yaw.indexOf('~') != -1;
			boolean useMath = x.indexOf('+') != -1 || x.indexOf('-') != -1 || y.indexOf('+') != -1 || y.indexOf('-') != -1 || z.indexOf('+') != -1 || z.indexOf('-') != -1 || yaw.indexOf('+') != -1
					|| yaw.indexOf('-') != -1;
			teleport(sender,
					sender instanceof ConsoleCommandSender ? null : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : ((LivingEntity) sender).getWorld(),
					Arrays.asList((Player) sender), x, y, z, yaw, null, individual, useMath);
		}).selector(Selector.POSITION, (sender, structure, args) -> { // pitch
			if (!(sender instanceof Player)) {
				msgUsage(sender, "loc");
				return;
			}
			String x = args[0];
			String y = args[1];
			String z = args[2];
			String yaw = args[3];
			String pitch = args[4];
			boolean individual = x.indexOf('~') != -1 || y.indexOf('~') != -1 || z.indexOf('~') != -1 || yaw.indexOf('~') != -1 || pitch.indexOf('~') != -1;
			boolean useMath = x.indexOf('+') != -1 || x.indexOf('-') != -1 || y.indexOf('+') != -1 || y.indexOf('-') != -1 || z.indexOf('+') != -1 || z.indexOf('-') != -1 || yaw.indexOf('+') != -1
					|| yaw.indexOf('-') != -1 || pitch.indexOf('+') != -1 || pitch.indexOf('-') != -1;
			teleport(sender,
					sender instanceof ConsoleCommandSender ? null : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getWorld() : ((LivingEntity) sender).getWorld(),
					Arrays.asList((Player) sender), x, y, z, yaw, pitch, individual, useMath);
		});

		// register
		List<String> cmds = getCommands();
		if (!cmds.isEmpty())
			this.cmd = addBypassSettings(cmd).build().register(cmds.remove(0), cmds.toArray(new String[0]));
	}

	private void teleport(CommandSender sender, World world, Collection<? extends Player> players, String x, String y, String z, String yaw, String pitch, boolean individual, boolean useMath) {
		if (!individual) {
			double finalX = useMath ? MathUtils.calculate(x) : ParseUtils.getDouble(x);
			double finalY = useMath ? MathUtils.calculate(y) : ParseUtils.getDouble(y);
			double finalZ = useMath ? MathUtils.calculate(z) : ParseUtils.getDouble(z);
			float finalYaw = yaw == null ? 0F : useMath ? (float) MathUtils.calculate(yaw) : ParseUtils.getFloat(yaw);
			float finalPitch = pitch == null ? 0F : useMath ? (float) MathUtils.calculate(pitch) : ParseUtils.getFloat(pitch);

			if (world == null)
				BukkitLoader.getNmsProvider().postToMainThread(() -> {
					for (Player player : players) {
						PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", player.getName()).add("x", finalX).add("y", finalY).add("z", finalZ)
								.add("yaw", finalYaw).add("pitch", finalPitch).add("world", player.getWorld().getName());
						if (sender.equals(player))
							msg(sender, "loc.self", executor);
						else {
							msg(sender, "loc.sender", executor);
							msg(player, "loc.target", executor);
						}
						Location loc = new Location(player.getWorld(), finalX, finalY, finalZ, finalYaw, finalPitch);
						player.teleport(loc);
					}
				});
			else {
				Location loc = new Location(world, finalX, finalY, finalZ, 0, 0);
				BukkitLoader.getNmsProvider().postToMainThread(() -> {
					for (Player player : players) {
						PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", player.getName()).add("x", finalX).add("y", finalY).add("z", finalZ)
								.add("yaw", finalYaw).add("pitch", finalPitch).add("world", world.getName());
						if (sender.equals(player))
							msg(sender, "loc.self", executor);
						else {
							msg(sender, "loc.sender", executor);
							msg(player, "loc.target", executor);
						}
						player.teleport(loc);
					}
				});
			}
		} else if (world == null)
			BukkitLoader.getNmsProvider().postToMainThread(() -> {
				for (Player player : players) {
					double finalX = useMath ? MathUtils.calculate(x.replace("~", player.getLocation().getX() + "")) : ParseUtils.getDouble(x.replace("~", player.getLocation().getX() + ""));
					double finalY = useMath ? MathUtils.calculate(y.replace("~", player.getLocation().getY() + "")) : ParseUtils.getDouble(y.replace("~", player.getLocation().getY() + ""));
					double finalZ = useMath ? MathUtils.calculate(z.replace("~", player.getLocation().getZ() + "")) : ParseUtils.getDouble(z.replace("~", player.getLocation().getZ() + ""));
					float finalYaw = yaw == null ? 0F : useMath ? (float) MathUtils.calculate(yaw.replace("~", player.getYaw() + "")) : ParseUtils.getFloat(yaw.replace("~", player.getYaw() + ""));
					float finalPitch = pitch == null ? 0F
							: useMath ? (float) MathUtils.calculate(pitch.replace("~", player.getPitch() + "")) : ParseUtils.getFloat(pitch.replace("~", player.getPitch() + ""));
					PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", player.getName()).add("x", finalX).add("y", finalY).add("z", finalZ)
							.add("yaw", finalYaw).add("pitch", finalPitch).add("world", player.getWorld().getName());
					if (sender.equals(player))
						msg(sender, "loc.self", executor);
					else {
						msg(sender, "loc.sender", executor);
						msg(player, "loc.target", executor);
					}
					Location loc = new Location(player.getWorld(), finalX, finalY, finalZ, finalYaw, finalPitch);
					player.teleport(loc);
				}
			});
		else
			BukkitLoader.getNmsProvider().postToMainThread(() -> {
				for (Player player : players) {
					double finalX = useMath ? MathUtils.calculate(x.replace("~", player.getLocation().getX() + "")) : ParseUtils.getDouble(x.replace("~", player.getLocation().getX() + ""));
					double finalY = useMath ? MathUtils.calculate(y.replace("~", player.getLocation().getY() + "")) : ParseUtils.getDouble(y.replace("~", player.getLocation().getY() + ""));
					double finalZ = useMath ? MathUtils.calculate(z.replace("~", player.getLocation().getZ() + "")) : ParseUtils.getDouble(z.replace("~", player.getLocation().getZ() + ""));
					float finalYaw = yaw == null ? 0F : useMath ? (float) MathUtils.calculate(yaw.replace("~", player.getYaw() + "")) : ParseUtils.getFloat(yaw.replace("~", player.getYaw() + ""));
					float finalPitch = pitch == null ? 0F
							: useMath ? (float) MathUtils.calculate(pitch.replace("~", player.getPitch() + "")) : ParseUtils.getFloat(pitch.replace("~", player.getPitch() + ""));
					PlaceholdersExecutor executor = PlaceholdersExecutor.i().add("sender", sender.getName()).add("target", player.getName()).add("x", finalX).add("y", finalY).add("z", finalZ)
							.add("yaw", finalYaw).add("pitch", finalPitch).add("world", world.getName());
					if (sender.equals(player))
						msg(sender, "loc.self", executor);
					else {
						msg(sender, "loc.sender", executor);
						msg(player, "loc.target", executor);
					}
					Location loc = new Location(world, finalX, finalY, finalZ, finalYaw, finalPitch);
					player.teleport(loc);
				}
			});
	}

}
