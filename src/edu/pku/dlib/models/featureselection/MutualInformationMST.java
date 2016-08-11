package edu.pku.dlib.models.featureselection;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;


import edu.pku.dlib.models.clustering.ObjectWriter;
import edu.pku.dlib.models.graph.GraphWeightedMST;
import edu.pku.dlib.models.graph.WMSTTreeNode;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


/**
 * Unsupervised feature selection using mutual information and maximal spanning tree
 * 
 * See
 * @PHDTHESIS{Sahami98,
 * AUTHOR =       "Mehran Sahami",
 * TITLE =        "{Using Machine Learning to Improve Information Access}",
 * YEAR =         1998,
 * SCHOOL =       "Department of Computer Science, Stanford University",
 * ADDRESS =      "USA"
 * };
 * Input: feature matrix must be sparse, otherwise we can not evaluate mutual information 
 * (sparse matrix is considered as a future work  )
 * Output: Feature rank
 * 
 * @author Yangqiu Song @ IBM CRL
 */

public class MutualInformationMST {
	
	private DoubleMatrix2D fMat = null;
	private double[][] fArray;	// one row for a feature
	private GraphWeightedMST miGraph = null;
	private static Logger logger = Logger.getLogger("com.ibm.feature.selection");	
	private boolean isNormalized;

	public MutualInformationMST(DoubleMatrix2D features, boolean isNorm) {
		fMat = (DoubleMatrix2D) features;
		miGraph = new GraphWeightedMST(fMat.columns());
		isNormalized = isNorm;
		fMat.trimToSize();
	}
	
	public MutualInformationMST(double[][] features, boolean isNorm)
	{
		fArray = features;
		miGraph = new GraphWeightedMST(features.length);
		isNormalized = isNorm;
	}
	
	public MutualInformationMST(DoubleMatrix2D features) {
		this(features, false);
	}
	
	private double computeEuclidean(DoubleMatrix1D v1, DoubleMatrix1D v2) {
		assert(v1.size() == v2.size());
		
		double norm1 = v1.zDotProduct(v1);
		double norm2 = v2.zDotProduct(v2);
		double dot = v1.zDotProduct(v2);
				
		return Math.sqrt(norm1 + norm2 - 2 * dot);
	}
	
	private double computeSpecialEuclidean(DoubleMatrix1D v1, DoubleMatrix1D v2) {
		assert(v1.size() == v2.size());
		
		double norm1 = v1.zDotProduct(v1);
		double norm2 = v2.zDotProduct(v2);
		double dot = v1.zDotProduct(v2);
				
		return (1 - dot / ( Math.sqrt(norm1 * norm2) + Double.MIN_VALUE ) );
	}
	
