package edu.stanford.nlp.sempre.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;

import edu.stanford.nlp.sempre.Executor;
import edu.stanford.nlp.sempre.Formula;
import edu.stanford.nlp.sempre.Formulas;
import edu.stanford.nlp.sempre.JoinFormula;
import edu.stanford.nlp.sempre.LambdaFormula;
import edu.stanford.nlp.sempre.Relation;
import edu.stanford.nlp.sempre.RelationInfo;
import edu.stanford.nlp.sempre.RelationSearcher;
import edu.stanford.nlp.sempre.ValueFormula;
import edu.stanford.nlp.sempre.freebase.SparqlExecutor;
import fig.basic.Pair;

/**
 * @author Haoran Li
 */

public class RelationCache {
	private Map<String, List<RelationInfo>> cache;
	Executor executor;
	
	public RelationCache(Executor executor) {
		this.cache = new HashMap<>();
		this.executor = executor;
	}
	
	public static String entityPairID(String e1, String e2) {
		return e1 + "#" + e2;
	}
	
	static Set<String> bigQuerys;
	static Set<String> bigIDs;
	
	static public void init() {
		String[] bigQuerysArr = {"fb:en.united_states_of_america#fb:en.united_kingdom_of_great_britain_and_ireland", 
		"fb:en.united_kingdom_of_great_britain_and_ireland#fb:en.united_states_of_america"};
		String[] bigQueryIDArr = {"fb:en.united_states_of_america"};
		bigQuerys = new HashSet<>();
		bigIDs = new HashSet<>();
		Collections.addAll(bigQuerys, bigQuerysArr);
		Collections.addAll(bigIDs, bigQueryIDArr);
		
	}
	static public boolean isBigQuery(String id) {
		String[] parts = id.split("#");

		if (bigQuerys == null)
			init();
		if (bigIDs.contains(parts[0]) || bigIDs.contains(parts[1]))
			return true;
		return bigQuerys.contains(id);
	}
	
	public void search(String e1, String e2) {
		String id = entityPairID(e1, e2);
		if (cache.containsKey(id))
			return;
		if (isBigQuery(id)) {
			cache.put(id, new ArrayList<>());
			return;
		}
			
//		System.out.print(id);
		RelationSearcher searcher = new RelationSearcher(e1, e2);
		List<RelationInfo> res = searcher.searchRelation(executor);
		cache.put(id, res);
//		System.out.println(" over");
	}
	
	public List<Pair<String, String>> searchWeakRelation(String e1, String e2) {
		List<Pair<String, String>> res = new ArrayList<>();
		search(e1, e2);
		String id = entityPairID(e1, e2);
		List<RelationInfo> infos = cache.get(id);
		for (RelationInfo info: infos) {
			res.add(Pair.newPair(info.retType, info.argType));
		}
		return res;
	}
	
	public String properType(String type) {
		int x= type.indexOf(":");
		type = type.substring(x+1);
		return type;
	}
	
	public Relation getWeakRelation(String e1, String e2, String retType, String argType) {
		String id = entityPairID(e1, e2);
		List<RelationInfo> infos = cache.get(id);
		
		for (RelationInfo info: infos) {
			if (properType(info.retType).equals(retType) && properType(info.argType).equals(argType)) {
				return new Relation(info, e1, e2, 2); 
			}
		}
		return null;
	}
	
	public static String[] parseRelations(Formula formula) {
		String[] relations;
		if (formula instanceof ValueFormula) {
			relations = new String[1];
			relations[0] =  Formulas.getString(formula);
		} else if (formula instanceof LambdaFormula) {
			relations = new String[2];
			JoinFormula body= (JoinFormula)((LambdaFormula)formula).body;
			relations[0] = Formulas.getString(body.relation);
			relations[1] = Formulas.getString(((JoinFormula)(body.child)).relation);
		} else
			throw new RuntimeException("unvalid relation formula:" + formula.toString());
		return relations;
	}
	
	
	
	public static boolean isReversed(String relation) {
		return relation.startsWith("!");
	}
	
	public static RelationInfo properRelation(String[] relations, String retType, String argType) {
		
		int length = relations.length;
		if (isReversed(relations[0])) {
			String[] ret = new String[relations.length];
			for (int i = 0; i < length; i++)
				ret[i] = properRelation(relations[length - 1 - i]);
			return new RelationInfo(argType, retType, ret);
		} else
			return new RelationInfo(retType, argType, relations);
	}
	
	
	public static String properRelation(String relation) {
		if (relation.startsWith("!"))
			return relation.substring(1);
		else
			return relation;
	}
	
	public Relation searchStrongRelation(String e1, String e2, Formula formula, String retType, String argType) {
		String[] relations = parseRelations(formula);
		if (isReversed(relations[0])) {
			String tmp = e1;
			e1 = e2;
			e2 = tmp;
			String[] new_relations = new String[relations.length];
			for (int i = 0; i < relations.length; i++) {
				new_relations[relations.length - 1 - i] = properRelation(relations[i]);
			}
			relations = new_relations;
		}
		search(e1, e2);
		String id = entityPairID(e1, e2);
		int cnt = 0;
		for (RelationInfo info: cache.get(id)) {
			if (info.isSameRelation(relations)) 
				cnt ++;
		}
		if (cnt > 0) {
			RelationInfo rInfo = new RelationInfo(retType, argType, relations);
			return new Relation(rInfo, e1, e2, 1);
		} else
			return null;
	}

	
	
	public static void main(String[] argv) {
		RelationCache cache = new RelationCache(new SparqlExecutor());
		Formula formula = Formula.fromString("(lambda x (!fb:government.political_party_tenure.party (!fb:government.politician.party (var x))))");
		Formula formula2 = Formula.fromString("(lambda x (fb:government.politician.party (fb:government.political_party_tenure.party (var x))))");
//		int cnt = cache.searchStrongRelation("fb:en.democrat_party","fb:en.barack_obama",  formula);
//		System.out.println(cnt);
//		cnt = cache.searchStrongRelation("fb:en.barack_obama", "fb:en.democrat_party",  formula2);
//		System.out.println(cnt);
		List<Pair<String, String>> res = cache.searchWeakRelation("fb:en.democrat_party" ,"fb:en.barack_obama");
		for (Pair<String, String> p:res) {
			System.out.println(p.getFirst() + " " + p.getSecond());
		}
		cache.search("fb:en.christianity", "fb:en.bible");
		for (RelationInfo info:cache.cache.get(cache.entityPairID("fb:en.christianity", "fb:en.bible"))) {
			System.out.println(info);
		}
	}
	
	
}
