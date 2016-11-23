package threading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

public class MovieReader {

        public MovieReader() {
        }

        /**
         * Read movies from JSON files in directory 'moviesDir' formatted according to
         * 'example_movie_avatar.json'.
         *
         * Each movie should contain the attributes: url, title, year, genreList,
         * countryList, description, budget, gross, ratingValue, ratingCount,
         * duration, castList, characterList
         *
         * Each attribute is treated as a String and names ending in 'list' like
         * 'genreList' refer to JSON lists.
         *
         * @param moviesDir
         *          The directory containing the set of JSON files, each ending with a
         *          suffix ".json".
         * @return A list of movies
         * @throws IOException
         */
        public static List<Movie> readMoviesFrom(File moviesDir) throws IOException {
                List<Movie> movies = new ArrayList<>();
                for (File f : moviesDir.listFiles()) {
                        if (f.getName().endsWith(".json")) {
                                try (JsonReader reader = Json.createReader(new FileInputStream(f))) {
                                        JsonArray movie = reader.readArray();
                                        if (movie.size() > 0) {
                                                JsonObject m = (JsonObject) movie.get(0);
                                                Movie obj = new Movie();
                                                obj.setTitle(getString(m, "title"));
                                                obj.setYear(getString(m, "year"));
                                                obj.setUrl(getString(m, "url"));
                                                obj.setGenreList(getJsonArray(m, "genreList"));
                                                obj.setCountryList(getJsonArray(m, "countryList"));
                                                obj.setDescription(getString(m, "description"));
                                                obj.setBudget(getString(m, "budget"));
                                                obj.setGross(getString(m, "gross"));
                                                obj.setRatingValue(getString(m, "ratingValue"));
                                                obj.setRatingCount(getString(m, "ratingCount"));
                                                obj.setDuration(getString(m, "duration"));
                                                obj.setCastList(getJsonArray(m, ("castList")));
                                                obj.setCharacterList(getJsonArray(m, ("characterList")));
                                                obj.setDirectorList(getJsonArray(m, "directorList"));
                                                movies.add(obj);
                                        }
                                }
                        }
                }
                return movies;
        }

        /**
         * A helper function to parse a JSON array.
         *
         * @param m
         *          The JSON object, containing an array under the attribute 'key'.
         * @param key
         *          The key of the array
         * @return A list containing the Strings in the JSON object.
         */
        protected static List<String> getJsonArray(JsonObject m, String key) {
                try {
                        JsonArray array = m.getJsonArray(key);
                        List<String> result = new ArrayList<>();
                        for (JsonValue v : array) {
                                result.add(((JsonString) v).toString());
                        }
                        return result;
                } catch (Exception e) {
                        return new ArrayList<>();
                }
        }

        /**
         * A helper function to parse a JSON String.
         *
         * @param m
         *          The JSON object, containing a String under the attribute 'key'.
         * @param key
         *          The key of the array
         * @return The String in the JSON object.
         */
        protected static String getString(JsonObject m, String key) {
                try {
                        Object o = m.getString(key);
                        if (o != null) {
                                return (String) o;
                        }
                } finally {
                }
                return "";
        }
}
