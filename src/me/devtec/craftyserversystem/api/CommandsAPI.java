package me.devtec.craftyserversystem.api;

import me.devtec.craftyserversystem.commands.internal.bansystem.BanAPI;
import me.devtec.craftyserversystem.commands.internal.home.HomeManager;
import me.devtec.craftyserversystem.commands.internal.msgsystem.MsgManager;
import me.devtec.craftyserversystem.commands.internal.tprequest.TpaManager;
import me.devtec.craftyserversystem.commands.internal.warp.WarpManager;

public class CommandsAPI {

	protected CommandsAPI() {

	}

	/**
	 * Our own BanAPI with which you can manage bans, mutes, history and active
	 * punishments
	 *
	 * @return BanAPI
	 */
	public BanAPI getBanAPI() {
		return BanAPI.get();
	}

	/**
	 * Our own HomeManager with which you can retrieve max homes of Vault groups,
	 * list of homes of specified player and managing of homes of specified player
	 *
	 * @return HomeManager
	 */
	public HomeManager getHomeManager() {
		return HomeManager.get();
	}

	/**
	 * Our own MsgManager with which you can manage private messages between players
	 *
	 * @return MsgManager
	 */
	public MsgManager getMsgManager() {
		return MsgManager.get();
	}

	/**
	 * Our own TpaManager with which you can manage tpa requests between players
	 *
	 * @return TpaManager
	 */
	public TpaManager getTpaManager() {
		return TpaManager.getProvider();
	}

	/**
	 * Our own WarpManager with which you can manage server warps
	 *
	 * @return WarpManager
	 */
	public WarpManager getWarpManager() {
		return WarpManager.getProvider();
	}

}
