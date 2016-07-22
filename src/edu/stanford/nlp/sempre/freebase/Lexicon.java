package edu.stanford.nlp.sempre.freebase;

import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.cache.StringCache;
import edu.stanford.nlp.sempre.cache.StringCacheUtils;
import edu.stanford.nlp.sempre.freebase.lexicons.LexicalEntry;
import edu.stanford.nlp.sempre.freebase.lexicons.LexicalEntry.LexicalEntrySerializer;
import fig.basic.LispTree;
import fig.basic.LogInfo;
import fig.basic.Option;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Lexicon {
  public static class Options {
    @Option(gloss = "The path for the cache")
    public String cachePath;
    @Option public boolean use_entity_map = true;
    @Option public boolean entity_expansion = true;
  }
  public static Options opts = new Options();

  private static Lexicon lexicon;
  public static Lexicon getSingleton() {
    try {
      if (lexicon == null)
        lexicon = new Lexicon();
      return lexicon;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public StringCache cache;

  private EntityLexicon entityLexicon;
  private UnaryLexicon unaryLexicon;
  private BinaryLexicon binaryLexicon;

  public EntityLexicon getEntityLexicon() { return entityLexicon; }

  private Lexicon() throws IOException {
    LogInfo.begin_track("Lexicon()");
    // TODO(joberant): why is BinaryLexicon special? -- wait why is it special?
    entityLexicon = EntityLexicon.getInstance();
    unaryLexicon = UnaryLexicon.getInstance();
    binaryLexicon = BinaryLexicon.getInstance();
    LogInfo.end_track();

    if (opts.cachePath != null)
      cache = StringCacheUtils.create(opts.cachePath);
  }

  public List<? extends LexicalEntry> lookupUnaryPredicates(String query) throws IOException {
    return unaryLexicon.lookupEntries(query);
  }

  public List<? extends LexicalEntry> lookupBinaryPredicates(String query) throws IOException  {
    return binaryLexicon.lookupEntries(query);
  }

  public List<? extends LexicalEntry> lookupEntities(String query, EntityLexicon.SearchStrategy strategy, Example ex) throws IOException, ParseException {
	String ori_query = query;
	if (opts.use_entity_map) {
		if (ex.entity_map != null && ex.entity_map.containsKey(query))
			query = ex.entity_map.get(query);
		else if (opts.entity_expansion) {
			query = ex.expandQuery(query);
			if (query == null) {
				query = ori_query;
				ex.addQuery(query);
			} else {
				LogInfo.logs("expand query[%s] to [%s]", ori_query, query);
				if (ex.entity_map != null && ex.entity_map.containsKey(query))
					query = ex.entity_map.get(query);
			}
				
				
		}
	}
    List<? extends LexicalEntry> entries = getCache("entity", query);
    if (entries == null) {
//    	LogInfo.logs("lookup Entity [%s] for query [%s]", query, ori_query);
      putCache("entity", query, entries = entityLexicon.lookupEntries(query, strategy, ori_query));
      LogInfo.logs("lookup Entity [%s] for query [%s], ret num = %d", query, ori_query, entries.size());
    }
    return entries; 
  }
  
  public List<? extends LexicalEntry> lookupConcepts(String query, EntityLexicon.SearchStrategy strategy, Example ex)  throws IOException, ParseException {
	  List<? extends LexicalEntry> entries = entityLexicon.lookupConcepts(query, strategy);
	  return entries;
  }

  private List<LexicalEntry> getCache(String mode, String query) {
    if (cache == null) return null;
    String key = mode + ":" + query;
    String response;
    synchronized (cache) {
      response = cache.get(key);
    }
    if (response == null) return null;
    LispTree tree = LispTree.proto.parseFromString(response);
    List<LexicalEntry> entries = new ArrayList<>();
    for (int i = 0; i < tree.children.size(); i++)
      entries.add(LexicalEntrySerializer.entryFromLispTree(tree.child(i)));
    return entries;
  }

  private void putCache(String mode, String query, List<? extends LexicalEntry> entries) {
    if (cache == null) return;
    String key = mode + ":" + query;
    LispTree result = LispTree.proto.newList();
    for (LexicalEntry entry : entries)
      result.addChild(LexicalEntrySerializer.entryToLispTree(entry));
    synchronized (cache) {
      cache.put(key, result.toString());
    }
  }
}
