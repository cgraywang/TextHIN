package edu.stanford.nlp.sempre;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sempre.cache.PopularityCache;
import edu.stanford.nlp.sempre.cache.RelationCache;
import edu.stanford.nlp.sempre.corenlp.CoreNLPAnalyzer;
import edu.stanford.nlp.sempre.freebase.EntityLexicon;
import edu.stanford.nlp.sempre.freebase.SparqlExecutor.ServerResponse;
import edu.stanford.nlp.util.CoreMap;
import fig.basic.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A Master manages multiple sessions. Currently, they all share the same model,
 * but they need not in the future.
 */
public class Master {
	public static class Options {
		@Option(gloss = "Execute these commands before starting")
		public List<String> scriptPaths = Lists.newArrayList();
		@Option(gloss = "Execute these commands before starting (after scriptPaths)")
		public List<String> commands = Lists.newArrayList();
		@Option(gloss = "Write a log of this session to this path")
		public String logPath;

		@Option(gloss = "Print help on startup")
		public boolean printHelp = true;

		@Option(gloss = "Number of exchanges to keep in the context")
		public int contextMaxExchanges = 0;

		@Option(gloss = "Online update weights on new examples.")
		public boolean onlineLearnExamples = true;
		@Option(gloss = "Write out new examples to this directory")
		public String newExamplesPath;
		@Option(gloss = "Write out new parameters to this directory")
		public String newParamsPath;

		@Option(gloss = "Write out new grammar rules")
		public String newGrammarPath;
		
		@Option(gloss = "Where to load stopwords")
		public String stopWordsFile = "config/stopwords_lemmatized.txt";
	}

	public static Options opts = new Options();

	public class Response {
		// Example that was parsed, if any.
		Example ex;

		// Which derivation we're selecting to show
		int candidateIndex = -1;

		// Detailed information
		List<String> lines = new ArrayList<>();

		public String getFormulaAnswer() {
			if (ex.getPredDerivations().size() == 0)
				return "(no answer)";
			else {
				Derivation deriv = getDerivation();
				return deriv.getFormula() + " => " + deriv.getValue();
			}
		}

		public String getAnswer() {
			if (ex.getPredDerivations().size() == 0)
				return "(no answer)";
			else {
				Derivation deriv = getDerivation();
				deriv.ensureExecuted(builder.executor, ex.context);
				return deriv.getValue().toString();
			}
		}

		public List<String> getLines() {
			return lines;
		}

		public Example getExample() {
			return ex;
		}

		public int getCandidateIndex() {
			return candidateIndex;
		}

		public Derivation getDerivation() {
			return ex.getPredDerivations().get(candidateIndex);
		}
	}

	private Builder builder;
	private Learner learner;
	private EntityLinkingResult linking_result;
	private HashMap<String, Session> sessions = new LinkedHashMap<>();
	public static StanfordCoreNLP pipeline = null;
	public EntityAlias entityAlias;
	public TermDictionary medicineDict, computerDict;

	public Master(Builder builder) {
		this.builder = builder;
		this.learner = new Learner(builder.parser, builder.params,
				new Dataset());

	}

	public Params getParams() {
		return builder.params;
	}

	// Return the unique session identified by session id |id|.
	// Create a new session if one doesn't exist.
	public Session getSession(String id) {
		Session session = sessions.get(id);
		if (session == null) {
			session = new Session(id);
			for (String path : opts.scriptPaths)
				processScript(session, path);
			for (String command : opts.commands)
				processQuery(session, command);
			if (id != null)
				sessions.put(id, session);
		}
		return session;
	}

