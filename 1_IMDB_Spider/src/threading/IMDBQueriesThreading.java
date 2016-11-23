/* Queries with some threading - don't know if it's worth the hassle */

package threading;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;


@SuppressWarnings("static-method")
public class IMDBQueriesThreading {
        private ExecutorService executorService;

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
                List<Tuple<Movie, String>> results = new ArrayList<>();

                //get number of cores
                int availableCores = Runtime.getRuntime().availableProcessors();
                executorService = Executors.newFixedThreadPool(availableCores);

                int end = movies.size() / availableCores;
                int start = 0;
                boolean done = false;

                List<Future<List<Tuple<Movie, String>>>> futureList = new ArrayList<>();


                //only start multiple threads if there're a lot of movies
                if (movies.size() < 4000) {
                        end = movies.size() - 1;
                }
                // now let every core iterate through a given part of the array.
                while (!done) {
                        // if the end is set, we're done...
                        if (end == movies.size() - 1) {
                                done = true;
                        }

                        final int startF = start;
                        final int endF = end;
                        Future<List<Tuple<Movie, String>>> submit;

                        //++++++++++++ THREADING ++++++++++++++
                        submit = executorService.submit(new Callable<List<Tuple<Movie, String>>>() {

                                @Override
                                public List<Tuple<Movie, String>> call() throws Exception {
                                        List<Tuple<Movie, String>> resultsThread = new ArrayList<>();
                                        int startT = startF;
                                        System.out.println("start " + Thread.currentThread().getName());
                                        // iterate through movies from given start to given end
                                        while (startT <= endF) {
                                                for (String dList : movies.get(startT).getDirectorList()) {
                                                        // check if director is in the cast list
                                                        // if there are more than one and one of them matches the cast, break
                                                        if (movies.get(startT).getCastList().contains(dList)) {
                                                                Tuple<Movie, String> match = new Tuple<>(movies.get(startT), dList);
                                                                resultsThread.add(match);
                                                                break;
                                                        }
                                                }
                                                startT++;
                                        }
                                        System.out.println(Thread.currentThread().getName() + " is done");
                                        return resultsThread;
                                }
                        });
                        futureList.add(submit);
                        // ++++++++++++ THREADING ++++++++++++++

                        // the new start for the next thread is the old end + 1
                        start = end + 1;
                        // the new end ist given by the size of the list and the available cores
                        end = end + movies.size() / availableCores;
                        // but if the next part for a thread is smaller than "normal" the last
                        // thread will go a bit longer
                        if (movies.size() - end < movies.size() / availableCores) {
                                end = movies.size() - 1;
                        }
                }

