package com.maizer.text.layout;

import android.view.View;

/**
 * text scroll manager
 * 
 * @author Maizer/����
 */
public interface TextAreaScroller {

	public enum ScrollMode {
		TOP, BOTTOM,
	}

	/**
	 * may be is {@link View#getScrollY()} or custom
	 * 
	 * @return scrollY
	 */
	int getTextScrollY();

	/**
	 * may be is {@link View#getScrollX()} or custom
	 * 
	 * @return scrollX
	 */
	int getTextScrollX();

	/**
	 * may be is {@link View#scrollBy(int, int)} or custom
	 * 
	 * @param x
	 * @param y
	 */
	void requestTextScrollBy(int x, int y);

	/**
	 * ���ӷ���,������Ҫ
	 * 
	 * @param run
	 * @param delayMillis
	 */
	void postRunnable(Runnable run, long delayMillis);
}
