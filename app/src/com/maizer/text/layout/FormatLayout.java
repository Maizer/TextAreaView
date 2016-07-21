package com.maizer.text.layout;

import java.util.Arrays;

import com.maizer.BuildConfig;
import com.maizer.text.cursor.CursorDevicer;
import com.maizer.text.factory.LineFactory;
import com.maizer.text.liner.Lineable;
import com.maizer.text.liner.Lineable.LineCompute;
import com.maizer.text.measure.LineMeasurer;
import com.maizer.text.measure.Measurer;
import com.maizer.text.util.ArrayGc;
import com.maizer.text.util.ObjectLinked;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.widget.Toast;

/**
 * 格式化布局,对给予的Text进行排版
 * 
 * @node 线程不安全
 * @author Maizer/麦泽
 */
public class FormatLayout extends TextAreaLayout {

	private static final String TAG = FormatLayout.class.getSimpleName();

	private static final int WAIT_TIME = 2000;// wait 2 second
	
	private PointSyncTask mSyncTask;
	private PointArray mPoints;

	private TextAreaPaint mTempPaint;
	private Measurer mMeasurer;

	private ObjectLinked<Lineable> mLinears;

	public FormatLayout(LayoutAttrubute attrubute, CharSequence text, TextAreaPaint wp, boolean startSyncTask) {
		super(attrubute, wp, text);
		mTempPaint = new TextAreaPaint();
		mLinears = new ObjectLinked<Lineable>();
		mPoints = new PointArray(attrubute.initLineArraySize, this);
		mMeasurer = attrubute.build();
		if (startSyncTask) {
			mSyncTask = new PointSyncTask(WAIT_TIME, attrubute.build());
		}
		initgenerate();
	}

	private void initgenerate() {
		stopSyncTask();
		CharSequence text = getText();
		if (text == null || text.length() <= 0) {
			checkLimitLines();
			return;
		}
		int length = getTextMaxEnd(text);
		if (isSingleLine()) {
			generateHorizontalWithLimit(text, 0, length, 0);
		} else {
			generateVerticalWithLimit(text, 0, length, 0, 0, true, true);
		}
		startSyncTask();
	}

	protected void generateHorizontalWithLimit(CharSequence source, int bufStart, int bufEnd, int v) {
		requestSingLineMeasure(0, getMaxEms(), null);
	}

	protected final Measurer getMeasurer() {
		return mMeasurer;
	}

	protected final ObjectLinked<Lineable> getLineArray() {
		return mLinears;
	}

	protected int generateVerticalWithLimit(CharSequence source, int bufStart, int bufEnd, int v, int offsetBase,
			boolean checkHeight, boolean checkMaxLines) {
		if (mPoints.isLimit()) {
			return 0;
		}
		TextAreaPaint paint = getPaint();
		int maxH = getMaxHeight();
		int length = getTextMaxEnd(source);
		int countH = 0;
		int offsetH = v;
		boolean noLast = length != source.length();
		if (mLinears.size() > 0) {
			offsetH -= offsetBase;
		}
		Lineable lastLine;
		checkMaxLines = checkMaxLines ? getMaxLinesLimitHeight() > 0 ? true : false : false;
		while ((!checkHeight || (checkHeight && offsetH < maxH)) && bufStart < bufEnd) {
			lastLine = mMeasurer.measureAfterLine(source, bufStart, length, v, mLinears.getRecycle(), paint, 0);
			countH += lastLine.getHeight();
			mLinears.addLast(lastLine);
			mPoints.insertLine(lastLine);
			if (mPoints.isLimit()) {
				checkEllipsis(lastLine, source, v, paint, bufStart, length);
				break;
			} else if (noLast && lastLine.getMaxEnd() == length) {
				checkEllipsis(lastLine, source, v, paint, bufStart, length);
			}
			bufStart = lastLine.getMaxEnd();
			v = lastLine.getBottom();
			offsetH = v - offsetBase;
			if (checkMaxLines) {
				if (mLinears.size() >= getMaxLinesLimitHeight()) {
					break;
				}
			}
		}
		if (checkMaxLines || isWrapHeight()) {
			checkLimitLines();
		}
		checkFirstPageMaxDisplay();
		return countH;
	}

	private void checkEllipsis(Lineable lastLine, CharSequence source, int v, TextAreaPaint paint, int start, int end) {
		String ellipsis = getEllipsis();
		if (getTruncateAt() != null && lastLine.getEnd() < source.length() && ellipsis != null) {
			float ellipsizeWidth = mMeasurer.measureReplaceChar(source, lastLine.getEnd(), source.length(), ellipsis,
					paint);
			mMeasurer.measureAfterLine(source, start, end, v, lastLine, paint, ellipsizeWidth);
		}
	}

	private void checkFirstPageMaxDisplay() {
		if (mLinears.size() > 0 && mLinears.getFirst().getMinStart() <= 0) {
			mPoints.setIncreaseCapacity(mLinears.size());
		}
	}

	public int getLineCount() {
		return mPoints.length();
	}

	public int getTopPadding() {
		return mMeasurer.getToppadding();
	}

	public int getBottomPadding() {
		return mMeasurer.getBottomPadding();
	}

	@Override
	public float scrollByHorizontal(float scrollX) {
		if (!isSingleLine()) {
			return -1;
		}
		if (getLineRecyleCount() <= 0) {
			return 0;
		}
		CharSequence text = getText();
		TextAreaPaint wp = getPaint();
		return getFirstLine().moveHorizontal(scrollX, text, wp, mMeasurer);
	}

	@Override
	public float scrollByOffset(int offset, float scrollOffset) {
		if (inLayout()) {
			checkLimitLines();
		}
		scrollOffset += getHeight();
		int index = getLineForOffset(offset);
		if (index == -1) {
			return -1;
		}
		int offsetBottom = getTextLiner(index).getBottom();
		float v = scrollOffset - offsetBottom;
		if (v < 0) {
			scrollByVertical(scrollOffset - getHeight() - v);
		}
		return v;
	}

