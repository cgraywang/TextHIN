package edu.pku.dlib.models.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.pku.dlib.models.datastructure.ColtSparseVector;
import edu.pku.dlib.models.graph.PageRank_Nibble;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * pruning the graph to get a compact local graph
 * @author Haoran Li
 */

class Index {
	int type_index;
	int entity_index;
	Index(int type_index, int entity_index)
	{
		this.type_index = type_index;
		this.entity_index = entity_index;
	}
	
	@Override
	public int hashCode()
	{
		return type_index * 999997 + entity_index;
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Index v = (Index) o;

        return this.type_index == v.type_index && this.entity_index == v.entity_index;
	
	}

}

public class GraphPrune {
	
	static List<DoubleMatrix1D> doubleArray2Matrix(double[][] arr)
	{
		List<DoubleMatrix1D> matrix = new ArrayList<DoubleMatrix1D>();
		for (int i = 0; i < arr.length;  i++)
		{
			matrix.add(new ColtSparseVector(arr[i]));
		}
		return matrix;
	}
	
	public static int getVertexNum(Map<Integer, double[][]> interMats, boolean self_cycle)
	{
		int num = 0;
		for (int i = 0; i < interMats.size(); i++)
		{
			num += interMats.get(i).length;
		}
		if (! self_cycle)
		{
			num += interMats.get(interMats.size() - 1)[0].length;
		}
		return num;
	}
	
