package ue_inforet_bool;

import com.eaio.stringsearch.StringSearch;
import com.eaio.stringsearch.BoyerMooreHorspoolRaita;
// from https://github.com/johannburkard/StringSearch

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
// from http://java-performance.info/primitive-types-collections-trove-library
// and thanks Benny

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

import javolution.text.TextBuilder;
// http://javolution.org

import org.apache.commons.lang3.StringUtils;

public class BooleanQuery {

	private ArrayList<String> allMoviesList = new ArrayList<>(530000);

	private TIntObjectHashMap<String> plotPhrases = new TIntObjectHashMap<>(530000);
	private TIntObjectHashMap<String> titlePhrases = new TIntObjectHashMap<>(530000);
	private TIntObjectHashMap<String> episodetitlePhrases = new TIntObjectHashMap<>(220000);

	private THashMap<String, TIntHashSet> hashType = new THashMap<>(6);
	private THashMap<String, TIntHashSet> hashYear = new THashMap<>(150);
	private THashMap<String, TIntHashSet> hashPlot = new THashMap<>(700000);
	private THashMap<String, TIntHashSet> hashTitle = new THashMap<>(150000);
	private THashMap<String, TIntHashSet> hashEpisodeTitle = new THashMap<>(100000);

	/**
	 * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
	 */
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
	public void buildIndices(String plotFile) {
		int nextMovieID = 0;
		int movieID = 0;
		boolean isPlotLine = false;

		// there are only 6 types, so we can initialize them already
		hashType.put("video", new TIntHashSet(21000));
		hashType.put("movie", new TIntHashSet(250000));
		hashType.put("series", new TIntHashSet(22000));
		hashType.put("episode", new TIntHashSet(222000));
		hashType.put("videogame", new TIntHashSet(2500));
		hashType.put("television", new TIntHashSet(20000));

		// TextBuilder will build us the Strings and is way faster than the + Operator,
		// a little bit faster than StringBuilder and is supposed to run in O(logn)
		TextBuilder textBuilder = new TextBuilder();

		/* read from the file - thanks Christoph */
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				// is it an MV: line?
				if (line.startsWith("M")) {
					// if isPlotLine is true we know that a new movie-document starts so we have
					// to add the plot String to the hash map
					if (isPlotLine) {
						plotPhrases.put(movieID, textBuilder.toString());
						isPlotLine = false;
						textBuilder = new TextBuilder(6000);
					}
					// add an entry and increase movieID
					movieID = nextMovieID++;
					allMoviesList.add(movieID, line);
					// add movie to list and add title, type and year to the hash maps
					// remove 'MV: ' first and convert everything toLowerCase()
					getTitleTypeYear(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
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
			plotPhrases.put(movieID, textBuilder.toString());
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/* adds the movie and it's values to the hash map - thanks Benny */
	private void addPlotTitleYearToHashMap(THashMap<String, TIntHashSet> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			TIntHashSet movieList = new TIntHashSet();
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
			episodetitlePhrases.put(movieID, textBuilder.toString());

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

		titlePhrases.put(movieID, textBuilder.toString());
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
			TIntHashSet intersection = new TIntHashSet(20000);
			THashSet<String> alreadyQueriedTokens = new THashSet<>(512);

			String singleQuery[] = queryString.split(" AND ");

			// do a first search so we have a first list for the intersection
			if (singleQuery[0].contains("\"")) {
				alreadyQueriedTokens.add(singleQuery[0]);
				intersection.addAll(phraseQuerySearch(singleQuery[0].toLowerCase()));

			} else {
				alreadyQueriedTokens.add(singleQuery[0]);
				intersection.addAll(singleTokenSearch(singleQuery[0].toLowerCase()));
			}

			for (int i = 1; i < singleQuery.length; ++i) {
				// we can stop if there's only one entry in the intersection
				if (intersection.size() == 1) {
					break;
				}
				// only search if it wasn't done before
				if (!alreadyQueriedTokens.contains(singleQuery[i])) {
					TIntHashSet matchingMovies = new TIntHashSet(20000);
					if (singleQuery[i].contains("\"")) { // phrasequery
						matchingMovies.addAll(phraseQuerySearch(singleQuery[i].toLowerCase()));
						// if the retainAll method is not true it can mean two things:
						// we found an identical list or we found nothing. if the latter is the
						// case we have to be sure so we make an intersection "the other way
						// 'round". in this case retainAll() would remove everything from the
						// matchingMovies set and we can stop searching further
						if (!intersection.retainAll(matchingMovies)) {
							matchingMovies.retainAll(intersection);
							if (matchingMovies.size() == 0) {
								return results;
							}
						}
					} else { // tokenquery
						matchingMovies.addAll(singleTokenSearch(singleQuery[i].toLowerCase()));
						if (!intersection.retainAll(matchingMovies)) {
							matchingMovies.retainAll(intersection);
							if (matchingMovies.size() == 0) {
								return results;
							}
						}
					}
					alreadyQueriedTokens.add(singleQuery[i]);
				}
			}

			// add everything to the results
			TIntIterator it = intersection.iterator();
			for (int i = 0; i < intersection.size(); ++i) {
				results.add(allMoviesList.get(it.next()));
			}

			return results;
		} else if (queryString.contains("\"")) {
		/* QUERY IS ONLY A PHRASE SEARCH */
			for (TIntIterator it = phraseQuerySearch(queryString.toLowerCase()).iterator(); it.hasNext(); ) {
				results.add(allMoviesList.get(it.next()));
			}
			return results;
		} else {
		/* QUERY IS ONLY TOKEN SEARCH */
			for (TIntIterator it = singleTokenSearch(queryString.toLowerCase()).iterator(); it.hasNext(); ) {
				results.add(allMoviesList.get(it.next()));
			}
			return results;
		}
	}

	/* perform phrase query search */
	private TIntArrayList phraseQuerySearch(String queryString) {
		TIntArrayList matchingMovies = new TIntArrayList(32);

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
		TextBuilder textBuilder = new TextBuilder();
		StringTokenizer st = new StringTokenizer(queryString, " .,:!?", false);

		String tmp = st.nextToken();
		textBuilder.append(tmp);
		textBuilder.append(" ");

		TIntHashSet intersection = new TIntHashSet();
		THashSet<String> alreadyQueriedTokens = new THashSet<>();

		// do a first search to fill the list for the intersection
		intersection.addAll(tokenSearchForPhraseQuery(tmp, fieldTypePhraseQuery));
		alreadyQueriedTokens.add(tmp);

		while (st.hasMoreTokens()) {
			tmp = st.nextToken();
			// only search and check for intersection if the token wasn't searched before or
			// if the intersection-list is != 1
			if (!alreadyQueriedTokens.contains(tmp) || intersection.size() != 1) {
				TIntHashSet foundMoviesWithTokensFromPhrases = new TIntHashSet();
				foundMoviesWithTokensFromPhrases.addAll(tokenSearchForPhraseQuery(tmp,
					fieldTypePhraseQuery));
				intersection.retainAll(foundMoviesWithTokensFromPhrases);
				alreadyQueriedTokens.add(tmp);
			}
			textBuilder.append(tmp);
			textBuilder.append(" ");
		}

		String phraseQuery = textBuilder.toString();

		StringSearch bmhRaita = new BoyerMooreHorspoolRaita();

		TIntIterator iterator = intersection.iterator();
		for (int i = 0; i < intersection.size(); ++i) {
			int it = iterator.next();
			// the SearchString class returns -1 if the pattern is not found
			if (fieldTypePhraseQuery == 'i') {
				// search in title
				// do string search only if the query (pattern) is at least less equal than
				// the text to search in. do this only for title and episodetitle, because the
				// plot is in most cases quite long
				if (phraseQuery.length() <= titlePhrases.get(it).length()) {
					if (bmhRaita.searchString(titlePhrases.get(it), phraseQuery) != -1) {
						matchingMovies.add(it);
					}
				}
			} else if (fieldTypePhraseQuery == 'p') {
				// search in plot
				if (bmhRaita.searchString(plotPhrases.get(it), phraseQuery) != -1) {
					matchingMovies.add(it);
				}
			} else {
				// search in episode title
				if (phraseQuery.length() <= episodetitlePhrases.get(it).length()) {
					if (bmhRaita.searchString(episodetitlePhrases.get(it), phraseQuery) != -1) {
						matchingMovies.add(it);
					}
				}
			}
		}

		return matchingMovies;
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
		} else { // episode title
			String tmp = StringUtils.substring(queryString, 13, queryString.length());
			if (hashEpisodeTitle.containsKey(tmp)) {
				matchingMovies.addAll(hashEpisodeTitle.get(tmp));
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
		System.out.println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
		System.out.println("memory: " + ((runtime.totalMemory() - mem) / (1048576L)) + " MB (rough estimate)");

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

			System.out.println("runtime:         " + (System.nanoTime() - tic) + " nanoseconds.");
			System.out.println("expected result: " + expectedResultSorted.toString());
			System.out.println("actual result:   " + actualResultSorted.toString());
			System.out.println(expectedResult.equals(actualResult) ? "SUCCESS" : "FAILURE");
		}
	}
}