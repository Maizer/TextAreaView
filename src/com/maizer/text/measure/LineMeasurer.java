package com.maizer.text.measure;

import java.util.Arrays;

import com.maizer.text.layout.LayoutAttribute;
import com.maizer.text.layout.TextAreaLayout;
import com.maizer.text.layout.TextAreaPaint;
import com.maizer.text.liner.Lineable;
import com.maizer.text.measure.LineMeasurer.TabStops;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.Trace;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils.TruncateAt;
import android.text.style.LeadingMarginSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.text.style.TabStopSpan;
import android.util.Log;
import android.widget.TextView;

/**
 * 完整的测量类
 * 
 * @author Maizer/麦泽
 *
 */
@SuppressLint("NewApi")
public class LineMeasurer extends Measurer {

	private static final String TAG = LineMeasurer.class.getCanonicalName();
	public static final float EMOjI_FLAG = Float.MIN_NORMAL;
	public static final float ELLIPSIS_FLAG = EMOjI_FLAG + 1;
	private int mLen;
	private float[] mWidths;
	private float[] mRecverWidth;
	private char[] mRecverChars;

	@Override
	public void gc() {
		if (mWidths != null) {
			Arrays.fill(mWidths, 0);
		}
		mWidths = new float[2];
	}

	@Override
	public void clear() {
		if (mWidths != null) {
			Arrays.fill(mWidths, 0);
		}
		if (mRecverChars != null) {
			Arrays.fill(mRecverChars, (char) 0);
		}
		if (mRecverWidth != null) {
			Arrays.fill(mRecverWidth, 0);
		}
		mWidths = null;
		mRecverChars = null;
		mRecverWidth = null;
	}

	public LineMeasurer(MeasureAttribute attr, int size) {
		super(attr);
		mRecverWidth = new float[2];
		mRecverChars = new char[2];
		mWidths = new float[size == 0 ? 1 : size];
	}

	/**
	 * 重置缓存长度
	 */
	protected void regainBuffer() {
		mLen = 0;
	}

	/**
	 * 设置指定位置的指定缓存的宽度
	 * 
	 * @param l
	 *            位置
	 * @param v
	 *            宽度数值
	 */
	protected void setBufferWidth(int l, float v) {
		mWidths[l] = v;
	}

	/**
	 * 获取指定缓存的宽度
	 * 
	 * @param l
	 *            位置
	 */
	protected final float getBufferWidth(int l) {
		return mWidths[l];
	}

	/**
	 * 设置当前位置的指定缓存的宽度
	 * 
	 * @param v
	 *            宽度数值
	 */
	protected final void setBufferWidth(float v) {
		if (mWidths.length <= mLen) {
			mWidths = Arrays.copyOf(mWidths, mLen * 2);
		}
		mWidths[mLen] = v;
		mLen++;
	}

	protected final int getBufferLength() {
		return mLen;
	}

	protected final float[] getBufferWidths() {
		return mWidths;
	}

	public float measureChar(CharSequence text, TextAreaPaint wp, int index) {
		regainBuffer();
		return super.measureChar(text, wp, index);
	}

	@Override
	public float measureChar(CharSequence text, int charIndex, char chs, TextAreaPaint paint, Paint.FontMetricsInt fm) {
		if (chs != CHAR_NEW_LINE) {
			if (fm != null) {
				paint.getFontMetricsInt(fm);
			}
			mRecverChars[0] = chs;
			paint.getTextWidths(mRecverChars, 0, 1, mRecverWidth);
		} else {
			mRecverWidth[0] = 0;
		}
		if (mWidths.length <= mLen) {
			mWidths = Arrays.copyOf(mWidths, mLen * 2);
		}
		mWidths[mLen] = mRecverWidth[0];
		mLen++;
		return mRecverWidth[0];
	}

	public float measureReplaceChar(CharSequence text, int start, int end, String ch, TextAreaPaint paint) {
		MetricAffectingSpan[] spans = null;
		if (text instanceof Spanned) {
			spans = getParagraphSpans((Spanned) text, start, end, MetricAffectingSpan.class);
		}
		TextAreaPaint mPaint = updataMeasureState(paint, fontMetricsInt, spans);
		char[] mChars = ch.toCharArray();
		float[] mWidths = new float[mChars.length];
		mPaint.getTextWidths(mChars, 0, mChars.length, mWidths);
		float ws = 0;
		for (float w : mWidths) {
			ws += w;
		}
		return ws;
	}

	@Override
	public float measureSpannedChar(Spanned text, int charIndex, char chs, MetricAffectingSpan[] spans,
			TextAreaPaint paint, Paint.FontMetricsInt fm) {
		TextAreaPaint workPaint = mTempPaint;

		workPaint.set(paint);
		// XXX paint should not have a baseline shift, but...
		workPaint.baselineShift = 0;
		ReplacementSpan replacement = null;
		for (int i = 0; i < spans.length; i++) {
			MetricAffectingSpan span = spans[i];
			if (span instanceof ReplacementSpan) {
				replacement = (ReplacementSpan) span;
			} else {
				span.updateMeasureState(workPaint);
			}
		}

		float wid;
		if (replacement == null) {
			wid = measureChar(text, charIndex, chs, workPaint, fm);
		} else {
			wid = replacement.getSize(workPaint, text, charIndex, charIndex + 1, fm);
			if (mWidths.length < mLen) {
				mWidths = Arrays.copyOf(mWidths, mLen * 2);
			}
			mWidths[mLen] = wid;
			mLen++;
		}

		if (fm != null) {
			if (workPaint.baselineShift < 0) {
				fm.ascent += workPaint.baselineShift;
				fm.top += workPaint.baselineShift;
			} else {
				fm.descent += workPaint.baselineShift;
				fm.bottom += workPaint.baselineShift;
			}
		}
		return wid;
	}

