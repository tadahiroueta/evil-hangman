/**
 * EvilHangman.java  06/04/2015
 *
 * @author - Jane Doe
 * @author - Period n
 * @author - Id nnnnnnn
 *
 * @author - I received help from ...
 *
 */

import java.sql.*;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.*;

public class EvilHangman
{
	private boolean debug;
	private Scanner in;
	private List<String> wordList;
	private int wordLength;
	private int remainingGuesses;
	private String solution;
	private String guessedLetters;


	/**
	 * Construct an EvilHangman object.
	 * @param fileName the name of the file that contains the word list.
	 * @param debug indicates if the size of the remaining word list
	 *        should be included in the toString result.
	 * @throws FileNotFoundException when the fileName file does not exist.
	 */
   public EvilHangman(String fileName, boolean debug)
    	 throws FileNotFoundException
   {
		this.debug = debug;
		in = new Scanner(System.in);
		inputWords(fileName);

		System.out.print("Number of guesses? ");
		remainingGuesses = in.nextInt();

		char[] charArray = new char[wordLength];
		Arrays.fill(charArray, '-');
		solution = new String(charArray);

		guessedLetters = "";
   }

	/**
	 * Plays one the game.  The user guesses letters until either
	 * they guess the word, or they run out of guesses.
	 * Game status is printed each turn and winning / losing
	 * information is printed at the conclusion of the game.
	 */
   public void playGame()
   {
		while (solution.contains("-") && remainingGuesses > 0) {
			System.out.println(this);
			String letter = inputLetter();
			guessedLetters += letter;
			List<Partition> partitions = getPartitionList(letter);
			removeSmallerPartitions(partitions);
			wordList = getWordsFromOptimalPartition(partitions);
			String oldSolution = solution;
			substitute(wordList.get(0), letter);
			if (oldSolution.equals(solution)) remainingGuesses--;
		}
		if (remainingGuesses > 0) System.out.println("You win, congratulations!");
		else System.out.println("You lose, sorry!");
		System.out.println(String.format("The word was \"%s\"", wordList.get(new Random().nextInt(wordList.size()))));
   }

	/**
	 * Creates and returns a status string that indicates the
	 * state of the game.
	 * @return the game status string.
	 */
   public String toString()
   {
		return debug ? String.format(
				"""
						
				Remaining guesses: %d
				Guessed letters: %s
				Solution: %s
				Remaining words: %d
				""",
				remainingGuesses,
				guessedLetters,
				solution,
				wordList.size()
		) : String.format(
				"""
				
				Remaining guesses: %d
				Guessed letters: %s
				Solution: %s
				""",
				remainingGuesses,
				guessedLetters,
				solution
		);
   }


	////////// PRIVATE HELPER METHODS //////////

	/**
	 * Helper method for the constructor:
	 * Inputs the word length from the user, reads in the words from
	 * the fileName file, and initializes the wordList instance variable
	 * with the words of the correct length.  This method loops until
	 * a valid word length is entered.
	 * @param fileName the name of the file that contains the word list.
	 * @throws FileNotFoundException when the fileName file does not exist.
	 */
   private void inputWords(String fileName) throws FileNotFoundException
   {
		wordList = new ArrayList<String>();
		while (wordList.isEmpty()) {
			System.out.print("Word length? ");
			wordLength = in.nextInt();
			Scanner file = new Scanner(new File(fileName));
			String word;
			while (file.hasNext()) {
				word = file.next();
				if (word.length() == wordLength) wordList.add(word);
			}
			if (wordList.isEmpty()) System.out.println("There are no words with " + wordLength + " letters.");
		}
   }

	/**
	 * Helper method for playGame:
	 * Inputs a one-letter string from the user.
	 * Loops until the string is a one-character character in the range
	 * a-z or A-Z.
	 * @return the single-letter string converted to upper-case.
	 */
	private String inputLetter()
	{
		while (true) {
			System.out.print("Next letter? ");
			String letter = in.next().toUpperCase(Locale.ROOT);
			char letterChar = letter.charAt(0);
			if (letterChar < 'A' || 'Z' < letterChar) System.out.println("Invalid input!");
			else return letter;
		}
	}

	/**
	 * Helper method for getPartitionList:
	 * Uses word and letter to create a pattern.  The pattern string
	 * has the same number of letter as word.  If a character in
	 * word is the same as letter, the pattern is letter; Otherwise
	 * it's "-".
	 * @param word the word used to create the pattern
	 * @param letter the letter used to create the pattern
	 * @return the pattern
	 */
	private String getPattern(String word, String letter)
	{
		char[] charArray = word.toCharArray();
		StringBuilder pattern = new StringBuilder();
		for (char wordLetter: charArray) pattern.append(wordLetter == letter.charAt(0) ? letter : '-');
		return pattern.toString();
	}

	/**
	 * Helper method for playGame:
	 * Partitions each string in wordList based on their patterns.
	 * @param letter the letter used to find the pattern for
	 *        each word in wordList.
	 * @return the list of partitions.
	 */
	private List<Partition> getPartitionList(String letter)
	{
		List partitionList = new ArrayList<Partition>();
		for (String word: wordList) {
			String pattern = getPattern(word, letter);
			boolean added = false;
			for (Object each: partitionList) if (((Partition) each).addIfMatches(pattern, word)) added = true;
			if (!added) partitionList.add(new Partition(pattern, word));
		}
		return partitionList;
	}

	/**
	 * Helper method for playGame:
	 * Removes all but the largest (most words) partitions from partitions.
	 * @param partitions the list of partitions.
	 *        Precondition: partitions.size() > 0
	 * Postcondition: partitions
	 * 1) contains all of the partitions with the most words.
	 * 2) does not contain any of the partitions with fewer than the most words.
	 */
	private void removeSmallerPartitions(List<Partition> partitions)
	{
		int maxPartitions = 1;
		for (Partition partition: partitions) {
			int partitionSize = partition.getWords().size();
			if (maxPartitions < partitionSize) maxPartitions = partitionSize;
		}
		for (int i = partitions.size() - 1; i >= 0; i--) if (partitions.get(i).getWords().size() < maxPartitions) partitions.remove(i);
	}

	/**
	 * Helper method for playGame:
	 * Returns the words from one of the optimal partitions,
	 *    that is a partition with the most dashes in its pattern.
	 * @param partitions the list of partitions.
	 * @return the optimal partition.
	 */
	private List<String> getWordsFromOptimalPartition(List<Partition> partitions)
	{
		int maxDashes = 1;
		Partition maxDashesPartition = partitions.get(0);
		for (Partition partition: partitions) {
			int partitionDashes = partition.getPatternDashCount();
			if (maxDashes < partitionDashes) {
				maxDashes = partitionDashes;
				maxDashesPartition = partition;
			}
		}
		return maxDashesPartition.getWords();
	}

	/**
	 * Helper method for playGame:
	 * Creates a new string for solution.  If the ith letter of
	 * found equals letter, then the ith letter of solution is
	 * changed to letter; Otherwise it is unchanged.
	 * @param found the string to check for occurances of letter.
	 * @param letter the letter to check for in found.
	 */
	private void substitute(String found, String letter)
	{
		for (int i = 0; i < found.length(); i++) if (found.substring(i, i + 1).equals(letter)) solution = (
				solution.substring(0, i) +
				letter +
				solution.substring(i + 1)
		);
	}
}
