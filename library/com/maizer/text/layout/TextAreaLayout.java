package com.maizer.text.layout;

import java.util.NoSuchElementException;

import com.maizer.text.factory.EmojiFactory;
import com.maizer.text.factory.LineFactory;
import com.maizer.text.liner.Lineable;
import com.maizer.text.measure.MeasureAttrubute;
import com.maizer.text.measure.Measurer;
import com.maizer.text.util.ArrayGc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.TextUtils.TruncateAt;

public abstract class TextAreaLayout implements ArrayGc {

	private static final String TAG = TextAreaLayout.class.getCanonicalName();

	/**
	 * get Lineable
	 * 
	 * @param raw
	 *            location
	 * @return
	 */
	protected abstract Lineable getTextLiner(int raw);

	/**
	 * 获取当前显示的行数 get show lines in screen
	 * 
	 * @return
	 */
	public abstract int getLineRecyleCount();

	/**
	 * 获取当前最大加载的行数 get max loaded lines
	 * 
	 * @return
	 */
	public abstract int getLineCount();

	/**
	 * 在可见的行中,通过文字位置获取行
	 * 
	 * @param offset
	 * @return -1,not found
	 */
	public abstract int getLineForOffset(int offset);

	/**
	 * 通过垂直距离获取行
	 * 
	 * @param offset
	 * @return -1,not found
	 */
	public abstract int getLineForVertical(int vertical);

	/**
	 * horizontal scroll
	 * 
	 * @param scrollX
	 * @return
	 */
	public abstract float scrollByHorizontal(float scrollX);

	/**
	 * vertical scroll
	 * 
	 * @param scrollY
	 * @return
	 */
	public abstract float scrollByVertical(float scrollY);

	/**
	 * 将显示的所有行基准调整到Scroll为0
	 * 
	 * @param scroller
	 * @return true 你需要修正Vertical Scroll位置到0,false 没有必须修正
	 */
	public abstract boolean amendTopVerticalLevelToZoer(TextAreaScroller scroller);

	/**
	 * 在可见的行中,通过offsetPoint垂直卷动
	 * 
	 * @param offsetPoint
	 *            word location
	 * @param scrollY
	 *            vertical scrollY
	 * @return
	 */
	public abstract float scrollByOffset(int offsetPoint, float scrollY);

	/**
	 * our text in full layout height
	 * 
	 * @return >0 text height > layout height ,== 0 text height == layout
	 *         height,otherwise
	 */
	public abstract int hasFullLayoutHeight();

	/**
	 * our text in full layout width
	 * 
	 * @return >0 text width > layout width ,== 0 text width== layout
	 *         width,otherwise
	 */
	public abstract int hasFullLayoutWidth();

	public abstract FontMetricsInt getDefaultFontMetricsInt();

	public abstract void draw(Canvas c);

	/**
	 * 更新测量信息 update measure information
	 * 
	 * @param index
	 * @param helper
	 * @return
	 */
	public abstract boolean restartMeasure(int index, TextAreaScroller helper);

	/**
	 * clear buffer
	 */
	public abstract void gc();

	/**
	 * view detached,layout die clear data
	 */
	public abstract void clear();

	private static EmojiFactory EMOJI_FACTORY = EmojiFactory.installDefault();
	private static int MIN_EMOJI, MAX_EMOJI;

	static {
		MIN_EMOJI = EMOJI_FACTORY.getMinimumAndroidPua();
		MAX_EMOJI = EMOJI_FACTORY.getMaximumAndroidPua();
	}

	public static final Bitmap getEmojiBitmap(int emoji) {
		if (emoji >= MIN_EMOJI && emoji <= MAX_EMOJI) {
			return EMOJI_FACTORY.getBitmapFromAndroidPua(emoji);
		}
		return null;
	}

	public static final boolean isEmojiEnable(int c) {
		if (EMOJI_FACTORY != null && c >= EMOJI_FACTORY.getCharFirstHighSuRRogate()
				&& c <= EMOJI_FACTORY.getCharLastLowSuRRogate()) {
			return true;
		}
		return false;
	}

