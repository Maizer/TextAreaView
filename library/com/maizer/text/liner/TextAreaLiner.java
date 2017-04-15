package com.maizer.text.liner;

import com.maizer.text.BuildConfig;
import com.maizer.text.layout.TextAreaLayout;
import com.maizer.text.layout.TextAreaPaint;
import com.maizer.text.measure.LineMeasurer;
import com.maizer.text.measure.Measurer;
import com.maizer.text.util.ArrayUtils;
import com.maizer.text.util.FloatLinked;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.text.style.CharacterStyle;
import android.util.Log;

/**
 * 
 * 文本属性对象
 * 
 * @author Maizer/麦泽
 *
 */
public class TextAreaLiner implements Lineable {

	private static final String TAG = TextAreaLiner.class.getSimpleName();

	/**
	 * 记录当前的起点与终点
	 */
	private int mStart, mEnd;
	/**
	 * 在本行中,最小的起点,最大的终点
	 */
	private int minStart, maxEnd;

	/**
	 * 行的最低端
	 */
	private int mBottom, mTop;
	private int offsetTop, offsetBottom;
	/**
	 * 用于行缩进
	 */
	private int mLendingMargin;
	/**
	 * Word的位置
	 */
	private int mAbove = Integer.MIN_VALUE, mBelow = Integer.MIN_VALUE;
	private String ellipsize;
	private float ellipsisWidth;
	/**
	 * only is {@link TruncateAt#END} or {@link TruncateAt#START} in between.
	 */
	private TruncateAt truncateAt;
	/**
	 * 当前所有能显示文字的位置起点
	 */
	private FloatLinked mWordSites;
	private boolean isPositive;
	// private LineFactory mFactory;

	/**
	 * 这是一行文字对象,保存行属性
	 */

	public TextAreaLiner() {
	}

	public int getMinStart() {
		return minStart;
	}

	public int getMaxEnd() {
		return maxEnd;
	}

	public int getStart() {
		return mStart;
	}

	protected void setStart(int start) {
		this.mStart = start;
	}

	protected void setEnd(int end) {
		this.mEnd = end;
	}

	public boolean isPositive() {
		return isPositive;
	}

	public void setOppositeDraw(boolean p) {
		isPositive = p;
	}

	public int getEnd() {
		return mEnd;
	}

	public void setMinStart(int minStart) {
		this.minStart = minStart;
	}

	public void setMaxEnd(int maxEnd) {
		this.maxEnd = maxEnd;
	}

	public int getBottom() {
		return mBottom;
	}

	public int getBelow() {
		return mBelow;
	}

	public void setLength(int start, int end) {
		if (start > end) {
			throw new IllegalArgumentException("strat>end");
		} else if (start < 0) {
			throw new IllegalArgumentException("start < 0");
		}
		this.mStart = start;
		this.mEnd = end;
	}

	public int getOffsetTop() {
		return offsetTop;
	}

	public int getOffsetBottom() {
		return offsetBottom;
	}

	public int getTop() {
		return mTop;
	}

	public int getAbove() {
		return mAbove;
	}

	public int getOffsetForHorizontal(float horiz) {
		return 0;
	}

	public int getIndexBySite(float site) {
		return mWordSites.getIndexByValue(site);
	}

	public void setSiteByWidths(float offsetLeading, int leadingMarger, float[] ws, int len, float letterSpac,
			boolean positive) {
		mLendingMargin = leadingMarger;
		if (positive) {
			float w = offsetLeading == -1 ? leadingMarger : offsetLeading;
			if (mWordSites == null) {
				mWordSites = new FloatLinked();
			} else if (mWordSites.size() > 0) {
				w = mWordSites.removeLast();
			}
			for (int i = 0; i < len; i++) {
				mWordSites.add(w);
				if (ws[i] == LineMeasurer.EMOjI_FLAG) {
				} else if (ws[i] != -1) {
					w += ws[i] + letterSpac;
				}
			}
			mWordSites.add(w);
			return;
		}
		float w = offsetLeading == -1 ? leadingMarger : offsetLeading;
		if (mWordSites == null) {
			mWordSites = new FloatLinked();
		} else if (mWordSites.size() > 0) {
			w = mWordSites.removeLast();
		}
		for (int i = len - 1; i >= 0; i--) {
			mWordSites.add(w);
			if (ws[i] == LineMeasurer.EMOjI_FLAG) {
			} else if (ws[i] != -1) {
				w += ws[i] + letterSpac;
			}
		}
		mWordSites.add(w);
	}

