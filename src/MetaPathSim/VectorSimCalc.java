package MetaPathSim;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import models.datastructure.ColtSparseVector;


import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;

/**
 * feature based similarity computation
 * @author Haoran Li
 */

class Feature
{
	String label;
	public DoubleMatrix1D feature;
	public IntArrayList indexList;
	public DoubleArrayList valueList;

//	double[] feature;
	Feature(int size, String label)
	{
		feature = new ColtSparseVector(size);
		this.label = label;
	}
	
	public void set(double[] value, int st, int ed)
	{
		for (int i = st; i < ed; i++)
		{
			if (value[i-st] != 0)
				feature.setQuick(i, value[i-st]);
		}
	}
	
	public void getNonZeroes()
	{
		indexList = new IntArrayList();
		valueList= new DoubleArrayList();
		feature.getNonZeros(indexList, valueList);
		
	}
	
	static double calcFeatureSimByCache(Feature u, Feature v, String method)
	{
		if (method.equals("cosine"))
			return SimilarityMeasures.computeCosineSimilarity(u.indexList,u.valueList, v.feature, v.valueList);
		else if (method.equals("jaccard"))
			return SimilarityMeasures.computeJaccardSimilarity(u.indexList,u.valueList, v.feature, v.valueList);
		else if (method.equals("dice"))
			return SimilarityMeasures.computeDiceSimilarity(u.indexList,u.valueList, v.feature, v.valueList);
		else 
		{
			System.out.println("err!!!!!!!##~^@&*~");
			return -10000;
		}

	}
	
	static double calcFeatureSim(Feature u, Feature v, String method)
	{
		if (method.equals("cosine"))
			return SimilarityMeasures.computeCosineSimilarity(u.feature, v.feature);
		else if (method.equals("jaccard"))
			return SimilarityMeasures.computeJaccardSimilarity(u.feature, v.feature);
		else if (method.equals("dice"))
			return SimilarityMeasures.computeDiceSimilarity(u.feature, v.feature);
		else 
		{
			System.out.println("err!!!!!!!##~^@&*~");
			return -10000;
		}

		
	}
	
}

public class VectorSimCalc {
	static boolean bag_of_words = true;
	static boolean entity_type = false;
	static boolean topics_model = false;
	static Map<String, Map<String, Integer>> doc_word_cnt, doc_e_cnt;
	static Feature[] features = null;
	static Map<String, Integer> doc2idx, word2idx , e2idx;
	static Map<Integer, String> idx2doc;
	static Map<String, String> e_topet;
	static Map<String, double[]> doc_topics;
	static String eva_method;
	static int doc_num;
	static int topic_num;
	static int type_limit = 10000000;
	
	
	static void init_index()
	{
		doc2idx = new HashMap<String, Integer>();
		idx2doc = new HashMap<Integer, String>();
		int cnt = 0;
		for (String doc: doc_word_cnt.keySet())
		{
			doc2idx.put(doc, cnt);
			idx2doc.put(cnt, doc);
			cnt ++;
		}
		
		word2idx = new HashMap<String, Integer>();
		int word_cnt = 0;
		for (Map<String, Integer> words: doc_word_cnt.values())
		{
			for (String word: words.keySet())
			{
				if (!word2idx.containsKey(word))
				{
					word2idx.put(word, word_cnt);
					word_cnt ++;
				}
			}
		}

		Map<String, Integer> type_cnt = new HashMap<String, Integer>();
		int visited_cnt = 0;
		e2idx = new HashMap<String, Integer>();
		for (Map<String, Integer> e_cnt: doc_e_cnt.values())
		{
			for (String e: e_cnt.keySet())
			{
				if (e2idx.containsKey(e))
					continue;
				if (e_topet.containsKey(e))
				{
					String type = e_topet.get(e);
					if (!type_cnt.containsKey(type))
						type_cnt.put(type, 0);
					if (type_cnt.get(type) < type_limit)
					{
						int tmp = type_cnt.get(type) + 1;
						type_cnt.put(type, tmp);
						e2idx.put(e, visited_cnt++);
					}
				}
			}
			
		}
		
	}

	static void init(Map<String, Map<String, Integer>> doc_words, Map<String, Map<String, Integer>> _doc_e_cnt, Map<String, double[]> _doc_topics, Map<String, String> _e_topet)
	{
		doc_word_cnt = doc_words;
		doc_e_cnt = _doc_e_cnt;
		
		
		// debug
		doc_topics = _doc_topics;
//		doc_topics = new HashMap<String, double[]>();
//		for (String doc: _doc_topics.keySet())
//		{
//			double[] dis = new double[4];
//			for (int i = 0; i < 4; i++)
//				dis[i] = _doc_topics.get(doc)[i];
//			doc_topics.put(doc, dis);
//		}
		e_topet = _e_topet;
		bag_of_words = true;
//		entity_type = true;
		init_index();
		doc_num = doc2idx.size();
		
		for (double[] topic_dis: doc_topics.values())
		{
			topic_num = topic_dis.length;
			break;
		}
		features = new Feature[doc_num];
		
	}
	

	
	static double[][] sim;
	//parameter method should be one of ["cosine", "jaccard", "dice"]
	static double[][] calcSim(String method)
	{
		addFeature();
		int size = features.length;
		
		System.out.println("doc size:" + size + "\tfeature size:" + features[0].feature.size() );
		sim = new double[size][size];
		for (int i = 0; i < features.length; i++)
			features[i].getNonZeroes();
		for (int i = 0; i < features.length; i++)
		{
			if (i % 100 == 0)
				System.out.println("have calc " + i + " documents' similarity");
			for (int j = 0; j < features.length; j++)
			{
				sim[i][j] = Feature.calcFeatureSimByCache(features[i], features[j], method);
//				sim[i][j] = Feature.calcFeatureSim(features[i], features[j], method);
			}
		}
		return sim;
	}
	
