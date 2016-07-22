package models.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cern.colt.matrix.DoubleMatrix1D;

/**
 * @author Haoran Li
 */

class MatrixGenThread implements Runnable{
	MetaPath metaPath;
	String indir, outdir;
	List<List<DoubleMatrix1D>> interMats;
	private static Lock lock = new ReentrantLock();  
	private static int handle_num = 0;
	static long startTime;
	static long endTime;
	static boolean pruning = false;
	
	public MatrixGenThread(String indir, String outdir, MetaPath metaPath) throws FileNotFoundException, ClassNotFoundException, IOException {
		this.metaPath = metaPath;
		this.indir = indir;
		this.outdir = outdir;
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		interMats = new ArrayList<List<DoubleMatrix1D>>();
		for (int i = 0; i < metaPath.path.size() - 1; i++)
		{
			String type1 = metaPath.path.get(i);
			String type2 = metaPath.path.get(i+1);
			String file = indir + "/" + type1 + " -> " + type2 + ".mat";
			
			try {
				interMats.add((List<DoubleMatrix1D>) ObjectWriter.readObject(file));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		
		MetaPathInstanceCnt path_cnt = new MetaPathInstanceCnt(metaPath, interMats);

		if (pruning && metaPath.path.size() >= 3 && !metaPath.path.get(1).equals("word"))
		{
			path_cnt.pruning();
		}
		path_cnt.calc();
		
		
		lock.lock();
		try {  
			System.out.println(metaPath);
			endTime = System.currentTimeMillis();
			System.out.println("runing time: "+(endTime-startTime)/1000.0+"s");
			handle_num ++;
			if (handle_num % 10 == 0)
				System.out.println("handle_num = " + handle_num);
	    } finally {  
	        lock.unlock();  
	    }  
		
		String file = outdir + "/" + (metaPath.path.size() - 1) +  "/" + metaPath.toString() + ".mat";
		File dir_file = new File(outdir + "/" + (metaPath.path.size() - 1));
		dir_file.mkdirs();
		ObjectWriter.writeObject(path_cnt.restore(), file);
	}
}


public class handleSemanticParsing {
	
	

	String DocTextFile;
	String SPFile;
	String OutPrefix;
	String StopWordsFile = "lib/data/stopwords_lemmatized.txt";
	String MatPrefix;
	int maxMetapathLen;
	
	static boolean normalized = false;
	
	static boolean convertToSports = false;
	static String[] sportsDomain = {"skiing","cricket","basketball","baseball","ice_hockey","olympics","soccer","american_football","boxing","tennis"};
	
	
	public Set<String> stopwords;
	public Map<String, Map<String, Integer>> doc_word_cnt;
	public Map<String, Map<String, Integer>> doc_e_cnt;
	public Map<String, Integer> doc_tote;
	public Map<String, String> entity2domain;
	public List<Co_Occur_Block> blocks;
	
	//argv should take the form:tokenFile SPFile outputDir matrixOutDir [stopwordsFile]
	handleSemanticParsing(String[] argv) {
		DocTextFile = argv[0];
		SPFile = argv[1];
		OutPrefix = argv[2];
		MatPrefix = argv[3];
		maxMetapathLen = Integer.parseInt(argv[4]);
		if (argv.length >= 6) {
			StopWordsFile = argv[5];
		}
	}

	
	
	public void load() {
		loadStopWords();
		loadDocWords();
		loadSPResult();
//		eliminateUnvalidDoc();
	}
	
	public static String getDomain(String type) {
		if (type.startsWith("user"))
			return "computer";
		else {
			String[] token = type.split("\\.");
			return token[0];
		}
	}
	
	public void storeMat(List<DoubleMatrix1D> mat, MetaPath metaPath)
	{
		String dir_path = MatPrefix + "/" + (metaPath.path.size() - 1);
		File dir = new File(dir_path);
		dir.mkdirs();
		try {
			List<DoubleMatrix1D> matrix = mat;
			ObjectWriter.writeObject(matrix, dir + "/" + metaPath + ".mat");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, Map<String, Integer>> extract(Map<String, Map<String, Integer>> map, Set<String> keyset) {
		Map<String, Map<String, Integer>> ret = new HashMap<>();
		for (String key:keyset) {
			if (!map.containsKey(key)) { 
				ret.put(key, new HashMap<String, Integer>());
				continue;
//				throw new RuntimeException("no info for key:" + key);
			}
			ret.put(key, map.get(key));
		}
		return ret;
	}
	
	public Map<String, Integer> extractCnt(Map<String, Integer> map, Set<String> keyset) {
		Map<String, Integer> ret = new HashMap<>();
		for (String key: keyset) {
			if (!map.containsKey(key)){
				ret.put(key, 0);
				continue;
//				throw new RuntimeException("no info for key:" + key);
			}
//				
			ret.put(key, map.get(key));
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> docsetFromPrevious(String dir) {
		Map<String, Map<String, Integer>> doc_word_cnt = (Map<String, Map<String, Integer>>) ObjectWriter.readObject(dir);
		Set<String> docs = new HashSet<String>();
		for (String docid:doc_word_cnt.keySet())
			docs.add(docid.substring(4));
		return docs;
	}
	
	public Map<String, Map<String, Integer>> docwordFromPrevious(String dir) {
		Map<String, Map<String, Integer>> doc_word_cnt = (Map<String, Map<String, Integer>>) ObjectWriter.readObject(dir);
		Map<String, Map<String, Integer>> ret = new HashMap<>();
		for (String docid:doc_word_cnt.keySet())
			ret.put(docid.substring(4), doc_word_cnt.get(docid));
		return ret;
	}
	
	public void eliminateUnvalidDoc() {
		Set<String> goodDocs= new HashSet<>();
		for (String docid: doc_word_cnt.keySet()) {
			if (doc_word_cnt.get(docid).size() == 0)
				continue;
//			if (!doc_e_cnt.containsKey(docid) || doc_e_cnt.get(docid).size() == 0)
//				continue;
			goodDocs.add(docid);
		}
		doc_word_cnt = extract(doc_word_cnt, goodDocs);
		doc_e_cnt = extract(doc_e_cnt, goodDocs);
		doc_tote = extractCnt(doc_tote, goodDocs);
		System.out.println("good doc size = " + doc_e_cnt.size());	
	}
	
	public void writeData(List<MetaPath> metaPaths, Map<String, Map<String, Integer>> et_e_idx) {
		ObjectWriter.writeObject(metaPaths, OutPrefix + "/metaPaths.list");
		ObjectWriter.writeObject(et_e_idx, OutPrefix + "/et_e_idx.map");
		ObjectWriter.writeObject(this.entity2domain, OutPrefix + "/entity2domain.map");
		ObjectWriter.writeObject(this.doc_e_cnt, OutPrefix + "/doc_e_cnt.map");
		ObjectWriter.writeObject(this.doc_word_cnt, OutPrefix + "/doc_word_cnt.map");
		
	}
	
	public void generateMatrix() {
		System.out.println("meta-path generation");
		List<MetaPath> metaPaths = getAllMetaPaths(this.entity2domain);
		setCo_Occur_Blocks(entity2domain);
		generateBasisMatrix(this.entity2domain);
		writeData(metaPaths, Co_Occur_Block.et_e_idx);
		generateAllMatrix(metaPaths);
	}
	
	public void loadStopWords(){
		stopwords = new HashSet<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(StopWordsFile));
			String line;
			while ((line = reader.readLine()) != null) {
				stopwords.add(line.trim().toLowerCase());
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	Pattern num_pattern = Pattern.compile("-?[0-9]+.?[0-9]+"); 
	public boolean isNumeric(String str)
	{
	   Matcher isNum = num_pattern.matcher(str);
	   if( !isNum.matches() ){
	       return false; 
	   } 
	   return true; 
	}
	
	public boolean isValidWord(String word)
	{
		if (stopwords.contains(word))
			return false;
		if (word.contains("'"))
			return false;
		if (isNumeric(word.trim()))
			return false;
		return true;
	}
	
	
	public void addToMapCnt(Map<String, Integer> map, String key) {
		if (!map.containsKey(key))
			map.put(key, 0);
		int cnt = map.get(key);
		map.put(key, cnt+1);
	}
	
	public Set<String> getWordLessThan(Map<String, Map<String, Integer>> doc_words, int k)
	{
		Map<String, Integer> word_cnt = new HashMap<String, Integer>();
		String word;
		int num;
		for (Map<String, Integer> adoc_word_cnt: doc_words.values())
		{
			for (Entry<String, Integer> entry: adoc_word_cnt.entrySet())
			{	
				word = entry.getKey();
				num = 1;
				if (word_cnt.containsKey(word))
				{
					int tmp = num + word_cnt.get(word);
					word_cnt.put(word, tmp);
				} else {
					word_cnt.put(word, new Integer(num));
				}
			}
		}
		Set<String> rare_words = new HashSet<String>();
		for (Entry<String, Integer> entry: word_cnt.entrySet())
		{
			if (entry.getValue() < k)
			{
				rare_words.add(entry.getKey());
			}
		}
		return rare_words;
	}
	
	public void loadDocWords() {
		doc_word_cnt = new HashMap<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(DocTextFile));
			String line;
			String docid = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains(" ||| "))  {
					docid = line.trim();
				} else {
					Map<String, Integer> word_cnt = new HashMap<String, Integer>();
					String[] tokens = line.split(" +");
					for (String token: tokens) {
						token = token.toLowerCase();
						if (isValidWord(token)) {
							addToMapCnt(word_cnt, token);
						}
					}
					this.doc_word_cnt.put(docid, word_cnt);
				}
			}
			
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Set<String> rare_words = getWordLessThan(doc_word_cnt, 2);
		for (String docID: doc_word_cnt.keySet())
		{
			Map<String, Integer> word_cnt = doc_word_cnt.get(docID);
			Set<String> deleted_word = new HashSet<String>();
			for (String word:word_cnt.keySet())
				if (rare_words.contains(word))
					deleted_word.add(word);
			for (String word:deleted_word)
				word_cnt.remove(word);	
		}
	}
	
	public Map<String, String> entityDomainAdjust(Map<String, String> entity2domain) {
		Map<String, String> ret = new HashMap<>();
		Set<String> sportsDomainSet = new HashSet<>();
		sportsDomainSet.addAll(Arrays.asList(sportsDomain));
		for (String entity: entity2domain.keySet()) {
			String domain = entity2domain.get(entity);
			if (sportsDomainSet.contains(domain))
				domain = "sports";
			ret.put(entity, domain);
		}
		return ret;
	}
	
	public void loadSPResult() {
		doc_e_cnt = new HashMap<>();
		doc_tote = new HashMap<>();
		entity2domain = new HashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(SPFile));
			String line;
			String docid = null;
			while ((line = reader.readLine()) != null) {

				if (line.trim().equals(""))
					continue;
				if (line.startsWith("doc:")) {
					docid = line.trim().substring(4);
				} else {
					if (line.startsWith("----sentence result") || line.startsWith("\t\t"))
						continue;
					if (!doc_e_cnt.containsKey(docid)) 
						doc_e_cnt.put(docid, new HashMap<>());
					Map<String, Integer> e_cnt = doc_e_cnt.get(docid);
					String[] tokens = line.trim().split(" +");
					String entity_id = tokens[0];
					String entity_type = tokens[1];
					String phrase = tokens[2];
					for (int i = 3; i < tokens.length; i++)
						phrase = phrase + " " + tokens[i];
					if (isValidWord(phrase)) {
						String entity_domain = getDomain(entity_type);
						entity2domain.put(entity_id, entity_domain);
						addToMapCnt(e_cnt, entity_id);
						addToMapCnt(doc_tote, docid);
					}
				}
			}
			if (convertToSports)
				entity2domain = entityDomainAdjust(entity2domain);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public List<MetaPath> getAllMetaPaths(Map<String, String> e_topet)
	{
		List<MetaPath> metaPaths = MetaPath.generateAllMetaPaths(maxMetapathLen - 1, e_topet);
		metaPaths.add(new MetaPath("word"));
		System.out.println("path size:"+ metaPaths.size()); 
		return metaPaths;
	}
	
	public void setCo_Occur_Blocks(Map<String, String> e_topet)
	{
		// sentence-level co-occurrence
//		for (Integer sentid: sent_e_cnt.keySet())
//			blocks.add(new Co_Occur_Block(sent2doc.get(sentid), sent_e_cnt.get(sentid), e_topet));
		
		// document-level co-occurrence
		blocks = new ArrayList<>();
		for (String docid: doc_e_cnt.keySet())
			blocks.add(new Co_Occur_Block(docid, doc_e_cnt.get(docid), e_topet));
		Map<String, String> tmp_e_et = new HashMap<String, String>();
		for (String doc: doc_word_cnt.keySet())
		{
			for (String word: doc_word_cnt.get(doc).keySet())
				tmp_e_et.put(word, "word");
			blocks.add(new Co_Occur_Block(doc, doc_word_cnt.get(doc),tmp_e_et));
		}
	}
	
	public void generateBasisMatrix(Map<String, String> e_topet)
	{
		
		Set<String> types = new HashSet<String>();
		for (String type:e_topet.values())
			types.add(type);
		System.out.println("type list");
		for (String type:types)
			System.out.println("\t" + type);
		System.out.println("generate Matrix of length-1 metapath");
		int mat_cnt = 0;
		for (String type1: types)
			for (String type2: types)
			{
				if (type1.equals(type2))
					continue;
				List<DoubleMatrix1D> mat  = Co_Occur_Block.calcMatrix(type1, type2, blocks, this.doc_tote, normalized);
				MetaPath metaPath = new MetaPath();
				metaPath.add(type1);
				metaPath.add(type2);
				storeMat(mat, metaPath);
				mat_cnt += 1;
				if (mat_cnt % 100 == 0)
					System.out.println("\tmatrix_cnt:" + mat_cnt);
			}
		types.add("word");
		String type1 = "doc";
		for (String type2:types)
		{
			List<DoubleMatrix1D> mat = Co_Occur_Block.calcMatrix(type1, type2, blocks, this.doc_tote, normalized);
			MetaPath metaPath = new MetaPath();
			metaPath.add(type1);
			metaPath.add(type2);
			storeMat(mat, metaPath);
			mat_cnt += 1;
			if (mat_cnt % 100 == 0)
				System.out.println("\tmatrix_cnt:" + mat_cnt);
			
			mat = Co_Occur_Block.calcMatrix(type2, type1, blocks, this.doc_tote, normalized);
			metaPath = new MetaPath();
			metaPath.add(type2);
			metaPath.add(type1);
			storeMat(mat, metaPath);
			
			mat_cnt += 1;
			if (mat_cnt % 100 == 0)
				System.out.println("\tmatrix_cnt:" + mat_cnt);
		}
		System.out.println("\ttotal matrix_cnt:" + mat_cnt);
	}
	
	public void generateAllMatrix(List<MetaPath> metaPaths) {
		int rest = 0;
		ExecutorService pool=Executors.newFixedThreadPool(5);
		try {
			for (MetaPath metaPath: metaPaths)
			{
				
				if (metaPath.isSymmetrical() && metaPath.isProper())
				{
					pool.submit(new MatrixGenThread(MatPrefix+"/1", MatPrefix, metaPath));
					rest ++;
				}
			}
		System.out.println("rest metapath:" + rest);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		MatrixGenThread.startTime = System.currentTimeMillis();
		pool.shutdown();
	}
	
	public void printInfo(String docid) {
		
		System.out.println(doc_e_cnt.get(docid));
		for (String e: doc_e_cnt.get(docid).keySet()) {
			System.out.println(e + " " + this.entity2domain.get(e));
		}
		System.out.println(doc_word_cnt.get(docid));
		System.out.println(doc_tote.get(docid));
	}
	
	
	// argv should take the form:tokenFile SPFile outputDir matrixOutDir maxMetapathLen [stopwordsFile]
	public static void main(String[] argv) {
		if (argv.length < 5 || argv.length > 6) {
			System.out.println("argv should take the form:tokenFile SPFile outputDir matrixOutDir [stopwordsFile]");
			System.exit(0);
		}
		handleSemanticParsing hsp = new handleSemanticParsing(argv);
		hsp.load();
		hsp.generateMatrix();
//		hsp.printInfo("68272 ||| comp.windows.x");
	}	
}