	public static int getVertexNum(List<List<DoubleMatrix1D>> interMats, boolean self_cycle, Map<Integer, Integer> label_map)
	{
		int num = 0;
		Set<Integer> visited = new HashSet<Integer>();
		for (int i = 0; i < interMats.size(); i++)
		{
			int idx = label_map.get(i);
			if (! visited.contains(idx)){
				num += interMats.get(i).size();
				visited.add(idx);
			}
		}
		if (!self_cycle)
		{
			num += interMats.get(interMats.size() - 1).get(0).size();
		}
		return num;
	}
	
	
	public static List<List<DoubleMatrix1D>> pruneGraph(List<List<DoubleMatrix1D>> interMats, boolean self_cycle, int[] start_doc, Map<Integer, Integer> doc2graphidx, Map<Integer, Integer> label_map)
	{
		int vertex_num = getVertexNum(interMats, self_cycle, label_map);
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		// no start_doc means all docs
		if (start_doc.length == 0)
		{
			List<DoubleMatrix1D> mat = interMats.get(0);
			start_doc = new int[mat.size()];
			for (int i = 0; i < start_doc.length; i++)
				start_doc[i] = i;
		}
		PageRank_Nibble nibble = new PageRank_Nibble(vertex_num);
		Map<Integer, Index> pru2ori = new HashMap<Integer, Index>();
		Map<Index, Integer> ori2pru = new HashMap<Index, Integer>();
		int vertex_cnt = 0;
		
		// load the graph
		for (int i = 0; i < interMats.size(); i++)
		{
			List<DoubleMatrix1D> mat = interMats.get(i);
			int rows = mat.size();
			int row_type_index = label_map.get(i);
			int col_type_index = label_map.get(i + 1);
			for (int row = 0; row < rows; row ++)
			{
				Index row_index = new Index(row_type_index, row);
				if (!ori2pru.containsKey(row_index))
				{
					ori2pru.put(row_index, vertex_cnt);
					pru2ori.put(vertex_cnt, row_index);
					vertex_cnt ++;
				}
				int row_graph_index = ori2pru.get(row_index);
				int col_graph_index = -1;
				mat.get(row).getNonZeros(indexList, valueList);
				for (int k = 0; k < indexList.size(); k++)
				{
					int col = indexList.get(k);
					double value = valueList.get(k);
					Index col_index = new Index(col_type_index, col);
					if (!ori2pru.containsKey(col_index))
					{
						ori2pru.put(col_index, vertex_cnt);
						pru2ori.put(vertex_cnt, col_index);
						vertex_cnt ++;
					}
					col_graph_index = ori2pru.get(col_index);
					nibble.addUndirectedEdge(row_graph_index, col_graph_index,value);
				}
			}
		}
		Map<String, Double> setting = new HashMap<String, Double>();
;
		setting.put("eps", 0.0000002);
		setting.put("alpha", 0.0001);
		int[] graph_st_vertexes = new int[start_doc.length];
		for (int i = 0; i < start_doc.length; i++)
			graph_st_vertexes[i] = ori2pru.get(new Index(0, start_doc[i]));
		Set<Integer> reserved = nibble.PRNibble(graph_st_vertexes, 0.3, 13, setting);
		List<List<DoubleMatrix1D>> new_interMats = new ArrayList<List<DoubleMatrix1D>>();

		for (int i = 0; i < interMats.size(); i++)
		{
			List<DoubleMatrix1D> pre_mat = interMats.get(i), new_mat = new ArrayList<DoubleMatrix1D>();
			int row_type = label_map.get(i), col_type = label_map.get(i +1);
			int row_cnt = -1, col_cnt = -1;
			Map<Integer, Integer> valid_col_idx = new HashMap<Integer, Integer>();
			int cols = pre_mat.get(0).size();
			for (int col = 0; col < cols; col++)
			{
				int col_grapg_index = ori2pru.get(new Index(col_type, col));
				if (reserved.contains(col_grapg_index))
				{
					col_cnt ++ ;
					valid_col_idx.put(col, col_cnt);
				}
			}
			for (int row = 0; row < pre_mat.size(); row ++)
			{
				int row_graph_index = ori2pru.get(new Index(row_type, row));
				if (reserved.contains(row_graph_index))
				{
					row_cnt ++;
					if (i == 0)
					{
						doc2graphidx.put(row, row_cnt);
					}
					DoubleMatrix1D vector = new ColtSparseVector(valid_col_idx.size());
					pre_mat.get(row).getNonZeros(indexList, valueList);
					for (int k = 0; k < indexList.size(); k ++)
					{
						int col = indexList.get(k);
						double value = valueList.get(k);
						if (valid_col_idx.containsKey(col))
							vector.setQuick(valid_col_idx.get(col), value);
					}
					new_mat.add(vector);
				}
			}
			new_interMats.add(new_mat);
		}
		return new_interMats;
	}
	
	
	//  if self-cycle D -> P -> V -> D, if not self-cycle D -> P -> V 
	public static Map<Integer, double[][]>  pruneGraph(Map<Integer, double[][]> interMats, boolean self_cycle, int[] start_doc, Map<Integer, Integer> doc2graphidx)
	{
		
		int vertex_num = getVertexNum(interMats, self_cycle);
		// no start_doc means all docs
		if (start_doc.length == 0)
		{
			double[][] mat = interMats.get(0);
			start_doc = new int[mat.length];
			for (int i = 0; i < start_doc.length; i++)
				start_doc[i] = i;
		}
		PageRank_Nibble nibble = new PageRank_Nibble(vertex_num);
		Map<Integer, Index> pru2ori = new HashMap<Integer, Index>();
		Map<Index, Integer> ori2pru = new HashMap<Index, Integer>();
		int vertex_cnt = 0;
		
		// load the graph
		for (int i = 0; i < interMats.size(); i++)
		{
			double[][] mat = interMats.get(i);
			int rows = mat.length, cols = mat[0].length;
			int row_type_index = i;
			int col_type_index = i + 1;
			if (i == interMats.size() -1 && self_cycle)
				col_type_index = 0;
			for (int row = 0; row < rows; row++)
			{
				Index row_index = new Index(row_type_index, row);
				
				if (!ori2pru.containsKey(row_index))
				{
					ori2pru.put(row_index, vertex_cnt);
					pru2ori.put(vertex_cnt, row_index);
					vertex_cnt ++;
				}
				int row_graph_index = ori2pru.get(row_index);
				int col_graph_index = -1;
				for (int col = 0; col < cols; col ++)
				{
					Index col_index = new Index(col_type_index, col);
					if (!ori2pru.containsKey(col_index))
					{
						ori2pru.put(col_index, vertex_cnt);
						pru2ori.put(vertex_cnt, col_index);
						vertex_cnt ++;
					}
					col_graph_index = ori2pru.get(col_index);
					if (mat[row][col] > 0)
					{
						nibble.addUndirectedEdge(row_graph_index, col_graph_index, mat[row][col]);
					}
				}
			}
		}
		
		Map<String, Double> setting = new HashMap<String, Double>();
		setting.put("eps", 0.03);
		setting.put("alpha", 0.2);
		int[] graph_st_vertexes = new int[start_doc.length];
		for (int i = 0; i < start_doc.length; i++)
			graph_st_vertexes[i] = ori2pru.get(new Index(0, start_doc[i]));
		Set<Integer> reserved = nibble.PRNibble(graph_st_vertexes, 0.3, 9, setting);
		System.out.println("reserved:" + reserved);
		Map<Integer, double[][]> new_interMats = new HashMap<Integer, double[][]>();
//		Map<Integer, Integer> doc2graphidx = new HashMap<Integer, Integer>();
		for (int i = 0; i < interMats.size(); i++)
		{
			double[][] pre_mat = interMats.get(i);
			int n = pre_mat.length, m = pre_mat[0].length;
			int row_type = i, col_type = i +1;
			if (i == interMats.size() -1 && self_cycle)
				col_type = 0;
			Set<Integer> row_reserved = new HashSet<Integer>(), col_reserved = new HashSet<Integer>();
			for (int row = 0; row < pre_mat.length; row ++)
			{
				int graph_index = ori2pru.get(new Index(row_type, row));
				if (reserved.contains(graph_index))
					row_reserved.add(row);
			}
			for (int col = 0; col < m; col ++)
			{
				int graph_index = ori2pru.get(new Index(col_type, col));
				if (reserved.contains(graph_index))
					col_reserved.add(col);
			}
			
			int newn = row_reserved.size(), newm = col_reserved.size();
			double[][] new_mat = new double[newn][newm];
			
			int row_cnt = 0, col_cnt = 0;
			for (int row = 0; row < n; row++)
			{
				col_cnt = 0;
				if (!row_reserved.contains(row))
					continue;
				if (i == 0)
					doc2graphidx.put(row, row_cnt);
				for (int col = 0; col < m; col ++)
				{
					if (! col_reserved.contains(col))
						continue;
					new_mat[row_cnt][col_cnt] = pre_mat[row][col];
					col_cnt ++;
				}
				row_cnt ++;
			}
			new_interMats.put(i, new_mat);
		}
		return new_interMats;
	}
	