	static void addFeature()
	{
		int tot_size = 0;
		if (bag_of_words)
			tot_size += word2idx.size();
		if (entity_type)
			tot_size += e2idx.size();
		if (topics_model)
			tot_size += topic_num;
		int now_size = 0;
		
		for (int i = 0; i < features.length; i++)
			features[i] = new Feature(tot_size, idx2doc.get(i));
		if (bag_of_words)
		{
			add_bag_of_words(now_size);
			now_size += word2idx.size();
		}
		if (entity_type)
		{
			add_entity_type(now_size);
			now_size += e2idx.size();
		}
		if (topics_model)
		{
			add_topic_dis(now_size);
			now_size += topic_num;
		}
	}
	

	
	static void normalize(double[] tmp)
	{
		double sum =0 ;
		for (int i = 0; i < tmp.length; i++)
			sum += tmp[i] * tmp[i];
		if (sum < 0.000001)
			return;
		double norm = Math.sqrt(sum);
		for (int i = 0; i < tmp.length; i++)
			tmp[i] /= norm;
	}
	
	static void add_bag_of_words(int l)
	{
		double[] tmp;		int size = word2idx.size(), r = l + size;
		for (int i = 0; i < features.length; i++)
		{
			String doc = idx2doc.get(i);
			tmp = new double[size];
			Map<String, Integer> word_cnt = doc_word_cnt.get(doc);
			for (Entry<String, Integer> entry: word_cnt.entrySet())
			{
				String word = entry.getKey();
				int num = entry.getValue();
				tmp[word2idx.get(word)] = num;

			}
			normalize(tmp);
			features[i].set(tmp, l, r);
		}
	}
	
	static void add_entity_type(int l)
	{
		double[] tmp;
		int size = e2idx.size(), r = l + size;
		for (int i = 0; i < features.length; i++)
		{
			String doc = idx2doc.get(i);
			tmp = new double[size];
			Map<String, Integer> e_cnt = doc_e_cnt.get(doc);
			for (String e: e_cnt.keySet())
			{
				if (e2idx.containsKey(e))
				{
					int idx = e2idx.get(e);
					double value = e_cnt.get(e);
					tmp[idx] += value;
				}
			}
			normalize(tmp);
			features[i].set(tmp, l, r);
			
		
		}
	}
	
	static void add_topic_dis(int l)
	{
		double[] tmp;
		int size = topic_num, r = l + size;
		for (int i = 0; i < features.length; i++)
		{
			String doc = idx2doc.get(i);
			if (!doc.equals(features[i].label))
				System.out.println("err");
			tmp = doc_topics.get(doc);
			// no normalize
			features[i].set(tmp, l, r);
		}
	}
	
	static double[] normalizeToMAX1(double[] origin, int srcIndex)
	{
		double[] new_array = new double[origin.length];
		double min = 1e9,max = -1;
		for (int i = 0; i < origin.length; i++)
		{
			if (i == srcIndex)
				continue;
			min = Math.min(min, origin[i]);
			max = Math.max(max, origin[i]);
		}
		double delta = max - min;
		if (delta < 0.00001)
			delta = 1;
		for (int i = 0; i < origin.length; i++)
			if (i == srcIndex)
				new_array[i] = 1;
			else
				new_array[i] = (origin[i] - min) /  delta;
		return new_array;
	}
	
	
	public static double mean(double[] sim, int srcIdx)
	{
		double sum = 0.0, num = sim.length-1;
		for (int i = 0; i < sim.length; i++)
			if (i != srcIdx)
				sum += sim[i];
		return sum/num;
	}
	
	
	static double[] tmp = null;
	public static double maxk(double[] sim, int srcIndex, int k)
	{
		if (tmp == null)
			tmp = new double[sim.length];
		for (int i = 0; i < sim.length; i++)
			tmp[i] = sim[i];
		Arrays.sort(tmp);
		return (tmp[tmp.length -k]);
	}
	
	public static double calcCorrelation(double[] sim, int[] labels, int srcIndex)
	{
		if(sim.length != labels.length)
		{
			System.err.println("Mismatch length for calcCorrelation");
			return -1;
		}
		int doc = labels[srcIndex];
			
		double sumxy = 0,sumxx = 0, sumyy = 0, deltax, deltay, meanx, meany, relation;
		meany = 0.0;
		
		for (int i =0; i < sim.length; i++)
			if (i != srcIndex)
				if (labels[i] == doc)
					meany ++;
		
		meany /= sim.length - 1;
		meanx = mean(sim, srcIndex);

		for (int i = 0; i < sim.length; i++)
		{
			if (i == srcIndex)
				continue;
			if (labels[i] == doc)
				relation = 1.0;
			else
				relation = 0.0;
			
			deltax = (sim[i] - meanx);
			deltay = (relation - meany);
			sumxy += deltax * deltay;
			sumxx += deltax * deltax;
			sumyy += deltay * deltay;
		}

		if (sumxx * sumyy < 0.0000001)
			return 0;
		return sumxy / Math.sqrt(sumxx * sumyy);
	}
	
	
	
	public static void main(String[] args) {
		// just for test
		int test_num = 10;
		double[] sim = {0,0,2,3,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int[] labels = {0,0,1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		System.out.println(sim.length);
		System.out.println(labels.length);
		System.out.println(calcCorrelation(sim, labels, 2));

	}
}
