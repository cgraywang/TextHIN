package edu.stanford.nlp.sempre.freebase;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.cache.StringCache;
import edu.stanford.nlp.sempre.cache.StringCacheUtils;
import edu.stanford.nlp.sempre.freebase.index.FbEntitySearcher;
import edu.stanford.nlp.sempre.freebase.index.FbIndexField;
import edu.stanford.nlp.sempre.freebase.lexicons.EntrySource;
import edu.stanford.nlp.sempre.freebase.lexicons.LexicalEntry;
import edu.stanford.nlp.sempre.freebase.lexicons.LexicalEntry.EntityLexicalEntry;
import edu.stanford.nlp.sempre.freebase.lexicons.TokenLevelMatchFeatures;
import edu.stanford.nlp.sempre.freebase.utils.FileUtils;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.ArrayUtils;
import edu.stanford.nlp.util.StringUtils;
import fig.basic.MapUtils;
import fig.basic.Option;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;



import java.io.IOException;
import java.util.*;

public final class EntityLexicon {
  public enum SearchStrategy { exact, inexact, fbsearch }

  public static class Options {
    @Option(gloss = "Verbosity")
    public int verbose = 0;
    @Option(gloss = "Number of results return by the lexicon")
    public int maxEntries = 5;
    @Option int maxConcepts = 5;
    @Option(gloss = "Number of documents queried from Lucene")
    public int numOfDocs = 1000;
    @Option(gloss = "Path to the exact match lucene index directory")
    public String exactMatchIndex;
    @Option(gloss = "Path to the inexact match lucene index directory")
    public String inexactMatchIndex = "lib/lucene/4.4/inexact";
    @Option(gloss = "Cache path to the mid-to-id path")
    public String mid2idPath;
    @Option(gloss = "Path to entity popularity file")
    public String entityPopularityPath;
    @Option(gloss = "Path to valid Freebase domain file")
    public String freebaseDomainPath = null;
    @Option(gloss = "whether to use IDlevel in sort")
    public boolean useIDLevel = true;
    @Option(gloss = "whether delete unvalid domain")
    public boolean domainLimit = true;
    @Option(gloss = "whether expand by entityAlias")
    public boolean aliasExpansion = true;
    @Option(gloss = "number of entity from EntityAlias")
    public int maxAliasNum = 3;
  }

  public static Options opts = new Options();

  private static EntityLexicon entityLexicon;
  public static EntityLexicon getInstance() {
    if (entityLexicon == null) entityLexicon = new EntityLexicon();
    return entityLexicon;
  }

  FbEntitySearcher exactSearcher;  // Lucene
  FbEntitySearcher inexactSearcher;  // Lucene

  FreebaseSearch freebaseSearch;  // Google's API
  StringCache mid2idCache;  // Google's API spits back mids, which we need to convert to ids
  Map<String, Double> entityPopularityMap;
  static Set<String> validDomains = null;
  static EntityAlias entityAlias = null;
  static Set<String> validTypes = null;

  private EntityLexicon() {
    loadEntityPopularity();
    loadFreebaseDomain();
    entityAlias = EntityAlias.getSingleton();
  }

  public List<EntityLexicalEntry> lookupEntries(String query, SearchStrategy strategy, String ori_query) throws ParseException, IOException {
    if (strategy == null)
      throw new RuntimeException("No entity search strategy specified");
    switch (strategy) {
      case exact:
        if (exactSearcher == null) exactSearcher = new FbEntitySearcher(opts.exactMatchIndex, opts.numOfDocs, "exact");
        return lookupEntries(exactSearcher, query, ori_query);
      case inexact:
        if (inexactSearcher == null) inexactSearcher = new FbEntitySearcher(opts.inexactMatchIndex, opts.numOfDocs, "inexact");
        return lookupEntries(inexactSearcher, query, ori_query);
      case fbsearch:
        if (freebaseSearch == null) freebaseSearch = new FreebaseSearch();
        if (mid2idCache == null) mid2idCache = StringCacheUtils.create(opts.mid2idPath);
        return lookupFreebaseSearchEntities(query);
      default:
        throw new RuntimeException("Unknown entity search strategy: " + strategy);
    }
  }
  
