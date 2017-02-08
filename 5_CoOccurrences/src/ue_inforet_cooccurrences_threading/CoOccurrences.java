package ue_inforet_cooccurrences_threading;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

public class CoOccurrences {
	private HashSet<String> stopWords = new HashSet<>(175);
	private ConcurrentHashMap<String, AtomicInteger> wordCount = new ConcurrentHashMap<>(766000);
	private ConcurrentHashMap<BigramTuple, AtomicInteger> allBigrams = new ConcurrentHashMap<>(7650000);
	private ExecutorService executorService;

	// constructor
	private CoOccurrences() {
	}

	class BigramTuple<K, V> {
		K first;
		V second;

		BigramTuple(K f, V s) {
			this.first = f;
			this.second = s;
		}

		public int hashCode() {
			return this.first.hashCode() + this.second.hashCode();
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		public boolean equals(Object obj) {
			return this == obj || obj != null && getClass() == obj.getClass() &&
				this.first.equals(((BigramTuple<?, ?>) obj).first) &&
				this.second.equals(((BigramTuple<?, ?>) obj).second);
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

		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;
			ArrayList<String> plotTokens = new ArrayList<>();

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("MV:")) {
					// was the line before a PL:-line? if yes make some bigrams of the plot
					if (isPlotLine) {
						startThreads(plotTokens);
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
			reader.close();
			executorService.shutdown();
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void startThreads(ArrayList<String> tokenList) {
		executorService.execute(() -> createBiGram(tokenList));
	}

	private void createBiGram(ArrayList<String> tokenList) {
		for (String token : tokenList) {
			if (!stopWords.contains(token)) {
				wordCount.putIfAbsent(token, new AtomicInteger());
				wordCount.get(token).incrementAndGet();
			}
		}

		for (int i = 0; i + 1 < tokenList.size(); ++i) {
			if (!stopWords.contains(tokenList.get(i)) && !stopWords.contains(tokenList.get(i + 1))) {
				BigramTuple tmp = new BigramTuple<>(tokenList.get(i), tokenList.get(i + 1));
				allBigrams.putIfAbsent(tmp, new AtomicInteger());
				allBigrams.get(tmp).incrementAndGet();
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
				wordCount.putIfAbsent(titleTokens.get(0), new AtomicInteger());
				wordCount.get(titleTokens.get(0)).incrementAndGet();
			}
		} else if (titleTokens.size() > 1) {
			createBiGram(titleTokens);
		}
	}

	private void calcCollocations() {
		ArrayList<BigramTuple<String, Double>> results = new ArrayList<>(2081000);

		for (Map.Entry<BigramTuple, AtomicInteger> entry : allBigrams.entrySet()) {
			String firstWordOfBigram = entry.getKey().first.toString();
			String secondWordOfBigram = entry.getKey().second.toString();
			int countFirst = wordCount.get(firstWordOfBigram).intValue();
			int countSecond = wordCount.get(secondWordOfBigram).intValue();

			if (countFirst >= 1000 && countSecond >= 1000) {
				results.add(new BigramTuple<>(firstWordOfBigram + " " + secondWordOfBigram,
					(2 * entry.getValue().intValue()) / (double) (countFirst + countSecond)));
			}
		}

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

		co.parseStopWords();
		co.parsePlotList(args[0]);
		co.calcCollocations();
	}
}
