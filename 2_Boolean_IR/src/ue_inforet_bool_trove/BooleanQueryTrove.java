package ue_inforet_bool_trove;

// https://github.com/johannburkard/StringSearch

import com.eaio.stringsearch.StringSearch;
import com.eaio.stringsearch.BoyerMooreHorspoolRaita;

// http://java-performance.info/primitive-types-collections-trove-library
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.THashSet;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;

// http://javolution.org
import javolution.text.TextBuilder;

import org.apache.commons.lang3.StringUtils;

public class BooleanQueryTrove {

	private ArrayList<String> allMoviesList = new ArrayList<>(530000);

	private THashMap<Integer, String> hashPlotPhrases = new THashMap<>(530000);
	private THashMap<Integer, String> hashTitlePhrases = new THashMap<>(530000);
	private THashMap<Integer, String> hashEpisodeTitlePhrases = new THashMap<>(220000);

	private THashMap<String, THashSet<Integer>> hashType = new THashMap<>(6);
	private THashMap<String, THashSet<Integer>> hashYear = new THashMap<>(150);
	private THashMap<String, THashSet<Integer>> hashPlot = new THashMap<>(700000);
	private THashMap<String, THashSet<Integer>> hashTitle = new THashMap<>(150000);
	private THashMap<String, THashSet<Integer>> hashEpisodeTitle = new THashMap<>(100000);


