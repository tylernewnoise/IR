/* Version2 - (Ben & Jerry)*/
package BJ;
import threading.Movie;
import threading.MovieReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

@SuppressWarnings("static-method")
public class IMDBQueriesBJ {

        /**
         * A helper class for pairs of objects of generic types 'K' and 'V'.
         *
         * @param <K> first value
         * @param <V> second value
         */
        class Tuple<K, V> {
                K first;
                V second;

                public Tuple(K f, V s) {
                        this.first = f;
                        this.second = s;
                }

                @Override
                public int hashCode() {
                        return this.first.hashCode() + this.second.hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                        return this.first.equals(((Tuple<?, ?>) obj).first)
                                && this.second.equals(((Tuple<?, ?>) obj).second);
                }
        }

        /**
         * All-rounder: Determine all movies in which the director stars as an actor
         * (cast). Return the top ten matches sorted by decreasing IMDB rating.
         *
         * @param movies the list of movies which is to be queried
         * @return top ten movies and the director, sorted by decreasing IMDB rating
         */
        public List<Tuple<Movie, String>> queryAllRounder(List<Movie> movies) {

                List<Tuple<Tuple<Movie, String>, Double>> ratingList = new ArrayList<>();

                for (int movieIterator = 0; movieIterator < movies.size(); movieIterator++) {
                        for (int directorIterator = 0; directorIterator < movies.get(movieIterator).getDirectorList().size(); directorIterator++) {
                                String director = movies.get(movieIterator).getDirectorList().get(directorIterator);

                                if (movies.get(movieIterator).getCastList().contains(director)) {

                                        Movie currentMovie = movies.get(movieIterator);
                                        double movieRating = Double.parseDouble(movies.get(movieIterator).getRatingValue());
                                        Tuple<Movie, String> movieStringTuple = new Tuple<>(currentMovie, director);
                                        Tuple<Tuple<Movie, String>, Double> movieDoubleTuple = new Tuple<>(movieStringTuple, movieRating);

                                        if (ratingList.isEmpty()) {
                                                ratingList.add(movieDoubleTuple);
                                        } else {
                                                int k = 0;
                                                while (k < ratingList.size() && ratingList.get(k).second > movieDoubleTuple.second) {
                                                        k++;
                                                }
                                                ratingList.add(k, movieDoubleTuple);
                                        }
                                        break;
                                }
                        }
                }

                List<Tuple<Movie, String>> resultsList = new ArrayList<>();
                for (int i = 0; i < 10 && i < ratingList.size(); i++) {
                        resultsList.add(ratingList.get(i).first);
                }

                return resultsList;
        }

        /**
         * Under the Radar: Determine the top ten US-American movies until (including)
         * 2015 that have made the biggest loss despite an IMDB score above
         * (excluding) 8.0, based on at least 1,000 votes. Here, loss is defined as
         * budget minus gross.
         *
         * @param movies the list of movies which is to be queried
         * @return top ten highest rated US-American movie until 2015, sorted by
         * monetary loss, which is also returned
         */
        public List<Tuple<Movie, Long>> queryUnderTheRadar(List<Movie> movies) {
                List<Tuple<Movie, Long>> resultsList = new ArrayList<>();
                String countryFilter = "USA";
                int i = 0;

                while (i < movies.size()) {
                        for (String countryList : movies.get(i).getCountryList()) {
                                int year = Integer.parseInt(movies.get(i).getYear());
                                double ratingValue = Double.parseDouble(movies.get(i).getRatingValue());
                                int ratingCount = Integer.parseInt(movies.get(i).getRatingCount().replace(",", ""));

                                if ((countryList.matches(countryFilter)) && (year <= 2015) && (ratingValue > 8.0) && (ratingCount >= 1000)) {
                                        long budget =  Long.parseLong(movies.get(i).getBudget().replaceAll("[^0-9]", ""));
                                        long gross = Long.parseLong(movies.get(i).getGross().replaceAll("[^0-9]", ""));
                                        Long loss = budget - gross;
                                        Tuple<Movie, Long> newEntry = new Tuple<>(movies.get(i), loss);
                                        resultsList.add(newEntry);
                                }
                        }
                        i++;
                }

                Collections.sort(resultsList, new Comparator<Tuple<Movie, Long>>() {
                        @Override
                        public int compare(Tuple<Movie, Long> t1, Tuple<Movie, Long> t2) {
                                return t1.second.compareTo(t2.second);
                        }
                });

                if (resultsList.size() > 10) {
                        while (resultsList.size() > 10) {
                                resultsList.remove(resultsList.size()-1);
                        }
                }
                return resultsList;
        }

