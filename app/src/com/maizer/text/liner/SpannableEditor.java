package com.maizer.text.liner;

import com.maizer.text.layout.TextAreaPaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.ScaleXSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.widget.TextView;

/**
 * ≤‚ ‘Spannable
 * 
 * @author Maizer/¬Û‘Û
 *
 */
public class SpannableEditor extends SpannableStringBuilder {

	@Override
	public void getChars(int start, int end, char[] dest, int destoff) {
		super.getChars(start, end, dest, destoff);
	}

	public void setSpan(Object what, int start, int end, int flags) {
		super.setSpan(what, start, end, flags);
	}

	@Override
	public int nextSpanTransition(int start, int limit, Class kind) {
		return super.nextSpanTransition(start, limit, kind);
	}

	public SpannableStringBuilder replace(final int start, int end, CharSequence tb, int tbstart, int tbend) {
		SpannableStringBuilder ssb = super.replace(start, end, tb, tbstart, tbend);
		return ssb;
	}

	@Override
	public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind) {
		return super.getSpans(queryStart, queryEnd, kind);
	}

	private static final String TAG = SpannableEditor.class.getSimpleName();

	public SpannableStringBuilder append(CharSequence text) {
		SpannableStringBuilder seb = super.append(text);
		return seb;
	}

	public void setCursorLocation(int l) {
		// replace(l, l, "0");
		Object obj = new ForegroundColorSpan(Color.RED);
		Object obj1 = new ScaleXSpan(5);
		Object obj2 = new AbsoluteSizeSpan(90);
		Object obj3 = new LeadingMarginSpan.LeadingMarginSpan2() {

			@Override
			public int getLeadingMargin(boolean first) {
				if (first) {
					return 100;
				}
				return 0;
			}

			@Override
			public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
					CharSequence text, int start, int end, boolean first, Layout layout) {
			}

			@Override
			public int getLeadingMarginLineCount() {
				return 2;
			}
		};
		Object obj4 = new UnderlineSpan(){

			@Override
			public void updateDrawState(TextPaint ds) {
				if(ds instanceof TextAreaPaint){
					TextAreaPaint tap = (TextAreaPaint)ds;
					tap.setUnderlineText(true);
					tap.setUnderLineColor(Color.RED);
					tap.setUnderlineThickness(2);
				}
			}
			
		};
//		Object obj5 = new 
		setSpan(obj, 0, 20, SPAN_EXCLUSIVE_EXCLUSIVE);
		setSpan(obj1, 0, 20, SPAN_EXCLUSIVE_EXCLUSIVE);
		setSpan(obj2, 20, 50, SPAN_EXCLUSIVE_EXCLUSIVE);
		setSpan(obj3, 0, length(), SPAN_EXCLUSIVE_EXCLUSIVE);
		setSpan(obj4, 4, length(), SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	public SpannableEditor(CharSequence text) {
		this(text == null ? text = "" : text, 0, text.length());
	}

	public SpannableEditor(CharSequence text, int start, int end) {
		super(text, start, end);
	}



}
