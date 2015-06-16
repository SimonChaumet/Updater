package fr.scarex.updater.utils;

public class Grouper<V1, V2>
{
	private V1 firstValue;
	private V2 secondValue;
	
	public Grouper(V1 firstValue, V2 secondValue) {
		this.firstValue = firstValue;
		this.secondValue = secondValue;
	}
	
	public V1 getFirstValue() {
		return firstValue;
	}
	
	public V2 getSecondValue() {
		return secondValue;
	}
	
	public boolean equals(Grouper<V1, V2> obj) {
		return this.firstValue.equals(obj.firstValue) && this.secondValue.equals(obj.secondValue);
	}
	
	public boolean isFirstValueEqualTo(V1 obj) {
		return this.firstValue.equals(obj);
	}
	
	public boolean isSecondValueEqualTo(V2 obj) {
		return this.secondValue.equals(obj);
	}
}
