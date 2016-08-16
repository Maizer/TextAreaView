package com.maizer.text.util;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import android.util.Log;

public class ObjectLinked<T> implements ArrayGc {

	private static final Node EMTY = new Node(null);

	private static final String TAG = ObjectLinked.class.getSimpleName();

	private Node<T> die;

	private Node<T> first;
	private Node<T> last;

	private Node<T> next;

	private Node<T> save;

	private int nextNum;

	private int size;

	public T getLast() {
		if (last == null) {
			throw new NoSuchElementException();
		}
		return last.value;
	}

	public T getFirst() {
		if (first == null) {
			throw new NoSuchElementException();
		}
		return first.value;
	}

	public void add(T value) {
		if (value == null) {
			throw new NullPointerException();
		}
		if (last != null) {
			if (last.next == null) {
				Node<T> node;
				if (die == null) {
					node = new Node<T>(value);
				} else {
					node = die;
					node.setValue(value);
					die = die.next;
				}
				node.last = last;
				node.next = null;
				last.next = node;
				last = node;
			} else {
				last = last.next.setValue(value);
			}
		} else if (die != null) {
			die.setValue(value);
			first = last = die;
			die = die.next;
			first.next = null;
			first.last = null;
		} else {
			Node<T> node = new Node<T>(value);
			first = last = node;
		}
		size++;
	}

	public void addFirst(T value) {
		if (value == null) {
			throw new NullPointerException();
		}
		if (first != null) {
			if (first.last == null) {
				Node<T> node;
				if (die == null) {
					node = new Node<T>(value);
				} else {
					node = die;
					node.setValue(value);
					die = die.next;
				}
				node.last = null;
				node.next = first;
				first.last = node;
				first = node;
			} else {
				first = first.last.setValue(value);
			}
		} else if (die != null) {
			die.setValue(value);
			first = last = die;
			die = die.next;
			first.next = null;
			first.last = null;
		} else {
			Node<T> node = new Node<T>(value);
			first = last = node;
		}
		size++;
	}

	public void save(boolean isNext) {
		if (next == null) {
			return;
		}
		if (isNext) {
			save = next.next;
		} else {
			save = next.last;
		}
	}

	public boolean hasSave() {
		if (save != null) {
			return true;
		}
		return false;
	}

	public T nextSave() {
		T value = save.value;
		save = save.next;
		return value;
	}

	public void gc() {
		if (die != null) {
			// die.clearNext(); //->node:don't iteration.
			Node<T> mNode = die;
			while (mNode != null) {
				mNode = mNode.clearNext();
			}
		}
		die = null;
	}

	public void addLast(T value) {
		add(value);
	}

	public T get(int i) {
		if (size == 0 || i >= size) {
			return null;
		}
		int bin = size >> 1;
		if (i > bin) {
			Node<T> node = last;
			int l = size - 1;
			while (node != null) {
				if (l == i) {
					return node.value;
				}
				node = node.last;
				l--;
			}
		} else {
			Node<T> node = first;
			int l = 0;
			while (node != null) {
				if (l == i) {
					return node.value;
				}
				node = node.next;
				l++;
			}
		}
		return null;
	}

	public T removeFirst() {
		if (first != null) {
			Node<T> node = first;
			T value = node.value;
			if (node == next) {
				next = node.next;
				if (next == null) {
					next = EMTY;
				}
			}
			if (node.next != null) {
				first = node.next;
				first.last = null;
			} else {
				if (node == last) {
					last = null;
				}
				first = null;
			}
			if (die == null) {
				die = node;
				node.next = null;
				node.last = null;
			} else {
				die.last = node;
				node.next = die;
				node.last = null;
				die = node;
			}
			size--;
			return value;
		} else {
			throw new NoSuchElementException();
		}
	}

	public T removeAfter() {
		if (next == null) {
			return null;
		}
		if (next == EMTY) {
			next = last;
		}
		T value = null;
		while (next != EMTY) {
			value = remove();
		}
		return value;
	}

	public T remove() {
		if (next == null) {
			return null;
		}
		if (next == EMTY) {
			next = last;
		} else {
			next = next.last;
		}
		Node<T> node = next;
		T value = node.value;
		if (node == first) {
			first = node.next;
			if (first == null) {
				last = null;
			}
		} else if (node == last) {
			last = node.last;
			if (last == null) {
				first = null;
			}
		}
		if ((next = node.next) == null) {
			next = EMTY;
		}
		node.remove();
		if (die == null) {
			die = node;
			node.next = null;
			node.last = null;
		} else {
			die.last = node;
			node.next = die;
			node.last = null;
			die = node;
		}
		size--;
		return value;
	}

