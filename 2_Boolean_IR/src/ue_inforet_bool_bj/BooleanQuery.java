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
	episodetitle	= 2
	plot			= 3
	year			= 4
	type			= 5
	 *///++++++++++++++++++++++++++++++

	private String token = "";
	private ArrayList<Movie> allMovies = new ArrayList<>();
	private HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashYear = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>();
	private byte titleTermCount;
	private byte episodeTitleTermCount;
	private short plotTermCount;

	/**
	 * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
	 * public
	 */
	//BooleanQuery() { }
	public BooleanQuery() {
	}

	//Jonas
	private ArrayList<Integer> TokenQuery(String token, int field) {
		ArrayList<Integer> queryAnswer = new ArrayList<>();

		switch (field) {
			//Title
			case 1:
				if (hashTitle.containsKey(token)) {
					for (int i : hashTitle.get(token)) {
						queryAnswer.add(i);
					}
				}
				break;
			//Episodetitle
			case 2:
				if (hashEpisodeTitle.containsKey(token)) {
					for (int i : hashEpisodeTitle.get(token)) {
						queryAnswer.add(i);
					}
				}
				break;
			//Plot
			case 3:
				if (hashPlot.containsKey(token)) {
					for (int i : hashPlot.get(token)) {
						queryAnswer.add(i);
					}
				}
				break;
			//Year
			case 4:
				if (hashYear.containsKey(token)) {
					for (int i : hashYear.get(token)) {
						queryAnswer.add(i);
					}
				}
				break;
			//Type
			default:
				if (hashType.containsKey(token)) {
					for (int i : hashType.get(token)) {
						queryAnswer.add(i);
					}
				}
				break;

		}
		return queryAnswer;
	}


	private ArrayList<Integer> MergeLists(ArrayList<ArrayList<Integer>> llist) {
		ArrayList<Integer> mergedList = new ArrayList<>();
		if (llist.size() == 0) {
			return mergedList;
		}
		for (int i : llist.get(0)) {
			int cnt = llist.size();
			for (List<Integer> list : llist) {
				if (!list.contains(i)) {
					continue;
				}
				cnt--;
			}
			if (cnt == 0) {
				mergedList.add(i);
			}
		}
		return mergedList;
	}


	//Christoph
	private ArrayList<Integer> PhraseQuery(List<String> tokenList, int field) {
		ArrayList<Integer> queryAnswer = new ArrayList<>();

		boolean tokenMatch = true;

		ArrayList<ArrayList<Integer>> tmpLlist = new ArrayList<>(); // List of List of Integers

		ArrayList<Integer> mergedLists = new ArrayList<>();
		switch (field) {
			//Title
			case 1:
				// create List of List of Integers (--> resulting MovieIDs)
				for (String tokenFromList : tokenList) {
					ArrayList<Integer> tmpList = new ArrayList<>();
					for (int i : hashTitle.get(tokenFromList)) {
						tmpList.add(i);
					}
					tmpLlist.add(tmpList);
				}
				// merge resulting movieIDLists
				mergedLists.addAll(MergeLists(tmpLlist));

				// iterate through mergedList (each movieID contains all tokens from phrase)

				// for each movie from mergedList -->
				for (Integer currentMovie : mergedLists) {
					// Values: tokenMatch, tokenPosition

					// get first token and its positions
					ArrayList<Byte> tmpPosList = new ArrayList<>();
					tmpPosList.addAll(allMovies.get(currentMovie).getPositionsOfTermInTitle(tokenList.get(0)));
					// compare the other tokens with first token and all of its positions
					// position loop
					for (Byte itemPosList : tmpPosList) {
						// token loop
						for (int i = 1; i < tokenList.size(); i++){
							if (!allMovies.get(currentMovie).isTermAtPositionInTitle(tokenList.get(i),
								(byte)(itemPosList + i))) {
								tokenMatch = false;
								break;
							}
						}
						// add Item to resultlist, because tokenMatch is still true
						if (tokenMatch) {
							queryAnswer.add(currentMovie);
						}
						tokenMatch = true;
					}
				}
				break;
			//Episodetitle
			case 2:
				// create List of List of Integers (--> resulting MovieIDs)
				for (String tokenFromList : tokenList) {
					ArrayList<Integer> tmpList = new ArrayList<> ();
					for (int i : hashEpisodeTitle.get(tokenFromList)) {
						tmpList.add(i);
					}
					tmpLlist.add(tmpList);
				}
				// merge resulting movieIDLists
				//ArrayList<Integer> mergedLists = new ArrayList<> ();
				mergedLists.addAll(MergeLists(tmpLlist));

				// iterate through mergedList (each movieID contains all tokens from phrase)

				// for each movie from mergedList -->
				for (Integer currentMovie : mergedLists) {
					// Values: tokenMatch, tokenPosition

					// get first token and its positions
					ArrayList<Byte> tmpPosList = new ArrayList<>();
					tmpPosList.addAll(allMovies.get(currentMovie).getPositionsOfTermInEpisodeTitle(tokenList.get(0)));
					// compare the other tokens with first token and all of its positions
					// position loop
					for (Byte itemPosList : tmpPosList) {
						// token loop
						for (int i = 1; i < tokenList.size(); i++){
							if (!allMovies.get(currentMovie).isTermAtPositionInEpisodeTitle(tokenList.get(i), (byte)(itemPosList + i))) {
								tokenMatch = false;
								break;
							}
						}
						// add Item to resultlist, because tokenMatch is still true
						if (tokenMatch) {
							queryAnswer.add(currentMovie);
						}
						tokenMatch = true;
					}
				}
				break;
			//Plot
			case 3:
				// create List of List of Integers (--> resulting MovieIDs)
				for (String tokenFromList : tokenList) {
					ArrayList<Integer> tmpList = new ArrayList<>();
					for (int i : hashPlot.get(tokenFromList)) {
						tmpList.add(i);
					}
					tmpLlist.add(tmpList);
				}
				// merge resulting movieIDLists
				mergedLists.addAll(MergeLists(tmpLlist));

				// iterate through mergedList (each movieID contains all tokens from phrase)

				// for each movie from mergedList -->
				for (Integer currentMovie : mergedLists) {
					// Values: tokenMatch, tokenPosition

					// get first token and its positions
					ArrayList<Short> tmpPosList = new ArrayList<>();

					tmpPosList.addAll(allMovies.get(currentMovie).getPositionsOfTermInPlot(tokenList.get(0)));
					// compare the other tokens with first token and all of its positions
					// position loop
					for (Short itemPosList : tmpPosList) {
						// token loop
						for (int i = 1; i < tokenList.size(); i++){
							if (!allMovies.get(currentMovie).isTermAtPositionInPlot(tokenList.get(i), (short)(itemPosList + i))) {
								tokenMatch = false;
								break;
							}
						}
						// add Item to resultlist, because tokenMatch is still true
						if (tokenMatch) {
							queryAnswer.add(currentMovie);
						}
						tokenMatch = true;
					}
				}
				break;
			default:
				break;
		}
		// List of movieIDs
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
					//set all counters to 0
					plotTermCount = titleTermCount = episodeTitleTermCount = 0;
					// add an entry and increase movieID
					movieID = nextMovieID++;
					Movie movie = new Movie(movieID, line);
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
	//Christoph
	public Set<String> booleanQuery(String queryString) {
		// extract the queryString
		// split into TokenQueries, PhraseQueries

		ArrayList<ArrayList<Integer>> tmpResultLlist = new ArrayList<>(); // List of List of Integers (MovieIDs)
		queryString += " ";
		boolean phraseQuery = false;
		boolean phraseQueryEnd = false;
		boolean catchQuery = false;
		String currentString = "";
		int field = 0;
		String query;

		// iterate through queryString char by char
		for (int i = 0; i < queryString.length(); i++) {
			// add only characters != : || "
			if ((queryString.charAt(i) != '"') && (queryString.charAt(i) != ':')) {
				currentString += queryString.charAt(i);
			} else if (phraseQuery && (queryString.charAt(i) != '"')){
				currentString += queryString.charAt(i);
				if (queryString.charAt(i + 1) == '"') {
					phraseQueryEnd = true;
				}
			}

			// field read, is it phrase?, translate field to int val
			if (queryString.charAt(i) == ':') {
				if (queryString.charAt(i + 1) == '"') {
					phraseQuery = true;
				}
				// translate field string to int value
				switch (currentString) {
					case "title":
						field = 1;
						break;
					case "episodetitle":
						field = 2;
						break;
					case "plot":
						field = 3;
						break;
					case "year":
						field = 4;
						break;
					case "type":
						field = 5;
						break;
				}

				currentString = ""; // reset current string to process query
				catchQuery = true; // next part is the query
			}

			if ((catchQuery) && (queryString.charAt(i) == ' ') && (!phraseQuery)) {
				// token
				query = currentString;
				currentString = "";
				catchQuery = false;
				phraseQuery = false;
				tmpResultLlist.add(TokenQuery(query.substring(0, query.length() - 1).toLowerCase(), field));
				field = 0;
			}
			char a = queryString.charAt(i);

			//if ((catchQuery) && (queryString.charAt(i) == '"') && (phraseQuery)) {
			if (catchQuery && phraseQueryEnd) {
				// phrase
				query = currentString;
				currentString = "";
				catchQuery = false;
				phraseQuery = false;
				phraseQueryEnd = false;

				String tmpQuery = "";
				ArrayList<String> tmpTokenList = new ArrayList<>();
				for (int j = 0; j < query.length(); j++) {
					tmpQuery += query.charAt(j);
					if (query.charAt(j) == ' ') {
						tmpTokenList.add(tmpQuery.substring(0, tmpQuery.length() - 1).toLowerCase()); // remove trailing space and add token to tokenlist
						tmpQuery = ""; // reset tmpQuery variable
					}
				}

				tmpResultLlist.add(PhraseQuery(tmpTokenList, field));
				field = 0;
			}

			// AND beseitigen
			if (currentString.length() > 3) {
				if (currentString.substring(currentString.length() - 4, currentString.length()).equals("AND ")) {
					currentString = currentString.substring(currentString.length() - 3, currentString.length());
				}
			}
		}

		// combine results
		ArrayList<Integer> tmpResultList = new ArrayList<>();
		tmpResultList.addAll(MergeLists(tmpResultLlist));

		// iterate through results list and put "MV: " + Moviestrings (title lines) in it.
		HashSet<String> hashResults = new HashSet<>();
		for (int currentResult : tmpResultList) {
			hashResults.add(allMovies.get(currentResult).getTitleLine());
		}
		return hashResults;
	}


	/* Adds a  value of a movieID to a HashMap */
	private void addToHashmap(HashMap<String, HashSet<Integer>> hashMap, String value, Integer movieID) {
		if (hashMap.containsKey(value)) {
			hashMap.get(value).add(movieID);
		} else {
			// value has no entry yet, create a list for the value and store film in it
			HashSet<Integer> movieList = new HashSet<Integer>();
			movieList.add(movieID);
			hashMap.put(value, movieList);
		}
	}

	/* Gets a plot line and adds it to the hash map */
	private void addTokenFromPlotLineToHashMap(Integer movieID, String line) {
		token = "";

		// iterate through the line char by char and check for delimiters
		for (int i = 0; i < line.length(); i++) {

			// if a delimiter is found, add the token to the hash map and continue with the next word.
			if (line.charAt(i) == ' ' || line.charAt(i) == '.' || line.charAt(i) == ':' ||
				line.charAt(i) == '?' || line.charAt(i) == '!' || line.charAt(i) == ',') {

				if (!token.isEmpty()) { // we have to check if the token is empty first
					addToHashmap(hashPlot, token, movieID);
					allMovies.get(movieID).setPositionOfTermInPlot(token, plotTermCount++);
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
			allMovies.get(movieID).setPositionOfTermInPlot(token, plotTermCount++);
		}
	}

	/*
	 this method parses the year, title, episode title and adds it to the
	 * hash map. it basically works like this: first check the type (movie, tv, vg, ...).
	 * the episode type is a bit tricky: we have to parse the episode title first.
	 * after that we cut it off and parse the title and year like the other (see
	 * below on how to)
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
				allMovies.get(movieID).setPositionOfTermInEipsodeTitle(token, episodeTitleTermCount++);
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
		parseTitleAndYear(movieID, mvLine, false);
	}

	/* This method parses the year and the title. it starts with the year because it is always the last
	* part of the 'MV: ' - line. in the method above we already cut off the the types and before the types is
	* ALWAYS the year. after the year is parsed, we cut it off and parse the title from the start. we can
	* do that because we already cut off everything else could lead to misinformation.
	* for example: MV: Terminator (2033) (1995) -> MV: is already removed in buildIndices and after the year is
	* parsed it is cut off and the title is now unique: 'Terminator (2033)'
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
			allMovies.get(movieID).setPositionOfTermInTitle(token, titleTermCount++);
		}
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
		String query = "title:\"John\"";
		//String query = "title:\"Terminator Genisys\"";
		//String query = "plot:\"John Connor\"";
		Set<String> actualResult = bq.booleanQuery(query);
		System.out.println(actualResult);


/*		// parsing the queries' expected results from the results file
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
