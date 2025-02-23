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
		if (original == null) {
			int min = Math.min(start, count);
			int size = lookingFor.length();
			if (min + size > count)
				return null;

			char firstChar = lookingFor.charAt(0);
			char prev = 0;
			for (int i = min; i < count; ++i) {
				char c = value[i];
				if (ignoreSpaces && Character.isWhitespace(c))
					continue;
				if (c == firstChar) {
					++i;
					int foundPos = 1;
					for (int d = i; d < count; ++d) {
						char e = value[d];
						if (ignoreSpaces && Character.isWhitespace(e))
							continue;
						if (e == lookingFor.charAt(foundPos)) {
							if (++foundPos == size)
								return new int[] { i - 1, d };
						} else if (removeSequentialDuplicates && e == prev && !Character.isWhitespace(e))
							continue;
						else
							break;
						prev = e;
					}
				}
				prev = c;
			}
		} else {
			int min = Math.min(start, count);
			int size = lookingFor.length();
			if (min + size > count)
				return null;

			char firstChar = lookingFor.charAt(0);
			char prev = 0;
			for (int i = min; i < count; ++i) {
				char c = original.charAt(realPos[i]);
				if (ignoreSpaces && Character.isWhitespace(c))
					continue;
				if (c == firstChar) {
					++i;
					int foundPos = 1;
					for (int d = i; d < count; ++d) {
						char e = original.charAt(realPos[d]);
						if (ignoreSpaces && Character.isWhitespace(e))
							continue;
						if (e == lookingFor.charAt(foundPos)) {
							if (++foundPos == size)
								return new int[] { i - 1, d };
						} else if (removeSequentialDuplicates && e == prev && !Character.isWhitespace(e))
							continue;
						else
							break;
						prev = e;
					}
				}
				prev = c;
			}
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