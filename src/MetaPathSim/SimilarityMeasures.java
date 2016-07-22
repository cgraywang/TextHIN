package MetaPathSim;

import java.util.*;
import java.lang.Math;
import models.util.matrix.*;
import models.datastructure.ColtDenseVector;
import models.datastructure.ColtSparseVector;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * @author Haoran Li
 */

public class SimilarityMeasures {

	public SimilarityMeasures(){}
	
	
	public static double computeCosineSimilarity(IntArrayList indexA, DoubleArrayList valueA, DoubleMatrix1D B, DoubleArrayList valueB)
	{
		double sim = -1;
		
		double num = 0;
		double den = 0;
		double den_1 = 0;
		double den_2 = 0;

		num = Matrix2DUtil.productQuick(indexA, valueA, B);
		den_1 = Matrix2DUtil.getSqrSum(valueA);
		den_2 = Matrix2DUtil.getSqrSum(valueB);
		den = Math.sqrt(den_1) * Math.sqrt(den_2);
		if(den == 0)
			return 0;
		sim = num/den;
		return sim;
	}
	
	
	public static double computeCosineSimilarity(DoubleMatrix1D a, DoubleMatrix1D b)
	{
		double sim = -1;
		if (a.size() != b.size())
		{
			System.err.println("mismatched size");
			return -1;
		}
		
		double num = 0;
		double den = 0;
		double den_1 = 0;
		double den_2 = 0;

		num = Matrix2DUtil.productQuick(a, b);
		den_1 = Matrix2DUtil.getSqrSum(a);
		den_2 = Matrix2DUtil.getSqrSum(b);
		den = Math.sqrt(den_1) * Math.sqrt(den_2);
		if(den == 0)
			return 0;
		sim = num/den;
		return sim;
	}
	
	public static double computeCosineSimilarity(double[] a, double[] b)
	{
		double sim = -1;
		if(a.length != b.length)
			return sim;
		double num = 0;
		double den = 0;
		double den_1 = 0;
		double den_2 = 0;
		for(int i = 0; i < a.length; i++)
		{
			num += a[i] * b[i];
			den_1 += a[i]*a[i];
			den_2 += b[i]*b[i];
		}
		den = Math.sqrt(den_1) * Math.sqrt(den_2);
		if(den == 0)
			return 0;
		sim = num/den;
		return sim;
	}
	
	public static double computeJaccardSimilarity(IntArrayList indexA, DoubleArrayList valueA, DoubleMatrix1D B, DoubleArrayList valueB)
	{
		double sim = -1;
		double num = 0;
		double den = 0;
		num = Matrix2DUtil.productQuick(indexA, valueA, B);
		den = Matrix2DUtil.getSqrSum(valueA) + Matrix2DUtil.getSqrSum(valueB);
		
	
		if((den-num) == 0)
			return 0;
		sim = num/(den - num);
		return sim;
	}
	
	public static double computeJaccardSimilarity(DoubleMatrix1D a, DoubleMatrix1D b)
	{
		double sim = -1;
		if(a.size() != b.size())
			return sim;
		double num = 0;
		double den = 0;
		num = Matrix2DUtil.productQuick(a, b);
		den = Matrix2DUtil.getSqrSum(a) + Matrix2DUtil.getSqrSum(b);
		
	
		if((den-num) == 0)
			return 0;
		sim = num/(den - num);
		return sim;
	}
	
	public static double computeJaccardSimilarity(double[] a, double[] b)
	{
		double sim = -1;
		if(a.length != b.length)
			return sim;
		double num = 0;
		double den = 0;
		for(int i = 0; i < a.length; i++)
		{
			num += a[i] * b[i];
			den += a[i]*a[i] + b[i]*b[i];
		}
		if((den-num) == 0)
			return 0;
		sim = num/(den - num);
		return sim;
	}
	
	public static double computeDiceSimilarity(IntArrayList indexA, DoubleArrayList valueA, DoubleMatrix1D B, DoubleArrayList valueB)
	{
		double sim = -100000;
		double num = 0;
		double den = 0;
		
		num = Matrix2DUtil.productQuick(indexA, valueA, B);
		den = Matrix2DUtil.getSqrSum(valueA) + Matrix2DUtil.getSqrSum(valueB);
		if(den == 0)
			return 0;
		sim = 2*num/den;
		return sim;
	}
	public static double computeDiceSimilarity(DoubleMatrix1D a, DoubleMatrix1D b)
	{
		double sim = -1;
		if(a.size() != b.size())
			return sim;
		double num = 0;
		double den = 0;
		
		num = Matrix2DUtil.productQuick(a, b);
		den = Matrix2DUtil.getSqrSum(a) + Matrix2DUtil.getSqrSum(b);
		if(den == 0)
			return 0;
		sim = 2*num/den;
		return sim;
	}
	
