package ue_inforet_cooccurrences;

/**
 * Created by benjamin on 24.01.17.
 */
public class Bigram {

	private String first;
	private String second;
	private double score;


	public Bigram(String first, String second) {
		this.first = first;
		this.second = second;
		this.score = -1;
	}

	/***
	 *
	 * @return first word of the bigram
	 */
	public String getFirst() { return this.first; }

	/***
	 *
	 * @param first sets first word of the bigram
	 */
	public void setFirst(String first) { this.first = first; }

	/***
	 *
	 * @return second word of the bigram
	 */
	public String getSecond() { return this.second; }

	/***
	 *
	 * @param second sets second word of the bigram
	 */
	public void setSecond(String second) { this.second = second; }

	/***
	 *
	 * @param score sets score of the bigram, has to be 0 <= score <= 1
	 */
	public void setScore(double score){
		if (score <= 0 && score >= 1){
			this.score = score;
		} else {
			System.out.println("Score: " + score + " of the Bigram <" + this.first + "," + this.second + "> is out of Range");
		}
	}

	/***
	 *
	 * @return score of the bigram if already set. if not set yet it returns 0 and a warning.
	 */
	public double getScore(){
		if (this.score < 0){
			System.out.println("Score of the Bigram <" + this.first + "," + this.second + "> has not been set yet." );
			return 0;
		} else {
			return this.score;
		}
	}

}
