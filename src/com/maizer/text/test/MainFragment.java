package com.maizer.text.test;

import com.maizer.R;
import com.maizer.text.liner.SpannableEditor;
import com.maizer.text.view.TextAreaView;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Editable.Factory;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainFragment extends Fragment {

	private static final String TAG = MainFragment.class.getSimpleName();
	private Point point = new Point();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		initFragment(view);
		return view;
	}

	private void initFragment(View view) {
		Activity mActivity = getActivity();
		mActivity.getWindowManager().getDefaultDisplay().getSize(point);
		ListView listView = (ListView) view.findViewById(R.id.listview);
		final LayoutInflater mInflater = mActivity.getLayoutInflater();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity, R.layout.i_textview) {
			public View getView(int position, View convertView, ViewGroup parent) {
				TextAreaView tav;
				if (convertView == null) {
					tav = (TextAreaView) mInflater.inflate(R.layout.i_textview, parent, false);
					tav.setMaxEms(-1);
					tav.setMaxLines(1);
					tav.setMaxLinesLimitHeight(-1);
				} else {
					tav = (TextAreaView) convertView;
				}
				tav.setFocusable(false);
				tav.setMaxEms(-1);
				tav.setLetterSpacing(10);
				tav.setEditable(false);
				tav.setTextSize(15);
				switch (position) {
				case 0:
					tav.setTruncateAt(TruncateAt.END);
					tav.setGravity(Gravity.LEFT);
					break;
				case 1:
					tav.setTruncateAt(TruncateAt.START);
					tav.setGravity(Gravity.LEFT);
					break;
				case 2:
					tav.setTruncateAt(TruncateAt.MIDDLE);
					tav.setGravity(Gravity.LEFT);
					break;
				case 3:
					tav.setTruncateAt(TruncateAt.END);
					tav.setGravity(Gravity.CENTER);
					break;
				case 4:
					tav.setTruncateAt(TruncateAt.START);
					tav.setGravity(Gravity.CENTER);
					break;
				case 5:
					tav.setTruncateAt(TruncateAt.MIDDLE);
					tav.setGravity(Gravity.CENTER);
					break;
				case 6:
					tav.setTruncateAt(TruncateAt.START);
					tav.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				case 7:
					tav.setTruncateAt(TruncateAt.END);
					tav.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				case 8:
					tav.setTruncateAt(TruncateAt.MIDDLE);
					tav.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
					break;
				case 9:
					tav.setTruncateAt(null);
					tav.setGravity(Gravity.CENTER);
					break;
				case 10:
					tav.setTruncateAt(null);
					tav.setGravity(Gravity.RIGHT);
					break;
				case 11:
					tav.setTruncateAt(null);
					tav.setGravity(Gravity.LEFT);
					break;
				case 12:
					tav.setTextSize(20);
					tav.setTruncateAt(null);
					tav.setGravity(Gravity.LEFT);
					tav.setMaxEms(0);
					break;
				}
				tav.setText(getItem(position));
				return tav;
			}
		};
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.END),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.START),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.MIDDLE),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.END,Gravity.CENTER),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.START,Gravity.CENTER),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.MIDDLE,Gravity.CENTER),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.START,Gravity.CENTER,Gravity.RIGHT),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.END,Gravity.CENTER,Gravity.RIGHT),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add(
				"TextAreaView(outer Width,TruncateAt.MIDDLE,Gravity.CENTER,Gravity.RIGHT),这是列表测试下的TextAreaView,这是列表测试下的TextAreaView,这是列表测试下的TextAreaView");
		adapter.add("TextAreaView(Gravity.CENTER)");
		adapter.add("TextAreaView(Gravity.RIGHT)");
		adapter.add("TextAreaView(Gravity.LEFT)");
		adapter.add("click one item,enter editor!");
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Toast.makeText(getActivity(), "setLetterSpacing(20),setEditable(true)", Toast.LENGTH_SHORT).show();
				TextAreaView tv = (TextAreaView) view;
				tv.setFocusable(true);
				tv.setEditable(true);
				tv.setLetterSpacing(20);
			}
		});
		StringBuilder mStrings = new StringBuilder();
		Resources sources = mActivity.getResources();
		mStrings.append(sources.getString(R.string.hint1));
		mStrings.append(sources.getString(R.string.hint2));
		mStrings.append(sources.getString(R.string.hint3));
		mStrings.append(sources.getString(R.string.hint4));
		mStrings.append(sources.getString(R.string.hint5));
		mStrings.append(sources.getString(R.string.hint6));
		mStrings.append(sources.getString(R.string.hint7));
		mStrings.append(sources.getString(R.string.hint8));
		mStrings.append(sources.getString(R.string.hint9));
		mStrings.append(sources.getString(R.string.hint10));
		mStrings.append(sources.getString(R.string.hint11));
		mStrings.append(sources.getString(R.string.hint12));
		mStrings.append(sources.getString(R.string.hint13));
		int size = Integer.valueOf(sources.getString(R.string.hint_size));
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < size; j++) {
			sb.append("\n");
			sb.append(j);
			sb.append(mStrings);
		}

		final View v = (View) view.findViewById(R.id.t1);
		final TextAreaView t = (TextAreaView) v;
		t.setEditableFactory(new Factory() {

			@Override
			public Editable newEditable(CharSequence source) {
				return new SpannableEditor(source);
			}

		});
		t.setClickable(true);
		t.setText(sb);
		((SpannableEditor) t.getEditableText()).setCursorLocation(1);
	}

	private class T extends Thread {

		TextAreaView tv;
		int count;
		int point = 0;
		float y = 0;
		boolean top;

		public void start() {
			tv.postDelayed(this, 2000);
		}

		public void run() {
			// MotionEvent event;
			// if (count >= 10) {
			// return;
			// }
			// if (point == 0) {
			// count++;
			// event = MotionEvent.obtain(SystemClock.uptimeMillis(),
			// SystemClock.uptimeMillis(),
			// MotionEvent.ACTION_DOWN, 0, y, 0);
			// point--;
			// } else {
			// event = MotionEvent.obtain(SystemClock.uptimeMillis(),
			// SystemClock.uptimeMillis(),
			// MotionEvent.ACTION_MOVE, 0, y, 0);
			// if (top) {
			// y -= 10;
			// if (y <= 0) {
			// point = 0;
			// event.recycle();
			// event = MotionEvent.obtain(SystemClock.uptimeMillis(),
			// SystemClock.uptimeMillis(),
			// MotionEvent.ACTION_UP, 0, y, 0);
			// }
			// } else {
			// y += 10;
			// }
			// if (y >= 2000) {
			// top = !top;
			// }
			// }
			// tv.dispatchTouchEvent(event);
			// event.recycle();
			// tv.postDelayed(this, 10);
		}
	}

	Handler handler;

	private void start(final View v) {
		final Thread t = new Thread() {
			public void run() {
				if (v.isFocused()) {
					int c1 = 0x000000;
					int c2 = -16777215;
					int c = (int) (Math.random() * c2);
					v.setBackgroundColor(c);
					handler.post(this);
				} else {
					Run r = new Run();
					r.d = this;
					r.v = v;
					new Thread(r).start();
				}
			}
		};
		handler = new Handler();
		handler.post(t);
	}

	private class Run implements Runnable {

		private View v;
		private Thread d;

		@Override
		public void run() {
			while (!v.isFocused()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			v.post(d);
		}

	}
}