	public void copy(TextAreaLiner line) {
		mWordSites = line.mWordSites;
		mAbove = line.mAbove;
		mBottom = line.mBottom;
		truncateAt = line.truncateAt;
		mBelow = line.mBelow;
		mStart = line.mStart;
		mEnd = line.mEnd;
		minStart = line.minStart;
		maxEnd = line.maxEnd;
	}

	public void clear() {
		if (mWordSites != null) {
			this.mWordSites.clear();
		}
		this.mLendingMargin = 0;
		this.mBelow = Integer.MIN_VALUE;
		this.mAbove = Integer.MIN_VALUE;
		this.mBottom = 0;
		this.mStart = 0;
		this.mEnd = 0;
		this.minStart = 0;
		this.maxEnd = 0;
		this.ellipsisWidth = 0;
		this.truncateAt = null;
		this.ellipsize = null;
	}

	protected FloatLinked getSites() {
		return mWordSites;
	}

	public TruncateAt getTruncateAt() {
		return truncateAt;
	}

	@Override
	public void setTruncateAt(String ellipsize, float ellipsisWidth, TruncateAt truncateAt) {
		this.ellipsize = ellipsize;
		this.ellipsisWidth = ellipsisWidth;
		this.truncateAt = truncateAt;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Widths size:" + (mWordSites == null ? 0 : mWordSites.size()));
		return sb.toString();
	}

	public int getSiteSize() {
		if (mWordSites == null) {
			return 0;
		}
		return mWordSites.size();
	}

	public float moveHorizontal(float scrollX, CharSequence text, TextAreaPaint wp, Measurer measurer) {
		int maxW = measurer.getWidth();
		if (mWordSites.size() > 0) {
			float value = mWordSites.toFromFirst(0, true);
			if (scrollX <= value && mStart > minStart) {
				do {
					mStart--;
					float w;
					if ((w = measurer.getEmoji(text, wp, mStart - 1, true)) != -1) {
						mStart--;
						mWordSites.addFirst(value -= w);
						mWordSites.addFirst(value);
					} else if (text.charAt(mStart) == Measurer.CHAR_TAB) {
						w = measurer.getTabMeasure(text, mStart, wp);
						mWordSites.addFirst(value -= w);
					} else {
						w = measurer.measureChar(text, wp, mStart);
						mWordSites.addFirst(value -= w);
					}
				} while (scrollX <= value && mStart > minStart);
				if (mStart == minStart) {
					if (scrollX <= getMinSite() - mLendingMargin) {
						scrollX = getMinSite() - mLendingMargin;
					}
				}
			} else if (scrollX + maxW >= (value = mWordSites.toFromLast(0, true)) && mEnd < maxEnd) {
				do {
					float w;
					if ((w = measurer.getEmoji(text, wp, mEnd, true)) != -1) {
						mWordSites.addLast(value);
						mEnd++;
					} else if (text.charAt(mStart) == Measurer.CHAR_TAB) {
						w = measurer.getTabMeasure(text, mStart, wp);
					} else {
						w = measurer.measureChar(text, wp, mEnd);
					}
					mWordSites.addLast(value += w);
					mEnd++;
				} while (scrollX + maxW >= value && mEnd < maxEnd);
				if (mEnd == maxEnd) {
					if (scrollX + maxW >= getMaxSite()) {
						scrollX = getMaxSite() - maxW;
					}
				}
			}
			mWordSites.toFromFirst(0, true);
			if (mWordSites.hasNext()) {
				mWordSites.next();
				if (mWordSites.getNext() != -1 && scrollX >= mWordSites.getNext() && mWordSites.hasNext()) {
					if (mEnd >= maxEnd) {
						if (scrollX + maxW >= getMaxSite()) {
							scrollX = getMaxSite() - maxW;
						}
					}
					do {
						mWordSites.removeFirst();
						mStart++;
						mWordSites.next();
					} while (mWordSites.getNext() != -1 && scrollX >= mWordSites.getNext() && mWordSites.hasNext());
					if (measurer.getEmoji(text, wp, mStart - 1, false) != -1) {
						mWordSites.removeFirst();
						mStart++;
					}
				} else {
					if (mStart <= minStart) {
						if (scrollX < getMinSite() - mLendingMargin) {
							scrollX = getMinSite() - mLendingMargin;
						}
					}
					mWordSites.toFromLast(0, true);
					if (mWordSites.hasPrevoious()) {
						value = mWordSites.prevoious();
						while (scrollX + maxW < value && mWordSites.hasPrevoious()) {
							mWordSites.removeLast();
							value = mWordSites.prevoious();
							mEnd--;
						}
					}
				}
			}
		}

		if (mEnd >= maxEnd) {
			if (scrollX + maxW >= getMaxSite()) {
				return getMaxSite() - maxW;
			}
		} else if (mStart <= minStart) {
			if (scrollX <= getMinSite() - mLendingMargin) {
				return getMinSite() - mLendingMargin;
			}
		}
		return -1;
	}

