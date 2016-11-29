// DO NOT CHANGE THIS PACKAGE NAME.
// TODO: remove global token as it is bad practice and maybe we can save some time, too.
package ue_inforet_bool;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
//import java.util.List;
//import java.util.Comparator;


public class BooleanQuery {
        private ArrayList<String> allMovies = new ArrayList<>(530000);
        private HashMap<Integer, String> allPlotPhrases = new HashMap<>(530000);
        private HashMap<Integer, String> allTitlePhrases = new HashMap<>(530000);
        private HashMap<Integer, String> allEpisodeTitlePhrases = new HashMap<>(220000);
        private HashMap<String, HashSet<Integer>> hashType = new HashMap<>(6);
        private HashMap<String, HashSet<Integer>> hashYear = new HashMap<>(140);
        private HashMap<String, HashSet<Integer>> hashPlot = new HashMap<>(700000);
        private HashMap<String, HashSet<Integer>> hashTitle = new HashMap<>(150000);
        private HashMap<String, HashSet<Integer>> hashEpisodeTitle = new HashMap<>(100000);
        private String tokenPhrase = "";
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
                                        // this check is necessary due to the fact that the first time we run through
                                        // this tokenPhrase is empty. we add here the tokenized phrase for
                                        // phrase search
                                        if (!tokenPhrase.isEmpty()) {
                                                allPlotPhrases.put(movieID, tokenPhrase);
                                                tokenPhrase = "";
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
                                        // tokenize the line and add the tokens to the hash map.
                                        addPlotLineToHashMap(movieID, StringUtils.substring(line, 4, line.length()).toLowerCase());
                                }
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                }
                // add the last plot phrase
                if (!tokenPhrase.isEmpty()) {
                        allPlotPhrases.put(movieID, tokenPhrase);
                }
                print(); // TODO: remove
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

        private void addPlotLineToHashMap(Integer movieID, String mvLine) {
                token = "";

                StringTokenizer st = new StringTokenizer(mvLine, " .,:!)", false);

                while (st.hasMoreTokens()) {
                        token = st.nextToken();
                        if (token.contains("\"")) {
                                tokenPhrase += token + " ";
                                continue;
                        }
                        addToHashMap(hashPlot, token, movieID);
                        tokenPhrase += token + " ";
                }
        }

        /**
         * TODO: Add better explanation
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
                        token = "";

                        while (st.hasMoreTokens()) {
                                token = st.nextToken();
                                if (token.contains("\"")) {
                                        tokenPhrase += token + " ";
                                        continue;
                                }
                                addToHashMap(hashEpisodeTitle, token, movieID);
                                tokenPhrase += token + " ";
                        }

                        // add the token for the phrase search to the array list
                        allEpisodeTitlePhrases.put(movieID, tokenPhrase);
                        tokenPhrase = "";

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

        /* this method parses the year and the title */
        private void parseTitleAndYear(int movieID, String mvLine, boolean isSeries) {
                token = "";
                int end = 3;

                // first, get the year right from behind
                // we start behind the last parenthesis and add only digits to the token
                // with end we compute the end for the tokenizer
                for (int i = mvLine.length() - 1; mvLine.charAt(i) != '('; i--) {
                        if (mvLine.charAt(i) >= '0' && mvLine.charAt(i) <= '9') {
                                token += mvLine.charAt(i);
                        }
                        end++;
                }
                // if the token contains exact 4 digits, we reverse it and add it to the hash map
                if (token.length() == 4) {
                        addToHashMap(hashYear, new StringBuilder(token).reverse().toString(), movieID);
                }
                StringTokenizer st;
                // then parse the title and add the token to hash map similar to addTokenFromPlotList
                if (isSeries) {
                        st = new StringTokenizer(mvLine.substring(1, mvLine.length() - end), " .,:!?", false);
                } else {

                        st = new StringTokenizer(mvLine.substring(0, mvLine.length() - end + 1), " .,:!?", false);
                }

                token = "";
                while (st.hasMoreTokens()) {
                        token = st.nextToken();
                        if (token.contains("\"")) {
                                tokenPhrase += token + " ";
                                continue;
                        }
                        addToHashMap(hashTitle, token, movieID);
                        tokenPhrase += token + " ";
                }
                allTitlePhrases.put(movieID, tokenPhrase);
                tokenPhrase = "";
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
                // TODO: AND, phrase queries

                // TODO, after checking for AND turn toLowerCase()

                //TODO parse the query and extract phrases, token and ANDs.
                // first, check for ANDS (string.contains(" AND ") should be fine)
                // if there are ANDS, split the queries, put them in a list and execute them
                // then "AND" the results and return the movies from the arraylist.
                // than check for phrases (string.contains("\"") sould be fine)
                // for phrase-query: split the phrase into tokens, then do the tokensearch
                // as result we get all movies with the tokens. on these movies we execute the boyer-moore
                // algorithm and return the results (only the numbers, not the titles)

                if (queryString.contains("\"")) {
                        // do query search
                        // first tokenize the string

                }
                char searchField = ' ';
                // check for the field to search in:
                if (queryString.indexOf("i") == 1) {
                        //cut the string for token search
                        tokenSearch(StringUtils.substring(queryString, 6, queryString.length()), 'i');

                }


                tokenSearch(queryString, searchField);
                return null;
        }

        /**
         * @param queryString The token to be searched.
         * @return A list which only contains the movieIDs in the array allMovies.
         */
        private List<Integer> tokenSearch(String queryString, char searchField) {
                List<Integer> matchingMovies = new ArrayList<>(32);

                // +++ title +++
                //if (queryString.indexOf("i") == 1) {
                if (searchField == 'i') {
                        if (hashTitle.containsKey(StringUtils.substring(queryString, 6, queryString.length()).toLowerCase())) {

                                for (int i : hashTitle.get(StringUtils.substring(queryString, 6, queryString.length()).toLowerCase())) {
                                        System.out.println(allMovies.get(i)); // TODO: remove!
                                        matchingMovies.add(i);
                                }
                                return matchingMovies;
                        }
                        return null;
                }
                // +++ plot +++
                if (queryString.startsWith("p")) {
                        if (hashPlot.containsKey(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
                                for (int i : hashPlot.get(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
                                        System.out.println(allMovies.get(i)); // TODO: remove!
                                        matchingMovies.add(i);
                                }
                                return matchingMovies;
                        }
                        return null;
                }
                // +++ type +++
                if (queryString.indexOf("y") == 1) {
                        if (hashType.containsKey(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
                                for (int i : hashType.get(StringUtils.substring(queryString, 5, queryString.length()).toLowerCase())) {
                                        System.out.println(allMovies.get(i)); // TODO: remove!
                                        matchingMovies.add(i);
                                }
                                return matchingMovies;
                        }
                        return null;
                }
                // +++ year +++
                if (queryString.startsWith("y")) {
                        if (hashYear.containsKey(StringUtils.substring(queryString, 5, queryString.length()))) {
                                for (int i : hashYear.get(StringUtils.substring(queryString, 5, queryString.length()))) {
                                        System.out.println(allMovies.get(i)); // TODO: remove!
                                        matchingMovies.add(i);
                                }
                                return matchingMovies;
                        }
                        return null;
                }
                // +++ episode title +++
                if (queryString.startsWith("e")) {
                        if (hashEpisodeTitle.containsKey(StringUtils.substring(queryString, 8, queryString.length()).toLowerCase())) {
                                for (int i : hashEpisodeTitle.get(StringUtils.substring(queryString, 8, queryString.length()).toLowerCase())) {
                                        System.out.println(allMovies.get(i)); // TODO: remove!
                                        matchingMovies.add(i);
                                }
                                return matchingMovies;
                        }
                        return null;
                }
                return null;
        }

        /* for testing */
        private void print() {
        /*System.out.println(hashEpisodeTitle);
        System.out.println(hashYear);
                System.out.println(hashType);
                System.out.println(hashPlot);
                System.out.println(hashTitle);
                int i = 0;
                while (i < allMovies.size()) {
                        System.out.println(allMovies.get(i));
                        i++;
                }*/
/*                System.out.println("years: ");
                for (Map.Entry<String, HashSet<Integer>> entry : hashYear.entrySet()) {
                        System.out.println(entry.getKey());
                }*/
                //System.out.println("years: " + hashYear.entrySet());

                /*System.out.println("sizes: ");
                System.out.println("allMovies: " + allMovies.size());
                System.out.println("allPlotPhrases: " + allPlotPhrases.size());
                System.out.println("allTitlePhrases: " + allTitlePhrases.size());
                System.out.println("allEpisodeTitlePhrases: " + allEpisodeTitlePhrases.size());
                System.out.println("hashType: " + hashType.size());
                System.out.println("hashYear: " + hashYear.size());
                System.out.println("hashPlot: " + hashPlot.size());
                System.out.println("hashTitle: " + hashTitle.size());
                System.out.println("hashEpisodeTitle: " + hashEpisodeTitle.size());*/
                //System.out.println(hashTitle.entrySet());
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

                //String query = "title:salvation";
                //String query = "title:focus";
                //String query = "title:\"Redemption Rise\"";
                //String query = "title:Genisys";
                //String query = "plot:Skynet";
                //String query = "type:series";
                String query = "year:2007";
                //String query = "episode:party";
                //String query = "episode:sexy";
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
