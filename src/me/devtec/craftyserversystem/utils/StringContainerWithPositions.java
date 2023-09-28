package me.devtec.craftyserversystem.utils;

import java.util.Arrays;

public class StringContainerWithPositions {
	private static final int DEFAULT_CAPACITY = 16;
	private static final int MAX_ARRAY_SIZE = 2147483639;
	private transient char[] value;
	private transient int[] realPos;
	private int count;

	public StringContainerWithPositions() {
		value = new char[DEFAULT_CAPACITY];
		realPos = new int[DEFAULT_CAPACITY];
	}

	public StringContainerWithPositions(final int capacity) {
		value = new char[capacity <= 0 ? DEFAULT_CAPACITY : capacity];
		realPos = new int[capacity <= 0 ? DEFAULT_CAPACITY : capacity];
	}

	public int length() {
		return count;
	}

	public void ensureCapacity(final int minimumCapacity) {
		if (minimumCapacity > 0)
			ensureCapacityInternal(minimumCapacity);
	}

	private void ensureCapacityInternal(final int minimumCapacity) {
		if (minimumCapacity - value.length > 0) {
			value = Arrays.copyOf(value, newCapacity(minimumCapacity));
			realPos = Arrays.copyOf(realPos, newCapacity(minimumCapacity));
		}
	}

	private int newCapacity(final int minCapacity) {
		int newCapacity = (value.length << 1) + 2;
		if (newCapacity - minCapacity < 0)
			newCapacity = minCapacity;
		return newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0 ? hugeCapacity(minCapacity) : newCapacity;
	}

	private int hugeCapacity(final int minCapacity) {
		if (Integer.MAX_VALUE - minCapacity < 0)
			throw new OutOfMemoryError();
		return minCapacity > MAX_ARRAY_SIZE ? minCapacity : MAX_ARRAY_SIZE;
	}

	public char charAt(final int index) {
		return value[index];
	}

	public int posAt(final int index) {
		return realPos[index];
	}

	public StringContainerWithPositions append(final char c, int pos) {
		ensureCapacityInternal(count + 1);
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

	public int indexOf(final String value) {
		return this.indexOf(value, 0);
	}

	public int indexOf(final String value, final int start) {
		return this.indexOf(start, value);
	}

	protected int indexOf(final int start, final String lookingFor) {
		final int min = Math.min(start, count);
		final int size = lookingFor.length();
		if (min + size > count)
			return -1;
		final char firstChar = lookingFor.charAt(0);
		for (int i = min; i < count; ++i)
			if (value[i] == firstChar) {
				++i;
				for (int foundPos = 1; i < count && value[i] == lookingFor.charAt(foundPos); ++i)
					if (++foundPos == size)
						return i - (size - 1);
			}
		return -1;
	}

	public void increaseCount(final int newCount) {
		count += newCount;
	}

	public boolean isEmpty() {
		return length() == 0;
	}
}