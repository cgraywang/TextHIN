package models.datastructure;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;

public class ColtDenseVector extends DenseDoubleMatrix1D {

//	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double normValue;

	public ColtDenseVector(double[] values) {
		this(values.length);
		assign(values);
	}
	public ColtDenseVector(int size) {
		super(size);
	}
	public double getQuick(int index) {
		return elements[index];
	}
	public void setQuick(int index, double value) {
		elements[index] = value;
	}
	
	public void setNormValue(double value) {
		this.normValue = value;
	}
	
	public double getNormValue () {
		return this.normValue;
	}
}
