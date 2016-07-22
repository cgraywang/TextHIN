package MetaPathSim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import models.clustering.MetaPath;
import models.clustering.ObjectWriter;
import models.datastructure.ColtSparseVector;
import models.featureselection.LaplacianScore;
import models.featureselection.MutualInformationMST;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;


/**
 * @author Haoran Li
 */

public class calcMI {

	static List<MetaPath> metaPaths;
	static DoubleMatrix2D doc_path;
	static List<DoubleMatrix1D> mat;
	static int metapath_num;
	static BufferedWriter writer, errwriter;
	static String prefix = "./data/20NewsGroup_minmax/20NewsGroup";
//	static String prefix = "./data/GCAT_sample/GCAT";
//	static String prefix = "./data/sample/sample";
	static double[][] fArray;
	String method;
	String MatDir;
	int doc_num;
	
	@SuppressWarnings("unchecked")
	calcMI(String MatDir, String method, String metaPathFile) {
		this.MatDir = MatDir;
		this.method = method;
		metaPaths = new ArrayList<>();
		for (MetaPath metaPath:( List<MetaPath>)ObjectWriter.readObject(metaPathFile)) {
			if (metaPath.isSymmetrical() && metaPath.isProper())
				metaPaths.add(metaPath);
		}
		metapath_num = metaPaths.size();
				
	}
	
	static double[][] matrix2dDoubleArray(DoubleMatrix2D mat)
	{
		double[][] array = new double[mat.columns()][mat.rows()];
		for (int i = 0; i < mat.rows(); i++ )
		{;
			for (int j = 0; j < mat.columns(); j ++)
				array[j][i] = mat.get(i, j);
		}
		return array;
	}
	
	
	
	@SuppressWarnings("unchecked")
	double[] load_metapath(MetaPath metaPath)
	{
		mat = null;
		try {
			String suffix = "/" + (metaPath.path.size()-1) + "/" + metaPath.toString() + ".mat";
			mat = (List<DoubleMatrix1D>) ObjectWriter.readObject(MatDir + suffix);
			if (mat == null)
			{
				System.err.println("err metapath:" + metaPath);
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		IntArrayList indexArrayList = new IntArrayList();
		DoubleArrayList valueArrayList = new DoubleArrayList();
		if (doc_num == 0 && mat.size() > 0)
			doc_num = mat.size();
		double[] hasInstance = new double[doc_num];
		if (mat.size() > 0)
		{
			for (int i = 0; i < doc_num ; i++)
			{
				mat.get(i).getNonZeros(indexArrayList, valueArrayList);
				for (int j = 0; j < valueArrayList.size(); j++)
					if (valueArrayList.get(j) > 0.0001 &&  i != j)
					{
						hasInstance[i] = 1;
						break;
					}
			}
			return hasInstance;	
		} else {
			System.err.println("err metapath:" + metaPath);
			return null;
		}
	}
	
	
	public void init()
	{
		System.out.println("load matrix");

		load_metapath(metaPaths.get(0));
		if (doc_num == 0) {
			System.exit(0);
		}
		doc_path = new SparseDoubleMatrix2D(doc_num, metaPaths.size());

		for (int i = 0; i < metaPaths.size(); i++)
		{
			if (i % 20 == 0)
				System.err.println("has loaded " + i + " matrix");
			double[] hasInstance = load_metapath(metaPaths.get(i));
			for (int j = 0; j < doc_num; j ++)
				if (hasInstance[j] > 0.01)
					doc_path.setQuick(j, i, hasInstance[j]);
		}
	}
	
	public List<WeightedPath> calc(int maxPath) {
		if (method.equals("cosine"))
			return calc_MI(maxPath);
		else
			return calc_lap(maxPath);
	}

	
	public List<WeightedPath> calc_MI(int maxPath)
	{
		System.out.println("calc MI with cosine");
		fArray = matrix2dDoubleArray(doc_path);
		doc_path = null;
		
		MutualInformationMST mi = new MutualInformationMST(fArray, true);
		double[] weight = new double[metapath_num];
		int[] indices = mi.rankFeaturesByArray("cosine", weight);

		List<WeightedPath> ret = new ArrayList<>();
		for (int i = 0; i < Math.min(maxPath, indices.length); i ++) {
			int index = indices[i];
			if (metaPaths.get(index).path.get(1).equals("word"))
				continue;
			ret.add(new WeightedPath(metaPaths.get(index), weight[index]));
		}
		ret.add(new WeightedPath(new MetaPath("word"), 1));
		return ret;
	}
	
	public List<WeightedPath> calc_lap(int maxPath)
	{
		System.out.println("calc lap");
		double[] weight = new double[metapath_num];
		LaplacianScore ls = new LaplacianScore(doc_path);
		int neighborNum = 50;
		int[] indices = null;

		indices = ls.laplacianScore(neighborNum, "heat", weight);
		List<WeightedPath> ret = new ArrayList<>();
		for (int i = 0; i < Math.min(maxPath, indices.length); i ++) {
			int index = indices[i];
			if (metaPaths.get(index).path.get(1).equals("word"))
				continue;
			ret.add(new WeightedPath(metaPaths.get(index), weight[index]));
		}
		ret.add(new WeightedPath(new MetaPath("word"), 1));
		return ret;
	}
	
}
