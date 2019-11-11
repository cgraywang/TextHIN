# A text-to-network representation and semantic parsing toolkit

## Pre-constructed text-to-networks:
We released the network datasets [[data]](https://github.com/cgraywang/TextHINData) for 20 newsgroups text dataset and RCV1 GCAT category text dataset.

## How to use:
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
                
## References:

        @article{wang2018unsupervised,
        title={Unsupervised meta-path selection for text similarity measure based on heterogeneous information networks},
        author={Wang, Chenguang and Song, Yangqiu and Li, Haoran and Zhang, Ming and Han, Jiawei},
        journal={Data Mining and Knowledge Discovery},
        volume={32},
        number={6},
        pages={1735--1767},
        year={2018},
        publisher={Springer}
        }
        
        @inproceedings{wang2017distant,
        title={Distant meta-path similarities for text-based heterogeneous information networks},
        author={Wang, Chenguang and Song, Yangqiu and Li, Haoran and Sun, Yizhou and Zhang, Ming and Han, Jiawei},
        booktitle={Proceedings of the 2017 ACM on Conference on Information and Knowledge Management},
        pages={1629--1638},
        year={2017},
        organization={ACM}
        }
        
        @article{wang2016world,
        title={World knowledge as indirect supervision for document clustering},
        author={Wang, Chenguang and Song, Yangqiu and Roth, Dan and Zhang, Ming and Han, Jiawei},
        journal={ACM Transactions on Knowledge Discovery from Data (TKDD)},
        volume={11},
        number={2},
        pages={13},
        year={2016},
        publisher={ACM}
        }
        
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
