import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.io.*;

public class QGramParallel{
	static Semaphore semaphore, sTasks;
	// static String testString = "GGGGGGAGCT";
	static String testString;
	static int nTasks = 2;
	static HashMap<String, Integer> qGramTable;

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
				// System.out.println("Temp = " + temp);
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
			for(int i = 0; i < genes.length() - 2; i++){	// Traverse through the string
				// Get every pair of string na magkatabi (I forgot the word)
				// Compare it with the current key
				if (genes.substring(i, i+2).equals(key)){
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

	static class Indexer extends Thread {
		private int num;
		private String sub;
		public Indexer(int num, String sub){
			this.num = num;
			this.sub = sub;
		}

		public void run(){
			// Generate position table
			int[] posTable = new int[sub.length()-1];
			for (int i = 0; i < posTable.length; i++)
				posTable[i] = 0;
			HashMap<String, Integer> qgTable = getQGramSubstrings(2);
			qGramIndex(qgTable, posTable, sub);

			try{
				semaphore.acquire(1);
				printTables(posTable, qgTable);
				semaphore.release(1);
				sTasks.release(1);
			} catch(InterruptedException e){}
		}

		public void printTables(int[] posTable, HashMap<String, Integer> qgTable){
			for(String key : qGramTable.keySet())
				System.out.println("[" + num + "]" + key + " - " + qgTable.get(key));

			for(int i = 0; i < posTable.length; i++)
				System.out.print("t" + num + ":" + posTable[i] + " ");

			System.out.println("[" + sub + "]");
		}
	}

	public static void main(String[] args) {
		try{
			loadFile();
		} catch(Exception e){}

		semaphore = new Semaphore(0, true);
		sTasks = new Semaphore(0, true);
		// Generate q-grams and directory table
		qGramTable = getQGramSubstrings(2);
		int stringAmount = testString.length() / nTasks;
		int startIndex = 0;
		int endIndex = stringAmount;

		final long startTime = System.currentTimeMillis();
		semaphore.release(1);
		for(int i = 0; i < nTasks; i++){
			Indexer indexer = new Indexer(i, testString.substring(startIndex, endIndex));
			indexer.start();

			startIndex = endIndex;
			if(i + 1 == nTasks - 1)
				endIndex = testString.length();
			else endIndex += stringAmount;
		}

		try{
			sTasks.acquire(nTasks);
			final long endTime = System.currentTimeMillis();
			System.out.println(startTime);
			System.out.println(endTime - startTime);
		} catch(InterruptedException e){}
	}
}