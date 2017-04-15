/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maizer.text.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import android.text.Spanned;
import android.text.style.LineBackgroundSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.util.Log;

// XXX these should be changed to reflect the actual memory allocator we use.
// it looks like right now objects want to be powers of 2 minus 8
// and the array size eats another 4 bytes

/**
 * ArrayUtils contains some methods that you can call to find out the most
 * efficient increments by which to grow arrays.
 */
public class ArrayUtils {
	private static Object[] EMPTY = new Object[0];
	private static final int CACHE_SIZE = 73;
	private static final String TAG = ArrayUtils.class.getCanonicalName();
	private static Object[] sCache = new Object[CACHE_SIZE];

	private ArrayUtils() {
		/* cannot be instantiated */ }

	public static int idealByteArraySize(int need) {
		for (int i = 4; i < 32; i++)
			if (need <= (1 << i) - 12)
				return (1 << i) - 12;

		return need;
	}

	public static int idealBooleanArraySize(int need) {
		return idealByteArraySize(need);
	}

	public static int idealShortArraySize(int need) {
		return idealByteArraySize(need * 2) / 2;
	}

	public static int idealCharArraySize(int need) {
		return idealByteArraySize(need * 2) / 2;
	}

	public static int idealIntArraySize(int need) {
		return idealByteArraySize(need * 4) / 4;
	}

	public static int idealFloatArraySize(int need) {
		return idealByteArraySize(need * 4) / 4;
	}

	public static int idealObjectArraySize(int need) {
		return idealByteArraySize(need * 4) / 4;
	}

	public static int idealLongArraySize(int need) {
		return idealByteArraySize(need * 8) / 8;
	}

