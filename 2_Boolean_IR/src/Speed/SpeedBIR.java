package Speed;

//import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import com.eaio.stringsearch.BoyerMooreHorspoolRaita;
import com.eaio.stringsearch.StringSearch;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class SpeedBIR {
	private ArrayList<String> allMovies = new ArrayList<>(530000);
	private HashMap<Integer, String> allPlotPhrases = new HashMap<>(530000);
	private HashMap<Integer, String> allTitlePhrases = new HashMap<>(530000);
	private HashMap<Integer, String> allEpisodeTitlePhrases = new HashMap<>(220000);
	/*	private Int2ObjectOpenHashMap<String> allPlotPhrases = new Int2ObjectOpenHashMap<>(530000);
		private Int2ObjectOpenHashMap<String> allTitlePhrases = new Int2ObjectOpenHashMap<>(530000);
		private Int2ObjectOpenHashMap<String> allEpisodeTitlePhrases = new Int2ObjectOpenHashMap<>(220000);*/
	private HashMap<String, HashSet<Integer>> hashType = new HashMap<>(6);
	private HashMap<String, HashSet<Integer>> hashYear = new HashMap<>(150);
	private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>(700000);
	private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>(150000);
	private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>(100000);

	public SpeedBIR() {
	}

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
	private void buildIndices(String plotFile) {
		int nextMovieID = 0;
		int movieID = 0;
		boolean isPlotLine = false;

		StringBuilder stringBuilder = new StringBuilder();

		/* read from the file - thanks Christoph */
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {
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
					allMovies.add(movieID, line);
					// add movie to list and add title, type and year to the hash maps
					// remove 'MV: ' first and convert everything toLowerCase()
					addTitleTypeYearToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
				}
				// is it an PL: line?
				if (StringUtils.substring(line, 0, 3).contains("PL:")) {
					isPlotLine = true;

					StringTokenizer st = new StringTokenizer(StringUtils.substring(line, 4, line.length()).toLowerCase(), " .,:!", false);

					while (st.hasMoreTokens()) {
						String token = st.nextToken();
						if (token.contains("\"")) {
							stringBuilder.append(token);
							stringBuilder.append(" ");
							continue;
						}
						addToHashMap(hashPlot, token, movieID);
						stringBuilder.append(token);
						stringBuilder.append(" ");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		// add the last plot phrase
		allPlotPhrases.put(movieID, stringBuilder.toString());
	}

	/* adds the movie and it's values to the hash map - thanks Benny */
	private void addToHashMap(HashMap<String, HashSet<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			HashSet<Integer> movieList = new HashSet<>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	/**
	 * this method parses the year, title, episode title and adds it to the
	 * hash map. it basically works like this: first check the type (movie, tv, vg, ...).
	 * the episode type is a bit tricky: we have to parse the episode title first.
	 * after that we cut it off and parse the title and year like the other (see
	 * below on how to)
	 *
	 * @param movieID Gives each entry in our indices a unique number.
	 * @param mvLine  Contains all information for the different entries.
	 */
	private void addTitleTypeYearToHashMap(int movieID, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			// add the type to the hash map
			addToHashMap(hashType, "series", movieID);
			parseTitleAndYear(movieID, mvLine, true);
			// and we're done, no need to check the other if-statements
			return;
		}
		// +++ episode +++
		if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			addToHashMap(hashType, "episode", movieID);

			// first get the episode title and add it to the list. we know form grep that { is never
			// in a title/year, so we can start there. parsing is basically the same as in
			// addTokenFromPlotLine
			StringTokenizer st = new StringTokenizer(mvLine.substring(mvLine.indexOf('{') + 1, mvLine.length() - 1), " .,:!?", false);
			StringBuilder stringBuilder = new StringBuilder();

			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.contains("\"")) {
					stringBuilder.append(token);
					stringBuilder.append(" ");
					continue;
				}
				addToHashMap(hashEpisodeTitle, token, movieID);
				stringBuilder.append(token);
				stringBuilder.append(" ");
			}

			// add the token for the phrase search to the array list
			allEpisodeTitlePhrases.put(movieID, stringBuilder.toString());

			// get rid of the episode title. and proceed like it is an episode
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.indexOf('{')), true);
			return;
		}
		// +++ television +++
		if (mvLine.contains(") (tv)")) {
			addToHashMap(hashType, "television", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
			return;
		}
		// +++ video +++
		if (mvLine.contains(") (v)")) {
			addToHashMap(hashType, "video", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 5), false);
			return;
		}
		// +++ video game +++
		if (mvLine.contains(") (vg)")) {
			addToHashMap(hashType, "videogame", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
			return;
		}
		// +++ movie +++
		addToHashMap(hashType, "movie", movieID);
		parseTitleAndYear(movieID, mvLine, false);
	}

	/**
	 * This method parses the year and the title. it starts with the year because it is always the last
	 * part of the 'MV: ' - line. in the method above we already cut off the the types and before the types is
	 * ALWAYS the year. after the year is parsed, we cut it off and parse the title from the start. we can
	 * do that because we already cut off everything else could lead to misinformation.
	 * for example: MV: Terminator (2033) (1995) -> MV: is already removed in buildIndices and after the year is
	 * parsed it is cut off and the title is now unique: 'Terminator (2033)'
	 *
	 * @param movieID  the movie's unique id
	 * @param mvLine   the title containing all info about the movie
	 * @param isSeries boolean to determine if it's a series/episode or a movie
	 */
	private void parseTitleAndYear(int movieID, String mvLine, boolean isSeries) {
		String year = "";
		int end = 3;

		// first, get the year right from behind
		// we start behind the last parenthesis and add only digits to the token
		// with end we compute the end for the tokenizer
		for (int i = mvLine.length() - 1; mvLine.charAt(i) != '('; i--) {
			if (mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9') {
				year += mvLine.charAt(i);
			}
			end++;
		}
		// if the token contains exact 4 digits, we reverse it and add it to the hash map
		if (year.length() == 4) {
			addToHashMap(hashYear, new StringBuilder(year).reverse().toString(), movieID);
		}
		StringTokenizer st;
		// then parse the title and add the token to hash map similar to addTokenFromPlotList
		if (isSeries) {
			st = new StringTokenizer(mvLine.substring(1, mvLine.length() - end), " .,:!?", false);
		} else {
			st = new StringTokenizer(mvLine.substring(0, mvLine.length() - end + 1), " .,:!?", false);
		}

		StringBuilder stringBuilder = new StringBuilder();

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.contains("\"")) {
				stringBuilder.append(token);
				stringBuilder.append(" ");
				continue;
			}
			addToHashMap(hashTitle, token, movieID);
			stringBuilder.append(token);
			stringBuilder.append(" ");
		}

		allTitlePhrases.put(movieID, stringBuilder.toString());
	}


	private Set<String> booleanQuery(String queryString) {
		HashSet<String> results = new HashSet<>();

		/* QUERY IS AN AND SEARCH */
		if (queryString.contains(" AND ")) {
			ArrayList<Integer> matchingMovies = new ArrayList<>();
			// split the queries
			String singleQuery[] = queryString.split(" AND ");
			int howManyQueries = 0;

			// execute every query and safe the result in matchingMovies
			for (String tmp : singleQuery) {
				if (tmp.contains("\"")) {
					for (int i : phraseSearchSimple(tmp.toLowerCase())) {
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
			HashMap<Integer, Integer> countMatchingMovies = new HashMap<>(128);
			for (int i : matchingMovies) {
				Integer cnt = countMatchingMovies.get(i);
				countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1);
			}

			// sort the list for the count, the copyAndSort list is just a helper list
			// so we can sort the hash map
			ArrayList<Map.Entry<Integer, Integer>> copyAndSort = new ArrayList<>();
			copyAndSort.addAll(countMatchingMovies.entrySet());
			copyAndSort.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

			// than add all movies to the result list which matches the
			// query count (howManyQueries)
			for (Map.Entry<Integer, Integer> entry : copyAndSort) {
				if (entry.getValue() != howManyQueries) {
					break;
				}
				results.add(allMovies.get(entry.getKey()));
			}
		} else if (queryString.contains("\"")) {
		/* QUERY IS A PHRASE SEARCH */
			// call method for query search
			// we get a list with the matching movies
			for (int i : phraseSearchSimple(queryString.toLowerCase())) {
				results.add(allMovies.get(i));
			}
		} else {
		/* QUERY IS ONLY TOKEN SEARCH */
			for (int i : singleTokenSearch(queryString.toLowerCase())) {
				results.add(allMovies.get(i));
			}
		}
		return results;
	}

	/* perform a single token search */
	private List<Integer> singleTokenSearch(String queryString) {
		ArrayList<Integer> machtingMovies = new ArrayList<>(128);
		// so first we want to know the field type we have to search in
		// depending on the field type, we cut off the field and call tokenSearch
		if (getFieldType(queryString) == 'i') { // title
			for (int i : tokenSearch(StringUtils.substring(queryString, 6, queryString.length()), 'i')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 'p') { // plot
			for (int i : tokenSearch(StringUtils.substring(queryString, 5, queryString.length()), 'p')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 't') { // type
			for (int i : tokenSearch(StringUtils.substring(queryString, 5, queryString.length()), 't')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 'y') { // year
			for (int i : tokenSearch(StringUtils.substring(queryString, 5, queryString.length()), 'y')) {
				machtingMovies.add(i);
			}
		} else if (getFieldType(queryString) == 'e') { // episode title
			for (int i : tokenSearch(StringUtils.substring(queryString, 13, queryString.length()), 'e')) {
				machtingMovies.add(i);
			}
		}
		return machtingMovies;
	}

	private List<Integer> phraseSearchSimple(String phraseString) {
		ArrayList<Integer> matchingMovies = new ArrayList<>();

		// get the field
		char fieldTypePhraseQuery = getFieldType(phraseString);

		// depending on the field cut off the field
		if (fieldTypePhraseQuery == 'i') {
			// cut of 'title:'
			phraseString = StringUtils.substring(phraseString, 7, phraseString.length() - 1);
		} else if (fieldTypePhraseQuery == 'p' || fieldTypePhraseQuery == 't' || fieldTypePhraseQuery == 'y') {
			// cut of 'type:', 'year:', 'plot:'
			phraseString = StringUtils.substring(phraseString, 6, phraseString.length() - 1);
		} else {
			// cut of 'episodetitle'
			phraseString = StringUtils.substring(phraseString, 14, phraseString.length() - 1);
		}

		StringSearch raita = new BoyerMooreHorspoolRaita();

		if (fieldTypePhraseQuery == 'i') {
			// search in title
			for (Map.Entry<Integer, String> entry: allTitlePhrases.entrySet()){
				if (raita.searchString(entry.getValue(), phraseString) > - 1) {
					matchingMovies.add(entry.getKey());
				}
			}
		} else if (fieldTypePhraseQuery == 'p') {
			// search in plot
			for (Map.Entry<Integer, String> entry: allPlotPhrases.entrySet()){
				if (raita.searchString(entry.getValue(), phraseString) > - 1) {
					matchingMovies.add(entry.getKey());
				}
			}
		} else {
			// search in episode title
			for (Map.Entry<Integer, String> entry: allEpisodeTitlePhrases.entrySet()){
				if (raita.searchString(entry.getValue(), phraseString) > - 1) {
					matchingMovies.add(entry.getKey());
				}
			}
		}
		return matchingMovies;
	}

	/* perform a phrase search */
	private List<Integer> phraseSearch(String phraseString) {
		ArrayList<Integer> matchingMovies = new ArrayList<>();

		// get the field
		char fieldTypePhraseQuery = getFieldType(phraseString);

		// depending on the field cut off the field
		if (fieldTypePhraseQuery == 'i') {
			// cut of 'title:'
			phraseString = StringUtils.substring(phraseString, 7, phraseString.length() - 1);
		} else if (fieldTypePhraseQuery == 'p' || fieldTypePhraseQuery == 't' || fieldTypePhraseQuery == 'y') {
			// cut of 'type:', 'year:', 'plot:'
			phraseString = StringUtils.substring(phraseString, 6, phraseString.length() - 1);
		} else {
			// cut of 'episodetitle'
			phraseString = StringUtils.substring(phraseString, 14, phraseString.length() - 1);
		}

		// now tokenize the phraseString
		StringTokenizer st = new StringTokenizer(phraseString, " .,:!", false);
		// make a list of movies in which at least one of the tokens appear
		ArrayList<Integer> foundMoviesWithTokensFromPhrases = new ArrayList<>(64);

		// count how many tokens (aka terms) are in the phrase
		int howManyTokens = 0;

		while (st.hasMoreTokens()) {
			// we add every movie to the list in which we find at least one token, we  use our
			// tokenSearch Method for it
			for (int i : tokenSearch(st.nextToken(), fieldTypePhraseQuery)) {
				foundMoviesWithTokensFromPhrases.add(i);
			}
			howManyTokens++;
		}
		// now count the occurrence of the movies we got from our token search
		HashMap<Integer, Integer> countMatchingMovies = new HashMap<>(64);
		for (int i : foundMoviesWithTokensFromPhrases) {
			Integer cnt = countMatchingMovies.get(i);
			countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1); // if cnt == null add 1 else cnt + 1)
		}

		// sort the list for the count, the copyAndSort list is just a helper list so we can sort the hash map
		ArrayList<Map.Entry<Integer, Integer>> copyAndSort = new ArrayList<>();
		copyAndSort.addAll(countMatchingMovies.entrySet());
		copyAndSort.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

		StringSearch raita = new BoyerMooreHorspoolRaita();

		// now we use boyer-moore to go through the movies which have all tokens
		for (Map.Entry<Integer, Integer> entry : copyAndSort) {
			// run through the list only as long as the count of tokens matches the
			// appearance count in the movies
			if (entry.getValue() != howManyTokens) {
				break;
			}
			// call boyer-moore here for the phrase
			if (fieldTypePhraseQuery == 'i') {
				// search in title
				if (raita.searchString(allTitlePhrases.get(entry.getKey()), phraseString) > -1) {
					matchingMovies.add(entry.getKey());
				}
			} else if (fieldTypePhraseQuery == 'p') {
				// search in plot
				if (raita.searchString(allPlotPhrases.get(entry.getKey()), phraseString) > -1) {
					matchingMovies.add(entry.getKey());
				}
			} else {
				// search in episode title
				if (raita.searchString(allEpisodeTitlePhrases.get(entry.getKey()), phraseString) > -1) {
					matchingMovies.add(entry.getKey());
				}
			}
		}
		return matchingMovies;
	}

	/* get the field type from the query - helper method */
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

	/* perform tokenSearch - helper method*/
	private List<Integer> tokenSearch(String tokenString, char searchField) {
		ArrayList<Integer> matchingMovies = new ArrayList<>(32);

		// +++ title +++
		if (searchField == 'i') {
			// get all movie ids from the hash map and add them to the matching movies list
			if (hashTitle.containsKey(tokenString)) {
				for (int i : hashTitle.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		// +++ plot +++
		if (searchField == 'p') {
			if (hashPlot.containsKey(tokenString)) {
				for (int i : hashPlot.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		// +++ type +++
		if (searchField == 't') {
			if (hashType.containsKey(tokenString)) {
				for (int i : hashType.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		// +++ year +++
		if (searchField == 'y') {
			if (hashYear.containsKey(tokenString)) {
				for (int i : hashYear.get(tokenString)) {
					matchingMovies.add(i);
				}
				return matchingMovies;
			}
			return matchingMovies;
		}
		// +++ episode title +++
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
		ArrayList<Long> allTimes = new ArrayList<>(15);
		long time;
		SpeedBIR bq = new SpeedBIR();
		if (args.length < 3) {
			System.err
				.println("usage: java -jar SpeedBIR.jar <plot list file> <queries file> <results file>");
			System.exit(-1);
		}

		System.out.println("SPEED SETUP \n");

		// build indices
		System.out.println("building indices...");
		long tic = System.nanoTime();
		Runtime runtime = Runtime.getRuntime();
		long mem = runtime.totalMemory();
		bq.buildIndices(args[0]);
		time = System.nanoTime() - tic;
		allTimes.add(time);
		System.out
			.println("runtime: " + time + " nanoseconds");
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
			Comparator<String> stringComparator = String::compareTo;
			expectedResultSorted.sort(stringComparator);
			actualResultSorted.sort(stringComparator);

			time = System.nanoTime() - tic;
			allTimes.add(time);

			System.out.println("runtime:         " + (System.nanoTime() - tic)
				+ " nanoseconds.");
			System.out.println("expected result: " + expectedResultSorted.toString());
			System.out.println("actual result:   " + actualResultSorted.toString());
			System.out.println(expectedResult.equals(actualResult) ? "SUCCESS"
				: "FAILURE");
		}

		try {
			FileWriter writer = new FileWriter("output.txt");
			for (long str : allTimes) {
				writer.write(Long.toString(str) + "\n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}


	}
}
