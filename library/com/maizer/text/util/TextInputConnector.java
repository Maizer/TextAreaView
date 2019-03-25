package com.maizer.text.util;

import com.maizer.text.cursor.CursorDevicer;
import com.maizer.text.cursor.CursorDevicer.CursorHelper;
import com.maizer.text.layout.FormatLayout;
import com.maizer.text.layout.TextAreaLayout;
import com.maizer.text.view.TextAreaView;
import com.maizer.text.view.TextAreaView.OnImeActionListener;

import android.R;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputMethodManager;

/**
 * AbsInputConnector ,我们需要添加负值标记,请注意
 * 
 * @author Maizer/麦泽
 *
 */
public class TextInputConnector extends BaseInputConnection {
	@Override
	public void closeConnection() {
		super.closeConnection();
		ourView = null;
		mEditText = null;
		mBatchString = null;
		Log.e(TAG, "close");
	}

	@Override
	public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
		// TODO Auto-generated method stub
		return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
	}

	@Override
	public CharSequence getTextBeforeCursor(int n, int flags) {
		return null;
	}

	@Override
	public CharSequence getTextAfterCursor(int n, int flags) {
		return null;
	}

	public Editable getEditable() {
		if (noTrick) {
			return mEditText;
		}
		return TRICK_TEXT;
	}

	@Override
	public CharSequence getSelectedText(int flags) {
		return null;
	}

	@Override
	public boolean clearMetaKeyStates(int states) {
		return super.clearMetaKeyStates(states);
	}

	private static final String TAG = TextInputConnector.class.getCanonicalName();

	/**
	 * 标识
	 */
	public static final int INPUT_EVENT = -100;
	public static final int INPUT_MOVE_DOWN = -1;
	public static final int INPUT_MOVE_UP = -2;
	public static final int INPUT_MOVE_LEFT = -3;
	public static final int INPUT_MOVE_RIGHT = -4;

	public static final int INPUT_MOVE_SHIF_DOWN = -5;
	public static final int INPUT_MOVE_SHIF_UP = -6;
	public static final int INPUT_MOVE_SHIF_LEFT = -7;
	public static final int INPUT_MOVE_SHIF_RIGHT = -8;

	public static final int INPUT_COPY = -9;
	public static final int INPUT_COPYURL = -10;
	public static final int INPUT_SWITHINPUT = -11;
	public static final int INPUT_PASTE = -12;
	public static final int INPUT_ENTER = -13;

	private static final SpannableStringBuilder TRICK_TEXT = new SpannableStringBuilder("     ");
	private static final ExtractedText EXTRACTED_TEXt = new ExtractedText();
	private Editable mEditText;
	private TextAreaView ourView;
	private StringBuilder mBatchString;
	/**
	 * 锁定shift键
	 */
	private boolean isShift;
	private boolean isBatch;
	/**
	 * 对于某些带移标的输入法,我们需要伪装来获取移动标记,仅限垂直视图,强烈建议大文本使用Trick,默认开启
	 */
	private boolean noTrick;

	/**
	 * 抽象的输入法连接类,
	 * 
	 * @param editor
	 */

	public TextInputConnector(Editable editor, TextAreaView view) {
		super(view, true);
		if (view == null || editor == null) {
			throw new NullPointerException();
		}
		mEditText = editor;
		ourView = view;
	}

	/**
	 * @hide
	 */
	public void setEditable(Editable editable) {
		mEditText = editable;
	}

	@Override
	public int getCursorCapsMode(int reqModes) {
		int end = getSelectEnd();
		return TextUtils.getCapsMode(getText(), end == -1 ? 0 : end, reqModes);
	}

	@Override
	public boolean deleteSurroundingText(int beforeLength, int afterLength) {
		if (beforeLength > afterLength) {
			int len = beforeLength - afterLength;
			if (mBatchString != null && mBatchString.length() > len) {
				int length = mBatchString.length();
				mBatchString.delete(length - len, length);
				return true;
			}
			deleteSelect(getSelectStart(), getSelectEnd());
		}
		return true;
	}

	/**
	 * need trick input keyboard,if password mode,using suggest
	 * 
	 * @param t
	 */
	public void setTrick(boolean t) {
		noTrick = t;
	}

	@Override
	public boolean setComposingText(CharSequence text, int newCursorPosition) {
		return super.setComposingText(text, newCursorPosition);
	}

	@Override
	public boolean setComposingRegion(int start, int end) {
		return super.setComposingRegion(start, end);
	}

	// public Editable getEditable() {
	// return mEditText;
	// }

	@Override
	public boolean finishComposingText() {
		setShift(false);
		int start = getSelectStart();
		int end = getSelectEnd();
		if (start != end) {
			if (start > end) {
				setSelectToEditor(start);
			} else {
				setSelectToEditor(end);
			}
		}
		return super.finishComposingText();
	}

	@Override
	public boolean commitText(CharSequence text, int newCursorPosition) {
		if (isBatch && mBatchString != null) {
			mBatchString.append(text);
			return true;
		}
		// setComposingSpans(mEditText);
		// super.commitText(text, newCursorPosition);
		commitText(getSelectStart(), getSelectEnd(), text);
		return true;
	}

	@Override
	public boolean commitCompletion(CompletionInfo text) {
		Log.e(TAG, "commoot");
		return false;
	}

	@Override
	public boolean commitCorrection(CorrectionInfo correctionInfo) {
		Log.e(TAG, "commootCorre");
		return true;
	}

	public Editable getText() {
		return mEditText;
	}

	protected boolean isShift() {
		return isShift;
	}

	protected void setShift(boolean s) {
		isShift = s;
	}

	protected int getSelectStart() {
		return Selection.getSelectionStart(getText());
	}

	protected int getSelectEnd() {
		return Selection.getSelectionEnd(getText());
	}

	protected int length() {
		return getText().length();
	}

	private void delete(int st, int en) {
		getText().delete(st, en);
	}

	protected void insert(int start, CharSequence text) {
		Editable edit = getText();
		if (start == -1) {
			edit.insert(0, text);
			if (edit.length() > 0 && text != null) {
				Selection.setSelection(edit, text.length());
			}
		} else {
			edit.insert(start, text);
		}
	}

	protected void replace(int st, int en, CharSequence source, int start, int end) {
		getText().replace(st, en, source, start, end);
	}

	protected void setSelectToEditor(int start, int end) {
		Selection.setSelection(getText(), start, end);
	}

	protected void setSelectToEditor(int start) {
		if (isShift() && start >= 0) {
			Selection.setSelection(getText(), getSelectStart(), start);
		} else {
			Selection.setSelection(getText(), start);
		}
	}

	protected void getCharsforSelect(int start, int end, char[] dest, int destoff) {
		getText().getChars(start, end, dest, destoff);
	}

	protected char getChar(int index) {
		return getText().charAt(index);
	}

	/**
	 * mask our text
	 */
	@Override
	public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
		if (noTrick) {
			EXTRACTED_TEXt.text = getText();
			EXTRACTED_TEXt.partialStartOffset = 0;
			EXTRACTED_TEXt.partialEndOffset = getText().length();
			EXTRACTED_TEXt.selectionStart = getSelectStart();
			EXTRACTED_TEXt.selectionEnd = getSelectEnd();
			return EXTRACTED_TEXt;
		}
		Log.e(TAG,
				"" + request.flags + "  " + request.hintMaxChars + "  " + request.hintMaxLines + "  " + request.token);
		int trickLength = TRICK_TEXT.length();
		int realLength = length();
		int start = getSelectStart();
		int end = getSelectEnd();
		int m = -1;
		if (start > end) {
			m = start;
			start = end;
			end = m;
		}
		if (start != end) {
			if (start != 0) {
				start = trickLength >> 1;
			}
			if (end != realLength) {
				end = (trickLength >> 1) + 1;
			} else {
				end = trickLength;
			}
		} else {
			if (start <= 0) {
				start = end = 0;
			} else if (start == realLength) {
				start = end = trickLength;
			} else {
				start = end = trickLength >> 1;
			}
		}
		Log.e(TAG, "GETTrick>" + start + "  " + end);
		Selection.setSelection(TRICK_TEXT, start, end);
		EXTRACTED_TEXt.text = TRICK_TEXT;
		EXTRACTED_TEXt.partialStartOffset = 0;
		EXTRACTED_TEXt.partialEndOffset = trickLength;
		EXTRACTED_TEXt.selectionStart = start;
		EXTRACTED_TEXt.selectionEnd = end;
		return EXTRACTED_TEXt;
	}

	/**
	 * 我们自己不能使用这个方达操作我们的Editable,否则将直接导致误差,如果你不需要骗取移动标识 请覆写该方法,强烈建议大文本使用Trick
	 */
	@Override
	public boolean setSelection(int start, int end) {

		if (noTrick) {
			setSelectToEditor(start, end);
			return true;
		}
		int oldStart = Selection.getSelectionStart(TRICK_TEXT);
		int oldEnd = Selection.getSelectionEnd(TRICK_TEXT);

		int mStart = getSelectStart();
		int mEnd = getSelectEnd();

		if (start == end) {
			if (start < oldStart || start < oldEnd) {
				if (start == 0) {
					setSelectToEditor(0);
				} else {
					sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
				}
			} else if (start > oldStart || start > oldEnd) {
				if (start == 0) {
					setSelectToEditor(0);
				} else if (start == TRICK_TEXT.length() && mStart == mEnd && mEnd != length()) {
					setSelectToEditor(length());
				} else {
					sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
				}
			} else {
				if (start == 0) {
					setSelectToEditor(0);
				} else if (start == TRICK_TEXT.length()) {
					setSelectToEditor(length());
				}
			}
		} else {
			if (oldStart == end) {
				if (start == 0) {
					setSelectToEditor(0, mStart);
				} else {
					if (mEnd > mStart) {
						mEnd = mStart;
					}
					setSelectToEditor(mStart, mEnd > 1 ? mEnd - 1 : 0);
				}
				return true;
			}
			if (oldEnd == start) {
				if (end == 0) {
					setSelectToEditor(mStart, 0);
				} else if (end == TRICK_TEXT.length()) {
					setSelectToEditor(mStart, length());
				} else if (end < oldEnd) {
					setSelectToEditor(mStart, --mEnd);
				} else {
					if (mEnd < mStart) {
						mEnd = mStart;
					}
					setSelectToEditor(mStart, ++mEnd);
				}
				return true;
			}
			if (end < oldEnd) {
				if (end == 0) {
					setSelectToEditor(mStart, 0);
				} else {
					if (start != end) {
						setShift(true);
					}
					sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
				}
			} else if (end > oldEnd) {
				if (end == TRICK_TEXT.length()) {
					setSelectToEditor(mStart, length());
				} else {
					if (start != end) {
						setShift(true);
					}
					sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
				}
			} else {
				if (start == 0) {
					setSelectToEditor(0, length());
				} else if (start < oldStart) {
					setSelectToEditor(--mStart, length());
				} else if (start > oldStart) {
					setSelectToEditor(++mStart, length());
				}
			}
		}
		return true;
	}

	private void copy() {
		ClipboardManager cm = (ClipboardManager) ourView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
		if (cm == null) {
			Log.e(TAG, "System Error : clipboardManager die!");
			return;
		}
		CharSequence text = getText();
		int start = Selection.getSelectionStart(text);
		int end = Selection.getSelectionEnd(text);
		if (start != end) {
			if (start > end) {
				int d = start;
				start = end;
				end = d;
			}
			Intent intent = new Intent();
			intent.setType(ClipDescription.MIMETYPE_TEXT_INTENT);
			ClipData data = ClipData.newPlainText("", text.subSequence(start, end));
			cm.setPrimaryClip(data);
		}
	}

	public boolean performEditorAction(int actionCode) {
		return super.performEditorAction(actionCode);
	}

	@Override
	public boolean performContextMenuAction(int id) {
		switch (id) {
		case R.id.selectAll:
			setSelectToEditor(0, length());
			break;
		case R.id.cut:
			copy();
			deleteSelect(getSelectStart(), getSelectEnd());
			break;
		case R.id.startSelectingText:
			setShift(true);
			break;
		case R.id.stopSelectingText:
			setShift(false);
			break;
		case R.id.copy:
			copy();
			break;
		case R.id.paste:
			ClipboardManager cm = (ClipboardManager) ourView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
			if (cm == null) {
				Log.e(TAG, "System Error : clipboardManager die!");
				return true;
			}
			commitText(getSelectStart(), getSelectEnd(), "");
			ClipData data = cm.getPrimaryClip();
			if (data == null) {
				return true;
			}
			if (data.getItemCount() > 0) {
				CharSequence clipText = data.getItemAt(0).getText();
				int end = Selection.getSelectionEnd(getText());
				insert(end, clipText);
			}
			break;
		case R.id.copyUrl:
			send(INPUT_COPYURL);
			break;
		case R.id.switchInputMethod:
			break;
		default:
			return false;
		}
		return true;
	}

	private boolean send(int code) {
		CursorHelper mCursorMove = ourView.getCursorHelper();
		CursorDevicer mCursorDevicer = ourView.getCursorDivicer();
		OnImeActionListener mActionListener = ourView.getImeActionListener();
		int mImeOptions = ourView.getImeOptions();
		boolean handle = false;
		switch (code) {
		case TextInputConnector.INPUT_ENTER:
			if (TextAreaView.isShareCursor(mCursorDevicer)) {
				int flag = mImeOptions & EditorInfo.IME_MASK_ACTION;
				switch (flag) {
				case EditorInfo.IME_ACTION_NEXT:
					View view = ourView.focusSearch(View.FOCUS_DOWN);
					if (view != null) {
						ourView.clearFocus();
						view.requestFocus();
						return true;
					}
					break;
				case EditorInfo.IME_ACTION_PREVIOUS:
					view = ourView.focusSearch(View.FOCUS_UP);
					if (view != null) {
						view.requestFocus();
						return true;
					}
					break;
				case EditorInfo.IME_ACTION_DONE:
					if (mActionListener != null) {
						hideInput();
						mActionListener.onDoneAction(ourView);
					}
					if (ourView.isSingleLine()) {
						hideInput();
						return true;
					}
					return false;
				case EditorInfo.IME_ACTION_GO:
					if (mActionListener != null) {
						hideInput();
						mActionListener.onGoAction(ourView);
						return true;
					}
					if (ourView.isSingleLine()) {
						hideInput();
						return true;
					}
					return false;
				case EditorInfo.IME_ACTION_SEARCH:
					if (mActionListener != null) {
						hideInput();
						mActionListener.onSearchAction(ourView);
						return true;
					}
					if (ourView.isSingleLine()) {
						hideInput();
						return true;
					}
					return false;
				case EditorInfo.IME_ACTION_SEND:
					if (mActionListener != null) {
						hideInput();
						mActionListener.onSendAction(ourView);
						return true;
					}
					if (ourView.isSingleLine()) {
						hideInput();
						return true;
					}
					return false;
				}
			}
			return false;
		case TextInputConnector.INPUT_COPY:
			return true;
		case TextInputConnector.INPUT_COPYURL:
			return true;
		case TextInputConnector.INPUT_SWITHINPUT:
			return true;
		case TextInputConnector.INPUT_PASTE:
			return true;
		default:
			if (mCursorMove == null) {
				return false;
			}

		case TextInputConnector.INPUT_MOVE_SHIF_DOWN:
			if (ourView.isTextSelectable()) {
				handle = mCursorMove.moveDown(mCursorDevicer, ourView, true);
				break;
			}
		case TextInputConnector.INPUT_MOVE_DOWN:
			handle = mCursorMove.moveDown(mCursorDevicer, ourView, false);
			break;
		case TextInputConnector.INPUT_MOVE_SHIF_UP:
			if (ourView.isTextSelectable()) {
				handle = mCursorMove.moveUp(mCursorDevicer, ourView, true);
				break;
			}
		case TextInputConnector.INPUT_MOVE_UP:
			handle = mCursorMove.moveUp(mCursorDevicer, ourView, false);
			break;
		case TextInputConnector.INPUT_MOVE_SHIF_LEFT:
			if (ourView.isTextSelectable()) {
				handle = mCursorMove.moveLeft(mCursorDevicer, ourView, true);
				break;
			}
		case TextInputConnector.INPUT_MOVE_LEFT:
			handle = mCursorMove.moveLeft(mCursorDevicer, ourView, false);
			break;
		case TextInputConnector.INPUT_MOVE_SHIF_RIGHT:
			if (ourView.isTextSelectable()) {
				handle = mCursorMove.moveRight(mCursorDevicer, ourView, true);
				break;
			}
		case TextInputConnector.INPUT_MOVE_RIGHT:
			handle = mCursorMove.moveRight(mCursorDevicer, ourView, false);
			break;
		}
		if (!handle) {
			ourView.invalidate();
		}
		ourView.updateCursorToVisibileScreen();
		return handle;
		// return ourView.onKeyPreIme(code, new KeyEvent(INPUT_EVENT, code));
	}

	private void hideInput() {
		InputMethodManager imm = (InputMethodManager) ourView.getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(ourView.getWindowToken(), 0);
	}

	@Override
	public boolean beginBatchEdit() {
		if (hasBatchBuffer()) {
			if (mBatchString == null) {
				mBatchString = new StringBuilder();
			}
		}
		return isBatch = true;
	}

	protected boolean hasBatchBuffer() {
		return true;
	}

	protected boolean isRepeatBatchString() {
		return false;
	}

	@Override
	public boolean endBatchEdit() {
		if (mBatchString != null) {
			if (mBatchString.length() > 0) {
				commitText(getSelectStart(), getSelectEnd(), mBatchString);
				mBatchString.setLength(0);
			}
			if (!isRepeatBatchString()) {
				mBatchString = null;
			}
		}
		isBatch = false;
		return true;
	}

	@Override
	public boolean sendKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int key = event.getKeyCode();
		switch (action) {
		case KeyEvent.ACTION_DOWN:
			switch (key) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				setShift(true);
				return true;
			}
			if (onKeyDown(key)) {
				return true;
			}
			return ourView.onKeyDown(key, event);
		case KeyEvent.ACTION_UP:
			switch (key) {
			case KeyEvent.KEYCODE_SHIFT_LEFT:
			case KeyEvent.KEYCODE_SHIFT_RIGHT:
				setShift(false);
				return true;
			}
			if (onKeyUp(key)) {
				return true;
			}
			break;
		case KeyEvent.ACTION_MULTIPLE:
			String keyChar = event.getCharacters();
			if (keyChar != null) {
				commitText(keyChar, getSelectEnd());
				return true;
			}
			break;
		}
		return false;
	}

	protected boolean onKeyDown(int key) {
		return false;
	}

	protected boolean onKeyUp(int key) {
		int selectStart = getSelectStart();
		int selectEnd = getSelectEnd();
		switch (key) {
		case KeyEvent.KEYCODE_DEL:
			deleteSelect(selectStart, selectEnd);
			break;
		case KeyEvent.KEYCODE_ENTER:
			if (send(INPUT_ENTER)) {
				return true;
			}
			commitText(selectStart, selectEnd, "\n");
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (isShift()) {
				send(INPUT_MOVE_SHIF_RIGHT);
			} else {
				send(INPUT_MOVE_RIGHT);
			}
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (isShift()) {
				send(INPUT_MOVE_SHIF_LEFT);
			} else {
				send(INPUT_MOVE_LEFT);
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (isShift()) {
				send(INPUT_MOVE_SHIF_DOWN);
			} else {
				send(INPUT_MOVE_DOWN);
			}
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
			if (isShift()) {
				send(INPUT_MOVE_SHIF_UP);
			} else {
				send(INPUT_MOVE_UP);
			}
			break;
		case KeyEvent.KEYCODE_MOVE_END:
			setSelectToEditor(length());
			break;
		case KeyEvent.KEYCODE_MOVE_HOME:
			setSelectToEditor(0);
			break;
		default:
			String code = getKeyCodeToUnicodeString(key);
			if (code == null) {
				return false;
			}
			commitText(selectStart, selectEnd, code);
		}
		return true;
	}

	/**
	 * 务必将select起点还原之后,再来修改Text
	 * 
	 * @param selectStart
	 * @param selectEnd
	 * @param text
	 */
	protected void commitText(int selectStart, int selectEnd, CharSequence text) {
		if (selectStart == selectEnd) {
			insert(selectEnd, text);
		} else if (selectEnd < selectStart) {
			setSelectToEditor(selectEnd);
			replace(selectEnd, selectStart, text, 0, text.length());
			setSelectToEditor(selectEnd + text.length());
		} else {
			setSelectToEditor(selectStart);
			replace(selectStart, selectEnd, text, 0, text.length());
			setSelectToEditor(selectStart + text.length());
		}
	}

	/**
	 * @deprecated 为了更加精准的游标处理,已经废弃
	 * @see FormatLayout#moveLeft(com.maizer.text.cursor.CursorDevicer,
	 *      com.maizer.textview.TextScroller, boolean)
	 */
	@Deprecated
	protected void moveLeft(int selectStart, int selectEnd) {
		if (isShift()) {
			if (selectEnd > 0) {
				if (selectEnd > 1) {
					char c = getChar(selectEnd - 2);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(getText(), selectEnd - 2));
						if (count > 1) {
							setSelectToEditor(selectStart, selectEnd - count);
							return;
						}
					}
				}
				setSelectToEditor(selectStart, selectEnd - 1);
			}
		} else if (selectEnd != selectStart) {
			setSelectToEditor(selectStart);
		} else if (selectEnd > 0) {
			if (selectEnd > 1) {
				char c = getChar(selectEnd - 2);
				if (TextAreaLayout.isEmojiEnable(c)) {
					int count = Character.charCount(Character.codePointAt(getText(), selectEnd - 2));
					if (count > 1) {
						setSelectToEditor(selectEnd - count);
						return;
					}
				}
			}
			setSelectToEditor(selectEnd - 1);
		}
	}

	@Deprecated
	protected void moveRight(int selectStart, int selectEnd) {
		if (isShift()) {
			if (selectEnd < length()) {
				if (selectEnd < length() - 1) {
					char c = getChar(selectEnd);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(getText(), selectEnd));
						if (count > 1) {
							setSelectToEditor(selectStart, selectEnd + count);
							return;
						}
					}
				}
				setSelectToEditor(selectStart, selectEnd + 1);
			}
		} else if (selectEnd != selectStart) {
			setSelectToEditor(selectEnd);
		} else {
			if (selectEnd < length()) {
				if (selectEnd < length() - 1) {
					char c = getChar(selectEnd);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(getText(), selectEnd));
						if (count > 1) {
							setSelectToEditor(selectEnd + count);
							return;
						}
					}
				}
				setSelectToEditor(selectEnd + 1);
			}
		}
	}

	/**
	 * 务必将select起点更正之后,再来修改Text
	 * 
	 */
	protected void deleteSelect(int selectStart, int selectEnd) {

		if (selectStart == selectEnd) {
			if (selectEnd > 0) {
				if (selectEnd > 1) {
					char c = getChar(selectEnd - 2);
					if (TextAreaLayout.isEmojiEnable(c)) {
						int count = Character.charCount(Character.codePointAt(getText(), selectEnd - 2));
						if (count > 1) {
							delete(selectEnd - count, selectEnd);
							return;
						}
					}
				}
				delete(selectEnd - 1, selectEnd);
			}
		} else {
			if (selectStart < selectEnd) {
				setSelectToEditor(selectStart);
				delete(selectStart, selectEnd);
			} else {
				setSelectToEditor(selectEnd);
				delete(selectEnd, selectStart);
			}
		}
	}

	@Override
	public boolean reportFullscreenMode(boolean enabled) {
		Log.e(TAG, "refull");
		return false;
	}

	@Override
	public boolean performPrivateCommand(String action, Bundle data) {
		Log.e(TAG, "performprivatecom");
		return true;
	}

	@SuppressLint("Override")
	public boolean requestCursorUpdates(int cursorUpdateMode) {
		Log.e(TAG, "requestCursorUpdates");
		return false;
	}

	public int getKeyCodeToUnicode(int key) {
		if (KeyEvent.KEYCODE_0 <= key && KeyEvent.KEYCODE_9 >= key) {
			return ('0' + (key - KeyEvent.KEYCODE_0));
		} else if (KeyEvent.KEYCODE_A <= key && KeyEvent.KEYCODE_Z >= key) {
			if (isShift()) {
				return ('A' + (key - KeyEvent.KEYCODE_A));
			}
			return ('a' + (key - KeyEvent.KEYCODE_A));
		} else if (KeyEvent.KEYCODE_STAR == key) {
			return '*';
		} else if (KeyEvent.KEYCODE_POUND == key) {
			return '#';
		} else if (KeyEvent.KEYCODE_COMMA == key) {
			return ',';
		} else if (KeyEvent.KEYCODE_PERIOD == key) {
			return '.';
		} else if (KeyEvent.KEYCODE_SPACE == key) {
			return ' ';
		} else if (KeyEvent.KEYCODE_TAB == key) {
			return '\t';
		}
		return 0;
	}

	public String getKeyCodeToUnicodeString(int key) {
		if (KeyEvent.KEYCODE_11 == key) {
			return "11";
		} else if (KeyEvent.KEYCODE_12 == key) {
			return "12";
		}
		int code = getKeyCodeToUnicode(key);
		if (code == 0) {
			return null;
		}
		return (char) code + "";
	}

}
