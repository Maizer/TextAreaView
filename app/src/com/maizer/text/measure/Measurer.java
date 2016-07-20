package com.maizer.text.measure;

import com.maizer.text.measure.LineMeasurer.TabStops;
import com.maizer.text.util.ArrayGc;
import com.maizer.text.util.ArrayUtils;
import com.maizer.text.factory.LineFactory;
import com.maizer.text.layout.LayoutAttribute;
import com.maizer.text.layout.TextAreaLayout;
import com.maizer.text.layout.TextAreaPaint;
import com.maizer.text.layout.TextAreaLayout.Alignment;
import com.maizer.text.liner.Lineable;
import com.maizer.text.liner.TextAreaLiner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;
import android.text.style.TabStopSpan;
import android.util.Log;

/**
 * 我们的属性,一旦代入不能随意更改任意数值,必须通知layout进行更改
 * 
 * @note 线程不安全
 * @author Maizer/麦泽
 */
public abstract class Measurer implements ArrayGc {

	@Override
	public void clear() {
		mAttribute = null;
	}

	protected final Paint.FontMetricsInt fontMetricsInt = new FontMetricsInt();

	private MeasureAttribute mAttribute;

	private int topPadding;
	private int bottomPadding;

	protected final TextAreaPaint mTempPaint;

	/**
	 * 只可以用于大体测量,宽度信息并不准确
	 */
	@Deprecated
	private static final char[] SPECIAL_LETTERS = new char[129];
	@Deprecated
	private float[] mSpecialLetterWidths = new float[129];

	public static final char CHAR_TAB = '\t';
	public static final char CHAR_NEW_LINE = '\n';
	protected static final int TAB_INCREMENT = 20;
	protected static final char CHAR_SPACE = ' ';
	protected static final char CHAR_ZWSP = '\u200B';
	protected static final char CHAR_DOT = '.';
	protected static final char CHAR_COMMA = ',';
	protected static final char CHAR_COLON = ':';
	protected static final char CHAR_SEMICOLON = ';';
	protected static final char CHAR_SLASH = '/';
	protected static final char CHAR_HYPHEN = '-';
	protected static final char CHAR_FIRST_CJK = '\u2E80';
	protected static final double EXTRA_ROUNDING = 0.5;

	public static final int DIR_REQUEST_LTR = 1;
	public static final int DIR_REQUEST_RTL = -1;
	public static final int DIR_REQUEST_DEFAULT_LTR = 2;
	public static final int DIR_REQUEST_DEFAULT_RTL = -2;

	private static final String TAG = Measurer.class.getCanonicalName();

	static {
		for (char i = 0; i < 128; i++) {
			SPECIAL_LETTERS[i] = i;
		}
		SPECIAL_LETTERS[128] = '\uFFCD';
	}

	protected Measurer(MeasureAttribute attribute) {
		mAttribute = attribute;
		mTempPaint = new TextAreaPaint();
	}

	/**
	 * 设置下一次测量的属性
	 * 
	 * @param attr
	 */
	public final void setMeasureAttrbute(MeasureAttribute attr) {
		mAttribute = attr;
	}

	public final boolean isIncludepad() {
		return mAttribute.isIncludepad;
	}

	public final boolean isTrackpad() {
		return mAttribute.isTrackpad;
	}

	public final float getSpacingMult() {
		return mAttribute.spacingMult;
	}

	public final float getSpacingAdd() {
		return mAttribute.spacingAdd;
	}

	public final int getMaxEms() {
		return mAttribute.maxEms;
	}

	public final int getMaxLines() {
		return mAttribute.maxLines;
	}

	public final int getMaxLinesLimitHeight() {
		return mAttribute.maxLinesLimitHeight;
	}

	public final boolean isSingleLine() {
		return mAttribute.maxLines == 1;
	}

