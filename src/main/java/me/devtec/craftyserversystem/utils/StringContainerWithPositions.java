package me.devtec.craftyserversystem.utils;

import java.util.Arrays;

public class StringContainerWithPositions {
	private transient char[] value;
	private transient int[] realPos;
	private int count;

	public StringContainerWithPositions(final int capacity) {
		value = new char[capacity];
		realPos = new int[capacity];
	}

	public int length() {
		return count;
	}

	public char charAt(final int index) {
		return value[index];
	}

	public int posAt(final int index) {
		return realPos[index];
	}

	public void setPosAt(final int index, int pos) {
		realPos[index] = pos;
	}

	public StringContainerWithPositions append(final char c, int pos) {
		value[count] = c;
		realPos[count++] = pos;
		return this;
	}

	public char[] getValue() {
		if (count < value.length)
			value = Arrays.copyOf(value, count);
		return value;
	}

	@Override
	public String toString() {
		return new String(value, 0, count);
	}

	public int indexOf(final char c) {
		return this.indexOf(c, 0);
	}

	public int indexOf(final char c, final int start) {
		for (int i = Math.min(start, count); i < count; ++i)
			if (value[i] == c)
				return i;
		return -1;
	}

	public int[] indexOf(final String value, boolean ignoreSpaces, boolean removeSequentialDuplicates) {
		return this.indexOf(value, 0, ignoreSpaces, removeSequentialDuplicates);
	}

	public int[] indexOf(final String value, final int start, boolean ignoreSpaces,
			boolean removeSequentialDuplicates) {
		return this.indexOf(start, value, ignoreSpaces, removeSequentialDuplicates, null);
	}

	protected int[] indexOf(final int start, final String lookingFor, boolean ignoreSpaces,
			boolean removeSequentialDuplicates, String original) {
		int min = Math.min(start, count);
		int size = lookingFor.length();
		if (min + size > count)
			return null;

		char firstChar = lookingFor.charAt(0);
		char prev = 0;
		for (int i = min; i < count; ++i) {
			char c = original == null ? value[i] : original.charAt(realPos[i]);
			if (ignoreSpaces && Character.isWhitespace(c))
				continue;
			// $/@ act as their letter counterpart ('s'/'a') when they appear at the
			// start of a swear ("$hit" → "shit"); '*' is a general wildcard start.
			boolean startsWildcard = c == '*'
					|| c == '$' && firstChar == 's'
					|| c == '@' && firstChar == 'a';
			if (c == firstChar || startsWildcard) {
				prev = c;
				byte countStars = (byte) (c == '*' ? 1 : 0);
				boolean hasObfuscation = startsWildcard;
				int extrasUsed = 0;
				int foundPos = 1;
				for (int d = ++i; d < count; ++d) {
					char e = original == null ? value[d] : original.charAt(realPos[d]);
					if (ignoreSpaces && Character.isWhitespace(e))
						continue;
					if (foundPos == size)
						return new int[] { i - 1, d - 1 };
					char expected = lookingFor.charAt(foundPos);
					// $ / @ inside a match: match their letter if that fits the pattern,
					// otherwise they're silent — swallowed like whitespace with
					// ignoreSpaces. This is what catches "K$o$k$o$t" against "kokot".
					if (e == '$' || e == '@') {
						hasObfuscation = true;
						char asLetter = e == '$' ? 's' : 'a';
						if (asLetter == expected && ++foundPos == size)
							return new int[] { i - 1, d };
						prev = e;
						continue;
					}
					if (e == '*' && ++countStars <= 2) {
						hasObfuscation = true;
						if (++foundPos == size)
							return new int[] { i - 1, d };
					} else if (e == expected) {
						if (++foundPos == size)
							return new int[] { i - 1, d };
					} else if (removeSequentialDuplicates && e == prev && !Character.isWhitespace(e)) {
						continue;
					} else if (hasObfuscation && extrasUsed < 1 && Character.isLetter(e)) {
						// Once we're already inside an obfuscated match, tolerate one
						// spurious letter ("K$o$k$xo$t" against "kokot"). Gating on
						// prior obfuscation stops random typos from being treated
						// as swears.
						++extrasUsed;
						prev = e;
						continue;
					} else {
						break;
					}
					prev = e;
				}
			}
			prev = c;
		}
		return null;
	}

	public void increaseCount(final int newCount) {
		count += newCount;
	}

	public boolean isEmpty() {
		return length() == 0;
	}
}