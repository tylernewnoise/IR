package ue_inforet_bool_variant;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BooleanQueryVariant {
	private ArrayList<String> allMovies = new ArrayList<>();
	private HashMap<Integer, String> allPlotPhrases = new HashMap<>();
	private HashMap<Integer, String> allTitlePhrases = new HashMap<>();
	private HashMap<Integer, String> allEpisodeTitlePhrases = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashYear = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>();
	private String tokenPhrase = "";
	private String token = "";

	/**
	 * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
	 */
	public BooleanQueryVariant() {
	}

	/* ******** Boyer Moore ******** */
	/* thx to en.wikipedia.org */
	/**
	 * Returns the index within this string of the first occurrence of the
	 * specified substring. If it is not a substring, return -1.
	 *
	 * @param haystack The string to be scanned
	 * @param needle   The target string to search
	 * @return The start index of the substring
	 */
	private static int boyerMoore(char[] haystack, char[] needle) {
		if (needle.length == 0) {
			return 0;
		}
		int charTable[] = makeCharTable(needle);
		int offsetTable[] = makeOffsetTable(needle);
		for (int i = needle.length - 1, j; i < haystack.length; ) {
			for (j = needle.length - 1; needle[j] == haystack[i]; --i, --j) {
				if (j == 0) {
					return i;
				}
			}
			i += needle.length - j; // For naive method
			//i += Math.max(offsetTable[needle.length - 1 - j], charTable[haystack[i]]);
		}
		return -1;
	}

	/**
	 * Makes the jump table based on the mismatched character information.
	 */
	private static int[] makeCharTable(char[] needle) {
		final int ALPHABET_SIZE = 256;
		int[] table = new int[ALPHABET_SIZE];
		for (int i = 0; i < table.length; ++i) {
			table[i] = needle.length;
		}
		for (int i = 0; i < needle.length - 1; ++i) {
			table[needle[i]] = needle.length - 1 - i;
		}
		return table;
	}

	/**
	 * Makes the jump table based on the scan offset which mismatch occurs.
	 */
	private static int[] makeOffsetTable(char[] needle) {
		int[] table = new int[needle.length];
		int lastPrefixPosition = needle.length;
		for (int i = needle.length - 1; i >= 0; --i) {
			if (isPrefix(needle, i + 1)) {
				lastPrefixPosition = i + 1;
			}
			table[needle.length - 1 - i] = lastPrefixPosition - i + needle.length - 1;
		}
		for (int i = 0; i < needle.length - 1; ++i) {
			int slen = suffixLength(needle, i);
			table[slen] = needle.length - 1 - i + slen;
		}
		return table;
	}

	/**
	 * Is needle[p:end] a prefix of needle?
	 */
	private static boolean isPrefix(char[] needle, int p) {
		for (int i = p, j = 0; i < needle.length; ++i, ++j) {
			if (needle[i] != needle[j]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the maximum length of the substring ends at p and is a suffix.
	 */
	private static int suffixLength(char[] needle, int p) {
		int len = 0;
		for (int i = p, j = needle.length - 1;
		     i >= 0 && needle[i] == needle[j]; --i, --j) {
			len += 1;
		}
		return len;
	}
	/* ******** Boyer Moore ******** */

	/**
	 * A method for reading the textual movie plot file and building indices. The
	 * purpose of these indices is to speed up subsequent boolean searches using
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
		int nextMovieID = 0;
		int movieID = 0;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("M")) {
					if (!tokenPhrase.isEmpty()) {
						allPlotPhrases.put(movieID, tokenPhrase);
						tokenPhrase = "";
					}
					movieID = nextMovieID++;
					allMovies.add(movieID, line);
					addTitleTypeYearToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
				}
				if (StringUtils.substring(line, 0, 3).contains("PL:")) {
					addPlotLineToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (!tokenPhrase.isEmpty()) {
			allPlotPhrases.put(movieID, tokenPhrase);
		}
	}

	private void addToHashMap(HashMap<String, HashSet<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			HashSet<Integer> movieList = new HashSet<>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	private void addPlotLineToHashMap(Integer movieID, String line) {
		token = "";

		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == ' ' || line.charAt(i) == '.' || line.charAt(i) == ':' ||
				line.charAt(i) == '?' || line.charAt(i) == '!' || line.charAt(i) == ',') {

				if (!token.isEmpty()) {
					tokenPhrase += token + " ";
					addToHashMap(hashPlot, token, movieID);
				}
				token = "";
				continue;
			}
			token += line.charAt(i);
		}

		if (!token.isEmpty()) {
			tokenPhrase += token + " ";
			addToHashMap(hashPlot, token, movieID);
		}
	}

	private void addTitleTypeYearToHashMap(int movieID, String mvLine) {

		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			addToHashMap(hashType, "series", movieID);
			parseTitleAndYear(movieID, mvLine, true);
			return;
		}
		if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			addToHashMap(hashType, "episode", movieID);

			StringTokenizer st = new StringTokenizer(mvLine.substring(mvLine.indexOf('{') + 1, mvLine.length() - 1), " .,:!?", false);
			token = "";

			while (st.hasMoreTokens()) {
				token = st.nextToken();
				if (token.contains("\"")) {
					tokenPhrase += token + " ";
					continue;
				}
				addToHashMap(hashEpisodeTitle, token, movieID);
				tokenPhrase += token + " ";
			}

			allEpisodeTitlePhrases.put(movieID, tokenPhrase);
			tokenPhrase = "";

			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.indexOf('{')), true);
			return;
		}
		if (mvLine.contains(") (tv)")) {
			addToHashMap(hashType, "television", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
			return;
		}
		if (mvLine.contains(") (v)")) {
			addToHashMap(hashType, "video", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 5), false);
			return;
		}
		if (mvLine.contains(") (vg)")) {
			addToHashMap(hashType, "videogame", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
			return;
		}
		addToHashMap(hashType, "movie", movieID);
		parseTitleAndYear(movieID, mvLine, false);
	}

	private void parseTitleAndYear(int movieID, String mvLine, boolean isSeries) {
		token = "";
		int end = 3;

		for (int i = mvLine.length() - 1; mvLine.charAt(i) != '('; i--) {
			if (mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9') {
				token += mvLine.charAt(i);
			}
			end++;
		}
		if (token.length() == 4) {
			addToHashMap(hashYear, new StringBuilder(token).reverse().toString(), movieID);
		}
		StringTokenizer st;
		if (isSeries) {
			st = new StringTokenizer(mvLine.substring(1, mvLine.length() - end), " .,:!?", false);
		} else {

			st = new StringTokenizer(mvLine.substring(0, mvLine.length() - end + 1), " .,:!?", false);
		}

		token = "";
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			if (token.contains("\"")) {
				tokenPhrase += token + " ";
				continue;
			}
			addToHashMap(hashTitle, token, movieID);
			tokenPhrase += token + " ";
		}
		allTitlePhrases.put(movieID, tokenPhrase);
		tokenPhrase = "";
	}

	/**
	 * A method for performing a boolean search on a textual movie plot file after
	 * indices were built using the {@link #buildIndices(String) buildIndices}
	 * method. The movie plot file contains entries of the <b>types</b> movie,
	 * series, episode, television, video, and videogame. This method allows term
	 * and phrase searches (the latter being enclosed in double quotes) on any of
	 * the <b>fields</b> title, plot, year, episode, and type. Multiple term and
	 * phrase searches can be combined by using the character sequence " AND ".
	 * Note that queries are case-insensitive.<br>
	 * <br>
	 * Examples of queries include the following:
	 * <p>
	 * <pre>
	 * title:"game of thrones" AND type:episode AND plot:shae AND plot:Baelish
	 * plot:Skywalker AND type:series
	 * plot:"year 2200"
	 * plot:Berlin AND plot:wall AND type:television
	 * plot:Cthulhu
	 * title:"saber rider" AND plot:april
	 * plot:"James Bond" AND plot:"Jaws" AND type:movie
	 * title:"Pimp my Ride" AND episodetitle:mustang
	 * plot:"matt berninger"
	 * title:"grand theft auto" AND type:videogame
	 * plot:"Jim Jefferies"
	 * plot:Berlin AND type:videogame
	 * plot:starcraft AND type:movie
	 * type:video AND title:"from dusk till dawn"
	 * </pre>
	 * <p>
	 * More details on (a superset of) the query syntax can be found at <a
	 * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
	 * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param queryString the query string, formatted according to the Lucene query syntax,
	 *                    but only supporting term search, phrase search, and the AND
	 *                    operator
	 * @return the exact content (in the textual movie plot file) of the title
	 * lines (starting with "MV: ") of the documents matching the query
	 */
	public Set<String> booleanQuery(String queryString) {
		HashSet<String> results = new HashSet<>();

		if (queryString.contains(" AND ")) {
			ArrayList<Integer> matchingMovies = new ArrayList<>();
			String singleQuery[] = queryString.split(" AND ");
			int howManyQueries = 0;

			for (String tmp : singleQuery) {
				if (tmp.contains("\"")) {
					for (int i : phraseSearch(tmp)) {
						matchingMovies.add(i);
					}
				} else {
					for (int i : singleTokenSearch(tmp)) {
						matchingMovies.add(i);
					}
				}
				howManyQueries++;
			}

			HashMap<Integer, Integer> countMatchingMovies = new HashMap<>();
			for (int i : matchingMovies) {
				Integer cnt = countMatchingMovies.get(i);
				countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1);
			}

			ArrayList<Map.Entry<Integer, Integer>> copyAndSort = new ArrayList<>();
			copyAndSort.addAll(countMatchingMovies.entrySet());
			copyAndSort.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

			for (Map.Entry<Integer, Integer> entry : copyAndSort) {
				if (entry.getValue() != howManyQueries) {
					break;
				}
				results.add(allMovies.get(entry.getKey()));
			}
		} else if (queryString.contains("\"")) {
			for (int i : phraseSearch(queryString)) {
				results.add(allMovies.get(i));
			}
		} else {
			for (int i : singleTokenSearch(queryString)) {
				results.add(allMovies.get(i));
			}
		}
		return results;
	}

	private List<Integer> phraseSearch(String phraseString) {
		ArrayList<Integer> matchingMovies = new ArrayList<>();

		char fieldTypePhraseQuery = getFieldType(phraseString);

		if (fieldTypePhraseQuery == 'i') {
			phraseString = StringUtils.substring(phraseString, 7, phraseString.length() - 1).toLowerCase();
		} else if (fieldTypePhraseQuery == 'p' || fieldTypePhraseQuery == 't' || fieldTypePhraseQuery == 'y') {
			phraseString = StringUtils.substring(phraseString, 6, phraseString.length() - 1).toLowerCase();
		} else {
			phraseString = StringUtils.substring(phraseString, 14, phraseString.length() - 1).toLowerCase();
		}

		int howManyTokens = 0;
		ArrayList<Integer> foundMoviesWithTokensFromPhrases = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(phraseString, " .,:!", false);

		while (st.hasMoreTokens()) {
			for (int i : tokenSearch(st.nextToken(), fieldTypePhraseQuery)) {
				foundMoviesWithTokensFromPhrases.add(i);
			}
			howManyTokens++;
		}

		HashMap<Integer, Integer> countMatchingMovies = new HashMap<>(64);
		for (int i : foundMoviesWithTokensFromPhrases) {
			Integer cnt = countMatchingMovies.get(i);
			countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1);
		}

		ArrayList<Map.Entry<Integer, Integer>> sortedMatchingMovies = new ArrayList<>();
		sortedMatchingMovies.addAll(countMatchingMovies.entrySet());
		sortedMatchingMovies.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		for (Map.Entry<Integer, Integer> entry : sortedMatchingMovies) {
			if (entry.getValue() != howManyTokens) {
				break;
			}

			if (fieldTypePhraseQuery == 'i') {
				if (boyerMoore(allTitlePhrases.get(entry.getKey()).toCharArray(), phraseString.toCharArray()) > -1) {
					matchingMovies.add(entry.getKey());
				}
			} else if (fieldTypePhraseQuery == 'p') {
				if (boyerMoore(allPlotPhrases.get(entry.getKey()).toCharArray(), phraseString.toCharArray()) > -1) {
					matchingMovies.add(entry.getKey());
				}
			} else {
				if (boyerMoore(allEpisodeTitlePhrases.get(entry.getKey()).toCharArray(), phraseString.toCharArray()) > -1) {
					matchingMovies.add(entry.getKey());
				}
			}
		}
		return matchingMovies;
	}

	private List<Integer> singleTokenSearch(String queryString) {
		ArrayList<Integer> machtingMovies = new ArrayList<>();
		if (getFieldType(queryString) == 'i') {
			for (int i : tokenSearch(StringUtils.substring(queryString, 6, queryString.length()).toLowerCase(), 'i')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 'p') {
			for (int i : tokenSearch(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase(), 'p')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 't') {
			for (int i : tokenSearch(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase(), 't')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 'y') {
			for (int i : tokenSearch(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase(), 'y')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 'e') {
			for (int i : tokenSearch(StringUtils.substring(queryString, 13, queryString.length()).toLowerCase(), 'e')) {
				machtingMovies.add(i);
			}
		}
		return machtingMovies;
	}

	private char getFieldType(String queryString) {
		// title
		if (queryString.indexOf("i") == 1) {
			return 'i';
		}
		// plot
		if (queryString.startsWith("p")) {
			return 'p';
		}
		// type
		if (queryString.indexOf("y") == 1) {
			return 't';
		}
		// year
		if (queryString.startsWith("y")) {
			return 'y';
		}
		// episode title
		if (queryString.startsWith("e")) {
			return 'e';
		}
		return ' ';
	}

	private List<Integer> tokenSearch(String tokenString, char searchField) {
		ArrayList<Integer> matchingMovies = new ArrayList<>();

		if (searchField == 'i') {
			if (hashTitle.containsKey(tokenString)) {
				for (int i : hashTitle.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		if (searchField == 'p') {
			if (hashPlot.containsKey(tokenString)) {
				for (int i : hashPlot.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		if (searchField == 't') {
			if (hashType.containsKey(tokenString)) {
				for (int i : hashType.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		if (searchField == 'y') {
			if (hashYear.containsKey(tokenString)) {
				for (int i : hashYear.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		if (searchField == 'e') {
			if (hashEpisodeTitle.containsKey(tokenString)) {
				for (int i : hashEpisodeTitle.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		return matchingMovies;
	}

	public static void main(String[] args) {
		BooleanQueryVariant bq = new BooleanQueryVariant();
		if (args.length < 3) {
			System.err
				.println("usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
			System.exit(-1);
		}

		// build indices
		System.out.println("building indices...");
		long tic = System.nanoTime();
		Runtime runtime = Runtime.getRuntime();
		long mem = runtime.totalMemory();
		bq.buildIndices(args[0]);
		System.out
			.println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
		System.out
			.println("memory: " + ((runtime.totalMemory() - mem) / (1048576L))
				+ " MB (rough estimate)");

		// parsing the queries that are to be run from the queries file
		List<String> queries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(args[1]), StandardCharsets.ISO_8859_1))) {
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
			new FileInputStream(args[2]), StandardCharsets.ISO_8859_1))) {
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
			System.out.println("expected result: " + expectedResultSorted.toString());
			System.out.println("actual result:   " + actualResultSorted.toString());
			System.out.println(expectedResult.equals(actualResult) ? "SUCCESS"
				: "FAILURE");
		}
	}
}
