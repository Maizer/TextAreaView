package com.maizer.text.cursor;

import com.maizer.text.layout.TextAreaScroller;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;

public interface CursorDevicer {
	/**
	 * check or change current linked view
	 * @param view
	 * @return
	 */
	boolean checkView(View view);

	void draw(Canvas canvas);

	/**
	 * prepare flicking cursor
	 */
	void prepareFlicking();

	/**
	 * current cursor x
	 * 
	 * @return
	 */
	int getCurrX();

	/**
	 * current cursor y
	 * 
	 * @return
	 */
	int getCurrY();

	/**
	 * cursor width
	 * @return
	 */
	int getWidth();
	/**
	 * cursor height
	 * @return
	 */
	int getHeight();

	/**
	 * cursor bounds
	 * @param rect bounds
	 */
	void getBounds(Rect rect);

	/**
	 * visibility
	 * @return
	 */
	boolean isVisibility();
	/**
	 * set visibility
	 * @return
	 */
	void setVisibility(boolean v);

	/**
	 * set cursor layout update listener
	 * @param l
	 */
	void setLayoutListener(OnCursorLayoutListener l);

	/**
	 * update cursor location with bounds
	 * 
	 * @param top
	 * @param bottom
	 * @param x
	 * @param y
	 * @param maxWidth
	 * @param refreshAll
	 *            refresh view or only refresh cursor bounds
	 * @param sendUpdate
	 *            notification layout changed
	 */
	void requestUpdate(int top, int bottom, int x, int y, float maxWidth, boolean refreshAll, boolean sendUpdate);

	void requestUpdate(int x,int y,boolean sendUpdate);
	/**
	 * 默认移动基准
	 * default move base
	 * 
	 * @param x
	 * @param y
	 */
	void setDefaultMoveLevel(int x, int y);

	/**
	 * cursor min width
	 * 
	 * @param w
	 */
	void setDefaultMinWidth(int w);

	void setWidth(int w);
	
	void setCursorDrawable(Drawable cursorDraw);
	
	void setColor(int color);
	public interface CursorHelper {
		/**
		 * for compatible {@link Selection#setSelection(Spannable, int, int)}
		 * call {@link Spannable#setSpan(Object, int, int, int)} of after send
		 * span change,maybe need avoid repeat call
		 * 
		 * @return
		 */
		boolean isSelecting();

		/**
		 * touch move cursor method
		 * 
		 * @param cursor
		 *            {@link CursorDevicer}
		 * @param ts
		 *            {@link TextAreaScroller}
		 * @return if request succeed scroll
		 */
		boolean requestMoveCursor(int x, int y, CursorDevicer cursor, TextAreaScroller ts);

		/**
		 * text select {@link Selection#setSelection(Spannable, int, int)} move
		 * cursor method
		 * 
		 * @param cursor
		 *            {@link CursorDevicer}
		 * @param ts
		 *            {@link TextAreaScroller}
		 * @return if request succeed scroll
		 */
		boolean requestMoveCursor(CursorDevicer cursor, TextAreaScroller ts);

		boolean moveDown(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect);

		boolean moveUp(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect);

		boolean moveLeft(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect);

		boolean moveRight(CursorDevicer mCursorAide, TextAreaScroller vAide, boolean isSelect);

	}

	/**
	 * cursor layout update listener
	 * @author Maizer/麦泽
	 */
	public interface OnCursorLayoutListener {
		void updatedCursorLayout(int top, int left, int bottom, int right);
	}

}
