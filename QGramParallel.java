import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.io.*;

public class QGramParallel {
	static Semaphore semIndex, semKey, semDir;
	static int sharedPosPointer, nThreads = 2;
	static String testString, sharedDirPointer, sharedKey;
	static HashMap<String, Integer> qGramTable;
	static Iterator sharedMapIterator;
	static int[] posTable;

	public static void loadFile() throws Exception{
		File file = new File("data/Ndna_1M.txt");
		BufferedReader br = new BufferedReader(new FileReader(file));

		String st;
		while((st = br.readLine()) != null)
			testString = st;
		//testString = "ACAGGGCA";
	}

	public static HashMap<String, Integer> getQGramSubstrings(int q) {
		HashMap<String, Integer> qGrams = new HashMap<String, Integer>();
		String[] letters = {"A", "C", "G", "T"};

		for(int i = 0; i < 4; i++){
			String temp = "";
			temp += letters[i];
			for(int j = 0; j < 4; j++){
				temp += letters[j];
				for (int k=0;k<4;k++) {
					temp += letters[k];
					qGrams.put(temp, 0);
					//System.out.println("Temp = " + temp);
					temp = temp.substring(0, 2);
				}
				temp = temp.substring(0,1);
			}
		}
		return qGrams;
	}

	static class Indexer extends Thread {
		private Semaphore semMap, semKey, semDir;
		private String testString, threadName;
		private String testKey;
		public Indexer(Semaphore semMap, Semaphore semKey, Semaphore semDir, String testString, String threadName) {
			super(threadName);
			this.semMap = semMap;
			this.semKey = semKey;
			this.semDir = semDir;
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

				try {
					boolean newPosIndex = true;
					//System.out.println(threadName + " is waiting for a permit for directory."); 
					semDir.acquire(); // acquire the lock
					//System.out.println(threadName + " gets a permit for dir.");

					for(int i=0;i<testString.length()-2;i++) {
						if(newPosIndex) {
							qGramTable.put(testKey, sharedPosPointer);
							newPosIndex = false;
						}

						if(testString.substring(i, i+3).equals(testKey)) {
							try {
								//System.out.println(threadName + " is waiting for a permit for index."); 
								semMap.acquire(); // acquire the lock
								//System.out.println(threadName + " gets a permit for index.");

								posTable[sharedPosPointer] = i;
								if (sharedPosPointer < testString.length()-2)
									sharedPosPointer += 1; // updated position table pointer
							} catch (InterruptedException exc) { 
		                    	System.out.println(exc); 
		                	}
							//System.out.println(threadName + " releases the permit for index.");
							semMap.release();
						}
					}

				} catch (InterruptedException exc) { 
                    System.out.println(exc); 
                }
                //System.out.println(threadName + " releases the permit for dir.");
				semDir.release();
			}
		}
	}

	public static void main(String[] args) {
		try {
			loadFile();
		} catch(Exception e){}
	
		semIndex = new Semaphore(1, true);
		semKey = new Semaphore(1, true);
		semDir = new Semaphore(1, true);
		sharedPosPointer = 0;
		qGramTable = getQGramSubstrings(2);
		sharedMapIterator = qGramTable.keySet().iterator();
		posTable = new int[testString.length()-1];
		for(int i=0;i<posTable.length;i++)
			posTable[i] = 0;

		Indexer indexer1 = new Indexer(semIndex, semKey, semDir, testString, "A");
		Indexer indexer2 = new Indexer(semIndex, semKey, semDir, testString, "B");
		Indexer indexer3 = new Indexer(semIndex, semKey, semDir, testString, "C");
		Indexer indexer4 = new Indexer(semIndex, semKey, semDir, testString, "D");
		Indexer indexer5 = new Indexer(semIndex, semKey, semDir, testString, "E");
		Indexer indexer6 = new Indexer(semIndex, semKey, semDir, testString, "F");

		final long startTime = System.currentTimeMillis();
		try {
				indexer1.start();
				indexer2.start();
				indexer3.start();
				indexer4.start();
				indexer5.start();
				indexer6.start();

				indexer1.join();
				indexer2.join();
				indexer3.join();
				indexer4.join();
				indexer5.join();
				indexer6.join();
		} catch(Exception e){}
		final long endTime = System.currentTimeMillis();

		System.out.println("Time Elapsed: " + (endTime - startTime));
		//System.out.println("Test String = " + testString);
		for (String key : qGramTable.keySet())
			System.out.println(key + " - " + qGramTable.get(key));

		//for (int i = 0; i < posTable.length; i++)
			//System.out.print(posTable[i] + " ");
	}
}