	private double computeCorrelation(DoubleMatrix1D v1, DoubleMatrix1D v2) {
		assert(v1.size() == v2.size());
		int size = v1.size();

		double dot = v1.zDotProduct(v2);
		double v1mean = 0;
		double v2mean = 0;
		double v1square = 0;
		double v2square = 0;
		for (int i = 0; i < size; ++i) {
			v1mean += v1.getQuick(i);
			v2mean += v2.getQuick(i);
			v1square += v1.getQuick(i) * v1.getQuick(i);
			v2square += v2.getQuick(i) * v2.getQuick(i);
		}
		
		return  1 - Math.abs( (size * dot - v1mean * v2mean) /
				  ( Math.sqrt(size * v1square - v1mean * v1mean) + Double.MIN_VALUE) /
				  ( Math.sqrt(size * v2square - v2mean * v2mean) + Double.MIN_VALUE) );
	}

	
	private double computeSquareLossofPCA(DoubleMatrix1D v1, DoubleMatrix1D v2) {
		assert(v1.size() == v2.size());

		int size = v1.size();

		double dot = v1.zDotProduct(v2);
		double v1mean = 0;
		double v2mean = 0;
		double v1square = 0;
		double v2square = 0;
		for (int i = 0; i < size; ++i) {
			v1mean += v1.getQuick(i);
			v2mean += v2.getQuick(i);
			v1square += v1.getQuick(i) * v1.getQuick(i);
			v2square += v2.getQuick(i) * v2.getQuick(i);
		}
		
		double rho =  ( (size * dot - v1mean * v2mean) /
					  	(Math.sqrt(size * v1square - v1mean * v1mean) + Double.MIN_VALUE) /
					  	(Math.sqrt(size * v2square - v2mean * v2mean) + Double.MIN_VALUE) );
		
		v1mean /= size;
		v2mean /= size;
		
		double v1var = 0;
		double v2var = 0;
		for (int i = 0; i < size; ++i) {
			v1var += (v1.getQuick(i) - v1mean) * (v1.getQuick(i) - v1mean);
			v2var += (v2.getQuick(i) - v2mean) * (v2.getQuick(i) - v2mean);
		}
		v1var /= size;
		v2var /= size;
		
		return v1var + v2var - 
				Math.sqrt((v1var + v2var) * (v1var + v2var) - 4 * v1var * v2var * (1 - rho * rho));
		
	}
	
/**
 * Defined in following articles
	@article{Strehl02,
		  AUTHOR = {Alexander Strehl and Joydeep Ghosh},
		  TITLE = {Cluster Ensembles -- A Knowledge Reuse Framework for Combining Multiple Partitions},
		  JOURNAL = {Journal on Machine Learning Research (JMLR)},
		  PAGES = {583--617},
		  YEAR = {2002},
		  VOLUME = {3},
	};
	@inproceedings{Yang97,
  		author    = {Yiming Yang and Jan O. Pedersen},
  		title     = {A Comparative Study on Feature Selection in Text Categorization},
  		booktitle = {Proceedings of the Fourteenth International Conference on Machine Learning (ICML)},
  		year      = {1997},
  		pages     = {412-420},
	}
*/
	
	public double computeMutualInformation(double[] v1, double[] v2)
	{
		 assert(v1.length == v2.length);

		    double h1 = 0.0;
		    double h2 = 0.0;
		    double mi = 0.0;

		    double a = 0; 
		    double b = 0;
		    double c = 0; 
		    double d = 0;
		    double n = 0;
		    for (int i = 0; i < v1.length; ++i) {

		    	if (v1[i] != 0.0 && v2[i] != 0.0) { 
		    		a++; 
		    	} else if (v1[i] == 0.0 && v2[i] != 0.0) { 
		    		b++; 
		    	} else if (v1[i] != 0.0 && v2[i] == 0.0) { 
		    		c++; 
		    	} else if (v1[i] == 0.0 && v2[i] == 0.0) { 
		    		d++; 
		    	}
		    	n++;
		    }
		    
		    double temp1, temp2, temp3, temp4;
		    if (a != 0 && (a + b) * (a + c) != 0)
		    	temp1 = a / n * Math.log( a * n / (a + b) / (a + c) ) / Math.log(2);
		    else 
		    	temp1 = 0;
		    
		    if (b != 0 && (b + d) * (a + b) != 0)
		    	temp2 = b / n * Math.log( b * n / (b + d) / (a + b) ) / Math.log(2);
		    else 
		    	temp2 = 0;
		    
		    if (c != 0 && (c + d) * (a + c) != 0)
		    	temp3 = c / n * Math.log( c * n / (c + d) / (a + c) ) / Math.log(2);
		    else
		    	temp3 = 0;
		    
		    if (d != 0 && (b + d) * (c + d) != 0)
		    	temp4 = d / n * Math.log( d * n / (b + d) / (c + d) ) / Math.log(2);
		    else 
		    	temp4 = 0;
		    
		    mi = temp1 + temp2 + temp3 + temp4;
		    
		    if (isNormalized == true) {
		    	if ((a + c) != 0)
		    		temp1 = ((a + c) / n) * Math.log((a + c) / n) / Math.log(2);
		    	else 
		    		temp1 = 0;
		    	
		    	if ((b + d) != 0)
		    		temp2 = ((b + d) / n) * Math.log((b + d) / n) / Math.log(2);
		    	else 
		    		temp2 = 0;
		    	
		    	if ((a + b) != 0)
		    		temp3 = ((a + b) / n) * Math.log((a + b) / n) / Math.log(2);
		    	else
		    		temp3 = 0;
		    	
		    	if ((c + d) != 0)
		    		temp4 = ((c + d) / n) * Math.log((c + d) / n) / Math.log(2);
		    	else temp4 = 0;
		    	
		    	h1 = temp1 + temp2;
		    	h2 = temp3 + temp4;
		    	
		    	// since a + b + c + d = n, we can make sure h1*h2 != 0
		    	if (h1 * h2 == 0) 
		    		mi = 0;
		    	else
		    		mi /= Math.sqrt(h1 * h2);
		    }

		    return mi; 
	}
	