	public static void main(String[] args) {
		/*
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
		Map<Integer, double[][]> interMats = new HashMap<Integer, double[][]>(), mats;
		Map<Integer, Integer> doc2graph = new HashMap<Integer, Integer>();
		interMats.put(0, adj1);
		interMats.put(1, adj2);
		int[] vs = {0,2};
		mats = pruneGraph(interMats, false, vs, doc2graph);
		System.out.println(doc2graph);
		for (int i = 0; i < mats.size(); i++)
		{
			double[][] mat = mats.get(i);
			for (int row = 0; row < mat.length; row++)
			{
				
				for (int col = 0; col < mat[row].length; col++)
				{
					System.out.print(mat[row][col]+"\t");
					
				}
				System.out.println();
			}
			System.out.println();			
		}
		
		List<List<DoubleMatrix1D>> sparse_interMats = new ArrayList<List<DoubleMatrix1D>>();
		sparse_interMats.add(doubleArray2Matrix(adj1));
		sparse_interMats.add(doubleArray2Matrix(adj2));
		doc2graph = new HashMap<Integer, Integer>();
		sparse_interMats = pruneGraph(sparse_interMats, false, vs, doc2graph);
		for (List<DoubleMatrix1D> mat: sparse_interMats)
			System.out.println(mat);
		System.out.println(doc2graph);
//		for (int i = 0; i < 2; i ++)
//		{
//			for (int j = 0; j < 2; j++)
//				System.out.print(mats.get(0)[i][j] + " ");
//			System.out.println();
//		}
//		System.out.println("\n"+doc2graph);
 * */
 
		
	}
	
	
}
