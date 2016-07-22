package edu.stanford.nlp.sempre.cache;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.stanford.nlp.sempre.freebase.EntityLexicon;
import edu.stanford.nlp.sempre.freebase.FreebaseInfo;
import edu.stanford.nlp.sempre.freebase.SparqlExecutor;
import edu.stanford.nlp.sempre.freebase.FreebaseInfo.Options;
import fig.basic.IOUtils;
import fig.basic.Option;

/**
 * @author Haoran Li
 */

public class PopularityCache {
	Map<String, Map<String, Integer>> domainPopularity;
	Map<String, Map<String, Integer>> typePopularity;
	
	public PopularityCache() {
		domainPopularity = new HashMap<String, Map<String, Integer>>();
		typePopularity = new HashMap<String, Map<String, Integer>>();
	}
	
    public static class Options {
	    @Option(gloss = "which corpus to process")
	    public String corpus = "GCAT";
//	    public String corpus = "20NG";
	    
	    @Option(gloss = "sports domain to adjust")
	    public String[] sportsDomains = {"baseball", "ice_hockey"};
	    @Option(gloss = "GCAT sports domain to ajust")
	    public String[] GCATSportsDomains = {"skiing","cricket","basketball","baseball","ice_hockey","olympics","soccer","american_football","boxing","tennis"};
    }





	
	public static Options opts = new Options();
	
	public void adjustPopularity(Map<String, Integer> map) {
		if (opts.corpus.equals("20NG")) {
			if (!map.containsKey("sports"))
				return;
			int sportsPop = map.get("sports");
			for (String upperDomain: opts.sportsDomains) {
				if (!map.containsKey(upperDomain))
					map.put(upperDomain, 0);
				map.put(upperDomain, map.get(upperDomain) + sportsPop);
			}
		}
		if (opts.corpus.equals("GCAT")) {
			for (String lowerDomain: opts.GCATSportsDomains) {
				if (!map.containsKey(lowerDomain))
					continue;
				if (!map.containsKey("sports"))
					map.put("sports", 0);
				int lowerDomainPop = map.get(lowerDomain);
				map.put("sports", map.get("sports") + lowerDomainPop * 2);
			}
		}
	}
	
	public void add(String entity, String xml) {
		Map<String, Integer> mapValue = extractValueFromXML(xml);
		Map<String, Integer> domainPop = new HashMap<>(), typePop = new HashMap<>();
		for (Map.Entry<String, Integer> entry:mapValue.entrySet()) {
			String property = entry.getKey();
			int count = entry.getValue();
			String domain = EntityLexicon.getDomain(property);
			String type = EntityLexicon.getType(property);
			if (!domainPop.containsKey(domain)) {
				domainPop.put(domain, 0);
			}
			domainPop.put(domain, domainPop.get(domain) + count);
			if (!typePop.containsKey(type)) {
				typePop.put(type, 0);
			}
			typePop.put(type, typePop.get(type) + count);
		}
		adjustPopularity(domainPop);
		this.domainPopularity.put(entity, domainPop);
		this.typePopularity.put(entity, typePop);
		
	}
	
	public boolean has(String entity) {
		return domainPopularity.containsKey(entity);
	}
	
	public double getDomainScore(String entity, String Domain) {
		Map<String, Integer> domainMap = domainPopularity.get(entity);
		Integer score = domainMap.get(Domain);
		if (score == null)
			return 0;
		else
			return score;
	}
	
	public double getTypeScore(String entity, String type) {
		Map<String, Integer> typeMap = typePopularity.get(entity);
		Integer score = typeMap.get(type);
		if (score == null)
			return 0;
		else
			return score;
	}
	
	
	
	public static String constructQuery(String entity) {
		String query = String.format("PREFIX fb: <http://rdf.freebase.com/ns/> SELECT ?p count(?p) AS  ?count WHERE {  %s ?p ?o. } group by ?p ",
									entity);
		return query;
	}
	
	public static NodeList extractResultFromXML(String xml) {
		// trick
		xml = xml.replaceAll("&#[0-9]{1,4};", " ");
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	    NodeList results = null;
	    try {
	      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	      Document doc = docBuilder.parse(new InputSource(new StringReader(xml)));
	      results = doc.getElementsByTagName("result");
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    } catch (SAXException e) {
	      throw new RuntimeException(e);
	    } catch (ParserConfigurationException e) {
	      throw new RuntimeException(e);
	    }
	    return results;
	}
	
	public static Map<String, Integer> extractMapValue(NodeList results) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < results.getLength(); i++) {
			NodeList bindings = ((Element) results.item(i)).getElementsByTagName("binding");
			String property = null;
			int count = 0;
			for (int j = 0; j < bindings.getLength(); j++) {
				Element binding = (Element) bindings.item(j);
				String var = binding.getAttribute("name");
				if (var.equals("p") || var.equals("type")) {
					String uri = SparqlExecutor.getTagValue("uri", binding);
					property = FreebaseInfo.uri2id(uri);
				}
				if (var.equals("count")) {
					count = Integer.parseInt(SparqlExecutor.getTagValue("literal", binding));
				}
			}
			map.put(property, count);
		}
		return map;
	}
	
	public static Map<String, Integer> extractValueFromXML(String xml) {
		Map<String, Integer> values;
		NodeList results = extractResultFromXML(xml);
		values = extractMapValue(results);
		return values;
	}
	
	public static void main(String[] argv) throws IOException {
		StringBuffer xml = new StringBuffer("");
		for (String line: IOUtils.readLines("data/test/sparql.xml")) {
			xml.append(line);
			xml.append("\n");
		}
		PopularityCache cache = new PopularityCache();
		cache.add("test", xml.toString());
		System.out.println(cache.domainPopularity.get("test"));
		System.out.println(cache.typePopularity.get("test"));
		
	}
}
