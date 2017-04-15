package com.maizer.text.factory;

import com.maizer.text.cursor.CursorDevicer;
import com.maizer.text.cursor.DefaultCursor;

import android.view.View;

public class CursorFactory {

	private static CursorFactory FACTORY = new CursorFactory();
	
	public static CursorFactory getInstance(){
		return FACTORY;
	}

	public CursorDevicer newCursorDivicer(View view) {
		return new DefaultCursor(view);
	}
}
