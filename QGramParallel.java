import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.io.*;

public class QGramParallel {
	static Semaphore semIndex, semKey;
	static int sharedPosPointer, nThreads = 2;
	static String testString, sharedDirPointer, sharedKey;
	static HashMap<String, Integer> qGramTable;
	static Iterator sharedMapIterator;
	static int[] posTable;

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

	static class Indexer extends Thread {
		private Semaphore semMap, semKey;
		private String testString, threadName;
		private String testKey;
		public Indexer(Semaphore semMap, Semaphore semKey, String testString, String threadName) {
			super(threadName);
			this.semMap = semMap;
			this.semKey = semKey;
			this.testString = testString;
			this.threadName = threadName;
		}

		public void run() {
			// Get New Key
			while(sharedMapIterator.hasNext()) {
				try {
					//System.out.println(threadName + " is waiting for a permit on keys."); 
					semKey.acquire();
					//System.out.println(threadName + " gets a permit on keys."); 

					testKey = (String)sharedMapIterator.next(); // Thread gets next key
				} catch (InterruptedException exc) { 
                    System.out.println(exc); 
                } 
				//System.out.println(threadName + " releases the permit for keys.");
				semKey.release(); 
				//System.out.println("Thread Name: " + threadName + " Key - " + testKey);

				// Perform Q-Gram Index
				for(int i=0;i<testString.length()-2;i++) {
					boolean newPosIndex = true;
					if(testString.substring(i, i+2).equals(testKey)) {
						try {
							System.out.println(threadName + " is waiting for a permit for index."); 
							semMap.acquire(); // acquire the lock
							System.out.println(threadName + " gets a permit for index.");

							sharedPosPointer += 1; // updated position table pointer
							posTable[sharedPosPointer] = i;
							if (newPosIndex) {
								qGramTable.put(testKey, sharedPosPointer);
								newPosIndex = false;
							}
						} catch (InterruptedException exc) { 
	                    	System.out.println(exc); 
	                	}
						System.out.println(threadName + " releases the permit for index.");
						semMap.release();
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			loadFile();
		} catch(Exception e){}
	
		semIndex = new Semaphore(1, true);
		semKey = new Semaphore(1, true);
		sharedPosPointer = 0;
		qGramTable = getQGramSubstrings(2);
		sharedMapIterator = qGramTable.keySet().iterator();
		posTable = new int[testString.length()-1];
		for(int i=0;i<posTable.length;i++)
			posTable[i] = 0;

		Indexer indexer1 = new Indexer(semIndex, semKey, testString, "A");
		Indexer indexer2 = new Indexer(semIndex, semKey, testString, "B");
		Indexer indexer3 = new Indexer(semIndex, semKey, testString, "C");
		Indexer indexer4 = new Indexer(semIndex, semKey, testString, "D");

		final long startTime = System.currentTimeMillis();
		try {
				indexer1.start();
				indexer2.start();
				indexer3.start();
				indexer4.start();

				indexer1.join();
				indexer2.join();
				indexer3.join();
				indexer4.join();
		} catch(Exception e){}
		final long endTime = System.currentTimeMillis();

		System.out.println("Time Elapsed: " + (endTime - startTime));
		System.out.println("Test String = " + testString);
		for (String key : qGramTable.keySet())
			System.out.println(key + " - " + qGramTable.get(key));

		for (int i = 0; i < posTable.length; i++)
			System.out.print(posTable[i] + " ");
	}
}