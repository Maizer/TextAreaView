package com.maizer.text.layout;

import com.maizer.BuildConfig;
import com.maizer.text.layout.TextAreaLayout.Alignment;
import com.maizer.text.liner.Lineable;
import com.maizer.text.measure.Measurer;
import com.maizer.text.util.ObjectLinked;

import android.text.Selection;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.widget.RelativeLayout;

public class SingleMiddleLayout extends FormatLayout {

	private static final String TAG = SingleMiddleLayout.class.getCanonicalName();

	public SingleMiddleLayout(LayoutAttribute attrubute, CharSequence text, TextAreaPaint wp, boolean startSyncTask) {
		super(attrubute, text, wp, startSyncTask);
	}

	public int checkTruncateAt(CharSequence mEditor, int selectEnd) {
		TruncateAt ellipsize = getTruncateAt();
		int maxEms = getMaxEms();

		if (ellipsize == TruncateAt.MIDDLE) {
			int firstEnd = -1;
			int lastStart = -1;
			if (maxEms > 0 && maxEms < mEditor.length()) {
				int bin = maxEms >> 1;
				firstEnd = Measurer.codePointCount(mEditor, 0, mEditor.length(), bin, true);
				lastStart = Measurer.codePointCount(mEditor, 0, mEditor.length(), bin, false);
			} else if (maxEms == -1 && getLineRecyleCount() > 1) {
				firstEnd = getFirstLine().getEnd();
				lastStart = getLastLine().getStart();
			}
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "CheckTruncateAt>");
			}
			if (firstEnd != -1 && lastStart != -1) {
				if (selectEnd > lastStart) {
					if (BuildConfig.DEBUG) {
						Log.e(TAG, ">3");
					}
					return selectEnd;
				}
				if (selectEnd == firstEnd || selectEnd == lastStart) {
					if (BuildConfig.DEBUG) {
						Log.e(TAG, ">2");
					}
					return selectEnd;
				}
				if (selectEnd > firstEnd) {
					if (BuildConfig.DEBUG) {
						Log.e(TAG, ">1");
					}
					if (selectEnd <= (firstEnd + lastStart) / 2) {
						return lastStart;
					}
					return firstEnd;
				}
			}
		}
		return super.checkTruncateAt(mEditor, selectEnd);
	}

	protected float getLeadingMarger(Lineable mLine) {
		Alignment align = getAlignment();
		if (align != null && getMaxLines() == 1) {
			if (getTruncateAt() == TruncateAt.MIDDLE) {
				if (mLine != getLastLine()) {
					if (mLine.getStart() > mLine.getMinStart() || mLine.getEnd() < mLine.getMaxEnd()) {
						return 0;
					}
					Lineable lastLine = getLastLine();
					if (lastLine.getEnd() < lastLine.getMaxEnd() || lastLine.getStart() > lastLine.getMinStart()) {
						return 0;
					}
					if (getWidth() - lastLine.getMaxSite() <= 0) {
						return 0;
					}
					if (align == Alignment.ALIGN_CENTER) {
						return (getWidth() - lastLine.getMaxSite()) / 2;
					} else if (align == Alignment.ALIGN_RIGHT) {
						return getWidth() - lastLine.getMaxSite();
					} else {
						return 0;
					}
				}
			}
		}
		return super.getLeadingMarger(mLine);
	}

	@Override
	public float scrollByHorizontal(float scrollX) {
		if (!isSingleLine()) {
			return -1;
		}
		if (getLineRecyleCount() <= 0) {
			return 0;
		}
		int maxEms = getMaxEms();
		if (getMaxLines() == 1 && getTruncateAt() == TruncateAt.MIDDLE) {
			if (maxEms > 0 && maxEms < getText().length()) {
				ObjectLinked<Lineable> mLinears = getLineArray();
				CharSequence text = getText();
				TextAreaPaint wp = getPaint();
				Measurer mMeasurer = getMeasurer();
				int lastStart = Measurer.codePointCount(text, 0, text.length(), maxEms >> 1, false);
				mLinears.getFromFirst(0, false);
				while (mLinears.hasNext()) {
					Lineable line = mLinears.next();
					line.moveHorizontal(scrollX, text, wp, mMeasurer);
					if (line.getMinStart() == 0) {
						if (scrollX <= line.getMinSite() - line.getLeadingMergin()) {
							return line.getMinSite() - line.getLeadingMergin();
						} else if (scrollX > line.getMaxSite()) {
							if (mLinears.size() > 1) {
								mLinears.removeFirst();
							}
						} else if (scrollX + getWidth() >= line.getMaxSite() && line.getEnd() >= line.getMaxEnd()) {
							if (mLinears.size() < 2) {
								Lineable mLine;
								mLine = mLinears.getRecycle();
								if (mLine == null) {
									mLine = getMeasurer().installNewLineable();
								}
								mLinears.addLast(mLine);
								mMeasurer.measureSingleLineFromFrsit(text, lastStart, text.length(), lastStart,
										text.length(), scrollX + getWidth() - line.getMaxSite(), 0, line.getMaxSite(),
										wp, mLine, null, 0);
								break;
							}
						}
					} else {
						if (scrollX >= line.getMaxSite() - getWidth()) {
							return line.getMaxSite() - getWidth();
						} else if (scrollX + getWidth() < line.getMinSite()) {
							if (mLinears.size() > 1) {
								mLinears.removeLast();
							}
						} else if (line.getStart() == line.getMinStart()) {
							if (mLinears.size() < 2) {
								Lineable mLine;
								mLine = mLinears.getRecycle();
								if (mLine == null) {
									mLine = getMeasurer().installNewLineable();
								}
								mLinears.addFirst(mLine);
								int end = Measurer.codePointCount(text, 0, text.length(), maxEms >> 1, true);
								float ellipsisWidth = getEllipsisWidth(text, getPaint(), end, lastStart);
								float minSite = line.getMinSite() - ellipsisWidth;
								mMeasurer.measureSingleLineFromLast(text, 0, end, end, 0, minSite - scrollX, 0, minSite,
										wp, mLine, TruncateAt.END, ellipsisWidth, true);
								break;
							}
						}
					}
				}
				return -1;
			} else if (maxEms == -1) {
				Lineable firstLine = getFirstLine();
				return firstLine.getMinSite() - firstLine.getLeadingMergin();
			}
		}
		return super.scrollByHorizontal(scrollX);
	}

	private float getEllipsisWidth(CharSequence source, TextAreaPaint paint, int start, int end) {
		String ellipsis = getEllipsis();
		if (getTruncateAt() != null && end < source.length() && ellipsis != null) {
			float v = getMeasurer().measureReplaceChar(source, start, end, ellipsis, paint);
			Log.e(TAG, "0000>" + v);
			return v;
		}
		Log.e(TAG, "0000");
		return 0;
	}

	private Lineable getOrInstallFirst() {
		ObjectLinked<Lineable> mLines = getLineArray();
		if (mLines.size() <= 0) {
			Measurer mMeasurer = getMeasurer();
			Lineable line = mLines.getRecycle();
			if (line == null) {
				line = mMeasurer.installNewLineable();
			}
			mLines.add(line);
			return line;
		} else {
			return mLines.getFirst();
		}
	}

	private Lineable getOrInstallLast() {
		ObjectLinked<Lineable> mLines = getLineArray();
		if (mLines.size() < 2) {
			Measurer mMeasurer = getMeasurer();
			Lineable line = mLines.getRecycle();
			if (line == null) {
				line = mMeasurer.installNewLineable();
			}
			mLines.add(line);
			return line;
		} else {
			return mLines.getLast();
		}
	}

	private boolean inIterationMeasureInstabilityEllipsis(CharSequence text, int length, int width, int binWidth,
			TextAreaPaint paint, Measurer mMeasurer, Lineable firstLine, Lineable lastLine, int firstEnd,
			int lastStart) {
		binWidth -= firstLine.getLeadingMergin();
		mMeasurer.measureSingleLineFromFrsit(text, 0, firstEnd, 0, length, binWidth, 0, 0, paint, firstLine, null, 0);
		mMeasurer.measureSingleLineFromLast(text, lastStart, length, length, 0, binWidth, 0, 0, paint, lastLine, null,
				0, false);
		int start = firstLine.getEnd(), end = lastLine.getStart();
		if (firstLine.getMaxSite() > binWidth) {
			start = Measurer.codePointCount(text, 0, start, 1, false);
		}
		if (lastLine.getMaxSite() > binWidth) {
			end = Measurer.codePointCount(text, end, length, 1, true);
		}
		float ellipsisWidth = getEllipsisWidth(text, paint, start, end);
		float binEllipsisW = ellipsisWidth / 2;
		if (binWidth - binEllipsisW > 0) {
			binWidth = (int) ((width >> 1) - binEllipsisW);
		}
		mMeasurer.measureSingleLineFromFrsit(text, 0, start, 0, start, binWidth, 0, -1, paint, firstLine,
				TruncateAt.END, ellipsisWidth);
		mMeasurer.measureSingleLineFromFrsit(text, end, length, end, length, binWidth, 0, firstLine.getMaxSite(), paint,
				lastLine, null, 0);
		if (BuildConfig.DEBUG) {
			Log.e(TAG,
					"inIterationMeasureInstabilityEllipsis Output>" + lastLine.getEnd() + "  " + length + "  "
							+ lastLine.getMaxSite() + "  " + binWidth + "  " + width + "  "
							+ firstLine.getEllipsisWidth() + "  " + firstLine.getEnd() + "  " + firstLine.getMaxEnd());
		}
		if (firstLine.getEnd() == firstLine.getMaxEnd() && lastLine.getEnd() == length
				&& (lastLine.getMaxSite() <= width || binWidth - binEllipsisW <= 0)) {
			return false;
		}
		return inIterationMeasureInstabilityEllipsis(text, length, width, binWidth, paint, mMeasurer, firstLine,
				lastLine, start, end);
	}

	private boolean measureMiddleLimitWidth(TextAreaScroller aide, float ellipsisWidth) {
		Lineable firstLine = getOrInstallFirst();
		Measurer mMeasurer = getMeasurer();
		TextAreaPaint paint = getPaint();
		CharSequence text = getText();
		int length = text.length();
		int width = getWidth();
		mMeasurer.measureSingleLineFromFrsit(text, 0, length, 0, length, width, 0, -1, paint, firstLine, null, 0);
		if ((firstLine.getEnd() < length || firstLine.getMaxSite() > width) && firstLine.getEnd() > 1) {
			inIterationMeasureInstabilityEllipsis(text, length, width, width >> 1, paint, mMeasurer, firstLine,
					getOrInstallLast(), length, 0);
		} else {
			ObjectLinked<Lineable> lines = getLineArray();
			if (lines.size() > 1) {
				lines.removeLast();
			}
		}
		if (aide != null) {
			aide.requestTextScrollBy(-aide.getTextScrollX(), 0);
		}
		return false;
	}

	/**
	 * 主要测量策略
	 * 
	 * @param index
	 * @param aide
	 * @return
	 */
	private boolean measureMiddle(int index, int ems, TextAreaScroller aide) {

		CharSequence text = getText();

		int length = text.length();
		if (length <= 0) {
			return false;
		}
		ObjectLinked<Lineable> mLines = getLineArray();
		if (ems == 0 || ems >= length) {
			if (mLines.size() > 1) {
				mLines.removeLast();
			}
			return super.requestSingLineMeasure(index, ems, aide);
		}
		if (ems == -1) {
			return measureMiddleLimitWidth(aide, 0);
		}
		Measurer mMeasurer = getMeasurer();
		TextAreaPaint paint = getPaint();
		int width = getWidth();
		int bin = ems >> 1;

		int firstEnd = Measurer.codePointCount(text, 0, text.length(), bin, true);
		int lastStart = Measurer.codePointCount(text, 0, text.length(), bin, false);

		if (index > firstEnd && index < lastStart) {
			index = firstEnd;
		}
		if (findIndexFromLines(index, aide)) {
			return false;
		}

		int scrollX = aide == null ? 0 : aide.getTextScrollX();

		if (index <= firstEnd) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "measureMiddle Output> index<=firstEnd");
			}
			Lineable firstLine = getOrInstallFirst();
			if (firstLine.getMinStart() != 0) {
				firstLine.clear();
			}
			float offsetScroll = 0;
			float offsetLeading = 0;
			if (index <= firstLine.getStart()) {
				index--;
				if (index < 0) {
					index = 0;
				}
				mMeasurer.measureSingleLineFromFrsit(text, 0, firstEnd, index, firstEnd, width, 0, 0, paint, firstLine,
						TruncateAt.END, getEllipsisWidth(text, paint, firstEnd, lastStart));
				offsetScroll = firstLine.getMinSite();
				if (index == 0) {
					offsetScroll -= firstLine.getLeadingMergin();
				}
				if (firstLine.getEnd() < firstLine.getMaxEnd()) {
					if (mLines.size() > 1) {
						mLines.removeLast();
						if (aide != null) {
							aide.requestTextScrollBy((int) offsetScroll - scrollX, 0);
						}
						return false;
					}
				}
				if (firstLine.getMaxSite() >= width) {
					if (aide != null) {
						aide.requestTextScrollBy((int) offsetScroll - scrollX, 0);
					}
					return false;
				}
			} else if (index >= firstLine.getEnd()) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "measureMiddle Output> index >= firstLine.getEnd()");
				}
				offsetScroll = (int) (firstLine.getMaxSite() - scrollX);
				if (firstLine.getMinStart() != firstLine.getStart()) {
					width = (int) offsetScroll;
				}
				if (index == firstLine.getEnd()) {
					index++;
					if (index > firstEnd) {
						index = firstEnd;
					}
				}
				mMeasurer.measureSingleLineFromLast(text, 0, firstEnd, index, 0, width, 0, 0, paint, firstLine,
						TruncateAt.END, getEllipsisWidth(text, paint, firstEnd, lastStart), false);
				offsetScroll = firstLine.getMaxSite() - width;
				if (offsetScroll < 0) {
					offsetScroll = firstLine.getMinSite();
				}
			} else {
				offsetScroll = scrollX - firstLine.getMinSite();
				mMeasurer.measureSingleLineFromFrsit(text, 0, firstEnd, firstLine.getStart(), firstEnd, width, 0, 0,
						paint, firstLine, TruncateAt.END, getEllipsisWidth(text, paint, firstEnd, lastStart));
				offsetLeading = offsetScroll;
			}
			if (firstEnd <= firstLine.getEnd()) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "measureMiddle Output> firstEnd <= firstLine.getEnd()");
				}
				width = (int) (getWidth() - firstLine.getMaxSite() + scrollX);
				Lineable mLine = getOrInstallLast();
				mMeasurer.measureSingleLineFromFrsit(text, lastStart, length, lastStart, length, width + offsetLeading,
						0, firstLine.getMaxSite(), paint, mLine, null, 0);
				if (getMaxEms() == -1 && mLine.getMaxSite() - firstLine.getMinSite() > getWidth()) {
					measureMiddle(0, --ems, aide);
				}
			}
			if (aide != null) {
				scrollByHorizontal(offsetScroll);
				aide.requestTextScrollBy((int) (offsetScroll - scrollX), 0);
			}
			return false;
		} else {
			if (index < lastStart) {
				index = lastStart;
			}
			int start = lastStart;
			if (mLines.size() <= 0) {
				Lineable line = mLines.getRecycle();
				if (line == null) {
					line = mMeasurer.installNewLineable();
				}
				mLines.add(line);
			} else {
				Lineable mLine = mLines.getLast();
				if (mLine.getMinStart() != 0) {
					start = mLines.getLast().getStart();
				}
			}
			Lineable mLine = mLines.getLast();
			if (index <= start) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "measureMiddle Output> index <= start(lastStart)");
				}
				index--;
				if (index < lastStart) {
					index = lastStart;
				}
				if (mLines.size() > 1) {
					float maxSite = mLines.getFirst().getMaxSite();
					mMeasurer.measureSingleLineFromFrsit(text, lastStart, length, index, length,
							width - maxSite + scrollX, 0, maxSite, paint, mLine, null, 0);
				} else {
					mMeasurer.measureSingleLineFromFrsit(text, lastStart, length, index, length, width, 0, 0, paint,
							mLine, null, 0);
					float offsetScroll = mLine.getMinSite();
					scrollByHorizontal(offsetScroll);
					if (mLines.size() > 1) {
						aide.requestTextScrollBy((int) (mLines.getFirst().getMinSite() - scrollX), 0);
					} else {
						aide.requestTextScrollBy((int) (offsetScroll - scrollX), 0);
					}
				}
			} else if (index >= mLine.getEnd() && mLine.getEnd() > 0) {
				Lineable firstLine = mLines.getFirst();
				if (index == mLine.getEnd()) {
					index++;
					if (index > length) {
						index = length;
					}
				}
				float offsetLeading = 0;
				if (firstLine.getMinStart() == 0) {
					offsetLeading = firstLine.getMaxSite();
					start = lastStart;
				}
				mMeasurer.measureSingleLineFromLast(text, lastStart, length, index, start, width, 0, offsetLeading,
						paint, mLine, null, 0, false);
				float offsetScrollX = mLine.getMaxSite() - width;
				if (mLines.size() < 2 && mLine.getMinStart() == mLine.getStart() && offsetScrollX < 0) {
					firstLine = mLines.getRecycle();
					if (firstLine == null) {
						firstLine = mMeasurer.installNewLineable();
					}
					mLines.addFirst(firstLine);
					mMeasurer.measureSingleLineFromLast(text, 0, firstEnd, firstEnd, 0, -offsetScrollX, 0,
							mLine.getMinSite(), paint, firstLine, TruncateAt.END,
							getEllipsisWidth(text, paint, firstEnd, lastStart), false);
				}
				if (mLine.getSiteSize() > 0) {
					scrollByHorizontal(offsetScrollX);
					if (firstLine != mLine && firstLine.getMinStart() == firstLine.getStart()
							&& firstLine.getMaxEnd() == firstLine.getMaxEnd() && mLine.getStart() == mLine.getMinStart()
							&& mLine.getMaxEnd() == mLine.getEnd()
							&& mLine.getMaxSite() - firstLine.getMinSite() <= width) {
						offsetScrollX = firstLine.getMinSite();
					}
					aide.requestTextScrollBy((int) (offsetScrollX - scrollX), 0);
				}
				return false;
			} else {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "measureMiddle Output>index>lastStart,index<length");
				}
				float offsetScrollX;
				Lineable firstLine = mLines.getFirst();
				if (firstLine.getMinStart() == 0) {
					mMeasurer.measureSingleLineFromFrsit(text, lastStart, length, lastStart, length,
							width - firstLine.getMaxSite() + scrollX, 0, firstLine.getMaxSite(), paint, mLine, null, 0);
					offsetScrollX = mLine.getMaxSite() - getWidth();
					if (mLine.getStart() != mLine.getMinStart()) {
						scrollByHorizontal(offsetScrollX);
					}
					if (index < mLine.getEnd()) {
						return false;
					}
					if (aide != null) {
						if (mLine.getStart() != mLine.getMinStart()) {
							aide.requestTextScrollBy((int) (offsetScrollX - scrollX), 0);
						}
						return true;
					}
				} else {
					offsetScrollX = scrollX - mLine.getMinSite();
					mMeasurer.measureSingleLineFromFrsit(text, lastStart, length,
							mLine.getStart() < lastStart ? lastStart : mLine.getStart(), length, width + offsetScrollX,
							0, 0, paint, mLine, null, 0);
					scrollByHorizontal(offsetScrollX);
					if (aide != null) {
						aide.requestTextScrollBy((int) (offsetScrollX - scrollX), 0);
						return true;
					}
				}

			}

		}
		return false;

	}

	private boolean findIndexFromLines(int index, TextAreaScroller scroller) {
		ObjectLinked<Lineable> mLines = getLineArray();
		if (mLines.size() > 0) {
			mLines.getFromFirst(0, false);
			while (mLines.hasNext()) {
				Lineable line = mLines.next();
				if (line.getStart() < index && index < line.getEnd()) {
					if (scroller != null) {
						int scrollX = scroller.getTextScrollX();
						int mIndex = index - line.getStart() + 1;
						int count = line.getSiteSomeCountByOffset(mIndex);
						if (line.getSiteSize() <= mIndex + count) {
							return false;
						}
						float site = line.getSite(mIndex + count);
						int wdith = getWidth();
						if (scrollX + wdith <= site) {
							float offsetScroll = site - wdith;
							if (offsetScroll > scrollX) {
								scrollByHorizontal(offsetScroll);
								scroller.requestTextScrollBy((int) (offsetScroll - scrollX), 0);
							}
						} else {
							site = line.getSite(index - line.getStart() - 1);
							if (site < scrollX) {
								scrollByHorizontal(site);
								scroller.requestTextScrollBy((int) (site - scrollX), 0);
							}
						}
					}
				}
			}
		}
		return false;
	}

	protected void generateHorizontalWithLimit(CharSequence source, int bufStart, int bufEnd, int v) {
		requestSingLineMeasure(0, getMaxEms(), null);
	}

	protected boolean requestSingLineMeasure(int index, int ems, TextAreaScroller aide) {
		if (getWidth() <= 0 || getHeight() <= 0) {
			return false;
		}
		TruncateAt ellipsize = getTruncateAt();
		ObjectLinked<Lineable> mLinears = getLineArray();
		if (ellipsize == TruncateAt.MIDDLE) {
			boolean handle = measureMiddle(index, getMaxEms(), aide);
			checkLimitLines();
			return handle;
		}
		if (mLinears.size() > 1) {
			mLinears.removeLast();
		}
		boolean handle = super.requestSingLineMeasure(index, ems, aide);
		checkLimitLines();
		return handle;
	}
}
