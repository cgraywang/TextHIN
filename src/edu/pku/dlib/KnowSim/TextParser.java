package edu.pku.dlib.KnowSim;


import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sempre.Builder;
import edu.stanford.nlp.sempre.ChartParserState;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.Formula;
import edu.stanford.nlp.sempre.FuncSemType;
import edu.stanford.nlp.sempre.freebase.SparqlExecutor.ServerResponse;
import edu.stanford.nlp.util.CoreMap;
import fig.basic.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * entityLexcionForFile(inputFile, outputFile, tokenOutputFile) parsing the text of inFile, 
 * write semantic parsing result to outputFile,
 * write tokens of text to tokenOutputFile.
 * @author Haoran Li
 * 
 */
public class TextParser {
	public static class Options {
		@Option(gloss = "Where to load stopwords")
		public String stopWordsFile = "lib/data/stopwords_lemmatized.txt";
	}

	public static Options opts = new Options();

	

	private Builder builder;
	private EntityLinkingResult linking_result;
	public static StanfordCoreNLP pipeline = null;
	public EntityAlias entityAlias;
	public TermDictionary medicineDict, computerDict;
	Set<String> stopWords;

	public TextParser(Builder builder) {
		this.builder = builder;
	}


	
	static public void init() {
		endPuncs = new HashSet<>();
		String[] tempPuncs = {":", ".", "!", "?"};
		endPuncs.addAll(Arrays.asList(tempPuncs));
	}
	
	public void load() {
		init();
		this.linking_result = EntityLinkingResult.getSingleton();
		this.entityAlias = EntityAlias.getSingleton();
		this.medicineDict = TermDictionary.getDict("medicine");
		this.computerDict = TermDictionary.getDict("computer");
		loadStopWords();
	}
	
	public void loadStopWords(){
		stopWords = new HashSet<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(opts.stopWordsFile));
			String line;
			while ((line = reader.readLine()) != null) {
				stopWords.add(line.trim().toLowerCase());
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public static void initializeStanfordNLP() {
		if (pipeline == null) {
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit");
			pipeline = new StanfordCoreNLP(props);
		}
	}

	public static List<CoreMap> doAllAnnotation(String text) {
		if (pipeline == null)
			initializeStanfordNLP();
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		return sentences;
	}

	public static List<String> sentenceSplitter(List<CoreMap> sentences) {
		List<String> sentenceList = new ArrayList<String>();
		for (CoreMap sentence : sentences) {
			String sentenceString = "";
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				sentenceString += word + " ";
			}
			sentenceList.add(sentenceString);
		}
		return sentenceList;
	}

	public static List<String> sentenceSplitter(String text) {
		List<CoreMap> sentences = doAllAnnotation(text);
		List<String> sentenceList = sentenceSplitter(sentences);
		return sentenceList;
	}

	public static Example createExample(Map<String, String> entity_map,
			String sentence) {
		Example.Builder b = new Example.Builder();
		b.setUtterance(sentence);
		Example ex = b.createExample();
		ex.preprocess();
		if (entity_map == null)
			ex.entity_map = new HashMap<>();
		else
			ex.entity_map = entity_map;
		return ex;
	}


	public boolean validEntityPosition(Example ex, boolean[] pool, int st, int ed) {
		if (ed == st + 1) {
			String word = ex.lemmaToken(st);
			if (stopWords.contains(word))
				return false;
		}
		// check [st -1, ed] false
		for (int i = st - 1; i <= ed; i++) {
			if (i < 0 || i >= pool.length)
				continue;
			if (pool[i])
				return false;
		}
		return true;
	}

	public void addEntityToPool(boolean[] pool, int st, int ed) {
		// add [st, ed)
		for (int i = st; i < ed; i++)
			pool[i] = true;
	}

