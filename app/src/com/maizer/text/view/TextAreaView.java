package com.maizer.text.view;

import java.io.IOException;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import com.maizer.BuildConfig;
import com.maizer.R;
import com.maizer.text.cursor.CursorDevicer;
import com.maizer.text.cursor.CursorDevicer.CursorHelper;
import com.maizer.text.cursor.DefaultCursor;
import com.maizer.text.factory.CursorFactory;
import com.maizer.text.layout.SingleMiddleLayout;
import com.maizer.text.layout.DefaultCursorHelper;
import com.maizer.text.layout.FormatLayout;
import com.maizer.text.layout.LayoutAttrubute;
import com.maizer.text.layout.TextAreaLayout;
import com.maizer.text.layout.TextAreaPaint;
import com.maizer.text.layout.TextAreaScroller;
import com.maizer.text.measure.MeasureAttrubute;
import com.maizer.text.layout.TextAreaLayout.Alignment;
import com.maizer.text.util.TextInputConnector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.icu.math.BigDecimal;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.text.method.TimeKeyListener;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 对大文本进行支持的TextAreaView,可能并不支持原始TextView的部分特性
 * 
 * @node: 任何大文本,勿对外赋值传输,因为这可能造成过载导致事务交接失败,从而使inputconnection失效
 * 
 * @author Maizer/麦泽
 *
 */
public class TextAreaView extends View implements TextAreaScroller, OnPreDrawListener {

	@Override
	public void postRunnable(Runnable run, long delayMillis) {
		postDelayed(run, delayMillis);
	}

	@Override
	public void onStartTemporaryDetach() {
		super.onStartTemporaryDetach();
	}

	private void registerForPreDraw() {
		if (isEditable && imm != null) {
			getViewTreeObserver().addOnPreDrawListener(this);
		}
	}