	public T removeLast() {
		if (last != null) {
			Node<T> node = last;
			T value = node.value;
			if (next == node) {
				next = EMTY;
			}
			if (node.last != null) {
				last = node.last;
				node.next = null;
				node.last = null;
				last.next = null;
			} else {
				if (first == node) {
					first = null;
				}
				last = null;
			}
			if (die == null) {
				die = node;
				node.next = null;
				node.last = null;
			} else {
				die.last = node;
				node.next = die;
				node.last = null;
				die = node;
			}
			size--;
			return value;
		} else {
			throw new NoSuchElementException();
		}
	}

	public boolean hasNext() {
		if (next == null) {
			if (first != null) {
				next = first;
				return true;
			}
		} else if (next != EMTY) {
			return true;
		}
		return false;
	}

	public boolean hasPrevoious() {
		if (next == null) {
			if (last != null) {
				next = last;
				return true;
			}
		} else if (next != EMTY) {
			return true;
		}
		return false;
	}

	public T getNext() {
		if (next == null || next == EMTY) {
			return null;
		}
		return next.value;
	}

	public T next() {
		T value = next.value;
		next = next.next;
		if (next == null) {
			next = EMTY;
		}
		nextNum++;
		return value;
	}

	public int getNextIndex() {
		return nextNum - 1;
	}

	public int size() {
		return size;
	}

	public void clear() {
		nextNum = 0;
		save = null;
		next = null;
		while (hasNext()) {
			T t = removeFirst();
			if (t instanceof ArrayGc) {
				((ArrayGc) t).clear();
			}
		}
		first = null;
		last = null;
		next = null;
	}

	public T prevoious() {
		T value = next.value;
		next = next.last;
		if (next == null) {
			next = EMTY;
		}
		return value;
	}

	public T getFromLast(int location, boolean canNext) {
		if (last == null) {
			next = null;
			return null;
		}
		Node<T> node = last;
		for (int i = 0; i < location; i++) {
			if ((node = node.last) == null) {
				return null;
			}
		}
		if (canNext) {
			next = node.last;
		} else {
			next = node;
		}
		return node.value;
	}

	public T getFromFirst(int location, boolean canNext) {
		if (first == null) {
			next = null;
			nextNum = 0;
			return null;
		}
		Node<T> node = first;
		for (int i = 0; i < location; i++) {
			if ((node = node.next) == null) {
				return null;
			}
		}
		if (canNext) {
			next = node.next;
			nextNum = location + 1;
		} else {
			next = node;
			nextNum = location;
		}
		return node.value;
	}

	public T getRecycle() {
		if (die != null) {
			return die.value;
		}
		return null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("size > " + size);
		sb.append("\n");
		sb.append("First > " + first);
		sb.append("\n");
		sb.append("Last > " + last);
		sb.append("\n");
		return sb.toString();
	}

	public ListIterator<T> getIterator() {
		return new Iter();
	}

	private class Iter implements ListIterator<T> {

		private Node<T> mNext;

		@Override
		public boolean hasNext() {
			if (mNext == null) {
				if (first != null) {
					mNext = first;
					return true;
				}
			} else if (mNext != EMTY) {
				return true;
			}
			return false;
		}

		@Override
		public T next() {
			T value = mNext.value;
			mNext = mNext.next;
			if (mNext == null) {
				mNext = EMTY;
			}
			return value;
		}

		@Override
		public void remove() {
		}

		@Override
		public void add(T object) {
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public int nextIndex() {
			return 0;
		}

		@Override
		public T previous() {
			return null;
		}

		@Override
		public int previousIndex() {
			return 0;
		}

		@Override
		public void set(T object) {
			next.value = object;
		}

	}

	private static class Node<T> {

		private T value;
		private Node<T> last;
		private Node<T> next;

		public void remove() {
			if (last != null) {
				last.next = next;
			}
			if (next != null) {
				next.last = last;
			}
			last = null;
			next = null;
			value = null;
		}

		public Node<T> clearNext() {
			if (next != null) {
				// next.clearNext();
				Node<T> node = next;
				next = null;
				if (value instanceof ArrayGc) {
					ArrayGc g = (ArrayGc) value;
					g.clear();
					g.gc();
				}
				value = null;
				return node;
			}
			return next;
		}

		public void clearPrevoious() {
			if (last != null) {
				last.clearPrevoious();
				last = null;
				value = null;
			}
		}

		public Node(T value) {
			this.value = value;
		}

		public Node<T> setValue(T value) {
			this.value = value;
			return this;
		}
	}

}
