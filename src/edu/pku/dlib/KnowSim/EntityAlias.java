package edu.pku.dlib.KnowSim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * the class for keeping alias attributes of freebase from local file
 * @author Haoran Li
 */

public class EntityAlias {
	private static EntityAlias singleton;
    public static class Options {
    	@Option(gloss = "where to load EntityInfo")
    	String entityInfoPath = "lib/fb_data/alias/entityInfo.txt";
    	@Option(gloss = "where to load EntityAlias")
    	String entityAliasPath = "lib/fb_data/alias/abbreviation_alias.txt";
    	boolean toLowerCase = false;
	}
	public static Options opts = new Options();
	public static EntityAlias getSingleton() {
		if (singleton == null)
			singleton = new EntityAlias();
		return singleton;
	}
	
	public Map<String, List<EntityInfo>> aliasToEntityInfo;
	private EntityAlias() {
		LogInfo.begin_track("loading EntityAlias");
		Map<String, Set<String>> id2alias = loadEntityAlias(opts.entityAliasPath);
		List<EntityInfo> entityInfos = loadEntityInfo(opts.entityInfoPath);
		generateAliasMap(id2alias, entityInfos);
		LogInfo.logs("load %d alias, %d entities", aliasToEntityInfo.size(), entityInfos.size());
		LogInfo.end_track();
	}
	
	private void generateAliasMap(Map<String, Set<String>> id2alias, List<EntityInfo> entityInfos ) {
		Collections.sort(entityInfos, new EntityInfo.EntityInfoComparator());
		aliasToEntityInfo = new HashMap<>();
		for (EntityInfo entityInfo: entityInfos) {
			String id = entityInfo.id;
			Set<String> aliases = id2alias.get(id);
			if (aliases == null) 
				continue;
			for (String alias: aliases) {
				if (opts.toLowerCase)
					alias = alias.toLowerCase();
				if (!aliasToEntityInfo.containsKey(alias))
					aliasToEntityInfo.put(alias, new ArrayList<>());
				aliasToEntityInfo.get(alias).add(entityInfo);
			}
			
		}
	}
	
	public List<EntityInfo> getEntityInfo(String alias, int k) {
		List<EntityInfo> infos = aliasToEntityInfo.get(alias);
		if (infos == null)
			return null;
		else 
			return infos.subList(0, Math.min(infos.size(), k));
	}
	
	public boolean hasAlias(String alias) {
		return aliasToEntityInfo.containsKey(alias);
	}
	
	private Map<String, Set<String>> loadEntityAlias(String path) {
		Map<String, Set<String>> id2alias = new HashMap<>();
		for (String line: IOUtils.readLinesHard(path)) {
			String[] arr = line.trim().split("\t");
			if (!id2alias.containsKey(arr[0])) {
				id2alias.put(arr[0], new HashSet<String>());
			}
			id2alias.get(arr[0]).add(arr[1]);
		}
		return id2alias;
	}
	
	private List<EntityInfo> loadEntityInfo(String path) {
		List<EntityInfo> entityInfos = new ArrayList<>();
		for (String line: IOUtils.readLinesHard(path)) {
			EntityInfo info = EntityInfo.fromString(line);
			if (info == null)
				continue;
			entityInfos.add(info);
		}
		return entityInfos;
	}
	
	public static void main(String[] argv) {
		EntityAlias singleton = EntityAlias.getSingleton();
		List<EntityInfo> ret = singleton.getEntityInfo("AAA", 3);
		for (EntityInfo info: ret) {
			System.out.println(info.toString());
		}
		System.out.println(singleton.hasAlias("AA"));
	}
}
