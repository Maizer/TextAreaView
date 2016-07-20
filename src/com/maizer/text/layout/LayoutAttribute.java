package com.maizer.text.layout;

import com.maizer.text.factory.MeasureFactory;
import com.maizer.text.measure.MeasureAttribute;

import android.text.Layout.Alignment;

public class LayoutAttribute extends MeasureAttribute {

	public LayoutAttribute() {
		super();
	}

	public LayoutAttribute(LayoutAttribute m) {
		super(m);
	}

	@Override
	public void copy(MeasureAttribute m) {
		super.copy(m);
		if (m instanceof LayoutAttribute) {
			LayoutAttribute la = (LayoutAttribute) m;
			height = la.height;
			gravity = la.gravity;
			paddingLeft = la.paddingLeft;
			paddingBottom = la.paddingBottom;
			paddingTop = la.paddingTop;
			paddingRight = la.paddingRight;
			measureFactory = la.measureFactory;
		}
	}

	public int height;

	public int paddingLeft;
	public int paddingRight;
	public int paddingTop;
	public int paddingBottom;

	public int gravity;
	
	public int initLineArraySize = 10;

	public MeasureFactory measureFactory = MeasureFactory.getInstance();

}