	public double computeCosine(double[] v1, double[] v2)
	{
		double sumxy = 0, sumxx = 0, sumyy = 0;
		if (v1.length != v2.length)
			System.out.println("error:mismatched length");
		for (int i = 0; i < v1.length; i++)
		{
			sumxy += v1[i] * v2[i];
			sumxx += v1[i] * v1[i];
			sumyy += v2[i] * v2[i];
		}
		if (sumxx * sumyy < 0.001)
			return 0;
		return sumxy / (Math.sqrt(sumxx * sumyy));
	}
	
	public double computeMutualInformation(DoubleMatrix1D v1, DoubleMatrix1D v2) {
	    assert(v1.size() == v2.size());

	    double h1 = 0.0;
	    double h2 = 0.0;
	    double mi = 0.0;

	    double a = 0; 
	    double b = 0;
	    double c = 0; 
	    double d = 0;
	    double n = 0;
	    for (int i = 0; i < v1.size(); ++i) {

	    	if (v1.getQuick(i) != 0.0 && v2.getQuick(i) != 0.0) { 
	    		a++; 
	    	} else if (v1.getQuick(i) == 0.0 && v2.getQuick(i) != 0.0) { 
	    		b++; 
	    	} else if (v1.getQuick(i) != 0.0 && v2.getQuick(i) == 0.0) { 
	    		c++; 
	    	} else if (v1.getQuick(i) == 0.0 && v2.getQuick(i) == 0.0) { 
	    		d++; 
	    	}
	    	n++;
	    }
	    
	    double temp1, temp2, temp3, temp4;
	    if (a != 0 && (a + b) * (a + c) != 0)
	    	temp1 = a / n * Math.log( a * n / (a + b) / (a + c) ) / Math.log(2);
	    else 
	    	temp1 = 0;
	    
	    if (b != 0 && (b + d) * (a + b) != 0)
	    	temp2 = b / n * Math.log( b * n / (b + d) / (a + b) ) / Math.log(2);
	    else 
	    	temp2 = 0;
	    
	    if (c != 0 && (c + d) * (a + c) != 0)
	    	temp3 = c / n * Math.log( c * n / (c + d) / (a + c) ) / Math.log(2);
	    else
	    	temp3 = 0;
	    
	    if (d != 0 && (b + d) * (c + d) != 0)
	    	temp4 = d / n * Math.log( d * n / (b + d) / (c + d) ) / Math.log(2);
	    else 
	    	temp4 = 0;
	    
	    mi = temp1 + temp2 + temp3 + temp4;
	    
	    if (isNormalized == true) {
	    	if ((a + c) != 0)
	    		temp1 = ((a + c) / n) * Math.log((a + c) / n) / Math.log(2);
	    	else 
	    		temp1 = 0;
	    	
	    	if ((b + d) != 0)
	    		temp2 = ((b + d) / n) * Math.log((b + d) / n) / Math.log(2);
	    	else 
	    		temp2 = 0;
	    	
	    	if ((a + b) != 0)
	    		temp3 = ((a + b) / n) * Math.log((a + b) / n) / Math.log(2);
	    	else
	    		temp3 = 0;
	    	
	    	if ((c + d) != 0)
	    		temp4 = ((c + d) / n) * Math.log((c + d) / n) / Math.log(2);
	    	else temp4 = 0;
	    	
	    	h1 = temp1 + temp2;
	    	h2 = temp3 + temp4;
	    	
	    	// since a + b + c + d = n, we can make sure h1*h2 != 0
	    	if (h1 * h2 == 0) 
	    		mi = 0;
	    	else
	    		mi /= Math.sqrt(h1 * h2);
	    }

	    return mi; 
	    
	}
	