	public BooleanQueryTrove() {
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
	public void buildIndices(String plotFile) {
		int nextMovieID = 0;
		int movieID = 0;
		boolean isPlotLine = false;
		hashType.put("video", new THashSet<>(21000));
		hashType.put("movie", new THashSet<>(250000));
		hashType.put("series", new THashSet<>(22000));
		hashType.put("episode", new THashSet<>(222000));
		hashType.put("videogame", new THashSet<>(2500));
		hashType.put("television", new THashSet<>(20000));

		TextBuilder textBuilder = new TextBuilder();

		/* read from the file - thanks Christoph */
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				// is it an MV: line?
				if (line.startsWith("M")) {
					if (isPlotLine) {
						hashPlotPhrases.put(movieID, textBuilder.toString());
						isPlotLine = false;
						textBuilder = new TextBuilder(6000);
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
							textBuilder.append(token);
							textBuilder.append(" ");
							continue;
						}
						addPlotTitleYearToHashMap(hashPlot, token, movieID);
						textBuilder.append(token);
						textBuilder.append(" ");
					}
				}
			}
			// add the last plot phrase
			hashPlotPhrases.put(movieID, textBuilder.toString());
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/* adds the movie and it's values to the hash map - thanks Benny */
	private void addPlotTitleYearToHashMap(THashMap<String, THashSet<Integer>> hashMap, String value,
					       Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			THashSet<Integer> movieList = new THashSet<>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	/* gets the year, title, episode title out of the line */
	private void getTitleTypeYear(int movieID, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			// add the type to the hash map
			hashType.get("series").add(movieID);
			parseTitleAndYear(movieID, mvLine, true);
		}
		// +++ episode +++
		else if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			hashType.get("episode").add(movieID);

			// first get the episode title and add it to the list. we know form grep that { is never
			// in a title/year, so we can start there.
			StringTokenizer st = new StringTokenizer(mvLine.substring(mvLine.indexOf('{') + 1,
				mvLine.length() - 1), " .,:!?", false);

			TextBuilder textBuilder = new TextBuilder(256);

			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.contains("\"")) {
					textBuilder.append(token);
					textBuilder.append(" ");
					continue;
				}
				addPlotTitleYearToHashMap(hashEpisodeTitle, token, movieID);
				textBuilder.append(token);
				textBuilder.append(" ");
			}

			// add the token for the phrase search to the array list
			hashEpisodeTitlePhrases.put(movieID, textBuilder.toString());

			// get rid of the episode title. and proceed like it is an episode
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.indexOf('{')), true);
		}
		// +++ television +++
		else if (mvLine.contains(") (tv)")) {
			hashType.get("television").add(movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
		}
		// +++ video +++
		else if (mvLine.contains(") (v)")) {
			hashType.get("video").add(movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 5), false);
		}
		// +++ video game +++
		else if (mvLine.contains(") (vg)")) {
			hashType.get("videogame").add(movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
		} else {
			// +++ movie +++
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
			addPlotTitleYearToHashMap(hashYear, new TextBuilder(year).reverse().toString(), movieID);
		}

		StringTokenizer st;

		// then parse the title and add the token to hash map
		if (isSeries) {
			st = new StringTokenizer(mvLine.substring(1, mvLine.length() - end),
				" .,:!?", false);
		} else {
			st = new StringTokenizer(mvLine.substring(0, mvLine.length() - end + 1),
				" .,:!?", false);
		}

		TextBuilder textBuilder = new TextBuilder(256);

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.contains("\"")) {
				textBuilder.append(token);
				textBuilder.append(" ");
				continue;
			}
			addPlotTitleYearToHashMap(hashTitle, token, movieID);
			textBuilder.append(token);
			textBuilder.append(" ");
		}

		hashTitlePhrases.put(movieID, textBuilder.toString());
	}

	public Set<String> booleanQuery(String queryString) {
		HashSet<String> results = new HashSet<>(32);

		/* QUERY IS AN AND SEARCH */
		if (queryString.contains(" AND ")) {
			TIntArrayList matchingMovies = new TIntArrayList(256000);

			// split into single queries
			String singleQuery[] = queryString.split(" AND ");
			int howManyQueries = 0;

			// execute every query and safe the result in matchingMovies List
			// tmp is a single query and singleQuery[] a string array which contains
			// all queries
			for (String tmp : singleQuery) {
				if (tmp.contains("\"")) {
					matchingMovies.addAll(phraseQuerySearch(tmp.toLowerCase()));
				} else {
					matchingMovies.addAll(singleTokenSearch(tmp.toLowerCase()));
				}
				howManyQueries++;
			}

			// count the movies which fit the queries from above
			// if a movie matches all queries (f.e. 3 times) it is counted 3 times and if a
			// movie matches only 2 queries, it is counted only 2 times.
			TIntIntHashMap countMatchingMovies = new TIntIntHashMap(256000);

			for (TIntIterator it = matchingMovies.iterator(); it.hasNext(); ) {
				countMatchingMovies.adjustOrPutValue(it.next(), 1, 1);
			}

			// run through the hash map and add only the movies which match the count of howManyQueries
			TIntIntIterator it3 = countMatchingMovies.iterator();
			//for (TIntIntIterator it = countMatchingMovies.iterator(); it.hasNext(); ) {
			for (int i = countMatchingMovies.size(); --i > 0; ) {
				it3.advance();
				if (howManyQueries == it3.value()) {
					results.add(allMoviesList.get(it3.key()));
				}
			}

		} else if (queryString.contains("\"")) {
		/* QUERY IS A PHRASE SEARCH */
			for (TIntIterator it = phraseQuerySearch(queryString.toLowerCase()).iterator(); it.hasNext(); ) {
				results.add(allMoviesList.get(it.next()));
			}
		} else {
		/* QUERY IS ONLY TOKEN SEARCH */
			for (TIntIterator it = singleTokenSearch(queryString.toLowerCase()).iterator(); it.hasNext(); ) {
				results.add(allMoviesList.get(it.next()));
			}
		}
		return results;
	}

	/* perform phrase query search */
	private TIntArrayList phraseQuerySearch(String queryString) {
		TIntArrayList matchingMovies = new TIntArrayList(32);

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
		TIntArrayList foundMoviesWithTokensFromPhrases = new TIntArrayList(8000);

		// count how many tokens (aka terms) are in the phrase
		int howManyTokens = 0;

		while (st.hasMoreTokens()) {
			// we add every movie to the list in which we find at least one token, we use our
			// tokenSearchForPhraseQuery Method for it
			for (TIntIterator it = tokenSearchForPhraseQuery(st.nextToken(), fieldTypePhraseQuery).iterator();
			     it.hasNext(); ) {
				foundMoviesWithTokensFromPhrases.add(it.next());
			}
			howManyTokens++;
		}

		// now count the occurrence of the movies we got from our token search
		TIntIntHashMap countMatchingMovies = new TIntIntHashMap(8000);
/*		for (TIntIterator it = foundMoviesWithTokensFromPhrases.iterator(); it.hasNext(); ) {
			countMatchingMovies.adjustOrPutValue(it.next(), 1, 1);
		}*/
		TIntIterator it1 = foundMoviesWithTokensFromPhrases.iterator();
		for (int i = foundMoviesWithTokensFromPhrases.size(); --i > 0; ) {
			countMatchingMovies.adjustOrPutValue(it1.next(), 1, 1);

		}

		StringSearch bmhRaita = new BoyerMooreHorspoolRaita();

		// run through the hash map and do string search only for movies which have
		// the same count as howManyTokens
		TIntIntIterator it2 = countMatchingMovies.iterator();
		//for (TIntIntIterator it = countMatchingMovies.iterator(); it.hasNext(); ) {
		for (int i = countMatchingMovies.size(); --i > 0; ) {
			it2.advance();
			if (howManyTokens == it2.value()) {
				// the SearchString class returns -1 if the pattern is not found
				if (fieldTypePhraseQuery == 'i') {
					// search in title
					if (bmhRaita.searchString(hashTitlePhrases.get(it2.key()),
						queryString) != -1) {
						matchingMovies.add(it2.key());
					}
				} else if (fieldTypePhraseQuery == 'p') {
					// search in plot
					if (bmhRaita.searchString(hashPlotPhrases.get(it2.key()),
						queryString) != -1) {
						matchingMovies.add(it2.key());
					}
				} else {
					// search in episode title
					if (bmhRaita.searchString(hashEpisodeTitlePhrases.get(it2.key()),
						queryString) != -1) {
						matchingMovies.add(it2.key());
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
	private TIntArrayList tokenSearchForPhraseQuery(String tokenString, char searchField) {
		TIntArrayList matchingMovies = new TIntArrayList(50000);

		if (searchField == 'i') { // title
			if (hashTitle.containsKey(tokenString)) {
				matchingMovies.addAll(hashTitle.get(tokenString));
			}
		} else if (searchField == 'p') { // plot
			if (hashPlot.containsKey(tokenString)) {
				matchingMovies.addAll(hashPlot.get(tokenString));
			}
		} else if (searchField == 't') { // type
			if (hashType.containsKey(tokenString)) {
				matchingMovies.addAll(hashType.get(tokenString));
			}
		} else if (searchField == 'y') { // year
			if (hashYear.containsKey(tokenString)) {
				matchingMovies.addAll(hashYear.get(tokenString));
			}
		} else { // episode title
			if (hashEpisodeTitle.containsKey(tokenString)) {
				matchingMovies.addAll(hashEpisodeTitle.get(tokenString));
			}
		}
		return matchingMovies;
	}

	/* perform a single token search */
	private TIntArrayList singleTokenSearch(String queryString) {
		TIntArrayList matchingMovies = new TIntArrayList();

		// first we want to know the field type we have to search in
		// depending on the field type, we cut off the field and look for the token in the hash map
		if (queryString.indexOf("i") == 1) { // title
			String tmp = StringUtils.substring(queryString, 6, queryString.length());
			if (hashTitle.containsKey(tmp)) {
				matchingMovies.addAll(hashTitle.get(tmp));
			}
		} else if (queryString.startsWith("p")) { // plot
			String tmp = StringUtils.substring(queryString, 5, queryString.length());
			if (hashPlot.containsKey(tmp)) {
				matchingMovies.addAll(hashPlot.get(tmp));
			}
		} else if (queryString.indexOf("y") == 1) { // type
			String tmp = StringUtils.substring(queryString, 5, queryString.length());
			if (hashType.containsKey(tmp)) {
				matchingMovies.addAll(hashType.get(tmp));
			}
		} else if (queryString.startsWith("y")) { // year
			String tmp = StringUtils.substring(queryString, 5, queryString.length());
			if (hashYear.containsKey(tmp)) {
				matchingMovies.addAll(hashYear.get(tmp));
			}
		} else if (getFieldTypeForPhraseQuery(queryString) == 'e') { // episode title
			String tmp = StringUtils.substring(queryString, 13, queryString.length());
			if (hashEpisodeTitle.containsKey(tmp)) {
				matchingMovies.addAll(hashEpisodeTitle.get(tmp));
			}
		}
		return matchingMovies;
	}

	public static void main(String[] args) {
		BooleanQueryTrove bq = new BooleanQueryTrove();
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

/*		long time;
		BooleanQueryTrove bq = new BooleanQueryTrove();
		if (args.length < 3) {
			System.err
				.println("usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
			System.exit(-1);
		}

		// build indices
		long tic = System.nanoTime();
		Runtime runtime = Runtime.getRuntime();
		long mem = runtime.totalMemory();
		bq.buildIndices(args[0]);
		time = System.nanoTime() - tic;
		//System.out.println(time);

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
		for (int j = 0; j < 10000; j++) {
			System.out.println(j);
			for (int i = 0; i < queries.size(); i++) {
				String query = queries.get(i);
				Set<String> expectedResult = i < results.size() ? results.get(i)
					: new HashSet<>();
				//System.out.println("query:           " + query);
				tic = System.nanoTime();
				Set<String> actualResult = bq.booleanQuery(query);

				// sort expected and determined results for human readability
				List<String> expectedResultSorted = new ArrayList<>(expectedResult);
				List<String> actualResultSorted = new ArrayList<>(actualResult);
				Comparator<String> stringComparator = String::compareTo;
				expectedResultSorted.sort(stringComparator);
				actualResultSorted.sort(stringComparator);
				time = System.nanoTime() - tic;
				//System.out.println(time);
			}
		}
	}
}*/
