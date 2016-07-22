package models.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import models.datastructure.ColtSparseVector;


import cern.colt.matrix.DoubleMatrix1D;

/**
 * @author Haoran Li
 */

class Entity implements Serializable
{

	private static final long serialVersionUID = -8244748719673817319L;
	String entity, entity_type;
	Entity(String _entity, String _entity_type)
	{
		entity = _entity;
		entity_type = _entity_type;
	}
}


public class Co_Occur_Block implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2579799138682841222L;
	/**
	 * 
	 */

//	private static final long serialVersionUID = 1L;
	public List<Entity> entities;
	public static Map<String, Map<String, Integer>> et_e_idx = new HashMap<String, Map<String,Integer>>();
	
	Co_Occur_Block(String docid, Map<String, Integer> e_cnt, Map<String, String> e_topet)
	{
		entities = new ArrayList<Entity>();
		entities.add(new Entity(docid,"doc"));
		if (!et_e_idx.containsKey("doc"))
			et_e_idx.put("doc", new HashMap<String, Integer>());
		if (!et_e_idx.get("doc").containsKey(docid))
		{
			int size = et_e_idx.get("doc").size();
			et_e_idx.get("doc").put(docid, size);
		}
		
		for (String entity: e_cnt.keySet())
		{
			if (!e_topet.containsKey(entity))
				continue;
			String entity_type = e_topet.get(entity);
			int num = e_cnt.get(entity);
			for (int i = 0;  i < num; i++)
				entities.add(new Entity(entity, entity_type));
			if (!et_e_idx.containsKey(entity_type))
				et_e_idx.put(entity_type, new HashMap<String, Integer>());
			Map<String, Integer> e_idx = et_e_idx.get(entity_type);
			if (!e_idx.containsKey(entity))
			{
				int size = e_idx.size();
				e_idx.put(entity, size);
			}
		}
	} 
	
	
	
	
	
	public static List<DoubleMatrix1D> calcMatrix(String type1, String type2, List<Co_Occur_Block> blocks, Map<String, Integer> doc_totet, boolean normalized)
	{
		Map<String, Integer> type1_idx = et_e_idx.get(type1);
		Map<String, Integer> type2_idx = et_e_idx.get(type2);
 		int n = type1_idx.size(), m = type2_idx.size();
 		List<DoubleMatrix1D> mat = new ArrayList<DoubleMatrix1D>();
 		for (int i = 0; i < n; i++)
 			mat.add(new ColtSparseVector(m));
 					
 		for (Co_Occur_Block block: blocks)
 		{
 			List<Entity> entities = block.entities;
 			for (int i = 0; i < entities.size(); i++)
 			{
 				Entity ei = entities.get(i); 
 				if (!ei.entity_type.equals(type1))
 					continue;
 				for (int j = 0; j < entities.size(); j++)
 				{
 					if (j == i)
 						continue;
 					Entity ej = entities.get(j);
 					if (!ej.entity_type.equals(type2))
 						continue;
 					int idx_i = type1_idx.get(ei.entity), idx_j = type2_idx.get(ej.entity);

 					double tmp = mat.get(idx_i).get(idx_j) + 1;
 					mat.get(idx_i).set(idx_j, tmp);

 				}
 			}
 		}
 		
 		if (!normalized)
 			return mat;
 		if (type1.equals("doc"))
 		{
 			double[] sum = new double[n];
 			if (!type2.equals("word"))
 			{	

 				for (String doc: type1_idx.keySet())
 				{
 					sum[type1_idx.get(doc)] = doc_totet.get(doc);
 				}
 			} else
 			{
 				for (int i = 0; i < n; i++)
 					for (int j = 0; j < m; j++)
 						sum[i] += mat.get(i).getQuick(j);
 			}
 			for (int i = 0; i < n; i++)
 			{
 				double subsum = sum[i];
 				if (subsum != 0)
 					for (int j = 0; j < m; j++)
 						mat.get(i).setQuick(j, mat.get(i).getQuick(j)/subsum);
 			}
 		} else if (type2.equals("doc"))
 		{
 			double[] sum = new double[m];
 			if (!type1.equals("word"))
 			{
 				for (String doc: type2_idx.keySet())
 	 				sum[type2_idx.get(doc)] = doc_totet.get(doc);
 			} else
 			{
 				for (int i = 0; i < n; i++)
 					for (int j = 0; j < m; j++)
 						sum[j] += mat.get(i).getQuick(j);
 			}
 			for (int j = 0;j < m; j++)
 			{
 				double subsum = sum[j];
 				if (subsum != 0)
 					for(int i = 0; i < n; i++)
 					{
 						double value = mat.get(i).getQuick(j) / subsum;
 						mat.get(i).setQuick(j, value);
 					}
 			}
 		}
 		return mat;
	}
	
	static Map<String, Set<String>> e_etset;
	public static void setEntity_Et(List<Co_Occur_Block> blocks)
	{
		e_etset = new HashMap<String, Set<String>>();
		for (Co_Occur_Block block: blocks)
		{
			for (int i = 0; i < block.entities.size(); i++)
			{
				Entity entity_src = block.entities.get(i);
				String e = entity_src.entity;
				if (entity_src.entity_type.equals("doc"))
					continue;
				if (!e_etset.containsKey(e))
					e_etset.put(e, new HashSet<String>());
				Set<String> etset = e_etset.get(e);
				for (int j = 0; j < block.entities.size(); j++)
				{
					if (i == j || block.entities.get(j).entity_type.equals("doc") || block.entities.get(j).entity_type.equals(entity_src.entity_type))
						continue;
					String et = block.entities.get(j).entity_type;
					etset.add(et);
					
				}
			}
		}
	}
	
	static Map<String, Set<MetaPath>> doc_pathset;
	public static Map<String, Set<MetaPath>> getDocMetaPathSet(Map<String, Map<String, Integer>> doc_e_cnt, Map<String, String> e_topet)
	{
		doc_pathset = new HashMap<String, Set<MetaPath>>();
		for (Entry<String, Map<String, Integer>> doc_entry: doc_e_cnt.entrySet())
		{
			String doc = doc_entry.getKey();
			doc_pathset.put(doc, new HashSet<MetaPath>());
			Set<MetaPath> metapath_set = doc_pathset.get(doc);
			Set<String> eset = doc_entry.getValue().keySet();
//			Set<String> first_types = new HashSet<String>();
			for (String entity: eset) 	// enumerate entities of  doc 
			{
				if (e_topet.containsKey(entity))	// have a type
				{
					String first_type = e_topet.get(entity);

					metapath_set.add(new MetaPath(first_type));
					
					for (String second_type: e_etset.get(entity))
					{
						if (second_type.equals(first_type))
							continue;
						metapath_set.add(new MetaPath(first_type, second_type));
					}
				}
				
			}
		}
		return doc_pathset;
	}

}