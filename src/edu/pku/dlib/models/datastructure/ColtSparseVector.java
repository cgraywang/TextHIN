package edu.pku.dlib.models.datastructure;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;

public class ColtSparseVector extends SparseDoubleMatrix1D {

//	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8154426281608727474L;
	private double normValue;
	
//	protected JavaIntDoubleHashMap elements;
//	protected TroveIntDoubleHashMap elements;
	
	public ColtSparseVector(double[] values) {
		this(values.length);
		assign(values);
	}
	public ColtSparseVector(int size) {
		this(size,size/1000,0.2,0.5);
	}
	public ColtSparseVector(int size, int initialCapacity, double minLoadFactor, double maxLoadFactor) {
		super(size, initialCapacity, minLoadFactor, maxLoadFactor);
//		this.elements = new JavaIntDoubleHashMap();
//		this.elements = new TroveIntDoubleHashMap();
	}
	public double getQuick(int index) {
		return elements.get(index);
	}
	
	public void setQuick(int index, double value) {
		if (value == 0)
			this.elements.removeKey(index);
		else 
			this.elements.put(index, value);
	}

	public void getNonZeros(IntArrayList indexList, DoubleArrayList valueList) {
		boolean fillIndexList = indexList != null;
		boolean fillValueList = valueList != null;
		if (fillIndexList) 
			indexList.clear(); 
		else {
			System.out.println("Wrong use");
			return;
		}
		if (fillValueList) 
			valueList.clear();
		else {
			System.out.println("Wrong use");
			return;
		}
		int[] index = elements.keys().elements();
		double[] value = elements.values().elements(); 
		indexList.elements(index);
		valueList.elements(value);
//		for (int i = 0; i < index.size(); ++i) {
//			indexList.add(index.get(i));
//			valueList.add(value.get(i));
//		}
		
//		int s = size;
//		for (int i=0; i < s; i++) {
//			double value = getQuick(i);
//			if (value != 0) {
//				if (fillIndexList) indexList.add(i);
//				if (fillValueList) valueList.add(value);
//			}
//		}
	}
	public void setNormValue(double value) {
		this.normValue = value;
	}
	
	public double getNormValue () {
		return this.normValue;
	}
	

}
