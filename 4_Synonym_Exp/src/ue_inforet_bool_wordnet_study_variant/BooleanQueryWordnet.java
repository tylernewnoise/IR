/* BooleanQueryWordnet - Falkos Version */
package ue_inforet_bool_wordnet_study_variant;

// indexing
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

// building synset index
import java.util.Map;
import java.util.StringTokenizer;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import org.apache.commons.lang3.StringUtils;

// searching
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;

// file input
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// results output
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;

// threading
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class BooleanQueryWordnet {
	// global variables for thread executing, lucene's index directory and analyzer
	private ExecutorService executorService;
	private Directory index = new RAMDirectory();
	private StandardAnalyzer analyzer = new StandardAnalyzer();
	private QueryParser parser = new QueryParser("", analyzer);

	// synonyms
	private THashMap<String, THashSet<String>> allSynonyms = new THashMap<>(67000);
	private THashMap<String, THashSet<String>> adverbs = new THashMap<>(2300);
	private THashMap<String, THashSet<String>> adjectivs = new THashMap<>(16000);
	private THashMap<String, THashSet<String>> verbs = new THashMap<>(9100);

	/**
	 * DO NOT ADD ADDITIONAL PARAMETERS TO THE SIGNATURE
	 * OF THE CONSTRUCTOR.
	 */
	public BooleanQueryWordnet() {
	}

	/**
	 * A method for parsing the WortNet synsets.
	 * The data.[noun, verb, adj, adv] files contain the synsets.​
	 * The [noun, verb, adj, adv].exc	files contain the base forms
	 * of irregular words.
	 * <p>
	 * Please refer to ​
	 * http://wordnet.princeton.edu/man/wndb.5WN.html
	 * regarding the syntax of these plain files.​
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param wordnetDir the directory of the wordnet files
	 */
	public void buildSynsets(String wordnetDir) {
		parseDataFile(wordnetDir + "data.adv", adverbs);
		parseDataFile(wordnetDir + "data.verb", verbs);
		parseDataFile(wordnetDir + "data.adj", adjectivs);
		parseDataFile(wordnetDir + "data.noun", allSynonyms);

		parseExcFile(wordnetDir + "verb.exc", verbs);
		parseExcFile(wordnetDir + "adv.exc", adverbs);
		parseExcFile(wordnetDir + "adj.exc", adjectivs);
		parseExcFile(wordnetDir + "noun.exc", allSynonyms);

		mergeSynsets(adverbs);
		mergeSynsets(verbs);
		mergeSynsets(adjectivs);
	}

	private void parseDataFile(String file, THashMap<String, THashSet<String>> hashMap) {
		int start = 0;
		int howManyWords;
		boolean isAdjective = false;

		if (file.endsWith(".adj")) {
			isAdjective = true;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// skip the first 29 lines
				if (start < 29) {
					++start;
					continue;
				}

				// how many words are in the synset
				howManyWords = Integer.parseInt(StringUtils.substring(line, 14, 16), 16);

				// if there's only one word we can skip everything
				if (howManyWords == 1) {
					continue;
				}

				// tokenize the rest of the line, start after all the info-gibberish
				StringTokenizer st = new StringTokenizer(StringUtils.substring(line, 17, line.length()), " ", false);
				String token = st.nextToken().intern();

				// create a synset
				ArrayList<String> synset = new ArrayList<>();

				// add all the synonyms to our synset
				while (howManyWords > 0) {
					// skip if it's not a single-token synonym
					if (token.contains("_")) {
						--howManyWords;
						st.nextToken();
						token = st.nextToken().intern();
						continue;
					}
					// check adjective syntax
					if (isAdjective && (token.endsWith("(p)") || token.endsWith("(a)") || token.endsWith("(ip)"))) {
						if (token.endsWith("(p)") || token.endsWith("(a)")) {
							synset.add(StringUtils.substring(token, 0, token.length() - 3).toLowerCase().intern());
						} else {
							synset.add(StringUtils.substring(token, 0, token.length() - 4).toLowerCase().intern());
						}
					} else {
						synset.add(token.toLowerCase().intern());
					}
					--howManyWords;
					st.nextToken();
					token = st.nextToken().intern();
				}

				// if nothing is in the synset we can skip
				if (synset.size() == 0) {
					continue;
				}

				// if the synset contains after all only one word
				if (synset.size() == 1) {
					if (!hashMap.containsKey(synset.get(0))) {
						THashSet<String> tmp = new THashSet<>();
						tmp.add(synset.get(0));
						hashMap.put(synset.get(0), tmp);
					}
				} else if (synset.size() > 1) {
					// if the synset contains more than one word we have to build the relations
					for (int i = 0; i < synset.size(); ++i) {
						for (int j = 0; j < synset.size(); ++j) {
							if (i == j) {
								continue;
							}
							// add a relation
							if (hashMap.containsKey(synset.get(i))) {
								hashMap.get(synset.get(i)).add(synset.get(j));
							} else {
								// create a new synset
								THashSet<String> tmp = new THashSet<>();
								tmp.add(synset.get(j));
								tmp.add(synset.get(i));
								hashMap.put(synset.get(i), tmp);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void parseExcFile(String file, THashMap<String, THashSet<String>> hashMap) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {

				// tokenize the exceptions
				StringTokenizer st = new StringTokenizer(line, " ", false);
				ArrayList<String> exceptions = new ArrayList<>(3);
				while (st.hasMoreTokens()) {
					String token = st.nextToken().intern();
					exceptions.add(token.toLowerCase().intern());
				}

				// if the base is a non-single-token we can skip
				if (exceptions.get(0).contains("_")) {
					continue;
				}

				// check if the base-word is already in our list
				if (!hashMap.containsKey(exceptions.get(0))) {
					THashSet<String> tmp = new THashSet<>();
					tmp.add(exceptions.get(0));
					hashMap.put(exceptions.get(0), tmp);
				}

				// now run through the exceptions and union the synsets
				for (int i = 1; i < exceptions.size(); ++i) {
					// does the word even exist
					if (!exceptions.get(i).contains("_")) {
						// first, add the word itself to the base
						hashMap.get(exceptions.get(0)).add(exceptions.get(i));
						// if a synset exits for the word, add it too.
						if (hashMap.containsKey(exceptions.get(i))) {
							hashMap.get(exceptions.get(0)).addAll(hashMap.get(exceptions.get(i)));
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void mergeSynsets(THashMap<String, THashSet<String>> hashMap) {
		// run through the complete hashmap and check every word
		// since nouns are the biggest we use them as our merge-target
		for (Map.Entry<String, THashSet<String>> synsetEntry : hashMap.entrySet()) {
			if (!allSynonyms.containsKey(synsetEntry.getKey())) {
				// create a new synset if it's not already there
				allSynonyms.put(synsetEntry.getKey(), synsetEntry.getValue());
			} else {
				// or add everything to the existing one
				allSynonyms.get(synsetEntry.getKey()).addAll(synsetEntry.getValue());
			}
		}
	}

	/**
	 * A method for reading the textual movie plot file and building a Lucene index.
	 * The purpose of the index is to speed up subsequent boolean searches using
	 * the {@link #booleanQuery(String) booleanQuery} method.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param plotFile the textual movie plot file 'plot.list', obtainable from <a
	 *                 href="http://www.imdb.com/interfaces"
	 *                 >http://www.imdb.com/interfaces</a> for personal, non-commercial
	 *                 use.
	 */
	public void buildIndices(String plotFile) {
		boolean isPlotLine = false;

		// change the merge factor
		LogMergePolicy log = new LogDocMergePolicy();
		log.setMergeFactor(500);
		IndexWriterConfig config = new IndexWriterConfig(analyzer).setMergePolicy(log);
		config.setMaxBufferedDocs(600000);
		config.setRAMBufferSizeMB(IndexWriterConfig.DISABLE_AUTO_FLUSH);
		config.setUseCompoundFile(false);

		StringBuilder stringBuilder = new StringBuilder();

		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try (BufferedReader lineReader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;
			IndexWriter indexWriter = new IndexWriter(index, config);
			indexWriter.forceMerge(1, true);

			ArrayList<String> documentForThreadList = new ArrayList<>(3);

			// how this works: run through the file line by line and put every line from a movie
			// (aka document) in an ArrayList; give this ArrayList to a thread and let the tread
			// do the indexing
			while ((line = lineReader.readLine()) != null) {
				if (line.startsWith("M")) {
					if (isPlotLine) {
						documentForThreadList.add(stringBuilder.toString());
						startThreads(documentForThreadList, indexWriter);
						isPlotLine = false;
						stringBuilder = new StringBuilder();
						documentForThreadList = new ArrayList<>(3);
					}
					documentForThreadList.add(line);
					documentForThreadList.add(line.substring(4, line.length()));
				}
				if (line.startsWith("PL:")) {
					stringBuilder.append(line.substring(4, line.length()));
					stringBuilder.append(" ");
					isPlotLine = true;
				}
			}

			// add the very last line of the file to the ArrayList
			documentForThreadList.add(stringBuilder.toString());
			documentToIndex(documentForThreadList, indexWriter);
			// close everything and wait until all threads are finished
			lineReader.close();
			executorService.shutdown();
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			indexWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// start a thread
	private void startThreads(ArrayList<String> documentList, IndexWriter indexWriter) {
		executorService.execute(() -> documentToIndex(documentList, indexWriter));
	}

	// add an document to lucene's index
	private void documentToIndex(ArrayList<String> documentList, IndexWriter indexWriter) {
		try {
			Document doc = new Document();
			// add the complete "MV:-Line" to the lucene document so we can retrieve it later
			doc.add(new StringField("mvline", documentList.get(0), StringField.Store.YES));
			// add the plot, title, type and year fields to the document
			doc.add(new TextField("plot", documentList.get(2), TextField.Store.NO));
			getTitleTypeYear(doc, documentList.get(1));
			indexWriter.addDocument(doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// extract the type, title and year, the latter two with the help of parseTitleAndYear()
	private void getTitleTypeYear(Document doc, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			doc.add(new StringField("type", "series", StringField.Store.NO));
			parseTitleAndYear(doc, mvLine, true);
		}
		// +++ episode +++
		else if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			doc.add(new StringField("type", "episode", StringField.Store.NO));

			StringBuilder stringBuilder = new StringBuilder();

			for (int i = mvLine.length() - 2; mvLine.charAt(i) != '{'; --i) {
				stringBuilder.append(mvLine.charAt(i));
			}

			doc.add(new TextField("episodetitle",
				new StringBuilder(stringBuilder.toString()).reverse().toString(), TextField.Store.NO));

			parseTitleAndYear(doc, mvLine.substring(0, mvLine.indexOf('{') - 1), true);
		}
		// +++ television +++
		else if (mvLine.contains(") (TV)")) {
			doc.add(new StringField("type", "television", StringField.Store.NO));
			parseTitleAndYear(doc, mvLine.substring(0, mvLine.length() - 5), false);
		}
		// +++ video +++
		else if (mvLine.contains(") (V)")) {
			doc.add(new StringField("type", "video", StringField.Store.NO));
			parseTitleAndYear(doc, mvLine.substring(0, mvLine.length() - 4), false);
		}
		// +++ video game +++
		else if (mvLine.contains(") (VG)")) {
			doc.add(new StringField("type", "videogame", StringField.Store.NO));
			parseTitleAndYear(doc, mvLine.substring(0, mvLine.length() - 5), false);
		} else {
			// +++ movie +++
			doc.add(new StringField("type", "movie", StringField.Store.NO));
			parseTitleAndYear(doc, mvLine, false);
		}
	}

	private void parseTitleAndYear(Document doc, String mvLine, boolean isSeries) {
		int end = 3;
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = mvLine.length() - 2; mvLine.charAt(i) != '('; --i) {
			if (mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9') {
				stringBuilder.append(mvLine.charAt(i));
			}
			end++;
		}

		String year = stringBuilder.reverse().toString().intern();

		if (year.length() == 4) {
			doc.add(new StringField("year", year.intern(), StringField.Store.NO));
		}

		if (isSeries) {
			doc.add(new TextField("title", mvLine.substring(1, mvLine.length() - end - 1).intern(),
				TextField.Store.NO));
		} else {
			doc.add(new TextField("title", mvLine.substring(0, mvLine.length() - end).intern(),
				TextField.Store.NO));
		}
	}

	private String parseQueryString(String queryString) {
		StringBuilder newQuery = new StringBuilder();
		StringTokenizer st = new StringTokenizer(queryString, " (:)", true);

		// run through the tokenized query
		while (st.hasMoreTokens()) {
			String token = st.nextToken().intern();

			// add open parenthesis first
			if (token.equals("(")) {
				newQuery.append("(");
			} else {
				String field = token.intern();
				st.nextToken(); // this token is the colon after the field type
				token = st.nextToken().toLowerCase().intern(); // the actual word we're searching a synset for

				// now search and check for synonyms
				if (allSynonyms.containsKey(token) && allSynonyms.get(token).size() > 1) {
					// we blow up the query and start with a parenthesis
					newQuery.append("(");

					// add all the synsets to a list
					ArrayList<String> synset = new ArrayList<>();
					synset.addAll(allSynonyms.get(token));

					// run through the synset list and add the content to our new query
					for (int i = 0; i < synset.size(); ++i) {
						newQuery.append(field).append(":").append(synset.get(i));
						// add an OR after each word
						if (i < synset.size() - 1) {
							newQuery.append(" OR ");
						}
					}
					// close the expansion
					newQuery.append(")");
				} else {
					// if there are no synonyms for the query just add the query itself
					newQuery.append(field).append(":").append(token);
				}

				// check if the query is finished; if not add AND, OR, NOT or closing parenthesis
				if (st.hasMoreTokens()) {
					while (st.hasMoreTokens()) {
						token = st.nextToken().intern();
						// add closing parenthesis
						if (token.equals(")")) {
							newQuery.append(")");
						} else {
							// add AND, OR, NOT
							newQuery.append(token).append(st.nextToken()).append(st.nextToken());
							break;
						}
					}
				}
			}
		}
		return newQuery.toString();
	}

	/**
	 * A method for performing a boolean search on a textual movie plot file after
	 * Lucene indices were built using the {@link #buildIndices(String) buildIndices}
	 * method. The movie plot file contains entries of the <b>types</b> movie,
	 * series, episode, television, video, and videogame. This method allows queries
	 * following the Lucene query syntax on any of the <b>fields</b> title, plot, year,
	 * episode, and type. Note that queries are case-insensitive and stop words are
	 * removed.<br>
	 * <br>
	 * <p>
	 * More details on the query syntax can be found at <a
	 * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
	 * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param queryString the query string, formatted according to the Lucene query syntax.
	 * @return the exact content (in the textual movie plot file) of the title
	 * lines (starting with "MV: ") of the documents matching the query
	 */
	public Set<String> booleanQuery(String queryString) {

		HashSet<String> results = new HashSet<>();
		try {
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			Query q = parser.parse(parseQueryString(queryString));
			ConstantScoreQuery query = new ConstantScoreQuery(q);
			//searcher.createWeight(q, false);
			ScoreDoc[] hits = searcher.search(query, 100).scoreDocs;

			for (ScoreDoc hit : hits) {
				Document hitDoc = searcher.doc(hit.doc);
				results.add(hitDoc.get("mvline"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	/**
	 * A method for closing any open file handels or a ThreadPool.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 */
	public void close() {
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("usage: java -jar BooleanQueryWordnet.jar <plot list file> <wordnet directory> <queries file> <results file>");
			System.exit(-1);
		}

		// build indices
		System.out.println("building indices...");
		long tic = System.nanoTime();
		Runtime runtime = Runtime.getRuntime();
		long mem = runtime.totalMemory();

		// the directory to the wordnet-files: [*.exc], [data.*]
		String plotFile = args[0];
		String wordNetDir = args[1];

		BooleanQueryWordnet bq = new BooleanQueryWordnet();
		bq.buildSynsets(wordNetDir);

		bq.buildIndices(plotFile);
		System.gc();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		System.out.println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
		System.out.println("memory: " + ((runtime.totalMemory() - mem) / (1048576L)) + " MB (rough estimate)");

		// parsing the queries that are to be run from the queries file
		List<String> queries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(args[2]), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null)
				queries.add(line);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// parsing the queries' expected results from the results file
		List<Set<String>> results = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(args[3]), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Set<String> result = new HashSet<>();
				results.add(result);
				for (int i = 0; i < Integer.parseInt(line); i++) {
					result.add(reader.readLine());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// run queries
		for (int i = 0; i < queries.size(); i++) {
			String query = queries.get(i);
			Set<String> expectedResult = i < results.size() ? results.get(i)
				: new HashSet<>();
			System.out.println();
			System.out.println("query:           " + query);
			tic = System.nanoTime();
			Set<String> actualResult = bq.booleanQuery(query);

			// sort expected and determined results for human readability
			List<String> expectedResultSorted = new ArrayList<>(expectedResult);
			List<String> actualResultSorted = new ArrayList<>(actualResult);
			Comparator<String> stringComparator = Comparator.naturalOrder();
			expectedResultSorted.sort(stringComparator);
			actualResultSorted.sort(stringComparator);

			System.out.println("runtime:         " + (System.nanoTime() - tic) + " nanoseconds.");
			System.out.println("expected result (" + expectedResultSorted.size() + "): " + expectedResultSorted.toString());
			System.out.println("actual result (" + actualResultSorted.size() + "):   " + actualResultSorted.toString());
			System.out.println(expectedResult.equals(actualResult) ? "SUCCESS" : "FAILURE");
		}
		bq.close();
	}
}