	public static void setEmojiFactory(EmojiFactory ef) {
		if (ef != EMOJI_FACTORY) {
			EMOJI_FACTORY = ef;
			if (EMOJI_FACTORY != null) {
				MIN_EMOJI = EMOJI_FACTORY.getMinimumAndroidPua();
				MAX_EMOJI = EMOJI_FACTORY.getMaximumAndroidPua();
			} else {
				MIN_EMOJI = -1;
				MAX_EMOJI = -1;
			}
		}
	}

	private LayoutAttrubute mAttribute;
	private CharSequence mText;
	private TextAreaPaint mPaint;

	private Drawable mTextHighlightDrawable = new ColorDrawable(Color.parseColor("#dddddd"));

	private int maxHeight;

	private boolean inLayout;
	private boolean isSpanned;

	protected TextAreaLayout(LayoutAttrubute attrubute, TextAreaPaint wp, CharSequence text) {
		mAttribute = attrubute;
		maxHeight = attrubute.height;
		mPaint = wp;
		mText = text;
		isSpanned = mText instanceof Spanned;
	}

	protected Measurer newMeasurer() {
		return mAttribute.buildMeasurer();
	}

	public void setTextHighlightDrawable(Drawable draw) {
		mTextHighlightDrawable = draw;
	}

	public Drawable getTextHighlightDrawable() {
		return mTextHighlightDrawable;
	}

	public final LineFactory getLineFactory() {
		return mAttribute.lineFactory;
	}

	public final int getInitArraySize() {
		return mAttribute.initArraySize;
	}

	public int getDefaultHeight() {
		LineFactory mFactory = getLineFactory();
		FontMetricsInt fm = getDefaultFontMetricsInt();
		int below = fm.descent;
		int above = fm.ascent;
		int bottom = below - above;
		int top = mFactory.computeLineTop(bottom, below, above);
		return bottom - top;
	}

	public void setTextHighlightColor(int color) {
		if (mTextHighlightDrawable instanceof ColorDrawable) {
			((ColorDrawable) mTextHighlightDrawable).setColor(color);
		} else {
			mTextHighlightDrawable = new ColorDrawable(color);
		}
	}

	public void setText(CharSequence text) {
		mText = text;
		isSpanned = mText instanceof Spanned;
	}

	public final CharSequence getText() {
		return mText;
	}

	protected final boolean isSpanned() {
		return isSpanned;
	}

	public final boolean isSingleLine() {
		return mAttribute.maxLines == 1;
	}

	protected boolean inLayout() {
		return inLayout;
	}

	/**
	 * 刷新布局
	 * 
	 * @param vh
	 * @return
	 */
	public boolean restartLayout(TextAreaScroller vh) {
		if (mAttribute.width == 0 || mAttribute.height == 0) {
			if (mAttribute.height == 0) {
				maxHeight = 0;
			}
			return false;
		}
		return true;
	}

	public final boolean isWrapHeight() {
		return mAttribute.maxLinesLimitHeight == -1;
	}

	public final boolean isWrapWidth() {
		return mAttribute.width == -1;
	}

