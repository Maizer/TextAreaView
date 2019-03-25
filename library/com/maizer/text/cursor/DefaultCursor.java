package com.maizer.text.cursor;

import java.util.Timer;
import java.util.TimerTask;

import com.maizer.compater.ViewCompater;
import com.maizer.text.layout.TextAreaScroller;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class DefaultCursor implements CursorDevicer {

	private static final String TAG = DefaultCursor.class.getCanonicalName();

	private static final int DEFAULT_NO_SET = -1;

	private static final int DEFAULT_MAX_DELAYED = 1000;

	private static final int DEFAULT_MIN_DELAYED = 100;

	private static final int DEFAULT_MAX_ALPHA = 255;

	private static final int DEFAULT_MIN_ALPHA = 0;

	private int default_width = 3;

	private View mView;

	private Flicker mFlicker;

	private Drawable mCursor;

	private OnCursorLayoutListener mListener;

	private boolean mVisible = true;

	private int mWidth = default_width, mHeight = 50;

	private int mX, mY;

	private int dX, dY;

	private AnimationThread mAnimationThread;

	public DefaultCursor(View aide) {
		mView = aide;
		mAnimationThread = new AnimationThread();
	}

	private void fickCursor(boolean refreshAll, int lock, int alpha) {
		if (!isVisibility()) {
			return;
		}
		if (lock != DEFAULT_NO_SET && alpha != DEFAULT_NO_SET) {
			mFlicker.setAlpha(alpha);
			mFlicker.lock(lock);
		}
		if (refreshAll) {
			mView.invalidate();
			return;
		}
		if (mView instanceof TextAreaScroller) {
			TextAreaScroller scroller = (TextAreaScroller) mView;
			int scrollX = scroller.getTextScrollX() - mView.getScrollX();
			int scrollY = scroller.getTextScrollY() - mView.getScrollY();
			mView.invalidate(this.mX + dX - scrollX, this.mY + dY - scrollY, this.mX + mWidth + dX - scrollX,
					this.mY + mHeight + dY - scrollY);
		} else {
			mView.invalidate(this.mX + dX, this.mY + dY, this.mX + mWidth + dX, this.mY + mHeight + dY);
		}
	}

	private void fickCursor(int x, int y, boolean refreshAll, int lock, int alpha) {
		if (!isVisibility()) {
			return;
		}
		if (lock != DEFAULT_NO_SET && alpha != DEFAULT_NO_SET) {
			mFlicker.setAlpha(alpha);
			mFlicker.lock(lock);
		}
		if (refreshAll) {
			mView.invalidate();
			return;
		}

		int startX = x;
		int startY = y;
		int endX = x;
		int endY = y;

		if (x > mX) {
			startX = mX;
		} else {
			endX = mX;
		}

		if (y > mY) {
			startY = mY;
		} else {
			endY = mY;
		}

		if (mView instanceof TextAreaScroller) {
			TextAreaScroller scroller = (TextAreaScroller) mView;
			int scrollX = scroller.getTextScrollX() - mView.getScrollX();
			int scrollY = scroller.getTextScrollY() - mView.getScrollY();
			mView.invalidate(startX + dX - scrollX, startY + dY - scrollY, endX + mWidth + dX - scrollX,
					endY + mHeight + dY - scrollY);
		} else {
			mView.invalidate(startX + dX, startY + dY, endX + mWidth + dX, endY + mHeight + dY);
		}
	}

	public void draw(Canvas canvas) {
		if (!isVisibility()) {
			return;
		}
		if (mFlicker != null) {
			mCursor.setAlpha(mFlicker.mAlpha);
			mCursor.setBounds(mX + dX, mY + dY, mX + mWidth + dX, mY + mHeight + dY);
			mCursor.draw(canvas);
		}
	}

	public final void setCursorDrawable(Drawable draw) {
		if (draw == null) {
			throw new NullPointerException("Cursor Drawable not null!");
		}
		if (draw != mCursor) {
			mCursor = draw;
		}
	}

	public void prepareFlicking() {
		if (mVisible && mFlicker == null) {
			mFlicker = new Flicker();
		}
	}

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	protected Drawable getDrfaultCursorDrawable() {
		ColorDrawable cd = new ColorDrawable(Color.BLUE);
		return cd;
	}

	@Override
	public boolean isVisibility() {
		return mFlicker != null;
	}

	@Override
	public void setVisibility(boolean v) {
		if (mVisible != v) {
			mVisible = v;
			if (v) {
				if (mFlicker == null) {
					mFlicker = new Flicker();
				}
			} else if (mFlicker != null) {
				mAnimationThread.abortAnimation();
				mFlicker.remove();
				mFlicker = null;
			}
		}
	}

	@Override
	public void requestUpdate(int top, int bottom, int x, int y, float maxWidth, boolean refreshAll,
			boolean sendUpdate) {
		prepareFlicking();
		fickCursor(refreshAll, DEFAULT_NO_SET, DEFAULT_NO_SET);
		if (bottom > top) {
			mHeight = bottom - top;
		}
		if (mWidth < default_width || maxWidth < default_width) {
			mWidth = default_width;
		} else if (mWidth > maxWidth) {
			mWidth = (int) maxWidth;
		}
		// this.mY = (int) y;
		// this.mX = x == 0 ? 0 : (int) (x - mWidth / 2);
		// fickCursor(refreshAll, DEFAULT_MAX_DELAYED, DEFAULT_MAX_ALPHA);
		mAnimationThread.startAnimationMove(this.mX, this.mY, x == 0 ? 0 : (int) (x - mWidth / 2), y, refreshAll);
		if (sendUpdate && mListener != null) {
			mListener.updatedCursorLayout(mY + dY, mX + dX, dY + mY + mHeight, dX + mX + mWidth);
		}
	}

	@Override
	public void requestUpdate(int x, int y, boolean sendUpdate) {
		requestUpdate(-1, -1, x, y, mWidth, false, sendUpdate);
	}

	@Override
	public int getCurrX() {
		return mX;
	}

	@Override
	public int getCurrY() {
		return mY;
	}

	@Override
	public void setDefaultMinWidth(int w) {
		default_width = w;
	}

	public void setWidth(int w) {
		mWidth = w;
	}

	@Override
	public void getBounds(Rect rect) {
		if (rect == null) {
			return;
		}
		rect.left = mX;
		rect.top = mY;
		rect.right = mX + mWidth;
		rect.bottom = mY + mHeight;
	}

	@Override
	public void setLayoutListener(OnCursorLayoutListener l) {
		mListener = l;
	}

	private class AnimationThread implements Runnable {

		private int mStartX;
		private int mToX;

		private int mStartY;
		private int mToY;
		private boolean refreshAll;
		private Scroller mScroller;
		private Interpolator mInterpolator = new DecelerateInterpolator(1f);

		public AnimationThread() {
			mScroller = new Scroller(mView.getContext(), mInterpolator);
		}

		public void abortAnimation() {
			mScroller.abortAnimation();
			mX = mToX;
			mY = mToY;
		}

		public void startAnimationMove(int sx, int sy, int tx, int ty, boolean refresh) {

			refreshAll = refresh;
			mStartX = sx;
			mToX = tx;
			mStartY = sy;
			mToY = ty;
			mScroller.abortAnimation();
			int dx = mToX - mStartX;
			int dy = mToY - mStartY;

			int height = mView.getHeight();

			if (Math.abs(dy) > height) {

				if (mStartY < mToY) {
					sy = mStartY = mToY - height;
				} else {
					sy = mStartY = mToY + height;
				}
				dy = height;

			}

			mScroller.fling(0, 0, Math.abs(dx), Math.abs(dy), 0, 7000, 0, 7000);
			mScroller.startScroll(mStartX, mStartY, dx, dy, mScroller.getDuration());
			mView.removeCallbacks(this);
			mView.post(this);
		}

		@Override
		public void run() {
			if (mScroller.computeScrollOffset()) {
				int oldX = mX;
				int oldY = mY;
				int x = mScroller.getCurrX();
				int y = mScroller.getCurrY();
				mX = x;
				mY = y;
				if (mScroller.isFinished()) {
					mX = mToX;
					mY = mToY;
				}
				if (mView != null) {
					fickCursor(oldX, oldY, refreshAll, DEFAULT_MAX_DELAYED, DEFAULT_MAX_ALPHA);
					ViewCompater.postOnAnimation(mView, this);
				} else {
					mScroller.abortAnimation();
					mX = mToX;
					mY = mToY;
				}
			}
		}
	}

	protected class Flicker implements Runnable {

		private Timer mTimer = new Timer();

		private TimerTask mTask;

		private int mAlpha = DEFAULT_MAX_ALPHA;

		private boolean sequence;

		private boolean die;

		private boolean isLock;

		protected void setAlpha(int alpha) {
			mAlpha = alpha;
		}

		protected void lock(int millisecond) {
			if (millisecond > 0) {
				isLock = true;
				startWaitFick(millisecond);
			}
		}

		protected int getAlpha() {
			return mAlpha;
		}

		Flicker() {
			start();
		}

		void start() {
			if (mCursor == null) {
				mCursor = getDrfaultCursorDrawable();
			}
			boolean b = mView.post(this);
		}

		void remove() {
			mView.removeCallbacks(this);
			die = true;
		}

		@Override
		public void run() {
			if (!isVisibility() || die || isLock) {
				return;
			}
			if (!sequence) {
				mAlpha -= 10;
				if (mAlpha <= DEFAULT_MIN_ALPHA) {
					sequence = true;
					mAlpha = DEFAULT_MIN_ALPHA;
				}
			} else {
				mAlpha += 10;
				if (mAlpha >= DEFAULT_MAX_ALPHA) {
					sequence = false;
					mAlpha = DEFAULT_MAX_ALPHA;
				}
			}
			fickCursor(false, DEFAULT_NO_SET, DEFAULT_NO_SET);
			if (mAlpha == 0) {
				mView.postDelayed(this, DEFAULT_MIN_DELAYED);
			} else {
				mView.post(this);
			}
		}

		private class TimeTask extends TimerTask {

			@Override
			public void run() {
				if (isLock) {
					isLock = false;
					mView.removeCallbacks(Flicker.this);
					mView.post(Flicker.this);
				}
			}

		}

		private void startWaitFick(int time) {
			if (mTask != null) {
				mTask.cancel();
			}
			mTask = new TimeTask();
			mView.removeCallbacks(Flicker.this);
			mTimer.schedule(mTask, time);
		}

	}

	@Override
	public void setDefaultMoveLevel(int x, int y) {
		dX = x;
		dY = y;
		fickCursor(false, DEFAULT_NO_SET, DEFAULT_NO_SET);
	}

	@Override
	public boolean checkView(View view) {
		if (mView != view) {
			if (mView != null) {
				mView.clearFocus();
				mView.invalidate();
			}
			mAnimationThread.abortAnimation();
			if (mFlicker != null) {
				mFlicker.remove();
			}
			mFlicker = null;
			mVisible = true;
			mView = view;
			return true;
		}
		return false;
	}

	public boolean isViewFocus(View view) {
		return mView == view;
	}

	@Override
	public void setColor(int color) {
		if (mCursor == null) {
			mCursor = getDrfaultCursorDrawable();
		}
		if (mCursor instanceof ColorDrawable) {
			((ColorDrawable) mCursor).setColor(color);
		}
	}

}
