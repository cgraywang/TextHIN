package edu.stanford.nlp.sempre;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.stanford.nlp.sempre.cache.PopularityCache;
import edu.stanford.nlp.sempre.cache.RelationCache;
import edu.stanford.nlp.sempre.freebase.FreebaseInfo;
import edu.stanford.nlp.sempre.freebase.SparqlExecutor;
import edu.stanford.nlp.sempre.freebase.SparqlExecutor.ServerResponse;
import fig.basic.Pair;

/**
 * @author Haoran Li
 */

public class RelationSearcher {
	
	static String mediatorFormatStr = "PREFIX fb: <http://rdf.freebase.com/ns/> "
			+ " SELECT ?domainType ?expectedType ?p1 ?p2"
			+ " WHERE {  "
			+ " %s ?p1 ?mediator. "
			+ " ?mediator fb:type.object.type ?mediator_type. " 
			+ " ?mediator_type fb:freebase.type_hints.mediator \"true\"^^xsd:boolean. "
			+ " ?mediator ?p2 %s. "
			+ " ?p1 fb:type.property.schema ?domainType. "
			+ " ?p2 fb:type.property.expected_type ?expectedType. "
			+ " }";
	static String[] mediatorNames = {"domainType", "expectedType", "p1", "p2"};
	
	static String relationFormatStr = "PREFIX fb: <http://rdf.freebase.com/ns/> "
			+ " SELECT ?domainType ?expectedType ?p"
			+ " WHERE {  "
			+ "%s ?p %s. "
			+ "?p fb:type.property.expected_type ?expectedType.?p "
			+ "fb:type.property.schema ?domainType."
			+ " }";
	
	static String[] relationNames = {"domainType", "expectedType", "p"};
	String e1,e2;
	String[] relations;
	
	public RelationSearcher(String e1, String e2) {
		this.e1 = e1;
		this.e2 = e2;
	}
	
	public RelationSearcher(String e1, String e2, Formula formula) {
		this.e1 = e1;
		this.e2 = e2;
		parseRelation(formula);
	}
	
	public RelationSearcher(String e1, Formula formula) {
		this.e1 = e1;
		parseRelation(formula);
	}
	
	public void parseRelation(Formula formula) {
		if (formula instanceof ValueFormula) {
			relations = new String[1];
			relations[0] =  Formulas.getString(formula);
		} else if (formula instanceof LambdaFormula) {
			relations = new String[2];
			JoinFormula body= (JoinFormula)((LambdaFormula)formula).body;
			relations[0] = Formulas.getString(body.relation);
			relations[1] = Formulas.getString(((JoinFormula)(body.child)).relation);
		} else if (formula instanceof JoinFormula) {
			relations = new String[1];
			JoinFormula joinFormula = (JoinFormula) formula;
			relations[0] = Formulas.getString(joinFormula.relation);
			this.e2 = Formulas.getString(joinFormula.child);
		} else
			throw new RuntimeException("unvalid relation formula:" + formula.toString());
			
	}
	
	
	
	public String constructQuery(boolean mediator) {
		String query = null;
		if (relations == null) {
			if (mediator) {
				query = String.format(mediatorFormatStr, e1, e2);
			} else 
				query = String.format(relationFormatStr, e1, e2);
		} else {
			String[] entities = new String[relations.length + 1];
			entities[0] = e1;
			entities[relations.length] = e2;
			for (int i = 1; i < relations.length; i++) {
				entities[i] = "?" + "mediator" + i;
			}
			StringBuffer body = new StringBuffer();
			for (int i = 0; i < relations.length; i++) {
				String subject, object, relation = relations[i];
				if (relation.startsWith("!")) {
					subject = entities[i+1];
					object = entities[i];
					relation = relation.substring(1);
				} else {
					subject = entities[i];
					object = entities[i+1];
				}
				
				body.append(subject + " " + relation + " " + object + ". ");
			}
			query = String.format("PREFIX fb: <http://rdf.freebase.com/ns/> SELECT count(*) as ?count WHERE { %s }", body.toString());
		}
		return query;
	}
	
	public String constructTypeQuery() {
		String query = String.format("PREFIX fb: <http://rdf.freebase.com/ns/> SELECT count(*) as ?count WHERE { %s %s %s.}",
				e1, relations[0], e2);
		return query;
	}
	
