package ue_inforet_cooccurrences_variant;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

public class CoOccurrences {
	private THashSet<String> stopWords = new THashSet<>(175);
	private TObjectIntHashMap<Bigram<String, String>> allBigrams = new TObjectIntHashMap<>(7650000);
	private TObjectIntHashMap<String> wordCount = new TObjectIntHashMap<>(766000);

	// constructor
	private CoOccurrences() {
	}

	class Bigram<K, V> {
		K first;
		V second;

		Bigram(K f, V s) {
			this.first = f;
			this.second = s;
		}

		public int hashCode() {
			return this.first.hashCode() + this.second.hashCode();
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		public boolean equals(Object obj) {
			return this.first.equals(((Bigram<?, ?>) obj).first)
				&& this.second.equals(((Bigram<?, ?>) obj).second);
		}
	}

	// parse and safe the stop-words
	private void parseStopWords() {
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

	// parse the plot.list file
	private void parsePlotList(String plotFile) {
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

	// create a bigram out of a list of tokens
	private void createBiGram(ArrayList<String> tokenList) {
		// count the words in the plot without stop-words
		for (String token : tokenList) {
			if (!stopWords.contains(token)) {
				wordCount.adjustOrPutValue(token, 1, 1);
			}
		}
		// create some bigrams
		for (int i = 0; i + 1 < tokenList.size(); ++i) {
			// check for stop words in the bigram
			if (!stopWords.contains(tokenList.get(i)) && !stopWords.contains(tokenList.get(i + 1))) {
				allBigrams.adjustOrPutValue(new Bigram<>(tokenList.get(i), tokenList.get(i + 1)), 1, 1);
			}
		}
	}

	// handle the different title formats
	private void parseTitle(String title) {
		// remove {{SUSPENDED}}
		if (title.contains("{{suspended}}")) {
			title = title.replace(" {{suspended}}", "").intern();
		}

		StringTokenizer st;

		// +++ series +++
		if (title.startsWith("\"") && !title.endsWith("}")) {
			int end = 3;
			// remove the year
			for (int i = title.length() - 1; title.charAt(i) != '('; --i) {
				end++;
			}

			st = new StringTokenizer(title.substring(1, title.length() - end).intern(), " .,:!?", false);
		}
		// +++ episode +++
		else if (title.startsWith("\"") && title.endsWith("}")) {
			int end = 3;

			// remove episodeTitle
			title = StringUtils.substring(title, 0, title.indexOf('{') - 1).toLowerCase().intern();

			// remove the year
			for (int i = title.length() - 1; title.charAt(i) != '('; --i) {
				end++;
			}

			st = new StringTokenizer(title.substring(1, title.length() - end).intern(), " .,:!?", false);
		}
		// +++ movie +++
		else {
			int end = 3;
			// remove the year
			for (int i = title.length() - 1; title.charAt(i) != '('; --i) {
				end++;
			}

			st = new StringTokenizer(title.substring(0, title.length() - end + 1).intern(), " .,:!?", false);
		}

		// put all the title words in a list.
		ArrayList<String> titleTokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			titleTokens.add(st.nextToken());
		}

		// if there is only one word in the title add the word to the overall wordcount
		if (titleTokens.size() == 1) {
			// count it only if it is not a stop word
			if (!stopWords.contains(titleTokens.get(0))) {
				wordCount.adjustOrPutValue(titleTokens.get(0), 1, 1);
			}
		} else if (titleTokens.size() > 1) {
			createBiGram(titleTokens);
		}
	}

	// calculate the collocations and print the 1k highest scored
	private void calcCollocations() {
		ArrayList<Bigram<String, Double>> results = new ArrayList<>();

		// iterate through all the bigrams
		TObjectIntIterator<Bigram<String, String>> it = allBigrams.iterator();
		for (int i = 0; i < allBigrams.size(); ++i) {
			it.advance();
			// if both words of the collocation are more than 1k times in the plot, calculate the score
			if ((wordCount.get(it.key().first) >= 1000) && (wordCount.get(it.key().second) >= 1000)) {
				// WOAH, what a line! ok, this happens here: we add every matching bigram as one string
				// to our results list; we also add the score, which was the 2 times the frequency of a
				// bigram (it.value()) divided through the frequency of the words building the bigram
				// (it.key.first/second are the two words). we have to cast this into double.
				results.add(new Bigram<>(it.key().first + " " + it.key().second,
					(2 * it.value()) / (double) (wordCount.get(it.key().first)
						+ wordCount.get(it.key().second))));
			}
		}

		// sort for the score
		results.sort((o1, o2) -> o2.second.compareTo(o1.second));

		for (int i = 0; i < 1000; ++i) {
			System.out.println(results.get(i).first + " " + results.get(i).second);
		}
	}

	public static void main(String[] args) {
		CoOccurrences co = new CoOccurrences();

		if (args.length < 1) {
			System.err.println("usage: java -jar CoOccurrences.jar <plot list file>");
			System.exit(-1);
		}

		System.out.println("Parsing stop.words...");
		long tic = System.nanoTime();
		co.parseStopWords();
		System.out.println("Parsing plot.list...");
		co.parsePlotList(args[0]);
		System.out.println("Calculating Collocations...");
		co.calcCollocations();
		double tac = System.nanoTime() - tic;
		tac = tac / 1000000000;
		System.out.println("Time: " + tac + "s");
	}
}