	/**
	 * 检查与修正高度,通过{@link MeasureAttrubute#maxLinesLimitHeight}配置
	 * 
	 * @return 改变后的高
	 */
	protected int checkLimitLines() {
		int maxLimitLines = getMaxLinesLimitHeight();
		int count = getLineRecyleCount() - 1;
		if (maxLimitLines == -1) {
			if (count >= 0) {
				int height;
				if (isSingleLine()) {
					height = getTextLiner(0).getHeight();
				} else {
					if (getTextLiner(0).getMinStart() > getTextMinStart(mText)) {
						mAttribute.height = maxHeight;
						return maxHeight;
					}
					height = getTextLiner(count).getBottom() - getTextLiner(0).getTop();
				}
				if (height < maxHeight) {
					mAttribute.height = height;
				} else {
					mAttribute.height = maxHeight;
				}
			} else {
				mAttribute.height = getDefaultHeight();
			}
		} else if (maxLimitLines > 0) {
			if (count >= maxLimitLines) {
				int maxH = getTextLiner(maxLimitLines - 1).getBottom() - getTextLiner(0).getTop();
				if (mAttribute.height > maxH) {
					mAttribute.height = maxH;
				}
			} else if (count >= 0) {
				if (getTextLiner(0).getMinStart() > getTextMinStart(mText)) {
					mAttribute.height = maxHeight;
					return maxHeight;
				}
				int height = getTextLiner(count).getBottom() - getTextLiner(0).getTop();
				if (height < maxHeight) {
					mAttribute.height = height;
				} else {
					mAttribute.height = maxHeight;
				}
			} else {
				mAttribute.height = getDefaultHeight();
			}
		}
		return mAttribute.height;
	}

	/**
	 * 重置高度和宽度
	 * 
	 * @param offsetPosition
	 *            从那一个点开始
	 * @param w
	 *            宽度
	 * @param h
	 *            高度
	 * @param vh
	 * @return
	 */
	public boolean restartLayoutSize(int offsetPosition, int w, int h, TextAreaScroller vh) {
		w -= mAttribute.paddingLeft + mAttribute.paddingRight;
		h -= mAttribute.paddingTop + mAttribute.paddingBottom;
		maxHeight = h;
		inLayout = true;
		if (mAttribute.width != w) {
			mAttribute.width = w;
			mAttribute.height = h;
			restartLayout(vh);
		} else if (mAttribute.height == h) {
			scrollByVertical(vh == null ? 0 : vh.getTextScrollY());
			inLayout = false;
			checkLimitLines();
			return false;
		} else if (mAttribute.height > h) {
			mAttribute.height = h;
			int scroll = vh == null ? 0 : vh.getTextScrollY();
			int count = getLineRecyleCount();
			if (count > 0) {
				if (getTextLiner(count - 1).getMaxEnd() > offsetPosition) {
					float sc = scrollByOffset(offsetPosition, scroll);
					if (sc < 0 && vh != null) {
						vh.requestTextScrollBy(0, (int) -sc);
					}
				}
			}
		} else {
			if (!isSingleLine() && hasFullLayoutHeight() >= 0) {
				mAttribute.height = h;
				int count = getLineRecyleCount() - 1;
				if (count >= 0) {
					int maxLimitLines = getMaxLinesLimitHeight();
					int scroll = vh == null ? 0 : vh.getTextScrollY();
					float v = scrollByVertical(scroll);
					count = getLineRecyleCount() - 1;
					if (maxLimitLines > 0 && maxLimitLines <= count) {
						int realH = checkLimitLines();
						if (h > realH) {
							inLayout = false;
							return true;
						}
					}
					if (getLineMax(count) == getTextMaxEnd(mText) || getLineCount() == getMaxLines()) {
						v = scrollByVertical(getTextLiner(count).getBottom() - h);
						v = scrollByVertical(v);
						if (vh != null)
							vh.requestTextScrollBy(0, (int) (v - scroll));
					}
				}
			} else {
				mAttribute.height = h;
			}
		}
		inLayout = false;
		checkLimitLines();
		return true;
	}

	protected final int getTextMinStart(CharSequence text) {
		int maxEms = mAttribute.maxEms;
		TruncateAt truncateAt = mAttribute.truncateAt;
		if (maxEms > 0 && truncateAt != null) {
			switch (truncateAt) {
			case MIDDLE:
			case START:
				return Measurer.codePointCount(text, 0, text.length(), maxEms, false);
			}
		}
		return 0;
	}