	protected TextAreaPaint updataMeasureState(TextAreaPaint paint, Paint.FontMetricsInt fm,
			MetricAffectingSpan[] spans) {
		if (spans == null || spans.length == 0) {
			paint.getFontMetricsInt(fm);
			return paint;
		}
		TextAreaPaint workPaint = mTempPaint;

		workPaint.set(paint);
		// XXX paint should not have a baseline shift, but...
		workPaint.baselineShift = 0;
		for (int i = 0; i < spans.length; i++) {
			MetricAffectingSpan span = spans[i];
			if (!(span instanceof ReplacementSpan)) {
				span.updateMeasureState(workPaint);
			}
		}
		workPaint.getFontMetricsInt(fm);
		fm.descent += workPaint.baselineShift;
		fm.bottom += workPaint.baselineShift;
		return workPaint;
	}

	protected float measureEmoji(char start, char end, Paint paint, MetricAffectingSpan[] spans) {
		mRecverChars[0] = start;
		mRecverChars[1] = end;
		if (spans == null) {
			paint.getTextWidths(mRecverChars, 0, 2, mRecverWidth);
		} else {
			TextAreaPaint workPaint = mTempPaint;
			workPaint.set(paint);
			// XXX paint should not have a baseline shift, but...
			workPaint.baselineShift = 0;
			for (int i = 0; i < spans.length; i++) {
				MetricAffectingSpan span = spans[i];
				if (!(span instanceof ReplacementSpan)) {
					span.updateMeasureState(workPaint);
				}
			}
			workPaint.getTextWidths(mRecverChars, 0, 2, mRecverWidth);
		}
		return mRecverWidth[0] + mRecverWidth[1];
	}

	public Lineable measureSingleLineFromFrsit(CharSequence source, int minStart, int maxEnd, int start, int end,
			float width, int v, float offsetLeading, TextAreaPaint wp, Lineable line, TruncateAt truncateAt,
			float ellipsisWidth) {
		if (line == null) {
			line = installNewLineable();
		} else {
			line.clear();
		}
		TextAreaPaint paint = wp;
		Paint.FontMetricsInt fm = fontMetricsInt;
		Spanned spanned = null;

		if (source instanceof Spanned) {
			spanned = (Spanned) source;
		}
		float letterSpac = paint.getLetterSpacing();
		int fitAscent = 0, fitDescent = 0, fitTop = 0, fitBottom = 0;

		int paraStart = start;
		int paraEnd = end;
		int leadingMargin = 0;

		TabStopSpan[] tabSpans = null;
		MetricAffectingSpan[] mspans = null;
		if (spanned != null) {
			LeadingMarginSpan[] sp = getParagraphSpans(spanned, 0, source.length(), LeadingMarginSpan.class);
			if (sp != null) {
				for (int i = 0; i < sp.length; i++) {
					LeadingMarginSpan lms = sp[i];
					leadingMargin = lms.getLeadingMargin(true);
				}
			}
			// countWidth += leadingMargin;
		}
		if (spanned != null) {
			mspans = getParagraphSpans(spanned, 0, source.length(), MetricAffectingSpan.class);
		}
		updataMeasureState(paint, fm, mspans);
		if (fm.top < fitTop) {
			fitTop = fm.top;
		}
		if (fm.bottom > fitBottom) {
			fitBottom = fm.bottom;
		}
		if (fm.ascent < fitAscent) {
			fitAscent = fm.ascent;
		}
		if (fm.descent > fitDescent) {
			fitDescent = fm.descent;
		}

		regainBuffer();

		float w = 0;
		float countWidth = 0;
		boolean hasTab = false;

		if (start == 0) {
			countWidth += leadingMargin;
		}
		for (int j = paraStart; j < paraEnd; j++) {
			if (spanned != null) {
				mspans = getParagraphSpans(spanned, j, j + 1, MetricAffectingSpan.class);
			}
			char c = source.charAt(j);
			if (c == CHAR_TAB) {
				if (hasTab == false) {
					hasTab = true;
					if (spanned != null) {
						// First tab this para, check for tabstops
						tabSpans = getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
					}
				}
				float oldW = countWidth;
				if (tabSpans != null) {
					countWidth = TabStops.nextTab(tabSpans, countWidth, TAB_INCREMENT);
				} else {
					countWidth = TabStops.nextDefaultStop(countWidth, TAB_INCREMENT);
				}
				w = countWidth - oldW;
				setBufferWidth(w);
				countWidth = oldW;
				w += letterSpac;
				// updataMeasureState(paint, fm, mspans);
			} else if (c == CHAR_NEW_LINE) {
				w = 0;
				setBufferWidth(-1);
				// updataMeasureState(paint, fm, mspans);
			} else if (j < paraEnd && TextAreaLayout.isEmojiEnable(c)) {
				int emojicode = Character.codePointAt(source, j);
				w = getEmoji(source, paint, emojicode, c, j, true, mspans);
				if (Character.charCount(emojicode) > 1) {
					setBufferWidth(EMOjI_FLAG);
					setBufferWidth(w);
					j++;
				} else {
					setBufferWidth(w);
				}
				w += letterSpac;
			} else if (mspans == null) {
				w = measureChar(source, j - paraStart, c, paint, null);
				// w = quickMeasureChar(c, paint, fm);
				if (w > 0) {
					w += letterSpac;
				} else {
					setBufferWidth(j - paraStart, -1);
				}
			} else {
				w = measureSpannedChar(spanned, j, c, mspans, paint, null);
				if (w > 0) {
					w += letterSpac;
				} else {
					setBufferWidth(j, -1);
				}
			}
			if (c != CHAR_NEW_LINE) {
				countWidth += w;
			}

			if (j + 1 < paraEnd) {
				if (countWidth <= width) {
					continue;
				}
			}
			j++;
			out(source, minStart, maxEnd, start, j, fitAscent, fitDescent, fitTop, fitBottom, fm, leadingMargin,
					offsetLeading, v, true, j == paraEnd, paint, truncateAt, ellipsisWidth, letterSpac,
					getBufferWidths(), getBufferLength(), line, true, isOpposite());
			break;
		}
		return line;
	}

