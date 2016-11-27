// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.Set;
//import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
//import java.util.Comparator;

public class BooleanQuery {
        private ArrayList<String> allMovies = new ArrayList<>();
        private HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashYear = new HashMap<>();
	private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>();
	private String token = "";

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

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                                // is it an MV: line?
				if (line.startsWith("M")) {
					// add an entry and increase movieID
                                        movieID = nextMovieID++;
					allMovies.add(movieID, line);
					// add movie to list and add title, type and year to the hash maps
                                        insertTitleTypeYearToHashMap(movieID, line);
                                }
                                // is it an PL: line?
				if (StringUtils.substring(line, 0, 3).contains("PL:")) {
					// tokenize the line and add the tokens to the hash map.
					addTokenFromPlotLineToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
				}
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                }
		//print(); // TODO: remove
	}

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

        private void addTokenFromPlotLineToHashMap(Integer movieID, String line) {
		token = "";

                // iterate through the line and check for delimiters
                for (int i = 0; i < line.length(); i++) {

                        // if a delimiter is found, add the token to the hash map and continue with the next word.
                        if (line.charAt(i) == ' ' || line.charAt(i) == '.' || line.charAt(i) == ':' ||
                                line.charAt(i) == '?' || line.charAt(i) == '!' || line.charAt(i) == ',') {

				if (!token.isEmpty()) {
					addToHashMap(hashPlot, token, movieID);
				}

				token = "";
				continue;
			}
                        // add chars to the token
			token += line.charAt(i);
		}

                // when we reached the end of the line, we want to add the last token as well
		if (!token.isEmpty()) {
			addToHashMap(hashPlot, token, movieID);
		}
	}

        /**
         * In this code-pile-of-junk we add title, type and year to the hash maps.
         * Since series and episodes are a bit tricky/different they are handled
         * in a separate way. For info on how the code works have a look at episodes or
         * call Batman.
         *
         * @param movieID Gives each entry in our indices a unique number.
         * @param mvLine  Contains all information for the different entries.
         */
        private void insertTitleTypeYearToHashMap(int movieID, String mvLine) {
                mvLine = mvLine.toLowerCase();

                // remove {{SUSPENDED}}
                if (mvLine.contains("{{suspended}}")) {
                        mvLine = mvLine.replace(" {{suspended}}", "");
                }

                // +++ series +++
                if (mvLine.contains("mv: \"") && !mvLine.contains("}")) {
                        addToHashMap(hashType, "series", movieID);

                        int bracketStart = 8;
			token = "";

                        for (int i = 5; mvLine.charAt(i) != '\"'; i++) {
				if (mvLine.charAt(i) == ' ' || mvLine.charAt(i) == '.' || mvLine.charAt(i) == ':' ||
					mvLine.charAt(i) == '?' || mvLine.charAt(i) == '!' || mvLine.charAt(i) == ',') {

					if (!token.isEmpty()) {
						addToHashMap(hashTitle, token, movieID);
					}
					token = "";
					bracketStart++;
					continue;
				}
				token += mvLine.charAt(i);
				bracketStart++;
			}
			if (!token.isEmpty()) {
				addToHashMap(hashTitle, token, movieID);
			}

			token = "";
			for (int i = bracketStart; mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9'; i++) {
				token += mvLine.charAt(i);
			}
			if (token.length() == 4) {
				addToHashMap(hashYear, token, movieID);
			}
			return;
                }
                // +++ episode +++
                if (mvLine.contains("mv: \"") && mvLine.contains("}")) {
                        // add type to HashMap
                        addToHashMap(hashType, "episode", movieID);

			token = ""; // reset the token to 0
			int bracketStart = 8; // set the index for the year (yes, I know it's actually a parenthesis)

			// get and add title to hash map, start at the first quotation mark which ich always on
			// index 5 in the string and add every char to the token. add the token to the hash map.
			// repeat until the second quotation mark is reached.
			for (int i = 5; mvLine.charAt(i) != '\"'; i++) {

				// tokenize the title - see addTokenFromPlotLineToHashMap for description
				if (mvLine.charAt(i) == ' ' || mvLine.charAt(i) == '.' || mvLine.charAt(i) == ':' ||
					mvLine.charAt(i) == '?' || mvLine.charAt(i) == '!' || mvLine.charAt(i) == ',') {

					if (!token.isEmpty()) {
						addToHashMap(hashTitle, token, movieID);
					}
					token = "";
					bracketStart++;
					continue;
				}
				// add chars to the token
				token += mvLine.charAt(i);
				bracketStart++;
			}
			if (!token.isEmpty()) {
				addToHashMap(hashTitle, token, movieID);
			}
			token = "";

			// since wie already got the end of the title (thanks to bracketStart) and know where to start,
			// we can move on to the year directly. we only add digits to the token. if there are no
			// digits in the year field (????) the token is not added.
			for (int i = bracketStart; mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9'; i++) {
				token += mvLine.charAt(i);
				bracketStart++;
			}
			if (token.length() == 4) {
				addToHashMap(hashYear, token, movieID);
			}

			// we don't know exactly, if the year was a 4-digit number or some special shit. so let's see
			// see and if no, we have to iterate a bit to get to the start of the episode title.
			if (mvLine.charAt(bracketStart) == ')') {
				bracketStart += 3;
                        } else {
                                while (mvLine.charAt(bracketStart - 1) != '{') {
                                        bracketStart++;
                                }
                        }

			token = "";
			// yay, we found the start of the episode title, so let's tokenize this as well.
			for (int i = bracketStart; mvLine.charAt(i) != '}'; i++) {
				if (mvLine.charAt(i) == ' ' || mvLine.charAt(i) == '.' || mvLine.charAt(i) == ':' ||
					mvLine.charAt(i) == '?' || mvLine.charAt(i) == '!' || mvLine.charAt(i) == ',') {
					if (!token.isEmpty()) {
						addToHashMap(hashEpisodeTitle, token, movieID);
					}
					token = "";
					bracketStart++;
					continue;
				}
				token += mvLine.charAt(i);
				bracketStart++;
			}
			if (!token.isEmpty()) {
				addToHashMap(hashEpisodeTitle, token, movieID);
			}

                        // and we're done, no need to check the other if-statements.
                        return;
                }
                // +++ television +++
                if (mvLine.contains(") (tv)")) {
                        addToHashMap(hashType, "television", movieID);
			addTitleAndYear(movieID, mvLine);
			return;
		}
                // +++ video +++
                if (mvLine.contains(") (v)")) {
                        addToHashMap(hashType, "video", movieID);
			addTitleAndYear(movieID, mvLine);
			return;
		}
                // +++ video game +++
                if (mvLine.contains(") (vg)")) {
                        addToHashMap(hashType, "videogame", movieID);
			addTitleAndYear(movieID, mvLine);
			return;
		}
                // +++ movie +++
                addToHashMap(hashType, "movie", movieID);
		addTitleAndYear(movieID, mvLine);
	}

	/* Just a helper method to not blow the code even more. */
	private void addTitleAndYear(int movieID, String mvLine) {
		token = "";
		int bracketStart = 6;

                for (int i = 4; mvLine.charAt(i + 1) != '('; i++) {
			if (mvLine.charAt(i) == ' ' || mvLine.charAt(i) == '.' || mvLine.charAt(i) == ':' ||
				mvLine.charAt(i) == '?' || mvLine.charAt(i) == '!' || mvLine.charAt(i) == ',') {

				if (!token.isEmpty()) {
					addToHashMap(hashTitle, token, movieID);
				}
				token = "";
				bracketStart++;
				continue;
			}
			token += mvLine.charAt(i);
			bracketStart++;
		}
		if (!token.isEmpty()) {
			addToHashMap(hashTitle, token, movieID);
		}

		token = "";
		for (int i = bracketStart; mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9'; i++) {
			token += mvLine.charAt(i);
		}
		if (token.length() == 4) {
			addToHashMap(hashYear, token, movieID);
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
        public Set<String> booleanQuery(String queryString) {
		System.out.println("");
		HashSet<String> results = new HashSet<>();
		// TODO: AND, phrase queries
/*                if (queryString.contains(" AND ")) {
			//call AND - Verknüpfung
                }
                if (queryString.contains("\"")) {
                        // call phrase-search
                }*/

		// +++++ token-search +++++
		// +++ title +++
		if (queryString.indexOf("i") == 1) {
			if (hashTitle.containsKey(StringUtils.substring(queryString, 6, queryString.length()).toLowerCase())) {
				for (int i : hashTitle.get(StringUtils.substring(queryString, 6, queryString.length()).toLowerCase())) {
					System.out.println(allMovies.get(i)); // TODO: remove!
					results.add(allMovies.get(i));
				}
				return results;
			}
			return null;
		}
		// +++ plot +++
		if (queryString.startsWith("p")) {
			if (hashPlot.containsKey(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
				for (int i : hashPlot.get(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
					System.out.println(allMovies.get(i)); // TODO: remove!
					results.add(allMovies.get(i));
				}
				return results;
			}
			return null;
			// goto plot hashmap
		}
		// +++ type +++
		if (queryString.indexOf("y") == 1) {
			if (hashType.containsKey(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
				for (int i : hashType.get(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
					System.out.println(allMovies.get(i)); // TODO: remove!
					results.add(allMovies.get(i));
				}
				return results;
			}
			return null;
		}
		// +++ year +++
		if (queryString.startsWith("y")) {
			if (hashYear.containsKey(StringUtils.substring(queryString, 5, queryString.length()))) {
				for (int i : hashYear.get(StringUtils.substring(queryString, 5, queryString.length()))) {
					System.out.println(allMovies.get(i)); // TODO: remove!
					results.add(allMovies.get(i));
				}
				return results;
			}
			return null;
		}
		// +++ episode title +++
		if (queryString.startsWith("e")) {
			if (hashEpisodeTitle.containsKey(StringUtils.substring(queryString, 8, queryString.length()).toLowerCase())) {
				for (int i : hashEpisodeTitle.get(StringUtils.substring(queryString, 8, queryString.length()).toLowerCase())) {
					System.out.println(allMovies.get(i)); // TODO: remove!
					results.add(allMovies.get(i));
				}
				return results;
			}
			return null;
		}
		return null;
	}

        /* for testing */
	/*private void print() {
		System.out.println(hashEpisodeTitle);
                System.out.println(hashYear);
                System.out.println(hashType);
                System.out.println(hashPlot);
                System.out.println(hashTitle);
                int i = 0;
                while (i < allMovies.size()) {
                        System.out.println(allMovies.get(i));
                        i++;
                }
        }*/

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
                        .println("memory: " + ((runtime.totalMemory() - mem) / (1048576l))
                                + " MB (rough estimate)");

		String query = "title:Terminator";
		//String query = "title:Genisys";
		//String query = "plot:Skynet";
		//String query = "type:movie";
		//String query = "year:2004";
		//String query = "episode:party";
		Set<String> result = bq.booleanQuery(query);

/*              // parsing the queries that are to be run from the queries file
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