	protected final int getTextMaxEnd(CharSequence text) {
		int length = text.length();
		int maxEms = mAttribute.maxEms;
		TruncateAt truncateAt = mAttribute.truncateAt;
		if (maxEms > 0 && maxEms < length && (truncateAt == null || truncateAt == TruncateAt.END)) {
			return Measurer.codePointCount(text, 0, text.length(), maxEms, true);
		}
		return length;
	}

	public final TextAreaPaint getPaint() {
		return mPaint;
	}

	public final TruncateAt getTruncateAt() {
		return mAttribute.truncateAt;
	}

	public final String getEllipsis() {
		return mAttribute.ellipsize;
	}

	public final float getLetterSpacing() {
		return mPaint.getLetterSpacing();
	}

	public final int getWidth() {
		return mAttribute.width;
	}

	public final int getMaxEms() {
		return mAttribute.maxEms;
	}

	public final int getMaxLinesLimitHeight() {
		return mAttribute.maxLinesLimitHeight;
	}

	public final int getMaxLines() {
		return mAttribute.maxLines;
	}

	public final int getHeight() {
		return mAttribute.height;
	}

	/**
	 * 最大原始高度,>={@link LayoutBuilder#height}
	 * 
	 * @return max height
	 */
	protected final int getMaxHeight() {
		return maxHeight;
	}

	/**
	 * 当前循环{@link #getLineRecyleCount()}中的最小行与最大行距离
	 * 
	 * @return
	 */
	public final int getTextHieght() {
		if (getLineRecyleCount() > 0) {
			return getTextLiner(getLineRecyleCount() - 1).getOffsetBottom() - getTextLiner(0).getOffsetTop();
		}
		return 0;
	}

	public final int getPaddingLeft() {
		return mAttribute.paddingLeft;
	}

	public int getPaddingTop() {
		return mAttribute.paddingTop;
	}

	public final int getPaddingRight() {
		return mAttribute.paddingRight;
	}

	public final int getPaddingBottom() {
		return mAttribute.paddingBottom;
	}

	/**
	 * Return the base alignment of this layout.
	 */
	public final Alignment getAlignment() {
		return mAttribute.alignment;
	}

	/**
	 * Return what the text height is multiplied by to get the line height.
	 */
	public final float getSpacingMultiplier() {
		return mAttribute.spacingMult;
	}

	protected MeasureAttrubute getMeasureAttrubute() {
		return mAttribute;
	}

	public int getLineHeight(int line) {
		return checkTextLinear(line).getHeight();
	}

	public float getCarelessMaxLineBottom() {
		int lineCount = getLineRecyleCount();
		if (lineCount <= 0) {
			return 0;
		}
		Lineable lastLine = getTextLiner(lineCount - 1);
		int lastBottom = lastLine.getOffsetBottom();
		if (lastBottom <= getHeight()) {
			return 0;
		}
		Lineable firstLine = getTextLiner(0);
		int wcount = lastLine.getMaxEnd() - firstLine.getMinStart();
		int eachwcount = wcount / lineCount;
		int lines = getText().length() / eachwcount;
		int firstTop = firstLine.getTop();
		int eachHeight = (lastBottom - firstTop) / lineCount;
		return eachHeight * lines;
	}

	/**
	 * Return the number of units of leading that are added to each line.
	 */
	public final float getSpacingAdd() {
		return mAttribute.spacingAdd;
	}

	/**
	 * 通过水平距离获取循环行中的Text字符位置
	 * 
	 * @param line
	 *            那一个循环中的行{@link #getLineRecyleCount()}
	 * @param horiz
	 *            水平距离
	 * @return 字符位置
	 */
	public int getOffsetForHorizontal(int line, float horiz) {
		return checkTextLinear(line).getOffsetForHorizontal(horiz - getPaddingLeft());
	}

	/**
	 * 获取水平距离从指定的行与指定的字符位置
	 * 
	 * @param line
	 *            指定的行
	 * @param offset
	 *            >={@link #getLineStart(int)} <={@link #getLineEnd(int)}
	 * @param setSelect
	 *            是否添加选择
	 * @param isSelecting
	 *            是否在选择模式
	 * @return 水平距离
	 */
	public float getHorizontalForOffset(int line, int offset, boolean setSelect, boolean isSelecting) {
		return checkTextLinear(line).getHorizontalForOffset(getText(), offset, getLetterSpacing(), setSelect,
				isSelecting) + getPaddingLeft();
	}