	public static double computeDiceSimilarity(double[] a, double[] b)
	{
		double sim = -1;
		if(a.length != b.length)
			return sim;
		double num = 0;
		double den = 0;
		for(int i = 0; i < a.length; i++)
		{
			num += a[i] * b[i];
			den += a[i]*a[i] + b[i]*b[i];
		}
		if(den == 0)
			return 0;
		sim = 2*num/den;
		return sim;
	}
	
	
	/*
	//lihaoran
	public static double[] computeKnowSim(Integer did, Map<Integer, List<DoubleMatrix1D>> interMats)
	{
		List<DoubleMatrix1D> datastart = new ArrayList<DoubleMatrix1D>();
		DoubleMatrix1D sample = new ColtDenseVector(interMats.get(0).get(did).size());
		int n = interMats.get(0).size();
		sample.assign(interMats.get(0).get(did));
		datastart.add(sample);
		for (int i = 1; i < interMats.size(); i ++)
		{
			datastart = Matrix2DUtil.DenseMultDense(datastart, interMats.get(i));
		}
		
		if (datastart.size() == 0)
			return new double[n];
		double[] sim = new double[n];
		DoubleMatrix1D path_num = datastart.get(0);
		double mii = path_num.get(did);
		for (int  i = 0; i < n; i++)
		{
			mjj = 
		}
			
	}
	*/
	
	public static double computeKnowSim(Integer did_0, Integer did_1, Map<Integer, double[][]> interMats)
	{
		//notice that the element in a and b here are the number of path
		double sim = -1;
		boolean isDense = false;
		Map<Integer, List<DoubleMatrix1D>> datas = new HashMap<Integer, List<DoubleMatrix1D>>();
		List<DoubleMatrix1D> datastart = new ArrayList<DoubleMatrix1D>();
		List<DoubleMatrix1D> dataend = new ArrayList<DoubleMatrix1D>();
		for(int j = 0; j < interMats.keySet().size(); j++)
		{	
			double[][] dataMat = interMats.get(j);
			if(j == 0)
			{
				DoubleMatrix1D sample = new ColtDenseVector(dataMat[did_0].length);
				sample.assign(dataMat[did_0]);
				datastart.add(sample);
				sample = new ColtDenseVector(dataMat[did_1].length);
				sample.assign(dataMat[did_1]);
				dataend.add(sample);
				continue;
			}
			List<DoubleMatrix1D> data = new ArrayList<DoubleMatrix1D>();
			if (isDense == true) {
				for (int i = 0; i < dataMat.length; ++i) {
					DoubleMatrix1D sample = new ColtDenseVector(dataMat[i].length);
					sample.assign(dataMat[i]);
					data.add(sample);
				}
			} else {
				for (int i = 0; i < dataMat.length; ++i) {
					DoubleMatrix1D sample = new ColtSparseVector(dataMat[i].length);
					sample.assign(dataMat[i]);
					sample.trimToSize();
					data.add(sample);
				}
			}
			datas.put(j - 1, data);
		}
		//List<DoubleMatrix1D> tempMat_0 = null;
		//List<DoubleMatrix1D> tempMat_1 = null;
		List<DoubleMatrix1D> tmpST = new ArrayList<DoubleMatrix1D>(), tmpED = new ArrayList<DoubleMatrix1D>();
		for(int j = 0; j < datas.keySet().size(); j++) // modified by Li Haoran
		{
			if(isDense == true)
			{
	
				if (j == datas.keySet().size() - 1)
					for (DoubleMatrix1D vec: datastart)
						tmpST.add(vec.copy());
				datastart = Matrix2DUtil.DenseMultDense(datastart, datas.get(j));
				if (j == datas.keySet().size() - 1)	//  the passed path is A-V-P-P, so we need A-V-P-P and A-V-P
					for (DoubleMatrix1D vec: dataend)
						tmpED.add(vec.copy());
				dataend = Matrix2DUtil.DenseMultDense(dataend, datas.get(j));
			}
			else
			{
				if (j == datas.keySet().size() - 1)
					for (DoubleMatrix1D vec: datastart)
						tmpST.add(vec.copy());
				datastart = Matrix2DUtil.DenseMultDense(datastart, datas.get(j));
				if (j == datas.keySet().size() - 1)	//  the passed path is A-V-P-P, so we need A-V-P-P and A-V-P
					for (DoubleMatrix1D vec: dataend)
						tmpED.add(vec.copy());
				dataend = Matrix2DUtil.DenseMultDense(dataend, datas.get(j));
			}
		}
		double num = 0;
		double den = 0;
		for(int i = 0; i < datastart.size(); i++)
		{
			if (tmpED.size() == 0)
				return 0;
			num += Matrix2DUtil.product(datastart.get(i), tmpED.get(i));
			den += Matrix2DUtil.product(datastart.get(i), tmpST.get(i)) + Matrix2DUtil.product(dataend.get(i), tmpED.get(i)) ;
		}
		if(den == 0)
			return 0;
		sim = 2* num / den;
		return sim;
	}
	
