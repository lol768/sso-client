package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;

public final class Pair<A extends Serializable,B extends Serializable> implements Serializable {
	private static final long serialVersionUID = 8170177505650857297L;
	private final A first;
	private final B second;
	public Pair(A first, B second) {
		super();
		this.first = first;
		this.second = second;
	}
	public A getFirst() {
		return first;
	}
	public B getSecond() {
		return second;
	}
	
	public static Pair<String,String> of(String a, String b) {
		return new Pair<String,String>(a, b);
	}
	
	public String toString() {
		return "[" + first + "," + second + "]";
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		try {
			Pair<String,String> p = (Pair<String, String>) o;
			return p.first.equals(first) && p.second.equals(second);
		} catch (ClassCastException cce) {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return first.hashCode() + second.hashCode();
	}
}