	public List<IntPair> search_entities(Example ex) {
		List<IntPair> entities = new ArrayList<IntPair>();

		int numToken = ex.numTokens();
		boolean[] pool = new boolean[numToken];
		for (int i = 0; i < numToken; i++)
			pool[i] = false;
		int st;
		// search entity by abbreviation dict
		st = 0;
		while (st < numToken) {
			int ed = st;
			for (int j = st + 1; j <= numToken; j++) {
				String phrase = ex.oriPhrase(st, j);
				if (this.computerDict.has(phrase))
					ed = j;
			}
			if (ed == st) {
				st++;
				continue;
			} else {
				boolean isVerb = false;
				for (int j = st; j < ed; j++) {
					if (ex.posTag(j).startsWith("VB"))
						isVerb = true;
				}
				if (validEntityPosition(ex, pool, st, ed) && !(isVerb && ed == st + 1)) {
					entities.add(new IntPair(st, ed));
					String phrase = ex.oriPhrase(st, ed);
					String expansion = this.computerDict.getExpansion(phrase);
					
					addEntityToPool(pool, st, ed);
					if (expansion != null)
						ex.addExpansion(phrase, expansion);
				}
				st = ed + 1;
			}
		}

		// search entity by entity linking
		
		st = 0;
		while (st < numToken) {
			int ed = st;
			for (int j = st + 1; j < numToken; j++) {
				String phrase = ex.phrase(st, j);
				if (ex.entity_map.containsKey(phrase))
					ed = j;
			}
			if (ed == st) {
				st++;
				continue;
			} else {
				if (validEntityPosition(ex, pool, st, ed)) {
					entities.add(new IntPair(st, ed));
					addEntityToPool(pool, st, ed);
				}
				st = ed + 1;
			}
		}

		// searcg by entityAlias
		st = 0;
		for (int i = 0; i < numToken; i++) {
			st = i;
			int ed = st + 1;
			String token = ex.oriToken(i);
			if (this.entityAlias.hasAlias(token)) {
				if (validEntityPosition(ex, pool, st, ed)) {
					entities.add(new IntPair(st, ed));
					addEntityToPool(pool, st, ed);
				}
			}
		}

		// search entity by term dictionary

		st = 0;
		while (st < numToken) {
			int ed = st;
			
			for (int j = st + 1; j <= numToken; j++) {
				String phrase = ex.oriPhrase(st, j);
				if (this.medicineDict.has(phrase))
					ed = j;
			}
			if (ed == st) {
				st++;
				continue;
			} else {
				boolean isVerb = false;
				for (int j = st; j < ed; j++) {
					if (ex.posTag(j).startsWith("VB"))
						isVerb = true;
				}
				if (validEntityPosition(ex, pool, st, ed) && !(isVerb && ed == st + 1)) {
					entities.add(new IntPair(st, ed));
					addEntityToPool(pool, st, ed);
				}
				st = ed + 1;
			}
		}

		// search entity by PosTag
		st = 0;
		String[] temp = { "NNP", "NNPS" };
		Set<String> posTags = new HashSet<String>(Arrays.asList(temp));
		while (st < numToken) {
			String posTag = ex.posTag(st);
			if (!posTags.contains(posTag)) {
				st++;
				continue;
			}
			int ed = st + 1;
			for (int j = st + 1; j < numToken; j++) {
				if (posTag.equals(ex.posTag(j))) {
					ed = j + 1;
				} else
					break;
			}
			if (validEntityPosition(ex, pool, st, ed)) {
				entities.add(new IntPair(st, ed));
				addEntityToPool(pool, st, ed);
			}
			st = ed + 1;
		}
		return entities;
	}

	public List<IntPair> searchParsingEntities(ChartParserState state, Example ex) {
		String entityCat = "$Entity";
		List<IntPair> entities = new ArrayList<IntPair>();
		int numToken = ex.numTokens();
		for (int st = 0; st < numToken; st++) {
			for (int ed = st + 1; ed <= numToken; ed++) {
				if (state.chart[st][ed].get(entityCat) != null)
					entities.add(new IntPair(st, ed));
			}
		}
		return entities;
	}

