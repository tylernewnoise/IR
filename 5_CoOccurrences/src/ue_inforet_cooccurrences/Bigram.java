package ue_inforet_cooccurrences;

/**
 * Created by benjamin on 24.01.17.
 * Modified by falko on 25.01.17.
 */
public class Bigram {

	private String first;
	private String second;
	private double score;

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
	 * @param first sets first word of the bigram
	 */
	void setFirst(String first) {
		this.first = first;
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
	 * @param second sets second word of the bigram
	 */
	void setSecond(String second) {
		this.second = second;
	}

	/***
	 *
	 * @param score sets score of the bigram, has to be 0 <= score <= 1
	 */
	void setScore(double score) {
		if (score <= 0 && score >= 1) {
			this.score = score;
		} else {
			System.out.println("Score: " + score + " of the Bigram <" + this.first + "," + this.second + "> is out of Range");
		}
	}

	/***
	 *
	 * @return score of the bigram if already set. if not set yet it returns 0 and a warning.
	 */
	double getScore() {
		if (this.score < 0) {
			System.out.println("Score of the Bigram <" + this.first + "," + this.second + "> has not been set yet.");
			return 0;
		} else {
			return this.score;
		}
	}

}
