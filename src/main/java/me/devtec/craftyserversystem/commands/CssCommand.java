package me.devtec.craftyserversystem.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.devtec.craftyserversystem.api.API;
import me.devtec.craftyserversystem.managers.cooldown.CooldownHolder;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.commands.holder.CommandHolder;
import me.devtec.shared.commands.manager.PermissionChecker;
import me.devtec.shared.commands.structures.CommandStructure;
import me.devtec.shared.text.TextRenderer;
import me.devtec.shared.utility.StringUtils;
import me.devtec.theapi.bukkit.BukkitLoader;

public abstract class CssCommand {

	public static final PermissionChecker<CommandSender> DEFAULT_PERMS_CHECKER = (sender, permission, tablist) -> sender
			.hasPermission(permission);

	public static final PermissionChecker<Player> P_DEFAULT_PERMS_CHECKER = (sender, permission, tablist) -> sender
			.hasPermission(permission);

	@Nonnull
	protected CommandHolder<? extends CommandSender> cmd;

	@Nonnull
	public String section() {
		return getClass().getSimpleName().toLowerCase();
	}

	public abstract void register();

	public void reload() {
	}

	public void unregister() {
		if (!isRegistered())
			return;

		cmd.unregister();
		cmd = null;
	}

	public boolean isRegistered() {
		return cmd != null;
	}

	@Nonnull
	public List<String> getCommands() {
		return API.get().getConfigManager().getCommands().getStringList(section() + ".cmd");
	}

	@Nonnull
	public <T> CommandStructure<T> addBypassSettings(CommandStructure<T> cmd) {
		String cooldownGroup = API.get().getConfigManager().getCommands().getString(section() + ".cooldown");

		if (cooldownGroup != null) {
			CooldownHolder cooldown = API.get().getCooldownManager().getOrPrepare(cooldownGroup);

			if (cooldown != null)
				cmd.first().cooldownDetection((sender, structure, args) -> !cooldown.accept((CommandSender) sender));
		}

		return cmd;
	}

	public boolean perm(CommandSender sender, String path) {
		String permission = getPerm(path);

		return permission == null || sender.hasPermission(permission);
	}

	@Nullable
	public String getPerm(String path) {
		return API.get().getConfigManager().getCommands().getString(section() + ".perms." + path);
	}

	public void msg(CommandSender sender, String path) {
		msg(sender, path, renderer(sender));
	}

	public void msg(CommandSender sender, String path, TextRenderer renderer) {

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(),
				section() + (path.isEmpty() ? "" : "." + path), prepareRenderer(sender, renderer), sender);
	}

	public void msgOut(CommandSender sender, String path) {
		msgOut(sender, path, renderer(sender));
	}

	public void msgOut(CommandSender sender, String path, TextRenderer renderer) {

		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getTranslations(), path,
				prepareRenderer(sender, renderer), sender);
	}

	public void msgUsage(CommandSender sender, String path) {
		API.get().getMsgManager().sendMessageFromFile(API.get().getConfigManager().getCommands(),
				section() + ".usage." + path, renderer(sender), sender);
	}

	protected TextRenderer renderer() {
		return TextRenderer.create().placeholder("prefix", API.get().getConfigManager().getPrefix()).colorize();
	}

	protected TextRenderer renderer(CommandSender sender) {
		TextRenderer renderer = renderer();

		if (sender instanceof Player)
			renderer.target(((Player) sender).getUniqueId());

		return renderer;
	}

	protected TextRenderer prepareRenderer(CommandSender sender, TextRenderer renderer) {

		if (renderer == null)
			renderer = TextRenderer.create();

		if (!renderer.tokens().contains("{prefix}"))
			renderer.placeholder("prefix", API.get().getConfigManager().getPrefix());

		if (renderer.target() == null && sender instanceof Player)
			renderer.target(((Player) sender).getUniqueId());

		renderer.colorize();

		return renderer;
	}

	@Nonnull
	public Collection<? extends Player> selector(CommandSender sender, String selector) {

		char lowerCase = selector.length() == 1 && selector.charAt(0) == '*' ? '*'
				: selector.length() == 2 && selector.charAt(0) == '@' ? Character.toLowerCase(selector.charAt(1)) : 0;

		if (lowerCase != 0)
			switch (lowerCase) {
			case 'a':
			case 'e':
			case '*':
				return BukkitLoader.getOnlinePlayers();

			case 'r':
				return Collections.singleton(StringUtils.randomFromCollection(BukkitLoader.getOnlinePlayers()));

			case 's':
			case 'p':
				Location position;

				if (sender instanceof Player)
					position = ((Player) sender).getLocation();
				else if (sender instanceof BlockCommandSender)
					position = ((BlockCommandSender) sender).getBlock().getLocation();
				else
					position = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);

				double distance = -1;
				Player nearestPlayer = null;

				for (Player sameWorld : position.getWorld().getPlayers()) {
					double distanceRange = sameWorld.getLocation().distance(position);

					if (distance == -1 || distanceRange < distance) {
						distance = distanceRange;
						nearestPlayer = sameWorld;
					}
				}

				Collection<? extends Player> players = BukkitLoader.getOnlinePlayers();

				return players.isEmpty() ? Collections.emptyList()
						: Collections.singleton(nearestPlayer == null ? players.iterator().next() : nearestPlayer);

			default:
				break;
			}

		Player target = Bukkit.getPlayer(selector);

		return target == null ? Collections.emptyList() : Collections.singleton(target);
	}
}