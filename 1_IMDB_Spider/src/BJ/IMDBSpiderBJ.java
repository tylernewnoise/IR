/* Final Version BJ.IMDBSpider - Version for Benny & Jonas */
package BJ;

import threading.Movie;

import org.htmlcleaner.*;

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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;

public class IMDBSpiderBJ {

        public IMDBSpiderBJ() {
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
         * @throws IOException
         */
        public void fetchIMDBMovies(String movieListJSON, String outputDir) throws IOException {
                Movie[] movieArr;
                movieArr = fetchMovieList(movieListJSON);

                for (int i = 0; i < movieArr.length; i++) {
                        Movie tmp = movieArr[i];
                        storeToJson(i, htmlToMetaData(titleToURL(tmp)), outputDir);
                }
        }

        /**
         * Gets filenames from JSON File, creates Movie Objects and stores each title in one Movie object.
         * Movie Objects are stored in Array of Movie and returned from method.
         *
         * @param movieListJSON Path to JSON File
         * @return movieArr
         * Array of Movie Objects
         **/
        public static Movie[] fetchMovieList(String movieListJSON) throws IOException {
                JSONParser parser = new JSONParser();
                Movie[] movieArr = null;
                try {
                        //create an Object which reads json file
                        Object jsonFile = parser.parse(new FileReader(movieListJSON));
                        //cast object to JSONArray
                        JSONArray jsonArray = (JSONArray) jsonFile;

                        int numberOfMovies = jsonArray.size();
                        movieArr = new Movie[numberOfMovies];

                        for (int i = 0; i < numberOfMovies; i++) {
                                //run down the movieArray
                                String title = (String) ((JSONObject) jsonArray.get(i)).get("movie_name");
                                movieArr[i] = new Movie();
                                movieArr[i].setTitle(title);
                        }
                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                } catch (ParseException e) {
                        e.printStackTrace();
                }
                return movieArr;
        }

        /**
         * Find URL from imdb.com search
         *
         * @param film Name of movie extracted from movielist.json
         * @return URLofMovie
         * The URL of the searched movie
         * @throws IOException
         */
        private static Movie titleToURL(Movie film) throws IOException {
                //Construct URL
                String encodedMovieName = URLEncoder.encode(film.getTitle(), "UTF-8");
                String constructedURL = ("http://akas.imdb.com/find?q=" + encodedMovieName + "&s=tt&ttype=ft");

                URL imdbURL = new URL(constructedURL);
                //create cleaner
                HtmlCleaner cleaner = new HtmlCleaner();

                try {
                        // do parsing
                        TagNode root = cleaner.clean(imdbURL);

                        //xPath for first Element on IMDB search
                        XPather xpath = new XPather("//table[@class='findList']//td[@class='result_text']//a[@href]/@href");

                        Object[] obj = xpath.evaluateAgainstNode(root);
                        if (obj != null && obj.length > 0) {
                                film.setUrl("http://akas.imdb.com" + obj[0]);
                        } else {
                                System.out.print("No movie found on IMDb for this search query");
                                return null;
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return film;
        }

        /**
         * Download of metadata and store it in object
         *
         * @param movieObjekt Object of type Movie containing only the URL for the searched Movie
         * @return MovieObject
         * Object of type Movie containing fetched data from imdb.com
         * @throws IOException
         */
        private static Movie htmlToMetaData(Movie movieObjekt) throws IOException {
                try {
                        URL urlAccess = new URL(movieObjekt.getUrl());
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlAccess.openStream()));

                        // clean html
                        TagNode tagNode = new HtmlCleaner().clean(in);
                        org.w3c.dom.Document doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);

                        // JAPX Interface to query it
                        XPath xpath = XPathFactory.newInstance().newXPath();

                        // Queries + add to object
                        String year = (String) xpath.evaluate("//span[@id='titleYear']/a/text()", doc, XPathConstants.STRING);
                        year = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(year);
                        movieObjekt.setYear(year);

                        String description = (String) xpath.evaluate("normalize-space(//div[@itemprop='description']/p/text())", doc, XPathConstants.STRING);
                        description = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(description);
                        movieObjekt.setDescription(description);

                        String budget = (String) xpath.evaluate("normalize-space(//h4[text()='Budget:']/following-sibling::text()[1])", doc, XPathConstants.STRING);
                        budget = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(budget);
                        movieObjekt.setBudget(budget);

                        String gross = (String) xpath.evaluate("normalize-space(//h4[text()='Gross:']/following-sibling::text())", doc, XPathConstants.STRING);
                        gross = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(gross);
                        movieObjekt.setGross(gross);

                        String ratingValue = (String) xpath.evaluate("//span[@itemprop='ratingValue']/text()", doc, XPathConstants.STRING);
                        ratingValue = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(ratingValue);
                        movieObjekt.setRatingValue(ratingValue);

                        String ratingCount = (String) xpath.evaluate("//span[@itemprop='ratingCount']/text()", doc, XPathConstants.STRING);
                        ratingCount = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(ratingCount);
                        movieObjekt.setRatingCount(ratingCount);

                        String duration = (String) xpath.evaluate("normalize-space(//time[@itemprop='duration']/text())", doc, XPathConstants.STRING);
                        duration = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(duration);
                        movieObjekt.setDuration(duration);

                        NodeList genreList = (NodeList) xpath.evaluate("//div[@itemprop='genre']//a/text()", doc, XPathConstants.NODESET);
                        List<String> genreListArray = new ArrayList<>();
                        for (int i = 0; i < genreList.getLength(); i++) {
                                String list = genreList.item(i).getNodeValue();
                                list = list.substring(1);
                                list = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(list);
                                genreListArray.add(list);
                        }
                        movieObjekt.setGenreList(genreListArray);

                        NodeList castList = (NodeList) xpath.evaluate("//td[@itemprop='actor']/a/span[@itemprop='name']/text()", doc, XPathConstants.NODESET);
                        List<String> castListArray = new ArrayList<>();
                        for (int i = 0; i < castList.getLength(); i++) {
                                String list = castList.item(i).getNodeValue();
                                list = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(list);
                                castListArray.add(list);
                        }
                        movieObjekt.setCastList(castListArray);

                        NodeList characterList = (NodeList) xpath.evaluate("//td[@class='character']/div/a/text()", doc, XPathConstants.NODESET);
                        List<String> characterListArray = new ArrayList<>();
                        for (int i = 0; i < characterList.getLength(); i++) {
                                String list = characterList.item(i).getNodeValue();
                                list = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(list);
                                characterListArray.add(list);
                        }
                        movieObjekt.setCharacterList(characterListArray);

                        NodeList directorList = (NodeList) xpath.evaluate("//span[@itemprop='director']/a/span[@itemprop='name']/text()", doc, XPathConstants.NODESET);
                        List<String> directorListArray = new ArrayList<>();
                        for (int i = 0; i < directorList.getLength(); i++) {
                                String list = directorList.item(i).getNodeValue();
                                list = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(list);
                                directorListArray.add(list);
                        }
                        movieObjekt.setDirectorList(directorListArray);

                        NodeList countryList = (NodeList) xpath.evaluate("//div[@id='titleDetails']/div//a[contains(@href, 'countries')]/text()", doc, XPathConstants.NODESET);
                        List<String> countryListArray = new ArrayList<>();
                        for (int i = 0; i < countryList.getLength(); i++) {
                                String list = countryList.item(i).getNodeValue();
                                list = org.apache.commons.lang3.StringEscapeUtils.unescapeHtml3(list);
                                countryListArray.add(list);
                        }
                        movieObjekt.setCountryList(countryListArray);

                } catch (Exception e) {
                        e.printStackTrace();
                }
                return movieObjekt;
        }

