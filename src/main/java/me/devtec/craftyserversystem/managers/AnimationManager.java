package me.devtec.craftyserversystem.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.devtec.craftyserversystem.api.API;
import me.devtec.shared.dataholder.Config;
import me.devtec.shared.scheduler.Scheduler;
import me.devtec.shared.scheduler.Tasker;
import me.devtec.shared.utility.Animation;

public class AnimationManager {

	protected Map<String, Animation> registered;
	private int taskId;

	public AnimationManager() {
		registered = new HashMap<>();
	}

	public void load() {
		Config config = API.get().getConfigManager().getAnimations();
		List<Integer> speedSteps = new ArrayList<>();
		for (String key : config.getKeys()) {
			int step = Math.max(1, config.getInt(key + ".speed"));
			registered.put(key, new Animation(config.getStringList(key + ".lines"), step));
			speedSteps.add(step);
		}
		int minSpeed = findStep(speedSteps.toArray(new Integer[0]));
		taskId = new Tasker() {

			@Override
			public void run() {
				for (Animation animation : registered.values())
					animation.next();
			}
		}.runRepeating(minSpeed + 1, minSpeed);
	}

	public void unload() {
		Scheduler.cancelTask(taskId);
		registered.clear();
	}

	public Map<String, Animation> getRegistered() {
		return registered;
	}

	private int findStep(Integer[] numbers) {
		Arrays.sort(numbers);
		int gcd = 0;
		for (int i = 1; i < numbers.length; i++) {
			int diff = numbers[i] - numbers[i - 1];
			gcd = gcd == 0 ? diff : gcd(gcd, diff);
		}
		return gcd == 0 ? 1 : gcd;
	}

	private int gcd(int a, int b) {
		while (b != 0) {
			int temp = b;
			b = a % b;
			a = temp;
		}
		return a;
	}
}