	public Lineable measureSingleLineFromLast(CharSequence source, int minStart, int maxEnd, int end, int start,
			float width, int v, float offsetLeading, TextAreaPaint wp, Lineable line, TruncateAt truncateAt,
			float ellipsisWidth, boolean offsetLeadingIncludeMeasureWidths) {
		if (line == null) {
			line = installNewLineable();
		} else {
			line.clear();
		}
		TextAreaPaint paint = wp;
		Paint.FontMetricsInt fm = fontMetricsInt;
		Spanned spanned = null;

		if (source instanceof Spanned) {
			spanned = (Spanned) source;
		}
		float letterSpac = paint.getLetterSpacing();
		int fitAscent = 0, fitDescent = 0, fitTop = 0, fitBottom = 0;

		int paraStart = start;
		int paraEnd = end;
		int leadingMargin = 0;

		TabStopSpan[] tabSpans = null;
		MetricAffectingSpan[] mspans = null;
		if (spanned != null) {
			LeadingMarginSpan[] sp = getParagraphSpans(spanned, 0, source.length(), LeadingMarginSpan.class);
			if (sp != null) {
				for (int i = 0; i < sp.length; i++) {
					LeadingMarginSpan lms = sp[i];
					leadingMargin = lms.getLeadingMargin(true);
				}
			}
			// countWidth += leadingMargin;
		}

		if (spanned != null) {
			mspans = getParagraphSpans(spanned, 0, source.length(), MetricAffectingSpan.class);
		}
		updataMeasureState(paint, fm, mspans);
		if (fm.top < fitTop) {
			fitTop = fm.top;
		}
		if (fm.bottom > fitBottom) {
			fitBottom = fm.bottom;
		}
		if (fm.ascent < fitAscent) {
			fitAscent = fm.ascent;
		}
		if (fm.descent > fitDescent) {
			fitDescent = fm.descent;
		}

		regainBuffer();

		float w = 0;
		float countWidth = 0;
		boolean hasTab = false;

		for (int j = paraEnd - 1; j >= paraStart; j--) {
			char c = source.charAt(j);
			if (spanned != null) {
				mspans = getParagraphSpans(spanned, j, j + 1, MetricAffectingSpan.class);
			}
			if (c == CHAR_TAB) {
				if (hasTab == false) {
					hasTab = true;
					if (spanned != null) {
						// First tab this para, check for tabstops
						tabSpans = getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
					}
				}
				float oldW = countWidth;
				if (tabSpans != null) {
					countWidth = TabStops.nextTab(tabSpans, countWidth, TAB_INCREMENT);
				} else {
					countWidth = TabStops.nextDefaultStop(countWidth, TAB_INCREMENT);
				}
				w = countWidth - oldW;
				setBufferWidth(w);
				countWidth = oldW;
				w += letterSpac;
			} else if (c == CHAR_NEW_LINE) {
				w = 0;
				setBufferWidth(-1);
			} else if (TextAreaLayout.isEmojiEnable(c)) {
				int emojicode = Character.codePointAt(source, j);
				int codeCount = Character.charCount(emojicode);
				w = getEmoji(source, paint, emojicode, c, j, true, mspans);
				if (codeCount > 1) {
					int index = paraEnd - j - 2;
					if (index >= 0) {
						float lw = getBufferWidth(index);
						if (lw > 0) {
							countWidth -= lw;
						}
						setBufferWidth(index, w);
						setBufferWidth(EMOjI_FLAG);
					} else {
						index = 0;
						paraEnd++;
						setBufferWidth(w);
						setBufferWidth(EMOjI_FLAG);
					}
					w += letterSpac;
				} else {
					setBufferWidth(w);
				}
			} else if (mspans == null) {
				w = measureChar(source, j, c, paint, null);
				if (w > 0) {
					w += letterSpac;
				} else {
					setBufferWidth(paraEnd - j - 1, -1);
				}
			} else {
				w = measureSpannedChar(spanned, j, c, mspans, paint, null);
				if (w > 0) {
					w += letterSpac;
				} else {
					setBufferWidth(paraEnd - j - 1, -1);
				}
			}
			if (c != CHAR_NEW_LINE) {
				countWidth += w;
			}
			if (j > paraStart) {
				if (countWidth <= width) {
					continue;
				}
			}
			if (j - 1 >= 0 && TextAreaLayout.isEmojiEnable(c = source.charAt(j - 1))) {
				int emojicode = Character.codePointAt(source, j - 1);
				int codeCount = Character.charCount(emojicode);
				if (codeCount > 1) {
					j--;
					mspans = getParagraphSpans(spanned, j, j + 1, MetricAffectingSpan.class);
					w = getEmoji(source, paint, emojicode, c, j, true, mspans);
					int index = paraEnd - j - 2;
					if (index >= 0) {
						float lw = getBufferWidth(index);
						if (lw != -1) {
							countWidth -= lw;
						}
						setBufferWidth(index, w);
						setBufferWidth(EMOjI_FLAG);
					} else {
						setBufferWidth(0, w);
						setBufferWidth(EMOjI_FLAG);
						paraEnd++;
					}
				}
				countWidth += w + letterSpac;
			}
			if (offsetLeadingIncludeMeasureWidths) {
				offsetLeading -= countWidth;
			}
			out(source, minStart, maxEnd, j, paraEnd, fitAscent, fitDescent, fitTop, fitBottom, fm, leadingMargin,
					j == 0 ? leadingMargin : offsetLeading, v, true, paraEnd == maxEnd, paint, truncateAt,
					ellipsisWidth, letterSpac, getBufferWidths(), getBufferLength(), line, true, false);
			break;
		}
		return line;
	}

