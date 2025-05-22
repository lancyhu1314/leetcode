
package com.suning.fab.tup4ml.elfin;

/**
* @author 14050269 Howard
* @since 2016年6月1日 下午8:25:40
* 一个映射对，类模板
*/
public class Pair<F,S> {
	F first;
	S second;
	
	public F getFirst() {
		return first;
	}
	
	public S getSecond() {
		return second;
	}
	
	public Pair(F first, S second) {
		super();
		this.first = first;
		this.second = second;
	}

}