        /**
         * The Pillars of Storytelling: Determine all movies that contain both
         * (sub-)strings "kill" and "love" in their lowercase description
         * (String.toLowerCase()). Sort the results by the number of appearances of
         * these strings and return the top ten matches.
         *
         * @param movies the list of movies which is to be queried
         * @return top ten movies, which have the words "kill" and "love" as part of
         * their lowercase description, sorted by the number of appearances of
         * these words, which is also returned.
         */
        public List<Tuple<Movie, Integer>> queryPillarsOfStorytelling(List<Movie> movies) {
                List<Tuple<Movie, Integer>> resultsList = new ArrayList<>();
                int i = 0;

                while (i < movies.size()) {
                        if (movies.get(i).getDescription().contains("kill") && movies.get(i).getDescription().contains("love")) {
                                String description = movies.get(i).getDescription();
                                int z = 0;
                                while (description.contains("kill")) {
                                        description = description.replaceFirst("kill", "-");
                                        z++;

                                }
                                description = movies.get(i).getDescription();
                                while (description.contains("love")) {
                                        description = description.replaceFirst("love", "-");
                                        z++;

                                }
                                Tuple<Movie, Integer> newEntry = new Tuple<>(movies.get(i), z);
                                resultsList.add(newEntry);
                        }
                        i++;
                }

                Collections.sort(resultsList, new Comparator<Tuple<Movie, Integer>>() {
                        @Override
                        public int compare(Tuple<Movie, Integer> t1, Tuple<Movie, Integer> t2) {
                                return t1.second.compareTo(t2.second);
                        }
                });

                if (resultsList.size() > 10) {
                        while (resultsList.size() > 10) {
                                resultsList.remove(0);
                        }
                }
                return resultsList;
        }

        /**
         * The Red Planet: Determine all movies of the Sci-Fi genre that mention
         * "Mars" in their description (case-aware!). List all found movies in
         * ascending order of publication (year).
         *
         * @param movies the list of movies which is to be queried
         * @return list of Sci-Fi movies involving Mars in ascending order of
         * publication.
         */
        public List<Movie> queryRedPlanet(List<Movie> movies) {
                List<Movie> resultsList = new ArrayList<>();

                String sciFi = "Sci-Fi";
                String mars = "Mars";

                for (Movie movie : movies) {
                        for (String genre : movie.getGenreList()) {
                                if (genre.equals(sciFi)) {
                                        boolean newEntry = Pattern.compile(Pattern.quote(mars)).matcher(movie.getDescription()).find();
                                        if (newEntry) {
                                                resultsList.add(movie);
                                        }
                                        break;
                                }
                        }
                }

                Collections.sort(resultsList, new Comparator<Movie>() {
                        @Override
                        public int compare(Movie t1, Movie t2) {
                                return t1.getYear().compareTo(t2.getYear());
                        }
                });
                return resultsList;
        }


        public boolean cmp1(String s, double d, int i) {
                if (i >= 0) {
                        try {
                                double tmp = Double.parseDouble(s);
                                if (tmp > d) {
                                        return true;
                                }
                        } catch (NumberFormatException e) {
                                //e.printStackTrace();
                        }
                } else {
                        try {
                                double tmp = Double.parseDouble(s);
                                if (tmp < d) {
                                        return true;
                                }
                        } catch (NumberFormatException e) {
                                //e.printStackTrace();
                        }
                }
                return false;
        }