	/**
	 * Checks if the beginnings of two byte arrays are equal.
	 *
	 * @param array1
	 *            the first byte array
	 * @param array2
	 *            the second byte array
	 * @param length
	 *            the number of bytes to check
	 * @return true if they're equal, false otherwise
	 */
	public static boolean equals(byte[] array1, byte[] array2, int length) {
		if (array1 == array2) {
			return true;
		}
		if (array1 == null || array2 == null || array1.length < length || array2.length < length) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			if (array1[i] != array2[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns an empty array of the specified type. The intent is that it will
	 * return the same empty array every time to avoid reallocation, although
	 * this is not guaranteed.
	 */
	public static <T> T[] emptyArray(Class<T> kind) {
		if (kind == Object.class) {
			return (T[]) EMPTY;
		}

		int bucket = ((System.identityHashCode(kind) / 8) & 0x7FFFFFFF) % CACHE_SIZE;
		Object cache = sCache[bucket];

		if (cache == null || cache.getClass().getComponentType() != kind) {
			cache = Array.newInstance(kind, 0);
			sCache[bucket] = cache;

			// Log.e("cache", "new empty " + kind.getName() + " at " + bucket);
		}

		return (T[]) cache;
	}

	/**
	 * Checks that value is present as at least one of the elements of the
	 * array.
	 * 
	 * @param array
	 *            the array to check in
	 * @param value
	 *            the value to check for
	 * @return true if the value is present in the array
	 */
	public static <T> boolean contains(T[] array, T value) {
		for (T element : array) {
			if (element == null) {
				if (value == null)
					return true;
			} else {
				if (value != null && element.equals(value))
					return true;
			}
		}
		return false;
	}

	public static boolean contains(int[] array, int value) {
		for (int element : array) {
			if (element == value) {
				return true;
			}
		}
		return false;
	}

	private static int stars =0;
    @SuppressWarnings("unchecked")
    public static <T> T[] removeEmptySpans(T[] spans, Spanned spanned, Class<T> klass) {
        T[] copy = null;
        int count = 0;

        for (int i = 0; i < spans.length; i++) {
            final T span = spans[i];
            final int start = spanned.getSpanStart(span);
            final int end = spanned.getSpanEnd(span);

            if (start == end) {
                if (copy == null) {
                    copy = (T[]) Array.newInstance(klass, spans.length - 1);
                    System.arraycopy(spans, 0, copy, 0, i);
                    count = i;
                }
            } else {
                if (copy != null) {
                    copy[count] = span;
                    count++;
                }
            }
        }

        if (copy != null) {
            T[] result = (T[]) Array.newInstance(klass, count);
            System.arraycopy(copy, 0, result, 0, count);
            return result;
        } else {
            return spans;
        }
    }

	private static long[] newUnpaddedLongArray(int growSize) {
		return new long[growSize];
	}

	public static <T> T[] newUnpaddedArray(Class<T> class1, int i) {
		if(class1 == LineBackgroundSpan.class){
			LineBackgroundSpan[] value = new LineBackgroundSpan[i];
			return (T[]) value;
		}
		return null;
	}

	public static int[] newUnpaddedIntArray(int minLen) {
		return new int[minLen];
	}

	public static boolean[] newUnpaddedBooleanArray(int growSize) {
		return new boolean[growSize];
	}

	public static char[] newUnpaddedCharArray(int len) {
		return new char[len];
	}

	public static boolean doesNotNeedBidi(char[] mChars2, int i, int len) {
		return false;
	}

	public static int growSize(int want) {
		return want <= 4 ? 8 : want * 2;
	}

	public static long packRangeInLong(int start, int end) {
		return (((long) start) << 32) | end;
	}

	public static int unpackRangeStartFromLong(long range) {
		return (int) (range >>> 32);
	}

	public static int unpackRangeEndFromLong(long range) {
		return (int) (range & 0x00000000FFFFFFFFL);
	}

	public static <T> T[] append(T[] array, int currentSize, T element) {
		assert currentSize <= array.length;

		if (currentSize + 1 > array.length) {
			@SuppressWarnings("unchecked")
			T[] newArray = ArrayUtils.newUnpaddedArray((Class<T>) array.getClass().getComponentType(),
					growSize(currentSize));
			System.arraycopy(array, 0, newArray, 0, currentSize);
			array = newArray;
		}
		array[currentSize] = element;
		return array;
	}

	public static long[] append(long[] array, int currentSize, long element) {
		assert currentSize <= array.length;

		if (currentSize + 1 > array.length) {
			long[] newArray = newUnpaddedLongArray(growSize(currentSize));
			System.arraycopy(array, 0, newArray, 0, currentSize);
			array = newArray;
		}
		array[currentSize] = element;
		return array;
	}

	public static int[] append(int[] array, int currentSize, int element) {
		assert currentSize <= array.length;

		if (currentSize + 1 > array.length) {
			int[] newArray = newUnpaddedIntArray(growSize(currentSize));
			System.arraycopy(array, 0, newArray, 0, currentSize);
			array = newArray;
		}
		array[currentSize] = element;
		return array;
	}

	public static boolean[] append(boolean[] array, int currentSize, boolean element) {
		assert currentSize <= array.length;

		if (currentSize + 1 > array.length) {
			boolean[] newArray = newUnpaddedBooleanArray(growSize(currentSize));
			System.arraycopy(array, 0, newArray, 0, currentSize);
			array = newArray;
		}
		array[currentSize] = element;
		return array;
	}

	private static final LinkedList<float[]> BUFFERCHARS = new LinkedList<float[]>();

	public static void recycle(float[] buffer) {
		synchronized (BUFFERCHARS) {
			if (BUFFERCHARS.size() < 3) {
				BUFFERCHARS.add(buffer);
			}
		}
	}

	public static float[] obtainFloat(int contextLen) {
		float[] buffer;
		synchronized (BUFFERCHARS) {
			if (BUFFERCHARS.isEmpty()) {
				buffer = new float[contextLen];
			} else {
				buffer = BUFFERCHARS.getLast();
				BUFFERCHARS.removeLast();
				if (buffer.length < contextLen) {
					buffer = Arrays.copyOf(buffer, contextLen);
				}
			}
		}
		return buffer;
	}
}
