package models.util.matrix;

import java.util.ArrayList;
import java.util.List;

import models.datastructure.ColtDenseVector;
import models.datastructure.ColtSparseVector;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;

/**
 * Matrix Utils
 * 
 * @author Yangqiu Song
 */

public class Matrix2DUtil {
	
	// return a dense vector that contains column/row max values
	static public DoubleMatrix1D[] matrixMax (DoubleMatrix2D mat, int dim) {
		DoubleMatrix1D[] maxVector = new DoubleMatrix1D[2];
		if (dim == 1) {
			maxVector[0] = new ColtDenseVector(mat.columns());
			maxVector[1] = new ColtDenseVector(mat.columns());
			for (int i = 0; i < mat.columns(); ++i) {
				double value = 0;
				double index = 0;
				for (int j = 0; j < mat.rows(); ++j) {
					if (mat.getQuick(j, i) > value) {
						value = mat.getQuick(j, i);
						index = j;
					}
				}
				maxVector[0].setQuick(i, value);
				maxVector[1].setQuick(i, index);
			}
		} else if (dim == 2) {
			maxVector[0] = new ColtDenseVector(mat.rows());
			maxVector[1] = new ColtDenseVector(mat.rows());
			for (int i = 0; i < mat.rows(); ++i) {
				double value = 0;
				double index = 0;
				for (int j = 0; j < mat.columns(); ++j) {
					if (mat.getQuick(i, j) > value) {
						value = mat.getQuick(i, j);
						index = j;
					}
				}
				maxVector[0].setQuick(i, value);
				maxVector[1].setQuick(i, index);
			}
		} else {
			System.err.println("No match dimension!");
		}
		
		
		return maxVector;		
	}
	
	static public DoubleMatrix1D[] matrixMin (DoubleMatrix2D mat, int dim) {
		DoubleMatrix1D[] minVector = new DoubleMatrix1D[2];
		if (dim == 1) {
			minVector[0] = new ColtDenseVector(mat.columns());
			minVector[1] = new ColtDenseVector(mat.columns());
			for (int i = 0; i < mat.columns(); ++i) {
				double value = Double.MAX_VALUE;
				double index = 0;
				for (int j = 0; j < mat.rows(); ++j) {
					if (mat.getQuick(j, i) < value) {
						value = mat.getQuick(j, i);
						index = j;
					}
				}
				minVector[0].setQuick(i, value);
				minVector[1].setQuick(i, index);
			}
		} else if (dim == 2) {
			minVector[0] = new ColtDenseVector(mat.rows());
			minVector[1] = new ColtDenseVector(mat.rows());
			for (int i = 0; i < mat.rows(); ++i) {
				double value = Double.MAX_VALUE;
				double index = 0;
				for (int j = 0; j < mat.columns(); ++j) {
					if (mat.getQuick(i, j) < value) {
						value = mat.getQuick(i, j);
						index = j;
					}
				}
				minVector[0].setQuick(i, value);
				minVector[1].setQuick(i, index);
			}
		} else {
			System.err.println("No match dimension!");
		}
		
		
		return minVector;		
	}

	static public DoubleMatrix1D matrixSum (DoubleMatrix2D mat, int dim) {
		DoubleMatrix1D maxVector = null;
		if (dim == 1) {
			maxVector = new ColtDenseVector(mat.columns());
			for (int i = 0; i < mat.columns(); ++i) {
				double value = 0;
				for (int j = 0; j < mat.rows(); ++j) {
					value += mat.getQuick(j, i);
				}
				maxVector.setQuick(i, value);
			}
		} else if (dim == 2) {
			maxVector = new ColtDenseVector(mat.rows());
			for (int i = 0; i < mat.rows(); ++i) {
				double value = 0;
				for (int j = 0; j < mat.columns(); ++j) {
					value += mat.getQuick(i, j);
				}
				maxVector.setQuick(i, value);
			}
		} else {
			System.err.println("No match dimension!");
		}
		return maxVector;		
	}
	
	// NOTE! assume the rows in matrix have unique size
	static public DoubleMatrix1D matrixSum (List<DoubleMatrix1D> mat, int dim) {
		DenseDoubleMatrix1D maxVector = null;
		if (dim == 2) { // row sum valuse
			maxVector = new ColtDenseVector(mat.size());
		} else if(dim == 1) { // column sum value
			maxVector = new ColtDenseVector(mat.get(0).size());
		}
		
		for (int i = 0; i < mat.size(); ++i) {
			for (int j = 0; j < mat.get(i).size(); ++j) {
				if (dim == 2) {
					maxVector.setQuick(i, maxVector.getQuick(i) + mat.get(i).getQuick(j));
				} else if (dim == 1) {
					maxVector.setQuick(j, maxVector.getQuick(j) + mat.get(i).getQuick(j));
				}
			}
		}
		return maxVector;		
	}
	