        /**
         * Colossal Failure: Determine all US-American movies with a duration beyond 2
         * hours, a budget beyond 1 million and an IMDB rating below 5.0. Sort results
         * by ascending IMDB rating.
         *
         * @param movies the list of movies which is to be queried
         * @return list of US-American movies with high duration, large budgets and a
         * bad IMDB rating, sorted by ascending IMDB rating
         */
        public List<Movie> queryColossalFailure(List<Movie> movies) {
                List<Movie> resultList = new ArrayList<Movie>();
                int million = 1000000;
                for (Movie pickedMovie : movies) {
                        for (String pickedGenre : pickedMovie.getCountryList()) {
                                if (pickedGenre.equals("USA")) {
                                        if (Integer.parseInt(pickedMovie.getBudget().replaceAll("[^0-9]", "")) > million
                                                && cmp1(pickedMovie.getDuration().replace("h", ".").replace(" ", "").replace("min", ""), 2.0, 1)
                                                && cmp1(pickedMovie.getRatingValue(), 5.0, -1)) {
                                                resultList.add(pickedMovie);
                                        }
                                }
                        }
                }
                Collections.sort(resultList, new Comparator<Movie>() {
                        @Override
                        public int compare(Movie o1, Movie o2) {
                                return o1.getRatingValue().compareTo(o2.getRatingValue());
                        }
                });
                return resultList;
        }

        /**
         * Uncreative Writers: Determine the 10 most frequent character names of all
         * times ordered by frequency of occurrence. Filter any lowercase names
         * containing substrings "himself", "doctor", and "herself" from the result.
         *
         * @param movies the list of movies which is to be queried
         * @return the top 10 character names and their frequency of occurrence;
         * sorted in decreasing order of frequency
         */
        public List<Tuple<String, Integer>> queryUncreativeWriters(List<Movie> movies) {

                HashMap<String, Integer> hashCharacter = new HashMap<>();

                for (Movie movie : movies){
                        for (String character : movie.getCharacterList()){
                                if (!character.toLowerCase().contains("himself")
                                        && !character.toLowerCase().contains("doctor")
                                        && !character.toLowerCase().contains("herself")
                                        && !character.isEmpty()){
                                        if (hashCharacter.containsKey(character)){
                                                int nameCount = hashCharacter.get(character);
                                                hashCharacter.put(character, ++nameCount);
                                        } else {
                                                hashCharacter.put(character, 1);
                                        }
                                }
                        }
                }

                ArrayList<Entry<String, Integer>> hashArrayList = new ArrayList<>();
                hashArrayList.addAll(hashCharacter.entrySet());
                hashArrayList.sort(new Comparator<Entry<String, Integer>>() {
                        @Override
                        public int compare(Entry<String, Integer> t1, Entry<String, Integer> t2) {
                                return t1.getValue().compareTo(t2.getValue()) * -1;
                        }
                });

                ArrayList<Tuple<String, Integer>> resultsList = new ArrayList<>();
                if (hashArrayList.size() > 10) {
                        int length = 10;
                } else {
                        int length = hashArrayList.size();
                }
                for (int i = 0; i < 10; i++) {
                        resultsList.add(new Tuple<String, Integer>(hashArrayList.get(i).getKey(), hashArrayList.get(i).getValue()));
                }
                return resultsList;
        }

        /**
         * Workhorse: Provide a ranked list of the top ten most active actors (i.e.
         * starred in most movies) and the number of movies they played a role in.
         *
         * @param movies the list of movies which is to be queried
         * @return the top ten actors and the number of movies they had a role in,
         * sorted by the latter.
         */
        public List<Tuple<String, Integer>> queryWorkHorse(List<Movie> movies) {
                // TODO Impossibly Hard Query: insert code here
                return new ArrayList<>();
        }