	/**
	 * {@link MeasureAttribute#isFirstLineToTopWithSpacing}
	 * 
	 * @return
	 */
	public final boolean isFirstLineToTopWithSpacing() {
		return mAttribute.isFirstLineToTopWithSpacing;
	}

	public final int getWidth() {
		return mAttribute.width;
	}

	public final TruncateAt getTruncateAt() {
		return mAttribute.truncateAt;
	}

	public final String getEllipsis() {
		return mAttribute.ellipsize;
	}

	public final LineFormat getLineFormat() {
		return mAttribute.lineFormat;
	}

	public final boolean isOpposite() {
		return mAttribute.alignment != Alignment.ALIGN_OPPOSITE;
	}

	/**
	 * 新的行
	 * 
	 * @return 行对象
	 */
	public final Lineable installNewLineable() {
		return mAttribute.lineFactory.newLineable();
	}

	public final LineFactory getLineFactory() {
		return mAttribute.lineFactory;
	}

	public float getTabMeasure(CharSequence text, int index, TextAreaPaint wp) {
		if (text instanceof Spanned) {
			Spanned spanned = (Spanned) text;
			TabStopSpan[] spans = getParagraphSpans(spanned, index, index + 1, TabStopSpan.class);
			if (spans != null) {
				return TabStops.nextTab(spans, 0, TAB_INCREMENT) + wp.getLetterSpacing();
			}
		}
		return TabStops.nextDefaultStop(0, TAB_INCREMENT) + wp.getLetterSpacing();
	}

	protected float getEmoji(CharSequence text, TextAreaPaint wp, int emojicode, char c, int index, boolean measure,
			MetricAffectingSpan[] sp) {
		Bitmap bm;
		if ((bm = TextAreaLayout.getEmojiBitmap(emojicode)) != null) {
			if (!measure) {
				return 0;
			}
			TextPaint whichPaint;
			if (sp == null) {
				whichPaint = wp;
			} else {
				whichPaint = mTempPaint;
				whichPaint.set(wp);
				// XXX paint should not have a baseline shift, but...
				whichPaint.baselineShift = 0;
				for (int i = 0; i < sp.length; i++) {
					MetricAffectingSpan span = sp[i];
					if (!(span instanceof ReplacementSpan)) {
						span.updateMeasureState(whichPaint);
					}
				}
			}
			return bm.getWidth() * -whichPaint.ascent() / bm.getHeight();
		} else if (Character.charCount(emojicode) > 1) {
			if (!measure) {
				return 0;
			}
			return measureEmoji(c, text.charAt(index + 1), wp, sp);
		}
		return -1;
	}

	public float getEmoji(CharSequence text, TextAreaPaint wp, int index, boolean measure) {
		if (index >= 0) {
			char c = text.charAt(index);
			if (TextAreaLayout.isEmojiEnable(c)) {
				int emojicode = Character.codePointAt(text, index);
				float result = getEmoji(text, wp, emojicode, c, index, measure, text instanceof Spannable
						? getParagraphSpans((Spannable) text, index, index + 1, MetricAffectingSpan.class) : null);
				if (result != -1) {
					return result + wp.getLetterSpacing();
				}
			}
		}
		return -1;
	}

	public float measureChar(CharSequence text, TextAreaPaint wp, int index) {
		float letterSpac = wp.getLetterSpacing();
		float value;
		if (text instanceof Spanned) {
			Spanned spanned = (Spanned) text;
			value = measureSpannedChar(spanned, index, text.charAt(index),
					spanned.getSpans(index, index + 1, MetricAffectingSpan.class), wp, null);
		} else {
			value = measureChar(text, index, text.charAt(index), wp, null) + letterSpac;
		}
		return value > 0 ? value + letterSpac : 0;
	}

	protected boolean isSpecialLetter(char c, char lastSpecialChar, char lastChar, CharSequence source, int index) {
		if (mAttribute.lineFormat != null) {
			return mAttribute.lineFormat.mustNewLine(c, lastSpecialChar, lastChar, source, index);
		}
		return false;
	}