	void printHelp() {
		LogInfo.log("Enter an utterance to parse or one of the following commands:");
		LogInfo.log("  (help): show this help message");
		LogInfo.log("  (status): prints out status of the system");
		LogInfo.log("  (get |option|): get a command-line option (e.g., (get Parser.verbose))");
		LogInfo.log("  (set |option| |value|): set a command-line option (e.g., (set Parser.verbose 5))");
		LogInfo.log("  (reload): reload the grammar/parameters");
		LogInfo.log("  (grammar): prints out the grammar");
		LogInfo.log("  (params [|file|]): dumps all the model parameters");
		LogInfo.log("  (select |candidate index|): show information about the |index|-th candidate of the last utterance.");
		LogInfo.log("  (accept |candidate index|): record the |index|-th candidate as the correct answer for the last utterance.");
		LogInfo.log("  (answer |answer|): record |answer| as the correct answer for the last utterance (e.g., (answer (list (number 3)))).");
		LogInfo.log("  (rule |lhs| (|rhs_1| ... |rhs_k|) |sem|): adds a rule to the grammar (e.g., (rule $Number ($TOKEN) (NumberFn)))");
		LogInfo.log("  (type |logical form|): perform type inference (e.g., (type (number 3)))");
		LogInfo.log("  (execute |logical form|): execute the logical form (e.g., (execute (call + (number 3) (number 4))))");
		LogInfo.log("  (def |key| |value|): define a macro to replace |key| with |value| in all commands (e.g., (def type fb:type.object type)))");
		LogInfo.log("  (context [(user |user|) (date |date|) (exchange |exchange|) (graph |graph|)]): prints out or set the context");
		LogInfo.log("Press Ctrl-D to exit.");
	}