	public List<EntityParsingResult> getParsingResult(List<IntPair> entityPos,
			ChartParserState state, Example ex) {
		List<EntityParsingResult> res = new ArrayList<>();
		for (IntPair position : entityPos) {
			int st = position.st;
			int ed = position.ed;
			Map<String, Double> id2EntityScore = new HashMap<>();
			String phrase = ex.phrase(st, ed);
			for (Derivation deriv : state.chart[st][ed].get("$Entity")) {
				double score = deriv.getScore();
				String id = deriv.formula.toString();
				if (id2EntityScore.containsKey(id)) {
					score += 1000;
				} else
					id2EntityScore.put(id, score);

				String[] types = EntityLexicon.parsingTypes(deriv.type
						.toString());
				for (String type : types) {
					res.add(new EntityParsingResult(st, ed, phrase,
							deriv.formula.toString(), type, score));
				}
			}
		}
		return res;
	}

	public void computeTypePopularity(List<EntityParsingResult> parsingResults,
			 Example ex, PopularityCache popCache) {
		for (EntityParsingResult epResult : parsingResults) {
			String entity = epResult.entity;
			double domainPop, typePop;
			String type = epResult.type;
			String domain = EntityLexicon.getDomain(type);
			if (!popCache.has(entity)) {
				String query = PopularityCache.constructQuery(entity);
				ServerResponse response = this.builder.executor.execute(query);
				popCache.add(entity, response.getXml());
				// System.out.println(response.getXml());
			}
			domainPop = popCache.getDomainScore(entity, domain);
			typePop = popCache.getTypeScore(entity, type);
			epResult.setPopularity(domainPop, typePop);
		}
	}

	static public List<EntityParsingResult> getBestCandidate(List<IntPair> ePostion,
			List<EntityParsingResult> candidates) {
		for (EntityParsingResult resultCand : candidates) {
			resultCand.calcScore();
		}
		Collections.sort(candidates,
				new EntityParsingResult.EntityParsingResultComparator());
		Set<String> choosedPosition = new HashSet<>();
		Set<String> choosedEntity = new HashSet<>();
		List<EntityParsingResult> predRes = new ArrayList<>();
		for (EntityParsingResult resultCand : candidates) {
			String desID = resultCand.st + "#" + resultCand.ed;
			if (choosedPosition.contains(desID))
				continue;
			choosedPosition.add(desID);
			if (choosedEntity.contains(resultCand.entity))
				continue;
			choosedEntity.add(resultCand.entity);
			predRes.add(resultCand);
		}
		
		return predRes;
	}

	public void aggregateRelation(Map<String, Integer> relationCnt,
			List<Pair<String, String>> relations, String e1, String e2) {
		for (Pair<String, String> pair : relations) {
			String type1 = pair.getFirst();
			String type2 = pair.getSecond();
			String id1 = e1 + "#" + EntityLexicon.getType(type1);
			String id2 = e2 + "#" + EntityLexicon.getType(type2);
			if (!relationCnt.containsKey(id1))
				relationCnt.put(id1, 0);
			relationCnt.put(id1, relationCnt.get(id1) + 1);
			if (!relationCnt.containsKey(id2))
				relationCnt.put(id2, 0);
			relationCnt.put(id2, relationCnt.get(id2) + 1);
		}
	}
	
	public List<Derivation> getUniqueRelation(List<Derivation> derivations) {
		List<Derivation> ret = new ArrayList<>();
		Set<Formula> formulaSet = new HashSet<>();
		for (Derivation deriv: derivations) {
			Formula formula = deriv.formula;
			if (formulaSet.contains(formula))
				continue;
			formulaSet.add(formula);
			ret.add(deriv);
		}
		return ret;
		
	}
	
