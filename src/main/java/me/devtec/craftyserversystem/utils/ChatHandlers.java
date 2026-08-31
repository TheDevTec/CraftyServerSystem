package me.devtec.craftyserversystem.utils;

import java.text.Normalizer;
import java.util.*;

import me.devtec.shared.dataholder.StringContainer;

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
			if (checkedWords.size() == 1 && suspiciousWords == 1 || checkedWords.size() == 2 && suspiciousWords == 1)
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

		if (maxConsonantsInRow >= 5 || maxVowelsInRow >= 4 || word.length() >= 8 && suspiciousBigrams >= 2)
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
		private final boolean replaceWholeWord;
		private final Set<String> languages;
		private final Set<String> collisionLanguages;

		public BadWordRule(String word, boolean matchInsideWord, Set<String> languages,
				Set<String> collisionLanguages) {
			this(word, matchInsideWord, false, languages, collisionLanguages);
		}

		private BadWordRule(String word, boolean matchInsideWord, boolean replaceWholeWord, Set<String> languages,
				Set<String> collisionLanguages) {
			this.word = normalizeRuleWord(word);
			this.matchInsideWord = matchInsideWord;
			this.replaceWholeWord = replaceWholeWord;
			this.languages = normalizeLanguageSet(languages);
			this.collisionLanguages = normalizeLanguageSet(collisionLanguages);
		}

		public String getWord() {
			return word;
		}

		public boolean isMatchInsideWord() {
			return matchInsideWord;
		}

		public boolean isReplaceWholeWord() {
			return replaceWholeWord;
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

	public static final class LanguageProfile {
		private final byte[] scores = new byte[LANGUAGE_CODES.length];

		public void learn(String input) {
			int mask = detectLanguageMask(input);
			if (mask == 0)
				return;
			int increment = Integer.bitCount(mask) == 1 ? 2 : 1;
			for (int i = 0; i < scores.length; ++i)
				if ((mask & 1 << i) != 0)
					scores[i] = (byte) Math.min(32, scores[i] + increment);
		}

		public Set<String> getLanguages() {
			Set<String> languages = new LinkedHashSet<>();
			for (int i = 0; i < scores.length; ++i)
				if (scores[i] >= 3)
					languages.add(LANGUAGE_CODES[i]);
			return languages;
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
			boolean stem = value.startsWith("+");
			boolean anywhere = stem || value.startsWith("*");
			return new BadWordRule(anywhere ? value.substring(1) : value, anywhere, stem,
					Collections.<String>emptySet(),
					Collections.<String>emptySet());
		}
		String word = parts[2].trim();
		boolean stem = word.startsWith("+");
		boolean anywhere = stem || word.startsWith("*");
		return new BadWordRule(anywhere ? word.substring(1) : word, anywhere, stem, parseLanguages(parts[0]),
				parseLanguages(parts[1]));
	}

	public static ProfanityResult checkProfanity(String input, List<BadWordRule> rules,
			List<AllowedPhraseRule> allowedPhrases, int[][] ignoredSections) {
		return checkProfanity(input, rules, allowedPhrases, ignoredSections, Collections.<String>emptySet());
	}

	public static ProfanityResult checkProfanity(String input, List<BadWordRule> rules,
			List<AllowedPhraseRule> allowedPhrases, int[][] ignoredSections, Set<String> profileLanguages) {
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
			if ((candidate.type == ProfanityMatchType.OBFUSCATED && !isObfuscatedJoin(input, candidate)) || isAllowedPhrase(input, candidate, allowedPhrases))
				continue;
			ProfanityDecision decision = ProfanityDecision.MATCH;
			if (!candidate.rule.languages.isEmpty() || !candidate.rule.collisionLanguages.isEmpty()) {
				String ruleLanguage = detectRuleDiacritics(input, candidate.rule);
				if (ruleLanguage != null)
					detectedLanguage = ruleLanguage;
				else if (detectedLanguage == null)
					detectedLanguage = detectChatLanguage(input);
				if (detectedLanguage == null && profileLanguages != null && !profileLanguages.isEmpty()) {
					String profileMatch = null;
					boolean profileCollision = false;
					for (String language : profileLanguages) {
						if (candidate.rule.collisionLanguages.contains(language))
							profileCollision = true;
						if (candidate.rule.languages.contains(language))
							profileMatch = language;
					}
					if (profileMatch != null && !profileCollision)
						detectedLanguage = profileMatch + " (profile)";
					else if (profileCollision && profileMatch == null) {
						detectedLanguage = "profile-collision";
						decision = ProfanityDecision.IGNORE;
					}
				}
				if (detectedLanguage == null)
					decision = ProfanityDecision.REVIEW;
				else if (decision != ProfanityDecision.IGNORE && ruleLanguage == null && !detectedLanguage.endsWith(" (profile)")
						&& ((!candidate.rule.languages.isEmpty() && !containsDetectedLanguage(candidate.rule.languages, detectedLanguage))
								|| containsDetectedLanguage(candidate.rule.collisionLanguages, detectedLanguage)))
					decision = ProfanityDecision.IGNORE;
			}
			ProfanityMatchType type = candidate.type;
			String original = input.substring(candidate.start, candidate.end);
			if (type == ProfanityMatchType.NORMALIZED && original.equalsIgnoreCase(candidate.rule.word))
				type = ProfanityMatchType.EXACT;
			matches.add(new ProfanityMatch(candidate.rule, candidate.start, candidate.end, original, type, decision,
					detectedLanguage == null && candidate.rule.languages.isEmpty() && candidate.rule.collisionLanguages.isEmpty() ? "global" : detectedLanguage));
		}
		return new ProfanityResult(matches);
	}

	private static String detectRuleDiacritics(String input, BadWordRule rule) {
		if ((!rule.languages.contains("cs") && !rule.languages.contains("sk")) || input == null)
			return null;
		for (int i = 0; i < input.length(); ++i)
			switch (Character.toLowerCase(input.charAt(i))) {
			case '\u010d': case '\u010f': case '\u011b': case '\u0148': case '\u0159': case '\u0161': case '\u0165': case '\u016f': case '\u017e':
				return "cs/sk";
			case '\u013e': case '\u013a': case '\u0155': case '\u00e4': case '\u00f4':
				return "sk";
			default:
				break;
			}
		return null;
	}

	private static boolean containsDetectedLanguage(Set<String> languages, String detectedLanguage) {
		if (detectedLanguage == null)
			return false;
		String normalized = detectedLanguage.replace(" (profile)", "");
		for (String language : normalized.split("/"))
			if (languages.contains(language))
				return true;
		return false;
	}

	// Checks a word completed by the current message.
	public static ProfanityResult checkSplitProfanity(List<String> history, String current, List<BadWordRule> rules,
			List<AllowedPhraseRule> allowedPhrases) {
		return checkSplitProfanity(history, current, rules, allowedPhrases, Collections.<String>emptySet());
	}

	public static ProfanityResult checkSplitProfanity(List<String> history, String current, List<BadWordRule> rules,
			List<AllowedPhraseRule> allowedPhrases, Set<String> profileLanguages) {
		if (history == null || history.isEmpty() || current == null || current.isEmpty() || rules == null || rules.isEmpty())
			return new ProfanityResult(Collections.<ProfanityMatch>emptyList());

		int limit = 2;
		for (BadWordRule rule : rules)
			if (rule != null)
				limit = Math.max(limit, rule.word.length() + 2);
		StringBuilder previous = new StringBuilder();
		for (String message : history)
			if (message != null)
				previous.append(message);
		if (previous.length() > limit)
			previous.delete(0, previous.length() - limit);

		int boundary = previous.length();
		if (boundary == 0)
			return new ProfanityResult(Collections.<ProfanityMatch>emptyList());
		ProfanityResult combined = checkProfanity(previous.append(current).toString(), rules, allowedPhrases, null, profileLanguages);
		List<ProfanityMatch> matches = new ArrayList<>();
		for (ProfanityMatch match : combined.getMatches())
			if (match.getStart() < boundary && match.getEnd() > boundary) {
				int end = Math.min(current.length(), match.getEnd() - boundary);
				if (end > 0)
					matches.add(new ProfanityMatch(match.getRule(), 0, end, current.substring(0, end), match.getType(),
							match.getDecision(), match.getLanguage()));
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
			addCandidate(normalized, rule, type, candidates, at, rule.word.length());
			at = normalized.text.indexOf(rule.word, at + 1);
		}
		findMissingVowelCandidates(normalized, rule, type, candidates);
	}

	private static void findMissingVowelCandidates(NormalizedText normalized, BadWordRule rule, ProfanityMatchType type,
			Map<String, Candidate> candidates) {
		if (rule.word.length() < 5)
			return;
		Set<String> variants = new HashSet<>();
		for (int i = 1; i < rule.word.length() - 1; ++i)
			if (isVowel(rule.word.charAt(i)))
				variants.add(rule.word.substring(0, i) + rule.word.substring(i + 1));
		for (String variant : variants) {
			int at = normalized.text.indexOf(variant);
			while (at != -1) {
				addCandidate(normalized, rule, type, candidates, at, variant.length());
				at = normalized.text.indexOf(variant, at + 1);
			}
		}
	}

	private static boolean isVowel(char character) {
		return character == 'a' || character == 'e' || character == 'i' || character == 'o' || character == 'u' || character == 'y';
	}

	private static void addCandidate(NormalizedText normalized, BadWordRule rule, ProfanityMatchType type,
			Map<String, Candidate> candidates, int at, int length) {
		int end = at + length;
		if (!rule.matchInsideWord && !hasWordStart(normalized.text, at))
			return;
		int originalStart = normalized.starts[at];
		int originalEnd = normalized.ends[end - 1];
		if (rule.replaceWholeWord) {
			originalStart = findOriginalWordStart(normalized, at);
			originalEnd = findOriginalWordEnd(normalized, end);
		}
		String key = originalStart + ":" + originalEnd + ":" + rule.word;
		Candidate existing = candidates.get(key);
		if (existing == null || existing.type == ProfanityMatchType.OBFUSCATED && type != ProfanityMatchType.OBFUSCATED)
			candidates.put(key, new Candidate(rule, originalStart, originalEnd, type));
	}

	private static boolean hasWordStart(String text, int at) {
		return at == 0 || text.charAt(at - 1) == ' ';
	}

	private static int findOriginalWordStart(NormalizedText normalized, int at) {
		while (at > 0 && normalized.ends[at - 1] == normalized.starts[at])
			--at;
		return normalized.starts[at];
	}

	private static int findOriginalWordEnd(NormalizedText normalized, int end) {
		while (end < normalized.text.length() && normalized.starts[end] == normalized.ends[end - 1])
			++end;
		return normalized.ends[end - 1];
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
				int phraseStart = normalized.starts[at];
				int phraseEnd = normalized.ends[end - 1];
				if (isOriginalWordBounded(input, phraseStart, phraseEnd)
						&& candidate.start >= phraseStart && candidate.end <= phraseEnd)
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

	private static final String[] LANGUAGE_CODES = {
			"bg", "cs", "pl", "hr", "sk", "de", "da", "nl", "el", "en", "es", "fi", "fr", "hu", "it", "lt", "lv", "pt", "ro", "ru", "sr", "sv", "tr"
	};
	private static final Map<String, Integer> LANGUAGE_TOKENS = createLanguageTokens();

	private static Map<String, Integer> createLanguageTokens() {
		Map<String, Integer> tokens = new HashMap<>();
		addLanguageTokens(tokens, "bg", "здравей", "какво", "това", "защо", "добре", "хора", "благодаря", "няма", "съм");
		addLanguageTokens(tokens, "cs", "ahoj", "čau", "zdar", "lidi", "jsem", "jsi", "jste", "kde", "proč", "díky", "prosím", "něco", "tenhle");
		addLanguageTokens(tokens, "pl", "cześć", "jestem", "dlaczego", "dziękuję", "ludzie", "który", "dobrze", "proszę", "nie");
		addLanguageTokens(tokens, "hr", "bok", "ljudi", "sam", "što", "kako", "zašto", "hvala", "nije", "dobro");
		addLanguageTokens(tokens, "sk", "ahoj", "čau", "ľudia", "som", "si", "ste", "kde", "prečo", "ďakujem", "prosím", "niečo", "tento");
		addLanguageTokens(tokens, "de", "hallo", "was", "ist", "das", "wie", "warum", "danke", "ich", "nicht", "bitte");
		addLanguageTokens(tokens, "da", "hej", "hvad", "det", "hvordan", "hvorfor", "tak", "ikke", "jeg", "godt");
		addLanguageTokens(tokens, "nl", "hallo", "wat", "is", "dit", "hoe", "waarom", "bedankt", "niet", "jij");
		addLanguageTokens(tokens, "el", "γεια", "τι", "είναι", "αυτό", "πώς", "γιατί", "ευχαριστώ", "όχι", "καλά");
		addLanguageTokens(tokens, "en", "hello", "what", "is", "this", "how", "why", "thanks", "the", "and", "you", "your", "not", "people");
		addLanguageTokens(tokens, "es", "hola", "qué", "esto", "cómo", "porqué", "gracias", "gente", "bien", "usted", "no");
		addLanguageTokens(tokens, "fi", "hei", "mitä", "tämä", "miten", "miksi", "kiitos", "ihmiset", "hyvin", "en");
		addLanguageTokens(tokens, "fr", "bonjour", "quoi", "est", "ceci", "comment", "pourquoi", "merci", "gens", "bien", "pas");
		addLanguageTokens(tokens, "hu", "szia", "mi", "ez", "hogyan", "miért", "köszönöm", "emberek", "jól", "nem");
		addLanguageTokens(tokens, "it", "ciao", "cosa", "questo", "come", "perché", "grazie", "gente", "bene", "non");
		addLanguageTokens(tokens, "lt", "labas", "kas", "tai", "kaip", "kodėl", "ačiū", "žmonės", "gerai", "ne");
		addLanguageTokens(tokens, "lv", "sveiki", "kas", "tas", "kā", "kāpēc", "paldies", "cilvēki", "labi", "nē");
		addLanguageTokens(tokens, "pt", "olá", "que", "isto", "como", "porquê", "obrigado", "pessoas", "bem", "não");
		addLanguageTokens(tokens, "ro", "salut", "ce", "asta", "cum", "de ce", "mulțumesc", "oameni", "bine", "nu", "este");
		addLanguageTokens(tokens, "ru", "привет", "что", "это", "как", "почему", "спасибо", "люди", "хорошо", "нет");
		addLanguageTokens(tokens, "sr", "здраво", "šta", "шта", "ово", "kako", "како", "zašto", "зашто", "hvala", "хвала", "ljudi", "људи", "nije", "није");
		addLanguageTokens(tokens, "sv", "hej", "vad", "det", "hur", "varför", "tack", "människor", "bra", "inte");
		addLanguageTokens(tokens, "tr", "merhaba", "ne", "bu", "nasıl", "neden", "teşekkürler", "insanlar", "iyi", "değil");
		return Collections.unmodifiableMap(tokens);
	}

	private static void addLanguageTokens(Map<String, Integer> tokens, String language, String... words) {
		int bit = 1 << languageIndex(language);
		for (String word : words)
			tokens.put(word, tokens.getOrDefault(word, 0) | bit);
	}

	private static int languageIndex(String language) {
		for (int i = 0; i < LANGUAGE_CODES.length; ++i)
			if (LANGUAGE_CODES[i].equals(language))
				return i;
		return -1;
	}

	private static String detectChatLanguage(String input) {
		int mask = detectLanguageMask(input);
		if (mask == 0)
			return null;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < LANGUAGE_CODES.length; ++i)
			if ((mask & 1 << i) != 0) {
				if (result.length() != 0)
					result.append('/');
				result.append(LANGUAGE_CODES[i]);
			}
		return result.toString();
	}

	private static int detectLanguageMask(String input) {
		if (input == null || input.length() < 3)
			return 0;
		String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
		byte[] scores = new byte[LANGUAGE_CODES.length];
		StringBuilder token = new StringBuilder(16);
		for (int i = 0; i <= normalized.length(); ++i) {
			char character = i == normalized.length() ? ' ' : normalized.charAt(i);
			if (Character.isLetter(character)) {
				if (token.length() < 32)
					token.append(character);
				scoreLanguageCharacter(scores, character);
				continue;
			}
			if (token.length() != 0) {
				Integer mask = LANGUAGE_TOKENS.get(token.toString());
				if (mask != null)
					for (int language = 0; language < scores.length; ++language)
						if ((mask & 1 << language) != 0 && scores[language] < Byte.MAX_VALUE)
							++scores[language];
				token.setLength(0);
			}
		}
		int best = 0;
		for (byte score : scores)
			best = Math.max(best, score);
		if (best == 0)
			return 0;
		int result = 0;
		for (int i = 0; i < scores.length; ++i)
			if (scores[i] == best)
				result |= 1 << i;
		return result;
	}

	private static void scoreLanguageCharacter(byte[] scores, char character) {
		String languages = null;
		switch (character) {
		case 'ě': case 'ř': case 'ů': languages = "cs"; break;
		case 'ľ': case 'ĺ': case 'ŕ': case 'ô': languages = "sk"; break;
		case 'ą': case 'ę': case 'ł': case 'ź': case 'ż': languages = "pl"; break;
		case 'ă': case 'î': case 'ș': case 'ț': languages = "ro"; break;
		case 'ő': case 'ű': languages = "hu"; break;
		case 'ė': case 'ų': languages = "lt"; break;
		case 'ā': case 'ģ': case 'ķ': case 'ļ': case 'ņ': languages = "lv"; break;
		case 'ğ': case 'ı': languages = "tr"; break;
		default:
			if (character >= '\u0370' && character <= '\u03ff')
				languages = "el";
			break;
		}
		if (languages != null) {
			int index = languageIndex(languages);
			if (index >= 0 && scores[index] <= Byte.MAX_VALUE - 2)
				scores[index] += 2;
		}
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

	public static String normalizeAntiSwearPhrase(String input) {
		if (input == null || input.isEmpty())
			return "";
		StringBuilder builder = new StringBuilder(input.length());
		boolean lastSpace = false;
		for (int i = 0; i < input.length(); ++i)
			lastSpace = appendAntiSwearCharacter(builder, input.charAt(i), lastSpace);
		return builder.toString().trim();
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
			int floodMaxCapsChars, int floodMaxSameWords, int floodMinWordsBetweenSameToIgnore,
			int floodMaxPatternRepeats) {
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
				collapseRepeatedPattern(filtered, start, floodMaxPatternRepeats);
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
		collapseRepeatedPattern(filtered, start, floodMaxPatternRepeats);
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

	// Limits repeated short patterns in one word
	private static void collapseRepeatedPattern(StringContainer text, int start, int maximumRepeats) {
		int length = text.length() - start;
		if (maximumRepeats < 1 || length < 8)
			return;
		for (int patternLength = 2; patternLength <= 4; ++patternLength) {
			int repeats = length / patternLength;
			if (repeats < 4 || repeats <= maximumRepeats)
				continue;
			boolean repeated = true;
			for (int index = start; index < text.length(); ++index)
				if (!Character.isLetter(text.charAt(index)) || Character.toLowerCase(text.charAt(index)) != Character
						.toLowerCase(text.charAt(start + (index - start) % patternLength))) {
					repeated = false;
					break;
				}
			if (repeated)
				text.delete(start + patternLength * maximumRepeats, text.length());
			return;
		}
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

