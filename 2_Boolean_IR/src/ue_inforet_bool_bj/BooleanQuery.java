package ue_inforet_bool_bj;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BooleanQuery {

	//+++++++++++++++++++++++++++++++++
	/* fields:
	title			= 1
	episodetitle		= 2
	plot			= 3
	year			= 4
	type			= 5
	 *///++++++++++++++++++++++++++++++

	private String token = "";
	private ArrayList<Movie> allMovies = new ArrayList<>();
	private static HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
	private static HashMap<String, HashSet<Integer>> hashYear = new HashMap<>();
	private static HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
	private static HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>();
	private static HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>();
	private byte titleTermCount;
	private byte episodeTitleTermCount;
	private short plotTermCount;

	/**
	 * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
	 * public
	 */
	BooleanQuery() {
	}

	//Jonas // TODO
	public static List<Integer> tokenQuery(String token, int field) {
		List<Integer> queryAnswer = new ArrayList<Integer>();
		return queryAnswer;
	}

	//Jonas // TODO
	public static List<Integer> andQuery(List<Integer> tokenList) {
		List<Integer> queryAnswer = new ArrayList<Integer>();
		return queryAnswer;
	}

	//Christoph/ / TODO
	public static List<Integer> phraseQuery(List<Integer> tokenList, int field) {
		List<Integer> queryAnswer = new ArrayList<Integer>();
		return queryAnswer;
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

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				// is it an MV: line?
				if (line.startsWith("M")) {
					// add an entry and increase movieID
					movieID = nextMovieID++;
					Movie movie = new Movie(movieID,line);
					allMovies.add(movieID, movie);
					// add movie to list and add title, type to the hash maps
					// get rid of 'MV: ' first and toLowerCase()
					addTitleTypeYearToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
				}
				// is it an PL: line?
				if (StringUtils.substring(line, 0, 3).contains("PL:")) {
					// tokenize the line and add the tokens to the hash map.
					// get rid of 'PL: ' first and toLowerCase()

					addTokenFromPlotLineToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
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
	//Christoph // TODO
	public Set<String> booleanQuery(String queryString) {
		return new HashSet<>();
	}

	/* Adds a  value of a movieID to a HashMap */
	public static void addToHashmap(HashMap<String, HashSet<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			HashSet<Integer> movieList = new HashSet<Integer>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	/***
	 * Gets a plot line and adds it to the hash map
	 * @param movieID
	 * @param line
	 */
	private void addTokenFromPlotLineToHashMap(Integer movieID, String line) {
		token = "";

		// iterate through the line char by char and check for delimiters
		for (int i = 0; i < line.length(); i++) {

			// if a delimiter is found, add the token to the hash map and continue with the next word.
			if (line.charAt(i) == ' ' || line.charAt(i) == '.' || line.charAt(i) == ':' ||
				line.charAt(i) == '?' || line.charAt(i) == '!' || line.charAt(i) == ',') {

				if (!token.isEmpty()) { // we have to check if the token is empty first
					addToHashmap(hashPlot, token, movieID);
					allMovies.get(movieID).setPositionOfTermInPlot(token,plotTermCount++);
				}
				// after we added the token to the hash map, reset it and continue
				token = "";
				continue;
			}
			// add chars to the token if no delimiter is found
			token += line.charAt(i);
		}

		// when we reached the end of the line, we want to add the last token as well
		if (!token.isEmpty()) {
			addToHashmap(hashPlot, token, movieID);
			allMovies.get(movieID).setPositionOfTermInPlot(token,plotTermCount++);
		}
	}


	/***
	 this method parses the year, title, episode title and adds it to the
	 * hash map. it basically works like this: first check the type (movie, tv, vg, ...).
	 * the episode type is a bit tricky: we have to parse the episode title first.
	 * after that we cut it off and parse the title and year like the other (see
	 * below on how to)
	 *
	 * @param movieID
	 * @param mvLine
	 */
	private void addTitleTypeYearToHashMap(int movieID, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			// add the type to the hash map
			addToHashmap(hashType, "series", movieID);
			parseTitleAndYear(movieID, mvLine, true);
			// and we're done, no need to check the other if-statements
			return;
		}
		// +++ episode +++
		if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			addToHashmap(hashType, "episode", movieID);

			// first get the episode title and add it to the list. we know form grep that '{' is never
			// in a title/year, so we can start there.
			StringTokenizer st = new StringTokenizer(mvLine.substring(mvLine.indexOf('{') + 1, mvLine.length() - 1), " .,:!?", false);
			token = "";

			while (st.hasMoreTokens()) {
				token = st.nextToken();
				addToHashmap(hashEpisodeTitle, token, movieID);
				allMovies.get(movieID).setPositionOfTermInEipsodeTitle(token,episodeTitleTermCount++);
			}

			// get rid of the episode title. and proceed like it is an episode
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.indexOf('{')), true);
			return;
		}
		// +++ television +++
		if (mvLine.contains(") (tv)")) {
			addToHashmap(hashType, "television", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
			return;
		}
		// +++ video +++
		if (mvLine.contains(") (v)")) {
			addToHashmap(hashType, "video", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 5), false);
			return;
		}
		// +++ video game +++
		if (mvLine.contains(") (vg)")) {
			addToHashmap(hashType, "videogame", movieID);
			parseTitleAndYear(movieID, mvLine.substring(0, mvLine.length() - 6), false);
			return;
		}
		// +++ movie +++
		addToHashmap(hashType, "movie", movieID);
		parseTitleAndYear(movieID, mvLine, false); //TODO check substring! also check other substrings!
	}


	/***
	 /* This method parses the year and the title. it starts with the year because it is always the last
	 * part of the 'MV: ' - line. in the method above we already cut off the the types and before the types is
	 * ALWAYS the year. after the year is parsed, we cut it off and parse the title from the start. we can
	 * do that because we already cut off everything else could lead to misinformation.
	 * for example: MV: Terminator (2033) (1995) -> MV: is already removed in buildIndices and after the year is
	 * parsed it is cut off and the title is now unique: 'Terminator (2033)'
	 *
	 * @param movieID
	 * @param mvLine
	 * @param isSeries
	 */
	private void parseTitleAndYear(int movieID, String mvLine, boolean isSeries) {
		token = "";
		int end = 3;

                /* +++++++++++++++ PARSE THE YEAR ++++++++++++++++++++ */
		// first, get the year right from behind
		// we start behind the last parenthesis and add only digits to the token
		// with end we compute the end for the tokenizer so we know wher to
		// cut of the substring
		for (int i = mvLine.length() - 1; mvLine.charAt(i) != '('; i--) {
			if (mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9') {
				token += mvLine.charAt(i);
			}
			end++;
		}
		// if the token contains exact 4 digits, we reverse it and add it to the hash map
		if (token.length() == 4) {
			addToHashmap(hashYear, new StringBuilder(token).reverse().toString(), movieID);
		}

                /* +++++++++++++++ PARSE THE TITLE ++++++++++++++++++++ */
		StringTokenizer st;
		// check if it is a series we have to remove the quotation marks and the year else just remove the year
		if (isSeries) {
			st = new StringTokenizer(mvLine.substring(1, mvLine.length() - end), " .,:!?", false);
		} else {
			st = new StringTokenizer(mvLine.substring(0, mvLine.length() - end + 1), " .,:!?", false);
		}

		token = "";
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			addToHashmap(hashTitle, token, movieID);
			allMovies.get(movieID).setPositionOfTermInTitle(token,titleTermCount++);
		}
	}

	public static void main(String[] args) {
		ue_inforet_bool_variant.BooleanQuery bq = new ue_inforet_bool_variant.BooleanQuery();
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
			.println("memory: " + ((runtime.totalMemory() - mem) / (1048576l))
				+ " MB (rough estimate)");

/*		// parsing the queries that are to be run from the queries file
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
		}*/
	}
}
