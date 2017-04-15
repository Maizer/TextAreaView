package com.maizer.text.factory;

import com.maizer.text.liner.Lineable;
import com.maizer.text.liner.Lineable.LineCompute;
import com.maizer.text.liner.TextAreaLiner;

import android.text.Editable;

public class LineFactory implements LineCompute{

	private static LineFactory sInstance = new LineFactory();

	public static LineFactory getInstance() {
		return sInstance;
	}

	public int computeLineOffsetTop(int mBottom,int mBelow,int mAbove) {
		return mBottom - mBelow + mAbove - mBelow / 2;
	}

	public int computeLineOffsetBottom(int mBottom,int mBelow,int mAbove) {
		return mBottom - mBelow / 2;
	}
	
	public int computeLineTop(int mBottom,int mBelow,int mAbove){
		return mBottom - mBelow + mAbove;
	}

	public Lineable newLineable() {
		return new TextAreaLiner();
	}
}
