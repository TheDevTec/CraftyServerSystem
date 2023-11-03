package me.devtec.craftyserversystem.commands.internal.tprequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.devtec.craftyserversystem.annotations.IgnoredClass;
import me.devtec.craftyserversystem.annotations.Nonnull;
import me.devtec.craftyserversystem.annotations.Nullable;
import me.devtec.shared.API;
import me.devtec.shared.dataholder.Config;

@IgnoredClass
public class TpaManager {

	@Nonnull
	private static TpaManager instance;

	private Map<UUID, List<TpaRequest>> cache = new HashMap<>();

	@Nonnull
	public static TpaManager getProvider() {
		if (instance == null)
			instance = new TpaManager();
		return instance;
	}

	private TpaManager() {

	}

	/**
	 * Returns list of tpa/tpahere requests from or to this player
	 *
	 * @param owner Player's UUID
	 * @return List<TpaRequest>
	 */
	@Nullable
	public List<TpaRequest> getRequests(UUID owner) {
		List<TpaRequest> requests = cache.get(owner);
		if (requests == null)
			return null;
		Iterator<TpaRequest> iterator = requests.iterator();
		while (iterator.hasNext()) {
			TpaRequest request = iterator.next();
			if (!request.isValid()) {
				iterator.remove();
				List<TpaRequest> targetRequests = cache.get(request.getTarget());
				if (targetRequests != null) {
					targetRequests.remove(request);
					if (targetRequests.isEmpty())
						cache.remove(request.getTarget());
				}
			}
		}
		if (requests.isEmpty()) {
			cache.remove(owner);
			return null;
		}
		return requests;
	}

	/**
	 * Returns list of tpa/tpahere requests from or to this player (filtered by
	 * boolean) If @param fromOthers is set to true, returns only list of requests
	 * which are sent to this player. If @param fromOthers is set to false, returns
	 * only list of requests which are sent from this player.
	 * 
	 * @param owner      Player's UUID
	 * @param fromOthers Toggle status (true = Requests sent to this player, false =
	 *                   Requests sent from this player)
	 * @return List<TpaRequest>
	 */
	@Nullable
	public List<TpaRequest> getFilteredRequests(UUID owner, boolean fromOthers) {
		List<TpaRequest> requests = getRequests(owner);
		if (requests == null)
			return null;
		List<TpaRequest> filter = new ArrayList<>(requests);
		Iterator<TpaRequest> iterator = requests.iterator();
		while (iterator.hasNext()) {
			TpaRequest request = iterator.next();
			if (fromOthers ? request.getSender().equals(owner) : request.getTarget().equals(owner))
				iterator.remove();
		}
		if (filter.isEmpty())
			return null;
		return filter;
	}

	/**
	 * Get Player's toggled list of players from which player isn't accepting
	 * teleport requests
	 *
	 * @param player Player's UUID
	 * @return List<UUID>
	 */
	public List<UUID> getToggledPlayers(UUID owner) {
		List<UUID> uuids = new ArrayList<>();
		for (String uuidInString : API.getUser(owner).getStringList("css.tp-toggle.users"))
			uuids.add(UUID.fromString(uuidInString));
		return uuids;
	}

	/**
	 * Change player's global status of accepting teleport requests
	 *
	 * @param owner  Player's UUID
	 * @param status Toggle status
	 */
	public void setGlobalToggle(UUID owner, boolean status) {
		Config userData = API.getUser(owner);
		userData.set("css.tp-toggle.global", status);
	}

	/**
	 * Returns player's global teleport toggle
	 *
	 * @param owner Player's UUID
	 * @return boolean Toggle status
	 */
	public boolean hasGlobalToggle(UUID owner) {
		Config userData = API.getUser(owner);
		return userData.getBoolean("css.tp-toggle.global");
	}

	/**
	 * Add target's UUID to list of toggled players from which player (owner) isn't
	 * accepting teleport requests
	 *
	 * @param owner  Player's UUID
	 * @param target Player's UUID
	 */
	public void addToToggledPlayers(UUID owner, UUID target) {
		Config userData = API.getUser(owner);
		List<String> toggled = userData.getStringList("css.tp-toggle.users");
		toggled.add(target.toString());
		userData.set("css.tp-toggle.users", toggled);
	}

	/**
	 * Remove target's UUID from list of toggled players from which player (owner)
	 * isn't accepting teleport requests
	 *
	 * @param owner  Player's UUID
	 * @param target Player's UUID
	 */
	public void removeFromToggledPlayers(UUID owner, UUID target) {
		Config userData = API.getUser(owner);
		List<String> toggled = userData.getStringList("css.tp-toggle.users");
		toggled.remove(target.toString());
		userData.set("css.tp-toggle.users", toggled);
	}

	/**
	 * Sends a TpaRequest to the sender and target (adds to their request lists,
	 * this method doesn't send any messages)
	 *
	 * @param request TpaRequest
	 * @return Result
	 */
	public Result sendRequest(TpaRequest request) {
		if (!request.isValid())
			return Result.INVALID;

		Config targetData = API.getUser(request.getTarget());
		if (targetData.getBoolean("css.tp-toggle.global") || getToggledPlayers(request.getTarget()).contains(request.getSender()))
			return Result.DENIED_BY_TARGET;

		List<TpaRequest> sender = getRequests(request.getSender());
		if (sender == null)
			cache.put(request.getSender(), sender = new ArrayList<>());
		List<TpaRequest> target = getRequests(request.getTarget());
		if (target == null)
			cache.put(request.getTarget(), target = new ArrayList<>());

		if (sender.contains(request))
			return Result.FAILED_SENDER;
		if (target.contains(request))
			return Result.FAILED_TARGET;

		sender.add(request);
		target.add(request);
		return Result.SUCCESS;
	}

	/**
	 * Removes the TpaRequest request from the sender and target (this method
	 * doesn't send any messages)
	 *
	 * @param request TpaRequest
	 */
	public void removeRequest(TpaRequest request) {
		List<TpaRequest> sender = cache.get(request.getSender());
		if (sender != null)
			sender.remove(request);
		List<TpaRequest> target = getRequests(request.getTarget());
		if (target != null)
			target.remove(request);
	}
}
