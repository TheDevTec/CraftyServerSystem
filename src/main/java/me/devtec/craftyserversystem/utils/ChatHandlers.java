package me.devtec.craftyserversystem.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.devtec.shared.Pair;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.sorting.SortingAPI;
import me.devtec.shared.sorting.SortingAPI.ComparableObject;

public class ChatHandlers {

	// return true, if found not allowed ad
	public static boolean antiAd(String input, List<String> whitelist) {
		if (input == null)
			return false;

		Iterator<String> matcher = findWebAddress(input);
		lookup: while (matcher.hasNext()) {
			String next = matcher.next();
			if (next.startsWith("http://"))
				next = next.substring(7);
			if (next.startsWith("https://"))
				next = next.substring(8);
			if (next.startsWith("www."))
				next = next.substring(4);

			for (String wl : whitelist)
				if (next.startsWith(wl)) {
					char c;
					if (next.length() == wl.length() || (c = next.charAt(wl.length())) == '/' || c == '?' || c == '&')
						continue lookup;
				}
			return true;
		}
		matcher = findPlainWebAddress(input);
		lookup: while (matcher.hasNext()) {
			String next = matcher.next();
			for (String wl : whitelist)
				if (next.startsWith(wl)) {
					char c;
					if (next.length() == wl.length() || (c = next.charAt(wl.length())) == '/' || c == '?' || c == '&')
						continue lookup;
				}
			return true;
		}
		matcher = findIpAddress(input);
		lookup: while (matcher.hasNext()) {
			String next = matcher.next();
			for (String wl : whitelist)
				if (next.startsWith(wl) && next.length() == wl.length())
					continue lookup;
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
								if (i + 1 < input.length()
										&& ((c = input.charAt(i + 1)) == '.' || c == ',' || c == '-')) {
									++i;
									while (i + 1 < input.length()
											&& ((c = input.charAt(i + 1)) == '.' || c == ',' || c == '-' ))
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
								while (i + 1 < input.length()
										&& ((c = input.charAt(i + 1)) == '.' || c == ',' || c == '-' ))
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
									if (i + 2 < input.length()
											&& (input.charAt(i + 1) == 'p' && input.charAt(i + 2) == 'p'
											|| input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 't')) {
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
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'o'
											&& input.charAt(i + 2) == 'n' && input.charAt(i + 3) == 's'
											&& input.charAt(i + 4) == 't' && input.charAt(i + 5) == 'e'
											&& input.charAt(i + 6) == 'r') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'c':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'z') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 2 < input.length() && input.charAt(i + 1) == 'o'
											&& input.charAt(i + 2) == 'm') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && input.charAt(i + 1) == 'l'
											&& input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'u'
											&& input.charAt(i + 4) == 'd') {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'n':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'e'
									&& input.charAt(i + 2) == 't') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'o':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r'
									&& input.charAt(i + 2) == 'g') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 5 < input.length() && input.charAt(i + 1) == 'n'
											&& input.charAt(i + 2) == 'l' && input.charAt(i + 3) == 'i'
											&& input.charAt(i + 4) == 'n' && input.charAt(i + 5) == 'e') {
										i += 5;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'i':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'o') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'n'
											&& input.charAt(i + 2) == 'f' && input.charAt(i + 3) == 'o') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'u':
									if (i + 1 < input.length()
											&& (input.charAt(i + 1) == 's' || input.charAt(i + 1) == 'k')) {
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
									if (i + 3 < input.length() && input.charAt(i + 1) == 'l'
									&& input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'g') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 't':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'e'
									&& input.charAt(i + 2) == 'c' && input.charAt(i + 3) == 'h') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 's':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'k') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'i'
											&& input.charAt(i + 2) == 't' && input.charAt(i + 3) == 'e') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && (input.charAt(i + 1) == 'p'
											&& input.charAt(i + 2) == 'a' && input.charAt(i + 3) == 'c'
											&& input.charAt(i + 4) == 'e'
											|| input.charAt(i + 1) == 't' && input.charAt(i + 2) == 'o'
											&& input.charAt(i + 3) == 'r' && input.charAt(i + 4) == 'e')) {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'p':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'l') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r'
											&& input.charAt(i + 2) == 'o') {
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
									if (i + 2 < input.length() && input.charAt(i + 1) == 'u'
									&& input.charAt(i + 2) == 'n') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'x':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'y'
									&& input.charAt(i + 2) == 'z') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'w':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'i'
									&& input.charAt(i + 2) == 'k' && input.charAt(i + 3) == 'i') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'e'
											&& input.charAt(i + 2) == 'b' && input.charAt(i + 3) == 's'
											&& input.charAt(i + 4) == 'i' && input.charAt(i + 5) == 't'
											&& input.charAt(i + 6) == 'e') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								}
							break switchCase;
						}
						if (c == '.' || c == ',' || c == '-') { // xxx.
							if (count >= 4) {
								while (i + 1 < input.length()
										&& ((c = input.charAt(i + 1)) == '.' || c == ',' || c == '-' ))
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
									if (i + 2 < input.length()
											&& (input.charAt(i + 1) == 'p' && input.charAt(i + 2) == 'p'
											|| input.charAt(i + 1) == 'r' && input.charAt(i + 2) == 't')) {
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
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'o'
											&& input.charAt(i + 2) == 'n' && input.charAt(i + 3) == 's'
											&& input.charAt(i + 4) == 't' && input.charAt(i + 5) == 'e'
											&& input.charAt(i + 6) == 'r') {
										i += 6;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'c':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'z') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 2 < input.length() && input.charAt(i + 1) == 'o'
											&& input.charAt(i + 2) == 'm') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && input.charAt(i + 1) == 'l'
											&& input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'u'
											&& input.charAt(i + 4) == 'd') {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'n':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'e'
									&& input.charAt(i + 2) == 't') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'o':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r'
									&& input.charAt(i + 2) == 'g') {
										i += 2;
										lookingMode = 4; // Read until space
									} else if (i + 5 < input.length() && input.charAt(i + 1) == 'n'
											&& input.charAt(i + 2) == 'l' && input.charAt(i + 3) == 'i'
											&& input.charAt(i + 4) == 'n' && input.charAt(i + 5) == 'e') {
										i += 5;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'i':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'o') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'n'
											&& input.charAt(i + 2) == 'f' && input.charAt(i + 3) == 'o') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'u':
									if (i + 1 < input.length()
											&& (input.charAt(i + 1) == 's' || input.charAt(i + 1) == 'k')) {
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
									if (i + 3 < input.length() && input.charAt(i + 1) == 'l'
									&& input.charAt(i + 2) == 'o' && input.charAt(i + 3) == 'g') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 't':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'e'
									&& input.charAt(i + 2) == 'c' && input.charAt(i + 3) == 'h') {
										i += 3;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 's':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'k') {
										i += 1;
										lookingMode = 4; // Read until space
									} else if (i + 3 < input.length() && input.charAt(i + 1) == 'i'
											&& input.charAt(i + 2) == 't' && input.charAt(i + 3) == 'e') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 4 < input.length() && (input.charAt(i + 1) == 'p'
											&& input.charAt(i + 2) == 'a' && input.charAt(i + 3) == 'c'
											&& input.charAt(i + 4) == 'e'
											|| input.charAt(i + 1) == 't' && input.charAt(i + 2) == 'o'
											&& input.charAt(i + 3) == 'r' && input.charAt(i + 4) == 'e')) {
										i += 4;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'p':
									if (i + 1 < input.length() && input.charAt(i + 1) == 'l') {
										i += 1;
										lookingMode = 4; // Read until space
									}
									if (i + 2 < input.length() && input.charAt(i + 1) == 'r'
											&& input.charAt(i + 2) == 'o') {
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
									if (i + 2 < input.length() && input.charAt(i + 1) == 'u'
									&& input.charAt(i + 2) == 'n') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'x':
									if (i + 2 < input.length() && input.charAt(i + 1) == 'y'
									&& input.charAt(i + 2) == 'z') {
										i += 2;
										lookingMode = 4; // Read until space
									}
									break switchCase;
								case 'w':
									if (i + 3 < input.length() && input.charAt(i + 1) == 'i'
									&& input.charAt(i + 2) == 'k' && input.charAt(i + 3) == 'i') {
										i += 3;
										lookingMode = 4; // Read until space
									} else if (i + 6 < input.length() && input.charAt(i + 1) == 'e'
											&& input.charAt(i + 2) == 'b' && input.charAt(i + 3) == 's'
											&& input.charAt(i + 4) == 'i' && input.charAt(i + 5) == 't'
											&& input.charAt(i + 6) == 'e') {
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

	// Catches obfuscated ads where the dot was replaced by a space, e.g. "goodhost cz".
	// Requires a 5+ char domain-like word right before a TLD from a tightly curated list
	// (only TLDs that are virtually never standalone English/Czech words). The TLD itself
	// must end on a word boundary so phrases like "minecraft czech" or "minecraft czeczko"
	// don't trigger.
	public static Iterator<String> findPlainWebAddress(String input) {
		return new Iterator<String>() {

			int startAt = -1;
			int endAt = 0;
			String currentMatch = null;

			private boolean find() {
				startAt = endAt;
				int i = endAt;
				int wordStart = -1;
				int wordLength = 0;
				int state = 0;

				while (i < input.length()) {
					char c = input.charAt(i);
					switch (state) {
					case 0:
						if (isPlainDomainCharStart(c)) {
							wordStart = i;
							wordLength = 1;
							state = 1;
						}
						++i;
						break;
					case 1:
						if (isPlainDomainChar(c)) {
							++wordLength;
							++i;
							break;
						}
						if (c == ' ' && wordLength >= 5) {
							int spaceEnd = i;
							while (spaceEnd < input.length() && input.charAt(spaceEnd) == ' ')
								++spaceEnd;
							int tldLen = matchPlainTldLength(input, spaceEnd);
							if (tldLen > 0) {
								int afterTld = spaceEnd + tldLen;
								if (afterTld >= input.length() || !isPlainDomainChar(input.charAt(afterTld))) {
									currentMatch = input.substring(wordStart, i) + "."
											+ input.substring(spaceEnd, afterTld);
									startAt = wordStart;
									endAt = afterTld;
									return true;
								}
							}
							state = 0;
							i = spaceEnd;
							break;
						}
						state = 0;
						++i;
						break;
					}
				}
				endAt = i;
				return false;
			}

			@Override
			public boolean hasNext() {
				return endAt != startAt && find();
			}

			@Override
			public String next() {
				return currentMatch;
			}
		};
	}

	private static boolean isPlainDomainCharStart(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
	}

	private static boolean isPlainDomainChar(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_';
	}

	private static int matchPlainTldLength(String input, int pos) {
		// Longest first so "info" wins over a partial "in"/"io" scan, etc.
		// Restricted to TLDs that are practically never standalone words in English/Czech,
		// so "i love cz" / "hello cz" / "play app" don't false-positive.
		String[] tlds = { "monster", "online", "info", "xyz", "cz", "sk", "eu", "pl", "ru", "de", "gg", "io" };
		for (String tld : tlds) {
			int len = tld.length();
			if (pos + len > input.length())
				continue;
			boolean match = true;
			for (int k = 0; k < len; ++k) {
				char a = input.charAt(pos + k);
				if (a >= 'A' && a <= 'Z')
					a = (char) (a + 32);
				if (a != tld.charAt(k)) {
					match = false;
					break;
				}
			}
			if (match)
				return len;
		}
		return 0;
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
							if (suffixCount > 0 && suffixCount < 4) {
								lookingMode = 3;
								suffixCount = 0;
							} else {
								initAt = i;
								lookingMode = 0;
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
							if (suffixCount > 0 && suffixCount < 4) { // 1, 3
								lookingMode = 4;
								suffixCount = 0;
							} else {
								initAt = i;
								lookingMode = 0;
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
						if (suffixCount > 0 && suffixCount <= 3) {
							ipValid = true;
							break loop; // Valid ip
						}
						initAt = i;
						lookingMode = 0;
						ipValid = false;
						break;
					case 5:
						if (c == ' ')
							continue;
						if (c >= '0' && c <= '9') {
							if (++suffixCount > 5)
								break loop;
							continue;
						}
						if (suffixCount > 0)
							ipValid = true;
						break loop; // Valid ip
					}
				}
				if (!ipValid && lookingMode == 4 && suffixCount > 0 && suffixCount <= 3)
					ipValid = true;
				else if (!ipValid && lookingMode == 5 && suffixCount > 0 && suffixCount <= 5)
					ipValid = true;
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

	// return true - if player is in the antiSpam queue (can't send message)
	@SuppressWarnings("unchecked")
	public static boolean processAntiSpam(UUID uniqueId, String message, Map<UUID, Object[]> prevMsgs, int maxMessages,
			double minimalSimilarity) {
		Object[] sentMsgs = prevMsgs.get(uniqueId);
		if (sentMsgs == null)
			prevMsgs.put(uniqueId, sentMsgs = new Object[maxMessages]);
		Set<String> calcSpaces = new HashSet<>();
		splitSpaces(calcSpaces, message);
		for (int i = 0; i < maxMessages - 1; ++i)
			if (sentMsgs[i] != null && calculateSimilarity(calcSpaces, (Set<String>) sentMsgs[i]) >= minimalSimilarity)
				return true;
		int pos = sentMsgs[maxMessages - 1] == null ? 0 : (int) sentMsgs[maxMessages - 1];
		sentMsgs[pos] = calcSpaces;
		sentMsgs[maxMessages - 1] = ++pos >= maxMessages - 1 ? 0 : pos;
		return false;
	}

	private static double calculateSimilarity(Set<String> text1, Set<String> text2) {
		Set<String> set2 = new HashSet<>(text2);
		Set<String> intersection = new HashSet<>(text1);
		intersection.retainAll(set2);
		set2.addAll(text1);
		return (double) intersection.size() / text1.size();
	}

	private static void splitSpaces(Set<String> set, String text) {
		int prev = 0;
		int spaceAt;
		while ((spaceAt = text.indexOf(' ', prev)) != -1) {
			set.add(text.substring(prev, spaceAt));
			prev = spaceAt + 1;
		}
		set.add(text.substring(prev));
	}

	public static boolean randomLettersSpam(String input, int minimumLength, int minimumWordLength,
			int minimumSuspiciousWords, double minimumSuspiciousRatio, int repeatedSequenceThreshold) {
		if (input == null || input.isEmpty())
			return false;
		String normalized = normalizeLettersAndSpaces(input);
		if (normalized.length() < minimumLength)
			return false;

		List<String> checkedWords = new ArrayList<>();
		StringBuilder lettersOnly = new StringBuilder(normalized.length());
		for (String word : normalized.split(" ")) {
			if (word.isEmpty())
				continue;
			lettersOnly.append(word);
			if (word.length() >= minimumWordLength)
				checkedWords.add(word);
		}
		if (lettersOnly.length() < minimumLength)
			return false;

		if (hasRepeatedSequence(lettersOnly.toString(), repeatedSequenceThreshold))
			return true;
		if (checkedWords.size() < minimumSuspiciousWords) {
			int suspiciousWords = 0;
			for (String word : checkedWords)
				if (looksRandomLetterWord(word, Math.max(3, repeatedSequenceThreshold - 1)))
					++suspiciousWords;
			if ((checkedWords.size() == 1 && suspiciousWords == 1) || (checkedWords.size() == 2 && suspiciousWords == 1))
				return true;
			return false;
		}

		int suspiciousWords = 0;
		for (String word : checkedWords)
			if (looksRandomLetterWord(word, Math.max(3, repeatedSequenceThreshold - 1)))
				++suspiciousWords;
		return suspiciousWords >= minimumSuspiciousWords
				&& (double) suspiciousWords / checkedWords.size() >= minimumSuspiciousRatio;
	}

	private static String normalizeLettersAndSpaces(String input) {
		StringBuilder builder = new StringBuilder(input.length());
		boolean lastSpace = true;
		for (int i = 0; i < input.length(); ++i) {
			String normalized = Normalizer.normalize(String.valueOf(Character.toLowerCase(input.charAt(i))),
					Normalizer.Form.NFD);
			for (int d = 0; d < normalized.length(); ++d) {
				char c = normalized.charAt(d);
				if (Character.getType(c) == Character.NON_SPACING_MARK)
					continue;
				if (Character.isLetter(c)) {
					builder.append(simplifyCharacter(c));
					lastSpace = false;
					continue;
				}
				if (!lastSpace) {
					builder.append(' ');
					lastSpace = true;
				}
			}
		}
		int length = builder.length();
		if (length != 0 && builder.charAt(length - 1) == ' ')
			builder.deleteCharAt(length - 1);
		return builder.toString();
	}

	private static boolean looksRandomLetterWord(String word, int repeatedSequenceThreshold) {
		if (hasRepeatedSequence(word, repeatedSequenceThreshold))
			return true;

		int suspiciousBigrams = 0;
		int consonantsInRow = 0;
		int vowelsInRow = 0;
		int maxConsonantsInRow = 0;
		int maxVowelsInRow = 0;
		for (int i = 0; i < word.length(); ++i) {
			char c = word.charAt(i);
			if (isLatinVowel(c)) {
				consonantsInRow = 0;
				if (++vowelsInRow > maxVowelsInRow)
					maxVowelsInRow = vowelsInRow;
			} else {
				vowelsInRow = 0;
				if (++consonantsInRow > maxConsonantsInRow)
					maxConsonantsInRow = consonantsInRow;
			}
			if (i + 1 < word.length() && isSuspiciousRandomBigram(c, word.charAt(i + 1)))
				++suspiciousBigrams;
		}

		if (maxConsonantsInRow >= 5 || maxVowelsInRow >= 4 || (word.length() >= 8 && suspiciousBigrams >= 2))
			return true;
		return word.length() >= 12 && suspiciousBigrams >= 1 && maxConsonantsInRow >= 4;
	}

	private static boolean hasRepeatedSequence(String text, int threshold) {
		if (threshold <= 1)
			threshold = 2;
		for (int size = 2; size <= 4; ++size) {
			if (text.length() < size * threshold)
				continue;
			Map<String, Integer> sequences = new HashMap<>();
			for (int i = 0; i + size <= text.length(); ++i) {
				String sequence = text.substring(i, i + size);
				int count = sequences.getOrDefault(sequence, 0) + 1;
				if (count >= threshold)
					return true;
				sequences.put(sequence, count);
			}
		}
		return false;
	}

	private static boolean isLatinVowel(char c) {
		return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'а' || c == 'е' || c == 'и'
				|| c == 'о' || c == 'у' || c == 'ъ' || c == 'ю' || c == 'я';
	}

	private static boolean isSuspiciousRandomBigram(char first, char second) {
		switch ("" + first + second) {
		case "ao":
		case "ai":
		case "ia":
		case "io":
		case "iu":
		case "uo":
		case "ae":
		case "ea":
		case "jw":
		case "wj":
		case "jx":
		case "xj":
		case "qj":
		case "jq":
		case "qk":
		case "kq":
		case "qw":
		case "wq":
		case "qx":
		case "xq":
		case "qz":
		case "zq":
		case "wd":
		case "dw":
		case "fw":
		case "wf":
		case "gj":
		case "jg":
		case "vb":
		case "bv":
		case "vx":
		case "xv":
		case "zx":
		case "xz":
			return true;
		default:
			return false;
		}
	}

	// Multilingual profanity detector

	public enum ProfanityMatchType {
		EXACT, NORMALIZED, OBFUSCATED
	}

	public enum ProfanityDecision {
		MATCH, REVIEW, IGNORE
	}

	public static final class BadWordRule {
		private final String word;
		private final boolean matchInsideWord;
		private final Set<String> languages;
		private final Set<String> collisionLanguages;

		public BadWordRule(String word, boolean matchInsideWord, Set<String> languages,
				Set<String> collisionLanguages) {
			this.word = normalizeRuleWord(word);
			this.matchInsideWord = matchInsideWord;
			this.languages = normalizeLanguageSet(languages);
			this.collisionLanguages = normalizeLanguageSet(collisionLanguages);
		}

		public String getWord() {
			return word;
		}

		public boolean isMatchInsideWord() {
			return matchInsideWord;
		}

		public Set<String> getLanguages() {
			return languages;
		}

		public Set<String> getCollisionLanguages() {
			return collisionLanguages;
		}
	}

	public static final class AllowedPhraseRule {
		private final String word;
		private final String phrase;

		public AllowedPhraseRule(String word, String phrase) {
			this.word = normalizeRuleWord(word);
			this.phrase = normalizeAntiSwearPhrase(phrase);
		}

		public boolean isValid() {
			return !word.isEmpty() && !phrase.isEmpty();
		}
	}

	public static final class ProfanityMatch {
		private final BadWordRule rule;
		private final int start;
		private final int end;
		private final String original;
		private final ProfanityMatchType type;
		private final ProfanityDecision decision;
		private final String language;

		private ProfanityMatch(BadWordRule rule, int start, int end, String original, ProfanityMatchType type,
				ProfanityDecision decision, String language) {
			this.rule = rule;
			this.start = start;
			this.end = end;
			this.original = original;
			this.type = type;
			this.decision = decision;
			this.language = language;
		}

		public BadWordRule getRule() { return rule; }
		public int getStart() { return start; }
		public int getEnd() { return end; }
		public String getOriginal() { return original; }
		public ProfanityMatchType getType() { return type; }
		public ProfanityDecision getDecision() { return decision; }
		public String getLanguage() { return language; }
	}

	public static final class ProfanityResult {
		private final List<ProfanityMatch> matches;

		private ProfanityResult(List<ProfanityMatch> matches) {
			this.matches = Collections.unmodifiableList(matches);
		}

		public List<ProfanityMatch> getMatches() { return matches; }

		public boolean hasMatch() {
			for (ProfanityMatch match : matches)
				if (match.decision == ProfanityDecision.MATCH)
					return true;
			return false;
		}

		public String replace(String input, String replacement, boolean addColors) {
			if (input == null || !hasMatch())
				return input;
			StringBuilder result = new StringBuilder(input);
			int lastStart = input.length() + 1;
			for (int i = matches.size() - 1; i >= 0; --i) {
				ProfanityMatch match = matches.get(i);
				if (match.decision != ProfanityDecision.MATCH || match.end > lastStart)
					continue;
				result.replace(match.start, match.end, replacement + (addColors ? "§g" : ""));
				lastStart = match.start;
			}
			return result.toString();
		}
	}

	private static final class NormalizedText {
		private final String text;
		private final int[] starts;
		private final int[] ends;

		private NormalizedText(String text, int[] starts, int[] ends) {
			this.text = text;
			this.starts = starts;
			this.ends = ends;
		}
	}

	private static final class Candidate {
		private final BadWordRule rule;
		private final int start;
		private final int end;
		private final ProfanityMatchType type;

		private Candidate(BadWordRule rule, int start, int end, ProfanityMatchType type) {
			this.rule = rule;
			this.start = start;
			this.end = end;
			this.type = type;
		}
	}

	// Config: languages|collision-languages|word. Plain words stay global.
	public static BadWordRule parseProfanityRule(String value) {
		if (value == null)
			return null;
		String[] parts = value.split("\\|", 3);
		if (parts.length != 3) {
			boolean anywhere = value.startsWith("*");
			return new BadWordRule(anywhere ? value.substring(1) : value, anywhere, Collections.<String>emptySet(),
					Collections.<String>emptySet());
		}
		String word = parts[2].trim();
		boolean anywhere = word.startsWith("*");
		return new BadWordRule(anywhere ? word.substring(1) : word, anywhere, parseLanguages(parts[0]),
				parseLanguages(parts[1]));
	}

	public static ProfanityResult checkProfanity(String input, List<BadWordRule> rules,
			List<AllowedPhraseRule> allowedPhrases, int[][] ignoredSections) {
		if (input == null || input.isEmpty() || rules == null || rules.isEmpty())
			return new ProfanityResult(Collections.<ProfanityMatch>emptyList());

		NormalizedText semantic = normalizeProfanityText(input, ignoredSections, false);
		NormalizedText evasion = normalizeProfanityText(input, ignoredSections, true);
		Map<String, Candidate> candidates = new LinkedHashMap<>();
		for (BadWordRule rule : rules) {
			if (rule == null || rule.word.isEmpty())
				continue;
			findCandidates(semantic, rule, ProfanityMatchType.NORMALIZED, candidates);
			findCandidates(evasion, rule, ProfanityMatchType.OBFUSCATED, candidates);
		}

		if (candidates.isEmpty())
			return new ProfanityResult(Collections.<ProfanityMatch>emptyList());

		String detectedLanguage = null;
		List<ProfanityMatch> matches = new ArrayList<>();
		for (Candidate candidate : candidates.values()) {
			if (candidate.type == ProfanityMatchType.OBFUSCATED && !isObfuscatedJoin(input, candidate))
				continue;
			if (isAllowedPhrase(input, candidate, allowedPhrases))
				continue;
			ProfanityDecision decision = ProfanityDecision.MATCH;
			if (!candidate.rule.languages.isEmpty() || !candidate.rule.collisionLanguages.isEmpty()) {
				if (detectedLanguage == null)
					detectedLanguage = detectChatLanguage(input);
				if (detectedLanguage == null)
					decision = ProfanityDecision.REVIEW;
				else if (!candidate.rule.languages.isEmpty() && !candidate.rule.languages.contains(detectedLanguage))
					decision = ProfanityDecision.IGNORE;
				else if (candidate.rule.collisionLanguages.contains(detectedLanguage))
					decision = ProfanityDecision.IGNORE;
			}
			ProfanityMatchType type = candidate.type;
			String original = input.substring(candidate.start, candidate.end);
			if (type == ProfanityMatchType.NORMALIZED && original.equalsIgnoreCase(candidate.rule.word))
				type = ProfanityMatchType.EXACT;
			matches.add(new ProfanityMatch(candidate.rule, candidate.start, candidate.end, original, type, decision,
					detectedLanguage));
		}
		return new ProfanityResult(matches);
	}

	public static boolean checkContextualProfanity(String input, List<String> phrases) {
		return findContextualProfanity(input, phrases) != null;
	}

	public static String findContextualProfanity(String input, List<String> phrases) {
		if (input == null || input.isEmpty() || phrases == null || phrases.isEmpty())
			return null;
		NormalizedText normalized = normalizeProfanityText(input, null, true);
		for (String phrase : phrases) {
			String expected = normalizeRuleWord(phrase);
			if (expected.isEmpty())
				continue;
			int at = normalized.text.indexOf(expected);
			while (at != -1) {
				int end = at + expected.length();
				if (isOriginalWordBounded(input, normalized.starts[at], normalized.ends[end - 1]))
					return phrase;
				at = normalized.text.indexOf(expected, at + 1);
			}
		}
		return null;
	}

	private static boolean isOriginalWordBounded(String input, int start, int end) {
		return (start == 0 || !Character.isLetterOrDigit(input.charAt(start - 1)))
				&& (end >= input.length() || !Character.isLetterOrDigit(input.charAt(end)));
	}

	private static void findCandidates(NormalizedText normalized, BadWordRule rule, ProfanityMatchType type,
			Map<String, Candidate> candidates) {
		int at = normalized.text.indexOf(rule.word);
		while (at != -1) {
			int end = at + rule.word.length();
			if (rule.matchInsideWord || hasWordStart(normalized.text, at)) {
				int originalStart = normalized.starts[at];
				int originalEnd = normalized.ends[end - 1];
				String key = originalStart + ":" + originalEnd + ":" + rule.word;
				Candidate existing = candidates.get(key);
				if (existing == null || existing.type == ProfanityMatchType.OBFUSCATED && type != ProfanityMatchType.OBFUSCATED)
					candidates.put(key, new Candidate(rule, originalStart, originalEnd, type));
			}
			at = normalized.text.indexOf(rule.word, at + 1);
		}
	}

	private static boolean hasWordStart(String text, int at) {
		return at == 0 || text.charAt(at - 1) == ' ';
	}

	private static boolean isObfuscatedJoin(String input, Candidate candidate) {
		String part = input.substring(candidate.start, candidate.end);
		String[] tokens = part.trim().split("\\s+");
		if (tokens.length < 2)
			return true;
		for (int i = 0; i < tokens.length - 1; ++i)
			if (letterCount(tokens[i]) > 1)
				return false;
		return true;
	}

	private static int letterCount(String input) {
		int count = 0;
		for (int i = 0; i < input.length(); ++i)
			if (Character.isLetterOrDigit(input.charAt(i)))
				++count;
		return count;
	}

	private static boolean isAllowedPhrase(String input, Candidate candidate, List<AllowedPhraseRule> allowedPhrases) {
		if (allowedPhrases == null)
			return false;
		NormalizedText normalized = normalizeProfanityText(input, null, true);
		for (AllowedPhraseRule allowed : allowedPhrases) {
			if (allowed == null || !candidate.rule.word.equals(allowed.word) || allowed.phrase.isEmpty())
				continue;
			String expected = normalizeRuleWord(allowed.phrase);
			if (expected.isEmpty())
				continue;
			int at = normalized.text.indexOf(expected);
			while (at != -1) {
				int end = at + expected.length();
				if (isOriginalWordBounded(input, normalized.starts[at], normalized.ends[end - 1]))
					return true;
				at = normalized.text.indexOf(expected, at + 1);
			}
		}
		return false;
	}

	private static NormalizedText normalizeProfanityText(String input, int[][] ignoredSections, boolean removeSeparators) {
		StringBuilder text = new StringBuilder(input.length());
		List<Integer> starts = new ArrayList<>();
		List<Integer> ends = new ArrayList<>();
		boolean lastSpace = true;
		for (int i = 0; i < input.length(); ++i) {
			if (isIgnoredPosition(i, ignoredSections))
				continue;
			String value = Normalizer.normalize(String.valueOf(input.charAt(i)).toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
			for (int c = 0; c < value.length(); ++c) {
				char character = value.charAt(c);
				if (Character.getType(character) == Character.NON_SPACING_MARK)
					continue;
				char mapped = removeSeparators ? mapLeet(character) : character;
				if (Character.isLetterOrDigit(mapped)) {
					text.append(mapped);
					starts.add(i);
					ends.add(i + 1);
					lastSpace = false;
				} else if (Character.isWhitespace(character) || !removeSeparators) {
					if (!lastSpace && text.length() != 0) {
						text.append(' ');
						starts.add(i);
						ends.add(i + 1);
					}
					lastSpace = true;
				}
			}
		}
		int[] startArray = new int[starts.size()];
		int[] endArray = new int[ends.size()];
		for (int i = 0; i < starts.size(); ++i) {
			startArray[i] = starts.get(i);
			endArray[i] = ends.get(i);
		}
		return new NormalizedText(text.toString(), startArray, endArray);
	}

	private static boolean isIgnoredPosition(int position, int[][] sections) {
		if (sections == null)
			return false;
		for (int[] section : sections)
			if (section != null && section.length >= 2 && position >= section[0] && position < section[0] + section[1])
				return true;
		return false;
	}

	private static char mapLeet(char character) {
		switch (character) {
		case '0': return 'o';
		case '1': case '!': return 'i';
		case '3': return 'e';
		case '4': case '@': return 'a';
		case '5': case '$': return 's';
		case '7': return 't';
		default: return character;
		}
	}

	private static String detectChatLanguage(String input) {
		String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
		if (normalized.length() < 6)
			return null;
		int romanian = scoreLanguage(normalized, "ă", "â", "î", "ș", "ț", "cam", "greu", "este", "sunt", "pentru", "care", "acest");
		int czech = scoreLanguage(normalized, "ě", "ř", "ů", "č", "š", "ž", "jsem", "jsi", "kde", "jak", "proto", "tenhle", "něco");
		int slovak = scoreLanguage(normalized, "ľ", "ĺ", "ŕ", "ä", "ô", "ď", "ť", "ako", "som", "nie", "ktor", "toto");
		int polish = scoreLanguage(normalized, "ą", "ę", "ł", "ś", "ź", "ż", "cz", "sz", "jest", "nie", "który");
		int russian = scoreLanguage(normalized, "ы", "э", "ё", "привет", "это", "что", "для", "пожал");
		int bulgarian = scoreLanguage(normalized, "ъ", "щ", "й", "съм", "това", "българ", "няма");
		int best = Math.max(romanian, Math.max(czech, Math.max(slovak, Math.max(polish, Math.max(russian, bulgarian)))));
		if (best < 1)
			return null;
		if (best == romanian)
			return "ro";
		if (best == czech)
			return "cs";
		if (best == slovak)
			return "sk";
		if (best == polish)
			return "pl";
		if (best == russian)
			return "ru";
		return "bg";
	}

	private static int scoreLanguage(String input, String... markers) {
		int score = 0;
		for (String marker : markers)
			if (input.contains(marker))
				++score;
		return score;
	}

	private static Set<String> parseLanguages(String input) {
		Set<String> languages = new LinkedHashSet<>();
		if (input != null)
			for (String language : input.split(",")) {
				String code = language.trim().toLowerCase(Locale.ROOT);
				if (code.matches("bg|cs|pl|hr|sk|de|da|nl|el|en|es|fi|fr|hu|it|lt|lv|pt|ro|ru|sr|sv|tr"))
					languages.add(code);
			}
		return languages;
	}

	private static Set<String> normalizeLanguageSet(Set<String> languages) {
		return languages == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(languages));
	}

	private static String normalizeRuleWord(String word) {
		return normalizeAntiSwearPhrase(word).replace(" ", "");
	}

	private static boolean antiSwearContextLegacy(String input, List<String> phrases) {
		if (input == null || input.isEmpty() || phrases == null || phrases.isEmpty())
			return false;
		String normalized = normalizeAntiSwearPhrase(input);
		for (String phrase : phrases)
			if (containsNormalizedPhrase(normalized, phrase))
				return true;
		return false;
	}

	private static boolean containsNormalizedPhrase(String normalizedInput, String phrase) {
		boolean isDuckLeadingPhrase = phrase.startsWith("duck");
		int pos = normalizedInput.indexOf(phrase);
		while (pos != -1) {
			int end = pos + phrase.length();
			boolean before = pos == 0 || normalizedInput.charAt(pos - 1) == ' ';
			boolean after = end == normalizedInput.length() || normalizedInput.charAt(end) == ' ';
			if (before && after) {
				// "duck you" / "ducking man" caught inside a literal-duck phrase
				// ("the duck you mentioned", "a ducking clown video") is not the swear.
				if (isDuckLeadingPhrase) {
					String prevToken = getPrevToken(normalizedInput, pos);
					if (prevToken != null && isLegitDuckPrefix(prevToken)) {
						if ("the".equals(prevToken)) {
							String prevPrevToken = getPrevPrevToken(normalizedInput, pos);
							if (prevPrevToken != null && isProfanityQuestionWord(prevPrevToken))
								return true;
						}
						pos = normalizedInput.indexOf(phrase, pos + 1);
						continue;
					}
				}
				return true;
			}
			pos = normalizedInput.indexOf(phrase, pos + 1);
		}
		return false;
	}

	// Lookup for search words and return array of int[]
	// arg0=positionInString, arg1=stringLength
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

	// return true - if found any vulgarism
	private static boolean antiSwearLegacy(String input, List<String> words, List<String> exactWords,
			List<Pair> allowedPhrases, int[][] ignoredSections) {
		StringContainerWithPositions filtered = filterAntiSwearInput(input, ignoredSections);

		List<int[]> allowedSections = new ArrayList<>();
		for (String word : words)
			if (containsWord(input, filtered, word, allowedPhrases, true, allowedSections))
				return true;

		for (String word : exactWords)
			if (containsWord(input, filtered, word, allowedPhrases, false, allowedSections))
				return true;
		return false;
	}

	private static boolean containsWord(String original, StringContainerWithPositions filtered, String word,
			List<Pair> allowedPhrases, boolean exact, List<int[]> allowedSections) {
		int[] pos = filtered.indexOf(word, exact, true);
		loop: while (pos != null) {
			if (isPlainSubstringInsideWord(filtered, pos, word)
					|| matchesAllowedPhraseForWord(original, filtered, word, pos[0], exact, allowedPhrases,
							allowedSections)) {
				pos = filtered.indexOf(word, pos[0] + 1, exact, false);
				continue loop;
			}
			return true;
		}
		pos = filtered.indexOf(word, exact, false);
		loop: while (pos != null) {
			if (isPlainSubstringInsideWord(filtered, pos, word)
					|| matchesAllowedPhraseForWord(original, filtered, word, pos[0], exact, allowedPhrases,
							allowedSections)) {
				pos = filtered.indexOf(word, pos[0] + 1, exact, false);
				continue loop;
			}
			return true;
		}
		return containsSimilarWord(original, filtered, word, exact, allowedPhrases, allowedSections);
	}

	// Short dictionary words (≤ 3 chars) sit inside plenty of innocent longer words:
	// "cip" in "cipactli"/"recipient", "sex" in "sextant", "gay" in "Gayatri", "kkt" in
	// random strings, etc. For those, accept a plain contiguous substring match only if
	// the match is bounded by spaces (or string edges). If the match span is longer than
	// the word itself, an obfuscation gap was consumed (e.g. "c i p" or "ki*kt"), and we
	// keep flagging it. Words with the leading "*" wildcard convention are left alone —
	// admins opted into anywhere-matching there.
	private static boolean isPlainSubstringInsideWord(StringContainerWithPositions filtered, int[] pos, String word) {
		if (word.length() > 3 || word.startsWith("*"))
			return false;
		int start = pos[0];
		int end = pos[1];
		if (end - start + 1 > word.length())
			return false; // obfuscation chars were skipped during the match
		if ((start > 0 && filtered.charAt(start - 1) != ' ') || (end + 1 < filtered.length() && filtered.charAt(end + 1) != ' '))
			return true;
		return false;
	}

	private static boolean containsSimilarWord(String original, StringContainerWithPositions filtered, String word,
			boolean exact, List<Pair> allowedPhrases, List<int[]> allowedSections) {
		String normalized = filtered.toString();
		int length = normalized.length();
		int start = 0;
		while (start < length) {
			if (normalized.charAt(start) == ' ') {
				start++;
				continue;
			}
			int end = start;
			while (end < length && normalized.charAt(end) != ' ')
				end++;
			String token = normalized.substring(start, end);
			boolean match = isSimilarWord(token, word)
					|| isContextualDuckInsult(original, filtered, token, word, start, end);
			if (match)
				if (!matchesAllowedPhraseForWord(original, filtered, word, start, exact, allowedPhrases,
						allowedSections))
					return true;
			start = end;
		}
		return false;
	}

	private static boolean isSimilarWord(String token, String word) {
		int lenDiff = Math.abs(token.length() - word.length());
		if (lenDiff > 1)
			return false;
		// Direct sound-alike obfuscation map (c↔k, v↔u, k↔g, y↔i).
		// Catches "čokot" → normalized "cokot" → "kokot", "kreten" → "creten",
		// "kurva" → "kvrva", "kvrva" without needing repeated chars or digits.
		if (token.equals(word) || containsCommonSubstitution(token, word))
			return true;
		if (!containsObfuscationMarkers(token, word))
			return false;
		return levenshteinDistance(token, word, 1) <= 1;
	}

	private static boolean isContextualDuckInsult(String original, StringContainerWithPositions filtered,
			String token, String word, int filteredStart, int filteredEnd) {
		if ((!"fuck".equals(word) && !"ducking".equals(word)) || (!"duck".equals(token) && !"duckin".equals(token)))
			return false;
		String normalized = filtered.toString();

		// If "duck" is clearly used as a noun ("the duck", "a duck", "my duck", "yellow duck",
		// "see duck", "call me duck", "hey duck", ...) we leave it alone — even if an insult
		// target follows ("the duck you mentioned" is not a swear).
		String prevToken = getPrevToken(normalized, filteredStart);
		if (prevToken != null && isLegitDuckPrefix(prevToken)) {
			// Carve out "what/who/where/why/how the duck" — still profanity.
			if ("the".equals(prevToken)) {
				String prevPrevToken = getPrevPrevToken(normalized, filteredStart);
				if (prevPrevToken != null && isProfanityQuestionWord(prevPrevToken))
					return true;
			}
			return false;
		}

		String nextToken = getNextToken(normalized, filteredEnd);
		if (nextToken != null) {
			if (isInsultTargetPronoun(nextToken) || isInsultTargetNoun(nextToken))
				return true;
			if (isProfanityObject(nextToken))
				return true;
			if (isProfanityPhrasal(nextToken))
				return true;
		}
		String rawNext = getRawNextToken(original, filtered, filteredEnd);
		if (rawNext != null && (containsDigit(rawNext) || startsWithUppercase(rawNext)))
			return true;
		return false;
	}

	private static String getPrevToken(String normalized, int filteredStart) {
		if (filteredStart <= 0)
			return null;
		int end = filteredStart;
		while (end > 0 && normalized.charAt(end - 1) == ' ')
			--end;
		if (end <= 0)
			return null;
		int start = end;
		while (start > 0 && normalized.charAt(start - 1) != ' ')
			--start;
		return normalized.substring(start, end);
	}

	private static String getPrevPrevToken(String normalized, int filteredStart) {
		if (filteredStart <= 0)
			return null;
		int end = filteredStart;
		while (end > 0 && normalized.charAt(end - 1) == ' ')
			--end;
		while (end > 0 && normalized.charAt(end - 1) != ' ')
			--end;
		while (end > 0 && normalized.charAt(end - 1) == ' ')
			--end;
		if (end <= 0)
			return null;
		int start = end;
		while (start > 0 && normalized.charAt(start - 1) != ' ')
			--start;
		return normalized.substring(start, end);
	}

	private static boolean isLegitDuckPrefix(String token) {
		switch (token) {
		// articles & determiners (incl. question determiners "which duck...")
		case "the":
		case "a":
		case "an":
		case "what":
		case "which":
		case "whose":
			// possessives
		case "my":
		case "your":
		case "his":
		case "her":
		case "its":
		case "our":
		case "their":
		case "muj":
		case "moje":
		case "tvuj":
		case "tvoje":
		case "jeho":
		case "jeji":
			// demonstratives
		case "this":
		case "that":
		case "these":
		case "those":
		case "ten":
		case "ta":
		case "to":
		case "tento":
		case "tato":
			// common descriptors for a literal duck
		case "yellow":
		case "rubber":
		case "wooden":
		case "plastic":
		case "white":
		case "black":
		case "brown":
		case "blue":
		case "green":
		case "baby":
		case "little":
		case "big":
		case "small":
		case "cute":
		case "wild":
		case "old":
		case "new":
		case "young":
		case "zluty":
		case "maly":
		case "velky":
		case "krasny":
		case "novy":
			// names & titles
		case "donald":
		case "daffy":
		case "scrooge":
		case "mr":
		case "mrs":
		case "miss":
		case "dear":
			// verbs commonly taking "duck" as direct object (or copulas around it)
		case "see":
		case "saw":
		case "seen":
		case "seeing":
		case "watch":
		case "watched":
		case "watching":
		case "feed":
		case "fed":
		case "feeding":
		case "love":
		case "loves":
		case "loved":
		case "loving":
		case "like":
		case "likes":
		case "liked":
		case "hate":
		case "hates":
		case "hated":
		case "call":
		case "calls":
		case "called":
		case "calling":
		case "name":
		case "names":
		case "named":
		case "naming":
		case "be":
		case "is":
		case "are":
		case "was":
		case "were":
		case "been":
		case "have":
		case "has":
		case "had":
		case "get":
		case "gets":
		case "got":
		case "want":
		case "wants":
		case "wanted":
		case "give":
		case "gives":
		case "gave":
		case "given":
			// greetings — addressing someone named "Duck"
		case "hey":
		case "hi":
		case "hello":
		case "ahoj":
		case "cau":
		case "cus":
		case "cao":
			// pet / animal context
		case "pet":
		case "dog":
		case "cat":
		case "bird":
		case "animal":
		case "toy":
		case "zvire":
		case "ptak":
			// friend context
		case "friend":
		case "mate":
		case "buddy":
		case "pal":
		case "kamos":
		case "kamarad":
			// prepositions ("to" is already covered above as Czech demonstrative;
			// English infinitive "to duck you" is caught via contextual-phrases)
		case "with":
		case "without":
			return true;
		default:
			return false;
		}
	}

	private static boolean isProfanityObject(String token) {
		switch (token) {
		case "this":
		case "that":
		case "it":
		case "shit":
			return true;
		default:
			return false;
		}
	}

	private static boolean isProfanityPhrasal(String token) {
		switch (token) {
		case "off":
		case "up":
			return true;
		default:
			return false;
		}
	}

	private static boolean isProfanityQuestionWord(String token) {
		switch (token) {
		case "what":
		case "who":
		case "where":
		case "when":
		case "why":
		case "how":
			return true;
		default:
			return false;
		}
	}

	private static String getNextToken(String normalized, int end) {
		int length = normalized.length();
		int index = end;
		while (index < length && normalized.charAt(index) == ' ')
			index++;
		if (index >= length)
			return null;
		int nextEnd = index;
		while (nextEnd < length && normalized.charAt(nextEnd) != ' ')
			nextEnd++;
		return normalized.substring(index, nextEnd);
	}

	private static String getRawNextToken(String original, StringContainerWithPositions filtered, int filteredEnd) {
		if (filteredEnd <= 0)
			return null;
		int rawPos = filtered.posAt(filteredEnd - 1) + 1;
		while (rawPos < original.length() && Character.isWhitespace(original.charAt(rawPos)))
			rawPos++;
		if (rawPos >= original.length())
			return null;
		int rawEnd = rawPos;
		while (rawEnd < original.length() && !Character.isWhitespace(original.charAt(rawEnd)))
			rawEnd++;
		return original.substring(rawPos, rawEnd);
	}

	private static boolean isInsultTargetPronoun(String token) {
		switch (token) {
		case "me":
		case "you":
		case "us":
		case "him":
		case "her":
		case "them":
		case "they":
		case "everyone":
		case "anyone":
		case "nobody":
		case "somebody":
		case "everybody":
			return true;
		default:
			return false;
		}
	}

	private static boolean isInsultTargetNoun(String token) {
		switch (token) {
		case "lady":
		case "man":
		case "men":
		case "woman":
		case "women":
		case "boy":
		case "girl":
		case "kid":
		case "kids":
		case "dude":
		case "guys":
		case "people":
		case "player":
		case "players":
		case "admin":
		case "admins":
		case "owner":
		case "owners":
		case "mod":
		case "mods":
		case "staff":
		case "family":
		case "child":
		case "children":
			return true;
		default:
			return false;
		}
	}

	private static boolean containsDigit(String token) {
		for (int i = 0; i < token.length(); ++i)
			if (Character.isDigit(token.charAt(i)))
				return true;
		return false;
	}

	private static boolean startsWithUppercase(String token) {
		return token.length() > 0 && Character.isUpperCase(token.charAt(0));
	}

	private static boolean containsObfuscationMarkers(String token, String word) {
		if (token.length() == word.length()) {
			int mismatches = 0;
			boolean hasNonLetter = false;
			for (int i = 0; i < token.length(); ++i) {
				char tokenChar = token.charAt(i);
				char wordChar = word.charAt(i);
				if (tokenChar != wordChar) {
					if (!isObfuscationChar(tokenChar, wordChar))
						return false;
					if (++mismatches > maxAllowedMismatches(token, word))
						return false;
				}
				if (!Character.isLetter(tokenChar))
					hasNonLetter = true;
			}
			if ((mismatches == 0) || (mismatches == 1 && !hasNonLetter && !hasRepeatedChar(token)))
				return false;
			return mismatches >= 1;
		}
		return token.contains(" ") || token.matches(".*[^a-z].*") || hasRepeatedChar(token);
	}

	private static int maxAllowedMismatches(String token, String word) {
		if (token.length() <= 4)
			return 1;
		if (token.length() <= 7)
			return 2;
		return 3;
	}

	private static boolean containsCommonSubstitution(String token, String word) {
		// Same-length only on purpose: allowing a length diff of 1 would flag the
		// Czech informal "dik" (=díky/thanks) as similar to "dick" via the k↔c
		// substitution. The bypasses we want ("čokot"→"cokot"/"kokot", "fvck"/"fuck",
		// "kreten"/"creten") are all same-length swaps anyway.
		if (token.length() != word.length())
			return false;
		int matches = 0;
		for (int i = 0; i < token.length(); ++i) {
			char tokenChar = token.charAt(i);
			char wordChar = word.charAt(i);
			if (tokenChar == wordChar)
				continue;
			if (!isCommonSubstitution(tokenChar, wordChar))
				return false;
			++matches;
		}
		return matches > 0;
	}

	private static boolean isObfuscationChar(char tokenChar, char wordChar) {
		if (tokenChar == '*' || tokenChar == '$' || tokenChar == '@' || tokenChar == '!' || tokenChar == '1')
			return true;
		if (Character.isDigit(tokenChar) || !Character.isLetter(tokenChar))
			return true;
		switch (tokenChar) {
		case '0':
		case '3':
		case '4':
		case '5':
		case '7':
		case '8':
		case '9':
		case 'y':
		case 'z':
		case 'c':
		case 'v':
		case 'w':
		case 'k':
			return true;
		default:
			return false;
		}
	}

	private static boolean isCommonSubstitution(char tokenChar, char wordChar) {
		// d↔f is intentionally NOT here — it's handled in isContextualDuckInsult
		// because "duck"/"ducks" appear in plenty of legit messages ("i love ducks",
		// "the duck pond"); applying d↔f globally would flag them all.
		return tokenChar == 'y' && wordChar == 'i'
				|| tokenChar == 'i' && wordChar == 'y'
				|| tokenChar == 'k' && wordChar == 'g'
				|| tokenChar == 'g' && wordChar == 'k'
				|| tokenChar == 'c' && wordChar == 'k'
				|| tokenChar == 'k' && wordChar == 'c'
				|| tokenChar == 'v' && wordChar == 'u'
				|| tokenChar == 'u' && wordChar == 'v';
	}

	private static boolean hasRepeatedChar(String token) {
		char prev = 0;
		int repeat = 0;
		for (int i = 0; i < token.length(); ++i) {
			char c = token.charAt(i);
			if (c == prev) {
				if (++repeat >= 2)
					return true;
			} else {
				prev = c;
				repeat = 0;
			}
		}
		return false;
	}

	private static int levenshteinDistance(String a, String b, int threshold) {
		int aLength = a.length();
		int bLength = b.length();
		if (Math.abs(aLength - bLength) > threshold)
			return threshold + 1;
		if (aLength == 0)
			return bLength;
		if (bLength == 0)
			return aLength;

		int[] previous = new int[bLength + 1];
		int[] current = new int[bLength + 1];
		for (int j = 0; j <= bLength; ++j)
			previous[j] = j;

		for (int i = 1; i <= aLength; ++i) {
			current[0] = i;
			int min = current[0];
			for (int j = 1; j <= bLength; ++j) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
				min = Math.min(min, current[j]);
			}
			if (min > threshold)
				return threshold + 1;
			int[] temp = previous;
			previous = current;
			current = temp;
		}
		return previous[bLength];
	}

	@SuppressWarnings("unchecked")
	private static boolean matchesAllowedPhraseForWord(String original, StringContainerWithPositions filtered,
			String word, int pos, boolean exact, List<Pair> allowedPhrases, List<int[]> allowedSections) {
		for (Pair phrase : allowedPhrases)
			if (phrase.getKey().equals(word) && matchesAllowedPhrase(filtered, pos, (List<String>) phrase.getValue(),
					exact, allowedSections, original))
				return true;
		return false;
	}

	private static boolean matchesAllowedPhrase(StringContainerWithPositions filtered, int pos, List<String> phrases,
			boolean exact, List<int[]> allowedSections, String origin) {
		for (int[] i : allowedSections)
			if (i[0] <= pos && i[1] >= pos)
				return true;
		for (String phrase : phrases) {
			int[] index = filtered.indexOf(Math.max(0, pos - 6), phrase, false, true, origin);
			if (index != null) {
				allowedSections.add(index);
				return true;
			}
			index = filtered.indexOf(Math.max(0, pos - 6), phrase, false, false, origin);
			if (index != null) {
				allowedSections.add(index);
				return true;
			}
		}
		return false;
	}

	private static void retriveWords(String input, StringContainerWithPositions filtered, String word,
			List<Pair> allowedPhrases, boolean exact, Map<Integer, Integer> positionAndLength,
			List<int[]> allowedSections) {
		int[] pos = filtered.indexOf(word, exact, true);
		while (pos != null) {
			int start = filtered.posAt(pos[0]);
			if (!positionAndLength.containsKey(start) && !isPlainSubstringInsideWord(filtered, pos, word)
					&& !matchesAllowedPhraseForWord(input, filtered, word, pos[0], exact, allowedPhrases,
							allowedSections))
				positionAndLength.put(start, filtered.posAt(pos[1]) + 1);
			pos = filtered.indexOf(word, pos[0] + 1, exact, true);
		}
		pos = filtered.indexOf(word, exact, false);
		while (pos != null) {
			int start = filtered.posAt(pos[0]);
			if (!positionAndLength.containsKey(start) && !isPlainSubstringInsideWord(filtered, pos, word)
					&& !matchesAllowedPhraseForWord(input, filtered, word, pos[0], exact, allowedPhrases,
							allowedSections))
				positionAndLength.put(start, filtered.posAt(pos[1]) + 1);
			pos = filtered.indexOf(word, pos[0] + 1, exact, false);
		}
		retrieveSimilarWords(input, filtered, word, exact, allowedPhrases, positionAndLength, allowedSections);
	}

	private static void retrieveSimilarWords(String input, StringContainerWithPositions filtered, String word,
			boolean exact, List<Pair> allowedPhrases, Map<Integer, Integer> positionAndLength,
			List<int[]> allowedSections) {
		String normalized = filtered.toString();
		int length = normalized.length();
		int start = 0;
		while (start < length) {
			if (normalized.charAt(start) == ' ') {
				start++;
				continue;
			}
			int end = start;
			while (end < length && normalized.charAt(end) != ' ')
				end++;
			String token = normalized.substring(start, end);
			boolean match = isSimilarWord(token, word)
					|| isContextualDuckInsult(input, filtered, token, word, start, end);
			if (match && !matchesAllowedPhraseForWord(input, filtered, word, start, exact, allowedPhrases,
					allowedSections)) {
				int origStart = filtered.posAt(start);
				positionAndLength.putIfAbsent(origStart, filtered.posAt(end - 1) + 1);
			}
			start = end;
		}
	}

	// find vulgarism and replace it
	private static String antiSwearReplaceLegacy(String input, List<String> words, List<String> exactWords,
			List<Pair> allowedPhrases, int[][] ignoredSections, String replacement, boolean shouldAddColors) {
		StringContainerWithPositions filtered = filterAntiSwearInput(input, ignoredSections);

		List<int[]> allowedSections = new ArrayList<>();
		Map<Integer, Integer> positionAndLength = new HashMap<>();
		for (String word : words)
			retriveWords(input, filtered, word, allowedPhrases, true, positionAndLength, allowedSections);

		for (String word : exactWords)
			retriveWords(input, filtered, word, allowedPhrases, false, positionAndLength, allowedSections);
		if (positionAndLength.isEmpty())
			return input;
		StringContainer container = new StringContainer(input);
		ComparableObject<Integer, Integer>[] result = SortingAPI.sortByKeyArray(positionAndLength, true);
		for (ComparableObject<Integer, Integer> res : result)
			try {
				container.replace(res.getKey(), res.getValue(), replacement + (shouldAddColors ? "§g" : ""));
			} catch (Exception e) {
			}
		return container.toString();
	}

	private static StringContainerWithPositions filterAntiSwearInput(String input, int[][] ignoredSections) {
		StringContainerWithPositions filtered = new StringContainerWithPositions(input.length());

		int posOfSection = 0;
		int[] currentSection = ignoredSections == null ? null : ignoredSections[posOfSection];
		boolean lastSpace = false;
		for (int i = 0; i < input.length(); i++) {
			if (currentSection != null && currentSection[0] == i) {
				i += currentSection[1];
				if (ignoredSections!=null && ignoredSections.length != ++posOfSection)
					currentSection = ignoredSections[posOfSection];
				else
					currentSection = null;
				--i;
				continue;
			}
			lastSpace = appendAntiSwearCharacter(filtered, input.charAt(i), i, lastSpace);
		}
		return filtered;
	}

	public static String normalizeAntiSwearPhrase(String input) {
		if (input == null || input.isEmpty())
			return "";
		StringBuilder builder = new StringBuilder(input.length());
		boolean lastSpace = false;
		for (int i = 0; i < input.length(); ++i)
			lastSpace = appendAntiSwearCharacter(builder, input.charAt(i), lastSpace);
		return builder.toString().trim();
	}

	private static boolean appendAntiSwearCharacter(StringContainerWithPositions filtered, char origin, int pos,
			boolean lastSpace) {
		if (Character.isWhitespace(origin)) {
			if (!lastSpace && filtered.length() != 0)
				filtered.append(' ', pos);
			return true;
		}
		if (isIgnoredAntiSwearCharacter(origin))
			return lastSpace;
		String normalized = Normalizer.normalize(String.valueOf(Character.toLowerCase(origin)), Normalizer.Form.NFD);
		for (int i = 0; i < normalized.length(); ++i) {
			char c = normalized.charAt(i);
			if (Character.getType(c) == Character.NON_SPACING_MARK || isIgnoredAntiSwearCharacter(c))
				continue;
			filtered.append(simplifyCharacter(c), pos);
			lastSpace = false;
		}
		return lastSpace;
	}

	private static boolean appendAntiSwearCharacter(StringBuilder builder, char origin, boolean lastSpace) {
		if (Character.isWhitespace(origin)) {
			if (!lastSpace && builder.length() != 0)
				builder.append(' ');
			return true;
		}
		if (isIgnoredAntiSwearCharacter(origin))
			return lastSpace;
		String normalized = Normalizer.normalize(String.valueOf(Character.toLowerCase(origin)), Normalizer.Form.NFD);
		for (int i = 0; i < normalized.length(); ++i) {
			char c = normalized.charAt(i);
			if (Character.getType(c) == Character.NON_SPACING_MARK || isIgnoredAntiSwearCharacter(c))
				continue;
			builder.append(simplifyCharacter(c));
			lastSpace = false;
		}
		return lastSpace;
	}

	private static boolean isIgnoredAntiSwearCharacter(char origin) {
		return origin != '*' && origin != '$' && origin != '@' && !Character.isLetterOrDigit(origin);
	}

	// Removes from message flood and transfer uppercase characters to lowercase
	public static String antiFlood(String input, int[][] ignoredSections, int floodMaxNumbers, int floodMaxChars,
			int floodMaxCapsChars, int floodMaxSameWords, int floodMinWordsBetweenSameToIgnore) {
		StringContainer filtered = new StringContainer(input.length());
		char prev = 0;
		int times = 0;
		boolean inCaps = false;
		int capsTimes = 0;

		int posOfSection = 0;
		int numberTimes = 0;
		int dotTimes = 0;

		int wordPos = 0;
		Map<Integer, String> wordsInRow = new HashMap<>(floodMinWordsBetweenSameToIgnore);
		Map<String, Integer> counterOfSameWords = new HashMap<>();

		byte urlCount = 0;
		int start = 0;
		int[] currentSection = ignoredSections == null ? null : ignoredSections[posOfSection];
		charLoop: for (int i = 0; i < input.length(); i++) {
			if (currentSection != null && currentSection[0] == i) {
				for (int c = 0; c < currentSection[1]; ++c)
					filtered.append(input.charAt(i++));
				if (ignoredSections!=null && currentSection.length - 1 != ++posOfSection)
					currentSection = ignoredSections[posOfSection];
				else
					currentSection = null;
				--i;
				prev = 0;
				times = 0;
				numberTimes = 0;
				inCaps = false;
				capsTimes = 0;
				start = filtered.length();
				continue;
			}

			char origin = input.charAt(i);
			if (origin == 'w') {
				++times;
				if (urlCount <= 2) {
					filtered.append(origin);
					++urlCount;
					continue;
				}
			} else
				urlCount = 0;
			if (origin == ' ') {
				String word = filtered.substring(start).toLowerCase();
				if (filtered.charAt(filtered.length() - 1) != ' ')
					filtered.append(origin);
				inCaps = false;
				capsTimes = 0;
				numberTimes = 0;
				for (int ic = 0; ic < floodMinWordsBetweenSameToIgnore; ++ic) {
					String savedWord = wordsInRow.get(ic);
					if (!word.equals(savedWord))
						continue;
					int repeats = counterOfSameWords.getOrDefault(word, 0) + 1;
					if (repeats >= floodMaxSameWords)
						filtered.delete(start, filtered.length());
					else
						counterOfSameWords.put(word, repeats);
					start = filtered.length();
					continue charLoop;
				}
				wordsInRow.put(wordPos, word);
				if (++wordPos >= floodMinWordsBetweenSameToIgnore)
					wordPos = 0;
				start = filtered.length();
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
			boolean allowedForCapsCheck = origin >= 65 && origin <= 658;
			char c = allowedForCapsCheck ? Character.toLowerCase(origin) : origin;
			if (c == prev && ++times >= floodMaxChars)
				continue;
			if (allowedForCapsCheck && c != origin && ++capsTimes >= floodMaxCapsChars)
				inCaps = true;

			if (inCaps)
				filtered.append(inCaps ? c : origin);
			else
				filtered.append(origin);
			if (prev != c)
				times = 0;
			prev = c;
		}
		String word = filtered.substring(start).toLowerCase();
		for (int ic = 0; ic < floodMinWordsBetweenSameToIgnore; ++ic) {
			String savedWord = wordsInRow.get(ic);
			if (!word.equals(savedWord))
				continue;
			int repeats = counterOfSameWords.getOrDefault(word, 0) + 1;
			if (repeats >= floodMaxSameWords)
				filtered.delete(start, filtered.length());
			else
				counterOfSameWords.put(word, repeats);
			break;
		}
		return filtered.toString();
	}

	public static char simplifyCharacter(char c) {
		switch (c) {
		case 'é':
		case 'ě':
		case '3':
			c = 'e';
			break;
		case 'š':
		case 'ś':
		case '5':
		case 'ß':
			c = 's';
			break;
			// '$' intentionally NOT collapsed to 's' here — it's preserved into the filtered
			// text so StringContainerWithPositions.indexOf can treat it as either an 's'
			// stand-in ("$hit" → "shit") OR a skippable insertion ("K$o$k$o$t" → "kokot").
			// Collapsing to 's' up front turned inserted-'$' bypasses into gibberish that no
			// longer contained the swear.
		case 'č':
		case 'ć':
			c = 'c';
			break;
		case 'ť':
		case '1':
		case '7':
			c = 't';
			break;
		case 'ř':
		case 'ŕ':
			c = 'r';
			break;
		case 'ž':
		case 'ź':
		case 'ż':
			c = 'z';
			break;
		case 'ý':
		case 'y':
		case 'í':
			c = 'i';
			break;
		case 'á':
		case 'ä':
		case 'ą':
			c = 'a';
			break;
		case '4':
			c = 'a';
			break;
			// '@' preserved for the same reason as '$'.
		case '0':
		case 'ö':
		case 'ó':
			c = 'o';
			break;
		}
		return c;
	}
}