        /**
         * Must See: List the best-rated movie of each year starting from 1990 until
         * (including) 2010 with more than 10,000 ratings. Order the movies by
         * ascending year.
         *
         * @param movies the list of movies which is to be queried
         * @return best movies by year, starting from 1990 until 2010.
         */
        public List<Movie> queryMustSee(List<Movie> movies) {
                //array of Lists of movies, every entry is a list for one year
                Movie[] yearsArray = new Movie[21];

                final int YEAR = 1990;

                for (Movie movie : movies){
                        int ratingCount = Integer.parseInt(movie.getRatingCount().replaceAll(",",""));
                        double ratingValue = Double.parseDouble(movie.getRatingValue());
                        int year = Integer.parseInt(movie.getYear());
                        if (ratingCount > 10000 && year >= 1990 && year <= 2010) {
                                int arrayPosition = year - YEAR; //for example: a movie from 1990, is 1990 - 1990 = 0, comes to arrayposition 0 --> yearsArray[0]

                                //sorting
                                if (yearsArray[arrayPosition] == null) {
                                        yearsArray[arrayPosition] = new Movie();
                                        yearsArray[arrayPosition] = movie;
                                } else {
                                        //films is in the list, replace if worse rating
                                        if (Double.parseDouble(yearsArray[arrayPosition].getRatingValue()) < ratingValue) {
                                                yearsArray[arrayPosition] = movie;
                                        }
                                }
                        }
                }

                //concatenate all created lists to one
                ArrayList<Movie> resultsList = new ArrayList<Movie>();
                for (int i = 0; i < yearsArray.length; i++) {
                        resultsList.add(yearsArray[i]);
                }
                return resultsList;
        }

        /**
         * Rotten Tomatoes: List the worst-rated movie of each year starting from 1990
         * till (including) 2010 with an IMDB score larger than 0. Order the movies by
         * increasing year.
         *
         * @param movies the list of movies which is to be queried
         * @return worst movies by year, starting from 1990 till (including) 2010.
         */
        public List<Movie> queryRottenTomatoes(List<Movie> movies) {
                List<Movie> resultsList = new ArrayList<>();

                Movie tmp = new Movie();
                tmp.setRatingValue("10");
                tmp.setTitle("Test");
                for (int i = 0; i < 21; i++) {
                        resultsList.add(i, tmp);
                }

                for (Movie movie : movies) {
                        String movieYear = movie.getYear();
                        if (movieYear.isEmpty()) continue;
                        int jahr = Integer.parseInt(movieYear);
                        if (jahr > 2010 || jahr < 1990) continue;
                        if (Double.parseDouble(resultsList.get(jahr - 1990).getRatingValue()) > Double.parseDouble(movie.getRatingValue())) {
                                resultsList.set(jahr - 1990, movie);
                        }
                }
                int j = 0;
                for (int i = 0; i < 21; i++) {
                        if (resultsList.get(i - j).getTitle().equals("Test")) {
                                resultsList.remove(i - j);
                                j++;
                        }
                }
                return resultsList;
        }

        /**
         * Magic Couples: Determine those couples that feature together in the most
         * movies. E.g., Adam Sandler and Allen Covert feature together in multiple
         * movies. Report the top ten pairs of actors, their number of movies and sort
         * the result by the number of movies.
         *
         * @param movies the list of movies which is to be queried
         * @return report the top 10 pairs of actors and the number of movies they
         * feature together. Sort by number of movies.
         */
        public List<Tuple<Tuple<String, String>, Integer>> queryMagicCouple(List<Movie> movies) {
                HashMap<String, Integer> hashActor = new HashMap<>();
                int i = 0;

                while (i < movies.size()) {
                        List<String> actorList = new ArrayList<>(movies.get(i).getCastList());
                        Collections.sort(actorList);
                        for (int k = 0; k < actorList.size(); k++) {
                                for (int l = k + 1; l < actorList.size(); l++) {
                                        if (hashActor.containsKey(actorList.get(k) + "##" + actorList.get(l))) {
                                                int x = hashActor.get(actorList.get(k) + "##" + actorList.get(l));
                                                hashActor.put(actorList.get(k) + "##" + actorList.get(l), ++x);

                                        } else {
                                                hashActor.put(actorList.get(k) + "##" + actorList.get(l), 1);
                                        }
                                }
                        }
                        i++;
                }

                ArrayList<Entry<String, Integer>> hashArrayList = new ArrayList<>();
                hashArrayList.addAll(hashActor.entrySet());
                hashArrayList.sort(new Comparator<Entry<String, Integer>>() {
                        @Override
                        public int compare(Entry<String, Integer> t1, Entry<String, Integer> t2) {
                                return t1.getValue().compareTo(t2.getValue()) * -1;
                        }
                });

                List<Tuple<Tuple<String, String>, Integer>> resultsList = new ArrayList<>();
                if (hashArrayList.size() > 10) {
                        int length = 10;
                } else {
                        int length = hashArrayList.size();
                }
                for (int y = 0; y < 10; y++) {
                        String[] parts = hashArrayList.get(y).getKey().split("##");
                        resultsList.add(new Tuple<>(new Tuple<String, String>(parts[0], parts[1]), hashArrayList.get(y).getValue()));
                }
                return resultsList;
        }

