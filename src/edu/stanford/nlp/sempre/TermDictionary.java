package edu.stanford.nlp.sempre;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import edu.stanford.nlp.sempre.Master.Options;
import fig.basic.IOUtils;
import fig.basic.Option;

public class TermDictionary {
	public static class Options {
	    @Option(gloss = "where to load entity dictionary")
	    public String[] medicineDictPath = {"lib/dictionary/medical.terms.dict"};
	    @Option(gloss = "where to load abbreviation dictionary")
	    public String[] computerAbbreviationDictPath = {"lib/dictionary/computer.abbr.dict"};
	    @Option public String[] computerDictPath = {"lib/dictionary/computer.terms.dict", "lib/dictionary/wikiCompany.txt.termsc"};
	}
	public static Options opts = new Options();
	
	private static TermDictionary medicineDict;
	private static TermDictionary computerDict;
	
	public static TermDictionary getDict(String dictname) {
		switch (dictname) {
	      case "medicine":
	    	  if (medicineDict == null)
	    		  medicineDict = new TermDictionary(opts.medicineDictPath, null);
	    	  return medicineDict;
	      case "computer":
	    	  if (computerDict == null)
	    		  computerDict = new TermDictionary(opts.computerDictPath, opts.computerAbbreviationDictPath);
	    	  return computerDict;
	      default:	        
	          throw new RuntimeException("Error dict name: " + dictname);

	    }
	}
	
	
	
	Set<String> terms;
	Map<String, String> abbreviationDict;
	private TermDictionary(String[] termPaths, String[] expansionPaths) {
		if (termPaths != null) {
			terms = new HashSet<>();		
			for (String path: termPaths) {
				loadTermDict(path);
			}
		}
		if (expansionPaths != null) {
			abbreviationDict = new HashMap<String, String>();
			for (String path: expansionPaths) {
				loadAbbreviationDict(path);
			}
		}
	}
	
	private void loadTermDict(String path) {
		for (String line:IOUtils.readLinesHard(path)) {
			terms.add(line.trim());
		}
	}
	
	private void loadAbbreviationDict(String path) {
		for (String line:IOUtils.readLinesHard(path)) {
			String[] arr = line.trim().split(" \\|\\|\\| ");
			abbreviationDict.put(arr[0], arr[1]);
			terms.add(arr[0]);
		}
	}
	
	public boolean has(String term) {
		return terms.contains(term);
	}
	
	public boolean isAbbreviation(String phrase) {
		return abbreviationDict.containsKey(phrase);
	}

	public String getExpansion(String phrase) {
		return abbreviationDict.get(phrase);
	}
	
}