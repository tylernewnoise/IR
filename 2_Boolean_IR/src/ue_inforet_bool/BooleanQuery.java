package ue_inforet_bool;

//import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
/*
private Int2ObjectOpenHashMap<String> allPlotPhrases = new Int2ObjectOpenHashMap<>(530000);
private Int2ObjectOpenHashMap<String> allTitlePhrases = new Int2ObjectOpenHashMap<>(530000);
private Int2ObjectOpenHashMap<String> allEpisodeTitlePhrases = new Int2ObjectOpenHashMap<>(220000);
*/

import com.eaio.stringsearch.StringSearch;
import com.eaio.stringsearch.BoyerMooreHorspoolRaita;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BooleanQuery {
	private ArrayList<String> allMoviesList = new ArrayList<>(530000);
	private HashMap<Integer, String> allPlotPhrases = new HashMap<>(530000);
	private HashMap<Integer, String> allTitlePhrases = new HashMap<>(530000);
	private HashMap<Integer, String> allEpisodeTitlePhrases = new HashMap<>(220000);
	private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>(700000);
	private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>(150000);
	private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>(100000);

	private HashMap<String, ArrayList<Integer>> hashYear = new HashMap<>(150);
	private HashMap<String, ArrayList<Integer>> hashType = new HashMap<>(6);

	public BooleanQuery() {
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

		StringBuilder stringBuilder = new StringBuilder(8128);

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
						stringBuilder = new StringBuilder(8128);
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
						line.length()).toLowerCase(), " .,:!", false);

					// now tokenize the plot - thanks Jonas
					while (st.hasMoreTokens()) {
						String token = st.nextToken();
						if (token.contains("\"")) {
							stringBuilder.append(token);
							stringBuilder.append(" ");
							continue;
						}
						addPlotAndTitleToHashMap(hashPlot, token, movieID);
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
	private void addPlotAndTitleToHashMap(HashMap<String, HashSet<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			HashSet<Integer> movieList = new HashSet<>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	private void addTypeAndYearToHashMap(HashMap<String, ArrayList<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			ArrayList<Integer> movieList = new ArrayList<>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	/**
	 * this method adds the year, title, episode title to the hash map.
	 *
	 * @param movieID Gives each entry in our indices a unique number.
	 * @param mvLine  Contains all information for the different entries.
	 */
	private void getTitleTypeYear(int movieID, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			// add the type to the hash map
			addTypeAndYearToHashMap(hashType, "series", movieID);
			parseTitleAndYear(movieID, mvLine, true);
		}
		// +++ episode +++
		else if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			addTypeAndYearToHashMap(hashType, "episode", movieID);

			// first get the episode title and add it to the list. we know form grep that { is never
			// in a title/year, so we can start there.
			StringTokenizer st = new StringTokenizer(mvLine.substring(mvLine.indexOf('{') + 1,
				mvLine.length() - 1), " .,:!?", false);

			StringBuilder stringBuilder = new StringBuilder(256);

			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.contains("\"")) {
					stringBuilder.append(token);
					stringBuilder.append(" ");
					continue;
				}
				addPlotAndTitleToHashMap(hashEpisodeTitle, token, movieID);
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
			addTypeAndYearToHashMap(hashType, "television", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
		}
		// +++ video +++
		else if (mvLine.contains(") (v)")) {
			addTypeAndYearToHashMap(hashType, "video", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 5), false);
		}
		// +++ video game +++
		else if (mvLine.contains(") (vg)")) {
			addTypeAndYearToHashMap(hashType, "videogame", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
		} else {
			// +++ movie +++
			addTypeAndYearToHashMap(hashType, "movie", movieID);
			parseTitleAndYear(movieID, mvLine, false);
		}
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
			addTypeAndYearToHashMap(hashYear, new StringBuilder(year).reverse().toString(), movieID);
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

		StringBuilder stringBuilder = new StringBuilder(256);

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.contains("\"")) {
				stringBuilder.append(token);
				stringBuilder.append(" ");
				continue;
			}
			addPlotAndTitleToHashMap(hashTitle, token, movieID);
			stringBuilder.append(token);
			stringBuilder.append(" ");
		}

		allTitlePhrases.put(movieID, stringBuilder.toString());
	}

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

		// get the field
		char fieldTypePhraseQuery = getFieldTypeForPhraseQuery(queryString);

		// depending on the field cut off the field
		if (fieldTypePhraseQuery == 'i') {
			// cut of 'title:'
			queryString = StringUtils.substring(queryString, 7, queryString.length() - 1);
		} else if (fieldTypePhraseQuery == 'p' || fieldTypePhraseQuery == 't' || fieldTypePhraseQuery == 'y') {
			// cut of 'type:', 'year:', 'plot:'
			queryString = StringUtils.substring(queryString, 6, queryString.length() - 1);
		} else {
			// cut of 'episodetitle'
			queryString = StringUtils.substring(queryString, 14, queryString.length() - 1);
		}

		// now tokenize the phraseString
		StringTokenizer st = new StringTokenizer(queryString, " .,:!", false);
		// make a list of movies in which at least one of the tokens appear
		ArrayList<Integer> foundMoviesWithTokensFromPhrases = new ArrayList<>(8128);

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
		HashMap<Integer, Integer> countMatchingMovies = new HashMap<>(8128);
		for (int i : foundMoviesWithTokensFromPhrases) {
			Integer cnt = countMatchingMovies.get(i);
			countMatchingMovies.put(i, (cnt == null) ? 1 : cnt + 1); // if cnt == null add 1 else cnt + 1)
		}

		StringSearch bmhRaita = new BoyerMooreHorspoolRaita();

		// run through the hash map and do string search only for movies which have
		// the same count as howManyTokens
		for (Map.Entry<Integer, Integer> entry : countMatchingMovies.entrySet()) {
			if (howManyTokens == entry.getValue()) {
				if (fieldTypePhraseQuery == 'i') {
					// search in title
					// the SearchString class returns -1 if the pattern is not found
					if (bmhRaita.searchString(allTitlePhrases.get(entry.getKey()),
						queryString) != -1) {
						matchingMovies.add(entry.getKey());
					}
				} else if (fieldTypePhraseQuery == 'p') {
					// search in plot
					if (bmhRaita.searchString(allPlotPhrases.get(entry.getKey()),
						queryString) != -1) {
						matchingMovies.add(entry.getKey());
					}
				} else {
					// search in episode title
					if (bmhRaita.searchString(allEpisodeTitlePhrases.get(entry.getKey()),
						queryString) != -1) {
						matchingMovies.add(entry.getKey());
					}
				}
			}
		}
		return matchingMovies;
	}

	/* get the field type from the phrase query */
	private char getFieldTypeForPhraseQuery(String queryString) {
		// title
		if (queryString.indexOf("i") == 1) {
			return 'i';
		}
		// plot
		else if (queryString.startsWith("p")) {
			return 'p';
		}
		// type
		else if (queryString.indexOf("y") == 1) {
			return 't';
		}
		// year
		else if (queryString.startsWith("y")) {
			return 'y';
		}
		// episode title
		else {
			return 'e';
		}
	}

	/* perform a simple token search for a term of the phrase query */
	private List<Integer> tokenSearchForPhraseQuery(String tokenString, char searchField) {
		ArrayList<Integer> matchingMovies = new ArrayList<>(50000);

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
		ArrayList<Integer> matchingMovies = new ArrayList<>(256000);

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
		} else if (getFieldTypeForPhraseQuery(queryString) == 'e') { // episode title
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
		ArrayList<Long> allTimes = new ArrayList<>(15);
		long time;
		BooleanQuery bq = new BooleanQuery();
		if (args.length < 3) {
			System.err
				.println("usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
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
		long gesamt = 0;
		int i = 0;
		while (i < allTimes.size()) {
			gesamt += allTimes.get(i);
			i++;
		}
		try {
			FileWriter writer = new FileWriter("output.txt");
			for (long str : allTimes) {
				//writer.write(Integer.toString(i++) + ": " + Long.toString(str) + "\n");
				writer.write(Long.toString(str) + "\n");
			}
			writer.write("gesamt: \n" + Long.toString(gesamt));
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

