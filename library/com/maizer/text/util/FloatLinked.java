package com.maizer.text.util;

import java.util.NoSuchElementException;

public class FloatLinked implements ArrayGc {

	public final static float INVALID = Float.MIN_NORMAL;

	private static final Node EMTY = new Node(0);

	private static final String TAG = FloatLinked.class.getSimpleName();

	private Node die;

	private Node first;
	private Node last;

	private Node next;

	private int size;
	private int num;

	public float getLast() {
		if (last == null) {
			throw new NoSuchElementException();
		}
		return last.value;
	}

	public float getFirst() {
		if (first == null) {
			throw new NoSuchElementException();
		}
		return first.value;
	}

	public int getIndexByValue(float value) {
		if (size == 0) {
			return -1;
		}
		float v1 = value - first.value;
		if (v1 < 0) {
			return -1;
		} else if (v1 == 0) {
			return 0;
		}
		float v2 = (last.value - first.value) / 2;
		int i = 0;
		if (v1 >= v2) {
			Node node = last;
			float lastValue = -1;
			while (node != null) {
				float v = node.value;
				if (v < value) {
					if (lastValue == -1) {
						lastValue = value;
					} else {
						lastValue -= value;
					}
					if (value - v >= lastValue) {
						break;
					}
					i++;
					break;
				} else if (v == value) {
					break;
				}
				node = node.last;
				lastValue = v;
				i++;
			}
			return size - i;
		}
		Node node = first;
		float lastValue = -1;
		while (node != null) {
			float v = node.value;
			if (v > value) {
				if (lastValue == -1) {
					lastValue = value;
				} else {
					lastValue = value - lastValue;
				}
				if (v - value >= lastValue) {
					i--;
					break;
				}
				break;
			} else if (v == value) {
				break;
			}
			node = node.next;
			i++;
			lastValue = v;
		}
		return i;
	}

	public void gc() {
		if (die != null) {
			die.clearNext();
		}
		die = null;
	}

	public float get(int i) {
		if (size == 0 || i >= size || i < 0) {
			return -1;
		}
		int bin = size / 2;
		if (i > bin) {
			Node node = last;
			int l = size - 1;
			while (node != null) {
				if (l == i) {
					return node.value;
				}
				node = node.last;
				l--;
			}
		} else {
			Node node = first;
			int l = 0;
			while (node != null) {
				if (l == i) {
					return node.value;
				}
				node = node.next;
				l++;
			}
		}
		return -1;
	}

	public void switchLocation() {
		Node node = first;
		first = last;
		last = node;
	}

	public void add(float value) {
		if (last != null) {
			if (last.next == null) {
				Node node;
				if (die == null) {
					node = new Node(value);
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
			Node node = new Node(value);
			first = last = node;
		}
		size++;
	}

	public void addFirst(float value) {
		if (first != null) {
			if (first.last == null) {
				Node node;
				if (die == null) {
					node = new Node(value);
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
			Node node = new Node(value);
			first = last = node;
		}
		size++;
	}

	public void addLast(float value) {
		add(value);
	}

	public void set(float value) {
		if (next != null) {
			next.value = value;
		}
	}

	public float removeFirst() {
		if (first != null) {
			Node node = first;
			float value = node.value;
			if (node == next) {
				next = node.next;
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

	public float removeLast() {
		if (last != null) {
			Node node = last;
			float value = node.value;
			if (next == node) {
				next = node.last;
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

	public float prevoious() {
		float value = next.value;
		next = next.last;
		if (next == null) {
			next = EMTY;
		}
		num--;
		return value;
	}

	public float next() {
		float value = next.value;
		next = next.next;
		if (next == null) {
			next = EMTY;
		}
		num++;
		return value;
	}

	public int getCurrentIndex() {
		return num - 1;
	}

	public int size() {
		return size;
	}

	public void clear() {
		if (last != null) {
			last.next = die;
			die = first;
		}
		first = null;
		last = null;
		next = null;
		size = 0;
	}

	public float getRecycle() {
		if (die != null) {
			return die.value;
		}
		return -1;
	}

	public int equalsSome(int i) {
		if (size == 0 || i >= size || i < 0) {
			return -1;
		}
		Node mN = null;
		int bin = size / 2;
		if (i > bin) {
			Node node = last;
			int l = size - 1;
			while (node != null) {
				if (l == i) {
					mN = node;
					break;
				}
				node = node.last;
				l--;
			}
		} else {
			Node node = first;
			int l = 0;
			while (node != null) {
				if (l == i) {
					mN = node;
					break;
				}
				node = node.next;
				l++;
			}
		}
		if (mN != null) {
			if (mN.last != null && mN.last.value == mN.value) {
				return 0;
			} else if (mN.next != null && mN.next.value == mN.value) {
				return 1;
			}
		}
		return -1;
	}

	public float toFromLast(int location, boolean canNext) {
		if (last == null) {
			next = null;
			num = -1;
			return INVALID;
		}
		Node node = last;
		for (int i = 0; i < location; i++) {
			if ((node = node.last) == null) {
				return INVALID;
			}
		}
		if (canNext) {
			next = node.last;
			num = size - location - 1;
		} else {
			next = node;
			num = size - location;
		}
		return node.value;
	}

	public float getNext() {
		if (next != null && next != EMTY) {
			return next.value;
		}
		return -1;
	}

	public float getPrevoious(int num) {
		if (next == null || next == EMTY || next == first) {
			return -1;
		}
		Node node = next.last;
		int i = 0;
		while (node != null) {
			if (i == num) {
				return node.value;
			}
			i++;
			node = node.last;
		}
		return -1;
	}

	public float toFromFirst(int location, boolean canNext) {
		if (first == null) {
			next = null;
			return INVALID;
		}
		Node node = first;
		for (int i = 0; i < location; i++) {
			if ((node = node.next) == null) {
				return INVALID;
			}
		}
		if (canNext) {
			next = node.next;
			num = location + 1;
		} else {
			next = node;
			num = location;
		}
		return node.value;
	}

	private static class Node {

		private float value;
		private Node last;
		private Node next;

		public void clearNext() {
			if (next != null) {
				next.clearNext();
				next = null;
				last = null;
				value = 0;
			}
		}

		public void clearPrevoious() {
			if (last != null) {
				last.clearPrevoious();
				last = null;
				next = null;
				value = 0;
			}
		}

		public Node(float value) {
			this.value = value;
		}

		public Node setValue(float value) {
			this.value = value;
			return this;
		}
	}

}