	@SuppressLint("NewApi")
	public Lineable measureSingleLine(CharSequence source, int start, int minStart, int maxEnd, float upWidth,
			float offsetWdith, float offsetLeading, int v, int maxEms, Lineable line, boolean measureMode,
			TextAreaPaint wp) {
		if (line == null) {
			line = installNewLineable();
		} else {
			line.clear();
		}
		TextAreaPaint paint = wp;
		// initSpecialLetterWidths(paint);

		Paint.FontMetricsInt fm = fontMetricsInt;

		Spanned spanned = null;

		if (source instanceof Spanned) {
			spanned = (Spanned) source;
		}

		float letterSpac = paint.getLetterSpacing();

		int fitAscent = 0, fitDescent = 0, fitTop = 0, fitBottom = 0;

		TruncateAt truncateAt = getTruncateAt();
		int bufEnd = maxEnd;

		int paraStart = minStart;
		int paraEnd = start;
		int mStart = 0;
		float ellipsisW = 0;
		float width = upWidth == -1 ? getWidth() : upWidth;
		float countWidth = 0;

		boolean hasTab = false;
		int leadingMargin = 0;

		TabStopSpan[] tabSpans = null;
		MetricAffectingSpan[] mspans = null;
		if (spanned != null) {
			LeadingMarginSpan[] sp = getParagraphSpans(spanned, 0, source.length(), LeadingMarginSpan.class);
			if (sp != null) {
				for (int i = 0; i < sp.length; i++) {
					LeadingMarginSpan lms = sp[i];
					leadingMargin = lms.getLeadingMargin(true);
				}
			}
		}
		if (spanned != null) {
			mspans = getParagraphSpans(spanned, 0, source.length(), MetricAffectingSpan.class);
		}
		updataMeasureState(paint, fm, mspans);
		if (fm.top < fitTop) {
			fitTop = fm.top;
		}
		if (fm.bottom > fitBottom) {
			fitBottom = fm.bottom;
		}
		if (fm.ascent < fitAscent) {
			fitAscent = fm.ascent;
		}
		if (fm.descent > fitDescent) {
			fitDescent = fm.descent;
		}

		String ellipsis = getEllipsis();
		if (truncateAt != null && ellipsis != null) {
			if (minStart != 0) {
				ellipsisW = measureReplaceChar(source, 0, minStart, ellipsis, paint);
			} else if (source.length() > maxEnd) {
				ellipsisW = measureReplaceChar(source, maxEnd, source.length(), ellipsis, paint);
			}
		}

		regainBuffer();
		if (!measureMode) {
			float w;
			for (int j = paraEnd - 1; j >= paraStart; j--) {
				char c = source.charAt(j);
				if (spanned != null) {
					mspans = getParagraphSpans(spanned, j, j + 1, MetricAffectingSpan.class);
				}
				if (c == CHAR_TAB) {
					if (hasTab == false) {
						hasTab = true;
						if (spanned != null) {
							// First tab this para, check for tabstops
							tabSpans = getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
						}
					}
					float oldW = countWidth;
					if (tabSpans != null) {
						countWidth = TabStops.nextTab(tabSpans, countWidth, TAB_INCREMENT);
					} else {
						countWidth = TabStops.nextDefaultStop(countWidth, TAB_INCREMENT);
					}
					w = countWidth - oldW;
					setBufferWidth(w);
					countWidth = oldW;
					w += letterSpac;
				} else if (c == CHAR_NEW_LINE) {
					w = 0;
					setBufferWidth(-1);
				} else if (TextAreaLayout.isEmojiEnable(c)) {
					int emojicode = Character.codePointAt(source, j);
					int codeCount = Character.charCount(emojicode);
					w = getEmoji(source, paint, emojicode, c, j, true, mspans);
					if (codeCount > 1) {
						int index = paraEnd - j - 2;
						if (index >= 0) {
							float lw = getBufferWidth(index);
							if (lw > 0) {
								countWidth -= lw;
							}
							setBufferWidth(index, w);
							setBufferWidth(EMOjI_FLAG);
						} else {
							index = 0;
							paraEnd++;
							setBufferWidth(w);
							setBufferWidth(EMOjI_FLAG);
						}
						w += letterSpac;
					} else {
						setBufferWidth(w);
					}
				} else if (mspans == null) {
					w = measureChar(source, j, c, paint, null);
					if (w > 0) {
						w += letterSpac;
					} else {
						setBufferWidth(paraEnd - j - 1, -1);
					}
				} else {
					w = measureSpannedChar(spanned, j, c, mspans, paint, null);
					if (w > 0) {
						w += letterSpac;
					} else {
						setBufferWidth(paraEnd - j - 1, -1);
					}
				}
				if (c != CHAR_NEW_LINE) {
					countWidth += w;
				}
				if (j > paraStart) {
					if (countWidth <= width) {
						continue;
					}
				}
				if (j - 1 >= 0 && TextAreaLayout.isEmojiEnable(c = source.charAt(j - 1))) {
					int emojicode = Character.codePointAt(source, j - 1);
					int codeCount = Character.charCount(emojicode);
					if (codeCount > 1) {
						j--;
						mspans = getParagraphSpans(spanned, j, j + 1, MetricAffectingSpan.class);
						w = getEmoji(source, paint, emojicode, c, j, true, mspans);
						int index = paraEnd - j - 2;
						if (index >= 0) {
							float lw = getBufferWidth(index);
							if (lw != -1) {
								countWidth -= lw;
							}
							setBufferWidth(index, w);
							setBufferWidth(EMOjI_FLAG);
						} else {
							setBufferWidth(0, w);
							setBufferWidth(EMOjI_FLAG);
							paraEnd++;
						}
					}
				}
				out(source, minStart, bufEnd, j, paraEnd, fitAscent, fitDescent, fitTop, fitBottom, fm, leadingMargin,
						j == minStart ? offsetLeading + leadingMargin : offsetLeading, v, true, paraEnd == bufEnd,
						paint, truncateAt, ellipsisW, letterSpac, getBufferWidths(), getBufferLength(), line, true,
						false);
				break;
			}
			if (line.getEnd() >= bufEnd || line.getMaxSite() >= getWidth()) {
				outEnd(line, spanned, fm);
				return line;
			}
			regainBuffer();
			paraStart = line.getEnd();
			mStart = line.getStart();
			countWidth -= offsetWdith;
			width = getWidth();
		} else {
			if (start == 0 && maxEms > 0 && truncateAt == TruncateAt.START && bufEnd > maxEms) {
				minStart = start = bufEnd - Measurer.codePointCount(source, 0, source.length(), maxEms, false);
			}
			paraStart = start;
			mStart = start;
			width += offsetWdith;
		}
		if (mStart == 0) {
			countWidth += leadingMargin;
		}
		for (int j = paraStart; j < bufEnd; j++) {
			if (spanned != null) {
				mspans = getParagraphSpans(spanned, j, j + 1, MetricAffectingSpan.class);
			}
			char c = source.charAt(j);
			float w;
			if (c == CHAR_TAB) {
				if (hasTab == false) {
					hasTab = true;
					if (spanned != null) {
						// First tab this para, check for tabstops
						tabSpans = getParagraphSpans(spanned, paraStart, bufEnd, TabStopSpan.class);
					}
				}
				float oldW = countWidth;
				if (tabSpans != null) {
					countWidth = TabStops.nextTab(tabSpans, countWidth, TAB_INCREMENT);
				} else {
					countWidth = TabStops.nextDefaultStop(countWidth, TAB_INCREMENT);
				}
				w = countWidth - oldW;
				setBufferWidth(w);
				countWidth = oldW;
				w += letterSpac;
				// updataMeasureState(paint, fm, mspans);
			} else if (c == CHAR_NEW_LINE) {
				w = 0;
				setBufferWidth(-1);
				// updataMeasureState(paint, fm, mspans);
			} else if (j < bufEnd && TextAreaLayout.isEmojiEnable(c)) {
				int emojicode = Character.codePointAt(source, j);
				w = getEmoji(source, paint, emojicode, c, j, true, mspans);
				if (Character.charCount(emojicode) > 1) {
					setBufferWidth(EMOjI_FLAG);
					setBufferWidth(w);
					j++;
				} else {
					setBufferWidth(w);
				}
				w += letterSpac;
			} else if (mspans == null) {
				w = measureChar(source, j - paraStart, c, paint, null);
				// w = quickMeasureChar(c, paint, fm);
				if (w > 0) {
					w += letterSpac;
				} else {
					setBufferWidth(j - paraStart, -1);
				}
			} else {
				w = measureSpannedChar(spanned, j, c, mspans, paint, null);
				if (w > 0) {
					w += letterSpac;
				} else {
					setBufferWidth(j, -1);
				}
			}
			if (c != CHAR_NEW_LINE) {
				countWidth += w;
			}

			if (j + 1 < bufEnd) {
				if (countWidth <= width) {
					continue;
				}
			}
			j++;
			out(source, minStart, bufEnd, mStart, j, fitAscent, fitDescent, fitTop, fitBottom, fm, leadingMargin,
					mStart == minStart ? offsetLeading + leadingMargin : offsetLeading, v, true, j == bufEnd, paint,
					truncateAt, ellipsisW, letterSpac, getBufferWidths(), getBufferLength(), line, true, isOpposite());
			break;
		}
		outEnd(line, spanned, fm);
		return line;
	}