	static public DoubleMatrix1D[] matrixMin (List<DoubleMatrix1D> mat, int dim) {
		DoubleMatrix1D[] minVector = new DenseDoubleMatrix1D[2];
		if (dim == 2) {
			minVector[0] = new ColtDenseVector(mat.size());
			minVector[0].assign(Double.MAX_VALUE);
			minVector[1] = new ColtDenseVector(mat.size());
			minVector[1].assign(Double.MAX_VALUE);
		} else if (dim == 1) {
			minVector[0] = new ColtDenseVector(mat.get(0).size());
			minVector[0].assign(Double.MAX_VALUE);
			minVector[1] = new ColtDenseVector(mat.get(0).size());
			minVector[1].assign(Double.MAX_VALUE);
		}
		
		for (int i = 0; i < mat.size(); ++i) {
			for (int j = 0; j < mat.get(i).size(); ++j) {
				double ele = mat.get(i).getQuick(j);
				if (dim == 2) {
					double value = minVector[0].getQuick(i);
					if (value > ele) {
						minVector[0].setQuick(i, ele);
						minVector[1].setQuick(i, j);
					}
				}
				if (dim == 1) {
					double value = minVector[0].getQuick(j);
					if (value > ele) {
						minVector[0].setQuick(j, ele);
						minVector[1].setQuick(j, i);
					}
				}
			}
		}
		minVector[0].trimToSize();
		minVector[1].trimToSize();
				
		return minVector;		
	}
	
	static public DoubleMatrix1D[] matrixMax (List<DoubleMatrix1D> mat, int dim) {
		DoubleMatrix1D[] minVector = new DenseDoubleMatrix1D[2];
		if (dim == 2) {
			minVector[0] = new ColtDenseVector(mat.size());
			minVector[0].assign(Double.MIN_VALUE);
			minVector[1] = new ColtDenseVector(mat.size());
			minVector[1].assign(Double.MIN_VALUE);
		} else if (dim == 1) {
			minVector[0] = new ColtDenseVector(mat.get(0).size());
			minVector[0].assign(Double.MIN_VALUE);
			minVector[1] = new ColtDenseVector(mat.get(0).size());
			minVector[1].assign(Double.MIN_VALUE);
		}
		
		for (int i = 0; i < mat.size(); ++i) {
			for (int j = 0; j < mat.get(i).size(); ++j) {
				double ele = mat.get(i).getQuick(j);
				if (dim == 2) {
					double value = minVector[0].getQuick(i);
					if (value < ele) {
						minVector[0].setQuick(i, ele);
						minVector[1].setQuick(i, j);
					}
				}
				if (dim == 1) {
					double value = minVector[0].getQuick(j);
					if (value < ele) {
						minVector[0].setQuick(j, ele);
						minVector[1].setQuick(j, i);
					}
				}
			}
		}
		minVector[0].trimToSize();
		minVector[1].trimToSize();
				
		return minVector;		
	}
	
