// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

public class BooleanQuery {
        private ArrayList<String> allMovies = new ArrayList<>();
        private HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashAllYears = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>();

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
                                if (org.apache.commons.lang3.StringUtils.substring(line, 0, 3).contains("MV:")) {
                                        // add an entry and increase movieID
                                        allMovies.add(movieID, line);
                                        movieID = nextMovieID++;
                                        // add movie to list and add title, type and year to the hash maps
                                        insertTitleTypeYearToHashMap(movieID, line);
                                }
                                // is it an PL: line?
                                if (org.apache.commons.lang3.StringUtils.substring(line, 0, 3).contains("PL:")) {
                                        // tokenize the line and add the tokens to the hash map.
                                        addTokenFromPlotLineToHashMap(movieID, org.apache.commons.lang3.StringUtils.substring(line, 4, line.length()).toLowerCase());
                                }
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                }
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
                String tokenString = "";

                // iterate through the line and check for delimiters
                for (int i = 0; i < line.length(); i++) {

                        // if a delimiter is found, add the token to the hash map and continue with the next word.
                        if (line.charAt(i) == ' ' || line.charAt(i) == '.' || line.charAt(i) == ':' ||
                                line.charAt(i) == '?' || line.charAt(i) == '!' || line.charAt(i) == ',') {

                                if (!tokenString.isEmpty()) {
                                        addToHashMap(hashPlot, tokenString, movieID);
                                }
                                tokenString = "";
                                continue;
                        }
                        // add chars to the token
                        tokenString += line.charAt(i);
                }

                // when we reached the end of the line, we want to add the last token as well
                if (!tokenString.isEmpty()) {
                        addToHashMap(hashPlot, tokenString, movieID);
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
                        String title = "";
                        String year = "";

                        for (int i = 5; mvLine.charAt(i) != '\"'; i++) {
                                title += mvLine.charAt(i);
                                bracketStart++;
                        }
                        addToHashMap(hashTitle, title, movieID);

                        for (int i = bracketStart; mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9'; i++) {
                                year += mvLine.charAt(i);
                        }

                        if (year.length() == 4) {
                                addToHashMap(hashAllYears, year, movieID);
                        }
                        return;
                }
                // +++ episode +++
                if (mvLine.contains("mv: \"") && mvLine.contains("}")) {
                        // add type to HashMap
                        addToHashMap(hashType, "episode", movieID);

                        String year = "";
                        String title = "";
                        String episodeTitle = "";
                        int bracketStart = 8;

                        // get and add title to HashMap, start at the first '"' and add every char to the titleString
                        // until the end is reached.
                        for (int i = 5; mvLine.charAt(i) != '\"'; i++) {
                                title += mvLine.charAt(i);
                                bracketStart++;
                        }
                        addToHashMap(hashTitle, title, movieID);

                        // since wie already got the end of the title and know where to start, we can move on to
                        // the year directly
                        for (int i = bracketStart; mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9'; i++) {
                                year += mvLine.charAt(i);
                                bracketStart++;
                        }
                        if (year.length() == 4) {
                                addToHashMap(hashAllYears, year, movieID);
                        }

                        // we don't know exactly, if the year was a 4-digit number or some special shit
                        // so let's see and if no, we have to iterate a bit to get to the episode title.
                        if (mvLine.charAt(bracketStart) == ')') {
                                bracketStart += 3;
                        } else {
                                while (mvLine.charAt(bracketStart - 1) != '{') {
                                        bracketStart++;
                                }
                        }

                        // yay, we found the start of the episodeTitle...
                        for (int i = bracketStart; mvLine.charAt(i) != '}'; i++) {
                                episodeTitle += mvLine.charAt(i);

                        }
                        addToHashMap(hashEpisodeTitle, episodeTitle, movieID);

                        // and we're done, no need to check the other if-statements.
                        return;
                }
                // +++ television +++
                if (mvLine.contains(") (tv)")) {
                        addToHashMap(hashType, "television", movieID);
                        addYearAndTitle(movieID, mvLine);
                        return;
                }
                // +++ video +++
                if (mvLine.contains(") (v)")) {
                        addToHashMap(hashType, "video", movieID);
                        addYearAndTitle(movieID, mvLine);
                        return;
                }
                // +++ video game +++
                if (mvLine.contains(") (vg)")) {
                        addToHashMap(hashType, "videogame", movieID);
                        addYearAndTitle(movieID, mvLine);
                        return;
                }
                // +++ movie +++
                addToHashMap(hashType, "movie", movieID);
                addYearAndTitle(movieID, mvLine);
        }

        /* Just a helper method to not blow the code. */
        private void addYearAndTitle(int movieID, String mvLine) {
                String title = "";
                String year = "";
                int bracketStart = 6;

                for (int i = 4; mvLine.charAt(i + 1) != '('; i++) {
                        title += mvLine.charAt(i);
                        bracketStart++;
                }
                addToHashMap(hashTitle, title, movieID);

                for (int i = bracketStart; mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9'; i++) {
                        year += mvLine.charAt(i);
                }
                if (year.length() == 4) {
                        addToHashMap(hashAllYears, year, movieID);
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
                // TODO: insert code here
                return new HashSet<>();
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
                        .println("memory: " + ((runtime.totalMemory() - mem) / (1048576l))
                                + " MB (rough estimate)");

/*                // parsing the queries that are to be run from the queries file
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