	@Override
	public Lineable measureAfterLine(CharSequence source, int start, int end, int v, Lineable line, TextAreaPaint wp,
			float ellipsisWidth) {
		if (isSingleLine()) {
			return measureSingleLine(source, start, start, end, -1, 0, 0, v, getMaxEms(), line, true, wp);
		}
		if (line == null) {
			line = installNewLineable();
		} else {
			line.clear();
		}
		TextAreaPaint paint = wp;
		// initSpecialLetterWidths(paint);

		Paint.FontMetricsInt fm = fontMetricsInt;

		Spanned spanned = null;

		if (source instanceof Spanned) {
			spanned = (Spanned) source;
		}

		int DEFAULT_DIR = Layout.DIR_LEFT_TO_RIGHT; // XXX

		float letterSpac = paint.getLetterSpacing();

		int fitAscent = 0, fitDescent = 0, fitTop = 0, fitBottom = 0;

		TruncateAt truncateAt = getTruncateAt();

		int width = getWidth();

		int sourceEnd = end;
		int paraEnd = end;
		int paraStart = start;

		float countWidth = 0;

		boolean isFirstLine = paraStart == 0 || source.charAt(paraStart) == CHAR_NEW_LINE;

		boolean hasTabOrEmoji = false;
		boolean hasTab = false;
		boolean lastEmoji = false;

		int leadingMargin = 0;
		int lastSpecialPoint = 0;
		int lastEmojiPoint = -1;

		char lastSpecialChar = 0;
		char lastChar = 0;
		boolean stop = false;

		TabStopSpan[] tabSpans = null;
		MetricAffectingSpan[] mspans = null;
		if (spanned != null) {
			LeadingMarginSpan[] sp = getParagraphSpans(spanned, paraStart, paraEnd, LeadingMarginSpan.class);
			if (sp != null) {
				for (int i = 0; i < sp.length; i++) {
					LeadingMarginSpan lms = sp[i];
					leadingMargin = lms.getLeadingMargin(isFirstLine);
				}
			}
		}
		A: for (int spanStart = paraStart, spanEnd = spanStart; spanStart < paraEnd;) {
			if (spanStart == spanEnd) {
				if (spanned == null) {
					spanEnd = paraEnd;
				} else {
					spanEnd = spanned.nextSpanTransition(spanStart, paraEnd, MetricAffectingSpan.class);
					mspans = getParagraphSpans(spanned, spanStart, spanEnd, MetricAffectingSpan.class);
				}
			}
			regainBuffer();
			lastSpecialPoint = 0;
			lastEmojiPoint = -1;
			lastSpecialChar = 0;
			lastChar = 0;
			for (int j = spanStart; j < spanEnd; j++) {
				if (isFirstLine) {
					if (j == paraStart) {
						countWidth += leadingMargin;
					}
				} else if (j == spanStart) {
					countWidth += leadingMargin;
				}
				lastEmoji = false;

				char c = source.charAt(j);
				float w;
				if (c == CHAR_TAB) {
					if (hasTab == false) {
						hasTab = true;
						hasTabOrEmoji = true;
						if (spanned != null) {
							// First tab this para, check for tabstops
							tabSpans = getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
						}
					}
					float oldW = countWidth;
					if (tabSpans != null) {
						countWidth = TabStops.nextTab(tabSpans, countWidth, TAB_INCREMENT);
					} else {
						countWidth = TabStops.nextDefaultStop(countWidth, TAB_INCREMENT);
					}
					w = countWidth - oldW;
					setBufferWidth(w);
					w += letterSpac;
					countWidth = oldW;
					if (isSpecialLetter(c, lastSpecialChar, lastChar, source, j)) {
						lastSpecialPoint = j;
						lastSpecialChar = c;
					}
				} else if (c == CHAR_NEW_LINE) {
					w = 0;
					if (j != spanStart) {
						stop = true;
						paraEnd = j;
					} else {
						setBufferWidth(-1);
					}
					// updataMeasureState(paint, fm, mspans);
				} else if (j + 1 < spanEnd && TextAreaLayout.isEmojiEnable(c)) {
					lastEmojiPoint = j;
					lastEmoji = true;
					int emojicode = Character.codePointAt(source, j);
					w = getEmoji(source, paint, emojicode, c, j, true, mspans);
					if (Character.charCount(emojicode) > 1) {
						setBufferWidth(EMOjI_FLAG);
						setBufferWidth(w);
						j++;
					} else {
						setBufferWidth(w);
					}
					w += letterSpac;
				} else if (mspans == null) {
					w = measureChar(source, j, c, paint, null);
					if (w > 0) {
						w += letterSpac;
					} else {
						setBufferWidth(j - spanStart, -1);
					}
					if (isSpecialLetter(c, lastSpecialChar, lastChar, source, j)) {
						lastSpecialPoint = j;
						lastSpecialChar = c;
					}
				} else {
					w = measureSpannedChar(spanned, j, c, mspans, paint, null);
					if (w > 0) {
						w += letterSpac;
					} else {
						setBufferWidth(j - spanStart, -1);
					}
					if (isSpecialLetter(c, lastSpecialChar, lastChar, source, j)) {
						lastSpecialPoint = j;
						lastSpecialChar = c;
					}
				}
				if (!stop) {
					if ((lastChar = c) != CHAR_NEW_LINE) {
						countWidth += w;
					}

					if (j + 1 < spanEnd) {
						if (countWidth <= width) {
							continue;
						}
					}
				}
				if (countWidth <= width) {
					if (j + 1 == sourceEnd && !stop) {
						j = sourceEnd;
						paraEnd = j;
					} else if (countWidth == width) {
						paraEnd = j + 1;
					}
				} else {
					if (lastSpecialPoint == j || lastSpecialPoint == 0 || lastSpecialPoint == paraStart
							|| source.charAt(lastSpecialPoint - 1) == CHAR_NEW_LINE) {
						if (lastEmoji) {
							j--;
						}
						mLen--;
					} else if (lastEmojiPoint != lastSpecialPoint
							&& !isNextNewLine(lastSpecialChar, source, lastSpecialPoint)) {
						mLen -= (j - lastSpecialPoint);
						j = lastSpecialPoint + 1;
					} else if (lastEmojiPoint == lastSpecialPoint && lastEmojiPoint < j - 1) {
						mLen -= (j - lastSpecialPoint - 2);
						j = lastSpecialPoint + 2;
					} else {
						mLen -= (j - lastSpecialPoint + 1);
						j = lastSpecialPoint;
					}
					paraEnd = j;
				}
				// if (spanned != null) {
				// mspans = getParagraphSpans(spanned, paraStart, j,
				// MetricAffectingSpan.class);
				// }
				if (spanned != null && ellipsisWidth != 0 && source.length() > j) {
					if (countWidth + ellipsisWidth <= width) {
						mspans = getParagraphSpans(spanned, paraStart, source.length(), MetricAffectingSpan.class);
					} else {
						float countBufferWidth = 0;
						for (int i = mLen - 1; i >= 0; i--) {
							float buffw = getBufferWidth(i);
							if (buffw != ELLIPSIS_FLAG) {
								countBufferWidth += buffw;
							}
							mLen--;
							j--;
							if (countBufferWidth > ellipsisWidth) {
								break;
							}
						}
						if (mLen > 0) {
							if (getBufferWidth(mLen - 1) == ELLIPSIS_FLAG) {
								mLen--;
								j--;
							}
						}
						mspans = getParagraphSpans(spanned, j, source.length(), MetricAffectingSpan.class);
					}
					paraEnd = j;
				}
				updataMeasureState(paint, fm, mspans);
				if (fm.top < fitTop) {
					fitTop = fm.top;
				}
				if (fm.bottom > fitBottom) {
					fitBottom = fm.bottom;
				}
				if (fm.ascent < fitAscent) {
					fitAscent = fm.ascent;
				}
				if (fm.descent > fitDescent) {
					fitDescent = fm.descent;
				}
				out(source, paraStart, paraEnd, paraStart, j, fitAscent, fitDescent, fitTop, fitBottom, fm,
						leadingMargin, -1, v, paraStart == 0, paraEnd == sourceEnd, paint, truncateAt, ellipsisWidth,
						letterSpac, mWidths, mLen, line, true, isOpposite());
				fitAscent = fitDescent = fitTop = fitBottom = 0;
				spanStart = j + 1;
				if (countWidth >= width) {
					break A;
				}
				break;
			}
		}
		outEnd(line, spanned, fm);
		return line;
	}

