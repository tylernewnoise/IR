// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_study_variant;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;

public class BooleanQueryLucene {

	/**
	 * DO NOT CHANGE THE CONSTRUCTOR. DO NOT ADD PARAMETERS TO THE CONSTRUCTOR.
	 */
	public BooleanQueryLucene() {
	}

	/**
	 * A method for reading the textual movie plot file and building a Lucene index.
	 * The purpose of the index is to speed up subsequent boolean searches using
	 * the {@link #booleanQuery(String) booleanQuery} method.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param plotFile the textual movie plot file 'plot.list', obtainable from <a
	 *                 href="http://www.imdb.com/interfaces"
	 *                 >http://www.imdb.com/interfaces</a> for personal, non-commercial
	 *                 use.
	 */
	public void buildIndices(String plotFile) {
		boolean isPlotLine = false;
		StandardAnalyzer analyzer = new StandardAnalyzer();
		Directory index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		Document doc = new Document();

		StringBuilder stringBuilder = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			IndexWriter w = new IndexWriter(index, config);
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("M")) {
					if (isPlotLine) {
						doc.add(new TextField("plot", stringBuilder.toString(), Field.Store.YES));
						w.addDocument(doc);
						isPlotLine = false;
						doc = new Document();
						stringBuilder = new StringBuilder();
					}
					doc.add(new TextField("mvline", line, Field.Store.YES));
					getTitleTypeYear(doc, line.substring(4, line.length()));
				}
				if (line.startsWith("PL:")) {
					stringBuilder.append(line.substring(4, line.length()));
					isPlotLine = true;
				}
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		// for testing
		/*try {
			Query q = new QueryParser("title", analyzer).parse("title:Terminator");
			int hitsPerPage = 10;

			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs docs = searcher.search(q, hitsPerPage);
			ScoreDoc[] hits = docs.scoreDocs;

			// 4. display results
			System.out.println("Found " + hits.length + " hits.");
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document d = searcher.doc(docId);
				System.out.println((i + 1) + ". " + d.get("mvline"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	/* gets the year, title, episode title out of the line */
	private void getTitleTypeYear(Document doc, String mvLine) {

		// remove {{SUSPENDED}}
		if (mvLine.contains("{{suspended}}")) {
			mvLine = mvLine.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (mvLine.startsWith("\"") && !mvLine.endsWith("}")) {
			// add the type to the hash map
			doc.add((new TextField("type", "series", Field.Store.YES)));
			parseTitleAndYear(doc, mvLine, true);
		}
		// +++ episode +++
		else if (mvLine.contains("\"") && mvLine.endsWith("}")) {
			doc.add((new TextField("type", "episode", Field.Store.YES)));

			String episodeTitle = ""; // TODO Textbuilder verwenden
			for (int i = mvLine.length() - 1; mvLine.charAt(i) != '{'; --i) {
				episodeTitle += mvLine.charAt(i);
			}

			doc.add((new TextField("episodetitle", new StringBuilder(episodeTitle).reverse().toString(),
				Field.Store.YES)));

			parseTitleAndYear(doc, mvLine.substring(0, mvLine.indexOf('{')), true);
		}
		// +++ television +++
		else if (mvLine.contains(") (tv)")) {
			doc.add((new TextField("type", "television", Field.Store.YES)));
			parseTitleAndYear(doc, mvLine.substring(0, mvLine.length() - 6), false);
		}
		// +++ video +++
		else if (mvLine.contains(") (v)")) {
			doc.add((new TextField("type", "video", Field.Store.YES)));
			parseTitleAndYear(doc, mvLine.substring(0, mvLine.length() - 5), false);
		}
		// +++ video game +++
		else if (mvLine.contains(") (vg)")) {
			doc.add((new TextField("type", "videogame", Field.Store.YES)));
			parseTitleAndYear(doc, mvLine.substring(0, mvLine.length() - 6), false);
		} else {
			// +++ movie +++
			doc.add((new TextField("type", "movie", Field.Store.YES)));
			parseTitleAndYear(doc, mvLine, false);
		}
	}

	private void parseTitleAndYear(Document doc, String mvLine, boolean isSeries) {
		String year = "";
		int end = 3;

		for (int i = mvLine.length() - 1; mvLine.charAt(i) != '('; --i) {
			year += mvLine.charAt(i);
			end++;
		}
		doc.add((new TextField("year", new StringBuilder(year).reverse().toString(), Field.Store.YES)));

		if (isSeries) {
			doc.add((new TextField("title", mvLine.substring(1, mvLine.length() - end), Field.Store.YES)));
		} else {
			doc.add((new TextField("title", mvLine.substring(0, mvLine.length() - end + 1), Field.Store.YES)));
		}
	}

	/**
	 * A method for performing a boolean search on a textual movie plot file after
	 * Lucene indices were built using the {@link #buildIndices(String) buildIndices}
	 * method. The movie plot file contains entries of the <b>types</b> movie,
	 * series, episode, television, video, and videogame. This method allows queries
	 * following the Lucene query syntax on any of the <b>fields</b> title, plot, year,
	 * episode, and type. Note that queries are case-insensitive and stop words are
	 * removed.<br>
	 * <br>
	 * Examples of queries include the following:
	 * <p>
	 * <pre>
	 * title:"game of thrones" AND type:episode AND (plot:Bastards OR (plot:Jon AND plot:Snow)) -plot:son
	 * title:"Star Wars" AND type:movie AND plot:Luke AND year:[1977 TO 1987]
	 * plot:Berlin AND plot:wall AND type:television
	 * plot:men~1 AND plot:women~1 AND plot:love AND plot:fool AND type:movie
	 * title:westworld AND type:episode AND year:2016 AND plot:Dolores
	 * plot:You AND plot:never AND plot:get AND plot:A AND plot:second AND plot:chance
	 * plot:Hero AND plot:Villain AND plot:destroy AND type:movie
	 * (plot:lover -plot:perfect) AND plot:unfaithful* AND plot:husband AND plot:affair AND type:movie
	 * (plot:Innocent OR plot:Guilty) AND plot:crime AND plot:murder AND plot:court AND plot:judge AND type:movie
	 * plot:Hero AND plot:Marvel -plot:DC AND type:movie
	 * plot:Hero AND plot:DC -plot:Marvel AND type:movie
	 * </pre>
	 * <p>
	 * More details on the query syntax can be found at <a
	 * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
	 * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param queryString the query string, formatted according to the Lucene query syntax.
	 * @return the exact content (in the textual movie plot file) of the title
	 * lines (starting with "MV: ") of the documents matching the query
	 */
	public Set<String> booleanQuery(String queryString) {
		// TODO: insert code here
		return new HashSet<>();
	}

	/**
	 * A method for closing any open file handels or a ThreadPool.
	 * <p>
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 */
	public void close() {
		// TODO: you may insert code here
	}

	public static void main(String[] args) {
		BooleanQueryLucene bq = new BooleanQueryLucene();
		if (args.length < 3) {
			System.err
				.println("usage: java -jar BooleanQuery.jar <plot list file> <queries file> <results file>");
			System.exit(-1);
		}

		System.out.println("Boolean Query Variant ");

		// build indices
		System.out.println("building indices...");
		long tic = System.nanoTime();
		Runtime runtime = Runtime.getRuntime();
		long mem = runtime.totalMemory();
		bq.buildIndices(args[0]);
		System.out
			.println("runtime: " + (System.nanoTime() - tic) + " nanoseconds");
		System.out
			.println("memory: " + ((runtime.totalMemory() - mem) / (1048576L))
				+ " MB (rough estimate)");

		// parsing the queries that are to be run from the queries file
		List<String> queries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(args[1]), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null)
				queries.add(line);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// parsing the queries' expected results from the results file
		List<Set<String>> results = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(args[2]), StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null) {
				Set<String> result = new HashSet<>();
				results.add(result);
				for (int i = 0; i < Integer.parseInt(line); i++) {
					result.add(reader.readLine());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// run queries
		for (int i = 0; i < queries.size(); i++) {
			String query = queries.get(i);
			Set<String> expectedResult = i < results.size() ? results.get(i)
				: new HashSet<>();
			System.out.println();
			System.out.println("query:           " + query);
			tic = System.nanoTime();
			Set<String> actualResult = bq.booleanQuery(query);

			// sort expected and determined results for human readability
			List<String> expectedResultSorted = new ArrayList<>(expectedResult);
			List<String> actualResultSorted = new ArrayList<>(actualResult);
			Comparator<String> stringComparator = Comparator.naturalOrder();
			expectedResultSorted.sort(stringComparator);
			actualResultSorted.sort(stringComparator);

			System.out.println("runtime:         " + (System.nanoTime() - tic)
				+ " nanoseconds.");
			System.out.println("expected result: " + expectedResultSorted.toString());
			System.out.println("actual result:   " + actualResultSorted.toString());
			System.out.println(expectedResult.equals(actualResult) ? "SUCCESS"
				: "FAILURE");
		}

		bq.close();
	}

}