	protected boolean isNextNewLine(char c, CharSequence source, int index) {
		if (mAttribute.lineFormat != null) {
			return mAttribute.lineFormat.isNextNewLine(c, source, index);
		}
		return false;
	}

	/**
	 * 测量单独的char
	 * 
	 * @param text
	 *            当前的text
	 * @param charIndex
	 *            当前char的位置
	 * @param chs
	 *            当前char
	 * @param paint
	 * @param fm
	 * @return
	 */
	public abstract float measureChar(CharSequence text, int charIndex, char chs, TextAreaPaint paint,
			Paint.FontMetricsInt fm);

	/**
	 * 将测量的信息应用到指定的字符
	 * 
	 * @param text
	 *            需要提取测量信息的文本
	 * @param start
	 *            从哪里开始
	 * @param end
	 *            从哪里结束
	 * @param chs
	 *            指定的字符
	 * @param paint
	 * @return
	 */
	public abstract float measureReplaceChar(CharSequence text, int start, int end, String chs, TextAreaPaint paint);

	public abstract float measureSpannedChar(Spanned text, int charIndex, char chs, MetricAffectingSpan[] spans,
			TextAreaPaint paint, Paint.FontMetricsInt fm);

	protected abstract float measureEmoji(char start, char end, Paint paint, MetricAffectingSpan[] spans);

	public abstract Lineable measureSingleLineFromFrsit(CharSequence source, int minStart, int maxEnd, int start,
			int end, float width, int v, float offsetLeading, TextAreaPaint wp, Lineable line, TruncateAt e,
			float ellipsisWidth);

	public abstract Lineable measureSingleLineFromLast(CharSequence source, int minStart, int maxEnd, int end,
			int start, float width, int v, float offsetLeading, TextAreaPaint wp, Lineable line, TruncateAt e,
			float ellipsisWidth, boolean offsetLeadingIncludeMeasureWidths);

	public abstract Lineable measureSingleLine(CharSequence source, int start, int minStart, int maxEnd, float width,
			float offsetWidth, float offsetLeading, int v, int maxEms, Lineable line, boolean measureMode,
			TextAreaPaint wp);

	/**
	 * 计算首行与顶部的间隔
	 * @param above
	 * @param below
	 * @param extra
	 * @return
	 */
	protected abstract int computeFirstLineToTopWithSpacing(int above, int below, int extra);

	/**
	 * 
	 * @param source
	 * @param start
	 *            从那里开始测量,必须确保为本行最小起点{@code 及上行最大的终点}
	 * @param end
	 * @param v
	 *            从此处开始draw,即line的底边,相当于 {@link TextAreaLiner#getBottom()}
	 * @param line
	 * @return 如果Line为null,便返回new Line
	 */
	public abstract Lineable measureAfterLine(CharSequence source, int start, int end, int v, Lineable line,
			TextAreaPaint wp, float ellipsisWidth);

	public abstract Lineable measureBeforeLine(CharSequence source, int start, int end, int v, Lineable line,
			TextAreaPaint wp, float ellipsisWidth);

	protected static <T> T[] getParagraphSpans(Spanned text, int start, int end, Class<T> type) {
		if (start == end && start > 0) {
			return ArrayUtils.emptyArray(type);
		}
		T[] ts = text.getSpans(start, end, type);
		if (ts == null || ts.length == 0) {
			return null;
		}
		return ts;
	}

	public final int getToppadding() {
		return topPadding;
	}

	public final int getBottomPadding() {
		return bottomPadding;
	}

	void initSpecialLetterWidths(TextPaint paint) {
		paint.getTextWidths(SPECIAL_LETTERS, 0, SPECIAL_LETTERS.length, mSpecialLetterWidths);
	}

	protected float getSpecialLetterWidth(char c) {
		if (c < 128) {
			return mSpecialLetterWidths[c];
		}
		return mSpecialLetterWidths[128];
	}

