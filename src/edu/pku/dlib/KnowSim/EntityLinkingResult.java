package edu.pku.dlib.KnowSim;

import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.sempre.freebase.lexicons.TokenLevelMatchFeatures;
import fig.basic.LogInfo;
import fig.basic.Option;


/**
 * class for keeping AIDA's entity Linking result,
 * also do disambiguation for entities' names.
 * @author Haoran Li
 */

public class EntityLinkingResult {
	
	public static class Options {
		@Option(gloss = "where to load Entity Linking Result")
		public String EntityLinkingResultPath = null;
	}

	public static Options opts = new Options();
	private static EntityLinkingResult singleton;
	Map<String, Map<String, String>> result;
	
	public EntityLinkingResult() { this.load(); }
	public EntityLinkingResult(String load_path) { opts.EntityLinkingResultPath = load_path; this.load(); }
	
	public static EntityLinkingResult getSingleton() {
		if (singleton == null)
			singleton = new EntityLinkingResult();
		return singleton;
	}
	
	private static class EntityLinkingInfo {
		String mention;
		String full_name;
		String wiki_url;
		
		public EntityLinkingInfo(String line)
		{
			parse(line);
		}
		
		public static String stripBrackets(String ori) {
			int bracket_index = ori.indexOf(" (");
			if (bracket_index == -1)
				return ori;
			else
				return ori.substring(0, bracket_index);
		}
		
		private void parse(String line) 
		{
			String[] arr = line.trim().split("\t");
			mention = arr[0].toLowerCase();
			if (arr[1].trim().equals("NO MATCHING ENTITY")) 
			{
				full_name = "$" + mention;
				wiki_url = "UNKNOWN";
			} else 
			{
				full_name = stripBrackets(arr[2].toLowerCase());
				wiki_url = arr[3];
			}
		}
		
		public boolean isValid()
		{
			return !(full_name == null);
		}
		
		public String getMention() {
			return this.mention;
		}
		
		public String getFullname() {
			return this.full_name;
		}
	}
	
	public String vote_entity(Map<String, Integer> entity_cnt) {
		int max_cnt = 0;
		String voted_entity = null;
		for (String entity: entity_cnt.keySet()) {
			int cnt = entity_cnt.get(entity);
			if (cnt > max_cnt) {
				max_cnt = cnt;
				voted_entity = entity;
			}
		}
		return voted_entity;
	}
	
	public static boolean tokenContains(String parent, String child) {
		if (parent.equals(child))
			return false;
		if (! parent.contains(child))
			return false;
		int tokenDistance = TokenLevelMatchFeatures.diffSetSize(parent, child);
		return tokenDistance < 3;
	}
	
	public void disambiguate(String docid, Map<String, String> entity_map, Map<String, Map<String, Integer>> map_cnt) {
		for (String mention: map_cnt.keySet()) {
			Map<String, Integer> entity_cnt = map_cnt.get(mention);
			if (entity_cnt.size() < 2)
				continue;
			String entity = vote_entity(entity_cnt);
			entity_map.put(mention, entity);
		}
		Map<String, String> childString = new HashMap<String, String>();
		for (String child: entity_map.keySet()) {
			for (String parent: entity_map.keySet()) {
				if (entity_map.get(parent).startsWith("$"))
					continue;
				if (tokenContains(parent, child)) {
					if (childString.containsKey(child)) {
						String last_parent = childString.get(child);
						String ancestor = null;
						if (tokenContains(last_parent, parent))
							ancestor = last_parent;
						else if (tokenContains(parent, last_parent))
							ancestor = parent;
						if (ancestor == null) {
							childString.remove(child);
//							LogInfo.logs("In article [%s], too many parent for mention[%s], p1 = [%s] p2 = [%s]", docid, child, parent, last_parent);
							break;
						} else {
							childString.put(child, ancestor);
						}
							
					} else {
						childString.put(child, parent);
					}
				}
			}
		}
		for (String child:childString.keySet()) {
//			LogInfo.logs("In article [%s], change result of mention[%s]", docid, child);
			String parent = childString.get(child);
			entity_map.put(child, entity_map.get(parent));
		}
		
		for (String mention: entity_map.keySet()) {
			String entity = entity_map.get(mention);
			if (entity.startsWith("$")) {
				entity = entity.substring(1);
				entity_map.put(mention, entity);
			}
		}
	}
	
	
	public void load() {
		result = new HashMap<String, Map<String, String>>();
		if (opts.EntityLinkingResultPath == null)
			return;
		LogInfo.begin_track("Load Entity Linking Result from [%s]", opts.EntityLinkingResultPath);
		String docid = null;
		Map<String, String> entity_map = null; 
		Map<String, Map<String, Integer>> map_cnt = null;  
		for (String line: IOUtils.readLines(opts.EntityLinkingResultPath)) {
			boolean isDocID = ! line.startsWith("\t");
			if (isDocID) {
				if (map_cnt != null) {
					disambiguate(docid, entity_map, map_cnt);
				}
				docid = line.trim();
				result.put(docid, new HashMap<String, String>());
				entity_map = result.get(docid);
				map_cnt = new HashMap<String, Map<String, Integer>>();
			} else {
				EntityLinkingInfo el_info = new EntityLinkingInfo(line.trim());
				if (el_info.isValid()) {
					String mention = el_info.getMention();
					if (! entity_map.containsKey(mention)) {
						entity_map.put(mention, el_info.getFullname());
						map_cnt.put(mention, new HashMap<String, Integer>());
						map_cnt.get(mention).put(el_info.getFullname(), 1);
					}
					else {
						Map<String, Integer> entity_cnt = map_cnt.get(mention);
						String entity = el_info.getFullname();
						if (! entity_cnt.containsKey(entity)) {
							entity_cnt.put(entity, 0);
						}
						int cnt = entity_cnt.get(entity);
						entity_cnt.put(entity, cnt + 1);
					}
				}
			}
		}
		LogInfo.logs("load [%d] document's result", result.size());
		LogInfo.end_track();
	}
	
	public Map<String, String> getEntityMap(String docid)
	{
		if (!this.result.containsKey(docid))
			return null;
		return this.result.get(docid);
	}
	
	
	public static void main(String[] argv) {
		EntityLinkingResult elr = new EntityLinkingResult();
		elr.load();
	}
}