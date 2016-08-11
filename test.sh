doc="test/testdocs.txt"
doc_label="test/testdoc.label.txt"
token="test/test.token.txt"
sp="test/testdoc.sp.txt"
out_dir="test/out"
matrix_out_dir="${out_dir}/matrix"
max_length="4"
stopwords="lib/data/stopwords_lemmatized.txt"
sim_mode="2"
metapth="test/selected_metapath.txt"
sim_outfile="${out_dir}/sim.txt"

jvm_args="-ea "\
"-Dmodules=parser,core,freebase"

args="-Grammar.inPaths lib/models/15.exec/grammar "\
"-FeatureExtractor.featureDomains entityAlignmentFeature "\
"SparqlExecutor.endpointUrl http://162.105.146.246:3093/sparql "\
"-EntityLexicon.freebaseDomainPath test/validDomains.txt "\
"-EntityLinkingResultPath test/testdoc.linking.txt "
io_args="-inFile $doc "\
"-outFile $sp "\
"-tokenOutFile $token "

if [ "$1" == "parse" ]; then
	class="edu.pku.dlib.KnowSim.Main"
	java -cp libknowsim/knowsim.jar $jvm_args $class $args $io_args
fi
if [ "$1" == "pathcnt" ]; then
	if [ ! -d $out_dir ]; then
		mkdir $out_dir
	fi
	class="edu.pku.dlib.models.clustering.handleSemanticParsing"
	java -cp libknowsim/knowsim.jar $class $token $sp $out_dir $matrix_out_dir $max_length $stopwords
fi
if [ "$1" == "calcsim" ]; then
	class="edu.pku.dlib.MetaPathSim.MetaPathSim"
	java -cp libknowsim/knowsim.jar $class $sim_mode $metapth $matrix_out_dir $doc_label $out_dir $sim_outfile
fi
