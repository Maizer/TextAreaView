package com.maizer.text.liner;

import com.maizer.text.layout.TextAreaPaint;
import com.maizer.text.measure.Measurer;
import com.maizer.text.util.ArrayGc;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;

/**
 * 
 * @author Maizer/麦泽
 *
 */
public interface Lineable extends ArrayGc {
	/**
	 * line min start ,if single line return 0
	 * 
	 * @return minstart
	 */
	int getMinStart();

	/**
	 * line min start ,if single line return text length
	 * 
	 * @return minstart
	 */
	int getMaxEnd();

	/**
	 * line current start
	 * 
	 * @return strat
	 */
	int getStart();

	/**
	 * line current end
	 * 
	 * @return end
	 */
	int getEnd();

	/**
	 * 行底部
	 * 
	 * @return
	 */
	int getBottom();

	/**
	 * 行顶部
	 * 
	 * @return
	 */
	int getTop();

	/**
	 * 自定义的底部偏移值,应该大于行顶部,小于上行OffsetBottom
	 * 
	 * @return offsetTop
	 */
	int getOffsetTop();

	/**
	 * 自定义的底部偏移值,应该小于行底部,大于字体底部
	 * 
	 * @return offsetbottom
	 */
	int getOffsetBottom();

	/**
	 * 基于基线的Below{@link TextPaint#descent()}
	 * 
	 * @return
	 */
	int getBelow();

	/**
	 * 基于基线的Above{@link TextPaint#ascent()}
	 * 
	 * @return
	 */
	int getAbove();

	/**
	 * 获取行高
	 * 
	 * @return 行高
	 */
	int getHeight();

	int getLeadingMergin();

	/**
	 * end site
	 * 
	 * @return site
	 */
	float getMaxSite();

	float getEllipsisWidth();

	/**
	 * start site
	 * 
	 * @return site
	 */
	float getMinSite();

	/**
	 * index site , limits>0 to end-start
	 * 
	 * @return site
	 */
	float getSite(int index);

	void removeFirstSite(boolean offset);

	void removeLastSite(boolean offset);

	int getSiteSize();

	/**
	 * 从start开始索引,直到索引到>hori 的site
	 * 
	 * @param hori
	 *            水平距离
	 * @return 返回从start到索引成功的count
	 */
	int getSiteCountByHorizontal(float hori);

	int getSiteSomeCountByOffset(int index);

	/**
	 * @return last letter width
	 */
	float getLastLetterWidth();

	float getFirstLetterWidth();

	/**
	 * found letter location by horiz
	 * 
	 * @param horiz
	 *            水平距离
	 * @return index, limits> start to end
	 */
	int getOffsetForHorizontal(float horiz);

	TruncateAt getTruncateAt();

	void setTruncateAt(String ellipsis, float ellipsisWidth, TruncateAt truncateAt);

	/**
	 * @hide
	 * @param isOpposite
	 */
	void setOppositeDraw(boolean isOpposite);

	/**
	 * @param text
	 * @param offset
	 *            想要的到horizontal location 的index
	 * @param letterSpac
	 * @param setSelect
	 *            是否进入select模式
	 * @param isSelectt
	 *            是否为select模式
	 * @return horizontal location
	 */
	float getHorizontalForOffset(CharSequence text, int offset, float letterSpac, boolean setSelect, boolean isSelectt);

	/**
	 * exact horizontal location,letter to next latter center location
	 */
	float getExactHorizontalForHorizontal(CharSequence text, float horiz, float letterSpac, boolean setSelec,
			boolean isSelectt);

	/**
	 * 水平移动行
	 * 
	 * @param scrollX
	 *            scroll X 的距离
	 * @param text
	 *            当前的文字对象
	 * @param wp
	 *            paint对象
	 * @param measurer
	 *            测量对象
	 * @return 如果不为-1,便到达了最大移动的边界
	 */
	float moveHorizontal(float scrollX, CharSequence text, TextAreaPaint wp, Measurer measurer);

	/**
	 * @param outx
	 *            Where is the drawing of x
	 * @param outy
	 *            Where is the drawing of y
	 */
	void drawText(CharSequence text, Canvas canvas, TextPaint wp, float outx, float outy);

	/**
	 * 
	 * @param spanned
	 *            this is text spanned
	 * @param canvas
	 *            this is canvas
	 * @param wp
	 *            this is TextPaint
	 * @param wePaint
	 *            temp paint
	 * @param background
	 *            Text Highlight Draw
	 * @param bstart
	 *            highlight start
	 * @param bend
	 *            highlight end
	 * @param btop
	 *            highlight top
	 * @param bbottom
	 *            highlight bottom
	 * @param outx
	 *            Where is the drawing of x
	 * @param outy
	 *            Where is the drawing of y
	 * @param letterspacing
	 */
	void drawSpanned(Spanned spanned, Canvas canvas, TextPaint wp, TextPaint wePaint, Drawable background, int bstart,
			int bend, int btop, int bbottom, float outx, float outy, float letterspacing);

	/**
	 * 设置行属性
	 * 
	 * @param minStart
	 *            最小的起点
	 * @param maxEnd
	 *            最大的终点
	 * @param start
	 *            现在的起点
	 * @param end
	 *            现在的起点
	 * @param above
	 * @param below
	 * @param bottom
	 *            行的低端
	 * @return
	 * 
	 */
	int setAttribute(int minStart, int maxEnd, int start, int end, int above, int below, int bottom, LineCompute lc);

	/**
	 * set each letter site by letter widths
	 * 
	 * @param offsetIndentation
	 *            line offset indentation,if -1 = indentation, maybe use for
	 *            horizontal move
	 * @param indentation
	 *            line indentation
	 * @param ws
	 *            widths
	 * @param len
	 *            length
	 * @param letterSpace
	 *            letter space
	 * @param positive
	 *            positive sequence add or reverse order add,maybe use for
	 *            measure line
	 */
	void setSiteByWidths(float offsetIndentation, int indentation, float[] ws, int len, float letterSpace,
			boolean positive);

	public static interface LineCompute {
		int computeLineOffsetTop(int mBottom, int mBelow, int mAbove);

		int computeLineOffsetBottom(int mBottom, int mBelow, int mAbove);

		int computeLineTop(int mBottom, int mBelow, int mAbove);
	}

}
