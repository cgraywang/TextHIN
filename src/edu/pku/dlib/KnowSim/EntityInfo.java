package edu.pku.dlib.KnowSim;

import java.util.Comparator;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * class for keeping basic entities' info 
 * @author Haoran Li
 */

public class EntityInfo {
	public double popularity;
	public String id;
	public String name;
	public String[] types;
	
	public EntityInfo() {
		
	}
	
	public EntityInfo(String id, String name, List<String> types, double popularity) {
		this.id = id;
		this.name = name.replace("\t", " ");
		this.popularity = popularity;
		this.types = new String[types.size()];
		int idx = 0;
		for (String type: types) {
			this.types[idx++] = type;
		}
	}
	
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append(id);
		ret.append("\t" + name);
		ret.append("\t" + popularity);
		ret.append("\t" + Joiner.on(",").join(types));
		return ret.toString();
	}
	
	public static EntityInfo fromString(String str) {
		EntityInfo info = new EntityInfo();
		String[] arr = str.trim().split("\t");
		if (arr.length != 4)
			return null;
		info.id = arr[0];
		info.name = arr[1];
		info.popularity = Double.parseDouble(arr[2]);
		info.types = arr[3].split(",");
		return info;
	}
	
	public static class EntityInfoComparator implements Comparator<EntityInfo> {
	    @Override
	    public int compare(EntityInfo arg0, EntityInfo arg1) {
	    	if (arg0.popularity > arg1.popularity)
	    		return -1;
	    	if (arg0.popularity < arg1.popularity)
	    		return 1;
	    	return 0;
	    }
	  }
	
	
}
