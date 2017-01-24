package ue_inforet_cooccurrences_variant;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
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
	private THashMap<String, TIntArrayList> allWords = new THashMap<>();
	private TObjectIntHashMap<String> wordCount = new TObjectIntHashMap<>();
	private ArrayList<Tuple<String, Integer>> sortedWords = new ArrayList<>();

	// constructor
	private CoOccurrences() {
	}

	class Tuple<K, V> {
		K first;
		V second;

		Tuple(K f, V s) {
			this.first = f;
			this.second = s;
		}

/*		@Override
		public int hashCode() {
			return this.first.hashCode() + this.second.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return this.first.equals(((Tuple<?, ?>) obj).first)
				&& this.second.equals(((Tuple<?, ?>) obj).second);
		}*/
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

	private void parsePlotList(String plotFile) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("MV:")) {
					parseTitle(StringUtils.substring(line, 4, line.length()).toLowerCase());
				} else if (line.startsWith("PL:")) {
					StringTokenizer st = new StringTokenizer(StringUtils.substring(line, 4,
						line.length()).toLowerCase(), " .,:!?", false);
					while (st.hasMoreTokens()) {
						String token = st.nextToken();
						if (!stopWords.contains(token)) {
							allWords.putIfAbsent(token, new TIntArrayList());
							wordCount.adjustOrPutValue(token, 1, 1);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Tokenized " + allWords.size() + " token.");
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

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (!stopWords.contains(token)) {
				allWords.putIfAbsent(token, new TIntArrayList());
				wordCount.adjustOrPutValue(token, 1, 1);
			}
			//System.out.println(token);
		}
	}


/*	private void sortWordCount() {
		TObjectIntIterator it = wordCount.iterator();

		for (int i = 0; i < wordCount.size(); ++i) {
			it.advance();
			sortedWords.add(new Tuple<>(it.key().toString(), it.value()));
		}

		sortedWords.sort((o1, o2) -> o2.second.compareTo(o1.second));

*//*		for (int i = 0; i < 10; ++i) {
			System.out.println("word: \"" + sortedWords.get(i).first + "\", count: " + sortedWords.get(i).second);
		}*//*
		int occur = 0;
		while (sortedWords.get(occur).second > 1000) {
			++occur;
		}
		System.out.println("Number of words with occurrences > 1k: " + occur + ".");
	}*/

	public static void main(String[] args) {
		CoOccurrences co = new CoOccurrences();

		if (args.length < 1) {
			System.err.println("usage: java -jar CoOccurrences.jar <plot list file>");
			System.exit(-1);
		}

		System.out.println("Parsing stop.words...");
		co.parseStopWords();

		System.out.println("Parsing plot.list...");
		long tic = System.nanoTime();
		co.parsePlotList(args[0]);
		double tac = System.nanoTime() - tic;
		tac = tac / 1000000000;
		System.out.println("Time: " + tac + "s");

		//co.sortWordCount();
	}
}