        /**
         * Saves metadata from object to json file
         *
         * @param metaData    Object of type Movie containing fetched data from imdb.com
         * @param outputDir   output directory for JSON files with metadata of movies
         * @param movieNumber Number of movie for filenaming
         * @throws IOException
         */
        @SuppressWarnings("unchecked")
        private static void storeToJson(int movieNumber, Movie metaData, String outputDir) throws IOException {
                String FileName = outputDir + "/" + movieNumber + ".json";
                JSONObject metaDataToJSONObjekt = new JSONObject();

                metaDataToJSONObjekt.put("url", metaData.getUrl());
                metaDataToJSONObjekt.put("year", metaData.getYear());
                metaDataToJSONObjekt.put("title", metaData.getTitle());
                metaDataToJSONObjekt.put("gross", metaData.getGross());
                metaDataToJSONObjekt.put("budget", metaData.getBudget());
                metaDataToJSONObjekt.put("castList", metaData.getCastList());
                metaDataToJSONObjekt.put("duration", metaData.getDuration());
                metaDataToJSONObjekt.put("genreList", metaData.getGenreList());
                metaDataToJSONObjekt.put("countryList", metaData.getCountryList());
                metaDataToJSONObjekt.put("description", metaData.getDescription());
                metaDataToJSONObjekt.put("ratingCount", metaData.getRatingCount());
                metaDataToJSONObjekt.put("ratingValue", metaData.getRatingValue());
                metaDataToJSONObjekt.put("directorList", metaData.getDirectorList());
                metaDataToJSONObjekt.put("characterList", metaData.getCharacterList());

                String jsonString = "[" + metaDataToJSONObjekt.toJSONString() + "]";
                String jsonBeauty = JsonWriter.formatJson(jsonString);

                try (PrintWriter toFile = new PrintWriter(FileName, "UTF-8")) {
                        System.out.println("Writing " + metaData.getTitle());
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
                        System.out.println("Call with: IMDBSpiderBJ.jar <moviesPath> <outputDir>");
                        System.exit(0);
                }

                IMDBSpiderBJ sp = new IMDBSpiderBJ();
                sp.fetchIMDBMovies(moviesPath, outputDir);
        }
}