	public static List<Pair<String, String>> extractTypeValue(NodeList results) {
		List<Pair<String, String>> values = new ArrayList<>();
		for (int i = 0; i < results.getLength(); i++) {
			NodeList bindings = ((Element) results.item(i)).getElementsByTagName("binding");
			String domainType = null, expectedType = null;
			for (int j = 0; j < bindings.getLength(); j++) {
				Element binding = (Element) bindings.item(j);
				String var = binding.getAttribute("name");
				if (var.equals("domainType")) {
					String uri = SparqlExecutor.getTagValue("uri", binding);
					domainType = FreebaseInfo.uri2id(uri);
				}
				if (var.equals("expectedType")) {
					String uri = SparqlExecutor.getTagValue("uri", binding);
					expectedType = FreebaseInfo.uri2id(uri);
				}
			}
			values.add(Pair.newPair(domainType, expectedType));
		}
		return values;
	}
	
	public static String extractCount(NodeList results) {
		NodeList bindings = ((Element) results.item(0)).getElementsByTagName("binding");
		for (int j = 0; j < bindings.getLength(); j++) {
			Element binding = (Element) bindings.item(j);
			String var = binding.getAttribute("name");
			if (var.equals("count")) {
				String count = SparqlExecutor.getTagValue("literal", binding);
				return count;
			}
		}
		throw new RuntimeException("extractCount error:" + results.toString());
	}
	
	public static List<Map<String, String>> extractNamesFromResults(NodeList results, String[] names) {
		Set<String> exNames = new HashSet<>();
		List<Map<String, String>> values = new ArrayList<>();
		Collections.addAll(exNames, names);
		for (int i = 0; i < results.getLength(); i++) {
			NodeList bindings = ((Element) results.item(i)).getElementsByTagName("binding");
			Map<String, String> value = new HashMap<>();
			for (int j = 0; j < bindings.getLength(); j++) {
				Element binding = (Element) bindings.item(j);
				String var = binding.getAttribute("name");
				if (exNames.contains(var)) {
					String uri = SparqlExecutor.getTagValue("uri", binding);
					if (!(uri == null))
						value.put(var, FreebaseInfo.uri2id(uri));
				}

			}
			if (value.size() == exNames.size())
			values.add(value);
		}
		return values;
		
	}
	
	public static List<RelationInfo>  extractRelationFromXml(String xml) {
		NodeList results = PopularityCache.extractResultFromXML(xml);
		List<RelationInfo> relationInfos = new ArrayList<>();
		List<Map<String, String>> res = extractNamesFromResults(results, relationNames);
		for (Map<String, String> entry: res) {
			relationInfos.add(new RelationInfo(entry.get("domainType"), entry.get("expectedType"), entry.get("p")));
		}
		return relationInfos;
	}
	
	public static List<RelationInfo>  extractMediatorRelationFromXml(String xml) {
		NodeList results = PopularityCache.extractResultFromXML(xml);
		List<RelationInfo> relationInfos = new ArrayList<>();
		List<Map<String, String>> res = extractNamesFromResults(results, mediatorNames);
		for (Map<String, String> entry: res) {
			relationInfos.add(new RelationInfo(entry.get("domainType"), entry.get("expectedType"), entry.get("p1"), entry.get("p2")));
		}
		return relationInfos;
	}
	
	public List<RelationInfo> searchRelation(Executor executor) {
		List<RelationInfo> res = new ArrayList<>();
		
		String query = constructQuery(false);
//		System.out.print("query:" + query);
		ServerResponse response = executor.execute(query);
//		System.out.println("********");
		res.addAll(extractRelationFromXml(response.getXml()));
		
		query = constructQuery(true);
//		System.out.print("query:" + query);
		response = executor.execute(query);
//		System.out.println("********");
		res.addAll(extractMediatorRelationFromXml(response.getXml()));
		return res;
		/*
		else {
			String query = constructQuery(false);			
			ServerResponse response = executor.execute(query);
			NodeList results = PopularityCache.extractResultFromXML(response.getXml());
			List<Pair<String, String>> res = new ArrayList<>();
			String count = extractCount(results);
			res.add(Pair.newPair(count, count));
			return res;
		}
		*/
	}
	
	public int searchTypeRelation(Executor executor) {
		String query = constructTypeQuery();
		ServerResponse response = executor.execute(query);
		NodeList results = PopularityCache.extractResultFromXML(response.getXml());
		String count = extractCount(results);
		int countValue = Integer.parseInt(count);
		return countValue;
	}
	
	public static void main(String[] argv) {
//		RelationSearcher searcher = new RelationSearcher("fb:en.jack_morris", "fb:m.06rr6mm");
//		RelationSearcher searcher = new RelationSearcher("fb:m.06rr6mm", "fb:en.jack_morris");
		RelationSearcher searcher = new RelationSearcher("fb:en.barack_obama", "fb:en.democrat_party");
//		RelationSearcher searcher = new RelationSearcher( "fb:en.democrat_party", "fb:en.barack_obama");
		List<RelationInfo> res = searcher.searchRelation(new SparqlExecutor());
		for (RelationInfo info: res) {
			System.out.println(info);
		}
	}
}
