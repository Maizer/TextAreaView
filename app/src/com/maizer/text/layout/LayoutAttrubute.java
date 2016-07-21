package com.maizer.text.layout;

import com.maizer.text.measure.LineMeasurer;
import com.maizer.text.measure.MeasureAttrubute;
import com.maizer.text.measure.Measurer;

public class LayoutAttrubute extends MeasureAttrubute {

	public LayoutAttrubute() {
		super();
	}

	public LayoutAttrubute(LayoutAttrubute m) {
		super(m);
	}

	@Override
	public void copy(MeasureAttrubute m) {
		super.copy(m);
		if (m instanceof LayoutAttrubute) {
			LayoutAttrubute la = (LayoutAttrubute) m;
			height = la.height;
			gravity = la.gravity;
			paddingLeft = la.paddingLeft;
			paddingBottom = la.paddingBottom;
			paddingTop = la.paddingTop;
			paddingRight = la.paddingRight;
		}
	}

	public int height;

	public int paddingLeft;
	public int paddingRight;
	public int paddingTop;
	public int paddingBottom;

	public int gravity;
	
	public int initLineArraySize = 10;
	
	public Measurer build() {
		return new LineMeasurer(this, initArraySize);
	}

}