	@Deprecated
	public int hasEmoji(int p) {
		return mWordSites.equalsSome(p - mStart);
	}

	private void setSelect(Spannable spanned, int selectStart, int select) {
		if (!isPositive) {
			select = mEnd - select + mStart;
			if (select == mStart) {
				select += 1;
			} else if (select == maxEnd) {
				select = 0;
			}
		}
		if (selectStart == -1) {
			Selection.setSelection(spanned, select);
		} else {
			Selection.setSelection(spanned, selectStart, select);
		}
	}

	public int getHeight() {
		return getBottom() - getTop();
	}

	@Override
	public float getMaxSite() {
		if (mWordSites == null || mWordSites.size() <= 0) {
			return 0;
		}
		float lastSite = mWordSites.getLast();
		if (maxEnd <= mEnd && ellipsisWidth > 0 && truncateAt == TruncateAt.END || truncateAt == null) {
			return lastSite + ellipsisWidth;
		}
		return lastSite;
	}

	public float getMinSite() {
		if (mWordSites == null || mWordSites.size() <= 0) {
			return 0;
		}
		return mWordSites.getFirst();
	}

	@Override
	public void drawText(CharSequence text, Canvas canvas, TextPaint wp, float x, float y) {
		float top = mBottom - mBelow + y;
		mWordSites.toFromFirst(0, false);
		char[] cs = new char[1];
		if (isPositive) {
			if (minStart == mStart && truncateAt == TruncateAt.START) {
				x += ellipsisWidth;
			}
			for (int i = mStart; i < text.length() && i < mEnd && mWordSites.hasNext(); i++) {
				float w = mWordSites.next();
				char c = text.charAt(i);
				if (c == Measurer.CHAR_TAB || c == Measurer.CHAR_NEW_LINE) {
					continue;
				}
				if (TextAreaLayout.isEmojiEnable(c)) {
					Bitmap bm;
					int emoji = Character.codePointAt(text, i);
					if ((bm = TextAreaLayout.getEmojiBitmap(emoji)) != null) {
						drawBitmap(bm, canvas, w);
						if (mWordSites.getNext() == w) {
							i++;
							mWordSites.next();
						}
					} else if (mWordSites.getNext() == w) {
						canvas.drawText(text, i, i + 2, w + x, top, wp);
						i++;
						mWordSites.next();
						continue;
					}
				} else {
					cs[0] = c;
					canvas.drawText(cs, 0, 1, w + x, top, wp);
				}
			}
		} else {
			for (int i = mEnd - 1; i >= mStart && mWordSites.hasNext(); i++) {
				float w = mWordSites.next();
				char c = text.charAt(i);
				if (c == Measurer.CHAR_TAB || c == Measurer.CHAR_NEW_LINE) {
					continue;
				}
				if (TextAreaLayout.isEmojiEnable(c)) {
					Bitmap bm;
					int emoji = Character.codePointAt(text, i);
					if ((bm = TextAreaLayout.getEmojiBitmap(emoji)) != null) {
						drawBitmap(bm, canvas, w);
						if (mWordSites.getNext() == w) {
							i++;
							mWordSites.prevoious();
						}
					} else if (mWordSites.getNext() == w) {
						canvas.drawText(text, i, i + 2, w + x, top, wp);
						i++;
						mWordSites.prevoious();
						continue;
					}
				} else {
					cs[0] = c;
					canvas.drawText(cs, 0, 1, w + x, top, wp);
				}
			}
		}
		drawEllipsis(canvas, x, top, wp, null, text);
	}