	public static int codePointCount(CharSequence seq, int start, int end, int want, boolean asc) {
		if (seq == null) {
			throw new NullPointerException();
		} else if (end > seq.length()) {
			throw new ArrayIndexOutOfBoundsException("end = " + end + " but charsequence max length = " + seq.length());
		}
		int len = end;

		int result = 0;

		if (!asc) {
			for (int i = len - 1; i >= start; i--) {
				char c = seq.charAt(i);
				if (Character.isLowSurrogate(c)) {
					if (--i >= 0) {
						c = seq.charAt(i);
						if (!Character.isHighSurrogate(c)) {
							result++;
						}
					}
				}
				result++;
				if (result >= want) {
					return i;
				}
			}
			return 0;
		}

		for (int i = start; i < len; i++) {
			char c = seq.charAt(i);
			if (Character.isHighSurrogate(c)) {
				if (++i < len) {
					c = seq.charAt(i);
					if (!Character.isLowSurrogate(c)) {
						result++;
					}
				}
			}
			result++;
			if (result >= want) {
				return i + 1;
			}
		}
		return len;
	}

	protected void outEnd(Lineable liner, Spanned text, Paint.FontMetricsInt fm) {
		if (text == null) {
			return;
		}
		int minStart = liner.getMinStart();
		int maxEnd = liner.getMaxEnd();
		int bottom = liner.getBottom();
		int top = liner.getTop();
		/*
		 * 尽量不要使用LineHeightSpan,稍不留意,排版会零乱
		 */
		LineHeightSpan[] chooseHt = getParagraphSpans(text, minStart, maxEnd, LineHeightSpan.class);
		if (chooseHt != null) {
			for (int i = 0; i < chooseHt.length; i++) {
				chooseHt[i].chooseHeight(text, minStart, maxEnd, top, bottom, fm);
			}
			int below = fm.descent;
			int above = fm.ascent;
			int extra;
			float spacingMule = getSpacingMult();
			float spacingAdd = getSpacingAdd();
			if (spacingMule != 1 || spacingAdd != 0) {
				double ex = (below - above) * (spacingMule - 1) + spacingAdd;
				if (ex >= 0) {
					extra = (int) (ex + EXTRA_ROUNDING);
				} else {
					extra = -(int) (-ex + EXTRA_ROUNDING);
				}
			} else {
				extra = 0;
			}

			below += extra;
			if (liner.getBelow() != Integer.MIN_VALUE && liner.getBelow() > below) {
				below = liner.getBelow();
			}
			if (liner.getAbove() != Integer.MIN_VALUE && liner.getAbove() < above) {
				above = liner.getAbove();
			}
			liner.setAttribute(minStart, maxEnd, liner.getStart(), liner.getEnd(), above, below, top + (below - above),
					mAttribute.lineFactory);
		}
	}

	public void moveAfterLineForTopBase(Lineable liner, int topBase) {
		int above = liner.getAbove();
		int below = liner.getBelow();
		liner.setAttribute(liner.getMinStart(), liner.getMaxEnd(), liner.getStart(), liner.getEnd(), above, below,
				topBase + (below - above), mAttribute.lineFactory);
	}