  public List<EntityLexicalEntry> lookupConcepts(String query, SearchStrategy strategy) throws ParseException, IOException {
	  if (strategy == null)
		  throw new RuntimeException("No entity search strategy specified");
	  switch (strategy) {
	    case inexact:
		  if (inexactSearcher == null) inexactSearcher = new FbEntitySearcher(opts.inexactMatchIndex, opts.numOfDocs, "inexact");
		  return lookupConcepts(inexactSearcher, query);
	    default:
	      throw new RuntimeException("Unknown entity search strategy: " + strategy);
	  }
  }

  private void loadEntityPopularity() {
    entityPopularityMap = new HashMap<>();
    if (opts.entityPopularityPath == null) return;
    for (String line : IOUtils.readLines(opts.entityPopularityPath)) {
      String[] tokens = line.split("\t");
      entityPopularityMap.put(tokens[0], Double.parseDouble(tokens[1]));
    }
  }
  
  private void loadFreebaseDomain() {
	  if (opts.freebaseDomainPath == null) {
		  return;
	  }
	  validDomains = new HashSet<String>();
	  validTypes = new HashSet<>();
	  for (String line: IOUtils.readLines(opts.freebaseDomainPath)) {
		  String domain = line.trim();
		  if (domain.startsWith("#"))
			  continue;
		  if (domain.equals(""))
			  continue;
		  if (domain.startsWith("!"))
			  validTypes.add(domain.substring(1));
		  else
			  validDomains.add(domain);
	  }
  }
  
  public static boolean validDomain(String type) {
	  if (validDomains == null)
		  return true;
	  if (validTypes.contains(type))
		  return true;
	  String domain = getDomain(type);
	  String[] tokens = type.split("\\.");
	  if (tokens.length <= 1)
		  return false;
	  return validDomains.contains(domain);
  }
  
  private static String join(String[] tokens, int st, int ed, String joiner) {
	  StringBuffer ret = new StringBuffer();
	  for (int i = st; i <= ed; i++) {
		  if (i > st)
			  ret.append(joiner);
		  ret.append(tokens[i]);
	  }
	  return ret.toString();
  }
  
  public static String getDomain(String uri) {
	  if (uri.startsWith("fb:")) {
		  uri = uri.substring(3);
	  }
	  String[] tokens = uri.split("\\.");
	  String domain;
	  if (uri.startsWith("user")) {
		  domain = join(tokens, 0, 2, ".");
	  } else
		  domain = join(tokens, 0, 0, ".");
      return domain;
  }
  
  public static String getType(String uri) {
	  if (uri.startsWith("fb:"))
		  uri = uri.substring(3);
	  String[] tokens = uri.split("\\.");
	  String type;
	  if (uri.startsWith("user")) {
		  type = join(tokens, 0, 3, ".");
	  } else
		  type = join(tokens, 0, 1, ".");
      return type;
  }
  
  public static String[] parsingTypes(String semType) {
	  if (semType.startsWith("(union")) {
		  String union_types = semType.substring(7, semType.length() - 1);
		  String[] types = union_types.split(" ");
		  for (int i = 0; i < types.length; i++)
			  types[i] = getType(types[i]);
		  return types;
	  } else {
		  String[] types = new String[1];
		  types[0] = getType(semType);
		  return types;
	  }
		  
  }

  public List<EntityLexicalEntry> lookupFreebaseSearchEntities(String query) {
    FreebaseSearch.ServerResponse response = freebaseSearch.lookup(query);
    List<EntityLexicalEntry> entities = new ArrayList<>();
    if (response.error != null) {
      throw new RuntimeException(response.error.toString());
    }
    // num of words in query
    int numOfQueryWords = query.split("\\s+").length;
    for (FreebaseSearch.Entry e : response.entries) {
      if (entities.size() >= opts.maxEntries) break;
      // Note: e.id might not be the same one we're using (e.g., fb:en.john_f_kennedy_airport versus fb:en.john_f_kennedy_international_airport),
      // so get the one from our canonical mid2idCache
      String id = mid2idCache.get(e.mid);
      if (id == null) continue;  // Skip if no ID (probably not worth referencing)
      // skip if it is a long phrase that is not an exact match
      if (numOfQueryWords >= 4 && !query.toLowerCase().equals(e.name.toLowerCase())) {
        continue;
      }

      int distance = editDistance(query.toLowerCase(), e.name.toLowerCase());  // Is this actually useful?
      Counter<String> entityFeatures = TokenLevelMatchFeatures.extractFeatures(query, e.name);
      double popularity = MapUtils.get(entityPopularityMap, id, 0d);
      entityFeatures.incrementCount("text_popularity", Math.log(popularity + 1));
      entities.add(new EntityLexicalEntry(query, query, Collections.singleton(e.name),
              new ValueFormula<>(new NameValue(id, e.name)), EntrySource.FBSEARCH, e.score, distance,
              new FreebaseTypeLookup().getEntityTypes(id), entityFeatures));
    }
    return entities;
  }
  
