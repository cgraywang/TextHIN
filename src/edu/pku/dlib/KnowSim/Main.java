package edu.pku.dlib.KnowSim;

import edu.stanford.nlp.sempre.Builder;
import edu.stanford.nlp.sempre.Master;
import fig.basic.Option;
import fig.exec.Execution;

/**
 * Entry point for the Text Parser
 *
 * @author Haoran Li
 */

public class Main implements Runnable {
	  @Option public boolean entity_linking = true;
	  @Option public String inFile;
	  @Option public String outFile;
	  @Option public String tokenOutFile;

	  public void run() {
	    Builder builder = new Builder();
	    builder.build();


	    
	    if (entity_linking) {
	    	TextParser parser = new TextParser(builder);
	    	parser.load();
	    	parser.entityLexcionForFile(inFile, outFile, tokenOutFile);
	    }
	  }

	  public static void main(String[] args) {
	    Execution.run(args, "Main", new Main(), Master.getOptionsParser());
	  }
}