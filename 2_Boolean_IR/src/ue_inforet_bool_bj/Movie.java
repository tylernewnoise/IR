package ue_inforet_bool_bj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Movie {

	private Integer movieID;
	private String titleLine;
	private HashMap<String, List<Byte>> positionInTitleHashmap;
	private HashMap<String, List<Byte>> positionInEpisodeTitle;
	private HashMap<String, List<Short>> positionInPlotHashmap;

	public Movie(Integer movieID, String titleLine) {
		this.movieID = movieID;
		this.titleLine = titleLine;
		this.positionInTitleHashmap = new HashMap<String, List<Byte>>();
		this.positionInEpisodeTitle = new HashMap<String, List<Byte>>();
		this.positionInPlotHashmap = new HashMap<String, List<Short>>();
	}

	/***
	 * @return id of movie object in allMovies List
	 */
	public Integer getID() {
		return this.movieID;
	}

	/***
	 * @param movieID of movie objectin allMovies List
	 */
	public void setID(Integer movieID) {
		this.movieID = movieID;
	}

	/***
	 * @return whole line of titlerow for output
	 */
	public String getTitleLine() {
		return this.titleLine;
	}

	/***
	 * @param titleLine whole line of titlerow from input
	 */
	public void setTitleLine(String titleLine) {
		this.titleLine = titleLine;
	}

	//						+++ Plot +++

	/***
	 * Set the position of the term in the plot
	 * @param term we are handling
	 * @param position of term in plot
	 */
	public void setPositionOfTermInPlot(String term, Short position) {
		if (this.positionInPlotHashmap.containsKey(term)) {
			this.positionInPlotHashmap.get(term).add(position);
		} else {
			this.positionInPlotHashmap.put(term, new ArrayList<>());
			this.positionInPlotHashmap.get(term).add(position);
		}
	}

	/***
	 * @param term
	 * @return the position of the term in the plot
	 */
	public List<Short> getPositionsOfTermInPlot(String term) {
		return this.positionInPlotHashmap.get(term);
	}

	/***
	 * @param term
	 * @param position
	 * @return if or not a term is at a position in the plot
	 */
	public boolean isTermAtPositionInPlot(String term, Short position) {
		return this.positionInPlotHashmap.get(term).contains(position);
	}

	//						+++ Title +++

	/***
	 * Set the position of the term in the title
	 * @param term
	 * @param position
	 */
	public void setPositionOfTermInTitle(String term, Byte position) {
		if (this.positionInTitleHashmap.containsKey(term)) {
			this.positionInTitleHashmap.get(term).add(position);
		} else {
			this.positionInTitleHashmap.put(term, new ArrayList<>());
			this.positionInTitleHashmap.get(term).add(position);
		}
	}

	/***
	 * @param term
	 * @return the position of the term in the title
	 */
	public List<Byte> getPositionsOfTermInTitle(String term) {
		return this.positionInTitleHashmap.get(term);
	}

	/***
	 * @param term
	 * @param position
	 * @return if or not a term is at a position in the title.
	 */
	public boolean isTermAtPositionInTitle(String term, Byte position) {
		return this.positionInTitleHashmap.get(term).contains(position);
	}

	//						+++ Episode Title +++

	/***
	 * Set the position of the term in the episode title
	 * @param term
	 * @param position
	 */
	public void setPositionOfTermInEipsodeTitle(String term, Byte position) {
		if (this.positionInEpisodeTitle.containsKey(term)) {
			this.positionInEpisodeTitle.get(term).add(position);
		} else {
			this.positionInEpisodeTitle.put(term, new ArrayList<>());
			this.positionInEpisodeTitle.get(term).add(position);
		}
	}

	/***
	 * @param term
	 * @return the position of the term in the plot
	 */
	public List<Byte> getPositionsOfTermInEpisodeTitle(String term) {
		return this.positionInEpisodeTitle.get(term);
	}

	/***
	 * @param term
	 * @param position
	 * @return if or not a term is at a position in the episode title
	 */
	public boolean isTermAtPositionInEpisodeTitle(String term, Byte position) {
		return this.positionInEpisodeTitle.get(term).contains(position);
	}
}