	private void constructGraphByArray(String type, double[] weight, String outdir) {
		if (this.fArray == null) {
			logger.warning("No feature loaded");
			return;
		}
		for (int i = 0; i < fArray.length; ++i) {
			if (i % 100 == 0)
				System.err.println("has init " + i + " nodes in MI graph");
			for (int j = i + 1; j <  fArray.length; ++j) {
				double[] v1 = fArray[i];
				double[] v2 = fArray[j];
				double distances = 0;
				if (type.equalsIgnoreCase("MutualInformation")) {
					distances = computeMutualInformation(v1, v2);
					weight[i] += distances;
					weight[j] += distances;
					distances = 1 - distances;
				} else if (type.equals("cosine"))
				{
					distances = computeCosine(v1, v2);
					weight[i] += distances;
					weight[j] += distances;
					distances = 1 - distances;
				}
				else {
					logger.warning("No found simiarity type");
				}
//				if (distances == 0) {
//					distances += Double.MIN_VALUE;
//				}						
				miGraph.setEdge(i, j, distances);
				miGraph.setEdge(j, i, distances);
				
			}// end for j
		}// end for i
		if (outdir != null)
		{
			try {
				System.err.println("store graph");
				ObjectWriter.writeObject(miGraph.adjMat, outdir);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	// construct graph based on features and mutual information
	// use a minimum spanning tree to do maximal spanning tree for mi,
	// use 1-mi as the graph weight.
	private void constructGraph(String type, double[] weight) {
		if (this.fMat == null) {
			logger.warning("No feature loaded");
			return;
		}
		for (int i = 0; i < fMat.columns(); ++i) {
			if (i % 100 == 0)
				System.err.println("has init " + i + " nodes in MI graph");
			for (int j = i + 1; j < fMat.columns(); ++j) {
				DoubleMatrix1D v1 = fMat.viewColumn(i); 
				DoubleMatrix1D v2 = fMat.viewColumn(j);
				double distances = 0;
				if (type.equalsIgnoreCase("Euclidean")) {
					distances = computeEuclidean(v1, v2);
				} else if (type.equalsIgnoreCase("Special")) {
					distances = computeSpecialEuclidean(v1, v2);					
				} else if (type.equalsIgnoreCase("Correlation")) {
					distances = computeCorrelation(v1, v2);									
				} else if (type.equalsIgnoreCase("MaxInfoCompress")) {
					distances = computeSquareLossofPCA(v1, v2);					
				} else if (type.equalsIgnoreCase("MutualInformation")) {
					distances = computeMutualInformation(v1, v2);
					weight[i] += distances;
					weight[j] += distances;
					distances = 1 - distances;
				} else {
					logger.warning("No found simiarity type");
				}
//				if (distances == 0) {
//					distances += Double.MIN_VALUE;
//				}						
				miGraph.setEdge(i, j, distances);
				miGraph.setEdge(j, i, distances);
				
			}// end for j
		}// end for i
		
	}
	public int[] rankFeaturesByArray(String type, double[] weight) {
		if (this.fArray == null) {
			logger.warning("No feature loaded");
			return null;
		}
		if (this.fArray.length != weight.length)
		{
			System.err.println("err weight array length");
			return null;
		}
		Arrays.fill(weight, 0);
		constructGraphByArray(type, weight, null);
		for (int i = 0; i < weight.length; i++)
			weight[i] /= weight.length -1;
		
		int[] sortedIndices = new int[fArray.length];
		
		miGraph.mstw();
		List<WMSTTreeNode> mstTree = miGraph.getMSTTree();
		    
		int k = fArray.length - 1;
		while (k > 0) {
			double minDist = Double.MAX_VALUE;
			int parent = -1;
			int child = -1;
			int childIndex = -1;
			for (int i = 0; i < mstTree.size(); ++i) {
				List<Integer> children = mstTree.get(i).getChildren();
				List<Double> distances = mstTree.get(i).getDistances();
				for (int j = 0; j < children.size(); ++j) {
					if (1 - distances.get(j) < minDist) {
						parent = i;
						child = children.get(j);
						minDist = 1 - distances.get(j);
						childIndex = j;
					}
				}				
			}
			
			WMSTTreeNode node = mstTree.get(child);
			node.setParent(-1);
			if (node.getChildren().size() == 0 && node.getParent() == -1) {
				sortedIndices[k] = child;
				k--;
			}

			node = mstTree.get(parent);
			node.deleteChild(childIndex);
			if (node.getChildren().size() == 0 && node.getParent() == -1) {
				sortedIndices[k] = parent;
				k--;
			}
			
		}
		System.out.println();
		return sortedIndices;		
	}
	
	public int[] rankFeatures(String type, double[] weight) {
		if (this.fMat == null) {
			logger.warning("No feature loaded");
			return null;
		}
		if (this.fMat.columns() != weight.length)
		{
			System.err.println("err weight array length");
			return null;
		}
		Arrays.fill(weight, 0);
		constructGraph(type, weight);
		for (int i = 0; i < weight.length; i++)
			weight[i] /= weight.length -1;
		
		int[] sortedIndices = new int[fMat.columns()];
		
		miGraph.mstw();
		List<WMSTTreeNode> mstTree = miGraph.getMSTTree();
		    
		int k = fMat.columns() - 1;
		while (k > 0) {
			double minDist = Double.MAX_VALUE;
			int parent = -1;
			int child = -1;
			int childIndex = -1;
			for (int i = 0; i < mstTree.size(); ++i) {
				List<Integer> children = mstTree.get(i).getChildren();
				List<Double> distances = mstTree.get(i).getDistances();
				for (int j = 0; j < children.size(); ++j) {
					if (1 - distances.get(j) < minDist) {
						parent = i;
						child = children.get(j);
						minDist = 1 - distances.get(j);
						childIndex = j;
					}
				}				
			}
			
			WMSTTreeNode node = mstTree.get(child);
			node.setParent(-1);
			if (node.getChildren().size() == 0 && node.getParent() == -1) {
				sortedIndices[k] = child;
				k--;
			}

			node = mstTree.get(parent);
			node.deleteChild(childIndex);
			if (node.getChildren().size() == 0 && node.getParent() == -1) {
				sortedIndices[k] = parent;
				k--;
			}
			
		}
		
		return sortedIndices;		
	}
	
	// mini test
	public static void main (String[] args) {
		/*				double[][] feature = {
							  {1, 0, 0, 1},
							  {1, 0, 0, 1},
							  {1, 0, 0, 1},
							  {1, 0, 0, 1},
							  {1, 0, 0, 1},
							  {0, 1, 0, 1},
							  {0, 1, 0, 0},
							  {0, 1, 0, 0},
							  {0, 1, 0, 0},
							  {0, 1, 0, 0},
							  {0, 0, 1, 0},
							  {0, 0, 1, 0}};	*/
		double[][] feature = {
				  {1, 1, 1, 1, 1, 1},
				  {1, 1, 0, 1, 1, 1},
				  {1, 1, 0, 1, 1, 1},
				  {1, 1, 0, 1, 1, 1},
				  {0, 0, 1, 0, 1, 1},
				  {0, 0, 1, 0, 1, 1},
				  {0, 0, 1, 0, 1, 1},
				  {0, 0, 1, 0, 1, 1}};	
/*		double[][] feature = {
				  {11, 0, 1},
				  {12, 0, 2},
				  {13, 0, 3},
				  {14, 0, 4},
				  {0, 15, 5},
				  {0, 16, 6},
				  {0, 17, 0},
				  {0, 18, 8}};	*/
		
//		if (type.equalsIgnoreCase("Euclidean")) {
//			distances = computeEuclidean(v1, v2);
//		} else if (type.equalsIgnoreCase("Special")) {
//			distances = computeSpecialEuclidean(v1, v2);					
//		} else if (type.equalsIgnoreCase("Correlation")) {
//			distances = computeCorrelation(v1, v2);									
//		} else if (type.equalsIgnoreCase("MaxInfoCompress")) {
//			distances = computeSquareLossofPCA(v1, v2);					
//		} else if (type.equalsIgnoreCase("MutualInformation")) {
			
		MutualInformationMST ls = new MutualInformationMST(feature, true);
		double[] weights = new double[feature.length];
		int[] indexes = ls.rankFeaturesByArray("cosine", weights);
		System.out.println();
		for ( int index : indexes)  
			System.out.print(index + " ");
		System.out.println();
		for (double weight:weights) 
			System.out.print(weight + " ");
		System.out.println();
		

	}
	
}
