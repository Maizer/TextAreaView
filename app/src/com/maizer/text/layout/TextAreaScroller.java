package com.maizer.text.layout;

import android.view.View;

/**
 * text scroll manager
 * 
 * @author Maizer/麦泽
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
	 * 附加方法,可能需要
	 * 
	 * @param run
	 * @param delayMillis
	 */
	void postRunnable(Runnable run, long delayMillis);
}