	// private void quickGetMeasureData(WordPaint paint, CharSequence source,
	// int start, int end, float startW,
	// float width) {
	// if (startW >= width) {
	// return;
	// }
	// // initSpecialLetterWidths(paint);
	//
	// Paint.FontMetricsInt fm = fontMetricsInt;
	// float letterSpac = paint.getLetterSpacing();
	// float letterWidth = 0;
	// Spanned spanned = null;
	// MetricAffectingSpan[] mspans = null;
	// WordPaint mWorkPaint = mTempPaint;
	// boolean hasTab = false;
	// boolean hasTabOrEmoji;
	// TabStops tabStops = null;
	//
	// if (source instanceof Spanned) {
	// spanned = (Spanned) source;
	// }
	//
	// for (int spanStart = start, spanEnd = spanStart, nextSpanStart; spanStart
	// < end; spanStart = nextSpanStart) {
	// if (spanStart == spanEnd) {
	// if (spanned == null) {
	// spanEnd = end;
	// } else {
	// spanEnd = spanned.nextSpanTransition(spanStart, end,
	// MetricAffectingSpan.class);
	// mspans = spanned.getSpans(spanStart, spanEnd, MetricAffectingSpan.class);
	// mspans = ArrayUtils.removeEmptySpans(mspans, spanned,
	// MetricAffectingSpan.class);
	// if (mspans.length <= 0) {
	// mspans = null;
	// }
	// }
	// }
	// nextSpanStart = spanEnd;
	// for (int j = spanStart; j < spanEnd; j++) {
	// char ch = source.charAt(j);
	// if (mspans != null) {
	// letterWidth = measureSpannedChar(spanned, j, ch, mspans, paint, fm);
	// } else {
	// letterWidth = getSpecialLetterWidth(ch);
	// }
	// if (ch == CHAR_TAB) {
	// if (hasTab == false) {
	// hasTab = true;
	// hasTabOrEmoji = true;
	// if (spanned != null) {
	// // First tab this para, check for
	// // tabstops
	// TabStopSpan[] spans = getParagraphSpans(spanned, start, end,
	// TabStopSpan.class);
	// if (spans != null) {
	// tabStops = new TabStops(TAB_INCREMENT, spans);
	// }
	// }
	// }
	// if (tabStops != null) {
	// startW = tabStops.nextTab(startW);
	// } else {
	// startW = TabStops.nextDefaultStop(startW, TAB_INCREMENT);
	// }
	// } else if (TextLayout.isEmojiEnable(ch) && j + 1 < spanEnd) {
	// int emoji = Character.codePointAt(source, j - spanStart);
	// Bitmap bm;
	// if ((bm = TextLayout.getEmojiBitmap(emoji)) != null) {
	//
	// if (bm != null) {
	// Paint whichPaint;
	//
	// if (spanned == null) {
	// whichPaint = paint;
	// } else {
	// whichPaint = mWorkPaint;
	// }
	//
	// float wid = bm.getWidth() * -whichPaint.ascent() / bm.getHeight();
	//
	// startW += wid;
	// hasTabOrEmoji = true;
	// }
	// }
	// } else {
	// startW += letterWidth + letterSpac;
	// }
	// if (startW > width) {
	// break;
	// }
	// }
	// }
	// }

