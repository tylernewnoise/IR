# Information Retrieval WS 16/17
* 01 IMDB Spider
	- BJ: B&J's version
	- queries: executes all queries
	- threading: attempt to realize the queries and the spider with the help of multithreading
* 02 Boolean Information Retrieval
	- ue_inforet_bool: uses the the libraries from trove (http://trove.starlight-systems.com/)
	- ue_inforet_bool_bj: tries to implement a positional index, unfinished and does not work
	- ue_inforet_bool_variant: standard java libs
* 03 Boolean Information Retrieval with Lucene
	- ue_inforet_bool_study: standard java libs
	- ue_inforet_bool_study_variant: builds lucene's index with multithreading
* 04 Synonym Expansion with Lucene and WordNet
	- ue_inforet_bool_wordnet_study: standard java libs, different approach to build the synsets and parse the query
	- ue_inforet_bool_wordnet_study_variant: uses the trove libraries for the synsets, builds lucene's index with multithreading
* 05 Finding Frequent Word Co-Occurrences
	- ue_inforet_cooccurrences:
	- ue_inforet_cooccurrences_std: uses standard java libs and an extra class for the bigram data structure
	- ue_inforet_cooccurrences_variant: uses trove libraries
	- ue_inforet_cooccurrences_threading: uses multithreading

02 - 05 requires the plot.list file: http://www.imdb.com/interfaces
04 requires the wordnet dictionary: http://wordnetcode.princeton.edu/wn3.1.dict.tar.gz