	public int getLineTop(int line) {
		if (line == getLineRecyleCount()) {
			return getLineBottom(line) + getPaddingTop();
		}
		return checkTextLinear(line).getTop() + getPaddingTop();
	}

	public int getLineOffsetTop(int line) {
		if (line == getLineRecyleCount()) {
			return getLineOffsetBottom(line);
		}
		return checkTextLinear(line).getOffsetTop() + getPaddingTop();
	}

	public int getLineOffsetBottom(int line) {
		return checkTextLinear(line).getOffsetBottom() + getPaddingTop();
	}

	public int getLineBottom(int line) {
		return checkTextLinear(line).getBottom() + getPaddingTop();
	}

	public int getLineStart(int line) {
		return checkTextLinear(line).getStart();
	}

	public int getLineEnd(int line) {
		return checkTextLinear(line).getEnd();
	}

	/**
	 * Get the ascent of the text on the specified line. The return value is
	 * negative to match the Paint.ascent() convention.
	 */
	public int getLineAscent(int line) {
		return checkTextLinear(line).getAbove();
	}

	public int getLineDescent(int line) {
		return checkTextLinear(line).getBelow();
	}

	public int getLineBounds(int line, Rect bounds) {
		Lineable mLine = checkTextLinear(line);
		int top = getPaddingTop();
		if (bounds != null) {
			int left = getPaddingLeft();
			bounds.left = (int) mLine.getMinSite() + left;
			bounds.right = (int) mLine.getMaxSite() + left;
			bounds.top = mLine.getTop() + top;
			bounds.bottom = mLine.getBottom() + top;
		}
		return mLine.getBottom() + top;
	}

	public float getLineLeft(int line) {
		return checkTextLinear(line).getMinSite() + getPaddingLeft();
	}

	public float getLineRight(int line) {
		return checkTextLinear(line).getMaxSite() + getPaddingLeft();
	}

	/**
	 * 获取最大的终点
	 * 
	 * @param line
	 * @return 最大终点
	 */
	public int getLineMax(int line) {
		return checkTextLinear(line).getMaxEnd();
	}

	/**
	 * 获取最小的起点
	 * 
	 * @param line
	 * @return 最小起点
	 */
	public int getLineMin(int line) {
		return checkTextLinear(line).getMinStart();
	}

	public boolean checkFirstLineIsTextMin() {
		if (getLineRecyleCount() <= 0) {
			return true;
		}
		return checkTextLinear(0).getMinStart() == getTextMinStart(mText);
	}

	public boolean checkFirstLineIsTextMax() {
		int count = getLineRecyleCount();
		if (count <= 0) {
			return true;
		}
		if (getLineCount() >= getMaxLines()) {
			return true;
		}
		return checkTextLinear(count - 1).getMaxEnd() == getTextMaxEnd(mText);
	}

	public float getLineWidth(int line) {
		Lineable mLine = checkTextLinear(line);
		return mLine.getMaxSite() - mLine.getMinSite();
	}

	private Lineable checkTextLinear(int line) {
		Lineable mLine = getTextLiner(line);
		if (mLine == null) {
			throw new NoSuchTextLine(line, getLineRecyleCount());
		}
		return mLine;
	}

	public enum Alignment {
		ALIGN_NORMAL, ALIGN_CENTER, ALIGN_LEFT, ALIGN_RIGHT,
		/**
		 * @hide I don't handle,maybe has Bugs
		 */
		ALIGN_OPPOSITE
	}

	public static class NoSuchTextLine extends NoSuchElementException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public NoSuchTextLine(int index, int size) {
			super("not find " + index + " line!" + " array size == " + size);
		}
	}

}
