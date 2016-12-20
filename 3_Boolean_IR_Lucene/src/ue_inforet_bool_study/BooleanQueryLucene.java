// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_study;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File;

import java.nio.charset.StandardCharsets;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
// import org.apache.lucene.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.RAMDirectory;
//##
// for simple query testing
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;



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
	 *
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param plotFile
	 *          the textual movie plot file 'plot.list', obtainable from <a
	 *          href="http://www.imdb.com/interfaces"
	 *          >http://www.imdb.com/interfaces</a> for personal, non-commercial
	 *          use.
	 */
	public void buildIndices(String plotFile) {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile), StandardCharsets.ISO_8859_1))) {

			String line;  // current input (line)
			boolean lastLineWasPlot = false; // check if new MV Line starts
			StringBuilder currentMovieString = new StringBuilder();

			// set Analyzer
			Analyzer myAnalyzer = new StandardAnalyzer();
			// Specify	a	directory	and	an	index	writer
			//VORLAGE Directory index = FSDirectory.open(new File(plotFile).toPath()); aber RAM reicht ja ;)
			Directory index = new RAMDirectory();
			IndexWriterConfig config = new IndexWriterConfig(myAnalyzer);

			IndexWriter writer = new IndexWriter(index, config);
			// Create	a	document	and	add	this	document	to	the	index:
			Document doc = new Document();
			/* example
			doc.add(new StringField(“id”, id, StringField.Store.YES));
			doc.add(new TextField(“title”, title, TextField.Store.YES));
			writer.addDocument(doc);  */


			while ((line = reader.readLine()) != null){
				if (line.startsWith("MV:") && (lastLineWasPlot)) {
					doc.add(new TextField("plotline", currentMovieString.toString(), Field.Store.YES));
					writer.addDocument(doc);
					currentMovieString = new StringBuilder();
					doc = new Document();
					lastLineWasPlot = false;
				}
				if (line.startsWith("MV:") && (!lastLineWasPlot)) {
					doc.add(new TextField("movieline", line, Field.Store.YES));
					getTitleTypeYear(doc, line.substring(4, line.length()));
				}
				if (line.startsWith("PL:")) {
					currentMovieString.append(line.substring(4, line.length()));
					currentMovieString.append(" ");  // Space necessary at the end of each plotline
					lastLineWasPlot = true;
				}
			}
			// after last line was read from file - last operation has to be triggered
			doc.add(new TextField("plotline", currentMovieString.toString(), Field.Store.YES));
			writer.addDocument(doc);
			writer.close();

			// <thanks Falko>
			// for testing
			try {
				//Query q = new QueryParser("year", analyzer).parse("year:1995");
				//Query q = new QueryParser("title", analyzer).parse("title:Genisys");
				//Query q = new QueryParser("type", analyzer).parse("type:television");
				//Query q = new QueryParser("year", myAnalyzer).parse("year:2015");
				//Query q = new QueryParser("type", myAnalyzer).parse("type:videogame");
				Query q = new QueryParser("plotline", myAnalyzer).parse("Marshall");
				IndexReader reader2 = DirectoryReader.open(index);
				IndexSearcher searcher = new IndexSearcher(reader2);
				TopDocs docs = searcher.search(q, Integer.MAX_VALUE);
				ScoreDoc[] hits = docs.scoreDocs;
				// display results
				System.out.println("Found " + hits.length + " hits.");
				for (int i = 0; i < hits.length; ++i) {
					int docId = hits[i].doc;
					Document document = searcher.doc(docId);
					System.out.println((i + 1) + ". " + document.get("movieline"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// </thanks Falko>

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}



	// helper Methods
	private void getTitleTypeYear(Document doc, String movieline) {

		// remove {{SUSPENDED}}
		if (movieline.contains("{{suspended}}")) {
			movieline = movieline.replace(" {{suspended}}", "");
		}

		// +++ series +++
		if (movieline.startsWith("\"") && !movieline.endsWith("}")) {
			doc.add((new TextField("type", "series", Field.Store.YES)));
			parseTitleAndYear(doc, movieline, true);
		}
		// +++ episode +++
		else if (movieline.contains("\"") && movieline.endsWith("}")) {
			doc.add((new TextField("type", "episode", Field.Store.YES)));

			StringBuilder currentString = new StringBuilder();

			for (int i = movieline.length() - 2; movieline.charAt(i) != '{'; --i) {
				currentString.append(movieline.charAt(i));
			}

			doc.add((new TextField("episodetitle",
					new StringBuilder(currentString.toString()).reverse().toString(), Field.Store.YES)));

			parseTitleAndYear(doc, movieline.substring(0, movieline.indexOf('{') - 1), true);
		}
		// +++ television +++
		else if (movieline.contains(") (TV)")) {
			doc.add((new TextField("type", "television", Field.Store.YES)));
			parseTitleAndYear(doc, movieline.substring(0, movieline.length() - 5), false);
		}
		// +++ video +++
		else if (movieline.contains(") (V)")) {
			doc.add((new TextField("type", "video", Field.Store.YES)));
			parseTitleAndYear(doc, movieline.substring(0, movieline.length() - 4), false);
		}
		// +++ video game +++
		else if (movieline.contains(") (VG)")) {
			doc.add((new TextField("type", "videogame", Field.Store.YES)));
			parseTitleAndYear(doc, movieline.substring(0, movieline.length() - 5), false);
		} else {
			// +++ movie +++
			doc.add((new TextField("type", "movie", Field.Store.YES)));
			parseTitleAndYear(doc, movieline, false);
		}
	}

	// helper 2
	private void parseTitleAndYear(Document doc, String movieline, boolean isSeries) {
		int end = 3;
		StringBuilder currentString = new StringBuilder();

		for (int i = movieline.length() - 2; movieline.charAt(i) != '('; --i) {
			if (movieline.charAt(i) >= '0' && movieline.charAt(i) <= '9') {
				currentString.append(movieline.charAt(i));
			}
			end++;
		}
		doc.add((new StringField("year", new StringBuilder(currentString.toString()).reverse().toString(), Field.Store.YES)));

		if (isSeries) {
			doc.add((new TextField("title", movieline.substring(1, movieline.length() - end - 1), Field.Store.YES)));
		} else {
			doc.add((new TextField("title", movieline.substring(0, movieline.length() - end), Field.Store.YES)));
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
	 *
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
	 *
	 * More details on the query syntax can be found at <a
	 * href="http://www.lucenetutorial.com/lucene-query-syntax.html">
	 * http://www.lucenetutorial.com/lucene-query-syntax.html</a>.
	 *
	 * DO NOT CHANGE THIS METHOD'S INTERFACE.
	 *
	 * @param queryString
	 *          the query string, formatted according to the Lucene query syntax.
	 * @return the exact content (in the textual movie plot file) of the title
	 *         lines (starting with "MV: ") of the documents matching the query
	 */
	public Set<String> booleanQuery(String queryString) {
		// TODO: insert code here
		return new HashSet<>();
	}

	/**
	 * A method for closing any open file handels or a ThreadPool.
	 *
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
