package models.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * @author Haoran Li
 */

public class MetaPath implements Serializable{
	private static final long serialVersionUID = 5877310768012638146L;
	public List<String> path;
	public static int maxLength = 2;
	public MetaPath()
	{
		path = new ArrayList<String>();
	}
	
	public MetaPath(String Et)
	{
		path = new ArrayList<String>();
		this.add("doc");
		this.add(Et);
		this.add("doc");
	}
	
	public MetaPath(List<String> types)
	{
		path = new ArrayList<String>();
		this.add("doc");
		for (String type: types)
			this.add(type);
		this.add("doc");
	}
	
	public MetaPath(String Et1, String Et2)
	{
		path = new ArrayList<String>();
		this.add("doc");
		this.add(Et1);
		this.add(Et2);
		this.add("doc");
	}
	
	public MetaPath(String Et1, String Et2, String Et3)
	{
		path = new ArrayList<String>();
		this.add("doc");
		this.add(Et1);
		this.add(Et2);
		this.add(Et3);
		this.add("doc");
	}
	
	public MetaPath(double[] feature, int[] rank, Map<Integer, String> idx2type)
	{
		path = new ArrayList<String>();
		path.add("doc");
		int iter = 0;
		for (int i = 0; i< rank.length; i++)
		{
			int idx = rank[i];
			if (feature[idx] > 0.01)
			{
				path.add(idx2type.get(idx));
				iter += 1;
				if (iter >= maxLength)
					break;
			}
			
		}
	}
	
	@Override
	public int hashCode()
	{
		return path.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaPath v = (MetaPath) o;

        return this.path.equals(v.path);
	}
	
	public void add(String node)
	{
		path.add(node);
	}
	
	public boolean isSymmetrical() {
		int l = 0, r = path.size() - 1;
		while (l < r)
		{
			if (!path.get(l).equals(path.get(r)))
				return false;
			l ++; r--;
		}
		return true;
	}
	
	public boolean isProper() {
		for (int i = 0; i < path.size() - 1; i++)
			if (path.get(i).equals(path.get(i+1)))
				return false;
		return true;
	}
	
	public String toString()
	{
//		String out = "doc";
		String out = "";
		for (String node:path)
		{
			if (!out.equals(""))
				out += " -> ";
			out += node;
		}
		return out;
	}
	
	public static Set<String> getAllTypes(Map<String, String> e_topet)
	{
		Set<String> set = new HashSet<String>();
		for (String type: e_topet.values())
		{
			set.add(type);
		}
		return set;
	}
	
	public static void enumerate_path(int max_length, int now_length, Set<String> type_set, List<String> path, List<MetaPath> metaPaths)
	{
		if (now_length > 0)
		{
			MetaPath metaPath = new MetaPath(path);
			metaPaths.add(metaPath);
		}
		if (now_length == max_length)
			return;
		for (String next_type: type_set)
		{
			// limit ith type and i-1th type
//			if (path.size() > 0 && path.get(path.size() - 1).equals(next_type))
//				continue;
			path.add(next_type);
			enumerate_path(max_length, now_length+1, type_set, path, metaPaths);
			path.remove(now_length);
		}
		
	}
	
	public static List<MetaPath> generateAllMetaPaths(int max_length, Map<String, String> e_topet)
	{
		Set<String> type_set = getAllTypes(e_topet);
		List<MetaPath> metaPaths = new ArrayList<MetaPath>();
		enumerate_path(max_length, 0, type_set, new ArrayList<String>(), metaPaths);
		return metaPaths;
	}
	
}