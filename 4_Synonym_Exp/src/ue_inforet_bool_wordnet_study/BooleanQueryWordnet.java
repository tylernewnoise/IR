// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_wordnet_study;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;


public class BooleanQueryWordnet {

 	private THashMap<String, THashSet<String>> allSynonyms = new THashMap<>();
	private THashMap<String, THashSet<String>> verbs = new THashMap<>();
 	private THashMap<String, THashSet<String>> adjectivs = new THashMap<>();
 	private THashMap<String, THashSet<String>> adverbs = new THashMap<>();

 	// Parse Data Files
	private void parseData(String fileName, int type) {
		int wordCount = 0;
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
				String[] justSplit = StringUtils.substring(line, 18).split( Pattern.quote( " 0 " ) );
				// read/process the split words

				if (justSplit[0].contains("_")) {   // does this one word is made of >=2 tokens?
					continue; // leave this line alone
				}

				THashSet<String> tmp =  new THashSet();
				for (int i=0; i < wordCount; i++){
					if (justSplit[i].contains("_")) {   // does this one word is made of >=2 tokens?
						continue; // leave this word alone
					}
					// adjective?
					if (adjective && (justSplit[i].endsWith("(p)") || justSplit[i].endsWith("(a)") || justSplit[i].endsWith("(ip)"))){
						if (justSplit[i].endsWith("(p)") || justSplit[i].endsWith("(a)")){
							justSplit[i] = StringUtils.substring(justSplit[i], 0, justSplit[i].length() - 3);
						}
						else justSplit[i] = StringUtils.substring(justSplit[i], 0, justSplit[i].length() - 4);
					}
					tmp.add(justSplit[i].toLowerCase());
				}
				synDex(tmp, type); // run bennys magic
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}


	// Parse Exc Files
	private void parseExc(String fileName, int type) {
		int wordCount = 0;

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

				THashSet<String> tmp =  new THashSet();
				for (int i=0; i < wordCount; i++){
					if (justSplit[i].contains("_")) {   // does this one word is made of >=2 tokens?
						continue; // leave this word alone
					}
					tmp.add(justSplit[i].toLowerCase());
				}
				synDex(tmp, type); // run bennys magic :)
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
		parseData(wordnetDir + "data.noun", 3);
		parseData(wordnetDir + "data.verb", 4);
		parseData(wordnetDir + "data.adj", 1);
		parseData(wordnetDir + "data.adv", 2);
		parseExc(wordnetDir + "noun.exc", 7);
		parseExc(wordnetDir + "verb.exc", 8);
		parseExc(wordnetDir + "adj.exc", 5);
		parseExc(wordnetDir + "adv.exc", 6);

		mergeLists(adverbs);
		mergeLists(verbs);
		mergeLists(adjectivs);
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
		// TODO: insert code here
		return new HashSet<>();
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