  public List<EntityLexicalEntry> expandByAlias(String query, int topk) {
	  List<EntityLexicalEntry> res= new ArrayList<>();
	  query = query.toUpperCase();
	  List<EntityInfo> entityInfos = entityAlias.getEntityInfo(query, topk);
	  if (entityInfos == null)
		  return res;
	  for (EntityInfo info:entityInfos) {
		  String[] fbDescriptions = new String[1];
		  fbDescriptions[0] = info.name;
		  Formula formula = Formula.fromString(info.id);
	      String ID = info.id;
	      int IDLevel = 0;
	      if (opts.useIDLevel && ID.startsWith("fb:en"))
	    	  IDLevel = 1;
	      Set<String> types = new HashSet<>();
	      if (opts.domainLimit) {
	    	  for (String token: info.types) {
	    		  if (validDomain(token)) {
	    			  types.add(token);
	    		  }	
	    	  }
	       } else {
	          Collections.addAll(types, info.types);
	       }
      	   double popularity = info.popularity;
      	   int distance = 0;
      	   Counter<String> tokenEditDistanceFeatures = TokenLevelMatchFeatures.extractFeatures(query.toLowerCase(), fbDescriptions[0]);
      	   res.add(new EntityLexicalEntry(query.toLowerCase(), query.toLowerCase(), ArrayUtils.asSet(fbDescriptions), formula, EntrySource.ENTITYALIAS, popularity, distance, types, tokenEditDistanceFeatures, IDLevel));
	  }
	  return res;
	  
  }

  public List<EntityLexicalEntry> lookupEntries(FbEntitySearcher searcher, String textDesc, String ori_query) throws ParseException, IOException {

    List<EntityLexicalEntry> res = new ArrayList<>();
    textDesc = textDesc.replaceAll("\\?", "\\\\?").toLowerCase();
    textDesc = textDesc.replace("/", "\\/");
    List<Document> docs = searcher.searchDocs(textDesc);
    for (Document doc : docs) {
      Formula formula = Formula.fromString(doc.get(FbIndexField.ID.fieldName()));
      String ID = doc.get(FbIndexField.ID.fieldName());
      int IDLevel = 0;
      if (opts.useIDLevel && ID.startsWith("fb:en"))
    	  IDLevel = 1;
      String[] fbDescriptions = new String[]{doc.get(FbIndexField.TEXT.fieldName())};
      String typesDesc = doc.get(FbIndexField.TYPES.fieldName());

      Set<String> types = new HashSet<>();
      if (typesDesc != null) {
        String[] tokens = typesDesc.split(",");
        if (opts.domainLimit) {
        	for (String token: tokens) {
        		if (validDomain(token)) {
        			types.add(token);
        		}	
        	}
        } else
        	Collections.addAll(types, tokens);
      }

      double popularity = Double.parseDouble(doc.get(FbIndexField.POPULARITY.fieldName()));
      int distance = editDistance(textDesc.toLowerCase(), fbDescriptions[0].toLowerCase());
      Counter<String> tokenEditDistanceFeatures = TokenLevelMatchFeatures.extractFeatures(textDesc, fbDescriptions[0]);

      if ((popularity > 0 || distance == 0) && TokenLevelMatchFeatures.diffSetSize(textDesc, fbDescriptions[0]) < 4) {
        res.add(new EntityLexicalEntry(textDesc, textDesc, ArrayUtils.asSet(fbDescriptions), formula, EntrySource.LUCENE, popularity, distance, types, tokenEditDistanceFeatures, IDLevel));  
      } else if (TokenLevelMatchFeatures.diffSetSize(textDesc, fbDescriptions[0]) <= 1) {
    	res.add(new EntityLexicalEntry(textDesc, textDesc, ArrayUtils.asSet(fbDescriptions), formula, EntrySource.LUCENE, popularity, distance, types, tokenEditDistanceFeatures, IDLevel));
      }
    }
    Collections.sort(res, new LexicalEntryComparator());
    res = res.subList(0, Math.min(res.size(), opts.maxEntries));
    if (opts.aliasExpansion) {
    	List<EntityLexicalEntry> aliasRes = expandByAlias(ori_query, opts.maxAliasNum);
    	if (aliasRes.size() > 0) {
    		List<EntityLexicalEntry> new_res = new ArrayList<>();
    		new_res.addAll(aliasRes);
    		new_res.addAll(res.subList(0, Math.min(opts.maxEntries - opts.maxAliasNum, res.size())));
    		res = new_res;
    	}
    }
    if (opts.domainLimit) {
    	List<EntityLexicalEntry> new_res = new ArrayList<>();
    	for (EntityLexicalEntry entry: res) {
    		if (entry.types.size() > 0)
    			new_res.add(entry);
    	}
    	res = new_res;
    }
    return res;
  }
  