	@SuppressLint("NewApi")
	private void drawEllipsis(Canvas canvas, float offserx, float top, TextPaint wp, TextPaint wePaint,
			CharSequence text) {
		if (ellipsisWidth > 0 && ellipsize != null) {
			TextPaint tp = null;
			float x = 0;
			if (mEnd == maxEnd && truncateAt == TruncateAt.END || truncateAt == null) {
				if (text instanceof Spanned && wePaint != null) {
					CharacterStyle[] spns = getParagraphSpans((Spanned) text, maxEnd, text.length(),
							CharacterStyle.class);
					if (spns == null || spns.length <= 0) {
						tp = wp;
					} else {
						wePaint.set(wp);
						for (CharacterStyle span : spns) {
							span.updateDrawState(wePaint);
						}
						tp = wePaint;
					}
				} else {
					tp = wp;
				}
				float letterSpac = 0;
				if (wp instanceof TextAreaPaint) {
					letterSpac = ((TextAreaPaint) wp).getLetterSpacing() / 2;
				}
				x = mWordSites.getLast() + offserx - letterSpac;
			} else if (mStart == minStart && truncateAt == TruncateAt.START) {
				if (text instanceof Spanned && wePaint != null) {
					CharacterStyle[] spns = getParagraphSpans((Spanned) text, 0, mStart, CharacterStyle.class);
					if (spns == null || spns.length <= 0) {
						tp = wp;
					} else {
						wePaint.set(wp);
						for (CharacterStyle span : spns) {
							span.updateDrawState(wePaint);
						}
						tp = wePaint;
					}
				} else {
					tp = wp;
				}
				// offserx - ellipsisWidth + mLendingMargin
				x = offserx != 0 ? mWordSites.getFirst() - ellipsisWidth + offserx : mWordSites.getFirst();
			}
			if (tp != null) {
				char[] chs = ellipsize.toCharArray();
				canvas.drawText(chs, 0, chs.length, x, top, tp);
			}
		}
	}

	protected void drawBitmap(Bitmap bp, Canvas canvas, float x) {
	}

	protected void drawUnderline(Canvas canvas, TextPaint wePaint, int start, int end, float top, float offsetx) {
		if (wePaint instanceof TextAreaPaint) {
			TextAreaPaint tap = (TextAreaPaint) wePaint;
			if (tap.isUnderlineTextEnable()) {
				int color = tap.getUnderLineColor();
				if (color != 0) {
					int thickness = tap.getUnderlineThickness();
					int bottom = getBottom();
					int fromY = getOffsetBottom();
					int toY = fromY + thickness;
					float fromX = mWordSites.get(start - mStart) + offsetx;
					float toX = mWordSites.get(end - mStart) + offsetx;
					if (truncateAt == TruncateAt.START) {
						fromX -= ellipsisWidth;
					} else if (truncateAt == TruncateAt.END || truncateAt == null) {
						toX += ellipsisWidth;
						if (ellipsisWidth == 0 && end == mEnd) {
							toX -= tap.getLetterSpacing();
						}
					}
					tap.setColor(color);
					canvas.drawRect(fromX, fromY, toX, toY > bottom ? bottom : toY, tap);
				}
			}
		}
	}

