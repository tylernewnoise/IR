package ue_inforet_cooccurrences;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class CoOccurrences {

	private TObjectIntHashMap<String> allWords = new TObjectIntHashMap<>();
	private THashMap <Bigram, Integer> allBigrams = new THashMap<>();
	private THashSet<String> stopWords = new THashSet<>();

	private CoOccurrences(){}

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

	private void parsePlotList(String plotFile){
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("MV:")) {
					parseTitle(line.substring(4, line.length()).toLowerCase());
				} else if (line.startsWith("PL:")) {
					StringTokenizer st = new StringTokenizer(line.substring(4, line.length()).
						toLowerCase(), " .,:!?", false);
					while (st.hasMoreTokens()) {
						String token = st.nextToken();
						allWords.adjustOrPutValue(token, 1, 1);
						// TODO add occurrences and check for stop words and stuff
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// handle the different title formats
	private void parseTitle(String title) {
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

		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			//TODO check cooccurrences and stop words
			allWords.adjustOrPutValue(token, 1, 1);
		}
	}

	public static void main(String[] args) {
		CoOccurrences co = new CoOccurrences();
		if (args.length < 1) {
			System.err.println("usage: java -jar CoOccurrences.jar <plot list file>");
			System.exit(-1);
		}

		System.out.println("Parsing stop.words...");
		co.parseStopWords();
	}
}
