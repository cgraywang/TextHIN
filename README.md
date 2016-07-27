# KnowSim

description

# Installation

## Requirements

You must have the following already installed on your system.

- Java 8
- Ant
- Ruby
- wget

Other dependencies will be downloaded from Peking University Dlib Laboratory.

## Easy setup

1. Clone the GitHub repository:

        git clone https://github.com/SilverHelmet/KnowSim
        
2. Download the dependencies (the downloaded libs are stored at `lib` directory):

        ruby pull-dependencies.rb
        
3. Compile the source code (this produces `libknowsim/knowsim.jar`):

        ant compile

## Virtuoso graph database

KnowSim use Freebase as extenal Knowledge source. You need to setup your own [Virtuoso database] (https://github.com/openlink/virtuoso-opensource) (unless someone already has done this for you):

## AIDA
We use [Accurate Online Disambiguation of Entities(AIDA)](https://github.com/yago-naga/aida) to help recognize entities. It's optional for you to use AIDA. 

## non-definite Kernel SVM

# USAGE
We provide a small dataset at `test` directory to help you get through the workflow fo KnowSim.

## Text Semantic Parsing

At Text Sematic Parsing step, KnowSim analyzes documents and parses entities and relations on documents.
To anaylyze documents:

        java -cp libknowsim/knowsim.jar -ea -Dmodules=parser,core,freebase edu.pku.dlib.KnowSim.Main 
        -Grammar.inPaths lib/models/15.exec/grammar -FeatureExtractor.featureDomains entityAlignmentFeature -EntityLexicon.freebaseDomainPath test/validDomains.txt 
        -inFile test/testdocs.txt -outFile test/testdoc.sp.txt -tokenOutFile test/test.token.txt 
        -EntityLinkingResultPath test/testdoc.linking.txt
        
where `testdocs.txt` is the file of documents to analyze, `testdoc.sp.txt` is the output file of Text Semantic Parsing, `test.token.txt` is the output file of tokens of documents, `testdoc.linking.txt` is the result of AIDA apply to documents. `url` is the of url of freebase's query server.
  
# IO Format
  
## Documents Format
Each document occupies two line. The first line is the ID of the document. The second line is the content of the document.
  
## AIDA's entity linking file 
  
  






        