	protected static <T> T[] getParagraphSpans(Spanned text, int start, int end, Class<T> type) {
		if (start == end && start > 0) {
			return ArrayUtils.emptyArray(type);
		}
		return text.getSpans(start, end, type);
	}

	public float getEllipsisWidth() {
		return ellipsisWidth;
	}

	@Override
	public void drawSpanned(Spanned spanned, Canvas canvas, TextPaint wp, TextPaint wePaint, Drawable background,
			int bstart, int bend, int btop, int bbottom, float x, float y, float letterspacing) {
		drawBackground(background, canvas, bstart, bend, btop, bbottom, x, y, letterspacing);
		float top = mBottom - mBelow + y;
		int start = mStart;
		int end = mEnd;
		int spanEnd;
		if (isPositive) {
			mWordSites.toFromFirst(0, false);
			if (minStart == mStart && truncateAt == TruncateAt.START) {
				x += ellipsisWidth;
			}
		} else {
			mWordSites.toFromLast(0, true);
		}
		while ((spanEnd = spanned.nextSpanTransition(start, end, CharacterStyle.class)) != start
				&& spanEnd <= spanned.length()) {
			char[] cs = new char[1];
			try {
				CharacterStyle[] spans = getParagraphSpans(spanned, start, spanEnd, CharacterStyle.class);
				if (spans.length > 0) {
					wePaint.set(wp);
					for (CharacterStyle span : spans) {
						span.updateDrawState(wePaint);
					}
					wePaint.setAlpha(wp.getAlpha());
					if (isPositive) {
						for (int i = start; i < spanEnd && mWordSites.hasNext(); i++) {
							float w = mWordSites.next();
							char c = spanned.charAt(i);
							if (c == Measurer.CHAR_TAB || c == Measurer.CHAR_NEW_LINE) {
								continue;
							}
							if (TextAreaLayout.isEmojiEnable(c)) {
								Bitmap bm;
								int emoji = Character.codePointAt(spanned, i);
								if ((bm = TextAreaLayout.getEmojiBitmap(emoji)) != null) {
									drawBitmap(bm, canvas, w);
									if (mWordSites.getNext() == w) {
										i++;
										mWordSites.next();
									}
								} else if (mWordSites.getNext() == w) {
									canvas.drawText(spanned, i, i + 2, w + x, top, wePaint);
									i++;
									mWordSites.next();
									continue;
								}
							} else {
								cs[0] = c;
								canvas.drawText(cs, 0, 1, w + x, top, wePaint);
							}
						}
						drawUnderline(canvas, wePaint, start, spanEnd, top, x);
						drawEllipsis(canvas, x, top, wp, wePaint, spanned);
					} else {
						for (int i = start; i < spanEnd && mWordSites.hasPrevoious(); i++) {
							float w = mWordSites.prevoious();
							char c = spanned.charAt(i);
							if (c == Measurer.CHAR_TAB || c == Measurer.CHAR_NEW_LINE) {
								continue;
							}
							if (TextAreaLayout.isEmojiEnable(c)) {
								Bitmap bm;
								int emoji = Character.codePointAt(spanned, i);
								if ((bm = TextAreaLayout.getEmojiBitmap(emoji)) != null) {
									drawBitmap(bm, canvas, w);
									if (mWordSites.getNext() == w) {
										i++;
										mWordSites.prevoious();
									}
								} else if (mWordSites.getNext() == w) {
									canvas.drawText(spanned, i, i + 2, w + x, top, wePaint);
									i++;
									mWordSites.prevoious();
									continue;
								}
							} else {
								cs[0] = c;
								canvas.drawText(cs, 0, 1, w + x, top, wePaint);
							}
						}
					}
				} else {
					if (isPositive) {
						for (int i = start; i < spanEnd && mWordSites.hasNext(); i++) {
							float w = mWordSites.next();
							char c = spanned.charAt(i);
							if (c == Measurer.CHAR_TAB || c == Measurer.CHAR_NEW_LINE) {
								continue;
							}
							if (TextAreaLayout.isEmojiEnable(c)) {
								Bitmap bm;
								int emoji = Character.codePointAt(spanned, i);
								if ((bm = TextAreaLayout.getEmojiBitmap(emoji)) != null) {
									drawBitmap(bm, canvas, w);
									if (mWordSites.getNext() == w) {
										i++;
										mWordSites.next();
									}
								} else if (mWordSites.getNext() == w) {
									canvas.drawText(spanned, i, i + 2, w + x, top, wp);
									i++;
									mWordSites.next();
									continue;
								}
							} else {
								cs[0] = c;
								canvas.drawText(cs, 0, 1, w + x, top, wp);
							}
						}
						drawUnderline(canvas, wp, start, spanEnd, top, x);
						drawEllipsis(canvas, x, top, wp, wp, spanned);
					} else {
						for (int i = start; i < spanEnd && mWordSites.hasPrevoious(); i++) {
							float w = mWordSites.prevoious();
							char c = spanned.charAt(i);
							if (c == Measurer.CHAR_TAB || c == Measurer.CHAR_NEW_LINE) {
								continue;
							}
							if (TextAreaLayout.isEmojiEnable(c)) {
								Bitmap bm;
								int emoji = Character.codePointAt(spanned, i);
								if ((bm = TextAreaLayout.getEmojiBitmap(emoji)) != null) {
								} else if (mWordSites.getNext() == w) {
									canvas.drawText(spanned, i, i + 2, w + x, top, wp);
									i++;
									mWordSites.prevoious();
									continue;
								}
							} else {
								cs[0] = c;
								canvas.drawText(cs, 0, 1, w + x, top, wp);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
			start = spanEnd;
		}
	}

	private void drawBackground(Drawable back, Canvas canvas, int bstart, int bend, int top, int bottom, float outx,
			float outy, float letterspacing) {
		if (bstart != bend && bend > mStart && bstart < mEnd) {
			letterspacing /= 2;
			int tStart = bstart;
			int tEnd = bend;
			if (bstart > mStart) {
				bstart -= mStart;
			} else {
				bstart = 0;
			}
			if (bend >= mEnd) {
				bend = mWordSites.size() - 1;
			} else {
				bend -= mStart;
			}
			int dStart = (int) (mWordSites.get(bstart) + outx);
			int dEnd = (int) (mWordSites.get(bend) + outx);
			if (ellipsisWidth != 0 && ellipsize != null && TruncateAt.START == truncateAt) {
				dStart += ellipsisWidth;
				dEnd += ellipsisWidth;
			}
			if (dStart == dEnd) {
				if (tStart <= mStart && tEnd >= mEnd) {
					back.setBounds(0, (int) (top + outy), canvas.getWidth(), (int) (bottom + outy));
					back.draw(canvas);
				}
			} else {
				back.setBounds((int) (dStart - letterspacing), (int) (top + outy), (int) (dEnd - letterspacing),
						(int) (bottom + outy));
				back.draw(canvas);
			}
		}
	}

	@Override
	public int setAttribute(int minStart, int maxEnd, int start, int end, int above, int below, int v, LineCompute lc) {
		this.mBelow = below;
		this.mAbove = above;
		this.mBottom = v;
		this.mStart = start;
		this.mEnd = end;
		this.minStart = minStart;
		this.maxEnd = maxEnd;
		this.offsetTop = lc.computeLineOffsetBottom(mBottom, mBelow, mAbove);
		this.offsetBottom = lc.computeLineOffsetBottom(mBottom, mBelow, mAbove);
		this.mTop = lc.computeLineTop(mBottom, mBelow, mAbove);
		return v;
	}

	@Override
	public float getHorizontalForOffset(CharSequence text, int offset, float letterSpac, boolean setSelect,
			boolean isSelecting) {
		float w;
		int mOffset = offset - mStart;
		if (isPositive()) {
			w = mWordSites.get(mOffset);
		} else {
			int maxEnd = this.maxEnd;
			int minstart = this.minStart;
			int index = maxEnd - offset;
			w = mWordSites.get(index);
			if (setSelect && text instanceof Spannable) {
				Spannable spannable = (Spannable) text;
				int start = Selection.getSelectionStart(text);
				int end = Selection.getSelectionEnd(text);
				index = maxEnd - index;
				if (index == minstart) {
					index += 1;
				} else if (index == maxEnd) {
					index = 0;
				}
				if (!isSelecting && start == end) {
					Selection.setSelection(spannable, index);
				} else {
					Selection.setSelection(spannable, start, index);
				}
			}
		}
		if (BuildConfig.DEBUG) {
			Log.e(TAG, "getHorizontalForOffset Output>" + w + "  " + mOffset + "  " + mStart + "  " + offset);
		}
		float result = w - letterSpac / 2;
		float firstSite = mWordSites.getFirst();
		if (result <= firstSite) {
			float bin = letterSpac / 2;
			if (truncateAt == TruncateAt.START && mStart == minStart) {
				firstSite += ellipsisWidth;
			}
			if (mLendingMargin - bin > 0) {
				return firstSite - letterSpac / 2;
			}
			return firstSite;
		}
		if (truncateAt == TruncateAt.START && mStart == minStart) {
			result += ellipsisWidth;
		}
		return result;
	}

	@Override
	public float getExactHorizontalForHorizontal(CharSequence text, float horiz, float letterSpac, boolean setSelect,
			boolean isSelect) {
		Spannable spanned = null;
		int selectStart = -1;
		if (text instanceof Spannable && setSelect) {
			spanned = (Spannable) text;
			selectStart = Selection.getSelectionStart(spanned);
			if (!isSelect) {
				selectStart = -1;
			}
		}
		float value = mWordSites.toFromFirst(0, true);
		Log.e(TAG, "Value>" + value + "  " + horiz);
		if (value != FloatLinked.INVALID) {
			if (value > horiz) {
				if (spanned != null) {
					int select;
					if (text.charAt(mStart) == Measurer.CHAR_NEW_LINE) {
						select = mStart + 1;
					} else {
						select = mStart;
					}
					setSelect(spanned, selectStart, select);
				}
				float marger = mLendingMargin - letterSpac / 2;
				if (marger > 0) {
					return marger;
				}
				return mWordSites.getFirst();
			}
			float prevoious = value;
			if (truncateAt == TruncateAt.START && mStart == minStart) {
				value += ellipsisWidth;
			}
			while (mWordSites.hasNext()) {
				value = mWordSites.next();
				if (truncateAt == TruncateAt.START && mStart == minStart) {
					value += ellipsisWidth;
				}
				if (value > horiz) {
					double v = horiz - prevoious;
					float spac = value - prevoious - letterSpac;
					int l = mWordSites.getCurrentIndex();
					if (l == mWordSites.size() - 1) {
						if (v > spac / 2) {
							if (spanned != null) {
								setSelect(spanned, selectStart, mStart + l);
							}

							return value - letterSpac / 2;
						}
						if (spanned != null) {
							int select;
							if (mWordSites.size() > 2 && mWordSites.toFromLast(1, true) == mWordSites.prevoious()) {
								select = mStart + l - 2;
							} else {
								select = mStart + l - 1;
							}
							setSelect(spanned, selectStart, select);
						}
						if (prevoious == mLendingMargin) {
							float marger = mLendingMargin - letterSpac / 2;
							if (marger > 0) {
								return marger;
							}
							return mWordSites.getFirst();
						}
						return prevoious - letterSpac / 2;
					} else if (v < spac / 2) {
						if (prevoious == mLendingMargin) {
							if (spanned != null) {
								int select;
								if (text.charAt(minStart) == Measurer.CHAR_NEW_LINE) {
									select = mStart + 1;
								} else if (mWordSites.getPrevoious(2) == mWordSites.getPrevoious(1)) {
									select = mStart + l - 2;
								} else {
									select = mStart + l - 1;
								}
								setSelect(spanned, selectStart, select);
							}
							float marger = mLendingMargin - letterSpac / 2;
							if (marger > 0) {
								return marger;
							}
							return mWordSites.getFirst();
						}
						if (spanned != null) {
							int select;
							if (mWordSites.getPrevoious(2) == mWordSites.getPrevoious(1)) {
								select = mStart + l - 2;
							} else {
								select = mStart + l - 1;
							}
							setSelect(spanned, selectStart, select);
						}
						return prevoious - letterSpac / 2;
					}
					if (spanned != null) {
						setSelect(spanned, selectStart, mStart + l);
					}
					return value - letterSpac / 2;
				}
				prevoious = value;
			}
			if (value == mLendingMargin) {
				if (spanned != null) {
					setSelect(spanned, selectStart, mStart + 1);
				}
				float marger = mLendingMargin - letterSpac / 2;
				if (marger > 0) {
					return marger;
				}
				return mWordSites.getFirst();
			}
			if (spanned != null) {
				setSelect(spanned, selectStart, mStart + mWordSites.size() - 1);
			}

			return value - letterSpac / 2;
		}

		return -1;
	}

	@Override
	public void gc() {
		if (mWordSites != null) {
			this.mWordSites.gc();
		}
	}

	@Override
	public float getLastLetterWidth() {
		if (mWordSites != null && mWordSites.size() > 0) {
			return mWordSites.getLast() - mWordSites.toFromLast(1, false);
		}
		return 0;
	}

	@Override
	public float getFirstLetterWidth() {
		if (mWordSites != null && mWordSites.size() > 0) {
			return mWordSites.toFromFirst(1, false) - mWordSites.getFirst();
		}
		return 0;
	}

	public int getLeadingMergin() {
		return mLendingMargin;
	}

	public int getSiteCountByHorizontal(float hori) {
		if (mWordSites == null || mWordSites.size() == 0) {
			return 0;
		}
		mWordSites.toFromFirst(0, false);
		while (mWordSites.hasNext()) {
			if (mWordSites.next() + ellipsisWidth >= hori) {
				return mWordSites.getCurrentIndex();
			}
		}
		return mWordSites.getCurrentIndex();
	}

	public int getSiteSomeCountByOffset(int index) {
		int size = mWordSites.size();
		if (index >= size) {
			return 0;
		}
		int bin = size / 2;
		int count = 0;
		if (index >= bin) {
			float next = mWordSites.toFromLast(size - index, false);
			mWordSites.next();
			while (mWordSites.hasNext()) {
				float n = mWordSites.next();
				if (n != next) {
					return count;
				}
				count++;
			}
		} else {
			float next = mWordSites.toFromFirst(index, true);
			while (mWordSites.hasNext()) {
				float n = mWordSites.next();
				if (n != next) {
					return count;
				}
				count++;
			}
		}
		return count;
	}

	@Override
	public float getSite(int index) {
		if (mWordSites == null) {
			throw new NullPointerException("sites == null!");
		}
		if (index < 0 || index >= mWordSites.size()) {
			throw new IllegalArgumentException("index == " + index + "  sites = " + mWordSites.size());
		}
		return mWordSites.get(index);
	}

	public void removeFirstSite(boolean offset) {
		if (mWordSites == null || mWordSites.size() <= 2) {
			return;
		}
		if (offset) {
			float off = getFirstLetterWidth();
			mWordSites.removeFirst();
			float next = mWordSites.toFromFirst(0, false);
			mWordSites.set(next -= off);
			mWordSites.next();
			while (mWordSites.hasNext()) {
				mWordSites.set(mWordSites.next() - off);
			}
		} else {
			mWordSites.removeFirst();
		}
	}

	public void removeLastSite(boolean offset) {
		mWordSites.removeLast();
	}
}
