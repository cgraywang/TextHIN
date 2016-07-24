jvm_args="-ea "\
"-Dmodules=parser,core,freebase"

class="edu.pku.dlib.KnowSim.Main"
args="-Grammar.inPaths lib/models/15.exec/grammar "\
"-FeatureExtractor.featureDomains entityAlignmentFeature "\
"-EntityLexicon.freebaseDomainPath test/validDomains.txt "\
"-inFile test/testdocs.txt "\
"-outFile test/testdoc.sp.txt "\
"-tokenOutFile test/test.token.txt "\
"-EntityLinkingResultPath test/testdoc.linking.txt "
java -cp libknowsim/knowsim.jar $jvm_args $class $args
# java -cp libknowsim/knowsim.jar -ea -Dmodules=parser,core,freebase edu.pku.dlib.KnowSim.Main 
