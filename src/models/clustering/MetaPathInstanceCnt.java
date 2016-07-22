package models.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import models.datastructure.ColtSparseVector;
import models.util.matrix.Matrix2DUtil;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
/**
 * @author Haoran Li
 */

public class MetaPathInstanceCnt {
	MetaPath metaPath;
	List<List<DoubleMatrix1D>> interMats;
	List<DoubleMatrix1D> ans;
	Map<Integer, Integer> label_map;
	int doc_num;
	Map<Integer, Integer> doc2graphidx;
	
	public MetaPathInstanceCnt(MetaPath metaPath,List<List<DoubleMatrix1D>> interMats) {
		this.metaPath = metaPath;
		this.interMats = interMats;
		doc_num = interMats.get(0).size();
		
		label_map = new HashMap<Integer, Integer>();
		Set<String> types = new HashSet<String>();
		int cnt = 0;
		for(int i = 0; i < metaPath.path.size(); i++)
		{
			String type = metaPath.path.get(i);
			if (!types.contains(type))
			{
				types.add(type);
				label_map.put(i, cnt);
				cnt ++;
			} else {
				for (int j = 0; j < i; j++)
					if (metaPath.path.get(j).equals(type))
					{
						label_map.put(i, label_map.get(j));
						break;
					}
			}
		}	
	}
	
	
	public void pruning()
	{
		int[] vs = {};
		doc2graphidx = new HashMap<Integer, Integer>();
		interMats = GraphPrune.pruneGraph(interMats, true, vs, doc2graphidx, label_map);
//		interMats = GraphPrune.pruneGraph(interMats, false, vs, doc2graphidx, label_map);
	}
	
	public void calc()
	{
		int length = interMats.size();
		int minsize = 999999999, minidx = -1;
		for (int i = 0; i < interMats.size() ; i ++)
			if (interMats.get(i).size() < minsize)
			{
				minsize = interMats.get(i).size();
				minidx = i;
			}
		if (minsize == 0)
		{
			this.ans = new ArrayList<DoubleMatrix1D>();
			return;
		}
		if (minidx == 0)
		{
			ans = interMats.get(0);
			for (int i = 1; i < length; i++)
				ans = Matrix2DUtil.SparseMultSparse(ans, interMats.get(i));
		} else
		{
			List<DoubleMatrix1D> left, right;
			left = interMats.get(minidx-1);
			right = interMats.get(minidx);
			for (int i = minidx + 1; i < length; i++)
				right = Matrix2DUtil.SparseMultSparse(right, interMats.get(i));
			for (int i = minidx - 2; i >= 0; i--)
			{
				left = Matrix2DUtil.SparseMultSparse(interMats.get(i), left);
			}
			ans = Matrix2DUtil.SparseMultSparse(left, right);
		}
	}
	
	public List<DoubleMatrix1D> restore()
	{
		if (doc2graphidx == null)
			return ans;
		if (ans.size() == 0)
			return ans;
		Map<Integer, Integer> graphidx2doc = new HashMap<Integer, Integer>();
		for (Entry<Integer, Integer> entry: doc2graphidx.entrySet())
		{
			graphidx2doc.put(entry.getValue(), entry.getKey());
		}
		List<DoubleMatrix1D> mat  = new ArrayList<DoubleMatrix1D>();
		for(int i = 0; i < doc_num; i++)
			mat.add(new ColtSparseVector(doc_num));
		for (int i = 0; i < ans.size(); i++)
		{
			DoubleMatrix1D shrinked_vec = ans.get(i), vec = mat.get(graphidx2doc.get(i));
			IntArrayList indexList = new IntArrayList();
			DoubleArrayList valueList = new DoubleArrayList();
			shrinked_vec.getNonZeros(indexList, valueList);
			for (int k = 0; k < indexList.size(); k++)
			{
				int idx = indexList.get(k);
				double value = valueList.get(k);
				vec.setQuick(graphidx2doc.get(idx), value);
			}
		}
		return mat;
	}
	
	public void storeToFile(String file)
	{
		
		try {
			List<DoubleMatrix1D> mat = restore();
			ObjectWriter.writeObject(mat, file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		double[][] adj1 = {
				{1,1,1,0},
				{0,0,0,0},
				{0,0,1,0},
				{0,1,0,0}
		};
		double[][] adj2 = {
				{1,0,0,0,0},
				{1,1,0,0,0},
				{1,0,1,0,0},
				{0,1,0,1,1}
		};
		
		double[][] adj3 = {
				{1,0,0,1},
				{1,0,0,0},
				{0,0,0,0},
				{1,0,0,0},
				{1,0,0,0}
		};
		List<List<DoubleMatrix1D>> interMats = new ArrayList<List<DoubleMatrix1D>>();
		interMats.add(GraphPrune.doubleArray2Matrix(adj1));
		interMats.add(GraphPrune.doubleArray2Matrix(adj2));
		interMats.add(GraphPrune.doubleArray2Matrix(adj3));
		MetaPathInstanceCnt mpcnt = new MetaPathInstanceCnt(new MetaPath(), interMats);
		mpcnt.pruning();
		for (int i = 0; i < mpcnt.interMats.size(); i++)
			System.out.println(mpcnt.interMats.get(i));
		mpcnt.calc();
		System.out.println(mpcnt.ans);
		System.out.println(mpcnt.restore());
	}
}
