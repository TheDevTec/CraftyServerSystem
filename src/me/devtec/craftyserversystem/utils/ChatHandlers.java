package me.devtec.craftyserversystem.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.sorting.SortingAPI;
import me.devtec.shared.sorting.SortingAPI.ComparableObject;

public class ChatHandlers {

	// return true, pokud nalezne nepovolenou adresu
	public static boolean antiAd(String input, List<String> whitelist) {
		Iterator<String> matcher = findWebAddress(input);
		while (matcher.hasNext()) {
			String next = matcher.next();
			if (next.startsWith("http://"))
				next = next.substring(7);
			if (next.startsWith("https://"))
				next = next.substring(8);
			if (next.startsWith("www."))
				next = next.substring(4);

			boolean whitelisted = false;
			for (String wl : whitelist)
				if (next.startsWith(wl)) {
					char c;
					if (next.length() == wl.length() || (c = next.charAt(wl.length())) == '/' || c == '?' || c == '&') {
						whitelisted = true;
						break;
					}
				}
			if (!whitelisted)
				return true;
		}
		matcher = findIpAddress(input);
		while (matcher.hasNext()) {
			String next = matcher.next();
			boolean whitelisted = false;
			for (String wl : whitelist)
				if (next.startsWith(wl) && next.length() == wl.length()) {
					whitelisted = true;
					break;
				}
			if (!whitelisted)
				return true;
		}
		return false;
	}

