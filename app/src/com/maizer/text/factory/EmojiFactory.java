package com.maizer.text.factory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.maizer.text.layout.TextAreaLayout;

import android.graphics.Bitmap;
import android.text.Layout;
import android.util.Log;

public abstract class EmojiFactory {
	private static final String TAG = EmojiFactory.class.getCanonicalName();

	public int getCharFirstHighSuRRogate() {
		return 0xD800;
	}

	public int getCharLastLowSuRRogate() {
		return 0xDFFF;//FE0F
	}

	public static EmojiFactory installDefault() {
		return DefauleEmojiFactory.install();
	}

	public abstract int getMinimumAndroidPua();

	public abstract int getMaximumAndroidPua();

	public abstract Bitmap getBitmapFromAndroidPua(int code);

	protected static class DefauleEmojiFactory extends EmojiFactory {

		private static Object EMOJI_FACTORY;
		private static int MIN_EMOJI, MAX_EMOJI;

		private static final DefauleEmojiFactory install() {
			try {
				return new DefauleEmojiFactory();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		private Field getField(Object object, String name) {
			Class<?> c;
			if (object instanceof Class) {
				c = (Class<?>) object;
			} else {
				c = object.getClass();
			}
			for (Field f : c.getDeclaredFields()) {
				Log.e(EmojiFactory.DefauleEmojiFactory.class.getCanonicalName(), "" + f);
			}
			try {
				Field field = c.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
			return null;
		}

		Field getFields(Object object, String name) {
			Class<?> c;
			if (object instanceof Class) {
				c = (Class<?>) object;
			} else {
				c = object.getClass();
			}
			while (true) {
				try {
					Field field = c.getDeclaredField(name);
					field.setAccessible(true);
					return field;
				} catch (NoSuchFieldException e) {
				}
				c = c.getSuperclass();
				if (c.getSimpleName().equals("Object")) {
					return null;
				}
			}
		}

		private Method getMethod(Object object, String name, Class<?>... parameterTypes) {
			Class<?> c;
			if (object instanceof Class) {
				c = (Class<?>) object;
			} else {
				c = object.getClass();
			}
			Method field;
			try {
				field = c.getDeclaredMethod(name, parameterTypes);
				field.setAccessible(true);
				return field;
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			return null;
		}

		private DefauleEmojiFactory() throws IllegalAccessException, IllegalArgumentException,
				InvocationTargetException, ClassNotFoundException {
			Class<?> mC = ClassLoader.getSystemClassLoader().loadClass("android.emoji.EmojiFactory");
			Method method = getMethod(mC, "newAvailableInstance");
			Object obj = method.invoke(mC);
			EMOJI_FACTORY = obj;
			if (EMOJI_FACTORY != null) {
				MIN_EMOJI = (Integer) getMethod(EMOJI_FACTORY, "getMinimumAndroidPua").invoke(EMOJI_FACTORY);
				MAX_EMOJI = (Integer) getMethod(EMOJI_FACTORY, "getMaximumAndroidPua").invoke(EMOJI_FACTORY);
			} else {
				MIN_EMOJI = -1;
				MAX_EMOJI = -1;
			}
		}

		@Override
		public int getMinimumAndroidPua() {
			return MIN_EMOJI;
		}

		@Override
		public int getMaximumAndroidPua() {
			return MAX_EMOJI;
		}

		@Override
		public Bitmap getBitmapFromAndroidPua(int code) {
			if(EMOJI_FACTORY == null){
				return null;
			}
			try {
				return (Bitmap) getMethod(EMOJI_FACTORY, "getBitmapFromAndroidPua", int.class).invoke(EMOJI_FACTORY,
						code);
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			} catch (InvocationTargetException e) {
			}
			return null;
		}

	}
}
