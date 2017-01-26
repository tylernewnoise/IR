// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_wordnet_study;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BooleanQueryWordnet {
	// global accessible index :)
	Directory index = new RAMDirectory();
	// set Analyzer
	Analyzer myAnalyzer = new StandardAnalyzer();

 	private THashMap<String, THashSet<String>> allSynonyms = new THashMap<>();
	private THashMap<String, THashSet<String>> verbs = new THashMap<>();
 	private THashMap<String, THashSet<String>> adjectives = new THashMap<>();
 	private THashMap<String, THashSet<String>> adverbs = new THashMap<>();

 	// Parse Data Files
	private void parseData(String fileName, int type) {
		int wordCount;
		int skipNotice = 29; // 29 lines of copyright notice to skip
		boolean adjective = fileName.endsWith("adj"); // special handling for adjectives necessary

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (skipNotice > 0){
					skipNotice--; // count down
					continue;  // break the current while cycle to skip notice
				}

				// Get the expected amount of words in the current line
				wordCount = Integer.parseInt(StringUtils.substring(line,14,16),16);  // 14,16 -> to get the 2 digit Hex val. Last 16 because its Hex
				if (wordCount == 1) { // only one word...
					continue;
				}
				// split read words
				String[] justSplit = StringUtils.substring(line, 17).split( Pattern.quote( " " ) );
				// read/process the split words

				THashSet<String> tmp =  new THashSet<>();
				for (int i=0; i <= wordCount*2-1;) {
					if (justSplit[i].contains("_")) {   // does this one word is made of >=2 tokens?
						i = i+2;
						continue; // leave this word alone
					}
					// adjective?
					if (adjective && (justSplit[i].endsWith("(p)") || justSplit[i].endsWith("(a)") || justSplit[i].endsWith("(ip)"))) {
						if (justSplit[i].endsWith("(p)") || justSplit[i].endsWith("(a)")) {
							justSplit[i] = StringUtils.substring(justSplit[i], 0, justSplit[i].length() - 3);
						} else justSplit[i] = StringUtils.substring(justSplit[i], 0, justSplit[i].length() - 4);
					}
					tmp.add(justSplit[i].toLowerCase());
					i = i+2;
				}
				if (tmp.size() > 1) {
					if (tmp.contains("well")){
						System.out.println();
					}
					synDex(tmp, type); // run bennis magic :)
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// Parse Exc Files
	private void parseExc(String fileName, int type) {
		int wordCount;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// split read words
				String[] justSplit = StringUtils.substring(line, 0).split( Pattern.quote( " " ) );

				// wenn nur ein Wort in der Zeile ist... skippe Zeile
				wordCount = justSplit.length;
				if (wordCount < 2) continue;

				// else
				// read/process the split words
				if (justSplit[0].contains("_")) {   // does this one word is made of >=2 tokens?
					continue; // leave this line alone
				}

				THashSet<String> tmp =  new THashSet<>();
				for (int i=0; i < wordCount; i++){
					if (justSplit[i].contains("_")) {   // does this one word is made of >=2 tokens?
						continue; // leave this word alone
					}
					tmp.add(justSplit[i].toLowerCase());
				}
				if (tmp.size() > 0) {
					synDex(tmp, type); // run bennys magic :)
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}


	/**
	 * DO NOT ADD ADDITIONAL PARAMETERS TO THE SIGNATURE
	 * OF THE CONSTRUCTOR.
	 *
	 */
	public BooleanQueryWordnet() {
	}

	/**
	 * A method for parsing the WortNet synsets.
	 * The data.[noun, verb, adj, adv] files contain the synsets.​
	 * The [noun, verb, adj, adv].exc	files contain the base forms
	 * of irregular words.
	 *
	 * Please refer to ​
	 *  http://wordnet.princeton.edu/man/wndb.5WN.html
	 * regarding the syntax of these plain files.​
	 *
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param wordnetDir the directory of the wordnet files
	 */
	public void buildSynsets(String wordnetDir) {
		parseData(wordnetDir + "data.noun", 3);
		parseData(wordnetDir + "data.verb", 4);
		parseData(wordnetDir + "data.adj", 1);
		parseData(wordnetDir + "data.adv", 2);
		parseExc(wordnetDir + "noun.exc", 7);
		parseExc(wordnetDir + "verb.exc", 8);
		parseExc(wordnetDir + "adj.exc", 5);
		parseExc(wordnetDir + "adv.exc", 6);
		System.out.println("Not merged: " + adjectives.size()+verbs.size()+adverbs.size()+allSynonyms.size());
		mergeLists();
		System.out.println("Merged: " + allSynonyms.size());
		System.out.println();
		System.out.println("better: "+ allSynonyms.get("better"));
		System.out.println("good: " + allSynonyms.get("good"));
		System.out.println("well: " + allSynonyms.get("well"));

	}

	/***Adds a list of token into the fitting Hashmap
	 *
	 * @param tokenList
	 * @param type
	 */
	private void synDex (THashSet<String> tokenList, int type){
		THashSet<String> baseFormSynset = new THashSet<>();
		switch (type) {

			case 1: //data adjective
				for (String token : tokenList){
					addToHashmap(adjectives, token, tokenList);
				}
				break;
			case 2: //data adverbs
				for (String token : tokenList) {
					addToHashmap(adverbs, token, tokenList);
				}
				break;
			case 3: //data nouns
				for (String token : tokenList) {
					if (tokenList.contains("fountainhead")){
						System.out.println();
					}
					addToHashmap(allSynonyms, token, tokenList);
				}
				break;
			case 4: //data verbs
				for (String token : tokenList) {
					addToHashmap(verbs, token, tokenList);
				}
				break;
			case 5: //exc adjectives
				baseFormSynset.clear();
				baseFormSynset.addAll(tokenList);
				for (String token : tokenList){
					baseFormSynset.remove(token);
					for (String item : baseFormSynset){
						addToHashmap(adjectives, token, baseFormSynset); // add extensions
						if (adjectives.get(item) != null){
							addToHashmap(adjectives, token, adjectives.get(item) ); //add synonyms of extentions
						}
					}
				}
				break;
			case 6: //exc adverbs
				baseFormSynset.clear();
				baseFormSynset.addAll(tokenList);
				for (String token : tokenList){
					baseFormSynset.remove(token);
					for (String item : baseFormSynset){
						addToHashmap(adverbs, token, baseFormSynset); // add extensions
						if (adverbs.get(item) != null){
							addToHashmap(adverbs, token, adverbs.get(item) ); //add synonyms of extentions
						}
					}
				}
				break;
			case 7: //exc nouns
				baseFormSynset.clear();
				baseFormSynset.addAll(tokenList);
				for (String token : tokenList){
					baseFormSynset.remove(token);
					for (String item : baseFormSynset){
						addToHashmap(allSynonyms, token, baseFormSynset); // add extensions
						if (allSynonyms.get(item) != null){
							addToHashmap(allSynonyms, token, allSynonyms.get(item) ); //add synonyms of extentions
						}
					}
				}
				break;
			case 8: // exc verbs
				baseFormSynset.clear();
				baseFormSynset.addAll(tokenList);
				for (String token : tokenList){
					baseFormSynset.remove(token);
					for (String item : baseFormSynset){
						addToHashmap(verbs, token, baseFormSynset); // add extensions
						if (verbs.get(item) != null){
							addToHashmap(verbs, token, verbs.get(item) ); //add synonyms of extentions
						}
					}
				}
				break;
			default:
				System.out.println("Type > 8. Undefined type!");
				break;
		}
	}

	 /***Adds a  List (HashSet) of synonyms of a single token to a HashMap
	 *
	 * @param hashMap
	 * @param token
	 * @param synonymList
	 */
	private void addToHashmap(THashMap<String, THashSet<String>> hashMap, String token, THashSet<String> synonymList) {
		if (hashMap.containsKey(token)) {
			hashMap.get(token).addAll(synonymList);
			hashMap.get(token).remove(token);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			THashSet<String> tempHashSet = new THashSet<>();
			tempHashSet.addAll(synonymList);
			tempHashSet.remove(token); //remove itself from Hashset
			hashMap.put(token, tempHashSet);
		}
	}

	/***
	 * Merges all HashMaps into allSynonyms HashMap
	 */
	//private void mergeLists(){
	//	ArrayList<THashMap<String, THashSet<String>>> allLists = new ArrayList<>();
	//	allLists.add(adjectives);
	//	allLists.add(adverbs);
	//	allLists.add(verbs);
	//
	//	for (THashMap<String, THashSet<String>> list : allLists){
	//		for (Map.Entry<String, THashSet<String>> synsetEntry : list.entrySet()) {
	//			if (!allSynonyms.containsKey(synsetEntry.getKey())) {
	//				allSynonyms.put(synsetEntry.getKey(), synsetEntry.getValue());
	//			} else {
	//				allSynonyms.get(synsetEntry.getKey()).addAll(synsetEntry.getValue());
	//			}
	//		}
	//	}
	//
	//}

	/***
	 * Merges all HashMaps into allSynonyms HashMap
	 */
	private void mergeLists() {
		ArrayList<THashMap<String, THashSet<String>>> allLists = new ArrayList<>();
		allLists.add(adjectives);
		allLists.add(adverbs);
		allLists.add(verbs);

		for (THashMap<String, THashSet<String>> list : allLists) {

			for (Map.Entry<String, THashSet<String>> entry : list.entrySet()) {

				if (allSynonyms.containsKey(entry.getKey())) {
					//System.out.println(entry.getKey());
					allSynonyms.get(entry.getKey()).addAll(entry.getValue());
				} else {
					allSynonyms.put(entry.getKey(), entry.getValue());
				}

			}
		}
	}

	/**
	 * A method for reading the textual movie plot file and building a Lucene index.
	 * The purpose of the index is to speed up subsequent boolean searches using
	 * the {@link #booleanQuery(String) booleanQuery} method.
	 *
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param plotFile
	 *          the textual movie plot file 'plot.list', obtainable from <a
	 *          href="http://www.imdb.com/interfaces"
	 *          >http://www.imdb.com/interfaces</a> for personal, non-commercial
	 *          use.
	 */

	public void buildIndices(String plotFile) {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {

			String line;  // current input (line)
			boolean lastLineWasPlot = false; // check if new MV Line starts
			StringBuilder currentMovieString = new StringBuilder();  // concatenate the plotstring


			// Specify a directory (happend earlier) and an index writer
			IndexWriterConfig config = new IndexWriterConfig(myAnalyzer);
			// open a new IndexWriter instance
			IndexWriter writer = new IndexWriter(index, config);
			// Create a document (and add this document to the index, later):
			Document doc = new Document();

			while ((line = reader.readLine()) != null){
				// recognize MV: lines and handle them
				// new movie starts --> write old currentMovieString to Index and reset doc and currentMovieString
				if (line.startsWith("MV:") && (lastLineWasPlot)) {
					doc.add(new TextField("plot", currentMovieString.toString(), Field.Store.YES));
					writer.addDocument(doc);
					currentMovieString = new StringBuilder();
					doc = new Document();
					lastLineWasPlot = false;
				}
				// handle the new movie line
				if (line.startsWith("MV:") && (!lastLineWasPlot)) {
					doc.add(new TextField("movieline", line, Field.Store.YES));
					getTitleTypeYear(doc, line.substring(4, line.length()));
				}
				// detect and handle the (new) plotline
				if (line.startsWith("PL:")) {
					currentMovieString.append(line.substring(4, line.length()));
					currentMovieString.append(" ");  // Space necessary at the end of each plotline!
					lastLineWasPlot = true;
				}
			}
			// after last line was read from file - last write operation has to be triggered!
			doc.add(new TextField("plot", currentMovieString.toString(), Field.Store.YES));
			writer.addDocument(doc);
			// close index writer
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// helper Methods
	private void getTitleTypeYear(Document doc, String movieline) {

		// remove {{SUSPENDED}}
		if (movieline.contains("{{suspended}}")) {
			movieline = movieline.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (movieline.startsWith("\"") && !movieline.endsWith("}")) {
			doc.add((new TextField("type", "series", Field.Store.YES)));
			parseTitleAndYear(doc, movieline, true);
		}
		// +++ episode +++
		else if (movieline.contains("\"") && movieline.endsWith("}")) {
			doc.add((new TextField("type", "episode", Field.Store.YES)));

			StringBuilder currentString = new StringBuilder();

			for (int i = movieline.length() - 2; movieline.charAt(i) != '{'; --i) {
				currentString.append(movieline.charAt(i));
			}

			doc.add((new TextField("episodetitle",
				new StringBuilder(currentString.toString()).reverse().toString(), Field.Store.YES)));

			parseTitleAndYear(doc, movieline.substring(0, movieline.indexOf('{') - 1), true);
		}
		// +++ television +++
		else if (movieline.contains(") (TV)")) {
			doc.add((new TextField("type", "television", Field.Store.YES)));
			parseTitleAndYear(doc, movieline.substring(0, movieline.length() - 5), false);
		}
		// +++ video +++
		else if (movieline.contains(") (V)")) {
			doc.add((new TextField("type", "video", Field.Store.YES)));
			parseTitleAndYear(doc, movieline.substring(0, movieline.length() - 4), false);
		}
		// +++ video game +++
		else if (movieline.contains(") (VG)")) {
			doc.add((new TextField("type", "videogame", Field.Store.YES)));
			parseTitleAndYear(doc, movieline.substring(0, movieline.length() - 5), false);
		} else {
			// +++ movie +++
			doc.add((new TextField("type", "movie", Field.Store.YES)));
			parseTitleAndYear(doc, movieline, false);
		}
	}

	// helper 2
	private void parseTitleAndYear(Document doc, String movieline, boolean isSeries) {
		int end = 3;
		StringBuilder currentString = new StringBuilder();

		for (int i = movieline.length() - 2; movieline.charAt(i) != '('; --i) {
			if (movieline.charAt(i) >= '0' && movieline.charAt(i) <= '9') {
				currentString.append(movieline.charAt(i));
			}
			end++;
		}
		doc.add((new StringField("year", new StringBuilder(currentString.toString()).reverse().toString(), Field.Store.YES)));

		if (isSeries) {
			doc.add((new TextField("title", movieline.substring(1, movieline.length() - end - 1), Field.Store.YES)));
		} else {
			doc.add((new TextField("title", movieline.substring(0, movieline.length() - end), Field.Store.YES)));
		}
	}

	private String parseQueryString(String originalQuery) {
		HashSet<String> toModify = new HashSet<>();
		String modString = originalQuery;
		Pattern plPattern = Pattern.compile("plot:[^ )]*");
		Pattern tlPattern = Pattern.compile("title:[^ )]*");
		Matcher matcher = plPattern.matcher(originalQuery);
		while (matcher.find()) {
			toModify.add(matcher.group(0));
		}
		matcher = tlPattern.matcher((originalQuery));
		while (matcher.find()) {
			toModify.add(matcher.group(0));
		}
		for (String picked : toModify) {
			String prefix;
			String suffix;
			if (picked.startsWith("t")) {
				prefix = "title:";
				suffix = picked.substring(6);
			}
			else {
				prefix = "plot:";
				suffix = picked.substring(5);
			}
			suffix = suffix.toLowerCase();
			StringBuilder st = new StringBuilder();
			if (allSynonyms.containsKey(suffix) &&!allSynonyms.get(suffix).isEmpty()) {
				st.append("(").append(picked.toLowerCase());
				for (String synonym : allSynonyms.get(suffix)) {
					st.append(" OR ").append(prefix).append(synonym);
				}
				st.append(")");
			}
			else {
				st.append(picked);
			}
			modString = modString.replaceAll(picked, st.toString());
		}
		return modString;
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
	 *
	 * More details on the query syntax can be found at <a
	 * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
	 * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
	 *
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param queryString
	 *          the query string, formatted according to the Lucene query syntax.
	 * @return the exact content (in the textual movie plot file) of the title
	 *         lines (starting with "MV: ") of the documents matching the query
	 */
	public Set<String> booleanQuery(String queryString) {
		HashSet<String> results = new HashSet<>();

		try {
			IndexReader indexReader = DirectoryReader.open(index);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			Query qry = new QueryParser("title", myAnalyzer).parse(parseQueryString(queryString));
			TopDocs hits = indexSearcher.search(qry, Integer.MAX_VALUE);
			for(ScoreDoc scoreDoc : hits.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				results.add(doc.get("movieline"));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return results;
	}

	/**
	 * A method for closing any open file handels or a ThreadPool.
	 *
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 */
	public void close() {
		// TODO: you may insert code here
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err
				.println("usage: java -jar BooleanQueryWordnet.jar <plot list file> <wordnet directory> <queries file> <results file>");
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

		System.out
			.println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
		System.out
			.println("memory: " + ((runtime.totalMemory() - mem) / (1048576l))
				+ " MB (rough estimate)");

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
			Comparator<String> stringComparator = new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}
			};
			expectedResultSorted.sort(stringComparator);
			actualResultSorted.sort(stringComparator);

			System.out.println("runtime:         " + (System.nanoTime() - tic)
				+ " nanoseconds.");
			System.out.println("expected result (" + expectedResultSorted.size() + "): " + expectedResultSorted.toString());
			System.out.println("actual result (" + actualResultSorted.size() + "):   " + actualResultSorted.toString());
			System.out.println(expectedResult.equals(actualResult) ? "SUCCESS"
				: "FAILURE");
		}

		bq.close();
	}

}