	public void runInteractivePrompt() {
		Session session = getSession("stdin");

		if (opts.printHelp)
			printHelp();

		while (true) {
			LogInfo.stdout.print("> ");
			LogInfo.stdout.flush();
			String line;
			try {
				line = LogInfo.stdin.readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if (line == null)
				break;

			int indent = LogInfo.getIndLevel();
			try {
				processQuery(session, line);
			} catch (Throwable t) {
				while (LogInfo.getIndLevel() > indent)
					LogInfo.end_track();
				t.printStackTrace();
			}
		}
	}

	// Read LispTrees from |scriptPath| and process each of them.
	public void processScript(Session session, String scriptPath) {
		Iterator<LispTree> it = LispTree.proto.parseFromFile(scriptPath);
		while (it.hasNext()) {
			LispTree tree = it.next();
			processQuery(session, tree.toString());
		}
	}

	// Process user's input |line|
	// Currently, synchronize a very crude level.
	// In the future, refine this.
	// Currently need the synchronization because of writing to stdout.
	public synchronized Response processQuery(Session session, String line) {
		line = line.trim();
		Response response = new Response();

		// Capture log output and put it into response.
		// Hack: modifying a static variable to capture the logging.
		// Make sure we're synchronized!
		StringWriter stringOut = new StringWriter();
		LogInfo.setFileOut(new PrintWriter(stringOut));

		if (line.startsWith("("))
			handleCommand(session, line, response);
		else
			handleUtterance(session, line, response);

		// Clean up
		for (String outLine : stringOut.toString().split("\n"))
			response.lines.add(outLine);
		LogInfo.setFileOut(null);

		// Log interaction to disk
		if (!Strings.isNullOrEmpty(opts.logPath)) {
			PrintWriter out = IOUtils.openOutAppendHard(opts.logPath);
			out.println(Joiner.on("\t").join(
					Lists.newArrayList("date=" + new Date().toString(),
							"sessionId=" + session.id, "remote="
									+ session.remoteHost, "format="
									+ session.format, "query=" + line,
							"response=" + summaryString(response))));
			out.close();
		}

		return response;
	}

	String summaryString(Response response) {
		if (response.getExample() != null)
			return response.getFormulaAnswer();
		if (response.getLines().size() > 0)
			return response.getLines().get(0);
		return null;
	}

	private void handleUtterance(Session session, String query,
			Response response) {
		session.updateContext();

		// Create example
		Example.Builder b = new Example.Builder();
		b.setId("session:" + session.id);
		b.setUtterance(query);
		b.setContext(session.context);
		Example ex = b.createExample();

		ex.preprocess();

		// Parse!
		builder.parser.parse(builder.params, ex, false);

		response.ex = ex;
		ex.log();
		if (ex.predDerivations.size() > 0) {
			response.candidateIndex = 0;
			printDerivation(response.getDerivation());
		}
		session.updateContext(ex, opts.contextMaxExchanges);
	}

	private void printDerivation(Derivation deriv) {
		// Print features
		HashMap<String, Double> featureVector = new HashMap<>();
		deriv.incrementAllFeatureVector(1, featureVector);
		FeatureVector.logFeatureWeights("Pred", featureVector, builder.params);

		// Print choices
		Map<String, Integer> choices = new LinkedHashMap<>();
		deriv.incrementAllChoices(1, choices);
		FeatureVector.logChoices("Pred", choices);

		// Print denotation
		LogInfo.begin_track("Top formula");
		LogInfo.logs("%s", deriv.formula);
		LogInfo.end_track();
		if (deriv.value != null) {
			LogInfo.begin_track("Top value");
			deriv.value.log();
			LogInfo.end_track();
		}
	}

	private void handleCommand(Session session, String line, Response response) {
		LispTree tree = LispTree.proto.parseFromString(line);
		tree = builder.grammar.applyMacros(tree);

		String command = tree.child(0).value;

		if (command == null || command.equals("help")) {
			printHelp();
		} else if (command.equals("status")) {
			LogInfo.begin_track("%d sessions", sessions.size());
			for (Session otherSession : sessions.values())
				LogInfo.log(otherSession
						+ (session == otherSession ? " *" : ""));
			LogInfo.end_track();
			StopWatchSet.logStats();
		} else if (command.equals("reload")) {
			builder.build();
		} else if (command.equals("grammar")) {
			for (Rule rule : builder.grammar.rules)
				LogInfo.logs("%s", rule.toLispTree());
		} else if (command.equals("params")) {
			if (tree.children.size() == 1) {
				builder.params.write(LogInfo.stdout);
				if (LogInfo.getFileOut() != null)
					builder.params.write(LogInfo.getFileOut());
			} else {
				builder.params.write(tree.child(1).value);
			}
		} else if (command.equals("get")) {
			if (tree.children.size() != 2) {
				LogInfo.log("Invalid usage: (get |option|)");
				return;
			}
			String option = tree.child(1).value;
			LogInfo.logs("%s", getOptionsParser().getValue(option));
		} else if (command.equals("set")) {
			if (tree.children.size() != 3) {
				LogInfo.log("Invalid usage: (set |option| |value|)");
				return;
			}
			String option = tree.child(1).value;
			String value = tree.child(2).value;
			if (!getOptionsParser().parse(new String[] { "-" + option, value }))
				LogInfo.log("Unknown option: " + option);
		} else if (command.equals("select") || command.equals("accept")
				|| command.equals("s") || command.equals("a")) {
			// Select an answer
			if (tree.children.size() != 2) {
				LogInfo.logs("Invalid usage: (%s |candidate index|)", command);
				return;
			}

			Example ex = session.getLastExample();
			if (ex == null) {
				LogInfo.log("No examples - please enter a query first.");
				return;
			}
			int index = Integer.parseInt(tree.child(1).value);
			if (index < 0 || index >= ex.predDerivations.size()) {
				LogInfo.log("Candidate index out of range: " + index);
				return;
			}

			response.ex = ex;
			response.candidateIndex = index;
			session.updateContextWithNewAnswer(ex, response.getDerivation());
			printDerivation(response.getDerivation());

			// Add a training example. While the user selects a particular
			// derivation, there are three ways to interpret this signal:
			// 1. This is the correct derivation (Derivation).
			// 2. This is the correct logical form (Formula).
			// 3. This is the correct denotation (Value).
			// Currently:
			// - Parameters based on the denotation.
			// - Grammar rules are induced based on the denotation.
			// We always save the logical form and the denotation (but not the
			// entire
			// derivation) in the example.
			if (command.equals("accept") || command.equals("a")) {
				ex.setTargetFormula(response.getDerivation().getFormula());
				ex.setTargetValue(response.getDerivation().getValue());
				ex.setContext(session.getContextExcludingLast());
				addNewExample(ex);
			}
		} else if (command.equals("answer")) {
			if (tree.children.size() != 2) {
				LogInfo.log("Missing answer.");
			}

			// Set the target value.
			Example ex = session.getLastExample();
			if (ex == null) {
				LogInfo.log("Please enter a query first.");
				return;
			}
			ex.setTargetValue(Values.fromLispTree(tree.child(1)));
			addNewExample(ex);
		} else if (command.equals("rule")) {
			int n = builder.grammar.rules.size();
			builder.grammar.addStatement(tree.toString());
			for (int i = n; i < builder.grammar.rules.size(); i++)
				LogInfo.logs("Added %s", builder.grammar.rules.get(i));
			// Need to update the parser given that the grammar has changed.
			builder.parser = null;
			builder.buildUnspecified();
		} else if (command.equals("type")) {
			LogInfo.logs("%s", TypeInference.inferType(Formulas
					.fromLispTree(tree.child(1))));
		} else if (command.equals("execute")) {
			Example ex = session.getLastExample();
			ContextValue context = (ex != null ? ex.context : session.context);
			Executor.Response execResponse = builder.executor.execute(
					Formulas.fromLispTree(tree.child(1)), context);
			LogInfo.logs("%s", execResponse.value);
		} else if (command.equals("def")) {
			builder.grammar.interpretMacroDef(tree);
		} else if (command.equals("context")) {
			if (tree.children.size() == 1) {
				LogInfo.logs("%s", session.context);
			} else {
				session.context = new ContextValue(tree);
			}
		} else {
			LogInfo.log("Invalid command: " + tree);
		}
	}

	void addNewExample(Example origEx) {
		// Create the new example, but only add relevant information.
		Example ex = new Example.Builder().setId(origEx.id)
				.setUtterance(origEx.utterance).setContext(origEx.context)
				.setTargetFormula(origEx.targetFormula)
				.setTargetValue(origEx.targetValue).createExample();

		if (!Strings.isNullOrEmpty(opts.newExamplesPath)) {
			LogInfo.begin_track("Adding new example");
			Dataset.appendExampleToFile(opts.newExamplesPath, ex);
			LogInfo.end_track();
		}

		if (opts.onlineLearnExamples) {
			LogInfo.begin_track("Updating parameters");
			learner.onlineLearnExample(origEx);
			if (!Strings.isNullOrEmpty(opts.newParamsPath))
				builder.params.write(opts.newParamsPath);
			LogInfo.end_track();
		}
	}

	public static OptionsParser getOptionsParser() {
		OptionsParser parser = new OptionsParser();
		// Dynamically figure out which options we need to load
		// To specify this:
		// java -Dmodules=core,freebase
		List<String> modules = Arrays.asList(System.getProperty("modules",
				"core").split(","));

		// All options are assumed to be of the form <class>opts.
		// Read the module-classes.txt file, which specifies which classes are
		// associated with each module.
		List<Object> args = new ArrayList<Object>();
		for (String line : IOUtils.readLinesHard("module-classes.txt")) {

			// Example: core edu.stanford.nlp.sempre.Grammar
			String[] tokens = line.split(" ");
			if (tokens.length != 2)
				throw new RuntimeException("Invalid: " + line);
			String module = tokens[0];
			String className = tokens[1];
			if (!modules.contains(tokens[0]))
				continue;

			// Group (e.g., Grammar)
			String[] classNameTokens = className.split("\\.");
			String group = classNameTokens[classNameTokens.length - 1];

			// Object (e.g., Grammar.opts)
			Object opts = null;
			try {
				for (Field field : Class.forName(className).getDeclaredFields()) {
					if (!"opts".equals(field.getName()))
						continue;
					opts = field.get(null);
				}
			} catch (Throwable t) {
				System.out.println("Problem processing: " + line);
				throw new RuntimeException(t);
			}

			if (opts != null) {
				args.add(group);
				args.add(opts);
			}
		}

		parser.registerAll(args.toArray(new Object[0]));
		return parser;
	}
	
	Set<String> stopWords;
	
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

	class IntPair {
		int st, ed;

		public IntPair(int st, int ed) {
			this.st = st;
			this.ed = ed;
		}
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

	public List<IntPair> searchParsingEntities(BeamParserState state, Example ex) {
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
			BeamParserState state, Example ex) {
		List<EntityParsingResult> res = new ArrayList<>();
		for (IntPair position : entityPos) {
			int st = position.st;
			int ed = position.ed;
			Map<String, Double> id2EntityScore = new HashMap<>();
			String phrase = ex.phrase(st, ed);
			for (Derivation deriv : state.chart[st][ed].get("$Entity")) {
				double score = deriv.score;
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
			BeamParserState state, Example ex, PopularityCache popCache) {
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
			BeamParserState state, RelationCache cache) {
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
			tokenWriter.write(ex.phrase(0, ex.numTokens()) + " ");
			
			List<IntPair> entities = search_entities(ex);
			ex.fillEntities(entities);
			BeamParserState state = (BeamParserState) builder.parser.parse(
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
				computeTypePopularity(parsingResults, state, ex, popCache);
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
