package com.maizer.text.measure;

import com.maizer.text.factory.LineFactory;
import com.maizer.text.layout.TextAreaLayout.Alignment;
import com.maizer.text.measure.Measurer.LineFormat;

import android.text.TextUtils;

public abstract class MeasureAttrubute {
	/** this is "..." */
	public static final String ELLIPSIS_NORMAL = "\u2026";
	/** this is ".." */
	public static final String ELLIPSIS_TWO_DOTS = "\u2025";
	/** this is "···" */
	public static final String ELLIPSIS_CENTER = "\u00b7\u00b7\u00b7";
	/** this is "··" */
	public static final String ELLIPSIS_TWO_DOTS_CENTER = "\u00b7\u00b7";
	/** this is "•••" */
	public static final String ELLIPSIS_DENSE = "\u2022\u2022\u2022";
	/** this is "••" */
	public static final String ELLIPSIS_TWO_DOTS_DENSE = "\u2022\u2022";
	/** this is "~~~" */
	public static final String ELLIPSIS_WAVE = "\u007e\u007e\u007e";
	/** this is "~~" */
	public static final String ELLIPSIS_TWO_DOTS_WAVE = "\u007e\u007e";
	/**
	 * @hide
	 */
	public boolean isIncludepad = false;
	/**
	 * @hide
	 */
	public boolean isTrackpad = true;
	/**
	 * 是否将{@link #spacingAdd},{@link #spacingMult}应用到首行
	 */
	public boolean isFirstLineToTopWithSpacing = true;

	public float spacingMult = 1;
	public float spacingAdd = 0;

	/**
	 * 如果为-1,将自由扩展高度,但是最大高度极限应为view的height 与{@link #maxLines}
	 * 不同的是,maxLines会限制最大行数,而此参数只会限制其高度
	 */
	public int maxLinesLimitHeight;
	/**
	 * 如果为1,便为单行
	 */
	public int maxLines;
	/**
	 * 最大的文字限制,如果限制为-1,则ems将受限于宽度
	 */
	public int maxEms;
	public int width;

	public String ellipsize = ELLIPSIS_NORMAL;

	public TextUtils.TruncateAt truncateAt;
	public Alignment alignment;

	public LineFormat lineFormat = LineFormat.getInstance();
	public LineFactory lineFactory = LineFactory.getInstance();

	/**
	 * 此为保存行起点数组专用初始大小,一般不需要更改
	 */
	public int initArraySize = 20;

	public MeasureAttrubute() {
	}

	public MeasureAttrubute(MeasureAttrubute m) {
		copy(m);
	}

	protected void copy(MeasureAttrubute m) {
		lineFormat = m.lineFormat;
		isIncludepad = m.isIncludepad;
		isTrackpad = m.isTrackpad;
		spacingAdd = m.spacingAdd;
		spacingMult = m.spacingMult;
		maxEms = m.maxEms;
		width = m.width;
		truncateAt = m.truncateAt;
		alignment = m.alignment;
		lineFactory = m.lineFactory;
		maxLines = m.maxLines;
		maxLinesLimitHeight = m.maxLinesLimitHeight;
		initArraySize = m.initArraySize;
	}

	public abstract Measurer buildMeasurer();
}
