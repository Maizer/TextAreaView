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
 * @author Maizer/����
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
	 * �еײ�
	 * 
	 * @return
	 */
	int getBottom();

	/**
	 * �ж���
	 * 
	 * @return
	 */
	int getTop();

	/**
	 * �Զ���ĵײ�ƫ��ֵ,Ӧ�ô����ж���,С������OffsetBottom
	 * 
	 * @return offsetTop
	 */
	int getOffsetTop();

	/**
	 * �Զ���ĵײ�ƫ��ֵ,Ӧ��С���еײ�,��������ײ�
	 * 
	 * @return offsetbottom
	 */
	int getOffsetBottom();

	/**
	 * ���ڻ��ߵ�Below{@link TextPaint#descent()}
	 * 
	 * @return
	 */
	int getBelow();

	/**
	 * ���ڻ��ߵ�Above{@link TextPaint#ascent()}
	 * 
	 * @return
	 */
	int getAbove();

	/**
	 * ��ȡ�и�
	 * 
	 * @return �и�
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
	 * ��start��ʼ����,ֱ��������>hori ��site
	 * 
	 * @param hori
	 *            ˮƽ����
	 * @return ���ش�start�������ɹ���count
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
	 *            ˮƽ����
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
	 *            ��Ҫ�ĵ�horizontal location ��index
	 * @param letterSpac
	 * @param setSelect
	 *            �Ƿ����selectģʽ
	 * @param isSelectt
	 *            �Ƿ�Ϊselectģʽ
	 * @return horizontal location
	 */
	float getHorizontalForOffset(CharSequence text, int offset, float letterSpac, boolean setSelect, boolean isSelectt);

	/**
	 * exact horizontal location,letter to next latter center location
	 */
	float getExactHorizontalForHorizontal(CharSequence text, float horiz, float letterSpac, boolean setSelec,
			boolean isSelectt);

	/**
	 * ˮƽ�ƶ���
	 * 
	 * @param scrollX
	 *            scroll X �ľ���
	 * @param text
	 *            ��ǰ�����ֶ���
	 * @param wp
	 *            paint����
	 * @param measurer
	 *            ��������
	 * @return �����Ϊ-1,�㵽��������ƶ��ı߽�
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
	 * ����������
	 * 
	 * @param minStart
	 *            ��С�����
	 * @param maxEnd
	 *            �����յ�
	 * @param start
	 *            ���ڵ����
	 * @param end
	 *            ���ڵ����
	 * @param above
	 * @param below
	 * @param bottom
	 *            �еĵͶ�
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
