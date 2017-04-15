package com.maizer.text.layout;

import com.maizer.text.BuildConfig;
import com.maizer.text.cursor.CursorDevicer;
import com.maizer.text.cursor.CursorDevicer.CursorHelper;
import com.maizer.text.layout.TextAreaLayout.Alignment;
import com.maizer.text.liner.Lineable;
import com.maizer.text.measure.Measurer;
import com.maizer.text.util.ObjectLinked;

import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils.TruncateAt;
import android.util.Log;

public class DefaultCursorHelper implements CursorHelper {

	private static final String TAG = DefaultCursorHelper.class.getCanonicalName();
	private FormatLayout layout;
	private boolean isSelecting;

	public DefaultCursorHelper(FormatLayout l) {
		layout = l;
	}

	protected boolean requestMoveCursor(int x, int y, CursorDevicer cAide, TextAreaScroller vAide, boolean isSelect,
			boolean refreshAll, boolean sendUpdate) {
		if (isSelecting()) {
			return false;
		}
		FormatLayout mLayout = layout;
		CharSequence text = mLayout.getText();
		if (text instanceof Spannable) {
			if (text == null || text.length() <= 0) {
				regainCursor(cAide, refreshAll, sendUpdate);
				return refreshAll;
			}
			selecting();
			if (!refreshAll) {
				refreshAll = Selection.getSelectionStart(text) != Selection.getSelectionEnd(text);
			}
			boolean isSingleLine = mLayout.isSingleLine();
			boolean handUpdata = refreshAll;

			int paddingLeft = mLayout.getPaddingLeft();
			int paddingTop = mLayout.getPaddingTop();
			float letterSpacing = mLayout.getLetterSpacing();

			if (!isSingleLine) {
				ObjectLinked<Lineable> mLinears = mLayout.getLineArray();
				int raw = mLayout.getLineForVertical((int) y);
				if (raw <= 0) {
					int minStart = mLinears.getFirst().getMinStart();
					if (minStart <= mLayout.getTextMinStart(text)) {
						raw = mLayout.getLineForVertical((int) y);
					} else {
						mLayout.topToIndex(vAide, -1, false);
						raw = mLayout.getLineForOffset(minStart + 1);
					}
					Lineable line = mLinears.get(raw);
					y = line.getTop();

				} else if (raw >= mLinears.size()) {
					Lineable line = mLinears.getLast();
					int maxEnd = line.getMaxEnd();
					if (maxEnd < mLayout.getTextMaxEnd(text)) {
						mLayout.generateVerticalWithLimit(text, maxEnd, maxEnd + 1, line.getBottom(),
								vAide.getTextScrollY(), false, false);
						mLayout.removeFirst(vAide.getTextScrollY());
						raw = mLayout.getLineForVertical((int) y);
						if (raw >= mLinears.size()) {
							raw = mLinears.size() - 1;
						}
					} else {
						raw = mLinears.size() - 1;
					}
				}
				if (raw < 0) {
					selected();
					return false;
				}

				Lineable line = mLinears.get(raw);
				float lineLeading = mLayout.getLeadingMarger(line);
				float leading = lineLeading + paddingLeft;

				int top = mLayout.getCursorTop(null, line, raw) + paddingTop;
				int bottom = mLayout.getCursorBottom(line, raw) + paddingTop;

				x = (int) (line.getExactHorizontalForHorizontal(text, x - leading, letterSpacing, true, isSelect)
						+ leading);
				y = top;
				handUpdata |= checkScroll(raw, Selection.getSelectionEnd(text), text.length(), vAide,
						mLayout.getHeight(), true);
				if (cAide != null) {
					cAide.requestUpdate(top, bottom, x, y, letterSpacing, refreshAll, sendUpdate);
				}
			} else {
				int index = mLayout.getLineForHorizontal(x);
				Lineable line = mLayout.getTextLiner(index);
				float lineLeading = mLayout.getLeadingMarger(line);

				float leading = lineLeading + paddingLeft;
				int top = mLayout.getCursorTop(null, line, 0) + paddingTop;
				int bottom = mLayout.getCursorBottom(line, 0) + paddingTop;

				x = (int) (line.getExactHorizontalForHorizontal(text, x - leading, letterSpacing, true, isSelect)
						+ leading);
				int selectEnd = Selection.getSelectionEnd(text);
				y = top;
				mLayout.requestSingLineMeasure(selectEnd, mLayout.getMaxEms(), vAide);
				index = mLayout.getLineForHorizontal(x - leading);
				line = mLayout.getTextLiner(index);
				x = (int) (line.getHorizontalForOffset(text, selectEnd, letterSpacing, false, false) + leading);
				refreshAll = true;
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "requestMoveCursorByXY>" + (x - leading) + "  " + y + "  " + "  " + index + "  "
							+ selectEnd + "   " + x);
				}
				if (cAide != null) {
					cAide.requestUpdate(top, bottom, x, y, letterSpacing, refreshAll, sendUpdate);
				}
			}
			selected();
			return handUpdata;
		}
		return false;

	}

	protected final void selecting() {
		isSelecting = true;
	}

	protected final void selected() {
		isSelecting = false;
	}

	public final boolean isSelecting() {
		return isSelecting;
	}

	public void regainCursor(CursorDevicer cAide, boolean refreshAll, boolean sendUpdate) {
		if (cAide == null) {
			return;
		}
		FormatLayout mLayout = layout;
		Alignment a = mLayout.getAlignment();
		int h = mLayout.getDefaultCursorHeight();
		if (a != null) {
			switch (a) {
			case ALIGN_CENTER:
				cAide.requestUpdate(0, h,
						(int) (mLayout.getPaddingLeft() + mLayout.getWidth() / 2 + mLayout.getLetterSpacing() / 2),
						mLayout.getPaddingTop(), mLayout.getLetterSpacing(), refreshAll, sendUpdate);
				return;
			case ALIGN_RIGHT:
				cAide.requestUpdate(0, h,
						(int) (mLayout.getPaddingLeft() + mLayout.getWidth() - mLayout.getLetterSpacing() / 2),
						mLayout.getPaddingTop(), mLayout.getLetterSpacing(), refreshAll, sendUpdate);
				return;
			}
		}
		cAide.requestUpdate(0, h, mLayout.getPaddingLeft(), mLayout.getPaddingTop(), mLayout.getLetterSpacing(),
				refreshAll, sendUpdate);
	}

	private boolean checkScroll(int raw, int select, int length, TextAreaScroller aide, int maxH, boolean isMoveRange) {

		boolean handUpdata = false;
		FormatLayout mLayout = layout;
		ObjectLinked<Lineable> mLinears = mLayout.getLineArray();
		Lineable firstLine;
		Lineable lastLine;
		if ((firstLine = mLinears.getFirst()).getMaxEnd() >= select) {
			// int firstBottom = firstLine.getBottom();
			// int firstHeight = firstLine.getHeight();
			// int firstTop = firstLine.getTop();
			// if (firstTop < scrollY) {
			// if (scrollY - firstHeight > firstTop) {
			// handUpdata = true;
			// mLayout.scrollByVertical(scrollY - firstHeight);
			// aide.requestTextScrollBy(0, -firstHeight);
			// } else if (scrollY != firstTop) {
			// handUpdata = true;
			// mLayout.scrollByVertical(firstTop);
			// aide.requestTextScrollBy(0, firstTop - scrollY);
			// }
			// } else if (firstBottom - scrollY >= firstHeight) {
			// if (scrollY - firstHeight >= firstTop) {
			// handUpdata = true;
			// mLayout.scrollByVertical(scrollY - firstHeight);
			// aide.requestTextScrollBy(0, -firstHeight);
			// } else if (scrollY != firstTop) {
			// handUpdata = true;
			// mLayout.scrollByVertical(firstTop);
			// aide.requestTextScrollBy(0, firstTop - scrollY);
			// }
			// }
			// } else {
			// if (select < firstLine.getMinStart()) {
			if (!isMoveRange && select == firstLine.getMaxEnd()) {
				select--;
			}
			handUpdata |= mLayout.topToIndex(aide, select, false);
			int scrollY = aide.getTextScrollY();
			int index = mLayout.getLineForOffset(select);
			Lineable textLine = mLayout.getTextLiner(index);
			if (textLine != null) {
				int offsetBottom = mLayout.getCursorTop(null, textLine, index);
				if (scrollY != offsetBottom) {
					mLayout.scrollByVertical(offsetBottom);
					aide.requestTextScrollBy(0, offsetBottom - scrollY);
					handUpdata = true;
				}
			}
			// } else if (raw > 0) {
			// Lineable selectLine = mLayout.getTextLiner(raw - 1);
			// int offsetBottom = selectLine.getOffsetBottom();
			// if (scrollY > offsetBottom) {
			// mLayout.scrollByVertical(offsetBottom);
			// aide.requestTextScrollBy(0, offsetBottom - scrollY);
			// }
			// }
			// }
		} else if ((lastLine = mLinears.getLast()).getMaxEnd() <= select) {
			int scrollY = aide.getTextScrollY();
			int maxEnd = lastLine.getMaxEnd();
			if (maxEnd < select) {
				handUpdata |= mLayout.bottomToIndex(aide, select, false);
			} else {
				firstLine = mLinears.getFirst();
				int lastTop = lastLine.getBottom();
				if (scrollY + maxH < lastTop) {
					int h = lastTop - scrollY - maxH;
					if (h > 0) {
						handUpdata = true;
						mLayout.scrollByVertical(scrollY + h);
						aide.requestTextScrollBy(0, h);
					}
				} else if ((lastTop -= maxH) != scrollY) {
					handUpdata = true;
					mLayout.scrollByVertical(lastTop);
					aide.requestTextScrollBy(0, lastTop - scrollY);
				}
			}
		} else {
			int scrollY = aide.getTextScrollY();
			if (raw == -1) {
				raw = mLayout.getLineForOffset(select);
			}
			if (raw > 0) {
				Lineable nextLine = mLayout.getTextLiner(raw - 1);
				int bottom = nextLine.getBottom();
				if (bottom == scrollY) {
				} else if (bottom < scrollY) {
					handUpdata = true;
					mLayout.scrollByVertical(bottom);
					aide.requestTextScrollBy(0, bottom - scrollY);
				} else {
					Lineable selectLine = mLayout.getTextLiner(raw);
					int firstTop = firstLine.getTop();
					int lastTop = selectLine.getBottom();
					if (lastTop - firstTop > maxH) {
						int h = lastTop - scrollY - maxH;
						if (h > 0) {
							handUpdata = true;
							mLayout.scrollByVertical(scrollY + h);
							aide.requestTextScrollBy(0, h);
						}
					} else {
						float value = mLayout.scrollByVertical(scrollY);
						if (value != -1 && value < scrollY) {
							aide.requestTextScrollBy(0, (int) (value - scrollY));
							handUpdata = true;
						}
					}
				}
			}
		}
		return handUpdata;
	}

	@Override
	public boolean requestMoveCursor(int x, int y, CursorDevicer cursor, TextAreaScroller ts) {
		return requestMoveCursor(x, y, cursor, ts, false, false, false);
	}

	public boolean requestMoveCursor(CursorDevicer cursor, TextAreaScroller aide) {
		if (isSelecting()) {
			return false;
		}
		selecting();
		// if (BuildConfig.DEBUG) {
		// Log.e(TAG, "CursorChanged");
		// }
		FormatLayout mLayout = layout;
		CharSequence text = mLayout.getText();
		ObjectLinked<Lineable> mLinears = mLayout.getLineArray();
		if (text == null || text.length() <= 0 || mLinears.size() == 0) {
			regainCursor(cursor, true, true);
			selected();
			return true;
		}
		if (text instanceof Spannable) {
			int selectStart = Selection.getSelectionStart(text);
			int selectEnd = Selection.getSelectionEnd(text);
			int maxEms = mLayout.getMaxEms();
			int length = mLayout.getTextMaxEnd(text);
			boolean refreshAll = selectStart != selectEnd;

			if (selectStart == -1) {
				TruncateAt ellipsize = mLayout.getTruncateAt();
				if (maxEms > 0 && ellipsize != null) {
					switch (mLayout.getTruncateAt()) {
					case START:
						selectStart = text.length() - Measurer.codePointCount(text, 0, text.length(), maxEms, false);
						break;
					default:
						selectStart = 0;
					}
				} else {
					selectStart = 0;
				}
			} else {
				selectStart = mLayout.checkTruncateAt(text, selectStart);
			}
			if (selectEnd == -1) {
				TruncateAt ellipsize = mLayout.getTruncateAt();
				if (maxEms > 0 && ellipsize != null) {
					switch (mLayout.getTruncateAt()) {
					case START:
						selectEnd = text.length() - Measurer.codePointCount(text, 0, text.length(), maxEms, false);
						break;
					default:
						selectEnd = 0;
					}
				} else {
					selectEnd = 0;
				}
			} else {
				selectEnd = mLayout.checkTruncateAt(text, selectEnd);
			}
			boolean handUpdata = refreshAll;
			boolean isSingleLine = mLayout.isSingleLine();
			if (isSingleLine) {
				handUpdata |= mLayout.requestSingLineMeasure(selectEnd, mLayout.getMaxEms(), aide);
			} else {
				handUpdata |= checkScroll(-1, selectEnd, length, aide, mLayout.getHeight(), false);
			}
			int raw = mLayout.getLineForOffset(selectEnd);
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "requestMoveCursorBySelect 1>" + selectEnd + "   " + selectStart + "   " + length + "  "
						+ raw + "  " + text.length());
			}
			if (raw < 0) {
				if (maxEms > 0 && maxEms < text.length() && mLayout.getLastLine().getMaxEnd() == maxEms) {
					raw = mLayout.getLineRecyleCount() - 1;
				} else if (mLayout.isLimit()) {
					raw = mLayout.getLineRecyleCount() - 1;
					if (selectEnd > mLayout.getLastLine().getMaxEnd()) {
						selectEnd = mLayout.getLastLine().getMaxEnd();
						if (text instanceof Spannable) {
							Selection.setSelection((Spannable) text, selectEnd);
						}
					}
				}
			}
			if (raw >= 0) {
				if (selectEnd > length) {
					selectEnd = length;
				}
				Lineable mLine = mLayout.getTextLiner(raw);
				float letterSpacing = mLayout.getLetterSpacing();
				int top = mLayout.getCursorTop(null, mLine, isSingleLine ? 0 : raw) + mLayout.getPaddingTop();
				int bottom = mLayout.getCursorBottom(mLine, isSingleLine ? 0 : raw + 1) + mLayout.getPaddingTop();
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "requestMoveCursorBySelect 2>" + selectEnd + "   " + selectStart);
				}
				int x = (int) (mLine.getHorizontalForOffset(text, selectEnd, letterSpacing, true, true)
						+ mLayout.getLeadingMarger(mLine) + mLayout.getPaddingLeft());
				int y = top;
				if (cursor != null) {
					cursor.requestUpdate(top, bottom, x, y, letterSpacing, mLayout.isSingleLine() ? true : refreshAll,
							true);
				}
			}
			selected();
			return handUpdata;
		}
		selected();
		return false;
	}

	public boolean moveDown(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect) {
		FormatLayout mLayout = layout;
		if (mLayout.isSingleLine()) {
			return true;
		}
		int cursorY = mCursorAide.getCurrY();
		int lineIndex = mLayout.getLineForVertical(cursorY);
		if (lineIndex < 0) {
			return requestMoveCursor(mCursorAide, vAide);
		}
		Spannable text = (Spannable) mLayout.getText();
		int start = Selection.getSelectionStart(text);
		int end = Selection.getSelectionEnd(text);
		Lineable lastLine = mLayout.getTextLiner(lineIndex);
		int length = mLayout.getTextMaxEnd(text);
		if (lastLine.getMaxEnd() >= length) {
			if (isSelect) {
				Selection.setSelection(text, start, text.length());
			} else {
				Selection.setSelection(text, text.length(), text.length());
				if (start != end) {
					return false;
				}
			}
			return true;
		} else if (isSelect || start == end) {
			int minStart = mLayout.getFirstLine().getMinStart();
			int maxEnd = mLayout.getLastLine().getMaxEnd();
			if ((start >= minStart && start <= maxEnd) || (end >= minStart && maxEnd >= end)) {
				return requestMoveCursor(mCursorAide.getCurrX(),
						lastLine.getTop() + mLayout.getPaddingTop() + lastLine.getHeight(), mCursorAide, vAide,
						isSelect, isSelect, true);
			}
			return requestMoveCursor(mCursorAide, vAide);
		} else if (start > end) {
			Selection.setSelection(text, start);
			return false;
		}
		Selection.setSelection(text, end);
		return false;
	}

	public boolean moveUp(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect) {
		FormatLayout mLayout = layout;
		if (mLayout.isSingleLine()) {
			return true;
		}
		int cursorY = mCursorAide.getCurrY();
		int lineIndex = mLayout.getLineForVertical(cursorY);
		if (lineIndex < 0) {
			return requestMoveCursor(mCursorAide, vAide);
		}
		Spannable text = (Spannable) mLayout.getText();
		int start = Selection.getSelectionStart(text);
		int end = Selection.getSelectionEnd(text);
		if (lineIndex >= 0) {
			Lineable line = mLayout.getTextLiner(lineIndex);
			if (line.getMinStart() <= 0) {
				if (isSelect) {
					Selection.setSelection(text, start, 0);
				} else {
					Selection.setSelection(text, 0, 0);
					if (start != end) {
						return false;
					}
				}
			} else if (isSelect || start == end) {
				int minStart = mLayout.getFirstLine().getMinStart();
				int maxEnd = mLayout.getLastLine().getMaxEnd();
				if ((start >= minStart && start <= maxEnd) || (end >= minStart && maxEnd >= end)) {
					cursorY--;
					return requestMoveCursor(mCursorAide.getCurrX(), cursorY, mCursorAide, vAide, isSelect, isSelect,
							true);
				} else {
					return requestMoveCursor(mCursorAide, vAide);
				}
			} else if (start > end) {
				Selection.setSelection(text, end);
				return false;
			} else {
				Selection.setSelection(text, start);
				return false;
			}
			return true;
		}
		return true;
	}

	public boolean moveLeft(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect) {
		FormatLayout mLayout = layout;
		Spannable mEditor = (Spannable) mLayout.getText();
		TruncateAt ellipsize = mLayout.getTruncateAt();
		int selectStart = Selection.getSelectionStart(mEditor);
		int selectEnd = Selection.getSelectionEnd(mEditor);
		int result = mLayout.checkTruncateAt(mEditor, selectEnd);
		int min = mLayout.isSingleLine() && ellipsize == TruncateAt.END ? mLayout.getFirstLine().getMinStart() : 0;
		if (selectEnd == selectStart) {
			selectEnd = selectStart = result;
		} else {
			selectEnd = result;
		}
		if (isSelect) {
			if (selectEnd > min) {
				if (selectEnd > min + 1) {
					char c = mEditor.charAt(selectEnd - 2);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(mEditor, selectEnd - 2));
						if (count > 1) {
							int end = selectEnd - count;
							Selection.setSelection(mEditor, selectStart, end);
							if (end == selectStart) {
								return false;
							}
							return true;
						}
					}
				}
				int end = selectEnd - 1;
				Selection.setSelection(mEditor, selectStart, selectEnd - 1);
				if (end == selectStart) {
					return false;
				}
			}
		} else if (selectEnd != selectStart) {
			if (selectEnd >= selectStart) {
				Selection.setSelection(mEditor, selectStart);
			} else {
				Selection.setSelection(mEditor, selectEnd);
			}
			return false;
		} else if (selectEnd > min) {
			if (selectEnd > min + 1) {
				char c = mEditor.charAt(selectEnd - 2);
				if (TextAreaLayout.isEmojiEnable(c)) {
					int count = Character.charCount(Character.codePointAt(mEditor, selectEnd - 2));
					if (count > 1) {
						Selection.setSelection(mEditor, selectEnd - count);
						return true;
					}
				}
			}
			Selection.setSelection(mEditor, selectEnd - 1);
		}
		return true;
	}

	public boolean moveRight(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect) {
		FormatLayout mLayout = layout;
		Spannable mEditor = (Spannable) mLayout.getText();
		int selectStart = Selection.getSelectionStart(mEditor);
		int selectEnd = Selection.getSelectionEnd(mEditor);
		int result = mLayout.checkTruncateAt(mEditor, selectEnd);
		if (selectStart == selectEnd) {
			selectEnd = selectStart = result;
		} else {
			selectEnd = result;
		}
		if (isSelect) {
			int length = mLayout.getTextMaxEnd(mEditor);
			if (selectEnd <= length) {
				if (selectEnd <= length - 1) {
					char c = mEditor.charAt(selectEnd);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(mEditor, selectEnd));
						if (count > 1) {
							int end = selectEnd + count;
							Selection.setSelection(mEditor, selectStart, end);
							if (end == selectStart) {
								return false;
							}
							return true;
						}
					}
				}
				int end = selectEnd + 1;
				if (end > mEditor.length()) {
					end = mEditor.length();
				}
				Selection.setSelection(mEditor, selectStart, end);
				if (end == selectStart) {
					return false;
				}
			} else {
				selecting();
				Selection.setSelection(mEditor, selectStart, length);
				selected();
			}
		} else if (selectEnd != selectStart) {
			if (selectEnd >= selectStart) {
				Selection.setSelection(mEditor, selectEnd);
			} else {
				Selection.setSelection(mEditor, selectStart);
			}
			return false;
		} else {
			int length = mLayout.getTextMaxEnd(mEditor);
			if (length <= 0) {
				return false;
			}
			if (selectEnd < length) {
				if (selectEnd < length - 1) {
					char c = mEditor.charAt(selectEnd);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(mEditor, selectEnd));
						if (count > 1) {
							Selection.setSelection(mEditor, selectEnd + count);
							return true;
						}
					}
				}
				Selection.setSelection(mEditor, selectEnd + 1);
			} else {
				selecting();
				Selection.setSelection(mEditor, length);
				selected();
			}
		}
		return true;
	}

}