	/**
	 * @hide 待加强
	 */
	@Override
	public Lineable measureBeforeLine(CharSequence source, int start, int end, int v, Lineable line, TextAreaPaint wp,
			float ellipsizeWidth) {

		if (line == null) {
			line = installNewLineable();
		} else {
			line.clear();
		}
		TextAreaPaint paint = wp;
		// initSpecialLetterWidths(paint);

		Paint.FontMetricsInt fm = fontMetricsInt;

		Spanned spanned = null;

		if (source instanceof Spanned) {
			spanned = (Spanned) source;
		}

		int DEFAULT_DIR = Layout.DIR_LEFT_TO_RIGHT; // XXX

		float letterSpac = paint.getLetterSpacing();

		int fitAscent = 0, fitDescent = 0, fitTop = 0, fitBottom = 0;

		int maxEms = getMaxEms();
		int sourceEnd = source.length();

		int paraStart = start;
		int paraEnd = sourceEnd;
		int width = getWidth();
		float countWidth = 0;

		if (maxEms > 0 && maxEms < sourceEnd) {
			sourceEnd = paraEnd = maxEms;
		}

		boolean isFirstLine = paraStart == 0 || source.charAt(paraStart) == CHAR_NEW_LINE;
		/*
		 * not using TextUtils.indexOf,because we do without .
		 * 抛弃这让我恶心的索引吧,太降低性能了
		 */
		// if (paraEnd < 0) {
		// paraEnd = bufEnd;
		// } else if (paraStart == paraEnd) {
		// isFirstLine = true;
		// if (paraEnd + 1 == bufEnd) {
		// paraEnd++;
		// } else {
		// paraEnd = TextUtils.indexOf(source, CHAR_NEW_LINE, paraEnd + 1,
		// bufEnd);
		// if (paraEnd < 0) {
		// paraEnd = bufEnd;
		// } else {
		// bufEnd = paraEnd;
		// }
		// }
		// } else {
		// bufEnd = paraEnd;
		// }
		TruncateAt truncateAt = getTruncateAt();

		boolean hasTabOrEmoji = false;
		boolean hasTab = false;
		boolean lastEmoji = false;

		int leadingMargin = 0;
		int lastSpecialPoint = 0;
		int lastEmojiPoint = -1;

		char lastSpecialChar = 0;
		char lastChar = 0;

		boolean stop = false;

		MetricAffectingSpan[] mspans = null;
		TabStopSpan[] tabSpans = null;

		if (spanned != null) {
			LeadingMarginSpan[] sp = getParagraphSpans(spanned, paraStart, paraEnd, LeadingMarginSpan.class);
			if (sp != null) {
				for (int i = 0; i < sp.length; i++) {
					LeadingMarginSpan lms = sp[i];
					leadingMargin = lms.getLeadingMargin(isFirstLine);
				}
			}
		}
		A: for (int spanStart = paraStart, spanEnd = spanStart; spanStart < paraEnd;) {
			if (spanStart == spanEnd) {
				if (spanned == null) {
					spanEnd = paraEnd;
				} else {
					spanEnd = spanned.nextSpanTransition(spanStart, paraEnd, MetricAffectingSpan.class);
					mspans = getParagraphSpans(spanned, spanStart, spanEnd, MetricAffectingSpan.class);
				}
			}
			regainBuffer();
			lastSpecialPoint = 0;
			lastEmojiPoint = -1;
			lastSpecialChar = 0;
			lastChar = 0;
			for (int j = spanStart; j < spanEnd; j++) {
				if (isFirstLine) {
					if (j == paraStart) {
						countWidth += leadingMargin;
					}
				} else if (j == spanStart) {
					countWidth += leadingMargin;
				}
				lastEmoji = false;

				char c = source.charAt(j);
				float w;
				if (c == CHAR_TAB) {
					if (hasTab == false) {
						hasTab = true;
						hasTabOrEmoji = true;
						if (spanned != null) {
							// First tab this para, check for tabstops
							tabSpans = getParagraphSpans(spanned, paraStart, paraEnd, TabStopSpan.class);
						}
					}
					float oldW = countWidth;
					if (tabSpans != null) {
						countWidth = TabStops.nextTab(tabSpans, countWidth, TAB_INCREMENT);
					} else {
						countWidth = TabStops.nextDefaultStop(countWidth, TAB_INCREMENT);
					}
					w = countWidth - oldW;
					setBufferWidth(w);
					countWidth = oldW;
					w += letterSpac;
					// updataMeasureState(paint, fm, mspans);
					if (isSpecialLetter(c, lastSpecialChar, lastChar, source, j)) {
						lastSpecialPoint = j;
						lastSpecialChar = c;
					}
				} else if (c == CHAR_NEW_LINE) {
					w = 0;
					if (j != spanStart) {
						stop = true;
						paraEnd = j;
					} else {
						setBufferWidth(-1);
					}
					// updataMeasureState(paint, fm, mspans);
				} else if (j + 1 < spanEnd && TextAreaLayout.isEmojiEnable(c)) {
					lastEmojiPoint = j;
					lastEmoji = true;
					int emojicode = Character.codePointAt(source, j);
					w = getEmoji(source, paint, emojicode, c, j, true, mspans);
					if (Character.charCount(emojicode) > 1) {
						setBufferWidth(EMOjI_FLAG);
						setBufferWidth(w);
						j++;
					} else {
						setBufferWidth(w);
					}
					w += letterSpac;
				} else if (mspans == null) {
					w = measureChar(source, j, c, paint, null);
					if (w > 0) {
						w += letterSpac;
					} else {
						setBufferWidth(j - spanStart, -1);
					}
					if (isSpecialLetter(c, lastSpecialChar, lastChar, source, j)) {
						lastSpecialPoint = j;
						lastSpecialChar = c;
					}
				} else {
					w = measureSpannedChar(spanned, j, c, mspans, paint, null);
					if (w > 0) {
						w += letterSpac;
					} else {
						setBufferWidth(j - spanStart, -1);
					}
					if (isSpecialLetter(c, lastSpecialChar, lastChar, source, j)) {
						lastSpecialPoint = j;
						lastSpecialChar = c;
					}
				}
				if (!stop) {
					if ((lastChar = c) != CHAR_NEW_LINE) {
						countWidth += w;
					}

					if (j + 1 < spanEnd) {
						if (countWidth <= width) {
							continue;
						}
					}
				}
				if (countWidth <= width) {
					if (j + 1 == sourceEnd && !stop) {
						j = sourceEnd;
						paraEnd = j;
					} else if (countWidth == width) {
						paraEnd = j + 1;
					}
				} else {
					if (lastSpecialPoint == j || lastSpecialPoint == 0 || lastSpecialPoint == paraStart
							|| source.charAt(lastSpecialPoint - 1) == CHAR_NEW_LINE) {
						if (lastEmoji) {
							j--;
						}
						mLen--;
					} else if (lastEmojiPoint != lastSpecialPoint
							&& !isNextNewLine(lastSpecialChar, source, lastSpecialPoint)) {
						mLen -= (j - lastSpecialPoint);
						j = lastSpecialPoint + 1;
					} else if (lastEmojiPoint == lastSpecialPoint && lastEmojiPoint < j - 1) {
						mLen -= (j - lastSpecialPoint - 2);
						j = lastSpecialPoint + 2;
					} else {
						mLen -= (j - lastSpecialPoint + 1);
						j = lastSpecialPoint;
					}
					paraEnd = j;
				}
				// if (spanned != null) {
				// mspans = getParagraphSpans(spanned, paraStart, j,
				// MetricAffectingSpan.class);
				// }
				updataMeasureState(paint, fm, mspans);
				if (fm.top < fitTop) {
					fitTop = fm.top;
				}
				if (fm.bottom > fitBottom) {
					fitBottom = fm.bottom;
				}
				if (fm.ascent < fitAscent) {
					fitAscent = fm.ascent;
				}
				if (fm.descent > fitDescent) {
					fitDescent = fm.descent;
				}
				out(source, paraStart, paraEnd, paraStart, j, fitAscent, fitDescent, fitTop, fitBottom, fm,
						leadingMargin, -1, v, paraStart == 0, paraEnd == source.length(), paint, truncateAt,
						ellipsizeWidth, letterSpac, mWidths, mLen, line, false, isOpposite());
				fitAscent = fitDescent = fitTop = fitBottom = 0;
				spanStart = j + 1;
				if (countWidth >= width) {
					break A;
				}
				break;
			}
		}
		outEnd(line, spanned, fm);
		return line;

	}