	protected void out(CharSequence text, int minStart, int maxEnd, int start, int end, int above, int below, int top,
			int bottom, Paint.FontMetricsInt fm, int leadingMarger, float offsetLeading, int v, boolean isFirst,
			boolean isLast, TextAreaPaint wp, TruncateAt truncate, float ellipsisW, float letterSpac, float[] widths,
			int len, Lineable liner, boolean after, boolean destSetWidth) {

		if (isFirst) {
			if (isTrackpad()) {
				topPadding = top - above;
			}

			if (isIncludepad()) {
				above = top;
			}
		}
		if (isLast) {
			if (isTrackpad()) {
				bottomPadding = bottom - below;
			}

			if (isIncludepad()) {
				below = bottom;
			}
		}
		int extra;
		float spacingMule = getSpacingMult();
		float spacingAdd = getSpacingAdd();
		if (spacingMule != 1 || spacingAdd != 0) {
			double ex = (below - above) * (spacingMule - 1) + spacingAdd;
			if (ex >= 0) {
				extra = (int) (ex + EXTRA_ROUNDING);
			} else {
				extra = -(int) (-ex + EXTRA_ROUNDING);
			}
		} else {
			extra = 0;
		}
		below += extra;
		if (isFirstLineToTopWithSpacing() && isFirst) {
			above = computeFirstLineToTopWithSpacing(above, below, extra);
		}
		if (liner.getBelow() != Integer.MIN_VALUE && liner.getBelow() > below) {
			below = liner.getBelow();
		}
		if (liner.getAbove() != Integer.MIN_VALUE && liner.getAbove() < above) {
			above = liner.getAbove();
		}
		liner.setTruncateAt(getEllipsis(), ellipsisW, truncate);
		liner.setAttribute(minStart, maxEnd, start, end, above, below, after ? v + (below - above) : v,
				mAttribute.lineFactory);
		liner.setOppositeDraw(isOpposite());
		if (len > 0) {
			liner.setSiteByWidths(offsetLeading, leadingMarger, widths, len, letterSpac, destSetWidth);
		}
	}

	/**
	 * 获取默认的测量数值,包括{@link #getSpacingAdd()},{@link #getSpacingMult()},
	 * {@link #isFirstLineToTopWithSpacing()}
	 * 
	 * @node 只修改{@link FontMetricsInt#ascent}与{@link FontMetricsInt#descent}
	 * @param fm
	 */

	public final void getDefaultMeasure(FontMetricsInt fm) {
		int extra;
		int below = fm.descent;
		int above = fm.ascent;
		float spacingMule = getSpacingMult();
		float spacingAdd = getSpacingAdd();
		if (spacingMule != 1 || spacingAdd != 0) {
			double ex = (below - above) * (spacingMule - 1) + spacingAdd;
			if (ex >= 0) {
				extra = (int) (ex + EXTRA_ROUNDING);
			} else {
				extra = -(int) (-ex + EXTRA_ROUNDING);
			}
		} else {
			extra = 0;
		}
		below += extra;
		if (isFirstLineToTopWithSpacing()) {
			above = computeFirstLineToTopWithSpacing(above, below, extra);
		}
		fm.ascent = above;
		fm.descent = below;
	}

	/**
	 * 换行抽象类,可以自定义任意的换行规则
	 * 
	 * @author Maizer/麦泽
	 */
	public static class LineFormat {

		private static LineFormat mFormat = new LineFormat();

		public static LineFormat getInstance() {
			return mFormat;
		}

		/**
		 * 是否想要换行
		 * 
		 * @param c
		 *            当前字符
		 * @param lastSpecialChar
		 *            上一个想要换行的字符
		 * @param lastChar
		 *            上一个字符
		 * @param source
		 * @param index
		 *            当前字符的索引
		 * @return true move to new line
		 */
		public boolean mustNewLine(char c, char lastSpecialChar, char lastChar, CharSequence source, int index) {
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
				return false;
			}
			switch (c) {
			case '\'':
				return false;
			}
			return !Character.isDigit(c);
		}

		/**
		 * 与{@link #mustNewLine(char, char, char, CharSequence, int)}方法连锁响应
		 * 
		 * @param c
		 *            当前需要换行的字符
		 * @param source
		 * @param index
		 *            字符索引
		 * @return
		 * 
		 *         <pre>
		 * 例如: 
		 * abcd(.... 
		 * 
		 * char is "("
		 * 
		 *  if is false >
		 *  abcd(
		 *  ....
		 *  if is true >
		 *  abcd
		 *  (....
		 *         </pre>
		 */
		public boolean isNextNewLine(char c, CharSequence source, int index) {
			return false;
		}
	}
}
