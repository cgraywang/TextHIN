package edu.pku.dlib.models.featureselection;

import java.util.logging.Logger;

import edu.pku.dlib.models.util.MaxHeap;


import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

/**
 * Unsupervised feature selection using Laplacian score
 * See
 * @inproceedings{He05,
 * author    = {Xiaofei He and Deng Cai and Partha Niyogi},
 * title     = {Laplacian Score for Feature Selection},
 * booktitle = {Advances in Neural Information Processing Systems (NIPS)},
 * year      = {2005},
 * };
 * Input: convert feature sequence or feature vector to a dense matrix 
 * (sparse matrix is considered as a future work  )
 * Output: Feature rank
 * 
 * @author Yangqiu Song @ IBM CRL
 */
public class LaplacianScore {
	
	private DoubleMatrix2D fMat = null;
	private SparseDoubleMatrix2D Laplacian = null;
	private double[] sumRow = null;
	MaxHeap lScore = null;
	private static Logger logger = Logger.getLogger("com.ibm.feature.selection");	

	public LaplacianScore(DoubleMatrix2D features) {
		fMat = features;
		Laplacian = new SparseDoubleMatrix2D(fMat.rows(), fMat.rows());
		lScore = new MaxHeap(fMat.columns());
	}
	

/*	
 * Compute graph Laplacian
 * input: nearest neighbor number: numK
 *        graph type: type = "binary" or "heat" (using selftuning method)
 * @inproceedings{DBLP:conf/nips/Zelnik-ManorP04,
 *   author    = {Lihi Zelnik-Manor and Pietro Perona},
 *   title     = {Self-Tuning Spectral Clustering},
 *   booktitle = {Advances in Neural Information Processing Systems (NIPS)},
 *   year      = {2004},
 * }
*/	
	void computeLaplacian(int numK, String type) {
		if (this.fMat == null) {
			logger.warning("No feature loaded");
			return;
		}
		double[] colNorm = new double[fMat.rows()];
		
		DoubleMatrix1D rowVector = null;
		for (int i = 0; i < fMat.rows(); ++i) {
			rowVector = (DoubleMatrix1D) fMat.viewRow(i);
			colNorm[i] = rowVector.zDotProduct(rowVector);
//			colNorm[i] = 0;
//			for (int j = 0; j < fMat.columns(); ++j) {
//				colNorm[i] += fMat.getQuick(i, j) * fMat.getQuick(i, j);
//			}
		}
		
		Laplacian.assign(0);
		
		// Find weighted adjacency matrix
		double[] midValues = new double[fMat.rows()];
		
		DoubleMatrix1D rowVector1 = null;
		DoubleMatrix1D rowVector2 = null;
		for (int i = 0; i < fMat.rows(); ++i) {		
			if (i % 100 == 0)
				System.out.println("Laplacian:" + i);
				
			double dotProd = 0;
			double euclideanDis = 0;
			rowVector1 = (DoubleMatrix1D) fMat.viewRow(i);
			MaxHeap maxHeap = new MaxHeap(numK);
		    for (int j = 0; j < numK; ++j) {
		    	maxHeap.insert(0, Double.MAX_VALUE);
		    }
			for (int j = 0; j < fMat.rows(); ++j) {
				rowVector2 = (DoubleMatrix1D) fMat.viewRow(j);
				dotProd = rowVector1.zDotProduct(rowVector2);
//				dotProd = 0;
//				for (int k = 0; k < fMat.columns(); ++k) {
//					dotProd += fMat.getQuick(i, k) * fMat.getQuick(j, k);
//				}
				euclideanDis = colNorm[i] + colNorm[j] - 2 * dotProd + Double.MIN_VALUE;
				if (euclideanDis < maxHeap.max() && i != j) {
					maxHeap.changeMax(j, euclideanDis);
				}
			}
			
			int[] indices = maxHeap.getIndices();
			double[] values = maxHeap.getValues();
			for (int j = 0; j < indices.length; ++j) {
				Laplacian.setQuick(i, indices[j], values[j]);
			}
			// shouldn't comment this line!
			int[] newindices = MaxHeap.heapSort(maxHeap);
			midValues[i] = Math.sqrt(values[(values.length/2)]);

		}
				
		// Set symmetric
		for (int i = 0; i < Laplacian.rows(); ++i) {
			for (int j = 0; j < Laplacian.columns(); ++j) {
				double temp1 = Laplacian.getQuick(i, j);
				double temp2 = Laplacian.getQuick(j, i);
				if (temp1 != 0.0 && temp1 != temp2) {
					Laplacian.setQuick(j, i, temp1);
				}
			}
		}
		
		Laplacian.trimToSize();

	
		// Find weighted adjacency matrix
		// Need to improve to use non-zeroes elements
		// getNonZeros(IntArrayList rowList, IntArrayList columnList, DoubleArrayList valueList)
		sumRow = new double[Laplacian.columns()];
		for (int i = 0; i < Laplacian.rows(); ++i) {
			for (int j = 0; j < Laplacian.columns(); ++j) {
				if (Laplacian.getQuick(i, j) != 0.0) {
					double value = 0.0;
					if (type.equalsIgnoreCase("binary")) {
						value = 1.0;
					} else if (type.equalsIgnoreCase("heat")) {
						value = Math.exp( - Laplacian.get(i, j)/(midValues[i]+Double.MIN_VALUE)/(midValues[j]+Double.MIN_VALUE)/2);
					}
					sumRow[i] += value;
					Laplacian.setQuick(i, j, value);
				}
			}			
		}
		// Find Laplacian
		// Need to improve to use non-zeroes elements
		// getNonZeros(IntArrayList rowList, IntArrayList columnList, DoubleArrayList valueList)
		for (int i = 0; i < Laplacian.rows(); ++i) {
			double value = Laplacian.get(i, i);
			Laplacian.set(i, i, sumRow[i] - value);
			for (int j = i+1; j < Laplacian.columns(); ++j) {
				if (Laplacian.get(i, j) != 0.0) {
					value = Laplacian.get(i, j);
					Laplacian.set(i, j, - value);
					Laplacian.set(j, i, - value);
				}
			}			
		}
	}
	
