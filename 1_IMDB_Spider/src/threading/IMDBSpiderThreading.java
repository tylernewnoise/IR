/* Spider - with threads */
package threading;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringEscapeUtils;
import org.htmlcleaner.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.cedarsoftware.util.io.*;

import java.net.URL;
import java.net.URLEncoder;

import java.util.List;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class IMDBSpiderThreading {
        private ExecutorService executorService;

        private IMDBSpiderThreading() {
        }

        /**
         * For each title in file movieListJSON:
         * <p>
         * <pre>
         * You should:
         * - First, read a list of 500 movie titles from the JSON file in 'movieListJSON'.
         *
         * - Secondly, for each movie title, perform a web search on IMDB and retrieve
         * movie’s URL: http://akas.imdb.com/find?q=<MOVIE>&s=tt&ttype=ft
         *
         * - Thirdly, for each movie, extract metadata (actors, budget, description)
         * from movie’s URL and store to a JSON file in directory 'outputDir':
         *    http://www.imdb.com/title/tt0499549/?ref_=fn_al_tt_1 for Avatar - store
         * </pre>
         *
         * @param movieListJSON JSON file containing movie titles
         * @param outputDir     output directory for JSON files with metadata of movies
         */
        private void fetchIMDBMovies(String movieListJSON, String outputDir) throws IOException {
                JSONParser parser = new JSONParser();
                int i = 0;

                System.out.println("Found " + Runtime.getRuntime().availableProcessors() + " Cores, will start four threads per core...pls hold on.");
                try {
                        // create an object which reads the given json file
                        Object jsonFile = parser.parse(new FileReader(movieListJSON));
                        // cast object to jsonArray
                        JSONArray jsonArray = (JSONArray) jsonFile;

                        // four threads per core should be fine
                        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
                        // iterate through the list of movies
                        while (i < jsonArray.size()) {
                                // get the title
                                String title = (String) ((JSONObject) jsonArray.get(i)).get("movie_name");
                                // create new movie-object
                                Movie movieToFetch = new Movie();
                                movieToFetch.setTitle(title);
                                startThreads(i, movieToFetch, outputDir);
                                i++;
                        }
                        executorService.shutdown();
                } catch (ParseException e) {
                        e.printStackTrace();
                }
        }

        private void startThreads(int i, Movie movieName, String outputdir) {
                executorService.submit(new Callable<Void>() {
                        public Void call() {
                                storeMovieToJson(i, movieName, outputdir);
                                return null;
                        }
                });
        }

        /**
         * Download of metadata and store it in object
         *
         * @param movieName object of type Movie containing only the URL for the searched Movie
         * @param movieNumber number of the movie to be fetched, we use this as file name for the JSON-file
         * @param outputDir directory where the created JSON-file is stored
         */
        @SuppressWarnings("unchecked")
        private static void storeMovieToJson(int movieNumber, Movie movieName, String outputDir) {
                //System.out.println(Thread.currentThread().getName());

                try {
                        URL imdbURL = new URL("http://akas.imdb.com/find?q=" + URLEncoder.encode(movieName.getTitle(), "UTF-8") + "&s=tt&ttype=ft");
                        //create cleaner
                        HtmlCleaner cleaner = new HtmlCleaner();

                        // do parsing
                        TagNode root = cleaner.clean(imdbURL);

                        //xPath for first Element on IMDB search
                        XPather xPathURL = new XPather("//table[@class='findList']//td[@class='result_text']//a[@href]/@href");

                        Object[] obj = xPathURL.evaluateAgainstNode(root);
                        if (obj != null && obj.length > 0) {
                                movieName.setUrl("http://akas.imdb.com" + obj[0]);
                        } else {
                                System.out.print("No movie found on IMDb for this search query");
                                return;
                        }

                        // get Data form HTML
                        URL urlAccess = new URL(movieName.getUrl());

                        // clean html
                        TagNode tagNode = new HtmlCleaner().clean(urlAccess);
                        Document doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);

                        // JAPX Interface to query it
                        XPath xpath = XPathFactory.newInstance().newXPath();

                        // Queries + add to object
                        String year = (String) xpath.evaluate("//span[@id='titleYear']/a/text()", doc, XPathConstants.STRING);
                        year = StringEscapeUtils.unescapeHtml3(year);
                        movieName.setYear(year);

                        String description = (String) xpath.evaluate("normalize-space(//div[@itemprop='description']/p/text())", doc, XPathConstants.STRING);
                        description = StringEscapeUtils.unescapeHtml3(description);
                        movieName.setDescription(description);

                        String budget = (String) xpath.evaluate("normalize-space(//h4[text()='Budget:']/following-sibling::text()[1])", doc, XPathConstants.STRING);
                        budget = StringEscapeUtils.unescapeHtml3(budget);
                        movieName.setBudget(budget);

                        String gross = (String) xpath.evaluate("normalize-space(//h4[text()='Gross:']/following-sibling::text())", doc, XPathConstants.STRING);
                        gross = StringEscapeUtils.unescapeHtml3(gross);
                        movieName.setGross(gross);

                        String ratingValue = (String) xpath.evaluate("//span[@itemprop='ratingValue']/text()", doc, XPathConstants.STRING);
                        ratingValue = StringEscapeUtils.unescapeHtml3(ratingValue);
                        movieName.setRatingValue(ratingValue);

                        String ratingCount = (String) xpath.evaluate("//span[@itemprop='ratingCount']/text()", doc, XPathConstants.STRING);
                        ratingCount = StringEscapeUtils.unescapeHtml3(ratingCount);
                        movieName.setRatingCount(ratingCount);

                        String duration = (String) xpath.evaluate("normalize-space(//time[@itemprop='duration']/text())", doc, XPathConstants.STRING);
                        duration = StringEscapeUtils.unescapeHtml3(duration);
                        movieName.setDuration(duration);

                        NodeList genreList = (NodeList) xpath.evaluate("//div[@itemprop='genre']//a/text()", doc, XPathConstants.NODESET);
                        List<String> genreListArray = new ArrayList<>();
                        for (int i = 0; i < genreList.getLength(); i++) {
                                String list = genreList.item(i).getNodeValue();
                                list = list.substring(1);
                                list = StringEscapeUtils.unescapeHtml3(list);
                                genreListArray.add(list);
                        }
                        movieName.setGenreList(genreListArray);

                        NodeList castList = (NodeList) xpath.evaluate("//td[@itemprop='actor']/a/span[@itemprop='name']/text()", doc, XPathConstants.NODESET);
                        List<String> castListArray = new ArrayList<>();
                        for (int i = 0; i < castList.getLength(); i++) {
                                String list = castList.item(i).getNodeValue();
                                list = StringEscapeUtils.unescapeHtml3(list);
                                castListArray.add(list);
                        }
                        movieName.setCastList(castListArray);

                        NodeList characterList = (NodeList) xpath.evaluate("//td[@class='character']/div/a/text()", doc, XPathConstants.NODESET);
                        List<String> characterListArray = new ArrayList<>();
                        for (int i = 0; i < characterList.getLength(); i++) {
                                String list = characterList.item(i).getNodeValue();
                                list = StringEscapeUtils.unescapeHtml3(list);
                                characterListArray.add(list);
                        }
                        movieName.setCharacterList(characterListArray);

                        NodeList directorList = (NodeList) xpath.evaluate("//span[@itemprop='director']/a/span[@itemprop='name']/text()", doc, XPathConstants.NODESET);
                        List<String> directorListArray = new ArrayList<>();
                        for (int i = 0; i < directorList.getLength(); i++) {
                                String list = directorList.item(i).getNodeValue();
                                list = StringEscapeUtils.unescapeHtml3(list);
                                directorListArray.add(list);
                        }
                        movieName.setDirectorList(directorListArray);

                        NodeList countryList = (NodeList) xpath.evaluate("//div[@id='titleDetails']/div//a[contains(@href, 'countries')]/text()", doc, XPathConstants.NODESET);
                        List<String> countryListArray = new ArrayList<>();
                        for (int i = 0; i < countryList.getLength(); i++) {
                                String list = countryList.item(i).getNodeValue();
                                list = StringEscapeUtils.unescapeHtml3(list);
                                countryListArray.add(list);
                        }
                        movieName.setCountryList(countryListArray);

                } catch (Exception e) {
                        e.printStackTrace();
                }

                JSONObject movieToJSONObejct = new JSONObject();

                movieToJSONObejct.put("url", movieName.getUrl());
                movieToJSONObejct.put("year", movieName.getYear());
                movieToJSONObejct.put("title", movieName.getTitle());
                movieToJSONObejct.put("gross", movieName.getGross());
                movieToJSONObejct.put("budget", movieName.getBudget());
                movieToJSONObejct.put("castList", movieName.getCastList());
                movieToJSONObejct.put("duration", movieName.getDuration());
                movieToJSONObejct.put("genreList", movieName.getGenreList());
                movieToJSONObejct.put("countryList", movieName.getCountryList());
                movieToJSONObejct.put("description", movieName.getDescription());
                movieToJSONObejct.put("ratingCount", movieName.getRatingCount());
                movieToJSONObejct.put("ratingValue", movieName.getRatingValue());
                movieToJSONObejct.put("directorList", movieName.getDirectorList());
                movieToJSONObejct.put("characterList", movieName.getCharacterList());

                String fileName = outputDir + "/" + movieNumber + ".json";
                String jsonString = "[" + movieToJSONObejct.toJSONString() + "]";
                String jsonBeauty = JsonWriter.formatJson(jsonString);

                try (PrintWriter toFile = new PrintWriter(fileName, "UTF-8")) {
                        System.out.println("Downloading to JSON: " + movieName.getTitle() + " - saved in -> " + fileName);
                        toFile.println(jsonBeauty);
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        public static void main(String argv[]) throws IOException {
                String moviesPath = "";
                String outputDir = "";

                if (argv.length == 2) {
                        moviesPath = argv[0];
                        outputDir = argv[1];
                } else if (argv.length != 0) {
                        System.out.println("Call with: BJ.IMDBSpider.jar <moviesPath> <outputDir>");
                        System.exit(0);
                }

                IMDBSpiderThreading sp = new IMDBSpiderThreading();
                sp.fetchIMDBMovies(moviesPath, outputDir);
        }
}