  public List<EntityLexicalEntry> lookupConcepts(FbEntitySearcher searcher, String textDesc) throws ParseException, IOException {
    List<EntityLexicalEntry> res = new ArrayList<>();
    textDesc = textDesc.replaceAll("\\?", "\\\\?").toLowerCase();
    List<Document> docs = searcher.searchDocs(textDesc);
    for (Document doc : docs) {

      Formula formula = Formula.fromString(doc.get(FbIndexField.ID.fieldName()));
      String ID = doc.get(FbIndexField.ID.fieldName());
      int IDLevel = 0;
      if (opts.useIDLevel && ID.startsWith("fb:en"))
    	  IDLevel = 1;
      else
    	  continue;
      String[] fbDescriptions = new String[]{doc.get(FbIndexField.TEXT.fieldName())};
      String typesDesc = doc.get(FbIndexField.TYPES.fieldName());

      Set<String> types = new HashSet<>();
      if (typesDesc != null) {
        String[] tokens = typesDesc.split(",");
        Collections.addAll(types, tokens);
      }

      double popularity = Double.parseDouble(doc.get(FbIndexField.POPULARITY.fieldName()));

      
      int distance = editDistance(textDesc.toLowerCase(), fbDescriptions[0].toLowerCase());
      int tokenDistance = TokenLevelMatchFeatures.diffSetSize(textDesc, fbDescriptions[0]);
      if (tokenDistance >= 2 || popularity <= 0.0) 
    	  continue;
      Counter<String> tokenEditDistanceFeatures = TokenLevelMatchFeatures.extractFeatures(textDesc, fbDescriptions[0]);
      res.add(new EntityLexicalEntry(textDesc, textDesc, ArrayUtils.asSet(fbDescriptions), formula, EntrySource.LUCENE, popularity, distance, types, tokenEditDistanceFeatures, IDLevel));  
    }
    Collections.sort(res, new LexicalEntryComparator());
    return res.subList(0, Math.min(res.size(), opts.maxConcepts)); 
  }

  private int editDistance(String query, String name) {

    String[] queryTokens = FileUtils.omitPunct(query).split("\\s+");
    String[] nameTokens = FileUtils.omitPunct(name).split("\\s+");

    StringBuilder querySb = new StringBuilder();
    for (String queryToken : queryTokens)
      querySb.append(queryToken).append(" ");

    StringBuilder nameSb = new StringBuilder();
    for (String nameToken : nameTokens)
      nameSb.append(nameToken).append(" ");

    return StringUtils.editDistance(querySb.toString().trim(), nameSb.toString().trim());
  }

  public static class LexicalEntryComparator implements Comparator<LexicalEntry> {
    @Override
    public int compare(LexicalEntry arg0, LexicalEntry arg1) {

      if (arg0.popularity > arg1.popularity)
        return -1;
      if (arg0.popularity < arg1.popularity)
        return 1;
      if (arg0.idLevel > arg1.idLevel)
    	  return -1;
      if (arg0.idLevel < arg1.idLevel)
    	  return 1;
      if (arg0.distance < arg1.distance)
        return -1;
      if (arg0.distance > arg1.distance)
        return 1;
      return 0;
    }
  }
}


