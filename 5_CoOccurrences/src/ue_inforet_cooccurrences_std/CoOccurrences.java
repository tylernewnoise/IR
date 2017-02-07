package ue_inforet_cooccurrences_std;
/* variant without trove libs */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CoOccurrences {

	private static HashSet<String> stopWords = new HashSet<>();
	private static HashMap<String, Integer> allWords = new HashMap<>();
	private static HashMap<Bigram, Integer> allBigrams = new HashMap<>();

	// geklaut aus IMDBQueries.java, damit ich nicht "unnütz" mit Bigram Objekten rumjongliere :)
	class Tuple<K, V> {
		K first;
		V second;

		public Tuple(K f, V s) {
			this.first = f;
			this.second = s;
		}


		private void calcCollocations() {
		// result arraylist  // first idea: BiGram, but Tuple seems easier
		ArrayList<Tuple<String, Double>> results = new ArrayList<>(); // BiGram Object: getFirst getSecond getScore setScore

		// iterate through allBigrams HashMap
		for (Map.Entry<Bigram, Integer> oneBigram : allBigrams.entrySet()) {  // returned ein Objekt mit <Bigram, Integer> (HashMap!) keySet()
			// extract both words and get their count from allWords count list
			int appearanceFirstWord = allWords.get(oneBigram.getKey().getFirst());
			int appearanceSecondWord = allWords.get(oneBigram.getKey().getSecond());

			// wordappearance >= 1000
			if (appearanceFirstWord >= 1000 && appearanceSecondWord >= 1000) {
				// calc score
				results.add(new Tuple<>(oneBigram.getKey().getFirst().toString() + " " + oneBigram.getKey().getSecond().toString(),
						(2 * oneBigram.getValue().intValue()) / (double)(appearanceFirstWord + appearanceSecondWord)));  // double cast - thanks falko!
			}

			// sort results
			results.sort((o1, o2) -> o2.second.compareTo(o1.second));  // vgl. doubles gegeneinander!

			// output first 1000 results
			for (int i = 0; i < 1000; i++) {
				System.out.println(results.get(i).first + " " + results.get(i).second);  // Ausgabe Bigram + double result
			}
		}
	}

	// parse and safe the stop-words
	private static void parseStopWords() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(CoOccurrences.class.
			getResourceAsStream("stop.words"), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stopWords.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void parsePlotList(String plotFile) {
		boolean isPlotLine = false;
		ArrayList<String> plotTokens = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("MV:")) {
					// was the line before a PL:-line? if yes make some bigrams of the plot
					if (isPlotLine) {
						createBiGram(plotTokens);
						// create a new list for the next plot description
						plotTokens = new ArrayList<>();
						isPlotLine = false;
					}
					parseTitle(line.substring(4, line.length()).toLowerCase());
				} else if (line.startsWith("PL:")) {
					isPlotLine = true;

					StringTokenizer st = new StringTokenizer(line.substring(4, line.length()).
						toLowerCase(), " .,:!?", false);

					// tokenize the plot
					while (st.hasMoreTokens()) {
						plotTokens.add(st.nextToken());
					}
				}
			}
			// add the last plot of the file
			createBiGram(plotTokens);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void createBiGram(ArrayList<String> tokenList) {
		// count the words in the plot without stop-words
		for (String token : tokenList) {
			if (!stopWords.contains(token)) {
				if (allWords.containsKey(token)) {
					allWords.put(token, allWords.get(token) + 1);
				} else {
					allWords.put(token, 1);
				}
			}
		}
		// create some bigrams
		for (int i = 0; i + 1 < tokenList.size(); ++i) {
			// check for stop words in the bigram
			if (!stopWords.contains(tokenList.get(i)) && !stopWords.contains(tokenList.get(i + 1))) {
				Bigram tmp = new Bigram(tokenList.get(i), tokenList.get(i + 1));
				if (allBigrams.containsKey(tmp)) {
					allBigrams.put(tmp, allBigrams.get(tmp) + 1);
				} else {
					allBigrams.put(tmp, 1);
				}
			}
		}
	}

	// handle the different title formats
	private static void parseTitle(String title) {
		// remove {{SUSPENDED}}
		if (title.contains("{{suspended}}")) {
			title = title.replace(" {{suspended}}", "");
		}

		StringTokenizer st;

		// +++ series +++
		if (title.startsWith("\"") && !title.endsWith("}")) {
			int end = 3;
			// remove the year
			for (int i = title.length() - 1; title.charAt(i) != '('; --i) {
				end++;
			}

			st = new StringTokenizer(title.substring(1, title.length() - end), " .,:!?", false);
		}
		// +++ episode +++
		else if (title.startsWith("\"") && title.endsWith("}")) {
			int end = 3;

			// remove episodeTitle
			title = title.substring(0, title.indexOf('{') - 1).toLowerCase();

			// remove the year
			for (int i = title.length() - 1; title.charAt(i) != '('; --i) {
				end++;
			}

			st = new StringTokenizer(title.substring(1, title.length() - end), " .,:!?", false);
		}
		// +++ movie +++
		else {
			int end = 3;
			// remove the year
			for (int i = title.length() - 1; title.charAt(i) != '('; --i) {
				end++;
			}

			st = new StringTokenizer(title.substring(0, title.length() - end + 1), " .,:!?", false);
		}

		// put all the title words in a list.
		ArrayList<String> titleTokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			titleTokens.add(st.nextToken());
		}

		// if there is only one word in the title add the word to the overall wordcount
		if (titleTokens.size() == 1) {
			// count it only if it is not a stop word
			if (allWords.containsKey(titleTokens.get(0))) {
				allWords.put(titleTokens.get(0), allWords.get(titleTokens.get(0)) + 1);
			} else {
				allWords.put(titleTokens.get(0), 1);
			}
		} else if (titleTokens.size() > 1) {
			createBiGram(titleTokens);
		}
	}

	// TODO calculate score and print 1k bigrams

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("usage: java -jar CoOccurrences.jar <plot list file>");
			System.exit(-1);
		}

		System.out.println("Parsing stop.words...");
		parseStopWords();

		System.out.println("Parsing plot.list...");
		parsePlotList(args[0]);

	}
}