	public static Iterator<String> findWebAddress(String input) {
		return new Iterator<String>() {

			int startAt = -1;
			int endAt = 0;

			private boolean findUrl() {
				int lookingMode = 0;
				int prevCount = 0;
				int count = 0;
				startAt = endAt;
				int i;
				int initAt = 0;

				// url finder
				loop: for (i = endAt; i < input.length(); ++i) {
					char c = input.charAt(i);
					switchCase: switch (lookingMode) {
					case 0:
						initAt = i;
						if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_') {
							lookingMode = 1;
							count = 1;
							break switchCase;
						}
						break;
					case 1:
						if (c == ' ') {
							if (count >= 2)
								if (i + 1 < input.length() && (input.charAt(i + 1) == '.' || input.charAt(i + 1) == ',' || input.charAt(i + 1) == '-')) {
									++i;
									if (i + 1 < input.length() && input.charAt(i + 1) == ' ')
										++i;
									prevCount = count;
									count = 0;
									lookingMode = 2;
									break switchCase;
								}
							lookingMode = 0;
							break switchCase;
						}
						if (c == '.' || c == ',' || c == '-') { // xxx.
							if (count >= 2) {
								if (i + 1 < input.length() && input.charAt(i + 1) == ' ')
									++i;
								prevCount = count;
								count = 0;
								lookingMode = 2;
							} else
								lookingMode = 0;
							break switchCase;
						}
						if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_') {
							++count;
							break switchCase;
						}
						lookingMode = 0;
						break;
					case 2: // xxx.(lookingForPossibleEnding/xxx)
						if (c >= '0' && c <= '9') {
							lookingMode = 0; // Start
							count = 0;
							break switchCase;
						}
						if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
							if (++count == 1 && prevCount >= 4)
								switch (c) {
								case 'a':
									if (i + 2 < input.length() && (input.charAt(i + 1) == 'p' && input.charAt(i + 2) == 'p' || input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 't')) {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'g':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'g') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'm':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'c') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'o' && input.charAt(i + 2) == 'n' && input.charAt(i + 3) == 's' && input.charAt(i + 4) == 't'
											&& input.charAt(i + 5) == 'e' && input.charAt(i + 6) == 'r') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'c':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'z') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 2 < input.length() && input.charAt(i + 1) == 'o' && input.charAt(i + 2) == 'm') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && input.charAt(i + 1) == 'l' && input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'u' && input.charAt(i + 4) == 'd') {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'n':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'e' && input.charAt(i + 2) == 't') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'o':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 'g') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 5 < input.length() && input.charAt(i + 1) == 'n' && input.charAt(i + 2) == 'l' && input.charAt(i + 3) == 'i' && input.charAt(i + 4) == 'n'
											&& input.charAt(i + 5) == 'e') {
										i += 5;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'i':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'o') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'n' && input.charAt(i + 2) == 'f' && input.charAt(i + 3) == 'o') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'u':
									if (i + 1 < input.length() && (input.charAt(i + 1) == 's' || input.charAt(i + 1) == 'k')) {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'd':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'e') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'b':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'l' && input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'g') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 't':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'e' && input.charAt(i + 2) == 'c' && input.charAt(i + 3) == 'h') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 's':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'k') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'i' && input.charAt(i + 2) == 't' && input.charAt(i + 3) == 'e') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && (input.charAt(i + 1) == 'p' && input.charAt(i + 2) == 'a' && input.charAt(i + 3) == 'c' && input.charAt(i + 4) == 'e'
											|| input.charAt(i + 1) == 't' && input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'r' && input.charAt(i + 4) == 'e')) {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'p':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'l') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 'o') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'e':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'u') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'r':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'u') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'f':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'u' && input.charAt(i + 2) == 'n') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'x':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'y' && input.charAt(i + 2) == 'z') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'w':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'i' && input.charAt(i + 2) == 'k' && input.charAt(i + 3) == 'i') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'e' && input.charAt(i + 2) == 'b' && input.charAt(i + 3) == 's' && input.charAt(i + 4) == 'i'
											&& input.charAt(i + 5) == 't' && input.charAt(i + 6) == 'e') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								}
							break switchCase;
						}
						if (c == '.' || c == ',' || c == '-') { // xxx.
							if (count >= 4) {
								if (i + 1 < input.length() && input.charAt(i + 1) == ' ')
									++i;
								count = 0;
								lookingMode = 3;
							} else
								lookingMode = 0;
							break switchCase;
						}
						lookingMode = 0;
						break;
					case 3:
						if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
							if (++count == 1)
								switch (c) {
								case 'a':
									if (i + 2 < input.length() && (input.charAt(i + 1) == 'p' && input.charAt(i + 2) == 'p' || input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 't')) {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'g':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'g') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'm':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'c') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'o' && input.charAt(i + 2) == 'n' && input.charAt(i + 3) == 's' && input.charAt(i + 4) == 't'
											&& input.charAt(i + 5) == 'e' && input.charAt(i + 6) == 'r') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'c':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'z') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 2 < input.length() && input.charAt(i + 1) == 'o' && input.charAt(i + 2) == 'm') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && input.charAt(i + 1) == 'l' && input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'u' && input.charAt(i + 4) == 'd') {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'n':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'e' && input.charAt(i + 2) == 't') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'o':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 'g') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 5 < input.length() && input.charAt(i + 1) == 'n' && input.charAt(i + 2) == 'l' && input.charAt(i + 3) == 'i' && input.charAt(i + 4) == 'n'
											&& input.charAt(i + 5) == 'e') {
										i += 5;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'i':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'o') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'n' && input.charAt(i + 2) == 'f' && input.charAt(i + 3) == 'o') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'u':
									if (i + 1 < input.length() && (input.charAt(i + 1) == 's' || input.charAt(i + 1) == 'k')) {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'd':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'e') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'b':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'l' && input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'g') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 't':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'e' && input.charAt(i + 2) == 'c' && input.charAt(i + 3) == 'h') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 's':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'k') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'i' && input.charAt(i + 2) == 't' && input.charAt(i + 3) == 'e') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && (input.charAt(i + 1) == 'p' && input.charAt(i + 2) == 'a' && input.charAt(i + 3) == 'c' && input.charAt(i + 4) == 'e'
											|| input.charAt(i + 1) == 't' && input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'r' && input.charAt(i + 4) == 'e')) {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'p':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'l') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 'o') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'e':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'u') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'r':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'u') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'f':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'u' && input.charAt(i + 2) == 'n') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'x':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'y' && input.charAt(i + 2) == 'z') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'w':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'i' && input.charAt(i + 2) == 'k' && input.charAt(i + 3) == 'i') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'e' && input.charAt(i + 2) == 'b' && input.charAt(i + 3) == 's' && input.charAt(i + 4) == 'i'
											&& input.charAt(i + 5) == 't' && input.charAt(i + 6) == 'e') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								}
							break switchCase;
						}
						switch (c) {
						case '/':
							if (count < 2 || count > 5)
								break loop;
							lookingMode = 4; // Read until space
							break switchCase;
						default:
							lookingMode = 0;
							break;
						}
						break;
					case 4:
						if (c == ' ')
							break loop;
						break;
					}
				}
				if (lookingMode == 4) {
					startAt = initAt;
					endAt = i;
					return true;
				}
				endAt = i;
				return false;
			}

			@Override
			public boolean hasNext() {
				return endAt != startAt && findUrl();
			}

			@Override
			public String next() {
				return input.substring(startAt, endAt);
			}
		};
	}

	public static Iterator<String> findIpAddress(String input) {
		return new Iterator<String>() {

			int startAt = -1;
			int endAt = 0;

			private boolean findIp() {
				int lookingMode = 0;
				int suffixCount = 0;
				startAt = endAt;
				int i;
				int initAt = 0;
				boolean ipValid = false;

				// url finder
				loop: for (i = endAt; i < input.length(); ++i) {
					char c = input.charAt(i);
					switch (lookingMode) {
					case 0:
						initAt = i;
						if (c >= '0' && c <= '9') {
							++suffixCount;
							lookingMode = 1;
							continue;
						}
						break;
					case 1: // additional
						if (c == ' ')
							continue;
						if (c == '.' || c == ',' || c == '-') { // probably end
							if (suffixCount < 4) {
								lookingMode = 2;
								suffixCount = 0;
							}
							continue;
						}
						if (c >= '0' && c <= '9') {
							if (++suffixCount > 3) {
								lookingMode = 0;
								suffixCount = 0;
							}
							continue;
						}
						initAt = i;
						lookingMode = 0;
						break;
					case 2:
						if (c == ' ')
							continue;
						if (c == '.' || c == ',' || c == '-') { // probably end
							if (suffixCount < 4) {
								lookingMode = 3;
								suffixCount = 0;
							}
							continue;
						}
						if (c >= '0' && c <= '9') {
							if (++suffixCount > 3) {
								lookingMode = 0;
								suffixCount = 0;
							}
							continue;
						}
						initAt = i;
						lookingMode = 0;
						break;
					case 3:
						if (c == ' ')
							continue;
						if (c == '.' || c == ',' || c == '-') { // probably end
							if (suffixCount < 4) { // 1, 3
								lookingMode = 4;
								suffixCount = 0;
								ipValid = true;
							}
							continue;
						}
						if (c >= '0' && c <= '9') {
							if (++suffixCount > 3) {
								lookingMode = 0;
								suffixCount = 0;
							}
							continue;
						}
						initAt = i;
						lookingMode = 0;
						break;
					case 4:
						if (c == ' ')
							continue;
						if (c == ':') {
							if (suffixCount == 0) {
								ipValid = false;
								break loop; // Invalid ip
							}
							lookingMode = 5;
							suffixCount = 0;
							continue;
						}
						if (c >= '0' && c <= '9') {
							if (++suffixCount > 3) {
								lookingMode = 0;
								suffixCount = 0;
								ipValid = false;
							}
							continue;
						}
						if (suffixCount <= 3)
							break loop; // Valid ip
						initAt = i;
						lookingMode = 0;
						ipValid = false;
						break;
					case 5:
						if (c == ' ')
							continue;
						if (c >= '0' && c <= '9') {
							if (++suffixCount > 5)
								break loop; // Valid ip
							continue;
						}
						break loop; // Valid ip
					}
				}
				if (ipValid) {
					startAt = initAt;
					endAt = i;
					return true;
				}
				endAt = i;
				return false;
			}

			@Override
			public boolean hasNext() {
				return endAt != startAt && findIp();
			}

			@Override
			public String next() {
				return input.substring(startAt, endAt);
			}
		};
	}

	// return true - pokud je hrac v antiSpam queue (nemuze odeslat zpravu)
	public static boolean processAntiSpam(UUID uniqueId, String message, Map<UUID, Object[]> prevMsgs, int maxMessages) {
		Object[] sentMsgs = prevMsgs.get(uniqueId);
		if (sentMsgs == null)
			prevMsgs.put(uniqueId, sentMsgs = new Object[maxMessages]);
		for (int i = 0; i < maxMessages - 1; ++i)
			if (message.equals(sentMsgs[i]))
				return true;
		int pos = sentMsgs[maxMessages - 1] == null ? 0 : (int) sentMsgs[maxMessages - 1];
		sentMsgs[pos] = message;
		sentMsgs[maxMessages - 1] = ++pos >= maxMessages - 1 ? 0 : pos;
		return false;
	}

	// nalezne hledane sektory a returne int array - v prvnim array namisto
	// List<int[]> - int[] obsahuje v arg0=positionInString, arg1=stringLength
	public static int[][] match(String input, List<String> search) {
		List<int[]> list = new ArrayList<>();
		for (String name : search) {
			int pos = 0;
			int endPos = 0;
			while (pos != -1) {
				pos = input.indexOf(name, endPos);
				if (pos == -1)
					continue;
				endPos = pos + name.length();
				list.add(new int[] { pos, name.length() });
			}
		}
		return list.isEmpty() ? null : list.toArray(new int[0][0]);
	}

	// return true - pokud nalezne vulgarismy
	public static boolean antiSwear(String input, List<String> words, List<String> allowedPhrases, int[][] ignoredSections) {
		StringContainer filtered = new StringContainer(input.length());
		int posOfSection = 0;
		int[] currentSection = ignoredSections == null ? null : ignoredSections[posOfSection];
		char prev = 0;
		int times = 0;
		for (int i = 0; i < input.length(); i++) {
			if (currentSection != null && currentSection[0] == i) {
				i += currentSection[1];
				if (currentSection.length - 1 != ++posOfSection)
					currentSection = ignoredSections[posOfSection];
				else
					currentSection = null;
				--i;
				prev = 0;
				times = 0;
				continue;
			}

			char origin = input.charAt(i);
			if (origin == ' ')
				continue;
			char c = Character.toLowerCase(origin);
			switch (c) {
			case 'é':
			case 'ě':
				c = 'e';
				break;
			case 'š':
			case 'ś':
				c = 's';
				break;
			case 'č':
			case 'ć':
				c = 'c';
				break;
			case 'ť':
				c = 't';
				break;
			case 'ř':
			case 'ŕ':
				c = 'r';
				break;
			case 'ž':
			case 'ź':
				c = 'z';
				break;
			case 'ý':
				c = 'y';
				break;
			case 'í':
				c = 'i';
				break;
			case 'á':
				c = 'a';
				break;
			case '3':
				c = 'e';
				break;
			case '0':
				c = 'o';
				break;
			case '1':
				c = 't';
				break;
			case '5':
				c = 's';
				break;
			}
			if (prev == c && (c == 'k' ? ++times >= 2 : true))
				continue;
			filtered.append(c);
			times = 0;
			prev = c;
		}
		for (String word : words) {
			int posStart = 0;
			int pos = filtered.indexOf(word, posStart);
			if (pos != -1) {
				String phrase = null;
				int startAt = -1;
				for (String fphrase : allowedPhrases) {
					startAt = fphrase.indexOf(word);
					if (startAt != -1) {
						phrase = fphrase;
						break;
					}
				}
				boolean found = true;
				while (pos != -1) {
					found = true;
					posStart = pos + word.length();
					if (startAt != -1) {
						String before = startAt == 0 ? "" : phrase.substring(0, startAt);
						String after = startAt + word.length() == phrase.length() ? "" : phrase.substring(startAt + word.length());

						if (before.length() == 0 && after.length() == 0 || pos - before.length() < 0 || pos + after.length() > filtered.length()
								|| filtered.indexOf(phrase, pos - before.length()) != pos - before.length()) {
							pos = filtered.indexOf(word, posStart);
							return true;
						}
						posStart += after.length();
						found = false;
					}
					pos = filtered.indexOf(word, posStart);
				}
				return found;
			}
		}
		return false;
	}

	// Nalezne vulgarismy a nahradi za replacement
	public static String antiSwearReplace(String input, List<String> words, List<String> allowedPhrases, int[][] ignoredSections, String replacement, boolean shouldAddColors) {
		StringContainerWithPositions filtered = new StringContainerWithPositions(input.length());
		int posOfSection = 0;
		int[] currentSection = ignoredSections == null ? null : ignoredSections[posOfSection];

		char prev = 0;
		int times = 0;
		for (int i = 0; i < input.length(); ++i) {
			if (currentSection != null && currentSection[0] == i) {
				i += currentSection[1];
				if (currentSection.length - 1 != ++posOfSection)
					currentSection = ignoredSections[posOfSection];
				else
					currentSection = null;
				--i;
				prev = 0;
				times = 0;
				continue;
			}

			char origin = input.charAt(i);
			if (origin == ' ')
				continue;
			char c = Character.toLowerCase(origin);
			switch (c) {
			case 'é':
			case 'ě':
				c = 'e';
				break;
			case 'š':
			case 'ś':
				c = 's';
				break;
			case 'č':
			case 'ć':
				c = 'c';
				break;
			case 'ť':
				c = 't';
				break;
			case 'ř':
			case 'ŕ':
				c = 'r';
				break;
			case 'ž':
			case 'ź':
				c = 'z';
				break;
			case 'ý':
				c = 'y';
				break;
			case 'í':
				c = 'i';
				break;
			case 'á':
				c = 'a';
				break;
			case '3':
				c = 'e';
				break;
			case '0':
				c = 'o';
				break;
			case '1':
				c = 't';
				break;
			case '5':
				c = 's';
				break;
			}
			if (prev == c && (c == 'k' ? ++times >= 2 : true))
				continue;
			filtered.append(c, i);
			times = 0;
			prev = c;
		}
		StringContainer container = null;

		Map<Integer, Integer> positionAndLength = null;

		for (String word : words) {
			int posStart = 0;
			int pos = filtered.indexOf(word, posStart);
			if (pos != -1) {
				String phrase = null;
				int startAt = -1;
				for (String fphrase : allowedPhrases) {
					startAt = fphrase.indexOf(word);
					if (startAt != -1) {
						phrase = fphrase;
						break;
					}
				}
				while (pos != -1) {
					posStart = pos + word.length();
					if (startAt != -1) {
						String before = startAt == 0 ? "" : phrase.substring(0, startAt);
						String after = startAt + word.length() == phrase.length() ? "" : phrase.substring(startAt + word.length());
						if (before.length() == 0 && after.length() == 0 || pos - before.length() < 0 || pos + after.length() > filtered.length()
								|| filtered.indexOf(phrase, pos - before.length()) != pos - before.length()) {
							if (container == null)
								container = new StringContainer(input);
							if (positionAndLength == null)
								positionAndLength = new HashMap<>();
							int realPos = filtered.posAt(pos);
							positionAndLength.put(realPos, filtered.posAt(pos + word.length() - 1) + 1);
							pos = filtered.indexOf(word, posStart);
							continue;
						}
						posStart += after.length();
					} else {
						if (container == null)
							container = new StringContainer(input);
						if (positionAndLength == null)
							positionAndLength = new HashMap<>();
						int realPos = filtered.posAt(pos);
						positionAndLength.put(realPos, filtered.posAt(pos + word.length() - 1) + 1);
					}
					pos = filtered.indexOf(word, posStart);
				}
			}
		}

		if (container == null)
			return input;
		ComparableObject<Integer, Integer>[] result = SortingAPI.sortByKeyArray(positionAndLength, true);
		for (ComparableObject<Integer, Integer> res : result)
			try {
				container.replace(res.getKey(), res.getValue(), replacement + (shouldAddColors ? "§g" : ""));
			} catch (Exception e) {
			} // V případě že by se dvě slova překrývali
		return container.toString();
	}

	// Odstrani ze Stringu všechny zdvojeny pismena a predela caps na lowercaps
	public static String antiFlood(String input, int[][] ignoredSections, int floodMaxNumbers, int floodMaxChars, int floodMaxCapsChars) {
		StringContainer filtered = new StringContainer(input.length());
		char prev = 0;
		int times = 0;
		boolean inCaps = false;
		int capsTimes = 0;

		int posOfSection = 0;
		int numberTimes = 0;
		int dotTimes = 0;
		int[] currentSection = ignoredSections == null ? null : ignoredSections[posOfSection];
		for (int i = 0; i < input.length(); i++) {
			if (currentSection != null && currentSection[0] == i) {
				for (int c = 0; c < currentSection[1]; ++c) {
					filtered.append(input.charAt(i));
					++i;
				}
				if (currentSection.length - 1 != ++posOfSection)
					currentSection = ignoredSections[posOfSection];
				else
					currentSection = null;
				--i;
				prev = 0;
				times = 0;
				numberTimes = 0;
				inCaps = false;
				capsTimes = 0;
				continue;
			}

			char origin = input.charAt(i);
			if (origin == ' ') {
				filtered.append(origin);
				inCaps = false;
				capsTimes = 0;
				numberTimes = 0;
				continue;
			}
			if (origin == '.') {
				if (++dotTimes >= 4) {
					inCaps = false;
					continue;
				}
				filtered.append(origin);
				inCaps = false;
				continue;
			}
			dotTimes = 0;
			if (origin >= '0' && origin <= '9') {
				if (++numberTimes >= floodMaxNumbers) {
					inCaps = false;
					continue;
				}
				filtered.append(origin);
				inCaps = false;
				continue;
			}
			numberTimes = 0;

			boolean allowedForCapsCheck = origin >= 96 && origin <= 658;
			char c = allowedForCapsCheck ? Character.toLowerCase(origin) : origin;
			if (c == prev && ++times >= floodMaxChars)
				continue;
			if (++capsTimes >= floodMaxCapsChars && inCaps)
				filtered.append(inCaps ? c : origin);
			else
				filtered.append(origin);
			if (prev != c)
				times = 0;
			prev = c;
			if (allowedForCapsCheck && c != origin)
				inCaps = true;
		}
		return filtered.toString();
	}
}