	// matrix must be dense
	static public List<DoubleMatrix1D> DenseMultDense(List<DoubleMatrix1D> A,
								List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.get(0).size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < m; ++i) {
				C.add(new ColtDenseVector(p));
			}
		}
		if (B.size() != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < p; ++j) {
				if (B.size() != A.get(i).size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
				double sum = 0.0;
				for (int k = 0; k < n; ++k) {
					double temp1 = A.get(i).getQuick(k);
					double temp2 = B.get(k).getQuick(j);
					sum += temp1 * temp2;
				}
				C.get(i).setQuick(j, sum);
			}
		}
		
		return C;
	}
	
	static public List<DoubleMatrix1D> DenseMultDenseTranspose(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < m; ++i) {
				C.add(new ColtDenseVector(p));
			}
		}
		if (B.get(0).size() != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < m; ++i) {
			DoubleMatrix1D vector1 = A.get(i);
			for (int j = 0; j < p; ++j) {
				DoubleMatrix1D vector2 = B.get(j);

				if (vector2.size() != vector1.size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
				double sum = 0.0;
				for (int k = 0; k < vector1.size(); ++k) {
					sum += vector1.get(k) * vector2.get(k);
				}
				C.get(i).setQuick(j, sum);
			}	
		}
		return C;
	}
	
	static public List<DoubleMatrix1D> DenseTransposeMultDense(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.get(0).size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < n; ++i) {
				C.add(new ColtDenseVector(p));
			}
		}
		if (B.size() != m)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < p; ++j) {
				if (B.size() != A.size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
				double sum = 0.0;
				for (int k = 0; k < m; ++k) {
					double temp1 = A.get(k).getQuick(i);
					double temp2 = B.get(k).getQuick(j);
					sum += temp1 * temp2;
				}
				C.get(i).setQuick(j, sum);
			}
		}
		
		return C;
	}
	

	static public List<DoubleMatrix1D> SparseMultDense(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.get(0).size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < m; ++i) {
				C.add(new ColtDenseVector(p));
			}
		}
		if (B.size() != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < p; ++j) {
				if (B.size() != A.get(i).size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
					double sum = 0.0;
					for (int k = 0; k < n; ++k) {
						double temp1 = A.get(i).getQuick(k);
						if (temp1 != 0) {
							double temp2 = B.get(k).getQuick(j);
							sum += temp1 * temp2;
						}
					}
				C.get(i).setQuick(j, sum);
			}
		}
		
		return C;
	}
	
	static public List<DoubleMatrix1D> SparseTransposeMultDense(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.get(0).size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < n; ++i) {
				C.add(new ColtDenseVector(p));
			}
		}
		if (B.size() != m)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < p; ++j) {
				if (B.size() != A.size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
				double sum = 0.0;
				for (int k = 0; k < m; ++k) {
					double temp1 = A.get(k).getQuick(i);
					if (temp1 != 0) {
						double temp2 = B.get(k).getQuick(j);
						sum += temp1 * temp2;
					}
				}
				C.get(i).setQuick(j, sum);
			}
		}
		
		return C;
	}
	
	static public List<DoubleMatrix1D> SparseMultSparse(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.get(0).size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < m; ++i) {
				C.add(new ColtSparseVector(p));
			}
		}
		if (B.size() != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < m; ++i) {
			IntArrayList indexList = new IntArrayList();
			DoubleArrayList valueList = new DoubleArrayList();
			A.get(i).getNonZeros(indexList, valueList);
			for (int j = 0; j < p; ++j) {
				if (B.size() != A.get(i).size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
				double sum = 0.0;
				for (int k = 0; k < indexList.size(); ++k) {
					int index = indexList.get(k);
					double value1 = valueList.get(k);
					double value2 = B.get(index).getQuick(j); 
					if (value1 != 0 || value2 != 0) { 
						sum += value1 * value2;
					}
				}
				C.get(i).setQuick(j, sum);
			}	
		}
		return C;
	}

	static public List<DoubleMatrix1D> SparseMultSparseTranspose(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
		int m = A.size();
		int n = A.get(0).size();
		int p = B.size();
		List<DoubleMatrix1D> C = null;
		if (C==null) {
			C = new ArrayList<DoubleMatrix1D>();
			for (int i = 0; i < m; ++i) {
				C.add(new ColtSparseVector(p));
			}
		}
		if (B.get(0).size() != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
		for (int i = 0; i < m; ++i) {
			IntArrayList indexList = new IntArrayList();
			DoubleArrayList valueList = new DoubleArrayList();
			A.get(i).getNonZeros(indexList, valueList);
			for (int j = 0; j < p; ++j) {
				if (B.get(j).size() != A.get(i).size())
					throw new IllegalArgumentException("Matrix2D inner dimensions must agree.");
				double sum = 0.0;
				for (int k = 0; k < indexList.size(); ++k) {
					int index = indexList.get(k);
					double value1 = valueList.get(k);
					double value2 = B.get(j).getQuick(index); 
					if (value1 != 0 || value2 != 0) { 
						sum += value1 * value2;
					}
				}
				C.get(i).setQuick(j, sum);
			}	
		}
		return C;
	}
	
	static public List<DoubleMatrix1D> SparseTransposeMultSparse(List<DoubleMatrix1D> A,
			List<DoubleMatrix1D> B) {
//		List<DoubleMatrix1D> AT = new ArrayList<DoubleMatrix1D>();
//		for (int i = 0; i < A.get(0).size(); ++i) {
//			AT.add(new ColtSparseVector(A.size()));
//		}
//		for (int i = 0; i < A.size(); ++i) {
//			IntArrayList indexList = new IntArrayList();
//			DoubleArrayList valueList = new DoubleArrayList();
//			A.get(i).getNonZeros(indexList, valueList);
//			for (int k = 0; k < indexList.size(); ++k) {
//				int index = indexList.get(k);
//				double value = valueList.get(k);
//				AT.get(index).set(i, value);
//			}
//		}
		List<DoubleMatrix1D> AT = getSparseTranspose(A);
		List<DoubleMatrix1D> C = SparseMultSparse(AT, B);
		AT = null;
		return C;
	}
	
	static public List<DoubleMatrix1D> inverseDense(List<DoubleMatrix1D> A) {
		List<DoubleMatrix1D> C = null;
		
		DoubleMatrix2D A1 = new DenseDoubleMatrix2D(A.size(), A.get(0).size());
		for (int i = 0; i < A.size(); ++i) {
			DoubleMatrix1D vector = A.get(0);
			for (int j = 0; j < vector.size(); ++j ) {
				A1.set(i, j, vector.get(j));
			}
		}
		
		double tol = 0.001;
		for (int i = 0; i < A.size(); ++i) {
			A1.set(i, i, A1.get(i, i) + tol);
		}
		
		Algebra algebra = new Algebra();
		DoubleMatrix2D C1 = algebra.inverse(A1);
		
		C = new ArrayList<DoubleMatrix1D>();
		for (int i = 0; i < A.size(); ++i) {
			C.add(new ColtDenseVector(A.get(0).size()));
		}
		for (int i = 0; i < C1.rows(); ++i) {
			for (int j = 0; j < C1.columns(); ++j ) {
				C.get(i).set(j, C1.get(i, j));
			}
		}
		return C;
	}
	
	static public List<DoubleMatrix1D> inverseSparse(List<DoubleMatrix1D> A) {
		
		// TODO: optimize this code
		
		List<DoubleMatrix1D> C = null;
		
		DoubleMatrix2D A1 = new SparseDoubleMatrix2D(A.size(), A.get(0).size());
		for (int i = 0; i < A.size(); ++i) {
			DoubleMatrix1D vector = A.get(i);
			for (int j = 0; j < vector.size(); ++j ) {
				A1.set(i, j, vector.get(j));
			}
		}
		double tol = 0.001;
		for (int i = 0; i < A.size(); ++i) {
			A1.set(i, i, A1.get(i, i) + tol);
		}
		Algebra algebra = new Algebra();
		DoubleMatrix2D C1 = algebra.inverse(A1);
		
		C = new ArrayList<DoubleMatrix1D>();
		for (int i = 0; i < A.size(); ++i) {
			C.add(new ColtSparseVector(A.get(0).size()));
		}
		for (int i = 0; i < C1.rows(); ++i) {
			for (int j = 0; j < C1.columns(); ++j ) {
				C.get(i).set(j, C1.get(i, j));
			}
		}
		return C;
	}
	
	static public List<DoubleMatrix1D> getSparseTranspose(List<DoubleMatrix1D> A) {
		List<DoubleMatrix1D> AT = new ArrayList<DoubleMatrix1D>();
		for (int i = 0; i < A.get(0).size(); ++i) {
			AT.add(new ColtSparseVector(A.size()));
		}
		for (int i = 0; i < A.size(); ++i) {
			IntArrayList indexList = new IntArrayList();
			DoubleArrayList valueList = new DoubleArrayList();
			A.get(i).getNonZeros(indexList, valueList);
			for (int k = 0; k < indexList.size(); ++k) {
				int index = indexList.get(k);
				double value = valueList.get(k);
				AT.get(index).set(i, value);
			}
		}
		return AT;
	}
	
	static public double product(DoubleMatrix1D v1, DoubleMatrix1D v2) {
		if (v1 instanceof SparseDoubleMatrix1D) {
			return productQuick(v1, v2);
		} else if (v2 instanceof SparseDoubleMatrix1D) {
			return productQuick(v2, v1);
		} else {
			return v1.zDotProduct(v2);
		}
	}
	
	static public double productQuick(IntArrayList idxA, DoubleArrayList valueA, DoubleMatrix1D b)
	{
		double prod = 0.0;
		for (int i = 0; i < idxA.size(); ++i) {
			double temp = b.getQuick(idxA.getQuick(i));
			if (temp != 0.0) {
				prod += valueA.getQuick(i) * temp;
			}
		}
		return prod;
	}
	
	static public double productQuick(DoubleMatrix1D v1, DoubleMatrix1D v2) {
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		v1.getNonZeros(indexList, valueList);
		double prod = 0.0;
		for (int i = 0; i < indexList.size(); ++i) {
			double temp = v2.getQuick(indexList.getQuick(i));
			if (temp != 0.0) {
				prod += valueList.getQuick(i) * temp;
			}
		}

//		for (int i = 0; i < v1.size(); ++i) {
//			double temp1 = v1.getQuick(i);
//			double temp2 = v2.getQuick(i);
//			if (temp1 != 0.0 || temp2 != 0.0) {
//				prod += temp1 * temp2;
//			}
//		}
		return prod;
	}
	
	public static double getSqrSum(DoubleArrayList valueList)
	{
		double sum =0, tmp ;
		for (int i = 0; i < valueList.size(); i++)
		{
			tmp = valueList.get(i);
			sum += tmp * tmp;
		}
		
		return sum;
	}
	
	public static double getSqrSum(DoubleMatrix1D vector)
	{
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		vector.getNonZeros(indexList, valueList);
		double sum =0 ;
		for (int i = 0; i < indexList.size(); i++)
		{
			sum += valueList.get(i) * valueList.get(i);
		}
		indexList = null;
		valueList = null;
		return sum;
	}
}
 