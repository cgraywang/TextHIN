package edu.stanford.nlp.sempre;


import fig.basic.Option;
import fig.exec.Execution;

/**
 * Entry point for the semantic parser.
 *
 * @author Percy Liang
 */
public class Main implements Runnable {
  @Option public boolean entity_linking = true;
  @Option public String inFile;
  @Option public String outFile;
  @Option public String tokenOutFile;

  public void run() {
    Builder builder = new Builder();
    builder.build();

    Dataset dataset = new Dataset();
    dataset.read();

    Learner learner = new Learner(builder.parser, builder.params, dataset);
    learner.learn();

    
    if (entity_linking) {
    	Master master = new Master(builder);
    	master.load();
    	master.entityLexcionForFile(inFile, outFile, tokenOutFile);
    	
    }
  }

  public static void main(String[] args) {
    Execution.run(args, "Main", new Main(), Master.getOptionsParser());
  }
}