	private void unregisterForPreDraw() {
		getViewTreeObserver().removeOnPreDrawListener(this);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
	}

	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (mCursorDevicer != null) {
			if (gainFocus && onCheckIsTextEditor()) {
				mCursorDevicer.setVisibility(true);
			} else {
				if (SHARE_CURSOR == mCursorDevicer) {
					mCursorDevicer = null;
				} else {
					mCursorDevicer.setVisibility(false);
				}
			}
		} else {
			if (gainFocus) {
				if (SHARE_CURSOR != null && SHARE_CURSOR.checkView(this)) {
					SHARE_CURSOR.setDefaultMoveLevel(mReviseX, mReviseY);
					if (!inTouching()) {
						CursorHelper ch = getCursorHelper();
						ch.requestMoveCursor(SHARE_CURSOR, this);
					}
					mCursorDevicer = SHARE_CURSOR;
				}
			}
		}
	}

	public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
		return super.requestFocus(direction, previouslyFocusedRect);
	}

	public void clearFocus() {
		super.clearFocus();
		if (SHARE_CURSOR == mCursorDevicer) {
			mCursorDevicer = null;
		}
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		CursorDevicer cd = getCursorDivicer();
		if (cd != null) {
			if (visibility == View.VISIBLE && onCheckIsTextEditor()) {
				cd.setVisibility(true);
			} else {
				cd.setVisibility(false);
			}
		}
	}

	@Override
	public boolean isInEditMode() {
		return isEditable && isEnabled();
	}

	public boolean onKeyPreIme(int keycode, KeyEvent event) {
		int action = event.getAction();
		if (action == TextInputConnector.INPUT_EVENT) {
			CursorHelper mCursorMove = getCursorHelper();
			boolean handle = false;
			switch (keycode) {
			case TextInputConnector.INPUT_ENTER:
				if (SHARE_CURSOR == mCursorDevicer) {
					int flag = mPrivateImeOptions & EditorInfo.IME_MASK_ACTION;
					switch (flag) {
					case EditorInfo.IME_ACTION_NEXT:
						View view = focusSearch(View.FOCUS_DOWN);
						if (view != null) {
							view.requestFocus();
							return true;
						}
						break;
					case EditorInfo.IME_ACTION_PREVIOUS:
						view = focusSearch(View.FOCUS_UP);
						if (view != null) {
							view.requestFocus();
							return true;
						}
						break;
					case EditorInfo.IME_ACTION_DONE:
						if (mActionListener != null) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							mActionListener.onDoneAction(this);
						}
						if (isSingleLine()) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							return true;
						}
						return false;
					case EditorInfo.IME_ACTION_GO:
						if (mActionListener != null) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							mActionListener.onGoAction(this);
							return true;
						}
						if (isSingleLine()) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							return true;
						}
						return false;
					case EditorInfo.IME_ACTION_SEARCH:
						if (mActionListener != null) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							mActionListener.onSearchAction(this);
							return true;
						}
						if (isSingleLine()) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							return true;
						}
						return false;
					case EditorInfo.IME_ACTION_SEND:
						if (mActionListener != null) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
							mActionListener.onSendAction(this);
							return true;
						}
						if (isSingleLine()) {
							if (imm != null) {
								imm.hideSoftInputFromWindow(getWindowToken(), 0);
							}
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
				if (isTextSeletable) {
					handle = mCursorMove.moveDown(mCursorDevicer, this, true);
					break;
				}
			case TextInputConnector.INPUT_MOVE_DOWN:
				handle = mCursorMove.moveDown(mCursorDevicer, this, false);
				break;
			case TextInputConnector.INPUT_MOVE_SHIF_UP:
				if (isTextSeletable) {
					handle = mCursorMove.moveUp(mCursorDevicer, this, true);
					break;
				}
			case TextInputConnector.INPUT_MOVE_UP:
				handle = mCursorMove.moveUp(mCursorDevicer, this, false);
				break;
			case TextInputConnector.INPUT_MOVE_SHIF_LEFT:
				if (isTextSeletable) {
					handle = mCursorMove.moveLeft(mCursorDevicer, this, true);
					break;
				}
			case TextInputConnector.INPUT_MOVE_LEFT:
				handle = mCursorMove.moveLeft(mCursorDevicer, this, false);
				break;
			case TextInputConnector.INPUT_MOVE_SHIF_RIGHT:
				if (isTextSeletable) {
					handle = mCursorMove.moveRight(mCursorDevicer, this, true);
					break;
				}
			case TextInputConnector.INPUT_MOVE_RIGHT:
				handle = mCursorMove.moveRight(mCursorDevicer, this, false);
				break;
			}
			if (!handle) {
				invalidate();
			}
			updateCursorToVisibileScreen();
			return true;
		}
		return super.onKeyPreIme(keycode, event);
	}

	@Override
	public int getTextScrollY() {
		return mScrollY;
	}

	@Override
	public int getTextScrollX() {
		return mScrollX;
	}

	protected void setTextScrollY(int value) {
		// setScrollY(value);
		if (mScrollY != value) {
			mScrollY = value;
			invalidate();
		}
	}

	protected void setTextScrollX(int value) {
		// setScrollX(value);
		if (mScrollX != value) {
			mScrollX = value;
			invalidate();
		}
	}

	@Override
	public void requestTextScrollBy(int x, int y) {
		if (!inTouching()) {
			// scrollBy(x, y);
			mScrollX += x;
			mScrollY += y;
			oldScrollX -= x;
			oldScrollY -= y;
			invalidate();
		}
	}

	public final boolean inTouching() {
		return isTouching;
	}

	public int getSelectionStart() {
		CharSequence text = getText();
		if (text != null) {
			int start = Selection.getSelectionStart(text);
			return start == -1 ? 0 : start;
		}
		return 0;
	}

	public int getSelectionEnd() {
		CharSequence text = getText();
		if (text != null) {
			int start = Selection.getSelectionEnd(text);
			return start == -1 ? 0 : start;
		}
		return 0;
	}

	protected boolean shouldAdvanceFocusOnEnter() {
		return isSingleLine();
	}

	public boolean onCheckIsTextEditor() {
		return isEditable && isEnabled();
	}

	private boolean isMultilineInputType(int type) {
		return (type
				& (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE)) == (EditorInfo.TYPE_CLASS_TEXT
						| EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
	}

	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		if (onCheckIsTextEditor() && isEnabled()) {
			if (mInfo != null) {
				outAttrs.imeOptions = mInfo.imeOptions;
				outAttrs.privateImeOptions = mInfo.privateImeOptions;
				outAttrs.actionLabel = mInfo.actionLabel;
				outAttrs.actionId = mInfo.actionId;
				outAttrs.extras = mInfo.extras;
				outAttrs.inputType = mInfo.inputType;
			} else {
				outAttrs.imeOptions = EditorInfo.IME_NULL;
			}
			if (focusSearch(View.FOCUS_DOWN) != null) {
				outAttrs.imeOptions |= EditorInfo.IME_FLAG_NAVIGATE_NEXT;
			}
			if (focusSearch(View.FOCUS_UP) != null) {
				outAttrs.imeOptions |= EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS;
			}
			if ((outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_UNSPECIFIED) {
				if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0) {
					// An action has not been set, but the enter key will move
					// to
					// the next focus, so set the action to that.
					outAttrs.imeOptions |= EditorInfo.IME_ACTION_NEXT;
				} else {
					// An action has not been set, and there is no focus to move
					// to, so let's just supply a "done" action.
					outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
				}
				if (!shouldAdvanceFocusOnEnter()) {
					outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_ENTER_ACTION;
				}
			}
			if (isMultilineInputType(outAttrs.inputType)) {
				// Multi-line text editors should always show an enter key.
				outAttrs.imeOptions |= EditorInfo.IME_FLAG_NO_ENTER_ACTION;
			}
			mPrivateImeOptions = outAttrs.imeOptions;
			if (mText instanceof Editable) {
				InputConnection ic = getInputConnection((Editable) mText);
				outAttrs.hintText = "";// 默认忽略
				outAttrs.initialSelStart = getSelectionStart();
				outAttrs.initialSelEnd = getSelectionEnd();
				outAttrs.initialCapsMode = ic.getCursorCapsMode(mInfo.inputType);
				return ic;
			}
		}
		return null;
	}

	public CharSequence getHint() {
		return mHint;
	}

	public void setHint(CharSequence text) {
		if (mHint != text) {
			mHint = text;
		}
	}

	public TextAreaPaint getHintPaint() {
		if (mHintPaint == null) {
			mHintPaint = new TextAreaPaint(Paint.ANTI_ALIAS_FLAG);
			mHintPaint.setColor(Color.GRAY);
		}
		return mHintPaint;
	}

	@Override
	public boolean checkInputConnectionProxy(View view) {
		return isEditable && isEnabled() && view == this;
	}

	protected InputConnection getInputConnection(Editable text) {
		return new TextInputConnector(text, this);
	}

	private static final String TAG = TextAreaView.class.getSimpleName();

	private static boolean SHARE_ENABLE = true;
	private static CursorDevicer SHARE_CURSOR;

	private Editable.Factory mEditableFactory = Editable.Factory.getInstance();
	private CursorFactory mCursorFactory = CursorFactory.getInstance();

	private InputMethodManager imm;
	private Drawables mDrawables;

	private TextAreaPaint mPaint;
	private TextAreaPaint mHintPaint;

	private TextAreaLayout mLayout;
	private TextAreaLayout mHintLayout;

	private CharSequence mText;
	private CharSequence mHint;
	/**
	 * 任意修改layout属性,请执行相应的
	 * {@link TextAreaLayout#restartLayout(TextAreaScroller)},
	 * {@link TextAreaLayout#restartLayoutSize(int, int, int, TextAreaScroller)}
	 * ,
	 * {@link TextAreaLayout#restartMeasure(int, TextAreaScroller, CursorDevicer)}
	 * 方法
	 */
	private LayoutAttrubute mAttr;
	private LayoutAttrubute mHintAttr;

	private EditorInfo mInfo = new EditorInfo();
	private int mPrivateImeOptions;

	private Rect mTempRect;
	private CursorDevicer mCursorDevicer;
	private CursorHelper mCursorHelper;

	private float maxFriction = 1f;
	private int mGravity;
	private int mReviseY;
	private int mReviseX;

	private boolean isTouching;
	private boolean isEditable = true;
	private boolean isTextSeletable = true;
	private boolean receiveOutTouch;

	private int nextFocusBottom;
	private int nextFocusTop;
	private int nextFocusLeft;
	private int nextFocusRight;

	private InputFilter[] mInputFileters;
	private InputFilter[] mFilters;
	private KeyListener mKeyListener;
	private OnImeActionListener mActionListener;

	private int mScrollX;
	private int mScrollY;
	private int oldScrollX;
	private int oldScrollY;
	private float mDownX;
	private float mDownY;

	public TextAreaView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	Toast toast;

	public TextAreaView(Context context, AttributeSet attrs) {
		super(context, attrs);
		toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
		initBaseInfo();

		CharSequence text = null;
		String fontFamily = null;
		String actionLabel = null;
		int actionId = 0;
		int textColorHighlight = -1;
		int textStyle = 0;
		int inputType = EditorInfo.TYPE_CLASS_TEXT;
		int cursorColor = -1, cursorMinWidth = -1;
		float globalWidth = -1, globalHeight = -1;
		float leftWidth = -1, leftHeight = -1, rightWidth = -1, rightHeight = -1, topWidth = -1, topHeight = -1,
				bottomWidth = -1, bottomHeight = -1;
		int paddingLeft = 0, paddingRight = 0, paddingTop = 0, paddingBottom = 0;
		boolean cursorVisbile = true;
		boolean scaleLevel = false;
		boolean isEditable = true;

		Typeface typeface = null;
		Drawable cursorDraw = null;
		Drawable textColorHighlightDraw = null;
		Drawable leftDraw = null, rightDraw = null, topDraw = null, bottomDraw = null;

		TypedArray mTypeArray = context.obtainStyledAttributes(attrs, R.styleable.TextAreaView);
		int count = mTypeArray.getIndexCount();
		for (int i = 0; i < count; i++) {
			int attr = mTypeArray.getIndex(i);
			switch (attr) {
			case R.styleable.TextAreaView_textSize:
				int textsize = mTypeArray.getDimensionPixelSize(attr, 12);
				setTextSize(TypedValue.COMPLEX_UNIT_PX, textsize);
				break;
			case R.styleable.TextAreaView_capitalize:
				switch (mTypeArray.getInt(attr, 0)) {
				case 1:
					inputType |= EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES;
					break;
				case 2:
					inputType |= EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS;
					break;
				case 3:
					inputType |= EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS;
					break;
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_Width:
				try {
					globalWidth = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (globalWidth == -1) {
					globalWidth = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_Height:
				try {
					globalHeight = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (globalHeight == -1) {
					globalHeight = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_Bottom:
				bottomDraw = mTypeArray.getDrawable(attr);
				break;
			case R.styleable.TextAreaView_compoundDrawable_BottomHeight:
				try {
					bottomHeight = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (bottomHeight == -1) {
					bottomHeight = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_BottomWidth:
				try {
					bottomWidth = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (bottomWidth == -1) {
					bottomWidth = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_Left:
				leftDraw = mTypeArray.getDrawable(attr);
				break;
			case R.styleable.TextAreaView_compoundDrawable_LeftHeight:
				try {
					leftHeight = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (leftHeight == -1) {
					leftHeight = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_LeftWidth:
				try {
					leftWidth = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (leftWidth == -1) {
					leftWidth = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_Right:
				rightDraw = mTypeArray.getDrawable(attr);
				break;
			case R.styleable.TextAreaView_compoundDrawable_RightHeight:
				try {
					rightHeight = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (rightHeight == -1) {
					rightHeight = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_RightWidth:
				try {
					rightWidth = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (rightWidth == -1) {
					rightWidth = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_Top:
				topDraw = mTypeArray.getDrawable(attr);
				break;
			case R.styleable.TextAreaView_compoundDrawable_TopHeight:
				try {
					topHeight = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (topHeight == -1) {
					topHeight = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundDrawable_TopWidth:
				try {
					topWidth = mTypeArray.getDimensionPixelSize(attr, -1);
				} catch (UnsupportedOperationException e) {
				}
				if (topWidth == -1) {
					topWidth = mTypeArray.getFloat(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_compoundPadding_Bottom:
				try {
					paddingBottom = mTypeArray.getDimensionPixelSize(attr, 0);
				} catch (UnsupportedOperationException e) {
				}
				if (paddingBottom == 0) {
					paddingBottom = mTypeArray.getInteger(attr, 0);
				}
				break;
			case R.styleable.TextAreaView_compoundPadding_Left:
				try {
					paddingLeft = mTypeArray.getDimensionPixelSize(attr, 0);
				} catch (UnsupportedOperationException e) {
				}
				if (paddingLeft == 0) {
					paddingLeft = mTypeArray.getInteger(attr, 0);
				}
				break;
			case R.styleable.TextAreaView_compoundPadding_Right:
				try {
					paddingRight = mTypeArray.getDimensionPixelSize(attr, 0);
				} catch (UnsupportedOperationException e) {
				}
				if (paddingRight == 0) {
					paddingRight = mTypeArray.getInteger(attr, 0);
				}
				break;
			case R.styleable.TextAreaView_compoundPadding_Top:
				try {
					paddingTop = mTypeArray.getDimensionPixelSize(attr, 0);
				} catch (UnsupportedOperationException e) {
				}
				if (paddingTop == 0) {
					paddingTop = mTypeArray.getInteger(attr, 0);
				}
				break;
			case R.styleable.TextAreaView_cursorVisible:
				cursorVisbile = mTypeArray.getBoolean(attr, true);
				break;
			case R.styleable.TextAreaView_fontFamily:
				fontFamily = mTypeArray.getString(attr);
				break;
			case R.styleable.TextAreaView_gravity:
				setGravity(mTypeArray.getInt(attr, Gravity.LEFT | Gravity.CENTER_VERTICAL));
				break;
			case R.styleable.TextAreaView_hint:
				CharSequence hint = mTypeArray.getText(attr);
				if (hint != null && hint.length() > 0) {
					setHint(hint);
				}
				break;
			case R.styleable.TextAreaView_hintColor:
				int color = mTypeArray.getColor(attr, 0);
				if (color != 0) {
					getHintPaint().setColor(color);
				}
				break;
			case R.styleable.TextAreaView_hintSize:
				int hintSize = mTypeArray.getDimensionPixelSize(attr, 12);
				if (hintSize != 0) {
					getHintPaint().setTextSize(hintSize);
				}
				break;
			case R.styleable.TextAreaView_imeActionId:
				actionId = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_imeActionLabel:
				actionLabel = mTypeArray.getString(attr);
				break;
			case R.styleable.TextAreaView_imeOptions:
				mInfo.imeOptions = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_initArraySize:
				mAttr.initArraySize = mTypeArray.getInteger(attr, mAttr.initArraySize);
				break;
			case R.styleable.TextAreaView_inputType:
				inputType |= mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_privateImeOptions:
				setPrivateImeOptions(mTypeArray.getString(attr));
				break;
			case R.styleable.TextAreaView_letterSpacing:
				setLetterSpacing(mTypeArray.getFloat(attr, 0));
				break;
			case R.styleable.TextAreaView_lineSpacing_Add:
				setLineSpacingAdd(mTypeArray.getInteger(attr, 0));
				break;
			case R.styleable.TextAreaView_lineSpacing_Mult:
				setLineSpacingMult(mTypeArray.getInteger(attr, 1));
				break;
			case R.styleable.TextAreaView_maxEms:
				setMaxEms(mTypeArray.getInteger(attr, 0));
				break;
			case R.styleable.TextAreaView_maxLines:
				setMaxLines(mTypeArray.getInteger(attr, 0));
				break;
			case R.styleable.TextAreaView_maxLines_limitheight:
				setMaxLinesLimitHeight(mTypeArray.getInteger(attr, 0));
				break;
			case R.styleable.TextAreaView_nextFocusBottom:
				nextFocusBottom = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_nextFocusLeft:
				nextFocusLeft = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_nextFocusTop:
				nextFocusTop = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_nextFocusRight:
				nextFocusRight = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_outTouch:
				setReceiveOutTouch(mTypeArray.getBoolean(attr, false));
				break;
			case R.styleable.TextAreaView_phoneNumber:
				if (mTypeArray.getBoolean(attr, false)) {
					inputType |= EditorInfo.TYPE_CLASS_PHONE;
				}
				break;
			case R.styleable.TextAreaView_seletable:
				setTextIsSelectable(mTypeArray.getBoolean(attr, true));
				break;
			case R.styleable.TextAreaView_shareCursorDevicer:
				SHARE_ENABLE = mTypeArray.getBoolean(attr, true);
				break;
			case R.styleable.TextAreaView_text:
				text = mTypeArray.getText(attr);
				break;
			case R.styleable.TextAreaView_editable:
				isEditable = mTypeArray.getBoolean(attr, isEditable);
				break;
			case R.styleable.TextAreaView_textColor:
				color = mTypeArray.getColor(attr, -1);
				if (color != -1) {
					getTextPaint().setColor(color);
				}
				break;
			case R.styleable.TextAreaView_textHighlightDrawable:
				textColorHighlightDraw = mTypeArray.getDrawable(attr);
				if (textColorHighlightDraw == null) {
					textColorHighlight = mTypeArray.getColor(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_textColorLink:
				break;
			case R.styleable.TextAreaView_textCursorDrawable:
				cursorDraw = mTypeArray.getDrawable(attr);
				if (cursorDraw == null) {
					cursorColor = mTypeArray.getColor(attr, -1);
				}
				break;
			case R.styleable.TextAreaView_textCursorMinWidth:
				cursorMinWidth = mTypeArray.getInteger(attr, -1);
				break;
			case R.styleable.TextAreaView_textScaleX:
				getTextPaint().setTextScaleX(mTypeArray.getInteger(attr, 0));
				break;
			case R.styleable.TextAreaView_textStyle:
				textStyle = mTypeArray.getInteger(attr, 0);
				break;
			case R.styleable.TextAreaView_truncateAt:
				int index = mTypeArray.getInteger(attr, 0);
				switch (index) {
				case 0:
					setTruncateAt(null);
					break;
				case 1:
					setTruncateAt(TruncateAt.START);
					break;
				case 2:
					setTruncateAt(TruncateAt.END);
					break;
				case 3:
					setTruncateAt(TruncateAt.MIDDLE);
					break;
				case 4:
					setTruncateAt(TruncateAt.MARQUEE);
					break;
				}
				break;
			case R.styleable.TextAreaView_typeface:
				switch (mTypeArray.getInteger(attr, 0)) {
				case 0:
					typeface = Typeface.DEFAULT;
					break;
				case 1:
					typeface = Typeface.SANS_SERIF;
					break;
				case 2:
					typeface = Typeface.SERIF;
					break;
				case 3:
					typeface = Typeface.MONOSPACE;
					break;
				}
				break;
			case R.styleable.TextAreaView_scaleBase:
				scaleLevel = mTypeArray.getInteger(attr, 0) == 0;
				break;
			}
		}
		mTypeArray.recycle();

		setImeActionLabel(actionLabel, actionId);
		setInputType(inputType);

		if (typeface == null && fontFamily != null) {
			typeface = Typeface.create(fontFamily, textStyle);
		}
		setTypeface(typeface, textStyle);
		setEditable(isEditable);
		setText(text);
		assumeLayout();
		if (textColorHighlightDraw != null) {
			setTextHighlightDrawable(textColorHighlightDraw);
		} else if (textColorHighlight != -1) {
			setTextHighlightColor(textColorHighlight);
		}
		if (globalWidth >= 0) {
			if (leftWidth == -1) {
				leftWidth = globalWidth;
			}
			if (rightWidth == -1) {
				rightWidth = globalWidth;
			}
			if (topWidth == -1) {
				topWidth = globalWidth;
			}
			if (bottomWidth == -1) {
				bottomWidth = globalWidth;
			}
		}
		if (globalHeight >= 0) {
			if (leftHeight == -1) {
				leftHeight = globalHeight;
			}
			if (rightHeight == -1) {
				rightHeight = globalHeight;
			}
			if (bottomHeight == -1) {
				bottomHeight = globalHeight;
			}
			if (topHeight == -1) {
				topHeight = globalHeight;
			}
		}
		setCompoundDrawablesBoundsWithScale(leftWidth, leftHeight, rightWidth, rightHeight, topWidth, topHeight,
				bottomWidth, bottomHeight, scaleLevel);
		setCompoundDrawables(leftDraw, topDraw, rightDraw, bottomDraw);
		setCompoundDrawablePadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
		mCursorDevicer = getDefaultCursor();
		if (mCursorDevicer != null) {
			if (cursorDraw != null) {
				mCursorDevicer.setCursorDrawable(cursorDraw);
			} else if (cursorColor != -1) {
				mCursorDevicer.setColor(cursorColor);
			}
			if (cursorMinWidth != -1) {
				mCursorDevicer.setDefaultMinWidth(cursorMinWidth);
			}
			mCursorDevicer.setVisibility(cursorVisbile);
		}
	}

	public TextAreaView(Context context) {
		super(context);
		initBaseInfo();
		mCursorDevicer = getDefaultCursor();
	}

	private void initBaseInfo() {
		setFocusable(true);
		if (mAttr == null) {
			mAttr = new LayoutAttrubute();
		}
		mFilters = new InputFilter[] { new ITextFilter() };
		mPaint = new TextAreaPaint(Paint.ANTI_ALIAS_FLAG);
		mInfo.packageName = getContext().getPackageName();
		mInfo.fieldId = getId();
		mInfo.initialCapsMode = TextUtils.CAP_MODE_WORDS;
		mInfo.inputType = EditorInfo.TYPE_CLASS_TEXT;
		mInfo.imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED;
		mInfo.label = getClass().getCanonicalName();
	}

	/**
	 * Set the private content type of the text, which is the
	 * {@link EditorInfo#privateImeOptions EditorInfo.privateImeOptions} field
	 * that will be filled in when creating an input connection.
	 *
	 * @see #getPrivateImeOptions()
	 * @see EditorInfo#privateImeOptions
	 * @attr ref android.R.styleable#AbsTextView_privateImeOptions
	 */
	public void setPrivateImeOptions(String type) {
		if (type != mInfo.privateImeOptions) {
			mInfo.privateImeOptions = type;
			if (isEditable && isEnabled() && imm != null) {
				imm.restartInput(this);
			}
		}
	}

	/**
	 * Get the private type of the content.
	 *
	 * @see #setPrivateImeOptions(String)
	 * @see EditorInfo#privateImeOptions
	 */
	public String getPrivateImeOptions() {
		return mInfo.privateImeOptions;
	}

	/**
	 * Sets the list of input filters that will be used if the buffer is
	 * Editable. Has no effect otherwise.
	 *
	 * @attr ref android.R.styleable#TextView_maxLength
	 */
	public void setFilters(InputFilter[] filters) {
		mInputFileters = filters;
	}

	/**
	 * Returns the current list of input filters.
	 *
	 * @attr ref android.R.styleable#TextView_maxLength
	 */
	public InputFilter[] getFilters() {
		return mInputFileters;
	}

	/**
	 * Set the type of the content with a constant as defined for
	 * {@link EditorInfo#inputType}. This will take care of changing the key
	 * listener, by calling {@link #setKeyListener(KeyListener)}, to match the
	 * given content type. If the given content type is
	 * {@link EditorInfo#TYPE_NULL} then a soft keyboard will not be displayed
	 * for this text view.
	 *
	 * Note that the maximum number of displayed lines (see
	 * {@link #setMaxLinesLimitHeight(int)}) will be modified if you change the
	 * {@link EditorInfo#TYPE_TEXT_FLAG_MULTI_LINE} flag of the input type.
	 *
	 * @see #getInputType()
	 * @see #setRawInputType(int)
	 * @see android.text.InputType
	 * @attr ref android.R.styleable#AbsTextView_inputType
	 */
	public void setInputType(int type) {
		if (type != mInfo.inputType) {
			mInfo.inputType = type;
			final int cls = type & EditorInfo.TYPE_MASK_CLASS;
			KeyListener input;
			if (cls == EditorInfo.TYPE_CLASS_TEXT) {
				boolean autotext = (type & EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0;
				TextKeyListener.Capitalize cap;
				if ((type & EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
					cap = TextKeyListener.Capitalize.CHARACTERS;
				} else if ((type & EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS) != 0) {
					cap = TextKeyListener.Capitalize.WORDS;
				} else if ((type & EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
					cap = TextKeyListener.Capitalize.SENTENCES;
				} else {
					cap = TextKeyListener.Capitalize.NONE;
				}
				input = TextKeyListener.getInstance(autotext, cap);
			} else if (cls == EditorInfo.TYPE_CLASS_NUMBER) {
				input = DigitsKeyListener.getInstance((type & EditorInfo.TYPE_NUMBER_FLAG_SIGNED) != 0,
						(type & EditorInfo.TYPE_NUMBER_FLAG_DECIMAL) != 0);
			} else if (cls == EditorInfo.TYPE_CLASS_DATETIME) {
				switch (type & EditorInfo.TYPE_MASK_VARIATION) {
				case EditorInfo.TYPE_DATETIME_VARIATION_DATE:
					input = DateKeyListener.getInstance();
					break;
				case EditorInfo.TYPE_DATETIME_VARIATION_TIME:
					input = TimeKeyListener.getInstance();
					break;
				default:
					input = DateTimeKeyListener.getInstance();
					break;
				}
			} else if (cls == EditorInfo.TYPE_CLASS_PHONE) {
				input = DialerKeyListener.getInstance();
			} else {
				input = TextKeyListener.getInstance();
			}
			mKeyListener = input;
			if (mText != null) {
				setText(mText);
			}
			if (isEditable && isEnabled() && imm != null) {
				imm.restartInput(this);
			}
		}
	}

	/**
	 * Change the custom IME action associated with the text view, which will be
	 * reported to an IME with {@link EditorInfo#actionLabel} and
	 * {@link EditorInfo#actionId} when it has focus.
	 * 
	 * @see #getImeActionLabel
	 * @see #getImeActionId
	 * @see android.view.inputmethod.EditorInfo
	 * @attr ref android.R.styleable#AbsTextView_imeActionLabel
	 * @attr ref android.R.styleable#AbsTextView_imeActionId
	 */
	public void setImeActionLabel(CharSequence label, int actionId) {
		if (mInfo.actionId != actionId || mInfo.label != label) {
			mInfo.actionId = actionId;
			mInfo.label = label;
			if (isEditable && isEnabled() && imm != null) {
				imm.restartInput(this);
			}
		}
	}

	public void setCursorVisibile(boolean v) {
		if (mCursorDevicer == null) {
			mCursorDevicer = getDefaultCursor();
			if (mCursorDevicer != null) {
				mCursorDevicer.setVisibility(v);
			}
		} else {
			mCursorDevicer.setVisibility(v);
		}
	}

	public void setReceiveOutTouch(boolean out) {
		receiveOutTouch = out;
	}

	public boolean isReceiveOutTouch() {
		return receiveOutTouch;
	}

	public void setTextIsSelectable(boolean selectable) {
		if (isTextSeletable != selectable) {
			isTextSeletable = selectable;
			if (!isTextSeletable) {
				CharSequence text = getText();
				if (text instanceof Spannable) {
					int start = Selection.getSelectionStart(text);
					int end = Selection.getSelectionEnd(text);
					if (start != end) {
						Selection.setSelection((Spannable) text, end);
					}
				}
			}
		}
	}

	public void setEditable(boolean editable) {
		if (editable != isEditable) {
			isEditable = editable;
			if (editable) {
				if (mCursorDevicer == null) {
					mCursorDevicer = getDefaultCursor();
				} else {
					mCursorDevicer.setVisibility(true);
				}
				if (mText instanceof Editable) {
					return;
				}
				if (mText != null) {
					mText = getDefaultEditable(mText);
					if (mLayout != null) {
						mLayout.setText(mText);
						restartLayout();
					}
				}
			} else {
				if (mCursorDevicer != null) {
					mCursorDevicer.setVisibility(false);
				}
				if (imm != null) {
					imm.hideSoftInputFromWindow(getWindowToken(), 0);
				}
			}
		}
	}

	public void setTextHighlightDrawable(Drawable draw) {
		if (mLayout != null) {
			mLayout.setTextHighlightDrawable(draw);
			invalidate();
		}
	}

	public void setTextHighlightColor(int color) {
		if (mLayout != null) {
			mLayout.setTextHighlightColor(color);
			invalidate();
		}
	}

	protected final Editable getDefaultEditable(CharSequence text) {
		Editable editable = mEditableFactory.newEditable(text == null ? "" : text);
		editable.setSpan(new ITextChangListener(), 0, editable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		editable.setSpan(new ITextSpanListener(), 0, editable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		editable.setFilters(mFilters);
		if (text != null) {
			Selection.setSelection(editable, 0);
		}
		return editable;
	}

	/**
	 * Sets the Factory used to create new Editables.
	 */
	public final void setEditableFactory(Editable.Factory factory) {
		mEditableFactory = factory;
		setText(mText);
	}

	/**
	 * Get the IME action label previous set with {@link #setImeActionLabel}.
	 *
	 * @see #setImeActionLabel
	 * @see android.view.inputmethod.EditorInfo
	 */
	public CharSequence getImeActionLabel() {
		return mInfo.actionLabel;
	}

	/**
	 * Get the IME action ID previous set with {@link #setImeActionLabel}.
	 *
	 * @see #setImeActionLabel
	 * @see android.view.inputmethod.EditorInfo
	 */
	public int getImeActionId() {
		return mInfo.actionId;
	}

	/**
	 * Change the editor type integer associated with the text view, which will
	 * be reported to an IME with {@link EditorInfo#imeOptions} when it has
	 * focus.
	 * 
	 * @see #getImeOptions
	 * @see android.view.inputmethod.EditorInfo
	 * @attr ref android.R.styleable#AbsTextView_imeOptions
	 */
	public void setImeOptions(int imeOptions) {
		if (mInfo.imeOptions != imeOptions) {
			mInfo.imeOptions = imeOptions;
			if (isEditable && isEnabled() && imm != null) {
				imm.restartInput(this);
			}
		}
	}

	/**
	 * Get the type of the IME editor.
	 *
	 * @see #setImeOptions(int)
	 * @see android.view.inputmethod.EditorInfo
	 */
	public int getImeOptions() {
		return mInfo.imeOptions;
	}

	/**
	 * Set the extra input data of the text, which is the
	 * {@link EditorInfo#extras TextBoxAttribute.extras} Bundle that will be
	 * filled in when creating an input connection. The given integer is the
	 * resource ID of an XML resource holding an
	 * {@link android.R.styleable#InputExtras &lt;input-extras&gt;} XML tree.
	 *
	 * @see #getInputExtras(boolean)
	 * @see EditorInfo#extras
	 * @attr ref android.R.styleable#AbsTextView_editorExtras
	 */
	public void setInputExtras(int xmlResId) throws XmlPullParserException, IOException {
		XmlResourceParser parser = getResources().getXml(xmlResId);
		mInfo.extras = new Bundle();
		getResources().parseBundleExtras(parser, mInfo.extras);
	}

	/**
	 * Retrieve the input extras currently associated with the text view, which
	 * can be viewed as well as modified.
	 *
	 * @param create
	 *            If true, the extras will be created if they don't already
	 *            exist. Otherwise, null will be returned if none have been
	 *            created.
	 * @see #setInputExtras(int)
	 * @see EditorInfo#extras
	 * @attr ref android.R.styleable#AbsTextView_editorExtras
	 */
	public Bundle getInputExtras(boolean create) {
		if (create) {
			if (mInfo.extras == null) {
				mInfo.extras = new Bundle();
			}
		}
		return mInfo.extras;
	}

	/**
	 * Get the type of the editable content.
	 *
	 * @see #setInputType(int)
	 * @see android.text.InputType
	 */
	public int getInputType() {
		return mInfo.inputType;
	}

	@Override
	protected boolean verifyDrawable(Drawable who) {
		if (who == null) {
			return false;
		}
		boolean verified = super.verifyDrawable(who);
		return verified ? verified : mDrawables != null ? mDrawables.verifyDrawable(who) : false;
	}

	public void jumpDrawablesToCurrentState() {
		super.jumpDrawablesToCurrentState();
		if (mDrawables != null) {
			mDrawables.jumpDrawablesToCurrentState();
		}
	}

	/**
	 *
	 * Returns the state of the {@code textIsSelectable} flag (See
	 * {@link #setTextIsSelectable setTextIsSelectable()}). Although you have to
	 * set this flag to allow users to select and copy text in a non-editable
	 * TextView, the content of an {@link EditText} can always be selected,
	 * independently of the value of this flag.
	 * <p>
	 *
	 * @return True if the text displayed in this TextView can be selected by
	 *         the user.
	 *
	 * @attr ref android.R.styleable#TextView_textIsSelectable
	 */
	public boolean isTextSelectable() {
		return isTextSeletable;
	}

	public void setText(CharSequence text) {
		if (isEditable) {
			if (mText instanceof Editable) {
				((Editable) mText).clear();
				((Editable) mText).clearSpans();
				mText = null;
			}
			if (text != null) {
				text = mFilters[0].filter(text, 0, text.length(), null, 0, 0);
			}
			mText = getDefaultEditable(text);
			if (mLayout != null) {
				mLayout.setText(mText);
				restartLayout();
				// requestLayout();
			}
		} else if (mText != text) {
			if (text != null) {
				text = mFilters[0].filter(text, 0, text.length(), null, 0, 0);
			}
			mText = text;
			if (mLayout != null) {
				mLayout.setText(mText);
				restartLayout();
				// requestLayout();
			}
		}
		refreshHintLayout(getWidth(), getHeight());
	}

	private void refreshHintLayout(int w, int h) {
		if (w == 0 || h == 0) {
			return;
		}
		if ((mText == null || mText.length() <= 0) && mHint != null && mHint.length() > 0) {
			if (mHintLayout == null) {
				mHintAttr = new LayoutAttrubute(mAttr);
				mHintLayout = getDefaultLayoutAide(mAttr, mHint, getHintPaint());
			} else if (mHintLayout.getText() != mHint) {
				mHintLayout.setText(mHint);
			}
			mHintAttr.copy(mAttr);
			mHintLayout.restartLayout(this);
			mHintLayout.restartLayoutSize(0, w, h, this);
		} else if (mCursorDevicer != null && !mCursorDevicer.isVisibility() && onCheckIsTextEditor()) {
			mCursorDevicer.setVisibility(true);
		}
	}

	/**
	 * Convenience method: Append the specified text to the TextView's display
	 * buffer, upgrading it to BufferType.EDITABLE if it was not already
	 * editable.
	 */
	public final void append(CharSequence text) {
		append(text, 0, text.length());
	}

	/**
	 * Convenience method: Append the specified text slice to the TextView's
	 * display buffer, upgrading it to BufferType.EDITABLE if it was not already
	 * editable.
	 */
	public void append(CharSequence text, int start, int end) {
		CharSequence mText = getText();
		if (mText instanceof Editable) {
			((Editable) mText).append(text, start, end);
		}
	}

	protected final boolean canShowHint() {
		return (mText == null || mText.length() <= 0) && mHintLayout != null && mHint != null;
	}

	protected void dispatchDraw(Canvas canvas) {
		if (mDrawables != null) {
			mDrawables.draw(canvas, this);
		}
		int scrollX = mScrollX;
		int scrollY = mScrollY;
		canvas.translate(-scrollX, -scrollY);
		boolean showHint = canShowHint();
		int paddingTop = mLayout.getPaddingTop();
		int paddingLeft = mLayout.getPaddingLeft();
		int paddingBottom = mLayout.getPaddingBottom();
		int paddingRight = mLayout.getPaddingRight();
		int layoutheight = mLayout.getHeight(), mHeight = getHeight();
		if (showHint) {
			int hintLayoutHeight = mHintLayout.getHeight();
			if (layoutheight < hintLayoutHeight) {
				layoutheight = hintLayoutHeight;
			}
		}
		if (paddingTop != 0 || paddingLeft != 0 || paddingRight != 0 || paddingBottom != 0 || mHeight != layoutheight) {
			int save = canvas.save();
			int left = paddingLeft + scrollX;
			int right = getWidth() - paddingRight + scrollX;
			int top = paddingTop + scrollY + mReviseY;
			int bottom = top + layoutheight;
			canvas.clipRect(left, top, right, bottom);
			if (mCursorDevicer != null) {
				mCursorDevicer.draw(canvas);
			}
			canvas.translate(paddingLeft, paddingTop + mReviseY);
			if (showHint) {
				mHintLayout.draw(canvas);
			} else if (mLayout != null) {
				mLayout.draw(canvas);
			}
			canvas.restoreToCount(save);
		} else {
			if (mCursorDevicer != null) {
				mCursorDevicer.draw(canvas);
			}
			if (showHint) {
				mHintLayout.draw(canvas);
			} else if (mLayout != null) {
				mLayout.draw(canvas);
			}
		}
		canvas.translate(scrollX, scrollY);
	}

	public void setTextSize(float size) {
		setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
	}

	public float getTextSize() {
		return mPaint.getTextSize();
	}

	public void setTextSize(int unit, float size) {
		Context c = getContext();
		Resources r;

		if (c == null) {
			r = Resources.getSystem();
		} else {
			r = c.getResources();
		}
		size = TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
		if (mPaint.getTextSize() != size) {
			mPaint.setTextSize(size);
			restartLayout();
		}
	}

	public CursorHelper getCursorHelper() {
		if (mCursorHelper != null) {
			return mCursorHelper;
		}
		if (mLayout instanceof FormatLayout) {
			mCursorHelper = new DefaultCursorHelper((FormatLayout) mLayout);
			return mCursorHelper;
		}
		return null;
	}

	@SuppressLint("NewApi")
	public void setLetterSpacing(float spac) {
		if (spac != mPaint.getLetterSpacing()) {
			mPaint.setLetterSpacing(spac);
			restartLayout();
		}
	}

	public TextAreaPaint getTextPaint() {
		return mPaint;
	}

	protected void requestMoveCursor(int x, int y) {
		CursorHelper helper = getCursorHelper();
		if (helper != null) {
			helper.requestMoveCursor(x, y - mReviseY, mCursorDevicer, this);
			if (isEditable) {
				if (imm == null) {
					imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				}
				requestFocus();
				imm.viewClicked(this);
				imm.showSoftInput(this, 0);
			}
		}
	}

	/**
	 * default 1f
	 * 
	 * @param firction
	 *            <=1
	 */
	public void setFirction(float firction) {
		maxFriction = firction;
	}

	protected boolean startScrollY(int y) {
		float value;
		TextAreaLayout layout;
		if (canShowHint()) {
			if (mHintLayout == null) {
				setTextScrollY(-getTextScrollY());
				return false;
			} else {
				layout = mHintLayout;
			}
		} else {
			if (mLayout == null) {
				setTextScrollY(-getTextScrollY());
				return false;
			} else {
				layout = mLayout;
			}
		}
		value = layout.scrollByVertical(y);
		if (value != -1) {
			int mScrollY = (int) value;
			setTextScrollY(mScrollY);
			if (mLayout.checkFirstLineIsTextMin()) {
				updateTopToVisibileScreen(mScrollY);
			} else if (mLayout.checkFirstLineIsTextMax()) {
				updateTopToVisibileScreen(mScrollY + mLayout.getHeight() + getCompoundDrawablePaddingTop());
			}
			return false;
		} else if (mLayout.amendTopVerticalLevelToZoer(this)) {
			if (mCursorDevicer != null) {
				int firstTop = mLayout.getLineTop(0);
				int cX = mCursorDevicer.getCurrX();
				int cY = mCursorDevicer.getCurrY() - y+firstTop;
				mCursorDevicer.requestUpdate(cX, cY, false);
			}
			oldScrollY -= y;
			y = 0;
		}
		setTextScrollY((int) y);
		return true;
	}

	protected boolean startScrollX(float x) {
		float value;
		if (canShowHint()) {
			if (mHintLayout == null) {
				setTextScrollX(-getTextScrollX());
				return false;
			}
			value = mHintLayout.scrollByHorizontal(x);
		} else {
			if (mLayout == null) {
				setTextScrollX(-getTextScrollX());
				return false;
			}
			value = mLayout.scrollByHorizontal(x);
		}
		if (value != -1) {
			setTextScrollX((int) value);
			return false;
		}
		setTextScrollX((int) x);
		return true;
	}

	private void recyleVelocityTracker() {
		if (mTracker != null) {
			mTracker.recycle();
			mTracker = null;
		}
	}

	private void initVelocityTracker() {
		if (mTracker == null) {
			mTracker = VelocityTracker.obtain();
		} else {
			mTracker.clear();
		}
	}

	private final static int MAX_TRACK = 10000;
	private final static int MIN_TRACK = 1000;
	private MoveFlag mMoveFlag;
	private VelocityTracker mTracker;
	private FlingRunnable mFling;

	protected MoveFlag checkIsLayoutEvent(MotionEvent event) {
		if (!receiveOutTouch) {
			float x = event.getX();
			float y = event.getY();
			int left = getCompoundPaddingLeft();
			int right = getCompoundPaddingRight();
			if (x < left || x > left + mLayout.getWidth()) {
				return MoveFlag.BLOCK;
			}
			int top = getCompoundPaddingTop();
			if (y < top + mReviseY || y > top + mLayout.getHeight() + mReviseY) {
				return MoveFlag.BLOCK;
			}
		}
		stopFling();
		return MoveFlag.NO;
	}

	protected void stopFling() {
		if (mFling != null) {
			mFling.stop();
		}
	}

	private boolean touchDown(MotionEvent event) {
		if (!hasFocusable()) {
			return false;
		}
		mMoveFlag = checkIsLayoutEvent(event);
		if (mMoveFlag == MoveFlag.BLOCK) {
			isTouching = false;
			return false;
		}
		mDownX = event.getRawX();
		mDownY = event.getRawY();
		isTouching = true;
		oldScrollX = getTextScrollX();
		oldScrollY = getTextScrollY();
		// requestFocus();
		initVelocityTracker();
		if (!isFocusableInTouchMode() && isEditable) {
			setFocusableInTouchMode(true);
		}
		return true;
	}

	private boolean touchMove(MotionEvent event) {

		if (mMoveFlag == MoveFlag.BLOCK) {
			return false;
		}
		mTracker.addMovement(event);
		float rawX = event.getRawX();
		float rawY = event.getRawY();
		if (mMoveFlag == MoveFlag.NO) {
			mMoveFlag = foundTouchIntent(mDownX, mDownY, rawX, rawY, 15);
		}
		if (isSingleLine()) {
			if (MoveFlag.isHorizontal(mMoveFlag)) {
				if (canShowHint()) {
					if (mHintLayout.hasFullLayoutWidth() <= 0) {
						return false;
					}
				} else if (mLayout.hasFullLayoutWidth() <= 0) {
					return false;
				}
				float x = oldScrollX + mDownX - event.getRawX();
				startScrollX(x);
			}
		} else if (MoveFlag.isVertical(mMoveFlag)) {
			if (canShowHint()) {
				if (mHintLayout.hasFullLayoutHeight() <= 0) {
					return false;
				}
			} else if (mLayout.hasFullLayoutHeight() <= 0) {
				return false;
			}
			int y = (int) (mDownY - rawY) + oldScrollY;
			startScrollY(y);
		}
		return true;
	}

	private boolean touchUpOrCancel(MotionEvent event) {
		isTouching = false;
		if (mMoveFlag == MoveFlag.BLOCK) {
			return false;
		}
		if (mMoveFlag == MoveFlag.NO) {
			requestMoveCursor((int) (getTextScrollX() + event.getX()), (int) (getTextScrollY() + event.getY()));
		} else if (!isSingleLine()) {
			if (canShowHint()) {
				if (mHintLayout.hasFullLayoutHeight() <= 0) {
					return false;
				}
			} else if (mLayout.hasFullLayoutHeight() <= 0) {
				return false;
			}
			if (MoveFlag.isVertical(mMoveFlag)) {
				mTracker.computeCurrentVelocity(MIN_TRACK, MAX_TRACK);
				float track = mTracker.getYVelocity();
				float trackY = getVelocity(track, maxFriction);
				mFling = getFlingRunnable();
				int scrollY = getTextScrollY();
				float toScrollY = scrollY - trackY;
				mFling.start(scrollY, toScrollY, true, maxFriction, trackY);
				return true;
			}
		} else if (MoveFlag.isHorizontal(mMoveFlag)) {
			if (canShowHint()) {
				if (mHintLayout.hasFullLayoutWidth() <= 0) {
					return false;
				}
			} else if (mLayout.hasFullLayoutWidth() <= 0) {
				return false;
			}
			mTracker.computeCurrentVelocity(MIN_TRACK, MAX_TRACK);
			float trackX = getVelocity(mTracker.getXVelocity(), maxFriction);
			mFling = getFlingRunnable();
			int scrollX = getTextScrollX();
			float toScrollX = scrollX - trackX;
			mFling.start(scrollX, toScrollX, false, maxFriction, trackX);
			return true;
		}
		recyleVelocityTracker();
		return false;
	}

	public void setMaxLines(int maxlines) {
		int oldLines = mAttr.maxLines;
		if (oldLines != maxlines) {
			mAttr.maxLines = maxlines;

			if ((mInfo.inputType & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT) {
				if (maxlines == 1) {
					mInfo.inputType &= ~EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
				} else {
					mInfo.inputType |= EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
				}
			}
			if (mLayout != null) {
				int count = mLayout.getLineCount();
				if (oldLines == 1 || count > maxlines) {
					requestLayout();
				} else {
					requestLayoutSize(0, getWidth(), getHeight());
					reviseGrivaty();
				}
				invalidate();
			}

		}
	}

	/**
	 * {@docRoot MeasureAttribute#maxLinesLimitHeight}
	 * 
	 * @param maxlines
	 */
	public void setMaxLinesLimitHeight(int maxlines) {
		int oldLines = mAttr.maxLinesLimitHeight;
		if (oldLines != maxlines) {
			mAttr.maxLinesLimitHeight = maxlines;
			if (mLayout != null) {
				int count = mLayout.getLineRecyleCount();
				int start = 0;
				if (count > 0) {
					start = mLayout.getLineStart(0);
				}
				requestLayoutSize(start, getWidth(), getHeight());
				reviseGrivaty();
				invalidate();
			}
		}
	}

	public int getMaxLines() {
		return mAttr.maxLinesLimitHeight;
	}

	/**
	 * no support, maybe in later for support
	 *
	 * @param where
	 */
	public void setTruncateAt(TextUtils.TruncateAt where) {
		// if (!isEditable) {
		if (mAttr.truncateAt != where) {
			mAttr.truncateAt = where;
			restartLayout();
		}
		// }
	}

	public TruncateAt getEllipsize() {
		return mAttr.truncateAt;
	}

	public int getMaxEms() {
		return mAttr.maxEms;
	}

	public void setMaxEms(int maxems) {
		if (mAttr.maxEms != maxems) {
			mAttr.maxEms = maxems;
			restartLayout();
		}
	}

	public boolean dispatchTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (touchDown(event)) {
				return true;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (touchMove(event)) {
				return true;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if (touchUpOrCancel(event)) {
				return true;
			}
			break;
		}
		return super.dispatchTouchEvent(event);
	}

	protected FlingRunnable getFlingRunnable() {
		return new FlingRunnable();
	}

	protected float getVelocity(float velocity, float maxFriction) {
		float v = maxFriction - maxFriction * ((Math.abs(velocity)) / MAX_TRACK);
		if (v == 0) {
			return velocity;
		}
		return velocity * (1 - v);
	}

	protected enum MoveFlag {
		TOP, BOTTOM, LEFT, RIGHT, NO, UNKNOW, BLOCK;

		public static boolean isVertical(MoveFlag intent) {
			return intent == TOP || intent == BOTTOM;
		}

		public static boolean isHorizontal(MoveFlag intent) {
			return intent == LEFT || intent == RIGHT;
		}
	}

	/**
	 * 简单的滑动意图检测
	 * 
	 * @param oldX
	 * @param oldY
	 * @param nowX
	 * @param nowY
	 * @param offsetDistance
	 * @return
	 */
	protected final MoveFlag foundTouchIntent(float oldX, float oldY, float nowX, float nowY, float offsetDistance) {
		if (oldX != nowX && oldY != nowY) {
			float spacX = nowX - oldX;
			float spacY = nowY - oldY;
			float absSpacX = Math.abs(spacX);
			float absSpacY = Math.abs(spacY);
			if (absSpacX > offsetDistance || absSpacY > offsetDistance) {
				if (absSpacX > absSpacY) {
					if (spacX > 0) {
						return MoveFlag.RIGHT;
					}
					return MoveFlag.LEFT;
				} else if (absSpacX < absSpacY) {
					if (spacY > 0) {
						return MoveFlag.BOTTOM;
					}
					return MoveFlag.TOP;
				}
			}
		}
		return MoveFlag.NO;
	}

	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		// Log.e(TAG, ""+l+" "+t+" "+oldl+" "+oldt);
		invalidate();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
	}

	public void setTypeface(Typeface tf) {
		if (mPaint.getTypeface() != tf) {
			mPaint.setTypeface(tf);
			invalidate();
		}
	}

	public boolean isSingleLine() {
		return mLayout.isSingleLine();
	}

	/**
	 * set the base alignment of this layout.
	 * 
	 * @conflict {@link #setGravity(int)}
	 */
	public void setAlignment(Alignment a) {
		if (mAttr.alignment != a) {
			mAttr.alignment = a;
			invalidate();
		}
	}

	private void assumeLayout() {
		if (mLayout == null) {
			if (mText == null && isEditable) {
				mText = getDefaultEditable(mText);
			}
			mLayout = getDefaultLayoutAide(mAttr, mText, mPaint);
		}
	}

	/**
	 * 共享游标
	 * 
	 * @hide
	 */
	public static void shareCursorDevicer() {
		SHARE_ENABLE = true;
	}

	protected CursorDevicer getDefaultCursor() {
		if (isEnabled()) {
			if (SHARE_CURSOR != null) {
				if (!hasFocus()) {
					return null;
				}
				if (SHARE_CURSOR.checkView(this)) {
					return SHARE_CURSOR;
				}
				return null;
			}
			CursorDevicer cd = null;
			if (mCursorFactory != null) {
				cd = mCursorFactory.newCursorDivicer(this);
			}
			if (SHARE_ENABLE) {
				SHARE_CURSOR = cd;
			}
			return cd;
		}
		return null;
	}

	@Override
	public boolean onPreDraw() {
		if (updateCursorToVisibileScreen()) {
			updateCursorToVisibileScreen();
		}
		unregisterForPreDraw();
		return true;
	}

	public boolean updateTopToVisibileScreen(int top) {
		if (mTempRect == null) {
			mTempRect = new Rect();
		}
		mTempRect.top = top;
		mTempRect.bottom = top;
		offsetScrollRect(getTextScrollX(), getTextScrollY());
		return requestRectangleOnScreen(mTempRect);
	}

	public boolean updateCursorToVisibileScreen() {
		if (mCursorDevicer != null && isEditable && !inTouching()) {
			if (mTempRect == null) {
				mTempRect = new Rect();
			}
			mCursorDevicer.getBounds(mTempRect);
			// mTempRect.offset(getScrollX(), getScrollY());
			offsetScrollRect(getTextScrollX(), getTextScrollY());
			return requestRectangleOnScreen(mTempRect);
		}
		return true;
	}

	private void offsetScrollRect(int scrollX, int scrollY) {
		mTempRect.left -= scrollX;
		mTempRect.right -= scrollX;
		mTempRect.top -= scrollY - mReviseY;
		mTempRect.bottom -= scrollY - mReviseY;
	}

	/**
	 * <pre>
	 * {@link Gravity#LEFT}. conflict {@link #setAlignment(Alignment.ALIGN_LEFT)};
	 * </pre>
	 * 
	 * <pre>
	 * {@link Gravity#RIGHT}. conflict {@link #setAlignment(Alignment.ALIGN_RIGHT)};
	 * </pre>
	 * 
	 * <pre>
	 * {@link Gravity#CENTER_HORIZONTAL}. conflict {@link #setAlignment(Alignment.ALIGN_CENTER)};
	 * </pre>
	 * 
	 * @param gravity
	 *            this is gravity
	 * @conflict {@link #setAlignment(Alignment)}
	 * @see Gravity
	 */
	public void setGravity(int gravity) {
		if (mGravity != gravity) {
			mGravity = gravity;
			boolean update = reviseGrivaty();
			int hozitalMask = Gravity.HORIZONTAL_GRAVITY_MASK & gravity;
			switch (hozitalMask) {
			case Gravity.RIGHT:
			case Gravity.END:
				if (getAlignment() != Alignment.ALIGN_RIGHT) {
					setAlignment(Alignment.ALIGN_RIGHT);
					return;
				}
				break;
			case Gravity.CENTER_HORIZONTAL:
				if (getAlignment() != Alignment.ALIGN_CENTER) {
					setAlignment(Alignment.ALIGN_CENTER);
					return;
				}
				break;
			default:
				if (getAlignment() != Alignment.ALIGN_LEFT) {
					setAlignment(Alignment.ALIGN_LEFT);
					return;
				}
			}
			if (update) {
				invalidate();
			}
		}
	}

	protected final boolean reviseGrivaty() {
		if (mLayout == null) {
			return false;
		}
		int lH = mLayout.getHeight();
		int mH = getHeight() - getPaddingVertical();
		int value = mReviseY;

		if (lH >= mH) {
			value = 0;
		} else {
			int verticalMask = Gravity.VERTICAL_GRAVITY_MASK & mGravity;
			switch (verticalMask) {
			case Gravity.CENTER_VERTICAL:
				value = (mH - lH) >> 1;
				break;
			case Gravity.BOTTOM:
				value = mH - lH;
				break;
			default:
				value = 0;
			}
		}
		if (value != mReviseY) {
			mReviseY = value;
			if (mCursorDevicer != null) {
				mCursorDevicer.setDefaultMoveLevel(mReviseX, mReviseY);
			}
			return true;
		}
		return false;
	}

	public Alignment getAlignment() {
		return mAttr.alignment;
	}

	public final void setCursorFactory(CursorFactory cf) {
		mCursorFactory = cf;
	}

	public final CursorDevicer getCursorDivicer() {
		return mCursorDevicer;
	}

	public final int getGravity() {
		return mGravity;
	}

	public CharSequence getText() {
		return mText;
	}

	public Editable getEditableText() {
		CharSequence text = getText();
		if (text instanceof Editable) {
			return (Editable) text;
		}
		return null;
	}

	public int getLineHeight(int line) {
		return mLayout.getLineHeight(line);
	}

	/**
	 * Sets the typeface and style in which the text should be displayed, and
	 * turns on the fake bold and italic bits in the Paint if the Typeface that
	 * you provided does not have all the bits in the style that you specified.
	 *
	 * @attr ref android.R.styleable#TextView_typeface
	 */
	public void setTypeface(Typeface tf, int style) {
		Paint mTextPaint = getTextPaint();
		if (style > 0) {
			if (tf == null) {
				tf = mTextPaint.getTypeface();
			} else {
				tf = Typeface.create(tf, style);
			}

			setTypeface(tf);
			// now compute what (if any) algorithmic styling is needed
			int typefaceStyle = tf != null ? tf.getStyle() : 0;
			int need = style & ~typefaceStyle;
			mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
			mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
		} else {
			mTextPaint.setFakeBoldText(false);
			mTextPaint.setTextSkewX(0);
			setTypeface(tf);
		}
	}

	protected final TextAreaLayout getHintLayout() {
		return mHintLayout;
	}

	public final KeyListener getKeyListener() {
		return mKeyListener;
	}

	public void setKeyListener(KeyListener input) {
		mKeyListener = input;
	}

	/**
	 * @return the current transformation method for this TextView. This will
	 *         frequently be null except for single-line and password fields.
	 *
	 * @attr ref android.R.styleable#TextView_password
	 * @attr ref android.R.styleable#TextView_singleLine
	 */
	public final TransformationMethod getTransformationMethod() {
		return null;
	}

	/**
	 * Sets the transformation that is applied to the text that this TextView is
	 * displaying.
	 *
	 * @attr ref android.R.styleable#TextView_password
	 */
	public final void setTransformationMethod(TransformationMethod method) {
	}

	public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom) {
		setCompoundDrawables(left, top, right, bottom, false);
	}

	/**
	 * -1为无效的
	 * 
	 * @param leftWidth
	 *            0<i<=1
	 * @param leftHeight
	 *            0<i<=1
	 * @param rightWidth
	 *            0<i<=1
	 * @param rightHeight
	 *            0<i<=1
	 * @param topWidth
	 *            0<i<=1
	 * @param topHeight
	 *            0<i<=1
	 * @param bottomWidth
	 *            0<i<=1
	 * @param bottomHeight
	 *            0<i<=1
	 * @param scaleLevel
	 *            false,根据View宽度缩放,否则高度缩放
	 */
	public void setCompoundDrawablesBoundsWithScale(float leftWidth, float leftHeight, float rightWidth,
			float rightHeight, float topWidth, float topHeight, float bottomWidth, float bottomHeight,
			boolean scaleLevel) {
		if (mDrawables == null) {
			mDrawables = new Drawables();
		}
		mDrawables.setDrawablesBounds(leftWidth, leftHeight, rightWidth, rightHeight, topWidth, topHeight, bottomWidth,
				bottomHeight, scaleLevel);
	}

	public void cacelCompoundDrawablesBoundsWithScale() {
		setCompoundDrawablesBoundsWithScale(-1, -1, -1, -1, -1, -1, -1, -1, false);
	}

	private void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom,
			boolean withIntrinsicBounds) {
		boolean hasDrawable = left != null || top != null || right != null || bottom != null;
		int[] state = getDrawableState();
		if (hasDrawable) {
			if (mDrawables == null) {
				mDrawables = new Drawables();
			}
			if (withIntrinsicBounds) {
				if (left != null) {
					left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
				}
				if (top != null) {
					top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
				}
				if (right != null) {
					right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
				}
				if (bottom != null) {
					bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
				}
				cacelCompoundDrawablesBoundsWithScale();
			}
			mDrawables.setDrawables(left, top, right, bottom, state, this);
			updateLayoutPadding(true);
		}
	}

	public void setCompoundDrawablesWithIntrinsicBounds(int leftResourceId, int topResourceId, int rightResourceId,
			int bottomResourceId) {
		final Resources sources = getContext().getResources();
		setCompoundDrawablesWithIntrinsicBounds(leftResourceId != 0 ? sources.getDrawable(leftResourceId) : null,
				topResourceId != 0 ? sources.getDrawable(topResourceId) : null,
				rightResourceId != 0 ? sources.getDrawable(rightResourceId) : null,
				bottomResourceId != 0 ? sources.getDrawable(bottomResourceId) : null);
	}

	public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
		setCompoundDrawables(left, top, right, bottom, true);
	}

	/**
	 * 保留方法
	 * 
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	public void setCompoundDrawablesRelative(Drawable left, Drawable top, Drawable right, Drawable bottom) {
		setCompoundDrawables(left, top, right, bottom);
	}

	/**
	 * 保留方法
	 * 
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	public void setCompoundDrawablesRelativeWithIntrinsicBounds(int leftResourceId, int topResourceId,
			int rightResourceId, int bottomResourceId) {
	}

	/**
	 * 保留方法
	 * 
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable left, Drawable top, Drawable right,
			Drawable bottom) {
	}

	public Drawable getCompoundDrawableLeft() {
		if (mDrawables == null) {
			return null;
		}
		return mDrawables.mLeft;
	}

	public Drawable getCompoundDrawableRight() {
		if (mDrawables == null) {
			return null;
		}
		return mDrawables.mRight;
	}

	public Drawable getCompoundDrawableTop() {
		if (mDrawables == null) {
			return null;
		}
		return mDrawables.mTop;
	}

	public Drawable getCompoundDrawableBottom() {
		if (mDrawables == null) {
			return null;
		}
		return mDrawables.mBottom;
	}

	public void setCompoundDrawablePadding(int pad) {
		setCompoundDrawablePadding(pad, pad, pad, pad);
	}

	public void setCompoundDrawablePadding(int left, int top, int right, int bottom) {
		if (mDrawables == null) {
			mDrawables = new Drawables();
		}
		mDrawables.setDrawablePadding(left, top, right, bottom);
		updateLayoutPadding(true);
	}

	public int getCompoundDrawablePaddingTop() {
		if (mDrawables == null) {
			return -1;
		}
		return mDrawables.mCompoundPaddingTop;
	}

	public int getCompoundDrawablePaddingBottom() {
		if (mDrawables == null) {
			return -1;
		}
		return mDrawables.mCompoundPaddingBottom;
	}

	public int getCompoundDrawablePaddingLeft() {
		if (mDrawables == null) {
			return -1;
		}
		return mDrawables.mCompoundPaddingLeft;
	}

	public int getCompoundDrawablePaddingRight() {
		if (mDrawables == null) {
			return -1;
		}
		return mDrawables.mCompoundPaddingRight;
	}

	public void setPadding(int left, int top, int right, int bottom) {
		super.setPadding(left, top, right, bottom);
		updateLayoutPadding(true);
	}

	private void updateLayoutPadding(boolean layout) {
		int mBottom = getPaddingBottom();
		int mTop = getPaddingTop();
		int mLeft = getPaddingLeft();
		int mRight = getPaddingRight();
		if (mDrawables != null) {
			mBottom += mDrawables.getPaddingBottom();
			mLeft += mDrawables.getPaddingLeft();
			mRight += mDrawables.getPaddingRight();
			mTop += mDrawables.getPaddingTop();
		}
		if (mAttr == null) {
			mAttr = new LayoutAttrubute();
		}
		if (mAttr.paddingBottom != mBottom || mAttr.paddingTop != mTop) {
			mAttr.paddingBottom = mBottom;
			mAttr.paddingTop = mTop;
		}
		if (mAttr.paddingLeft != mLeft || mAttr.paddingRight != mRight) {
			mAttr.paddingLeft = mLeft;
			mAttr.paddingRight = mRight;
			if (layout) {
				requestLayout();
				invalidate();
			}
		}
	}

	/**
	 * Returns the top padding of the view, plus space for the top Drawable if
	 * any.
	 */
	public int getCompoundPaddingTop() {
		return mAttr.paddingTop;
	}

	/**
	 * Returns the bottom padding of the view, plus space for the bottom
	 * Drawable if any.
	 */
	public int getCompoundPaddingBottom() {
		return mAttr.paddingBottom;
	}

	/**
	 * Returns the left padding of the view, plus space for the left Drawable if
	 * any.
	 */
	public int getCompoundPaddingLeft() {
		return mAttr.paddingLeft;
	}

	/**
	 * Returns the right padding of the view, plus space for the right Drawable
	 * if any.
	 */
	public int getCompoundPaddingRight() {
		return mAttr.paddingRight;
	}

	protected TextAreaLayout getDefaultLayoutAide(LayoutAttrubute l, CharSequence text, TextAreaPaint paint) {
		return new SingleMiddleLayout(l, text, paint, true);
	}

	public void setLineSpacingAdd(int add) {
		if (mAttr.spacingAdd != add) {
			mAttr.spacingAdd = add;
			if (mLayout != null && mLayout.getLineRecyleCount() > 0) {
				mLayout.restartMeasure(mLayout.getLineStart(0), this);
				CursorDevicer cd = mCursorDevicer;
				CursorHelper ch = mCursorHelper;
				if (cd != null && ch != null) {
					ch.requestMoveCursor(cd, this);
				}
				reviseGrivaty();
			}
		}
	}

	public void setLineSpacingMult(int mult) {
		if (mAttr.spacingMult != mult) {
			mAttr.spacingMult = mult;
			if (mLayout != null && mLayout.getLineRecyleCount() > 0) {
				CursorDevicer cd = mCursorDevicer;
				CursorHelper ch = mCursorHelper;
				mLayout.restartMeasure(mLayout.getLineStart(0), this);
				if (cd != null && ch != null) {
					ch.requestMoveCursor(cd, this);
				}
				reviseGrivaty();
			}
		}
	}

	public int getPaddingHorizontal() {
		return mAttr.paddingLeft + mAttr.paddingRight;
	}

	public int getPaddingVertical() {
		return mAttr.paddingTop + mAttr.paddingBottom;
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int width = -1;
		int height = -1;
		assumeLayout();
		if (heightMode != MeasureSpec.EXACTLY) {
			/**
			 * 虽然我们支持ScrollView,但是,我们并不建议大文本使用ScrollView,因为将极大的影响性能,
			 * 如果你仅仅想要想增加下引性能,请看
			 * {@link FormatLayout#bottomToIndex(CharacterScroller, int)}
			 * 方法,相应的进行必要重写
			 */
			if (heightSize == 0) {
				heightSize = Integer.MAX_VALUE;
			}
		}
		if (mDrawables != null) {
			if ((heightSize != Integer.MAX_VALUE && mDrawables.scaleLevel)
					|| (!mDrawables.scaleLevel && widthSize != Integer.MAX_VALUE)) {
				if (mDrawables.onSizeChange(widthSize, heightSize)) {
					updateLayoutPadding(false);
				}
			}
		}
		requestLayoutSize(getSelectionEnd(), widthSize, heightSize);
		if (mText == null || mText.length() <= 0) {
			refreshHintLayout(widthSize, heightSize);
		}
		if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize;
		} else {
			if (canShowHint()) {
				width = mHintLayout.getWidth() + getPaddingHorizontal();
			} else {
				width = mLayout.getWidth() + getPaddingHorizontal();
			}
		}
		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		} else {
			if (canShowHint()) {
				height = mHintLayout.getHeight() + getPaddingVertical();
			} else {
				height = mLayout.getHeight() + getPaddingVertical();
			}
		}
		registerForPreDraw();
		setMeasuredDimension(width, height);
	}

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		CursorHelper ch = getCursorHelper();
		CharSequence text = getEditableText();
		// Log.e(TAG, "onLayout>"+text+" "+ch);
		if (ch != null && mCursorDevicer != null) {
			if (text == null || text.length() == 0 || Selection.getSelectionEnd(text) < 0) {
				ch.requestMoveCursor(mCursorDevicer, this);
			}
		}
		reviseGrivaty();
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.e(TAG, "sizeChanged");
	}

	public void requestLayout() {
		stopFling();
		super.requestLayout();
	}

	protected void restartLayout() {
		stopFling();
		boolean update = false;
		if (mLayout != null && !mLayout.restartLayout(this)) {
			update = true;
		}
		update |= reviseGrivaty();
		CursorHelper ch = getCursorHelper();
		if (ch != null) {
			update = !ch.requestMoveCursor(mCursorDevicer, this);
		}
		if (update) {
			invalidate();
		}
	}

	protected boolean requestLayoutSize(int point, int w, int h) {
		if (mLayout != null) {
			return mLayout.restartLayoutSize(point, w, h, this);
		}
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	protected class FlingRunnable implements Runnable {

		private float mStart;
		private float mEnd;
		private float mBin;
		private float mTime = 0.05f;
		private int mRun;
		private boolean stop;
		private boolean scrollDire;

		public FlingRunnable() {
		}

		protected void stop() {
			stop = true;
			removeCallbacks(this);
		}

		public void start(float start, float end, boolean dir, float maxFriction, float trackValue) {
			stop = false;
			mStart = start;
			mEnd = end;
			mBin = (start + end) / 2;

			if (start < end) {
				mRun = (int) end;
			} else {
				mRun = (int) start;
			}
			scrollDire = dir;
			execute();
		}

		protected void execute() {
			try {
				postOnAnimation(this);
			} catch (Throwable t) {
				post(this);
			}
		}

		public void run() {
			if (stop) {
				return;
			}
			int sRun = getRun();
			if (sRun == mRun) {
				stop();
				return;
			}
			if (mEnd > mStart) {
				if (sRun > mEnd) {
					stop();

					return;
				}
			} else {
				if (sRun <= mEnd) {
					stop();
					return;
				}
			}
			mRun = sRun;
			// mTime += 0.01f;
			mStart = mRun;
			mBin = (mStart + mEnd) / 2;
			// mLayout.getLineCount());
			if (scrollDire) {
				if (startScrollY(mRun)) {
					execute();
					return;
				}
			} else {
				if (startScrollX(mRun)) {
					execute();
					return;
				}
			}
			stop();
		}

		private int getRun() {
			return (int) Math
					.ceil(Math.pow(1 - mTime, 2) * mStart + 2 * mTime * (1 - mTime) * mBin + Math.pow(mTime, 2) * mEnd);
		}

	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mLayout != null) {
			mLayout.clear();
			mLayout = null;
		}
		if (mCursorDevicer != null) {
			mCursorDevicer.setVisibility(false);
			mCursorDevicer = null;
		}
		if (mDrawables != null) {
			mDrawables.mBottom = mDrawables.mLeft = mDrawables.mTop = mDrawables.mRight = null;
			mDrawables = null;
		}
		if (mAttr != null) {
			mAttr.lineFormat = null;
			mAttr = null;
		}
		mInfo = null;
		imm = null;
		mPaint = null;
	}

	private class ITextFilter implements InputFilter {

		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			if (source == null) {
				return source;
			}
			if (mInputFileters != null) {
				for (InputFilter inputFilter : mInputFileters) {
					source = inputFilter.filter(source, start, end, dest, dstart, dend);
				}
				if (source == null) {
					return source;
				}
			}
			if (end > source.length()) {
				end = source.length();
			}
			if (start < end) {
				if (mKeyListener instanceof InputFilter) {
					source = ((InputFilter) mKeyListener).filter(source, start, end, dest, dstart, dend);
				} else if (mKeyListener != null) {
					int type = mKeyListener.getInputType();
					TextKeyListener.Capitalize cap;
					if ((type & EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
						cap = TextKeyListener.Capitalize.CHARACTERS;
						return source.toString().toUpperCase(Locale.ENGLISH);
					} else if ((type & EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS) != 0) {
						cap = TextKeyListener.Capitalize.WORDS;
					} else if ((type & EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
						cap = TextKeyListener.Capitalize.SENTENCES;
					} else {
						cap = TextKeyListener.Capitalize.NONE;
					}
					// if (cap != TextKeyListener.Capitalize.NONE) {
					// StringBuilder mStringBuilder = null;
					// for (int i = start; i < end; i++) {
					// if (TextKeyListener.shouldCap(cap, source, i)) {
					// if (mStringBuilder == null) {
					// mStringBuilder = new StringBuilder(source);
					// }
					// mStringBuilder.setCharAt(i,
					// Character.toUpperCase(source.charAt(i)));
					// }
					// }
					// if (mStringBuilder != null) {
					// return mStringBuilder;
					// }
					// }
				}
			}
			return source;
		}

	}

	private class ITextSpanListener implements SpanWatcher {

		@Override
		public void onSpanAdded(Spannable text, Object what, int start, int end) {
			checkSelectionChanged(text, what);
		}

		private void checkSelectionChanged(Spannable text, Object what) {
			if (what == Selection.SELECTION_END) {
				if (!isTextSelectable()) {
					int start = Selection.getSelectionStart(text);
					int end = Selection.getSelectionEnd(text);
					if (start != end) {
						Selection.setSelection(text, end);
						return;
					}
				}
				CursorHelper mCursorMove = getCursorHelper();
				if (mCursorMove != null && !mCursorMove.isSelecting()) {
					mCursorMove.requestMoveCursor(mCursorDevicer, TextAreaView.this);
					updateCursorToVisibileScreen();
				}
				invalidate();
			}
		}

		@Override
		public void onSpanRemoved(Spannable text, Object what, int start, int end) {
		}

		@Override
		public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend) {
			checkSelectionChanged(text, what);
		}
	}

	private class ITextChangListener implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int end, int count) {
			if (mLayout != null) {
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "Textchange");
				}
				boolean handInvalidate = !mLayout.restartMeasure(isSingleLine() ? Selection.getSelectionEnd(s) : start,
						TextAreaView.this);
				if (mLayout.getHeight() + getPaddingVertical() != getHeight()
						|| mLayout.getWidth() + getPaddingHorizontal() != getWidth()) {
					requestLayout();
				}
				handInvalidate |= !reviseGrivaty();
				CursorHelper mCursorMove = getCursorHelper();
				if (mCursorMove == null) {
					if (handInvalidate) {
						invalidate();
					}
					refreshHintLayout(getWidth(), getHeight());
					return;
				}
				handInvalidate |= !mCursorMove.requestMoveCursor(mCursorDevicer, TextAreaView.this);
				handInvalidate |= !updateCursorToVisibileScreen();
				if (handInvalidate) {
					invalidate();
				}
				refreshHintLayout(getWidth(), getHeight());
			}
		}

		@Override
		public void afterTextChanged(Editable s) {
		}

	}

	protected static class Drawables {

		private int mCompoundPaddingTop;
		private int mCompoundPaddingBottom;
		private int mCompoundPaddingLeft;
		private int mCompoundPaddingRight;

		private Drawable mLeft;
		private Drawable mTop;
		private Drawable mRight;
		private Drawable mBottom;

		float leftWidth = -1, leftHeight = -1;
		float rightWidth = -1, rightHeight = -1;
		float topWidth = -1, topHeight = -1;
		float bottomWidth = -1, bottomHeight = -1;

		boolean scaleLevel;

		public void setDrawablePadding(int left, int top, int right, int bottom) {
			mCompoundPaddingTop = top;
			mCompoundPaddingBottom = bottom;
			mCompoundPaddingLeft = left;
			mCompoundPaddingRight = right;
		}

		private int getPaddingTop() {
			if (mTop != null) {
				return mCompoundPaddingTop + mTop.getBounds().height();
			}
			return 0;
		}

		private int getPaddingBottom() {
			if (mBottom != null) {
				return mCompoundPaddingBottom + mBottom.getBounds().height();
			}
			return 0;
		}

		private int getPaddingLeft() {
			if (mLeft != null) {
				return mCompoundPaddingLeft + mLeft.getBounds().width();
			}
			return 0;
		}

		private int getPaddingRight() {
			if (mRight != null) {
				return mCompoundPaddingRight + mRight.getBounds().width();
			}
			return 0;
		}

		private static final int EXACT = 5000000;

		private void draw(Canvas canvas, View v) {
			int left = v.getPaddingLeft(), right = v.getPaddingRight(), top = v.getPaddingTop(),
					bottom = v.getPaddingBottom();
			int w = v.getWidth(), h = v.getHeight(), x = v.getScrollX(), y = v.getScrollY();
			float tx = left + (w - left - right) / 2f + x;
			float ty = (h - top - bottom) / 2f + top + y;
			if (mTop != null) {
				canvas.save();
				canvas.translate(tx - mTop.getBounds().width() / 2f, y + top);
				mTop.draw(canvas);
				canvas.restore();
			}
			if (mBottom != null) {
				canvas.save();
				canvas.translate(tx - mBottom.getBounds().width() / 2f, h - bottom + y - mBottom.getBounds().height());
				mBottom.draw(canvas);
				canvas.restore();
			}
			if (mRight != null) {
				canvas.save();
				canvas.translate(w - right + x - mRight.getBounds().width(), ty - mRight.getBounds().height() / 2f);
				mRight.draw(canvas);
				canvas.restore();
			}
			if (mLeft != null) {
				canvas.save();
				canvas.translate(left + x, ty - mLeft.getBounds().height() / 2f);
				mLeft.draw(canvas);
				canvas.restore();
			}
		}

		public boolean verifyDrawable(Drawable who) {
			return who == mBottom || who == mLeft || who == mRight || who == mTop;
		}

		public void jumpDrawablesToCurrentState() {
			if (mBottom != null) {
				mBottom.jumpToCurrentState();
			}
			if (mTop != null) {
				mTop.jumpToCurrentState();
			}
			if (mLeft != null) {
				mLeft.jumpToCurrentState();
			}
			if (mRight != null) {
				mRight.jumpToCurrentState();
			}
		}

		private boolean onSizeChange(int w, int h) {
			boolean need = false;
			if (mLeft != null) {
				need |= updateBounds(w, h, leftWidth, leftHeight, mLeft);
			}
			if (mRight != null) {
				need |= updateBounds(w, h, rightWidth, rightHeight, mRight);
			}
			if (mTop != null) {
				need |= updateBounds(w, h, topWidth, topHeight, mTop);
			}
			if (mBottom != null) {
				need |= updateBounds(w, h, bottomWidth, bottomHeight, mBottom);
			}
			return need;
		}

		private boolean updateBounds(int w, int h, float scaleW, float scaleH, Drawable draw) {
			if (scaleW >= 0 && scaleH >= 0) {
				int baseLevel;
				if (scaleLevel) {
					baseLevel = h;
				} else {
					baseLevel = w;
				}
				if (scaleW > 0 && scaleW <= 1) {
					w = (int) (scaleW * baseLevel);
				} else if (scaleW <= 0) {
					w = 0;
				} else {
					w = (int) scaleW;
				}
				if (scaleH > 0 && scaleH <= 1) {
					h = (int) (scaleH * baseLevel);
				} else if (scaleH <= 0) {
					h = 0;
				} else {
					h = (int) scaleH;
				}
				Rect oldRect = draw.getBounds();
				if (oldRect.left != 0 || oldRect.right != w || oldRect.top != 0 || oldRect.bottom != h) {
					draw.setBounds(0, 0, w, h);
					return true;
				}
			}
			return false;
		}

		private void setDrawablesBounds(float leftWidth, float leftHeight, float rightWidth, float rightHeight,
				float topWidth, float topHeight, float bottomWidth, float bottomHeight, boolean scaleLevel) {
			this.leftWidth = leftWidth;
			this.leftHeight = leftHeight;
			this.rightHeight = rightHeight;
			this.rightWidth = rightWidth;
			this.topHeight = topHeight;
			this.topWidth = topWidth;
			this.bottomHeight = bottomHeight;
			this.bottomWidth = bottomWidth;
			this.scaleLevel = scaleLevel;
		}

		private void setDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom, int[] state,
				Callback cb) {
			if (left != mLeft) {
				if (mLeft != null) {
					mLeft.setCallback(null);
				}
				if (left != null) {
					left.setCallback(cb);
					left.setState(state);
				}
				mLeft = left;
			}
			if (mTop != top) {
				if (mTop != null) {
					mTop.setCallback(null);
				}
				if (top != null) {
					top.setCallback(cb);
					top.setState(state);
				}
				mTop = top;
			}
			if (mRight != right) {
				if (mRight != null) {
					mRight.setCallback(null);
				}
				if (right != null) {
					right.setCallback(cb);
					right.setState(state);
				}
				mRight = right;
			}
			if (mBottom != bottom) {
				if (mBottom != null) {
					mBottom.setCallback(null);
				}
				if (bottom != null) {
					bottom.setCallback(cb);
					bottom.setState(state);
				}
				mBottom = bottom;
			}
		}

		/**
		 * Returns the top padding of the view, plus space for the top Drawable
		 * if any.
		 */
		public int getCompoundPaddingTop() {
			return mCompoundPaddingTop;
		}

		/**
		 * Returns the bottom padding of the view, plus space for the bottom
		 * Drawable if any.
		 */
		public int getCompoundPaddingBottom() {
			return mCompoundPaddingBottom;
		}

		/**
		 * Returns the left padding of the view, plus space for the left
		 * Drawable if any.
		 */
		public int getCompoundPaddingLeft() {
			return mCompoundPaddingLeft;
		}

		/**
		 * Returns the right padding of the view, plus space for the right
		 * Drawable if any.
		 */
		public int getCompoundPaddingRight() {
			return mCompoundPaddingRight;
		}

		/**
		 * Returns the extended top padding of the view, including both the top
		 * Drawable if any and any extra space to keep more than maxLines of
		 * text from showing. It is only valid to call this after measuring.
		 */
		// public int getExtendedPaddingTop() {
		// // if (mMaxMode != LINES) {
		// // return getCompoundPaddingTop();
		// // }
		//
		// if (mLayout == null) {
		// assumeLayout();
		// }
		//
		// if (mLayout.getLineCount() <= mMaximum) {
		// return getCompoundPaddingTop();
		// }
		//
		// int top = getCompoundPaddingTop();
		// int bottom = getCompoundPaddingBottom();
		// int viewht = getHeight() - top - bottom;
		// int layoutht = mLayout.getLineTop(mMaximum);
		//
		// if (layoutht >= viewht) {
		// return top;
		// }
		//
		// final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
		// if (gravity == Gravity.TOP) {
		// return top;
		// } else if (gravity == Gravity.BOTTOM) {
		// return top + viewht - layoutht;
		// } else { // (gravity == Gravity.CENTER_VERTICAL)
		// return top + (viewht - layoutht) / 2;
		// }
		// }
		//
		// public int getExtendedPaddingBottom() {
		//
		// if (mLayout == null) {
		// assumeLayout();
		// }
		//
		// if (mLayout.getLineCount() <= mMaximum) {
		// return getCompoundPaddingBottom();
		// }
		//
		// int top = getCompoundPaddingTop();
		// int bottom = getCompoundPaddingBottom();
		// int viewht = getHeight() - top - bottom;
		// int layoutht = mLayout.getLineTop(mMaximum);
		//
		// if (layoutht >= viewht) {
		// return bottom;
		// }
		//
		// final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
		// if (gravity == Gravity.TOP) {
		// return bottom + viewht - layoutht;
		// } else if (gravity == Gravity.BOTTOM) {
		// return bottom;
		// } else { // (gravity == Gravity.CENTER_VERTICAL)
		// return bottom + (viewht - layoutht) / 2;
		// }
		// }
	}

	public interface OnImeActionListener {

		public void onSearchAction(TextAreaView tav);

		public void onDoneAction(TextAreaView tav);

		public void onSendAction(TextAreaView tav);

		public void onGoAction(TextAreaView tav);

	}
}
