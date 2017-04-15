package com.maizer.text.test;

import android.app.Activity;
import android.os.Bundle;
import android.text.StaticLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_MENU){
			if(mf!=null&&mf.mDialog!=null){
				mf.mDialog.show();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private static final String TAG = MainActivity.class.getCanonicalName();
	private MainFragment mf;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().add(android.R.id.content, mf = new MainFragment()).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}
}
