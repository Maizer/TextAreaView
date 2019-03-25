package com.maizer.text.test;

import java.util.ArrayList;
import java.util.List;

import com.maizer.text.view.TextAreaView;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class TimingDialog extends Dialog {

	private static final String TAG = "TimingDialog";
	private static final String TABLE_NAME = "timing";
	private static final String CLUMN_NAME = "time";
	private TimeAdapter<Integer> mAdapter;
	private ListView listView;

	public TimingDialog(Activity context) {
		super(context);
		setOwnerActivity(context);
		init(context);
	}

	private int W = 1080;

	private void init(Activity acter) {
		LayoutInflater inflater = acter.getLayoutInflater();
		listView = (ListView) inflater.inflate(R.layout.i_listview, null, false);
		listView.setLayoutParams(new LayoutParams(W, 0));
		listView.setDivider(null);
		listView.setAdapter(mAdapter = getAdapter(acter, listView));
		setContentView(listView, listView.getLayoutParams());
	}

	TimeAdapter<Integer> getAdapter(Activity acter, ListView listView) {
		List<Integer> times = new ArrayList<Integer>(6);
		TimeAdapter<Integer> timeAdapter = new TimeAdapter<Integer>(times, (int) (100), listView);
		return timeAdapter;
	}

	@Override
	public void dismiss() {
		mAdapter.hideDrawable(mAdapter.editText);
		mAdapter.dismiss();
		super.dismiss();
	}

	public static final SlideIntent foundTouchIntent(float oldX, float oldY, float nowX, float nowY,
			float offsetDistance) {
		if (oldX != nowX && oldY != nowY) {
			float spacX = nowX - oldX;
			float spacY = nowY - oldY;
			float absSpacX = Math.abs(spacX);
			float absSpacY = Math.abs(spacY);
			if (absSpacX > offsetDistance || absSpacY > offsetDistance) {
				if (absSpacX > absSpacY) {
					if (spacX > 0) {
						return SlideIntent.RIGHT;
					}
					return SlideIntent.LEFT;
				} else if (absSpacX < absSpacY) {
					if (spacY > 0) {
						return SlideIntent.BOTTOM;
					}
					return SlideIntent.TOP;
				}
			}
		}
		return SlideIntent.NO;
	}

	class TimeAdapter<T> extends BaseAdapter
			implements OnItemLongClickListener, OnItemClickListener, OnTouchListener, InputFilter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.BaseAdapter#notifyDataSetChanged()
		 */
		@Override
		public void notifyDataSetChanged() {
			listView.getLayoutParams().height = times.size() * height;
			super.notifyDataSetChanged();
		}

		public void dismiss() {
			Activity mActiivty = (Activity) getOwnerActivity();
		}

		private void save(SQLiteDatabase db) {
		}

		private final static String CUSTOM = "自定义";

		private List<T> times;
		private LayoutInflater inflater;
		private TextAreaView editText;

		Drawable deleteDrawable;
		Drawable addDrawable;
		Drawable selectDrawable;

		private ListView listView;

		private int height;

		InputFilter[] inputFilter;
		int DEFAULT_EDITINFO_TYPE = EditorInfo.TYPE_CLASS_PHONE;

		int selectPosition = -1;

		private boolean canSelect;

		private boolean isChanged;

		private void initDrawable(Resources source) {
			addDrawable = source.getDrawable(R.drawable.ic_launcher);
			deleteDrawable = source.getDrawable(R.drawable.ic_launcher);
			selectDrawable = source.getDrawable(R.drawable.ic_launcher);
			int h = height / 3;
			addDrawable.setBounds(0, 0, h, h);
			deleteDrawable.setBounds(0, 0, h, h);
			selectDrawable.setBounds(0, 0, h, h);
			hideDrawable(null);
		}

		T getSelectItem() {
			if (selectPosition != -1) {
				return getItem(selectPosition);
			}
			return null;
		}

		public TimeAdapter(List<T> time, int h, ListView listview) {
			times = time;
			height = h;
			listView = listview;
			listView.setOnItemClickListener(this);
			listView.setOnItemLongClickListener(this);
			inputFilter = new InputFilter[] { this };
			inflater = getOwnerActivity().getLayoutInflater();
			times.add(0, null);
			initDrawable(getOwnerActivity().getResources());
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (times == null) {
				return 0;
			}
			return times.size();
		}

		@Override
		public T getItem(int position) {
			if (times != null && position < times.size()) {
				return times.get(position);
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextAreaView textView;
			if (convertView == null) {
				textView = (TextAreaView) inflater.inflate(R.layout.i_textview, parent, false);
				// textView.setMaxEms(-1);
				textView.setLetterSpacing(10);
				textView.setMaxLines(1);
				textView.setMaxLinesLimitHeight(-1);
				textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 4);
				textView.setBackgroundDrawable(null);
				textView.setTextColor(Color.GRAY);
				textView.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, height));
				// textView.setCompoundDrawablePadding(-deleteDrawable.getBounds().width());
				textView.setText("liuqishuai");
				textView.setGravity(Gravity.CENTER);
				if (position == 0) {
					textView.setCompoundDrawables(addDrawable, null, deleteDrawable, null);
					editText = textView;
					editText.setEditable(false);
					editText.setFocusable(false);
					if (inputFilter != null) {
						textView.setFilters(inputFilter);
					}
				}
			} else {
				textView = (TextAreaView) convertView;
			}
			textView.setOnTouchListener(this);
			textView.setId(position);
			if (position != 0) {
				initText(position, textView);
				textView.setCompoundDrawables(null, null, deleteDrawable, null);
				if (canSelect) {
					if (position == selectPosition) {
						textView.setCompoundDrawables(selectDrawable, null, null, null);
						return textView;
					}
				}
				textView.setCompoundDrawables(null, null, textView.getCompoundDrawableRight(), null);
			} else if (!textView.hasFocusable()) {
				textView.setText(CUSTOM);
			} else {
				textView.setText("");
				textView.requestFocus();
			}
			return textView;
		}

		void initText(int position, TextAreaView textView) {
			T text = getItem(position);
			textView.setText(text + "分钟");
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			return false;
		}

		private void showDrawable() {
			addDrawable.setAlpha(255);
			deleteDrawable.setAlpha(255);
			editText.setEditable(true);
			editText.setFocusable(true);
			canSelect = false;
			notifyDataSetInvalidated();
		}

		private void hideDrawable(TextAreaView edit) {
			addDrawable.setAlpha(0);
			deleteDrawable.setAlpha(0);
			canSelect = true;
			if (edit != null) {
				Activity acter = (Activity) getOwnerActivity();
				// edit.canPopupImm(false, acter.getInputMethidManager());
				edit.setFocusable(false);
				edit.setEditable(false);
				if (DEFAULT_EDITINFO_TYPE != 0) {
					edit.setInputType(EditorInfo.TYPE_CLASS_TEXT);
				}
				edit.setText(CUSTOM);
			}
			notifyDataSetInvalidated();
		}

		@Override
		public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			if (view.getId() == 0) {
				TextAreaView edit = (TextAreaView) view;
				Activity acter = (Activity) getOwnerActivity();
				// edit.canPopupImm(true, acter.getInputMethidManager());
				// parent.setFocusableInTouchMode(false);
				// parent.setFocusable(false);
				edit.setText("");
				if (DEFAULT_EDITINFO_TYPE != 0) {
					edit.setInputType(DEFAULT_EDITINFO_TYPE);
				}
				showDrawable();
				// editText.viewClicked(acter.getInputMethidManager());
			} else if (canSelect) {
				select(parent, (TextAreaView) view, position);
			}
		}

		void select(AdapterView<?> parent, TextAreaView view, int position) {
			Drawable leftDrawable = view.getCompoundDrawableLeft();
			if (leftDrawable == null) {
				selectPosition = view.getId();
			} else {
				selectPosition = -1;
			}
			notifyDataSetInvalidated();
		}

		private SlideIntent lastIntent;
		private boolean isDelete;
		private float downX;
		private float downY;

		private boolean isDelete(View view) {
			Activity acter = (Activity) getOwnerActivity();
			Rect bounds = deleteDrawable.getBounds();
			int width = W;
			int height = bounds.width();
			int left = width - 2 * height;
			if (downX >= left) {
				isDelete = true;
				return true;
			}
			return false;
		}

		private boolean isAdd(View view) {
			Rect bounds = addDrawable.getBounds();
			int left = bounds.left;
			int height = bounds.height();
			if (downX <= left + 2 * height && view.getId() == 0) {
				isDelete = false;
				return true;
			}
			return false;
		}

		boolean handler = false;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				downY = event.getRawY();
				downX = event.getRawX();

				if (isAdd(v) || isDelete(v)) {
					lastIntent = SlideIntent.NO;
					Log.e(TAG, "HANDER");
					return handler = true;
				}
				Log.e(TAG, "HANDERTRUE");
				lastIntent = SlideIntent.BOTTOM;
				return handler = false;
			case MotionEvent.ACTION_MOVE:
				if (!handler) {
					return false;
				}
				if (lastIntent == SlideIntent.NO) {
					lastIntent = foundTouchIntent(downX, downY, event.getRawX(), event.getRawY(), 50);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (!handler) {
					return false;
				}
				TextAreaView edit = ((TextAreaView) v);
				int id = edit.getId();
				if (lastIntent == SlideIntent.NO) {
					if (isDelete) {
						if (id == 0) {
							hideDrawable(edit);
							break;
						}
						if (remove(id) && times.size() > id && times.remove(id) != null) {
							isChanged = true;
							notifyDataSetChanged();
						}
					} else if (id == 0) {
						T obj;
						if ((obj = add(edit)) != null) {
							for (T time : times) {
								if (time != null && time.equals(obj)) {
									edit.setText("");
									return true;
								}
							}
							if (times.add(obj)) {
								isChanged = true;
								notifyDataSetChanged();
							}
						}
					}
				} else if (id == 0) {
					Activity acter = (Activity) getOwnerActivity();
					// edit.canPopupImm(true, acter.getInputMethidManager());
					// parent.setFocusableInTouchMode(false);
					// edit.setFocusable(true);
					// edit.setEditable(true);
					// edit.setActivated(true);
					// edit.requestFocus();
					edit.setText("");
					if (DEFAULT_EDITINFO_TYPE != 0) {
						edit.setInputType(DEFAULT_EDITINFO_TYPE);
					}
					// edit.superViewClicked(acter.getInputMethidManager());
					// acter.showInputMethod(edit);
					showDrawable();
				}
				break;
			}
			if (!handler) {
				return false;
			}
			return true;
		}

		boolean remove(int position) {
			return true;
		}

		T add(TextAreaView edit) {
			try {
				if (getCount() > 6) {
					throw new IllegalArgumentException();
				}
				T time = (T) Integer.valueOf(edit.getText().toString());
				if ((Integer) time > 720) {
					throw new IllegalAccessException();
				}
				return time;
			} catch (IllegalAccessException e) {
			} catch (NumberFormatException e) {
			} catch (IllegalArgumentException e) {
			}
			return null;
		}

		private StringBuilder buffer;

		boolean canPass(char c) {
			if (c < '0' || c > '9') {
				return false;
			}
			return true;
		}

		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

			if (source.toString().equals(CUSTOM)) {
				return source;
			}
			int length = source.length();
			if (length == 1) {
				if (canPass(source.charAt(0))) {
					return source;
				}
				return "";
			} else {
				if (buffer == null) {
					buffer = new StringBuilder(source.length());
				} else {
					buffer.setLength(0);
				}
				for (int i = 0; i < source.length(); i++) {
					char c = source.charAt(i);
					if (canPass(c)) {
						buffer.append(c);
					}
				}
				return buffer.toString();
			}
		}
	}

	public enum SlideIntent {
		TOP, BOTTOM, LEFT, RIGHT, NO, UNKNOW;

		private static final float offsetDistance = 20;

		public static boolean isVertical(SlideIntent intent) {
			return intent == TOP || intent == BOTTOM;
		}

		public static boolean isHorizontal(SlideIntent intent) {
			return intent == LEFT || intent == RIGHT;
		}

		public static final SlideIntent foundTouchIntent(float oldX, float oldY, float nowX, float nowY) {
			if (oldX != nowX && oldY != nowY) {
				float spacX = nowX - oldX;
				float spacY = nowY - oldY;
				float absSpacX = Math.abs(spacX);
				float absSpacY = Math.abs(spacY);
				if (absSpacX > offsetDistance || absSpacY > offsetDistance) {
					if (absSpacX > absSpacY) {
						if (spacX > 0) {
							return SlideIntent.RIGHT;
						}
						return SlideIntent.LEFT;
					} else if (absSpacX < absSpacY) {
						if (spacY > 0) {
							return SlideIntent.BOTTOM;
						}
						return SlideIntent.TOP;
					}
				}
			}
			return SlideIntent.NO;
		}
	}
}
