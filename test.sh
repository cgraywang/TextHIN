jvm_args="-ea "\
"-Dmodules=parser,core,freebase"

class="edu.pku.dlib.KnowSim.Main"
args="-Grammar.inPaths G "\
"-FeatureExtractor.featureDomains entityAlignmentFeature "\
"SparqlExecutor.endpointUrl http://162.105.146.246:3093/sparql "\
"-EntityLexicon.freebaseDomainPath test/validDomains.txt "\
"-EntityLinkingResultPath test/testdoc.linking.txt "
io_args="-inFile test/testdocs.txt "\
"-outFile test/testdoc.sp.txt "\
"-tokenOutFile test/test.token.txt "\
java -cp libknowsim/knowsim.jar $jvm_args $class $args $io_args
