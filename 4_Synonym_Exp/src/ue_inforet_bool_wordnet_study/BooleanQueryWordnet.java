// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_wordnet_study;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Document;
import java.util.Map;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BooleanQueryWordnet {
	// global accessible index :)
	Directory index = new RAMDirectory();
	// set Analyzer
	Analyzer myAnalyzer = new StandardAnalyzer();


	private static THashMap<String, THashSet<String>> allSynonyms = new THashMap<>();
	private static THashMap<String, THashSet<String>> adverbs = new THashMap<>();
	private static THashMap<String, THashSet<String>> adjectives = new THashMap<>();
	private static THashMap<String, THashSet<String>> verbs = new THashMap<>();

	/**
	 * DO NOT ADD ADDITIONAL PARAMETERS TO THE SIGNATURE
	 * OF THE CONSTRUCTOR.
	 *
	 */
	public BooleanQueryWordnet() {
		// TODO you may insert code here
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
		// TODO: insert code here
	}

	/***Adds a list of token into the fitting Hashmap
	 *
	 * @param tokenList
	 * @param type
	 */
	public static void synDex (THashSet<String> tokenList, int type){
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
	private static void addToHashmap(THashMap<String, THashSet<String>> hashMap, String token, THashSet<String> synonymList) {
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
	private static void mergeLists(){
		ArrayList<THashMap<String, THashSet<String>>> allLists = new ArrayList<>();
		allLists.add(adjectives);
		allLists.add(adverbs);
		allLists.add(verbs);

		for (THashMap<String, THashSet<String>> list : allLists){
			for (Map.Entry<String, THashSet<String>> synsetEntry : list.entrySet()) {
				if (!allSynonyms.containsKey(synsetEntry.getKey())) {
					allSynonyms.put(synsetEntry.getKey(), synsetEntry.getValue());
				} else {
					allSynonyms.get(synsetEntry.getKey()).addAll(synsetEntry.getValue());
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
		// TODO: insert code here
	}

	public String parseQueryString(String originalQuery) {
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
					st.append(" OR ").append(prefix+synonym);
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
				results.add(doc.get("mvline"));
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
