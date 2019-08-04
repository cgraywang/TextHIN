# Text-based Heterogeneous Information Network Mining

## How to Use Unsupervised Semantic Parsing:
        $ git clone https://github.com/cgraywang/TextHIN.git
        $ ruby pull-dependencies.rb
        $ ant compile
        $ bash test.sh parse
        $ java -cp libknowsim/knowsim.jar -ea -Dmodules=parser,core,freebase edu.pku.dlib.KnowSim.Main 
                -Grammar.inPaths lib/models/15.exec/grammar 
                -FeatureExtractor.featureDomains entityAlignmentFeature 
                -EntityLexicon.freebaseDomainPath test/validDomains.txt 
                -inFile test/testdocs.txt 
                -outFile test/testdoc.sp.txt
                -tokenOutFile test/test.token.txt 
                -EntityLinkingResultPath test/testdoc.linking.txt
        $ bash test.sh pathcnt
        $ java -cp libknowsim/knowsim.jar edu.pku.dlib.models.clustering.handleSemanticParsing
                $token $sp $out_dir $matrix_out_dir $max_length $stopwords
        $ bash test.sh calcsim
        $ java -cp libknowsim/knowsim.jar edu.pku.dlib.MetaPathSim.MetaPathSim
                $sim_mode $metapth $matrix_out_dir $doc_label $out_dir $sim_outfile
                
## Where is Text based Heterogeneous Information Network Datasets:
        [data](https://github.com/cgraywang/TextHINData)

## References:

        @inproceedings{wang2016text,
        title={Text Classification with Heterogeneous Information Network Kernels},
        author={Wang, Chenguang and Song, Yangqiu and Li, Haoran and Zhang, Ming and Han, Jiawei},
        booktitle={AAAI},
        pages={2130--2136},
        year={2016}
        }

        @inproceedings{wang2015knowsim,
        title={Knowsim: A document similarity measure on structured heterogeneous information networks},
        author={Wang, Chenguang and Song, Yangqiu and Li, Haoran and Zhang, Ming and Han, Jiawei},
        booktitle={2015 IEEE International Conference on Data Mining},
        pages={1015--1020},
        year={2015},
        organization={IEEE}
        }

        @inproceedings{wang2015world,
        author = {Wang, Chenguang and Song, Yangqiu and El-Kishky, Ahmed and Roth, Dan and Zhang, Ming and Han, Jiawei},
        title = {Incorporating World Knowledge to Document Clustering via Heterogeneous Information Networks},
        booktitle = {KDD},
        pages = {1215--1224},
        year = {2015}
        }