	public void searchRelation(
			List<IntPair> ePosition, List<EntityParsingResult> candidates, 
			ChartParserState state, RelationCache cache) {
		List<Derivation> derivations = state.getSpecifiedCatDerivation("$Binary");
		derivations = getUniqueRelation(derivations);
		
		for (EntityParsingResult epResult1: candidates) {
			for (EntityParsingResult epResult2: candidates) {
				if (epResult1.st == epResult2.st)
					continue;
				if (epResult1.entity.equals(epResult2.entity))
					continue;
				int strongRelationCnt = 0;
				// search strong relation
				for (Derivation relation: derivations) {
					FuncSemType funcType = (FuncSemType) relation.type;
					String argType = EntityLexicon.getType(funcType.argType.toString());
					String retType = EntityLexicon.getType(funcType.retType.toString());
					Formula formula = relation.formula;
					if (!epResult1.type.equals(retType))
						continue;
					if (!epResult2.type.equals(argType))
						continue;
					
					Relation parsedRelation = cache.searchStrongRelation(epResult1.entity,
							epResult2.entity, formula, retType, argType);
					if (parsedRelation != null) {
						epResult1.addRelation(parsedRelation);
						epResult2.addRelation(parsedRelation);			
						strongRelationCnt ++;
					}
				}
				// search weak relation
				if (strongRelationCnt == 0) {
					cache.searchWeakRelation(epResult1.entity, epResult2.entity);
					Relation parsedRelation = cache.getWeakRelation(epResult1.entity, epResult2.entity, epResult1.type, epResult2.type);
					if (parsedRelation != null) {
						epResult1.addRelation(parsedRelation);
						epResult2.addRelation(parsedRelation);
					}
				}
			}
		}
		
		for (EntityParsingResult epResult: candidates) {
			for (Derivation relation: derivations) {
				FuncSemType funcType = (FuncSemType) relation.type;
				String argType = EntityLexicon.getType(funcType.argType.toString());
				String retType = EntityLexicon.getType(funcType.retType.toString());
				Formula formula = relation.formula;
				if (argType.equals(epResult.type) || retType.equals(epResult.type)) {
					 String[] rel = RelationCache.parseRelations(formula);
					 RelationInfo info = RelationCache.properRelation(rel, retType, argType);
					 
					 Relation parsedRelation;
					 if (epResult.type.equals(info.retType)) {
						 parsedRelation = new Relation(info, epResult.entity, "TBD", 3);
					 } else
						 parsedRelation = new Relation(info, "TBD", epResult.entity, 3);
					 epResult.addRelation(parsedRelation);
				}
			}
		}
	}

	
	
	static Set<String> endPuncs;
	static public List<String> tokenLimitedSentenceSplitter(String text) {
		String[] tokens = text.split(" +");
		List<String> sentences = new ArrayList<>();
		StringBuffer sentence = new StringBuffer();
		int tokenNum = 0;
		for (String token: tokens) {
			if (tokenNum > 0)
				sentence.append(' ');
			sentence.append(token);
			tokenNum ++;	
			if (tokenNum > 100 || endPuncs.contains(token)) {
				if (tokenNum > 1)
					sentences.add(sentence.toString());
				sentence = new StringBuffer();
				tokenNum = 0;
			}
		}
		if (tokenNum > 0) {
			sentences.add(sentence.toString());
		}
		return sentences;
	}
	
	static public List<Relation> gatherRelation(List<EntityParsingResult> epResults) {
		List<Relation> relations = new ArrayList<>();
		for (EntityParsingResult epResult:epResults) {
			relations.addAll(epResult.relations);
		}
		Collections.sort(relations, new Relation.RelationComparator());
		return relations;
		
	}
	
	static public int findIndexOfEntity(String entity, List<EntityParsingResult> epResults) {
		if (entity.equals("TBD"))
			return -1;
		for (int i = 0; i < epResults.size(); i++) {
			EntityParsingResult epr = epResults.get(i);
			if (epr.entity.equals(entity))
				return i;
		}
		return -1;
	}
	
	static public void merge(int[] f, int l, int r, int newColor) {
		int group1 = f[l], group2 = f[r];
		for (int i = 0; i < f.length; i++) {
			if (f[i] == group1 || f[i] == group2)
				f[i] = newColor;
		}
	}
	
	static public EntityParsingResult buildRelation(List<EntityParsingResult> epResults) {
		EntityParsingResult ret = new EntityParsingResult();
		int size = epResults.size();
		int[] f = new int[size];
		for (int i = 0; i < size; i++)
			f[i] = i;
		List<Relation> relations = gatherRelation(epResults);
		for (Relation rel: relations) {
			int l = findIndexOfEntity(rel.subj, epResults), r = findIndexOfEntity(rel.obj, epResults);
			int level = rel.level;
			if (level <= 2 && l != -1 && r != -1 && f[l] != f[r]) {
				merge(f, l, r, size++);
				ret.addRelation(rel);
			}
			if (level == 3 && Integer.min(l,r) == -1) {
				int x = Integer.max(l, r);
				if (x >= 0 && f[x] == x) {
					f[x] = size ++;
					ret.addRelation(rel);
				}
			}
		}
		return ret;
				
	}