	public static List<DoubleMatrix1D> array2mat(double[][] mat)
	{
		List<DoubleMatrix1D> matrix = new ArrayList<DoubleMatrix1D>();
		for (int i = 0; i < mat.length; i++)
		{
			DoubleMatrix1D tmp = new ColtDenseVector(mat[i]);
			matrix.add(tmp);
		}
		return matrix;
	}

	
	public static double[][] computeMetaPath(List<double[][]> interMats)
	{
		int length = interMats.size();
		List<List<DoubleMatrix1D>> mats = new ArrayList<List<DoubleMatrix1D>>(); 
		for (int i = 0; i < length; i++)
		{
			mats.add(array2mat(interMats.get(i)));
		}
		List<DoubleMatrix1D> M, ans;
		M = mats.get(0);
		for (int i = 1; i < length; i++)
			M = Matrix2DUtil.DenseMultDense(M, mats.get(i));
		ans = Matrix2DUtil.DenseMultDenseTranspose(M, M);
		double[][] ret = new double[ans.size()][ans.get(0).size()];
		for (int i = 0; i < ans.size(); i++)
		{
			for (int j = 0; j < ans.get(i).size(); j++)
				ret[i][j] = ans.get(i).getQuick(j);
		}
		return ret;
		
	}
	
	//evaluation
	public static double computeSpearmanCorrelation(double[] rank_0, double[] rank_1)
	{
		double rho = -1;
		double num = 0;
		double den = 0;
		for(int i = 0; i < rank_0.length; i++)
			num += Math.pow((rank_0[i] - rank_1[i]),2);
		den = rank_0.length * (Math.pow(rank_0.length, 2) - 1);
		if(den == 0)
			return rho;
		rho = 1 - 6*num/den;
		return rho;
	}
	public static void main (String[] args) 
	{
		double a[] = {1,0,1,0,1,0};
		double b[] = {1,0,1,1,0,1};
		//in our case, a and b are vectors representing each document for cosine, jaccard, dice similarity
		double sim = 0;
		sim = computeCosineSimilarity(a, b);
		System.out.println("cosine similarity: " + sim);
		sim = computeJaccardSimilarity(a, b);
		System.out.println("jaccard similarity: " + sim);
		sim = computeDiceSimilarity(a,b);
		System.out.println("dice similarity: " + sim);
		
		//for our knowsim, given meta-path d-p-l-p-d (doc-person-location-person-doc)
		double d_p[][] = {
				{1,0,1,0,1,0},
				{1,0,1,1,0,1},
				{1,2,1,2,3,2},
		};
		double p_l[][] = {
				{1,1,2,2},
				{2,3,2,3},
				{0,1,1,0},
				{1,1,0,0},
				{3,2,1,0},
				{0,2,1,3},
		};
		List<double[][]> interMats = new ArrayList<double[][]>();
		interMats.add(d_p);
		interMats.add(p_l);
		double[][] ret = computeMetaPath(interMats);
		for (int i = 0; i < ret.length; i++)
		{
			for (int j = 0; j < ret[i].length; j++)
				System.out.print(ret[i][j] + " ");
			System.out.println();
		}
	}
}
