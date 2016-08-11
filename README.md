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

KnowSim 使用Freebase做为外部知识来源，你需要启动你自己的[Virtuoso database] (https://github.com/openlink/virtuoso-opensource)来提供查询服务（除非有人已经为你做了）

KnowSim所使用的[Freebase副本] (net.pku.edu.cn/dlib/resources/vdb.tar.bz2)（已经加载了Freebase的Virtuoso database数据库）。

## AIDA
KnowSim使用[Accurate Online Disambiguation of Entities(AIDA)](https://github.com/yago-naga/aida)来帮助识别文本中的实体。AIDA的使用是可选择的。 

## non-definite Kernel SVM
KnowSim使用了[non-definite Kernel SVM](http://empslocal.ex.ac.uk/people/staff/yy267/indefinitesvm_nips2009.zip)进行分类，其依赖于[IndefiniteSVM](http://www.di.ens.fr/~aspremon/ZIP/IndefiniteSVM.zip)。

[IndefiniteSVM]中的一些c文件不与64位系统兼容，所以我们修改了它的mexFunctions目录下的一些文件来使其能在64为系统下编译。修改后的文件在本项目的`IndefiniteSVM`目录下

# USAGE
我们提供了一个小数据供测试使用，数据放在`test`目录下。`test.sh`脚本预设了了一些参数，并提供了一个简单的方法来运行程序。但是你需要修改其中的-SparqlExecutor.endpointUrl参数才能成功运行。

## Text Semantic Parsing
在文本语义分析阶段，KnowSim识别和解析文档中的实体与关系。
你可以使用运行test.sh来解析测试文档

        bash test.sh parse
        
也可以直接运行：

        java -cp libknowsim/knowsim.jar -ea -Dmodules=parser,core,freebase edu.pku.dlib.KnowSim.Main 
        -Grammar.inPaths lib/models/15.exec/grammar 
        -FeatureExtractor.featureDomains entityAlignmentFeature 
        -EntityLexicon.freebaseDomainPath test/validDomains.txt 
        -inFile test/testdocs.txt 
        -outFile test/testdoc.sp.txt
        -tokenOutFile test/test.token.txt 
        -EntityLinkingResultPath test/testdoc.linking.txt
其中`freebaseDomainPath`是预设的合法的Freebase的类型。

`inFile`是供解析的文档

`outFile`是分析结果的输出文件

`tokenOutFile`是文档token的输出文件。

`EntityLinkingResultPath` 是AIDA在供解析文档上运行结果，这个选项是可选择的。

## Commuting Matrix Generation
接下来，KnowSim需要生成元路径的所对应的邻接矩阵。同样的，你可以运行test.sh来生成测试数据的结果。

        bash test.sh pathcnt
        
也可以直接运行

        java -cp libknowsim/knowsim.jar edu.pku.dlib.models.clustering.handleSemanticParsing
        $token $sp $out_dir $matrix_out_dir $max_length $stopwords
        
其中`$token`语义分析阶段的token输出文件，`$sp`是语义分析阶段的结果输出文件, `$out_dir`是一个数据的输出目录，`$matrix_out_dir`是邻接矩阵的输出目录, `$max_length`是元路径的最大长度,`$stopwords`是停用词文本，默认为`lib/data/stopwords_lemmatized.txt`。

## Similarity 

  
# IO Format
  
## Documents Format
Each document occupies two line. The first line is the ID of the document. The second line is the content of the document.
  
## AIDA's entity linking file 
  
  






        