	public static class TabStops {
		private int[] mStops;
		private int mNumStops;
		private int mIncrement;

		// public TabStops(int increment, Object[] spans) {
		// reset(increment, spans);
		// }
		//
		// void reset(int increment, Object[] spans) {
		// this.mIncrement = increment;
		//
		// int ns = 0;
		// if (spans != null) {
		// int[] stops = this.mStops;
		// for (Object o : spans) {
		// if (o instanceof TabStopSpan) {
		// if (stops == null) {
		// stops = new int[10];
		// } else if (ns == stops.length) {
		// int[] nstops = new int[ns * 2];
		// for (int i = 0; i < ns; ++i) {
		// nstops[i] = stops[i];
		// }
		// stops = nstops;
		// }
		// stops[ns++] = ((TabStopSpan) o).getTabStop();
		// }
		// }
		// if (ns > 1) {
		// Arrays.sort(stops, 0, ns);
		// }
		// if (stops != this.mStops) {
		// this.mStops = stops;
		// }
		// }
		// this.mNumStops = ns;
		// }

		// public float nextTab(float h) {
		// int ns = this.mNumStops;
		// if (ns > 0) {
		// int[] stops = this.mStops;
		// for (int i = 0; i < ns; ++i) {
		// int stop = stops[i];
		// if (stop > h) {
		// return stop;
		// }
		// }
		// }
		// return nextDefaultStop(h, mIncrement);
		// }

		public static float nextTab(TabStopSpan[] spans, float h, int inc) {
			int ns = spans.length;
			if (ns > 0) {
				for (int i = 0; i < ns; ++i) {
					int stop = spans[i].getTabStop();
					if (stop > h) {
						return stop;
					}
				}
			}
			return nextDefaultStop(h, inc);
		}

		public static float nextDefaultStop(float h, int inc) {
			return h + inc;
		}
	}

	@Override
	protected int computeFirstLineToTopWithSpacing(int above, int below, int extra) {
		if (-extra < above) {
			return above - below / 2;
		}
		return above;
	}
}
