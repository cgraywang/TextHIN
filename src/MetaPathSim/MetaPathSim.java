package MetaPathSim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import fig.basic.IOUtils;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import models.clustering.MetaPath;
import models.clustering.ObjectWriter;

/**
 * @author Haoran Li
 */

class WeightedPath{
	MetaPath metaPath;
	double weight;
	
	public WeightedPath(MetaPath metaPath, double weight) {
		this.metaPath = metaPath;
		this.weight = weight;
	}
}


public class MetaPathSim {
	static List<MetaPath> metaPaths;
	
	static int[] topRank;
	static double[] weight;
	static double[][] path_cnt, sim, broadness;
	static String MatDir;
	public static String[] docs;
	public static int[] labels;
	static int doc_num;
	
	static Set<Integer> valid_docs_idx;
	static double[] word_weight;
	static double[] metapath_weight;
	static int maxPath = 40;
	static String OutDir;
	
	
	@SuppressWarnings("unchecked")
	public static void init(String _OutDir, String LabelFile, String _MatDir)
	{
		System.out.println("init");
		MatDir = _MatDir;
		OutDir = _OutDir;
		try {
			Map<String, Map<String, Integer>> et_e_index = (Map<String, Map<String,Integer>>) ObjectWriter.readObject(OutDir + "/et_e_idx.map");			
			Map<String, Integer> doc_idx = et_e_index.get("doc");
			doc_num = doc_idx.size();
			System.out.println("\tdoc size:" + doc_num);
			docs = new String[doc_num];
			labels = new int[doc_num];
			
			Map<String, Integer> doc_label_map = new HashMap<>();
			for (String line: IOUtils.readLinesHard(LabelFile)) {
				String[] parts = line.trim().split("\t");
				doc_label_map.put(parts[0], Integer.parseInt(parts[1]));
			}
			for (Entry<String, Integer> entry: doc_idx.entrySet())
			{
				int index = entry.getValue();
				String doc = entry.getKey();
				docs[index] = doc;
				labels[index] = doc_label_map.get(doc);
			}
			sim = new double[doc_num][doc_num];
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("init end");

	}
	
	static String pathToString(WeightedPath weightedPath) {
		double weight = weightedPath.weight;
		MetaPath path = weightedPath.metaPath;
		return path.toString() + "\t" + weight;
	}
	
	
	static WeightedPath pathFromString(String line, double defaultWeight) {
	
		String[] tokens = line.trim().split("\t");
		String[] types = tokens[0].split(" -> ");
		MetaPath path = new MetaPath();
		for (String type:types)
			path.add(type);
		double weight;
		if (tokens.length > 1)
			weight = Double.parseDouble(tokens[1]);
		else
			weight = defaultWeight;
		return new WeightedPath(path, weight);
	}
	
	static List<WeightedPath> pathsFromFile(String file, double weight) {
		List<WeightedPath> paths = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String ln;
			while ((ln = reader.readLine()) != null) {
				ln = ln.trim();
				if (ln.equals("") || ln.startsWith("#"))
					continue;
				paths.add(pathFromString(ln, weight));
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return paths;
	}
	
	static List<WeightedPath> select_metapath(int mode, String metaPathFile)
	{
		List<WeightedPath> weighted_paths;
		
		if (mode == 2) {
			double cus_weight = 0.15;
			weighted_paths = pathsFromFile(metaPathFile, cus_weight);
			return weighted_paths;
		} else {
			String method = "cosine";
			if (mode == 1)
				method = "lap";
			calcMI weightCalc = new calcMI(MatDir, method, OutDir + "/metaPaths.list");
			weightCalc.init();
			weighted_paths = weightCalc.calc(maxPath);
		}
		return weighted_paths;
	}
		
	
	@SuppressWarnings("unchecked")
	public static List<DoubleMatrix1D> load_metapath(MetaPath metaPath)
	{

		String suffix = "/" + (metaPath.path.size()-1) + "/" + metaPath.toString() + ".mat";
		List<DoubleMatrix1D> mat = null;
		mat = (List<DoubleMatrix1D>) ObjectWriter.readObject(MatDir+suffix);

		return mat;
	}
	
	public static Map<Integer, Double> getSortedFeature(IntArrayList indexList, DoubleArrayList valueList) {
		Map<Integer, Double> feature_map = new TreeMap<>();
		for (int i = 0; i < indexList.size(); i++)
			feature_map.put(indexList.get(i), valueList.get(i));
		return feature_map;
	}
	
	
	static void add_path(WeightedPath path)
	{
		List<DoubleMatrix1D> mat = load_metapath(path.metaPath);
		if (mat == null || mat.size() == 0 )
		{
			System.err.println("err metapath:" + path.metaPath);
			return;
		}
		System.out.println("load metapath:" + path.metaPath + " with weight:" + path.weight);

		
		if (path.metaPath.path.get(1).equals("word"))
		{
			double[][] arr = new double[doc_num][doc_num];
			for (int i = 0; i < doc_num; i++)
			{
				DoubleMatrix1D vec = mat.get(i);
				for (int j = 0; j < doc_num; j++)
					arr[i][j] = vec.getQuick(j);
			}
			for (int i = 0; i < doc_num; i++) {
				for (int j = 0; j < doc_num; j++)
				{		
					double value = arr[i][j];
					double path_i_i = arr[i][i];
					double path_j_j = arr[j][j];
					if (path_i_i * path_j_j > 0.00001) {
						double factor = (value / Math.sqrt(path_i_i* path_j_j)) * path.weight;
						sim[i][j] += factor; 			
					}
				}
			}
		} else
		{
			IntArrayList indexList = new IntArrayList();
			DoubleArrayList valueList = new DoubleArrayList();
			for (int i = 0; i < doc_num; i++)
			{
				int j = -1;
				double value;
				mat.get(i).getNonZeros(indexList, valueList);
				Map<Integer, Double> feature_map = getSortedFeature(indexList, valueList);
				for (Map.Entry<Integer, Double> entry: feature_map.entrySet()) {
					j = entry.getKey();
					value = entry.getValue();
					
					double path_i_i = mat.get(i).getQuick(i);
					double path_j_j = mat.get(j).getQuick(j);
					if (path_i_i * path_j_j > 0.00001) {
						double factor = (value / Math.sqrt(path_i_i* path_j_j)) * path.weight ;
						sim[i][j] += factor; 
					}

				}
			}
		}
		
	}
	
	static double calcCorrelation()
	{
		System.err.println("calc Correlations");
		double[] correlations = new double[doc_num];
		double tot_corres = 0;
		for (int i = 0; i < doc_num; i++)
		{
			correlations[i] = VectorSimCalc.calcCorrelation(sim[i], labels, i);
			tot_corres += correlations[i];
		}
		return tot_corres/doc_num;
	}	

	public static void printSimilarity(String outfile) {
		System.out.println("print similarity to file:" + outfile);
		try {
			int doc_idx = -1;
			BufferedWriter sim_writer = new BufferedWriter(new FileWriter(outfile));
			for (double[] sim_vec: sim) {
				doc_idx ++;
				StringBuffer ret = new StringBuffer();
				ret.append(docs[doc_idx]);
				ret.append("\t");
				for (int i = 0; i < doc_num; i++) {
					if (i > 0)
						ret.append(" ");
					ret.append(String.format("%.5f", sim_vec[i]));
				}
				ret.append("\n");
				sim_writer.write(ret.toString());
			}
			sim_writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	// mode = 0:cosine MI; mode = 1:laplacian; mode = 2: custom
	// args should taks the form:MetapathMode customMetaPathFile MatDir LabelFile SPOutDir simOutputDir 
	public static void main(String[] args) {
		if (args.length != 6) {
			System.out.println("args should taks the form:MetapathMode MetaPathFile MatDir LabelFile SPOutDir simOutputDir ");
			System.out.println("mode = 0:cosine MI; mode = 1:laplacian; mode = 2: custom");
			System.exit(0);
		}

		init(args[4], args[3], args[2]);
		List<WeightedPath> selected_paths = select_metapath(Integer.parseInt(args[0]), args[1]);
		for (WeightedPath path: selected_paths)
		{
			add_path(path);
		}
		double correlation = calcCorrelation();
		System.out.println("correlation = " + correlation);
		printSimilarity(args[5]);
		
	}
}