	public int[] laplacianScore(int numK, String type, double[] weights) {
		if (this.fMat == null) {
			logger.warning("No feature loaded");
			return null;
		}
		System.out.println("computeLaplacian");
		computeLaplacian(numK, type);
		DoubleMatrix2D diagSum = new SparseDoubleMatrix2D(fMat.rows(), fMat.rows()); 
		for (int i = 0; i < fMat.rows(); ++i) {
			diagSum.setQuick(i, i, sumRow[i]);
		}
		diagSum.trimToSize();

		DenseDoubleMatrix1D  onesVector = new DenseDoubleMatrix1D (fMat.rows());
		onesVector.assign(1);
		DenseDoubleMatrix1D  zerosVector = new DenseDoubleMatrix1D (fMat.rows());
		zerosVector.assign(0);

		// f = f - \frac{f^T D 1}{1^T D 1} 1
		System.out.println("compute weight");
//		DoubleMatrix1D[] trans_vec = new DoubleMatrix1D[fMat.columns()];
//		for (int i = 0; i < fMat.columns(); i++)
//		{
//			trans_vec[i] = fMat.viewColumn(i).copy();
//		}
		for (int i = 0; i < fMat.columns(); ++i) {
			if (i % 100 == 0)
				System.out.println("have handle " + i + " feature");
			DoubleMatrix1D colVector = null;
//			colVector = trans_vec[i];
			colVector = fMat.viewColumn(i).copy();
			DoubleMatrix1D tempVector = null;
			
			// potential problem: diagSum.zMult(onesVector, tempVector) will lead tempVector as null!!
			tempVector = diagSum.zMult(onesVector, tempVector);
			double temp1 = colVector.zDotProduct(tempVector);
			double temp2 = onesVector.zDotProduct(onesVector);
			
			for (int j = 0; j < colVector.size(); ++j) {
				double temp3 = colVector.getQuick(j);
				double temp4 = temp3 - temp1/temp2;
				colVector.setQuick(j, temp4);
			}
			
			Laplacian.zMult(colVector, tempVector);
			temp1 = colVector.zDotProduct(tempVector);
			diagSum.zMult(colVector, tempVector);
			temp2 = colVector.zDotProduct(tempVector);
			
			lScore.insert(i, temp1/(temp2+Double.MIN_VALUE));
			weights[i] = temp1/(temp2+Double.MIN_VALUE);
		}
		int[] sortedIndices = MaxHeap.heapSort(lScore);
		//reverse array
//		sortedIndices = lScore.getIndices();
//		double[] scores = lScore.getValues();
//		for (double score: scores)
//				System.out.print(score + " ");
//		System.out.println();
		return sortedIndices;
		
	}
	
	public double[] getRankValues() {
		return lScore.getValues();
	}
	
	class DoubleAddDoubleFunction implements DoubleDoubleFunction {
		public double apply(double arg0, double arg1) {
			return arg0 + arg1;
		}
	}
	
	// mini test
	public static void main (String[] args) {
				double[][] feature = {
							  {1, 0, 0},
							  {1, 0, 0},
							  {1, 0, 0},
							  {1, 0, 0},
							  {1, 0, 0},
							  {0, 0, 0},
							  {0, 0, 1},
							  {0, 0, 1},
							  {0, 1, 1},
							  {0, 1, 1},
							  {0, 1, 1},
							  {0, 1, 1},};	
/*		double[][] feature = {
				  {11, 0, 1},
				  {12, 0, 2},
				  {13, 0, 3},
				  {14, 0, 4},
				  {0, 15, 5},
				  {0, 16, 6},
				  {0, 17, 7},
				  {0, 18, 8}};	*/
		
/*		double[][] feature = {
				  {1, 1, 0, 1, 1, 0},
				  {1, 1, 0, 1, 1, 0},
				  {1, 1, 0, 1, 0, 1},
				  {1, 1, 0, 1, 1, 1},
				  {0, 0, 1, 1, 1, 1},
				  {0, 0, 1, 0, 0, 1},
				  {0, 0, 1, 0, 1, 1},
				  {0, 0, 1, 0, 1, 1}};	*/
		
		LaplacianScore ls = new LaplacianScore(new DenseDoubleMatrix2D(feature));
		int[] indices = null;
		double[] weight  = new double[3];
		
//		double[] values = null;
		for (int i = 0; i < weight.length; i++)
			System.out.print(weight[i] + " ");
		System.out.println();
		int neighborNum = 3;
		
		indices = ls.laplacianScore(neighborNum, "heat", weight);
		for (int i = 0; i < indices.length; i++)
			System.out.print(indices[i] + " ");
		System.out.print("\n");
		for (int i = 0; i < indices.length; i++)
			System.out.print(weight[i] + " ");
		System.out.println("\n");
	}
	
	
}