                for (Future<List<Tuple<Movie, String>>> future : futureList) {
                        try {
                                List<Tuple<Movie, String>> fromFutureToList = new ArrayList<>(future.get());
                                results.addAll(fromFutureToList);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }

                executorService.shutdown();

                // sort for imdb rating
                Collections.sort(results, (o1, o2) -> o2.first.getRatingValue().compareTo(o1.first.getRatingValue()));

                if (results.size() > 10) {
                        return results.subList(0, 10);
                }
                return results;
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
        private List<Tuple<Movie, Long>> queryUnderTheRadar(List<Movie> movies) {
                List<Tuple<Movie, Long>> results = new ArrayList<>();

                // see AllRounder for comments concerning threading
                int availableCores = Runtime.getRuntime().availableProcessors();
                executorService = Executors.newFixedThreadPool(availableCores);

                int end = movies.size() / availableCores;
                int start = 0;
                boolean done = false;

                List<Future<List<Tuple<Movie, Long>>>> futureList = new ArrayList<>();
                if (movies.size() < 4000) {
                        end = movies.size() - 1;
                }
                while (!done) {
                        if (end == movies.size() - 1) {
                                done = true;
                        }

                        final int startF = start;
                        final int endF = end;
                        Future<List<Tuple<Movie, Long>>> submit;

                        submit = executorService.submit(() -> {
                                List<Tuple<Movie, Long>> resultsThread = new ArrayList<>();
                                int startT = startF;

                                // iterate through movies from given start to given end
                                while (startT <= endF) {
                                        for (String clist : movies.get(startT).getCountryList()) {
                                                if ((clist.matches("USA")) &&
                                                        (Integer.parseInt(movies.get(startT).getYear()) <= 2015) &&
                                                        (Double.parseDouble(movies.get(startT).getRatingValue()) > 8.0) &&
                                                        (Integer.parseInt(movies.get(startT).getRatingCount().replace(",", "")) >= 1000)) {
                                                        // compute loss
                                                        Long loss = Long.parseLong(movies.get(startT).getBudget().replaceAll("[^0-9]", ""))
                                                                - Long.parseLong(movies.get(startT).getGross().replaceAll("[^0-9]", ""));
                                                        Tuple<Movie, Long> match = new Tuple<>(movies.get(startT), loss);
                                                        resultsThread.add(match);
                                                }
                                        }
                                        startT++;
                                }
                                return resultsThread;
                        });
                        futureList.add(submit);

                        start = end + 1;
                        end = end + movies.size() / availableCores;

                        if (movies.size() - end < movies.size() / availableCores) {
                                end = movies.size() - 1;
                        }
                }

                for (Future<List<Tuple<Movie, Long>>> future : futureList) {
                        try {
                                List<Tuple<Movie, Long>> fromFutureToList = new ArrayList<>(future.get());
                                results.addAll(fromFutureToList);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                executorService.shutdown();

                // sort for the highest loss
                Collections.sort(results, (o1, o2) -> o1.second.compareTo(o2.second));

                if (results.size() > 10) {
                        return results.subList(0, 10);
                }
                return results;
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
        private List<Tuple<Movie, Integer>> queryPillarsOfStorytelling(List<Movie> movies) {
                List<Tuple<Movie, Integer>> results = new ArrayList<>();

                // see AllRounder for comments concerning threading
                int availableCores = Runtime.getRuntime().availableProcessors();
                executorService = Executors.newFixedThreadPool(availableCores);

                int end = movies.size() / availableCores;
                int start = 0;
                boolean done = false;

                List<Future<List<Tuple<Movie, Integer>>>> futureList = new ArrayList<>();

                while (!done) {
                        if (movies.size() < 4000) {
                                end = movies.size() - 1;
                        }
                        if (end == movies.size() - 1) {
                                done = true;
                        }

                        final int startF = start;
                        final int endF = end;
                        Future<List<Tuple<Movie, Integer>>> submit;

                        submit = executorService.submit(() -> {
                                List<Tuple<Movie, Integer>> resultsThread = new ArrayList<>();
                                int startT = startF;

                                // iterate through movies from given start to given end
                                while (startT <= endF) {
                                        if (movies.get(startT).getDescription().toLowerCase().contains("kill") &&
                                                movies.get(startT).getDescription().toLowerCase().contains("love")) {
                                                // use a temporary string to count kill and love: every time kill/love is found
                                                // replace it by - and count it
                                                String tmp = movies.get(startT).getDescription().toLowerCase();
                                                int cnt = 0;
                                                while (tmp.contains("kill")) {
                                                        tmp = tmp.replaceFirst("kill", "-");
                                                        cnt++;

                                                }
                                                tmp = movies.get(startT).getDescription().toLowerCase();
                                                while (tmp.contains("love")) {
                                                        tmp = tmp.replaceFirst("love", "-");
                                                        cnt++;

                                                }
                                                Tuple<Movie, Integer> match = new Tuple<>(movies.get(startT), cnt);
                                                resultsThread.add(match);
                                        }
                                        startT++;
                                }
                                return resultsThread;
                        });
                        futureList.add(submit);

                        start = end + 1;
                        end = end + movies.size() / availableCores;

                        if (movies.size() - end < movies.size() / availableCores) {
                                end = movies.size() - 1;
                        }
                }

                for (Future<List<Tuple<Movie, Integer>>> future : futureList) {
                        try {
                                List<Tuple<Movie, Integer>> fromFutureToList = new ArrayList<>(future.get());
                                results.addAll(fromFutureToList);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                executorService.shutdown();

                // sort for appearance
                Collections.sort(results, (o1, o2) -> o2.second.compareTo(o1.second));

                if (results.size() > 10) {
                        return results.subList(0, 10);
                }
                return results;
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

        private List<Movie> queryRedPlanet(List<Movie> movies) {
                List<Movie> results = new ArrayList<>();
                // compare to:
                String filter1 = "Sci-Fi";
                String filter2 = "Mars";

                // see AllRounder for comments concerning threading
                int availableCores = Runtime.getRuntime().availableProcessors();
                executorService = Executors.newFixedThreadPool(availableCores);

                int end = movies.size() / availableCores;
                int start = 0;
                boolean done = false;

                List<Future<List<Movie>>> futureList = new ArrayList<>();

                while (!done) {
                        if (movies.size() < 4000) {
                                end = movies.size() - 1;
                        }
                        if (end == movies.size() - 1) {
                                done = true;
                        }

                        final int startF = start;
                        final int endF = end;
                        Future<List<Movie>> submit;

                        submit = executorService.submit(() -> {
                                List<Movie> resultsThread = new ArrayList<>();
                                int startT = startF;

                                // iterate through movies from given start to given end
                                while (startT <= endF) {
                                        // iterate through list of GenreList objects
                                        for (String oneGenre : movies.get(startT).getGenreList()) {
                                                if (oneGenre.equals(filter1)) {
                                                        // does it contain Mars?
                                                        if (Pattern.compile(Pattern.quote(filter2)).matcher(movies.get(startT).getDescription()).find()) {
                                                                resultsThread.add(movies.get(startT));
                                                        }
                                                        break;
                                                }
                                        }
                                        startT++;
                                }
                                return resultsThread;
                        });
                        futureList.add(submit);

                        start = end + 1;
                        end = end + movies.size() / availableCores;

                        if (movies.size() - end < movies.size() / availableCores) {
                                end = movies.size() - 1;
                        }
                }

                for (Future<List<Movie>> future : futureList) {
                        try {
                                List<Movie> fromFutureToList = new ArrayList<>(future.get());
                                results.addAll(fromFutureToList);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                executorService.shutdown();


                // sort for year
                Collections.sort(results, (o1, o2) -> o1.getYear().compareTo(o2.getYear()));

                // get rid of the rest
                if (results.size() > 10) {
                        results.subList(0, 10);
                }
                return results;
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

        private List<Movie> queryColossalFailure(List<Movie> movies) {
                List<Movie> results = new ArrayList<>();

                // see AllRounder for comments concerning threading
                int availableCores = Runtime.getRuntime().availableProcessors();
                executorService = Executors.newFixedThreadPool(availableCores);

                int end = movies.size() / availableCores;
                int start = 0;
                boolean done = false;
                List<Future<List<Movie>>> futureList = new ArrayList<>();

                while (!done) {
                        if (movies.size() < 4000) {
                                end = movies.size() - 1;
                        }
                        if (end == movies.size() - 1) {
                                done = true;
                        }

                        final int startF = start;
                        final int endF = end;
                        Future<List<Movie>> submit;

                        submit = executorService.submit(() -> {
                                List<Movie> resultsThread = new ArrayList<>();
                                int startT = startF;

                                // iterate through movies from given start to given end
                                while (startT <= endF) {
                                        // search movies: budget beyond 1mil, rating < 5.0, us-based, duration beyond 2h
                                        for (String cList : movies.get(startT).getCountryList()) {
                                                if ((!movies.get(startT).getDuration().isEmpty()) && (cList.matches("USA"))) {
                                                        if ((Integer.parseInt(movies.get(startT).getBudget().replaceAll("[^0-9]", "")) > 1000000) &&
                                                                (Double.parseDouble(movies.get(startT).getRatingValue()) < 5.0)) {

                                                                // evaluate time - this is not as simple as it seems because there are some cases:
                                                                // first, check if the duration contains minutes
                                                                if (movies.get(startT).getDuration().contains("min")) {

                                                                        // no split the time in hours and minutes
                                                                        String[] time = movies.get(startT).getDuration().split("h");

                                                                        // check if hours are at least 2
                                                                        if (Integer.parseInt(time[0]) >= 2) {
                                                                                // if yes, we still don't know if imdb didn't wrote 2h 0min,
                                                                                // which would not fit the query, so we better check on the
                                                                                // minutes, too.
                                                                                if (Integer.parseInt(time[1].replace(" ", "").replace("min", "")) > 0) {
                                                                                        resultsThread.add(movies.get(startT));
                                                                                }

                                                                        }
                                                                        // if duration not contains minutes, check if hours are at least three
                                                                        // because duration should be beyond 2h, which means exactly 2h-movies are out
                                                                } else {
                                                                        String[] time = movies.get(startT).getDuration().split("h");
                                                                        if (Integer.parseInt(time[0]) > 2) {
                                                                                resultsThread.add(movies.get(startT));
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                        startT++;
                                }
                                return resultsThread;
                        });
                        futureList.add(submit);

                        start = end + 1;
                        end = end + movies.size() / availableCores;

                        if (movies.size() - end < movies.size() / availableCores) {
                                end = movies.size() - 1;
                        }
                }

                for (Future<List<Movie>> future : futureList) {
                        try {
                                List<Movie> fromFutureToList = new ArrayList<>(future.get());
                                results.addAll(fromFutureToList);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                executorService.shutdown();

                // do sorting
                Collections.sort(results, (o1, o2) -> o1.getRatingValue().compareTo(o2.getRatingValue()));

                // get rid of the rest
                if (results.size() > 10) {
                        results.subList(0, 10);
                }
                return results;
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
        private List<Tuple<String, Integer>> queryUncreativeWriters(List<Movie> movies) {
                ArrayList<Tuple<String, Integer>> results = new ArrayList<>();
                HashMap<String, Integer> hashCharacter = new HashMap<>();
                int i = 0;
                int j = 0;
                // iterate through the movies
                while (i < movies.size()) {
                        // iterate through character-list
                        while (j < movies.get(i).getCharacterList().size()) {
                                // check if the list contains given substrings
                                if (!movies.get(i).getCharacterList().get(j).toLowerCase().contains("himself") &&
                                        !movies.get(i).getCharacterList().get(j).toLowerCase().contains("doctor") &&
                                        !movies.get(i).getCharacterList().get(j).toLowerCase().contains("herself") &&
                                        movies.get(i).getCharacterList().get(j).length() > 0) {
                                        // check if character is already hashed and if so, add 1 to his count
                                        if (hashCharacter.containsKey(movies.get(i).getCharacterList().get(j))) {
                                                int nameCount = hashCharacter.get(movies.get(i).getCharacterList().get(j));
                                                hashCharacter.put(movies.get(i).getCharacterList().get(j), ++nameCount);
                                        } else {
                                                hashCharacter.put(movies.get(i).getCharacterList().get(j), 1);
                                        }
                                }
                                j++;
                        }
                        i++;
                        j = 0;
                }

                // lets sort the HashMap for the values (frequency of occurrence)
                ArrayList<Map.Entry<String, Integer>> copyAndSort = new ArrayList<>();
                copyAndSort.addAll(hashCharacter.entrySet());
                copyAndSort.sort((o1, o2) -> o1.getValue().compareTo(o2.getValue()) * -1);

                // we only need 10
                if (copyAndSort.size() > 10) {
                        copyAndSort.subList(10, copyAndSort.size()).clear();
                }

                // add everything to results-list
                i = 0;
                while (i < copyAndSort.size()) {
                        results.add(new Tuple<>(copyAndSort.get(i).getKey(), copyAndSort.get(i).getValue()));
                        i++;
                }
                return results;
        }

        /**
         * Workhorse: Provide a ranked list of the top ten most active actors (i.e.
         * starred in most movies) and the number of movies they played a role in.
         *
         * @param movies the list of movies which is to be queried
         * @return the top ten actors and the number of movies they had a role in,
         * sorted by the latter.
         */
        private List<Tuple<String, Integer>> queryWorkHorse(List<Movie> movies) {
                List<Tuple<String, Integer>> results = new ArrayList<>();
                HashMap<String, Integer> hashAllActors = new HashMap<>();
                int i = 0;
                int j = 0;

                // iterate through the movies
                while (i < movies.size()) {
                        // iterate through the cast
                        while (j < movies.get(i).getCastList().size()) {
                                // check if actor is already hashed and if so, add 1 to his count
                                if (hashAllActors.containsKey(movies.get(i).getCastList().get(j))) {
                                        int x = hashAllActors.get(movies.get(i).getCastList().get(j));
                                        hashAllActors.put(movies.get(i).getCastList().get(j), ++x);
                                } else {
                                        hashAllActors.put(movies.get(i).getCastList().get(j), 1);
                                }
                                j++;
                        }
                        i++;
                        j = 0;
                }

                // lets sort the HashMap for the values (occurrence of actor)
                ArrayList<Map.Entry<String, Integer>> copyAndSort = new ArrayList<>();
                copyAndSort.addAll(hashAllActors.entrySet());
                copyAndSort.sort((o1, o2) -> o1.getValue().compareTo(o2.getValue()) * -1);

                // we only need 10
                if (copyAndSort.size() > 10) {
                        copyAndSort.subList(10, copyAndSort.size()).clear();
                }

                // add everything to results-list
                i = 0;
                while (i < copyAndSort.size()) {
                        results.add(new Tuple<>(copyAndSort.get(i).getKey(), copyAndSort.get(i).getValue()));
                        i++;
                }
                return results;
        }

        /**
         * Must See: List the best-rated movie of each year starting from 1990 until
         * (including) 2010 with more than 10,000 ratings. Order the movies by
         * ascending year.
         *
         * @param movies the list of movies which is to be queried
         * @return best movies by year, starting from 1990 until 2010.
         */
        private List<Movie> queryMustSee(List<Movie> movies) {
                ArrayList<Movie> results = new ArrayList<>();
                Movie[] resultsArray = new Movie[21];
                int i = 0;

                // iterate through movies
                while (i < movies.size()) {
                        // check if the movie fits the query
                        if ((Integer.parseInt(movies.get(i).getRatingCount().replaceAll(",", "")) > 10000) &&
                                (Integer.parseInt(movies.get(i).getYear()) <= 2010) &&
                                (Integer.parseInt(movies.get(i).getYear()) >= 1990)) {
                                // calculate position in the array and put it in
                                int p = Integer.parseInt(movies.get(i).getYear()) - 1990;

                                if (resultsArray[p] == null) {
                                        resultsArray[p] = movies.get(i);
                                } else {
                                        if (Double.parseDouble(resultsArray[p].getRatingValue())
                                                < Double.parseDouble(movies.get(i).getRatingValue())) {
                                                resultsArray[p] = movies.get(i);
                                        }
                                }
                        }
                        i++;
                }

                // put it all together
                i = 0;
                while (i < resultsArray.length) {
                        results.add(resultsArray[i]);
                        i++;
                }
                return results;
        }

        /**
         * Rotten Tomatoes: List the worst-rated movie of each year starting from 1990
         * till (including) 2010 with an IMDB score larger than 0. Order the movies by
         * increasing year.
         *
         * @param movies the list of movies which is to be queried
         * @return worst movies by year, starting from 1990 till (including) 2010.
         */
        private List<Movie> queryRottenTomatoes(List<Movie> movies) {
                ArrayList<Movie> results = new ArrayList<>();
                Movie[] resultsArray = new Movie[21];
                int i = 0;

                // iterate through movies
                while (i < movies.size()) {
                        // check if the movie fits the query
                        if ((Double.parseDouble(movies.get(i).getRatingValue()) > 0.0) &&
                                (Integer.parseInt(movies.get(i).getYear()) <= 2010) &&
                                (Integer.parseInt(movies.get(i).getYear()) >= 1990)) {
                                // calculate position in the array and put it in
                                int p = Integer.parseInt(movies.get(i).getYear()) - 1990;

                                if (resultsArray[p] == null) {
                                        resultsArray[p] = movies.get(i);
                                } else {
                                        if (Double.parseDouble(resultsArray[p].getRatingValue())
                                                > Double.parseDouble(movies.get(i).getRatingValue())) {
                                                resultsArray[p] = movies.get(i);
                                        }
                                }
                        }
                        i++;
                }

                // put it all together
                i = 0;
                while (i < resultsArray.length) {
                        results.add(resultsArray[i]);
                        i++;
                }
                return results;
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
        private List<Tuple<Tuple<String, String>, Integer>> queryMagicCouple(List<Movie> movies) {
                List<Tuple<Tuple<String, String>, Integer>> results = new ArrayList<>();
                HashMap<Tuple<String, String>, Integer> hashi = new HashMap<>();
                int i = 0;

                // iterate through the movies
                while (i < movies.size()) {
                        // first, make a list of the cast
                        List<String> actors = new ArrayList<>(movies.get(i).getCastList());
                        // sort it so that their are no different yet equal pairs later (e.g. Sly Arni != Arni Sly)
                        Collections.sort(actors);
                        // build pairs and hash them
                        for (int a = 0; a < actors.size(); a++) {
                                for (int b = a + 1; b < actors.size(); b++) {
                                        // check if the pair (aka key) is already in the HashMap
                                        // if it is, add 1 to the value, if not...well
                                        Tuple<String, String> actorListHelper = new Tuple<>(actors.get(a), actors.get(b));
                                        if (hashi.containsKey(actorListHelper)) {
                                                int x = hashi.get(actorListHelper);
                                                hashi.put(actorListHelper, ++x);
                                        } else {
                                                hashi.put(actorListHelper, 1);
                                        }
                                }
                        }
                        i++;
                }

                // lets sort the HashMap for the values
                ArrayList<Map.Entry<Tuple<String, String>, Integer>> copy = new ArrayList<>();
                copy.addAll(hashi.entrySet());

                copy.sort((o1, o2) -> o1.getValue().compareTo(o2.getValue()) * -1);

                // we need only the 10 highest matching pairs
                if (copy.size() > 10) {
                        copy.subList(10, copy.size()).clear();
                }

                // now put 'em in da double-tuple with the matching value
                for (Map.Entry<Tuple<String, String>, Integer> e : copy) {
                        results.add(new Tuple<>(e.getKey(), e.getValue()));
                }
                return results;
        }

        /**
         * Independent Perls: Determine those movies with a Budget of max. $1Million and
         * an IMDB Rating with at least 8.0. Sort by IMDB rating.
         *
         * @param movies the list of movies which is to be queried
         * @return report the top 10 movies with a budget below $1Mil and an IMDB
         * rating of at least 8.0, sortet by rating.
         */
        private List<Movie> queryIndependentPerls(List<Movie> movies) {
                List<Movie> results = new ArrayList<>();

                // see AllRounder for comments concerning threading
                int availableCores = Runtime.getRuntime().availableProcessors();
                executorService = Executors.newFixedThreadPool(availableCores);
                int end = movies.size() / availableCores;
                int start = 0;
                boolean done = false;
                List<Future<List<Movie>>> futureList = new ArrayList<>();

                while (!done) {
                        if (movies.size() < 4000) {
                                end = movies.size() - 1;
                        }
                        if (end == movies.size() - 1) {
                                done = true;
                        }

                        final int startF = start;
                        final int endF = end;
                        Future<List<Movie>> submit;

                        submit = executorService.submit(new Callable<List<Movie>>() {
                                @Override
                                public List<Movie> call() throws Exception {
                                        List<Movie> resultsThread = new ArrayList<>();

                                        int startT = startF;
                                        // iterate through movies from given start to given end
                                        while (startT <= endF) {
                                                if (Long.parseLong(movies.get(startT).getBudget().replaceAll("[^0-9]", "")) <= 1000000 &&
                                                        Long.parseLong(movies.get(startT).getBudget().replaceAll("[^0-9]", "")) > 0 &&
                                                        Double.parseDouble(movies.get(startT).getRatingValue()) >= 8.0) {
                                                        resultsThread.add(movies.get(startT));
                                                }
                                                startT++;
                                        }
                                        return resultsThread;
                                }
                        });
                        futureList.add(submit);

                        start = end + 1;
                        end = end + movies.size() / availableCores;

                        if (movies.size() - end < movies.size() / availableCores) {
                                end = movies.size() - 1;
                        }
                }

                for (Future<List<Movie>> future : futureList) {
                        try {
                                List<Movie> fromFutureToList = new ArrayList<>(future.get());
                                results.addAll(fromFutureToList);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                executorService.shutdown();

                // sort for ratingValue
                Collections.sort(results, (o1, o2) -> o2.getRatingValue().compareTo(o1.getRatingValue()));

                // get rid of the rest
                if (results.size() > 10) {
                        return results.subList(0, 10);
                }
                return results;
        }

        public static void main(String argv[]) throws IOException {
                String moviesPath = "data/movies";

                if (argv.length == 1) {
                        moviesPath = argv[0];
                } else if (argv.length != 0) {
                        System.out.println("Call with: ue_inforet_imdb_spider_study.BJ.IMDBQueries.jar <moviesPath>");
                        System.exit(0);
                }

                List<Movie> movies = MovieReader.readMoviesFrom(new File(moviesPath));

                System.out.println("All-rounder");
                {
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
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

                System.out.println("Independent Perls");
                {
                        IMDBQueriesThreading queries = new IMDBQueriesThreading();
                        long time = System.currentTimeMillis();
                        List<Movie> result = queries.queryIndependentPerls(movies);
                        System.out.println("Time:" + (System.currentTimeMillis() - time));

                        if (result != null && !result.isEmpty()) {
                                for (Movie movie : result) {
                                        System.out.println(movie.getRatingValue() + "\t" + movie.getTitle() + "\t Budget: " + movie.getBudget().replaceAll("[^0-9]", ""));
                                }
                        } else {
                                System.out.println("Error? Or not implemented?");
                        }
                        System.out.println("");
                }
        }
}