        public static void main(String argv[]) throws IOException {
                String moviesPath = "movies2";

                if (argv.length == 1) {
                        moviesPath = argv[0];
                } else if (argv.length != 0) {
                        System.out.println("Call with: IMDBQueriesBJ.jar <moviesPath>");
                        System.exit(0);
                }

                List<Movie> movies = MovieReader.readMoviesFrom(new File(moviesPath));

                System.out.println("All-rounder");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Tuple<Movie, String>> result = queries.queryAllRounder(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && result.size() == 10) {
                                for (Tuple<Movie, String> tuple : result) {
                                        System.out.println("\t" + tuple.first.getRatingValue() + "\t"
                                                + tuple.first.getTitle() + "\t" + tuple.second);
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("Under the radar");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Tuple<Movie, Long>> result = queries.queryUnderTheRadar(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && result.size() <= 10) {
                                for (Tuple<Movie, Long> tuple : result) {
                                        System.out.println("\t" + tuple.first.getTitle() + "\t"
                                                + tuple.first.getRatingCount() + "\t"
                                                + tuple.first.getRatingValue() + "\t" + tuple.second);
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("The pillars of storytelling");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Tuple<Movie, Integer>> result = queries
                                .queryPillarsOfStorytelling(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && result.size() <= 10) {
                                for (Tuple<Movie, Integer> tuple : result) {
                                        System.out.println("\t" + tuple.first.getTitle() + "\t"
                                                + tuple.second);
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("The red planet");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Movie> result = queries.queryRedPlanet(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty()) {
                                for (Movie movie : result) {
                                        System.out.println("\t" + movie.getTitle());
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("ColossalFailure");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Movie> result = queries.queryColossalFailure(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty()) {
                                for (Movie movie : result) {
                                        System.out.println("\t" + movie.getTitle() + "\t"
                                                + movie.getRatingValue());
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("Uncreative writers");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Tuple<String, Integer>> result = queries
                                .queryUncreativeWriters(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && result.size() <= 10) {
                                for (Tuple<String, Integer> tuple : result) {
                                        System.out.println("\t" + tuple.first + "\t" + tuple.second);
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("Workhorse");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Tuple<String, Integer>> result = queries.queryWorkHorse(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && result.size() <= 10) {
                                for (Tuple<String, Integer> actor : result) {
                                        System.out.println("\t" + actor.first + "\t" + actor.second);
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("Must see");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Movie> result = queries.queryMustSee(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && !result.isEmpty()) {
                                for (Movie m : result) {
                                        System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                                                + "\t" + m.getTitle());
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("Rotten tomatoes");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Movie> result = queries.queryRottenTomatoes(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty() && !result.isEmpty()) {
                                for (Movie m : result) {
                                        System.out.println("\t" + m.getYear() + "\t" + m.getRatingValue()
                                                + "\t" + m.getTitle());
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                }
                System.out.println("");

                System.out.println("Magic Couples");
                {
                        IMDBQueriesBJ queries = new IMDBQueriesBJ();
                        long time = System.currentTimeMillis();
                        List<Tuple<Tuple<String, String>, Integer>> result = queries
                                .queryMagicCouple(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty()) {
                                for (Tuple<Tuple<String, String>, Integer> tuple : result) {
                                        System.out.println("\t" + tuple.first.first + ":"
                                                + tuple.first.second + "\t" + tuple.second);
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                        System.out.println("");

                }


        }
}