	// EntityParsingResult are entities except last one which are graph(relations). 
	public List<List<EntityParsingResult>> handleOneDocument(String docid,
			String text, BufferedWriter tokenWriter) throws IOException {
		List<List<EntityParsingResult>> res = new ArrayList<>();
		Map<String, String> entity_map = this.linking_result
				.getEntityMap(docid);
		List<String> sentences = sentenceSplitter(text);
//		List<String> sentences = tokenLimitedSentenceSplitter(text);
		PopularityCache popCache = new PopularityCache();
		RelationCache relationCache = new RelationCache(this.builder.executor);
		for (String sentence : sentences) {

			Example ex = createExample(entity_map, sentence);
			if (ex.numTokens() > 0)
				tokenWriter.write(ex.oriPhrase(0, ex.numTokens()) + " ");
			
			List<IntPair> entities = search_entities(ex);
			ex.fillEntities(entities);
			ChartParserState state = (ChartParserState) builder.parser.parse(
					builder.params, ex);

			List<IntPair> parsingEntities = searchParsingEntities(state, ex);
			int numEntity = parsingEntities.size();

			List<EntityParsingResult> parsingResults = getParsingResult(
					parsingEntities, state, ex);

			List<EntityParsingResult> predResults = null;
			boolean hasCalcTypePopularity = false;
			if (numEntity == 0)
				continue;
			if (!hasCalcTypePopularity) {
				computeTypePopularity(parsingResults, ex, popCache);
				hasCalcTypePopularity = true;
			}
			if (numEntity <= 4) {
				searchRelation(parsingEntities, parsingResults, state, relationCache);
			}
			predResults = getBestCandidate(parsingEntities, parsingResults);
			List<EntityParsingResult> filteredResults = new ArrayList<>();
			for (EntityParsingResult spResult : predResults) {
				if (spResult.score < -1)
					continue;
				filteredResults.add(spResult);
			}
			predResults = filteredResults;
						
			if (predResults.size() > 0) {
				EntityParsingResult graph = buildRelation(predResults);
				graph.phrase = sentence;
				predResults.add(graph);
				res.add(predResults);
				
			}
		}
		return res;
	}
	

	public void entityLexcionForFile(String inputFile, String outputFile, String tokenOutputFile) {
		String line, docid = null;
		int cnt = 0;
		LogInfo.begin_track("Semantic Parsing");
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					outputFile));
			BufferedWriter tokenWriter = new BufferedWriter(new FileWriter(tokenOutputFile));
			while ((line = reader.readLine()) != null) {
				cnt += 1;
				if (cnt % 2 == 1) {
					docid = line.trim();
				} else {
					LogInfo.logs("start processing document [%s]", docid);
					writer.write("doc:" + docid + "\n");
					tokenWriter.write(docid + "\n");
					List<List<EntityParsingResult>> res = handleOneDocument(
							docid, line.trim(), tokenWriter);
					tokenWriter.write("\n");
					for (List<EntityParsingResult> sentenceRes : res) {
						writer.write("----" + "sentence result:" + sentenceRes.get(sentenceRes.size() - 1).phrase + "\n");
						for (EntityParsingResult epResult : sentenceRes) {
							if (epResult.type.equals("relations")) {
								for (Relation relation: epResult.relations) {
									if (relation.isRealRelation())
										writer.write("\t\t" + relation.toString() + "\n");
								}
							} else {
								writer.write("\t" + epResult.compactString() + "\n");
							}
						}
					}
					LogInfo.logs("finish processing document [%s]", docid);
					writer.flush();
				}
			}
			reader.close();
			writer.close();
			tokenWriter.close();
			LogInfo.end_track();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
