// DO NOT CHANGE THIS PACKAGE NAME.
package ue_inforet_bool_study;

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

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(plotFile),
			StandardCharsets.ISO_8859_1))) {
			String line;
			while ((line = reader.readLine()) != null){
				// TODO add Lucene Stuff here

			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
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