	@Override
	public float scrollByVertical(float scrollY) {
		if (isSingleLine()) {
			checkLimitLines();
			return 0;
		}
		if (getLineRecyleCount() <= 0) {
			return 0;
		}
		CharSequence text = getText();
		if (text == null) {
			return 0;
		}
		int start = getTextMinStart(text);
		int length = getTextMaxEnd(text);
		int maxH = getHeight();
		Lineable lastLine = mLinears.getLast();
		Lineable firstLine = mLinears.getFirst();
		if (!showBottom(lastLine, text, scrollY, length, -1)) {
			lastLine = mLinears.getLast();
			int offsetBottom = lastLine.getBottom() - maxH;
			if ((mPoints.isLimit() || lastLine.getMaxEnd() >= length) && scrollY >= offsetBottom) {
				showTop(firstLine, text, start, offsetBottom, false);
				offsetBottom = mLinears.getLast().getBottom() - maxH;
				int offsetTop = mLinears.getFirst().getTop();
				if (offsetTop > offsetBottom) {
					return offsetTop;
				}
				return offsetBottom;
			}
			showTop(firstLine, text, start, scrollY, false);
			firstLine = mLinears.getFirst();
			int offsetTop = firstLine.getTop();
			if (firstLine.getMinStart() <= start && scrollY <= offsetTop) {
				return offsetTop;
			}
		} else {
			lastLine = mLinears.getLast();
			int offsetBottom = lastLine.getBottom() - maxH;
			if ((mPoints.isLimit() || lastLine.getMaxEnd() >= length) && scrollY >= offsetBottom) {
				return offsetBottom;
			}
		}
		return -1;
	}

	public FontMetricsInt getDefaultFontMetricsInt() {
		FontMetricsInt fm = getPaint().getFontMetricsInt();
		mMeasurer.getDefaultMeasure(fm);
		return fm;
	}

	public int getDefaultCursorHeight() {
		LineFactory mFactory = getLineFactory();
		FontMetricsInt fm = getDefaultFontMetricsInt();
		int below = fm.descent;
		int above = fm.ascent;
		int bottom = mFactory.computeLineOffsetBottom(0, below, above);
		int top = mFactory.computeLineTop(0, below, above);
		return bottom - top;
	}

	private static final int EXACT = 10000000;

	public boolean amendTopVerticalLevelToZoer(TextAreaScroller scroller) {
		if (getLineRecyleCount() <= 0) {
			return false;
		}
		Lineable firstLine = mLinears.getFirst();
		Lineable lastLine = mLinears.getLast();
		int scrollY = scroller.getTextScrollY();
		int lastBottom = lastLine.getBottom();
		int firstTop = firstLine.getTop();
		if (lastBottom > EXACT) {
			mLinears.getFromFirst(0, false);
			int topLevel = firstTop - scrollY;
			while (mLinears.hasNext()) {
				Lineable liner = mLinears.next();
				mMeasurer.moveAfterLineForTopBase(liner, topLevel);
				topLevel = liner.getBottom();
			}
			return true;
		} else if (firstTop < -EXACT) {
			mLinears.getFromFirst(0, false);
			int topLevel = firstTop - scrollY;
			while (mLinears.hasNext()) {
				Lineable liner = mLinears.next();
				mMeasurer.moveAfterLineForTopBase(liner, topLevel);
				topLevel = liner.getBottom();
			}
			return true;
		}
		return false;
	}

	protected boolean requestSingLineMeasure(int index, int ems, TextAreaScroller aide) {

		if (getWidth() == 0 || getHeight() == 0) {
			return false;
		}

		CharSequence text = getText();
		TextAreaPaint paint = getPaint();
		Lineable mLine;
		if (mLinears.size() <= 0) {
			mLine = mLinears.getRecycle();
			if (mLine == null) {
				mLine = mMeasurer.installNewLineable();
			}
			mLinears.add(mLine);
		} else {
			mLine = getFirstLine();
		}
		int minStart = getTextMinStart(text);
		int maxEnd = getTextMaxEnd(text);
		int scrollX = aide == null ? 0 : aide.getTextScrollX();
		if (getMaxEms() == -1) {
			index = minStart;
			if (ems != -1) {
				TruncateAt truncateAt = getTruncateAt();
				if (truncateAt == TruncateAt.END || truncateAt == null) {
					maxEnd = Measurer.codePointCount(text, 0, text.length(), ems, true);
				} else if (truncateAt == TruncateAt.START) {
					minStart = Measurer.codePointCount(text, 0, text.length(), ems, false);
				}
			}
		}

		if (index <= minStart) {
			mMeasurer.measureSingleLine(text, minStart, minStart, maxEnd, -1, 0,
					mLine.getMinSite() - mLine.getLeadingMergin(), 0, ems, mLine, true, paint);
			if (getMaxEms() == -1 && mLine.getEnd() > 2
					&& mLine.getMaxSite() + mLine.getEllipsisWidth() >= getWidth()) {
				if (ems != -1) {
					ems--;
				} else {
					ems = mLine.getEnd() - 1;
				}
				requestSingLineMeasure(0, ems, aide);
			}
			checkLimitLines();
			if (scrollX != 0) {
				aide.requestTextScrollBy((int) (mLine.getMinSite() - scrollX - mLine.getLeadingMergin()), 0);
				return true;
			}
			return false;
		}

		int start = mLine.getStart();
		int maxScrollX = scrollX + getWidth();
		int offsetStart = mLine.getSiteCountByHorizontal(maxScrollX);
		int someCount = mLine.getSiteSomeCountByOffset(index - start + 1);
		if (mLine.getMaxSite() >= maxScrollX) {
			offsetStart--;
		}
		if (index >= offsetStart + start - someCount) {
			int end = mLine.getEnd();
			if (mLine.getMaxSite() >= maxScrollX) {
				if (index == end) {
					index++;
				} else if (index < end) {
					index = end;
				}
				if (mLine.getMaxSite() == maxScrollX) {
					if (someCount != 0) {
						if (index == end) {
							index++;
						} else if ((index += someCount) == end) {
							index++;
						}
					}
				}
			}
			mMeasurer.measureSingleLine(text, index > maxEnd ? maxEnd : index, minStart, maxEnd, -1, 0, 0, 0, ems,
					mLine, false, paint);
			checkLimitLines();
			if (aide != null) {
				int offset = (int) (mLine.getMaxSite() - getWidth());
				if (offset > 0) {
					if (offset - scrollX != 0) {
						aide.requestTextScrollBy(offset - scrollX, 0);
						return true;
					}
				} else if (scrollX != 0) {
					aide.requestTextScrollBy(-scrollX, 0);
					return true;
				}
			}
			return false;
		}
		float site = scrollX - mLine.getMinSite();
		if (site < 0) {
			site = 0;
		}
		offsetStart = mLine.getSiteCountByHorizontal(scrollX);

		if (index <= offsetStart + start) {
			index++;
			mMeasurer.measureSingleLine(text, index > maxEnd ? maxEnd : index, minStart, maxEnd, -1, 0, 0, 0, ems,
					mLine, false, paint);
			checkLimitLines();
			if (aide != null) {
				if (mLine.getStart() == minStart
						&& index < minStart + mLine.getSiteCountByHorizontal(scrollX + getWidth() - site) - 1) {
					if (scrollX != 0) {
						aide.requestTextScrollBy(-scrollX, 0);
						return true;
					}
				}
				int offset = (int) (mLine.getMaxSite() - getWidth());
				if (offset > 0) {
					if (offset - scrollX != 0) {
						aide.requestTextScrollBy(offset - scrollX, 0);
						return true;
					}
				} else if (scrollX != minStart) {
					aide.requestTextScrollBy((int) mLine.getMinSite(), 0);
					return true;
				}
			}
			return false;
		}
		if (start == minStart && index < offsetStart + minStart) {
			mMeasurer.measureSingleLine(text, minStart, minStart, maxEnd, -1, 0, 0, 0, ems, mLine, true, paint);
			checkLimitLines();
			if (aide != null) {
				if (scrollX != 0) {
					aide.requestTextScrollBy(-scrollX, 0);
					return true;
				}
			}
			return false;
		}
		float minSite = mLine.getMinSite();
		if (start == mLine.getMinStart()) {
			if (start != minStart) {
				start = minStart;
			}
			minSite -= mLine.getLeadingMergin();
		}
		mMeasurer.measureSingleLine(text, start, minStart, maxEnd, -1, site, minSite, 0, ems, mLine, true, paint);
		checkLimitLines();
		return false;
	}

