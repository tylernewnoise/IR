// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.regex.Pattern;

public class BooleanQuery {

        private ArrayList<String> allMovies = new ArrayList<>(); //later convert to array via allMovies.toArray? otherwise the allMovies[i] notation does not work but the uglier allMovies.get(i)
        private HashMap<String, HashSet<Integer>> hashAllYears = new HashMap<>();
        public HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>();
        private HashMap<String, HashSet<Integer>> hashType = new HashMap<>();
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
        //Christoph
        public void buildIndices(String plotFile) {
                int nextMovieID = 0;  // int eventuell zu klein ;)
                int movieID = 0;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {
                        String line;
                        while ((line = reader.readLine()) != null) {

                                // is it an MV: line?
                                if (Pattern.compile("^MV:").matcher(line).find()) {
                                        // add an entry and increase movieID
                                        movieID = nextMovieID;
                                        nextMovieID++;
                                        // add Film to ID/Film List
                                        allMovies.add(movieID, line);

                                        List<String> termList = tokenizeRowToTerms(line);

                                        // run MV line methods
                                        insertYearToHashMap(movieID, termList);
                                        insertTitleToHashMap(movieID, termList);
                                        insertTypeToHashMap(movieID, termList, line);
                                }
                                // is it an PL: line?
                                if (Pattern.compile("^PL:").matcher(line).find()) {
                                        // getting List<String> from tokenizeRowToTerms (line);
                                        // insertPlotRowToHashMap(movieID, plotrow);

                                        insertPlotRowToHashMap(movieID, line);
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

        private List<String> tokenizeRowToTerms(String row) {
                List<String> wordList = new ArrayList<>();
                for (String retval : row.split(" |,|\\.|:|!|\\?")) {
                        if (!retval.isEmpty()) {
                                wordList.add(retval.toLowerCase());
                        }
                }
                return wordList;
        }

        //Jonas
        private void insertPlotRowToHashMap(int movieID, String plotrow) {
        }

        private void insertYearToHashMap(int movieID, List<String> termList) {
                //TODO: handle:
                // MV: Displaced (2014/II)
                // MV: Displaced (2014/III)
                // MV: The Ambassador (????/IV)
                // MV: The Ambassador (1984)
                //get the year out of string
                String year = null;
                boolean openBracket = false;
                for (String term : termList) {
                        if (term.equals("(")) {
                                openBracket = true;
                                continue;
                        }
                        if (openBracket && term.equals(")")) {
                                year = "????";
                                break;
                        }
                        if (term.contains("(") && term.contains(")") && term.substring(term.indexOf("(") + 1, term.indexOf(")")).matches("\\d+")) {
                                year = term.substring(term.indexOf("(") + 1, term.indexOf(")"));
                                break;
                        }
                }

                //store year and movie in hashset
                addToHashMap(hashAllYears, year, movieID);
        }

        //Benni
        private void insertTitleToHashMap(int movieID, List<String> termList) {
                boolean MV = false;
                boolean openBracket = false;
                for (String term : termList) {
                        if (term.equals("mv")) {
                                MV = true;
                                continue;
                        }
                        if (term.equals("(")) {
                                openBracket = true;
                                continue;
                        }
                        if (openBracket && term.equals(")")) {
                                break;
                        }
                        if (term.contains("(") && term.contains(")") &&
                                term.substring(term.indexOf("(") + 1, term.indexOf(")")).matches("\\d+")) {
                                break;
                        }
                        //we add a term of the title (after MV and before the year)
                        if (MV) {
                                //store
                                addToHashMap(hashTitle, term, movieID);
                        }
                }
        }

        //Falko
        private void insertTypeToHashMap(int movieID, List<String> termList, String titleRow) {
                // get the year out of string
                String type = null;
                String lastTerm = termList.get(termList.size() - 1);
                String betweenBrackets = lastTerm.substring(lastTerm.indexOf("(") + 1, lastTerm.indexOf(")"));

                //find a term starting with " and ending with "
                for (String term : termList) {
                        //make sure the term inqoutes is before the year
                        if (term.contains("(") && term.contains(")") &&
                                term.substring(term.indexOf("(") + 1, term.indexOf(")")).matches("\\d+")) {
                                break;
                        }
                        if (term.contains("\"") && term.startsWith("\"") && term.endsWith("\"")) {
                                //qoute = true;
                                type = "series";
                        }
                }

                // videogame
                if (lastTerm.contains("(") && lastTerm.contains(")") && betweenBrackets.equals("vg")) {
                        type = "videogame";
                // television
                } else if (lastTerm.contains("(") && lastTerm.contains(")") && betweenBrackets.equals("tv")) {
                        type = "television";
                // video
                } else if (lastTerm.contains("(") && lastTerm.contains(")") && betweenBrackets.equals("v")) {
                        type = "video";
                //episode
                } else if (lastTerm.endsWith("}")) {
                        //handle "MV: Disparity (2013) {{SUSPENDED}}"
                        if (!lastTerm.contains("}}")) {
                                type = "episode";
                                insertEpisodeTitleToHashMap(movieID, titleRow);
                        }
                } else {
                //type is nothing of the above so it has to be a movie
                        type = "movie";
                }

                //store
                addToHashMap(hashType, type, movieID);
        }

        //Falko
        private void insertEpisodeTitleToHashMap(int movieID, String titleRow) {
                String betweenBrackets = titleRow.substring(titleRow.indexOf("{") + 1, titleRow.indexOf("}"));
                for (String term : tokenizeRowToTerms(betweenBrackets)) {
                        addToHashMap(hashEpisodeTitle, term, movieID);
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