import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

public class QGramIndex {
	static String testString;

	public static void loadFile() throws Exception{
		File file = new File("data/Ndna_100.txt");
		BufferedReader br = new BufferedReader(new FileReader(file));

		String st;
		while((st = br.readLine()) != null)
			testString = st;
	}

	public static HashMap<String, Integer> getQGramSubstrings(int q) {
		HashMap<String, Integer> qGrams = new HashMap<String, Integer>();
		String[] letters = {"A", "C", "G", "T"};

		for(int i = 0; i < 4; i++){
			String temp = "";
			temp += letters[i];
			for(int j = 0; j < 4; j++){
				temp += letters[j];
				//System.out.println("Temp = " + temp);
				qGrams.put(temp, 0);
				temp = temp.substring(0, 1);
			}
		}

		return qGrams;
	}

	public static void qGramIndex(HashMap<String, Integer> dirTable, int[] posTable, String genes) {
		int posTablePointer = 0;
		// Traverse to each qGram
		for (String key : dirTable.keySet()) {
			boolean newPosIndex = true;
			for(int i = 0; i < genes.length() - 2; i++) {	// Traverse through the string
				// Get every pair of string na magkatabi (I forgot the word)
				// Compare it with the current key
				if (genes.substring(i, i+2).equals(key)) {
					posTablePointer++;	// If yes, update pointer
					posTable[posTablePointer] = i;	// Ilagay sa pointerTable yung index kung saan yung qGram
					if (newPosIndex) {	// You should only update directory table once = kung saan yung start ng portion niya sa pointer table
						dirTable.put(key, posTablePointer); // Update directory table
						newPosIndex = false; // prevent from going in again
					}
				} 
			}
		}
	}

	public static void main(String[] args) {
		try{
			loadFile();
		} catch(Exception e){}

		// Generate q-grams and directory table
		HashMap<String, Integer> qGramTable = getQGramSubstrings(2);

		// Get test string
		// String testString = "ACAGGGCA";

		// Generate position table
		int[] posTable = new int[testString.length()-1];
		for (int i = 0; i < posTable.length; i++)
			posTable[i] = 0;

		final long startTime = System.currentTimeMillis();
		qGramIndex(qGramTable, posTable, testString);
		final long endTime = System.currentTimeMillis();
		System.out.println("Time Elapsed: " + (endTime - startTime));

		System.out.println("Test String = " + testString);
		for (String key : qGramTable.keySet())
			System.out.println(key + " - " + qGramTable.get(key));

		for (int i = 0; i < posTable.length; i++)
			System.out.print(posTable[i] + " ");
	}
}