	protected int searchExistPoint(int offsetPoint) {
		return mPoints.search(offsetPoint);
	}

	protected int getExistPoint(int location) {
		return mPoints.get(location);
	}

	/**
	 * 如果需要可在此使用Animation 或者post迭代,也可在此处优化上引速度,使用{@link PointArray#search(int)}
	 * 方法
	 * 
	 * <pre>
	 * if (index != -1) {
	 * 	{@link PointArray#search(int)}
	 * }
	 * </pre>
	 * 
	 * @param index
	 * @param aide
	 *            {@link TextAreaScroller}
	 */
	protected boolean topToIndex(TextAreaScroller aide, int index, boolean clearPoint) {
		CharSequence text = getText();
		TextAreaPaint tap = getPaint();
		int length = getTextMaxEnd(text);
		int result;
		if (index == -1) {
			result = mPoints.search(mLinears.getFirst().getMinStart() - 1);
		} else {
			result = mPoints.search(index);
			if (result > 0) {
				result--;
			}
		}
		if (result >= 0) {
			int scrollY = aide.getTextScrollY();
			int value = mPoints.get(result);
			if (clearPoint) {
				mPoints.removeLast(result);
			}
			int top = mLinears.getFromFirst(0, false).getTop();
			while (mLinears.hasNext()) {
				Lineable line = mLinears.next();
				mMeasurer.measureAfterLine(text, value, length, top, line, tap, 0);
				if (clearPoint) {
					mPoints.insertLine(line);
				}
				top = line.getBottom();
				value = line.getMaxEnd();
			}
			Lineable firstLine = mLinears.getFirst();
			Lineable lastLine = mLinears.getLast();
			generateVerticalWithLimit(text, lastLine.getMaxEnd(), getTextMaxEnd(text), lastLine.getBottom(), scrollY,
					true, true);
			// generateVerticalWithLimit(text, lastLine.getMaxEnd(),
			// getTextMaxEnd(text), lastLine.getBottom(), 0, true,
			// true);
			int firstTop = firstLine.getTop();
			removeLast(getHeight() + scrollY, clearPoint);
			// removeLast(getHeight(), clearPoint);
			if (scrollY > firstTop) {
				aide.requestTextScrollBy(0, firstTop - scrollY);
				return true;
			}
			return false;
		}
		int h = 0;
		int scrollY = aide.getTextScrollY();
		Lineable firstLine = mLinears.getFirst();
		do {
			h += firstLine.getHeight();
			scrollByVertical(scrollY - h);
			firstLine = mLinears.getFirst();
		} while (index < firstLine.getMinStart() && index != -1);

		if (h > 0) {
			int offsetTop = mLinears.getFirst().getOffsetTop();
			if (offsetTop < scrollY - h) {
				h += (scrollY - h - offsetTop);
			}
			if (scrollY != 0) {
				if (h > scrollY) {
					aide.requestTextScrollBy(0, -scrollY);
				} else {
					aide.requestTextScrollBy(0, -h);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 如果需要可在此使用Animation或者post迭代
	 * 
	 * @param index
	 *            >lastLine.getMaxEnd
	 */
	protected boolean bottomToIndex(TextAreaScroller aide, int index, boolean removeP) {
		int result = mPoints.search(index);
		int count = mLinears.size();
		if (!removeP) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "bottomToIndex() Output>" + result + "   " + getLastLine().getMaxEnd() + "   "
						+ mPoints.get(result) + "   " + getText().length() + "  " + count);
			}

			if (result - count >= 0 && getLastLine().getMaxEnd() < mPoints.get(result)) {
				CharSequence text = getText();
				TextAreaPaint tap = getPaint();
				int length = getTextMaxEnd(text);
				int value = mPoints.get(result - count);
				int top = mLinears.getFromFirst(0, false).getTop();
				while (mLinears.hasNext()) {
					Lineable line = mLinears.next();
					mMeasurer.measureAfterLine(text, value, length, top, line, tap, 0);
					top = line.getBottom();
					value = line.getMaxEnd();
				}
			}
		} else if (result > 0) {
			mPoints.removeLast(result);
		}
		Lineable lastLine = mLinears.getLast();
		int maxEnd = lastLine.getMaxEnd();
		int scrollY = aide.getTextScrollY();
		int length = getTextMaxEnd(getText());
		int height = getHeight();
		int h = 0;
		if (index > maxEnd && maxEnd < length && !mPoints.isLimit()) {
			do {
				if (lastLine.getBottom() - scrollY - height > 0 || maxEnd < length) {
					h += lastLine.getHeight();
				}
				scrollByVertical(scrollY + h);
				lastLine = mLinears.getLast();
				maxEnd = lastLine.getMaxEnd();
				height = checkLimitLines();
			} while (index > maxEnd && maxEnd < length && !mPoints.isLimit());
		}
		int lastBottom = mLinears.getLast().getBottom();
		int lastOffsetBottom = lastBottom - scrollY;
		if (lastOffsetBottom >= height) {
			if ((lastOffsetBottom -= height) > 0) {
				scrollByVertical(lastBottom - height);
				aide.requestTextScrollBy(0, lastOffsetBottom);
				return true;
			}
		} else if (scrollY != (lastOffsetBottom -= height)) {
			scrollByVertical(lastBottom - height);
			aide.requestTextScrollBy(0, lastOffsetBottom);
			return true;
		}
		return false;

	}

	/**
	 * 停止后台测量Text
	 */
	protected final void stopSyncTask() {
		if (mSyncTask != null) {
			mSyncTask.cancel();
		}
	}

	/**
	 * 开始后台等待测量Text
	 */
	protected final void startSyncTask() {
		if (mSyncTask != null) {
			mSyncTask.startWait();
		}
	}

	/**
	 * @param index
	 *            in single line mode, if text changed,must set after point
	 *            ,else set before point
	 * 
	 */
	@Override
	public boolean restartMeasure(int index, TextAreaScroller aide) {
		stopSyncTask();
		CharSequence source = getText();
		if (source == null) {
			if (mLinears.size() > 0) {
				mLinears.clear();
				mPoints.clear();
				gc();
			}
			checkLimitLines();
			return false;
		}
		int length = getTextMaxEnd(source);
		if (length <= 0) {
			if (mLinears.size() > 0) {
				mLinears.clear();
				mPoints.clear();
				gc();
			}
			checkLimitLines();
			int scrollY = aide.getTextScrollY();
			int scrollX = aide.getTextScrollX();
			if (scrollY != 0 || scrollX != 0) {
				aide.requestTextScrollBy(-scrollX, -scrollY);
				return true;
			}
			return false;
		}
		if (mLinears.size() > 0) {
			if (isSingleLine()) {
				return requestSingLineMeasure(index, getMaxEms(), aide);
			}
			boolean handerUpdata = false;
			boolean noLast = source.length() != length;
			Lineable lastLine = mLinears.getFromLast(0, false);
			Lineable firstLine = mLinears.getFromFirst(0, false);
			if (mPoints.isLimit() && index > lastLine.getMaxEnd()) {
				index = lastLine.getMaxEnd();
			} else if (index > 0 && index == firstLine.getMinStart()) {
				// 为了防止排行错误
				index--;
			}
			if (index < firstLine.getMinStart()) {
				handerUpdata = topToIndex(aide, index, true);
			} else if (index <= lastLine.getMaxEnd()) {
				lastLine = null;
				TextAreaPaint paint = getPaint();
				int tStart = getTextMinStart(source);
				int p = -1, minStart = -1, bottom = -1;
				while (mLinears.hasNext()) {
					firstLine = mLinears.next();
					Lineable next = mLinears.getNext();
					if (next == null || next.getMaxEnd() >= index) {
						if (lastLine == null) {
							minStart = firstLine.getMinStart();
							bottom = firstLine.getTop();
						} else {
							int end = lastLine.getMaxEnd();
							if (end > minStart) {
								minStart = lastLine.getMaxEnd();
								bottom = lastLine.getBottom();
							}
						}
						if (minStart < length) {
							int removeStart = firstLine.getMinStart();
							if (p < 0 && (p = mPoints.indexSync(removeStart)) >= 0) {
								mPoints.removeLast(p);
							}
							mMeasurer.measureAfterLine(source, minStart, length, bottom, firstLine, paint, 0);
							mPoints.insertLine(firstLine);
							if (mPoints.isLimit()) {
								checkEllipsis(firstLine, source, bottom, paint, minStart, length);
							} else if (firstLine.getEnd() == length && noLast) {
								checkEllipsis(firstLine, source, bottom, paint, minStart, length);
							}
						} else {
							if (p < 0 && (p = mPoints.indexSync(minStart)) >= 0) {
								mPoints.removeLast(p);
							}
							Lineable removeLine = mLinears.remove();
							int scrollY = aide.getTextScrollY();
							if (mLinears.size() > 0) {
								Lineable mLine = mLinears.getFirst();
								int top = mLine.getTop();
								if (mLinears.getFirst().getMinStart() > tStart) {
									int h = removeLine.getHeight();
									showTop(mLine, source, tStart, scrollY - h, false);
									aide.requestTextScrollBy(0, -h);
								} else {
									showTop(mLine, source, tStart, top, false);
									aide.requestTextScrollBy(0, top - scrollY);
								}
							}
						}
					}
					lastLine = firstLine;
				}
				generateVerticalWithLimit(source, lastLine.getMaxEnd(), length, lastLine.getBottom(),
						aide.getTextScrollY(), true, true);
			} else {
				handerUpdata = bottomToIndex(aide, index, true);
			}
			checkFirstPageMaxDisplay();
			startSyncTask();
			// +++1 122 470202 471222
			Log.e(TAG, "+++" + mLinears.size() + "  " + getHeight() + "  " + aide.getTextScrollY() + "   "
					+ mLinears.getFirst().getTop());
			return handerUpdata;
		}
		if (isSingleLine()) {
			generateHorizontalWithLimit(source, 0, length, 0);
			return false;
		}
		generateVerticalWithLimit(source, 0, length, 0, 0, true, true);
		startSyncTask();
		return false;
	}

	@Override
	public boolean restartLayoutSize(int offsetPosition, int w, int h, TextAreaScroller vh) {
		boolean result = super.restartLayoutSize(offsetPosition, w, h, vh);
		removeLast(vh.getTextScrollY() + getHeight(), false);
		return result;
	}

	protected void removeFirst(float scrollY) {
		if (mLinears.size() > 0 && scrollY > mLinears.getFirst().getBottom()) {
			do {
				mLinears.removeFirst();
			} while (mLinears.size() > 0 && scrollY > mLinears.getFirst().getBottom());
		}
	}

	protected void removeLast(float scrollY, boolean removeP) {
		if (mLinears.size() > 0 && scrollY < mLinears.getLast().getTop()) {
			do {
				mLinears.removeLast();
				if (removeP) {
					mPoints.removeLast(-1);
				}
			} while (mLinears.size() > 0 && scrollY < mLinears.getLast().getTop());
		}
	}

	protected boolean showBottom(Lineable lastLine, CharSequence text, float scrollY, int length, int index) {
		if (lastLine != null) {
			if (lastLine.getMaxEnd() < length && !mPoints.isLimit()) {
				int maxH = getHeight();
				if (scrollY + maxH > lastLine.getBottom()) {
					TextAreaPaint paint = getPaint();
					int maxEnd = lastLine.getMaxEnd();
					boolean noLast = length != text.length();
					do {
						if (scrollY > mLinears.getFirst().getBottom()) {
							mLinears.removeFirst();
						}
						if (maxEnd == -1) {
							break;
						}
						Lineable line = mMeasurer.measureAfterLine(text, maxEnd, length, lastLine.getBottom(),
								mLinears.getRecycle(), paint, 0);
						mLinears.addLast(line);
						mPoints.insertLine(line);
						if (mPoints.isLimit()) {
							checkEllipsis(line, text, lastLine.getBottom(), paint, maxEnd, length);
							break;
						} else if (line.getMaxEnd() == length && noLast) {
							checkEllipsis(line, text, lastLine.getBottom(), paint, maxEnd, length);
						}
						lastLine = line;
						maxEnd = lastLine.getMaxEnd();
					} while (scrollY + maxH > lastLine.getBottom() && lastLine.getMaxEnd() < length);
					return true;
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * 检查Truncate,用于精准选择位置
	 * 
	 * @param mEditor
	 * @param selectEnd
	 * @param isLeft
	 * @return
	 */
	public int checkTruncateAt(CharSequence mEditor, int selectEnd) {
		if (getMaxEms() == -1 && getLineRecyleCount() == 1) {
			Lineable firstLine = getFirstLine();
			if (selectEnd < firstLine.getStart()) {
				selectEnd = firstLine.getStart();
			} else if (selectEnd > firstLine.getEnd()) {
				selectEnd = firstLine.getEnd();
			}
		} else {
			// int maxLines = getMaxLines();
			// if (maxLines > 1 && getLineCount() >= maxLines) {
			// int end = getLastLine().getEnd();
			// if (selectEnd > end) {
			// selectEnd = end;
			// }
			// }
		}
		return selectEnd;
	}

	/**
	 * layout 改变,快速索引到指定位置
	 * 
	 * @param text
	 * @param length
	 * @param index
	 * @param scroller
	 * @return
	 */
	protected boolean quickToIndex(CharSequence text, int length, int index, TextAreaScroller scroller) {
		float scrollY = scroller.getTextScrollY();
		int maxLines = getMaxLinesLimitHeight();
		int maxH = getMaxHeight();
		int minStart = getTextMinStart(text);
		int maxEnd = 0;
		int v = 0;
		boolean noLast = length != text.length();

		TextAreaPaint paint = getPaint();
		Lineable lastLine;
		do {
			Lineable line = mMeasurer.measureAfterLine(text, maxEnd, length, v, mLinears.getRecycle(), paint, 0);
			if (line.getMaxEnd() >= index) {
				mLinears.addLast(line);
			}
			mPoints.insertLine(line);
			if (mPoints.isLimit()) {
				/**
				 * 如果达到了最大行,我们需要确定其存在一行
				 */
				if (mLinears.size() <= 0) {
					mLinears.addLast(line);
				}
				checkEllipsis(line, text, v, paint, maxEnd, length);
				break;
			} else if (line.getMaxEnd() == length && noLast) {
				checkEllipsis(line, text, v, paint, maxEnd, length);
			}
			maxEnd = line.getMaxEnd();
			v = line.getBottom();
			lastLine = line;
			if (maxLines > 0) {
				if (mLinears.size() >= maxLines) {
					break;
				}
			}
		} while ((mLinears.size() <= 0 || mLinears.getFirst().getTop() + maxH > mLinears.getLast().getOffsetBottom())
				&& lastLine.getMaxEnd() < length);
		lastLine = getLastLine();
		Lineable firstLine = getFirstLine();

		int scroll = lastLine.getOffsetBottom() - maxH;
		if (firstLine.getMinStart() > minStart && lastLine.getOffsetBottom() - firstLine.getTop() < maxH) {
			scroll -= maxH;
		}
		showTop(firstLine, text, minStart, scroll, false);
		scroll = getFirstLine().getTop();
		if (scrollY != scroll) {
			scroller.requestTextScrollBy(0, (int) (scroll - scrollY));
		}
		checkLimitLines();
		checkFirstPageMaxDisplay();
		return true;
	}

	protected boolean showTop(Lineable firstLine, CharSequence text, int start, float scrollY, boolean removeP) {
		int height = getHeight();

		if (scrollY < firstLine.getTop() && firstLine.getMinStart() > start) {
			int index, minStart;
			TextAreaPaint paint = getPaint();
			do {
				if (firstLine != null) {
					if (scrollY + height < mLinears.getLast().getTop()) {
						mLinears.removeLast();
						if (removeP) {
							mPoints.removeLast(-1);
						}
					}
					minStart = firstLine.getMinStart();
					index = mPoints.indexSync(minStart);
					if (index < 0) {
						if (minStart <= 1) {
							index = 0;
						} else {
							index = TextUtils.lastIndexOf(text, Measurer.CHAR_NEW_LINE, minStart - 1);
						}
					} else if (index > 0) {
						index = mPoints.get(index - 1);
					} else {
						index = mPoints.get(0);
					}
				} else {
					index = mPoints.get(mPoints.length() - 1);
					minStart = 0;
				}
				Lineable line = mMeasurer.measureBeforeLine(text, index == -1 ? start : index, minStart,
						firstLine.getTop(), mLinears.getRecycle(), paint, 0);
				mLinears.addFirst(line);
				if (firstLine.getMinStart() == line.getMinStart()) {
					break;
				}
				firstLine = line;
			} while (scrollY < firstLine.getTop() && firstLine.getMinStart() > start);
			return true;
		}
		return false;
	}

	public boolean restartLayout(TextAreaScroller va) {

		if (super.restartLayout(va)) {
			stopSyncTask();
			CharSequence text = getText();
			int scrollY = va == null ? 0 : va.getTextScrollY();
			int scrollX = va == null ? 0 : va.getTextScrollX();
			if (getLineRecyleCount() <= 0) {
				if (text == null) {
					return false;
				}
				if (getTextMaxEnd(text) <= 0) {
					return false;
				}
				va.requestTextScrollBy(-scrollX, -scrollY);
				initgenerate();
				return true;
			}
			boolean handle = false;
			int length = getTextMaxEnd(text);
			int start = getFirstLine().getStart();
			mLinears.clear();
			mPoints.clear();
			if (length > 0) {
				if (isSingleLine()) {
					requestSingLineMeasure(start, getMaxEms(), va);
					if (scrollY != 0) {
						va.requestTextScrollBy(0, -scrollY);
						handle = true;
					}
				} else {
					if (start > length) {
						start = length;
					}
					handle = quickToIndex(text, length, start, va);
					if (scrollX != 0) {
						va.requestTextScrollBy(-scrollX, 0);
						handle = true;
					}
					startSyncTask();
				}
			}
			gc();
			return handle;
		}
		return false;
	}

	@Override
	protected Lineable getTextLiner(int raw) {
		if (raw < 0 || raw > mLinears.size()) {
			return null;
		} else if (raw == 0) {
			return mLinears.getFirst();
		} else if (raw == mLinears.size()) {
			return mLinears.getLast();
		}
		return mLinears.get(raw);
	}

	protected Lineable getFirstLine() {
		// 性能不作处理
		// if (mLinears == null||mLinears.size() <= 0) {
		// return null;
		// }
		return mLinears.getFirst();
	}

	protected Lineable getLastLine() {
		// 性能不作检测处理
		// if (mLinears == null||mLinears.size() <= 0) {
		// return null;
		// }
		return mLinears.getLast();
	}

	@Override
	public void draw(Canvas c) {
		CharSequence text = getText();
		if (text == null || text.length() <= 0 || mLinears.size() <= 0) {
			return;
		}
		TextAreaPaint wp = getPaint();
		if (text instanceof Spannable) {
			boolean isSingleLine = isSingleLine();
			Spannable spanned = (Spannable) text;
			Lineable line = mLinears.getFromFirst(0, false);
			float letterSpacing = wp.getLetterSpacing();
			int selectStart = Selection.getSelectionStart(spanned);
			int selectEnd = Selection.getSelectionEnd(spanned);
			Drawable background;
			if (selectEnd == selectStart) {
				background = null;
			} else {
				if (selectStart > selectEnd) {
					int end = selectEnd;
					selectEnd = selectStart;
					selectStart = end;
				}
				background = getTextHighlightDrawable();
			}
			int btop;
			int bbottom;
			Lineable lastLine = line;
			Lineable nextLine = line;
			do {
				nextLine = mLinears.next();
				int index = mLinears.getNextIndex();
				btop = getCursorTop(lastLine, nextLine, index);
				bbottom = getCursorBottom(nextLine, index);
				if (!isSingleLine) {
					btop += mTempPaint.getUnderlineThickness();
				}
				nextLine.drawSpanned(spanned, c, wp, mTempPaint, background, selectStart, selectEnd, btop, bbottom,
						getLeadingMarger(nextLine), 0, letterSpacing);
				lastLine = nextLine;
			} while (mLinears.hasNext());
		} else {
			Lineable line = mLinears.getFromFirst(0, false);
			do {
				line = mLinears.next();
				line.drawText(text, c, wp, getLeadingMarger(line), 0);
			} while (mLinears.hasNext());
		}
	}

	/**
	 * 处理Alignment
	 * 
	 * @param mLine
	 * @return
	 */
	protected float getLeadingMarger(Lineable mLine) {
		Alignment align = getAlignment();
		if (align != null) {
			if (mLine.getEnd() < mLine.getMaxEnd() || mLine.getStart() > mLine.getMinStart()) {
				return 0;
			}
			float ellipsisW = 0;
			if (getTruncateAt() == TruncateAt.START) {
				ellipsisW = mLine.getEllipsisWidth();
			}
			switch (align) {
			case ALIGN_CENTER:
				float w = (getWidth() - mLine.getMaxSite() - ellipsisW) / 2;
				return w < 0 ? 0 : w;
			case ALIGN_RIGHT:
				w = getWidth() - mLine.getMaxSite() - ellipsisW;
				return w < 0 ? 0 : w;
			}
		}
		return 0;
	}

	@Override
	public int getLineRecyleCount() {
		return mLinears.size();
	}

	public int getLineForOffset(int offset) {
		mLinears.getFromFirst(0, false);
		while (mLinears.hasNext()) {
			Lineable mLine = mLinears.next();
			int end = mLine.getMaxEnd();
			if (offset <= end) {
				if (mLine.getMinStart() > offset) {
					return -1;
				}
				return mLinears.getNextIndex();
			}
		}
		return -1;
	}

	public Lineable getLineableForOffset(int offset) {
		mLinears.getFromFirst(0, false);
		while (mLinears.hasNext()) {
			Lineable mLine = mLinears.next();
			int end = mLine.getMaxEnd();
			if (offset <= end) {
				if (mLine.getMinStart() > offset) {
					return null;
				}
				return mLine;
			}
		}
		return null;
	}

	public int getLineForHorizontal(double horizontal) {
		if (getLineRecyleCount() <= 0) {
			return -1;
		}
		Lineable linear = mLinears.getFromFirst(0, false);
		do {
			linear = mLinears.next();
			if (linear.getMaxSite() > horizontal - getLeadingMarger(linear) - getPaddingLeft()) {
				return mLinears.getNextIndex();
			}
		} while (mLinears.hasNext());
		return getLineRecyleCount() - 1;
	}

	public Lineable getLineableForHorizontal(float horizontal) {
		if (getLineRecyleCount() <= 0) {
			return null;
		}
		Lineable linear = mLinears.getFromFirst(0, false);
		do {
			linear = mLinears.next();
			if (linear.getMaxSite() > horizontal - getLeadingMarger(linear) - getPaddingLeft()) {
				return linear;
			}
		} while (mLinears.hasNext());
		return mLinears.getLast();
	}

	public int getLineForVertical(int vertical) {
		if (getLineRecyleCount() <= 0) {
			return -1;
		}
		vertical -= getPaddingTop();
		Lineable linear = mLinears.getFromFirst(0, false);
		if (vertical < getCursorTop(linear, linear, mLinears.getNextIndex())) {
			return -1;
		}
		Lineable lastLine = linear;
		Lineable nextLine;
		do {
			nextLine = mLinears.next();
			int index = mLinears.getNextIndex();
			int top = getCursorTop(lastLine, nextLine, index);
			if (top > vertical) {
				if (index == 0) {
					return 0;
				}
				return index - 1;
			}
			lastLine = nextLine;
		} while (mLinears.hasNext());
		if (vertical >= linear.getBottom()) {
			return mLinears.size();
		}
		return mLinears.size() - 1;
	}

	public Lineable getLineableForVertical(float vertical) {
		if (getLineRecyleCount() <= 0) {
			return null;
		}
		vertical -= getPaddingTop();
		Lineable linear = mLinears.getFromFirst(0, false);
		if (vertical < getCursorTop(linear, linear, mLinears.getNextIndex())) {
			return null;
		}
		Lineable lastLine = linear;
		Lineable nextLine;
		do {
			nextLine = mLinears.next();
			int index = mLinears.getNextIndex();
			if (getCursorTop(lastLine, nextLine, index) > vertical) {
				if (index == 0) {
					return linear;
				}
				mLinears.prevoious();
				return mLinears.prevoious();
			}
			lastLine = nextLine;
		} while (mLinears.hasNext());
		if (vertical >= linear.getTop() + linear.getHeight()) {
			return mLinears.getLast();
		}
		return mLinears.getFromLast(1, false);
	}

	protected int getCursorTop(Lineable lastLine, Lineable line, int index) {
		if (isSingleLine()) {
			return line.getTop();
		}
		if (index <= 0) {
			return line.getTop();
		}
		if (lastLine != null) {
			return lastLine.getOffsetBottom();
		}
		return mLinears.get(index - 1).getOffsetBottom();
	}

	// int top = line.getTop() + getPaddingTop();
	// int bottom = line.getOffsetBottom()+getPaddingTop();

	protected int getCursorBottom(Lineable line, int index) {
		return line.getOffsetBottom();
	}

	/**
	 * not include displaying,include >
	 * BufferStartPoint/BufferLine/BufferWidth/BufferMearsureData...
	 */
	@Override
	public void gc() {
		if (mPoints != null) {
			mPoints.gc();
		}
		if (mLinears != null) {
			mLinears.gc();
		}
		if (mMeasurer != null) {
			mMeasurer.gc();
		}
	}

	@Override
	public void clear() {
		if (mPoints != null) {
			mPoints.die();
			mPoints = null;
		}
		if (mLinears != null) {
			mLinears.clear();
			mLinears.gc();
			mLinears = null;
		}
		if (mMeasurer != null) {
			mMeasurer.clear();
			mMeasurer = null;
		}
		mTempPaint = null;
		if (mSyncTask != null) {
			mSyncTask.cancel();
			mSyncTask = null;
		}
	}

	@Override
	public int hasFullLayoutHeight() {
		if (mLinears.size() <= 0) {
			return -getHeight();
		}
		if (mLinears.getFirst().getMinStart() > getTextMinStart(getText())
				|| mLinears.getLast().getMaxEnd() < getTextMaxEnd(getText())) {
			return getHeight();
		}
		int offset = mLinears.getLast().getBottom() - getHeight() - mLinears.getFirst().getTop();
		return offset;
	}

	public int hasFullLayoutWidth() {
		if (mLinears.size() <= 0) {
			return -getWidth();
		}
		if (isSingleLine()) {
			Lineable firstLine = getFirstLine();
			Lineable lastLine = getLastLine();
			if (firstLine.getStart() > firstLine.getMinStart() || lastLine.getEnd() < lastLine.getMaxEnd()) {
				return getWidth();
			}
			int offset = (int) (mLinears.getLast().getMaxSite() - getWidth());
			return offset;
		}
		return 0;
	}

	private static Handler handler;

	public static final void newTestHandler(final Context context) {
		handler = new Handler(context.getMainLooper()) {

			private Toast toast;

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (toast == null) {
					toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
				}
				toast.setText("" + msg.obj);
				toast.show();
			}

		};
	}

	/**
	 * 为了更优的体验,后台测量,同步Task
	 */
	private class PointSyncTask extends SyncTask {

		private Measurer bufferMeasurer;
		private PointSyncTask mTask;

		/**
		 * 
		 * @param time
		 *            延迟时间之后启动测量程序
		 * @param m
		 *            测量类
		 */
		public PointSyncTask(int time, Measurer m) {
			super(time);
			bufferMeasurer = m;
		}

		@Override
		void execute() {
			CharSequence text = getText();
			TextAreaPaint paint = getPaint();

			int length = getTextMaxEnd(text);
			int maxEnd = mLinears.getLast().getMaxEnd();
			int minStart;

			Lineable mLine = bufferMeasurer.installNewLineable();
			if (maxEnd < length) {
				while (!mPoints.isLimit() && !isCancel()) {
					try {
						bufferMeasurer.measureAfterLine(text, maxEnd, length, 0, mLine, paint, 0);
					} catch (Exception e) {
						break;
					}
					minStart = mLine.getMinStart();
					if (minStart >= length) {
						break;
					}
					if (isCancel() || minStart < 0 || !mPoints.insertLine(minStart)) {
						break;
					}
					maxEnd = mLine.getMaxEnd();
				}
			}
		}

		@Override
		void willWait() {
		}

		@Override
		void startThread() {
			new Thread(this).start();
		}
	}

	public final boolean isLimit() {
		return mPoints.isLimit();
	}

}

/**
 * 保留类
 * 
 * @author Maizer/麦泽
 */
abstract class SyncTask implements Runnable {

	private final int TIME_WAIT;

	private Object LOCK = new Object();
	private Object SYNC = new Object();

	private final static int NEW_STATE = 1;
	private final static int START_STATE = 2;
	private final static int WAIT_STATE = 3;
	private final static int WAITED_STATE = 4;
	private final static int EXCUTE_STATE = 5;
	private final static int END_STATE = 6;

	private volatile boolean isCancel;
	private volatile boolean isNotify;

	private transient volatile int state;

	public SyncTask(int time) {
		if (time <= 0) {
			throw new IllegalAccessError("Time wait 0 >=" + time);
		}
		TIME_WAIT = time;
		state = END_STATE;
	}

	public int getTime() {
		return TIME_WAIT;
	}

	public final void startWait() {
		if (state < WAIT_STATE) {
			return;
		}
		synchronized (SYNC) {
			if (state == WAIT_STATE) {
				isNotify = true;
				if (state == WAIT_STATE) {
					synchronized (LOCK) {
						LOCK.notify();
					}
				}
			} else if (state == WAITED_STATE) {
				synchronized (SYNC) {
					isNotify = true;
				}
				if (state == EXCUTE_STATE) {
					isCancel = true;
				}
			} else if (state == EXCUTE_STATE) {
				isCancel = true;
			} else if (state == END_STATE) {
				state = NEW_STATE;
				isCancel = false;
				startThread();
			}
		}
	}

	public final boolean isExecuting() {
		return state == EXCUTE_STATE;
	}

	public final boolean isCancel() {
		return isCancel;
	}

	public final void cancel() {
		if (state == END_STATE || state == 0) {
			return;
		}
		isCancel = true;
		isNotify = false;
		if (state == WAIT_STATE) {
			synchronized (LOCK) {
				LOCK.notify();
			}
		}
	}

	abstract void startThread();

	abstract void execute();

	abstract void willWait();

	public final void run() {
		if (state == END_STATE) {
			return;
		}
		state = START_STATE;
		try {
			willWait();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		isNotify = true;
		synchronized (LOCK) {
			try {
				while (true) {
					synchronized (SYNC) {
						if (!isNotify) {
							break;
						}
					}
					isNotify = false;
					if (isCancel) {
						break;
					}
					state = WAIT_STATE;
					LOCK.wait(TIME_WAIT);
					state = WAITED_STATE;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			state = EXCUTE_STATE;
		}
		try {
			if (!isCancel) {
				execute();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		state = END_STATE;
	}
}

/**
 * 保留类
 * 
 * @author Maizer/麦泽
 */
class PointArray implements ArrayGc {

	private static final String TAG = PointArray.class.getCanonicalName();
	private final Object SYNC = new Object();
	private volatile int mLen;
	private volatile int[] mPoints;
	private int capacity = 2;
	private TextAreaLayout layout;

	public PointArray(int size, TextAreaLayout l) {
		if (size == 0) {
			size = 1;
		}
		int limit = l.getMaxLines();
		if (limit == 1 && l.getTruncateAt() == TruncateAt.MIDDLE) {
			limit = 2;
		}
		mPoints = new int[limit <= 0 ? size : limit];
		layout = l;
	}

	public void die() {
		Arrays.fill(mPoints, 0);
		mPoints = null;
	}

	public void gc() {
		if (mLen == 0) {
			Arrays.fill(mPoints, 0);
			mPoints = new int[capacity];
		} else if (mLen < mPoints.length) {
			int[] newPointArray = new int[mLen];
			System.arraycopy(mPoints, 0, newPointArray, 0, mLen);
			Arrays.fill(mPoints, 0);
			mPoints = newPointArray;
		}
	}

	public int length() {
		return mLen;
	}

	public void setIncreaseCapacity(int size) {
		if (size < capacity) {
			return;
		}
		capacity = size;
	}

	public void capacity(int maxSize) {
		if (maxSize > mPoints.length) {
			mPoints = Arrays.copyOf(mPoints, maxSize);
		}
	}

	public void removeLast(int l) {
		synchronized (SYNC) {
			if (l == -1) {
				if (mLen > 0) {
					mLen--;
				}
			} else if (mLen > 0) {
				mLen -= (mLen - l);
				if (mLen < 0) {
					mLen = 0;
				}
			}
		}
	}

	public boolean set(int l, Lineable line) {
		if (l >= 0) {
			mPoints[l] = line.getMinStart();
			return true;
		} else {
			return insertLine(line);
		}
	}

	public boolean insertLine(Lineable line) {
		synchronized (SYNC) {
			int start = line.getMinStart();
			int l;
			l = index(start);
			if (l < 0) {
				return insert(l, start);
			}
			return true;
		}
	}

	public boolean insertLine(int start) {
		synchronized (SYNC) {
			int l = index(start);
			if (l < 0) {
				return insert(l, start);
			}
			return true;
		}
	}

	public boolean nextIsLimit() {
		int limit = layout.getMaxLines() - 1;
		if (!(limit > 0 && mLen >= limit)) {
			return false;
		}
		int count = layout.getLineRecyleCount();
		if (count > 0 && mLen > 0) {
			int min = layout.getLineMin(count - 1);
			return mPoints[limit - 1] <= min;
		}
		return true;
	}

	public boolean isLimit() {
		int limit = layout.getMaxLines();
		if (!(limit > 0 && mLen >= limit)) {
			return false;
		}
		int count = layout.getLineRecyleCount();
		if (count > 0 && mLen > 0) {
			int min = layout.getLineMin(count - 1);
			return mPoints[limit - 1] <= min;
		}
		return true;
	}

	private boolean insert(int location, int point) {
		if (location > 0) {
			throw new ArrayIndexOutOfBoundsException();
		}
		int limit = layout.getMaxLines();
		if (limit > 0 && (mLen >= limit || location >= limit)) {
			return false;
		}
		location = -location >= mLen ? -location : ~location;
		if (mLen >= mPoints.length) {
			mPoints = Arrays.copyOf(mPoints, mLen + capacity);
		} else if (location >= mPoints.length) {
			mPoints = Arrays.copyOf(mPoints, location + capacity);
		}
		mPoints[location] = point;
		mLen++;
		return true;
	}

	public void add(int point) {
		if (mPoints.length <= mLen) {
			mPoints = Arrays.copyOf(mPoints, mLen + capacity);
		}
		mPoints[mLen] = point;
		mLen++;
	}

	public void clear() {
		mLen = 0;
	}

	public int get(int l) {
		return mPoints[l];
	}

	public int search(int point) {
		int start = 0;
		int end = mLen - 1;
		int bin = -1;
		int p = 0;
		while (start <= end) {
			p = mPoints[bin = (end + start) >> 1];
			if (p > point) {
				end = bin - 1;
			} else if (p == point) {
				return bin;
			} else {
				start = bin + 1;
			}
		}
		if (point < p) {
			bin--;
		}
		return bin;
	}

	public int index(int point) {
		int start = 0;
		int end = mLen - 1;
		int bin = 0;
		while (start <= end) {
			bin = (end + start) >> 1;
			int p = mPoints[bin];
			if (p > point) {
				end = bin - 1;
			} else if (p == point) {
				return bin;
			} else {
				start = bin + 1;
			}
		}
		return ~bin;
	}

	public int indexSync(int point) {
		synchronized (SYNC) {
			int start = 0;
			int end = mLen - 1;
			int bin = 0;
			while (start <= end) {
				bin = (end + start) >> 1;
				int p = mPoints[bin];
				if (p > point) {
					end = bin - 1;
				} else if (p == point) {
					return bin;
				} else {
					start = bin + 1;
				}
			}
			return ~bin;
		}
	}
}