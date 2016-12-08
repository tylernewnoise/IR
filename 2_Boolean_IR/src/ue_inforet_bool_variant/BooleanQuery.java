package ue_inforet_bool_variant;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class BooleanQuery {
	private ArrayList<String> allMoviesList = new ArrayList<>();
	private HashMap<Integer, String> allPlotPhrases = new HashMap<>();
	private HashMap<Integer, String> allTitlePhrases = new HashMap<>();
	private HashMap<Integer, String> allEpisodeTitlePhrases = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashYear = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>();

	/**
	 * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
	 */
	public BooleanQuery() {
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
			//i += needle.length - j;  For naive method
			i += Math.max(offsetTable[needle.length - 1 - j], charTable[haystack[i]]);
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
		boolean isPlotLine = false;
		hashType.put("video", new HashSet<>());
		hashType.put("movie", new HashSet<>());
		hashType.put("series", new HashSet<>());
		hashType.put("episode", new HashSet<>());
		hashType.put("videogame", new HashSet<>());
		hashType.put("television", new HashSet<>());

		StringBuilder stringBuilder = new StringBuilder();

		/* read from the file - thanks Christoph */
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				// is it an MV: line?
				if (line.startsWith("M")) {
					if (isPlotLine) {
						allPlotPhrases.put(movieID, stringBuilder.toString());
						isPlotLine = false;
						stringBuilder = new StringBuilder();
					}
					// add an entry and increase movieID
					movieID = nextMovieID++;
					allMoviesList.add(movieID, line);
					// add movie to list and add title, type and year to the hash maps
					// remove 'MV: ' first and convert everything toLowerCase()
					getTitleTypeYear(movieID, StringUtils.substring(line, 4,
						line.length()).toLowerCase());
				}
				// is it an PL: line?
				if (StringUtils.substring(line, 0, 3).contains("PL:")) {
					isPlotLine = true;

					StringTokenizer st = new StringTokenizer(StringUtils.substring(line, 4,
						line.length()).toLowerCase(), " .,:!?", false);

					// now tokenize the plot - thanks Jonas
					while (st.hasMoreTokens()) {
						String token = st.nextToken();
						if (token.contains("\"")) {
							stringBuilder.append(token);
							stringBuilder.append(" ");
							continue;
						}
						addPlotTitleYearToHashMap(hashPlot, token, movieID);
						stringBuilder.append(token);
						stringBuilder.append(" ");
					}
				}
			}
			// add the last plot phrase
			allPlotPhrases.put(movieID, stringBuilder.toString());
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/* adds the movie and it's values to the hash map - thanks Benny */
	private void addPlotTitleYearToHashMap(HashMap<String, HashSet<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			HashSet<Integer> movieList = new HashSet<>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	/* gets the year, title, episode title out of the string to the hash map */
	private void getTitleTypeYear(int movieID, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			// add the type to the hash map
			hashType.get("series").add(movieID);
			//addYearToHashMap(hashType, "series", movieID);
			parseTitleAndYear(movieID, mvLine, true);
		}
		// +++ episode +++
		else if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			//addYearToHashMap(hashType, "episode", movieID);
			hashType.get("episode").add(movieID);

			// first get the episode title and add it to the list. we know form grep that { is never
			// in a title/year, so we can start there.
			StringTokenizer st = new StringTokenizer(mvLine.substring(mvLine.indexOf('{') + 1,
				mvLine.length() - 1), " .,:!?", false);

			StringBuilder stringBuilder = new StringBuilder();

			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.contains("\"")) {
					stringBuilder.append(token);
					stringBuilder.append(" ");
					continue;
				}
				addPlotTitleYearToHashMap(hashEpisodeTitle, token, movieID);
				stringBuilder.append(token);
				stringBuilder.append(" ");
			}

			// add the token for the phrase search to the array list
			allEpisodeTitlePhrases.put(movieID, stringBuilder.toString());

			// get rid of the episode title. and proceed like it is an episode
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.indexOf('{')), true);
		}
		// +++ television +++
		else if (mvLine.contains(") (tv)")) {
			//addYearToHashMap(hashType, "television", movieID);
			hashType.get("television").add(movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
		}
		// +++ video +++
		else if (mvLine.contains(") (v)")) {
			//addYearToHashMap(hashType, "video", movieID);
			hashType.get("video").add(movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 5), false);
		}
		// +++ video game +++
		else if (mvLine.contains(") (vg)")) {
			//addYearToHashMap(hashType, "videogame", movieID);
			hashType.get("videogame").add(movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
		} else {
			// +++ movie +++
			//addYearToHashMap(hashType, "movie", movieID);
			hashType.get("movie").add(movieID);
			parseTitleAndYear(movieID, mvLine, false);
		}
	}

	/* This method parses the year and the title. it starts with the year because it is always the last
	 * part of the 'MV: ' - line. in the method above we already cut off the the types and before the types is
	 * ALWAYS the year. after the year is parsed, we cut it off and parse the title from the start. we can
	 * do that because we already cut off everything else could lead to misinformation.
	 * for example: MV: Terminator (2033) (1995) -> MV: is already removed in buildIndices and after the year is
	 * parsed it is cut off and the title is now unique: 'Terminator (2033)' */
	private void parseTitleAndYear(int movieID, String mvLine, boolean isSeries) {
		String year = "";
		int end = 3;

		// first, get the year right from behind
		// we start behind the last parenthesis and add only digits to the token
		// with end we compute the end for the tokenizer
		for (int i = mvLine.length() - 1; mvLine.charAt(i) != '('; --i) {
			if (mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9') {
				year += mvLine.charAt(i);
			}
			end++;
		}
		// if the token contains exact 4 digits, we reverse it and add it to the hash map
		if (year.length() == 4) {
			addPlotTitleYearToHashMap(hashYear, new StringBuilder(year).reverse().toString(), movieID);
		}

		StringTokenizer st;

		// then parse the title and add the token to hash map similar to addTokenFromPlotList
		if (isSeries) {
			st = new StringTokenizer(mvLine.substring(1, mvLine.length() - end),
				" .,:!?", false);
		} else {
			st = new StringTokenizer(mvLine.substring(0, mvLine.length() - end + 1),
				" .,:!?", false);
		}

		StringBuilder stringBuilder = new StringBuilder(250);

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.contains("\"")) {
				stringBuilder.append(token);
				stringBuilder.append(" ");
				continue;
			}
			addPlotTitleYearToHashMap(hashTitle, token, movieID);
			stringBuilder.append(token);
			stringBuilder.append(" ");
		}

		allTitlePhrases.put(movieID, stringBuilder.toString());
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
		HashSet<String> results = new HashSet<>(32);

		/* QUERY IS AN AND SEARCH */
		if (queryString.contains(" AND ")) {
			ArrayList<Integer> matchingMovies = new ArrayList<>(256000);

			// split into single queries
			String singleQuery[] = queryString.split(" AND ");
			int howManyQueries = 0;

			// execute every query and safe the result in matchingMovies List
			// tmp is a single query and singleQuery[] a string array which contains
			// all queries
			for (String tmp : singleQuery) {
				if (tmp.contains("\"")) {
					for (int i : phraseQuerySearch(tmp.toLowerCase())) {
						matchingMovies.add(i);
					}
				} else {
					for (int i : singleTokenSearch(tmp.toLowerCase())) {
						matchingMovies.add(i);
					}
				}
				howManyQueries++;
			}

			// count the movies which fit the queries from above
			// if a movie matches all queries (f.e. 3 times) it is counted 3 times and if a
			// movie matches only 2 queries, it is counted only 2 times.
			HashMap<Integer, Integer> countMatchingMovies = new HashMap<>(256000);

			for (int i : matchingMovies) {
				Integer cnt = countMatchingMovies.get(i);
				countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1);
			}

			// run through the hash map and add only the movies which match the count of howManyQueries
			for (Map.Entry<Integer, Integer> entry : countMatchingMovies.entrySet()) {
				if (howManyQueries == entry.getValue()) {
					results.add(allMoviesList.get(entry.getKey()));
				}
			}

		} else if (queryString.contains("\"")) {
		/* QUERY IS A PHRASE SEARCH */
			for (int i : phraseQuerySearch(queryString.toLowerCase())) {
				results.add(allMoviesList.get(i));
			}

		} else {
		/* QUERY IS ONLY TOKEN SEARCH */
			for (int i : singleTokenSearch(queryString.toLowerCase())) {
				results.add(allMoviesList.get(i));
			}
		}

		return results;
	}

	/* perform phrase query search */
	private List<Integer> phraseQuerySearch(String queryString) {
		ArrayList<Integer> matchingMovies = new ArrayList<>(32);

		char fieldTypePhraseQuery;

		// depending on the field cut off the field
		if (queryString.indexOf("i") == 1) {
			// cut of 'title:'
			queryString = StringUtils.substring(queryString, 7, queryString.length() - 1);
			fieldTypePhraseQuery = 'i';
		} else if (queryString.startsWith("p")) {
			// cut of 'type:', 'year:', 'plot:'
			queryString = StringUtils.substring(queryString, 6, queryString.length() - 1);
			fieldTypePhraseQuery = 'p';
		} else {
			// cut of 'episodetitle'
			queryString = StringUtils.substring(queryString, 14, queryString.length() - 1);
			fieldTypePhraseQuery = 'e';
		}

		// now tokenize the phraseString
		StringTokenizer st = new StringTokenizer(queryString, " .,:!?", false);
		// make a list of movies in which at least one of the tokens appear
		ArrayList<Integer> foundMoviesWithTokensFromPhrases = new ArrayList<>(6000);

		// count how many tokens (aka terms) are in the phrase
		int howManyTokens = 0;

		while (st.hasMoreTokens()) {
			// we add every movie to the list in which we find at least one token, we  use our
			// tokenSearchForPhraseQuery Method for it
			for (int i : tokenSearchForPhraseQuery(st.nextToken(), fieldTypePhraseQuery)) {
				foundMoviesWithTokensFromPhrases.add(i);
			}
			howManyTokens++;
		}

		// now count the occurrence of the movies we got from our token search
		HashMap<Integer, Integer> countMatchingMovies = new HashMap<>();
		for (int i : foundMoviesWithTokensFromPhrases) {
			Integer cnt = countMatchingMovies.get(i);
			countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1); // if cnt == null add 1 else cnt + 1)
		}

		// run through the hash map and do string search only for movies which have
		// the same count as howManyTokens
		for (Map.Entry<Integer, Integer> entry : countMatchingMovies.entrySet()) {
			if (howManyTokens == entry.getValue()) {
				if (fieldTypePhraseQuery == 'i') {
					// search in title
					// the boyer-moore function returns -1 if the pattern is not found
					if (boyerMoore(allTitlePhrases.get(entry.getKey()).toCharArray(), queryString.toCharArray()) > -1) {
						matchingMovies.add(entry.getKey());
					}
				} else if (fieldTypePhraseQuery == 'p') {
					// search in plot
					if (boyerMoore(allPlotPhrases.get(entry.getKey()).toCharArray(), queryString.toCharArray()) > -1) {
						matchingMovies.add(entry.getKey());
					}
				} else {
					// search in episode title
					if (boyerMoore(allEpisodeTitlePhrases.get(entry.getKey()).toCharArray(), queryString.toCharArray()) > -1) {
						matchingMovies.add(entry.getKey());
					}
				}
			}
		}
		return matchingMovies;
	}

	/* perform a simple token search for a term of the phrase query */
	private List<Integer> tokenSearchForPhraseQuery(String tokenString, char searchField) {
		ArrayList<Integer> matchingMovies = new ArrayList<>();

		if (searchField == 'i') { // title
			if (hashTitle.containsKey(tokenString)) {
				for (int i : hashTitle.get(tokenString)) {
					matchingMovies.add(i);
				}
			}
		} else if (searchField == 'p') { // plot
			if (hashPlot.containsKey(tokenString)) {
				for (int i : hashPlot.get(tokenString)) {
					matchingMovies.add(i);
				}
			}
		} else if (searchField == 't') { // type
			if (hashType.containsKey(tokenString)) {
				for (int i : hashType.get(tokenString)) {
					matchingMovies.add(i);
				}
			}
		} else if (searchField == 'y') { // year
			if (hashYear.containsKey(tokenString)) {
				for (int i : hashYear.get(tokenString)) {
					matchingMovies.add(i);
				}
			}
		} else { // episode title
			if (hashEpisodeTitle.containsKey(tokenString)) {
				for (int i : hashEpisodeTitle.get(tokenString)) {
					matchingMovies.add(i);
				}
			}
		}
		return matchingMovies;
	}

	/* perform a single token search */
	private List<Integer> singleTokenSearch(String queryString) {
		ArrayList<Integer> matchingMovies = new ArrayList<>();

		// so first we want to know the field type we have to search in
		// depending on the field type, we cut off the field and look for the token in the hash map
		if (queryString.indexOf("i") == 1) { // title
			if (hashTitle.containsKey(StringUtils.substring(queryString, 6, queryString.length()))) {
				for (int i : hashTitle.get(StringUtils.substring(queryString, 6,
					queryString.length()))) {
					matchingMovies.add(i);
				}
			}
		} else if (queryString.startsWith("p")) { // plot
			if (hashPlot.containsKey(StringUtils.substring(queryString, 5, queryString.length()))) {
				for (int i : hashPlot.get(StringUtils.substring(queryString, 5,
					queryString.length()))) {
					matchingMovies.add(i);
				}
			}
		} else if (queryString.indexOf("y") == 1) { // type
			if (hashType.containsKey(StringUtils.substring(queryString, 5, queryString.length()))) {
				for (int i : hashType.get(StringUtils.substring(queryString, 5,
					queryString.length()))) {
					matchingMovies.add(i);
				}
			}
		} else if (queryString.startsWith("y")) { // year
			if (hashYear.containsKey(StringUtils.substring(queryString, 5, queryString.length()))) {
				for (int i : hashYear.get(StringUtils.substring(queryString, 5,
					queryString.length()))) {
					matchingMovies.add(i);
				}
			}
		} else { // episode title
			if (hashEpisodeTitle.containsKey(StringUtils.substring(queryString, 13, queryString.length()))) {
				for (int i : hashEpisodeTitle.get(StringUtils.substring(queryString, 13,
					queryString.length()))) {
					matchingMovies.add(i);
				}
			}
		}
		return matchingMovies;
	}

	public static void main(String[] args) {
		BooleanQuery bq = new BooleanQuery();
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
