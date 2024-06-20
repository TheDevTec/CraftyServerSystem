package me.devtec.craftyserversystem.commands.internal.msgsystem;

import java.util.List;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.shared.API;
import me.devtec.shared.annotations.Nonnull;
import me.devtec.shared.annotations.Nullable;
import me.devtec.shared.dataholder.Config;

@IgnoredClass
public class MsgManager {

	private static MsgManager instance;
	private String consoleReply;
	private String consoleName = "$CONSOLE";

	public static MsgManager get() {
		if (instance == null)
			instance = new MsgManager();
		return instance;
	}

	public boolean getSpy(@Nonnull String player) {
		return API.getUser(player).getBoolean("css.spy");
	}

	public void setSpy(@Nonnull String player, boolean status) {
		if (status)
			API.getUser(player).set("css.spy", true);
		else
			API.getUser(player).remove("css.spy");
	}

	@Nullable
	public String getReply(@Nullable String player) {
		if (player == null)
			return consoleReply;
		return API.getUser(player).getString("css.reply");
	}

	public void setReply(@Nullable String player, @Nullable String target) {
		if (target == null)
			target = consoleName;

		if (player == null)
			consoleReply = target;
		else
			API.getUser(player).set("css.reply", target);
	}

	@Nonnull
	public List<String> getIgnoredPlayers(@Nonnull String player) {
		return API.getUser(player).getStringList("css.msg-ignore");
	}

	public boolean addIgnore(@Nonnull String player, @Nonnull String target) {
		List<String> ignored = getIgnoredPlayers(player);
		if (!ignored.contains(target)) {
			ignored.add(target);
			API.getUser(player).set("css.msg-ignore", ignored);
			return true;
		}
		return false;
	}

	public boolean removeIgnore(@Nonnull String player, @Nonnull String target) {
		Config config = API.getUser(player);
		List<String> ignored = config.getStringList("css.msg-ignore");
		if (ignored.remove(target)) {
			config.set("css.msg-ignore", ignored);
			return true;
		}
		return false;
	}

	public boolean trySendMessage(@Nullable String player, @Nullable String target) {
		if (player != null && target != null && (getIgnoredPlayers(target).contains(player) || me.devtec.craftyserversystem.api.API.get().getCommandManager().getRegistered().containsKey("chatignore")
				&& API.getUser(target).getBoolean("css.chatignore") && me.devtec.craftyserversystem.api.API.get().getConfigManager().getMain().getBoolean("chatIgnore.hide-pms")))
			return false;
		return true;
	}
}
