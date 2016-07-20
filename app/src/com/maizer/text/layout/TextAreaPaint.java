package com.maizer.text.layout;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;

import com.maizer.text.util.ArrayUtils;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.style.UnderlineSpan;
import android.util.Log;

public class TextAreaPaint extends TextPaint {

	public static final int CURSOR_AT_OR_AFTER = 1;
	public static final int CURSOR_AFTER = 0;
	public static final int CURSOR_BEFORE = 2;
	public static final int CURSOR_AT_OR_BEFORE = 3;
	public static final int DIRECTION_LTR = 0;
	public static final int DIRECTION_RTL = 1;
	private static final String TAG = TextAreaPaint.class.getSimpleName();

	private int underlineColor;
	private int underlineThickness;
	private boolean underlineEnable;

	private float mLetterSpac;

	private Locale mLocale;

	public TextAreaPaint() {
		super();
	}

	public TextAreaPaint(int flags) {
		super(flags);
	}

	public TextAreaPaint(Paint p) {
		super(p);
	}

	public void setUnderLineColor(int color) {
		underlineColor = color;
	}

	public int getUnderLineColor() {
		return underlineColor;
	}

	public void setUnderlineText(boolean underlineText) {
		underlineEnable = underlineText;
	}

	public boolean isUnderlineTextEnable() {
		return underlineEnable;
	}

	public void setLetterSpacing(float spac) {
		mLetterSpac = spac;
	}

	public float getLetterSpacing() {
		return mLetterSpac;
	}

	public boolean hasLetterSpac() {
		return mLetterSpac != 0;
	}

	public int getTextRunCursor(char[] arg, int arg1, int arg2, int arg3, int arg4, int arg5) {
		return 0;
	}

	public int getTextRunCursor(CharSequence arg, int arg1, int arg2, int arg3, int arg4, int arg5) {
		return 0;
	}

	public Locale getTextLocale() {
		return mLocale;
	}

	@SuppressLint("NewApi")
	public void setTextLocale(Locale locale) {
		try {
			super.setTextLocale(locale);
		} catch (Exception e) {
		} catch (Error e) {
		}
		mLocale = locale;
	}

	@Override
	public void set(TextPaint tp) {
		super.set(tp);
		if (tp instanceof TextAreaPaint) {
			TextAreaPaint tap = (TextAreaPaint)tp;
			mLetterSpac = tap.mLetterSpac;
			underlineEnable =tap.underlineEnable;
			underlineColor = tap.underlineColor;
			underlineThickness = tap.underlineThickness;
		}
	}

	public int getUnderlineThickness() {
		return underlineThickness;
	}

	public void setUnderlineThickness(int thickness) {
		underlineThickness = thickness;
	}

	public float getWordRunAdvances(char[] mChars2, int start, int runLen, int contextStart, int contextLen, int flags,
			float[] object, int i) {
		float value = -1;
		if (runLen <= 10 || object == null) {
			float[] buffer = ArrayUtils.obtainFloat(runLen);
			// Log.e(TAG, ""+start+" "+runLen+" "+mChars2.length);
			getTextWidths(mChars2, start, runLen, buffer);
			value = 0;
			if (object != null) {
				if (object.length == 1 && runLen == 1) {
					value = object[0] = buffer[i];
				} else {
					for (i = 0; i < runLen; i++, contextStart++, start++) {
						value += buffer[i];
						object[contextStart] = buffer[i];
					}
				}
			} else {
				for (i = 0; i < runLen; i++, start++) {
					value += buffer[i];
				}
			}
			ArrayUtils.recycle(buffer);
		} else {
			getTextWidths(mChars2, start, runLen, object);
			for (i = 0; i < runLen; i++, start++) {
				value += object[i];
			}
		}
		return value;
	}

	public float getWordRunAdvances(CharSequence text, int start, int end, int contextStart, int contextEnd, int isRtl,
			float[] advances, int advancesIndex) {
		if (text == null) {
			throw new IllegalArgumentException("text cannot be null");
		}
		if ((start | end | contextStart | contextEnd | advancesIndex | (end - start) | (start - contextStart)
				| (contextEnd - end) | (text.length() - contextEnd)
				| (advances == null ? 0 : (advances.length - advancesIndex - (end - start)))) < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (text instanceof String) {
			// return getWordRunAdvances((String) text, start, end,
			// contextStart, contextEnd, isRtl, advances,
			// advancesIndex);
		}
		if (text instanceof SpannedString || text instanceof SpannableString) {
			// text = text.toString();
			// return getWordRunAdvances(text.toString(), start, end,
			// contextStart, contextEnd, isRtl, advances,
			// advancesIndex);
		}
		if (text.length() == 0 || end == start) {
			return 0f;
		}
		// int contextLen = contextEnd - contextStart;
		// int len = end - start;
		// char[] buf = obtain(contextLen,contextStart,contextEnd,text,0);
		// float result = getWordRunAdvances(buf, start, end, contextStart,
		// contextEnd, isRtl, advances, advancesIndex);

		// return measureText(text, contextStart, contextEnd-contextStart);
		int contextLen = contextEnd - contextStart;
		int len = end - start;
		char[] buf = obtain(contextLen, contextStart, contextEnd, text, 0);
		float result = getWordRunAdvances(buf, start - contextStart, len, 0, contextLen, isRtl, advances,
				advancesIndex);
		recycle(buf);
		return result;
	}

	// private void getChars(CharSequence text, int contextStart, int
	// contextEnd, char[] buf, int i) {
	// for (int j = contextStart; j < contextEnd; j++, i++) {
	// buf[i] = text.charAt(j);
	// }
	// }

	private static final LinkedList<char[]> BUFFERCHARS = new LinkedList<char[]>();

	private void recycle(char[] buffer) {
		synchronized (BUFFERCHARS) {
			if (BUFFERCHARS.size() < 4) {
				BUFFERCHARS.add(buffer);
			}
		}
	}

	private char[] obtain(int contextLen, int contextStart, int contextEnd, CharSequence text, int i) {
		char[] buffer;
		synchronized (BUFFERCHARS) {
			if (BUFFERCHARS.isEmpty()) {
				buffer = new char[contextLen];
			} else {
				buffer = BUFFERCHARS.getLast();
				BUFFERCHARS.removeLast();
				if (buffer.length < contextLen) {
					buffer = Arrays.copyOf(buffer, contextLen);
				}
			}
		}
		for (int j = contextStart; j < contextEnd; j++, i++) {
			buffer[i] = text.charAt(j);
		}
		return buffer;
	}

}
