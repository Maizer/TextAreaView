package com.maizer.text.factory;

import com.maizer.text.layout.LayoutAttribute;
import com.maizer.text.measure.LineMeasurer;
import com.maizer.text.measure.MeasureAttribute;
import com.maizer.text.measure.Measurer;

public class MeasureFactory {

	private static MeasureFactory sInstall = new MeasureFactory();

	public static MeasureFactory getInstance() {
		return sInstall;
	}

	public Measurer newMeasurer(MeasureAttribute attr) {
		return new LineMeasurer(attr, attr.initArraySize);
	}
}
