package ue_inforet_cooccurrences;

/**
 * Created by benjamin on 24.01.17.
 * Modified by falko on 08.02.17.
 */
public class Bigram {

	private String first;
	private String second;
	private double score;

	Bigram(String first, String second) {
		this.first = first;
		this.second = second;
		this.score = -1;
	}

	public int hashCode() {
		return this.first.hashCode() + this.second.hashCode();
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object obj) {
		return this.first.equals(((Bigram) obj).first) && this.second.equals(((Bigram) obj).second);
	}

	/***
	 *
	 * @return first word of the bigram
	 */
	String getFirst() {
		return this.first;
	}

	/***
	 *
	 * @return second word of the bigram
	 */
	String getSecond() {
		return this.second;
	}

	/***
	 *
	 * @param score sets score of the bigram
	 */
	void setScore(double score) {
			this.score = score;
	}

	/***
	 *
	 * @return score of the bigram
	 */
	double getScore() {
			return this.score;
	}
}
