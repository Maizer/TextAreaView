package com.maizer.text.test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class Tool {

	private final static String TAG = "Tool";

	public static final Field getField(Object object, String name) {
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

	public static final Class<?> getIndicateClassOrInterface(Object object, String simplename) {
		Class<?> thisClass = object.getClass();
		Class<?>[] types = thisClass.getDeclaredClasses();
		for (Class<?> cs : types) {
			if (!cs.isEnum() && !cs.isAnnotation() && cs.getSimpleName().equals(simplename)) {
				return cs;
			}
		}
		return null;
	}

	/**
	 * Object obj = installInterfaceObject(null, new InvocationHandler() {
	 * 
	 * public Object invoke(Object proxy, Method method, Object[] args) throws
	 * Throwable { // TODO Auto-generated method stub return null;
	 * 
	 * } };,Runnable.class); Runnable runnable = (Runnable)obj; runnable.run();
	 * 
	 * @param loader
	 * @param invocationHandler
	 * @param interfaces
	 * @return 返回一个proxyer,即便为null,也可转换为你需要转换的借口
	 */
	public static Object installInterfaceObject(ClassLoader loader, InvocationHandler invocationHandler,
			Class<?>... interfaces) {
		if (interfaces.length <= 0) {
			throw new NullPointerException("interfaces == null");
		}

		return Proxy.newProxyInstance(loader, interfaces, invocationHandler);
	}

	public static final Method getMethod(Object object, String name, Class<?>... params) {
		Class<?> c = object.getClass();
		while (true) {
			try {
				Method m = c.getDeclaredMethod(name, params);
				m.setAccessible(true);
				return m;
			} catch (NoSuchMethodException e) {
			}
			c = c.getSuperclass();
			if (c.getSimpleName().equals("Object")) {
				return null;
			}
		}
	}

	public static final Method[] getMethodArray(Object object) {
		return object.getClass().getDeclaredMethods();
	}

	public static final Field[] getFieldArray(Object object) {
		return object.getClass().getDeclaredFields();
	}

	public static final float getDpToPx(int dp, float density) {
		return dp * density / 160;
	}

	// t = (1-t)^2P0+2t(1-t)P1+t^2P2; 二次
	/**
	 * 
	 * @param start
	 *            开始的值
	 * @param end
	 *            结束的值
	 * @param peak
	 *            峰值
	 * @param t
	 *            1>=t>=0;
	 * @return 现在的值
	 */
	public static final float getSlowScrollQuadLocation(float start, float end, float peak, float t, float slow,
			float lastSpac) {
		if (t >= slow) {
			t = lastSpac;
		}
		return (float) (Math.pow(1 - t, 2) * start + 2 * t * (1 - t) * peak + t * t * end);
	}

	public static final float getScrollQuadLocation(float start, float end, float peak, float t) {
		return (float) (Math.pow(1 - t, 2) * start + 2 * t * (1 - t) * peak + t * t * end);
	}

	/**
	 * 根据基数获取卷动的最大速度
	 * 
	 * <pre>
	 * float max = getScrollMaxSleep(10, 1);
	 * float currentSleep = max -= 1;
	 * </pre>
	 * 
	 * @param range
	 *            卷动的长度
	 * @param base
	 *            基数,i+=base;
	 * @return
	 */
	public static final float getScrollMaxSleep(float range, float base) {
		int s = (int) Math.abs(range);
		int i = 1;
		while (s > 0) {
			s -= i;
			i += base;
		}
		return range > 0 ? i : -i;
	}

	public static final Object[] simpleResolveInvokeMethods(Object[] source, Class<?> c, String methodName,
			Object... params) {
		Class<?>[] types = null;
		Object[] results = null;
		if (source != null) {
			for (int i = 0; i < source.length; i++) {
				Object invokeObject = source[i];
				if (invokeObject == null) {
					continue;
				}
				try {
					c.cast(invokeObject);
					if (types == null) {
						types = new Class<?>[params.length];
						for (int j = 0; j < params.length; j++) {
							Object param = params[j];
							Class<?> mC;
							if (param instanceof Class) {
								mC = (Class<?>) param;
								params[j] = null;
							} else {
								mC = param.getClass();
								mC = params[j].getClass();
								if (mC == Integer.class) {
									mC = int.class;
								} else if (mC == Float.class) {
									mC = float.class;
								} else if (mC == Double.class) {
									mC = double.class;
								} else if (mC == Boolean.class) {
									mC = boolean.class;
								} else if (mC == Short.class) {
									mC = short.class;
								} else if (mC == Long.class) {
									mC = long.class;
								}
							}
							types[j] = mC;
						}
					}
					Method m = invokeObject.getClass().getDeclaredMethod(methodName, types);
					m.setAccessible(true);
					Object result = m.invoke(invokeObject, params);
					if (m.getReturnType().toString().equals("void")) {
						if (results == null) {
							results = new Object[source.length];
						}
						results[i] = result;
					}
				} catch (Exception e) {
					continue;
				}
			}
		}
		return results;
	}

	public final static void simpleCloseStream(Closeable... streams) {
		for (Closeable stream : streams) {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Drawable loadDrawable(Resources source, int id, Drawable draw) {
		try {
			XmlResourceParser xrp = source.getXml(id);
			int type;
			while ((type = xrp.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
			}
			AttributeSet attrs = Xml.asAttributeSet(xrp);
			draw.inflate(source, xrp, attrs);
			xrp.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return draw;
	}

	public static File[] getStorageFileList(Context context) {
		try {
			File mainFile = context.getExternalCacheDir();
			File[] mainFiles = new File(mainFile.getPath().substring(0, mainFile.getPath().indexOf(File.separator, 1)))
					.listFiles();
			if (mainFiles != null && mainFiles.length > 0) {
				return mainFiles;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static File getStorageRootFile(Context context) {
		try {
			File mainFile = context.getExternalCacheDir();
			if (mainFile != null) {
				return new File(mainFile.getPath().substring(0, mainFile.getPath().indexOf(File.separator, 1)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static File mFile;

	public static boolean isFileExists(String path) {
		if (mFile == null) {
			mFile = new File(path);
		} else {
			Method fixSlashes = Tool.getMethod(mFile, "fixSlashes", String.class);
			Field mPath = Tool.getField(mFile, "path");
			try {
				mPath.set(mFile, fixSlashes.invoke(mFile, path));
			} catch (Exception e) {
				mFile = new File(path);
			}
		}
		return mFile.exists();
	}

	public static int findTopRange(ViewGroup v) {
		int top = 0;
		ViewParent parent = v;
		while (parent != null) {
			if (parent instanceof View) {
				top += ((View) parent).getTop();
				parent = parent.getParent();
			} else {
				break;
			}
		}
		return top;
	}

	public final static class TimeFormat {

		public static int getHour(int millisecond) {
			return getMinute(millisecond) / 60;
		}

		public static int getMinute(int millisecond) {
			return millisecond / 60000;
		}

		public static int getSecond(int millisecond) {
			return millisecond / 1000 % 60;
		}

		public static int getMillisScond(long nanosecond) {
			return (int) (nanosecond / 1000000000);
		}

	}

}
