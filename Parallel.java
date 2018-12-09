/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lcs;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 *
 * @author user
 */
public class Parallel {

    public static void printTable(
            int[][] table,
            LinkedHashMap<Integer, Character> rowHeaders,
            LinkedHashMap<Integer, Character> columnHeaders,
            String s,
            String t) {
        System.out.print(" ");

        Iterator iterator = columnHeaders.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();

            System.out.print(pair.getValue());

            iterator.remove();
        }

        System.out.println();

        for (int rowIndex = 0; rowIndex < s.length() + 1; rowIndex++) {
            System.out.print(rowHeaders.get(rowIndex));

            for (int columnIndex = 0;
                    columnIndex < t.length() + 1;
                    columnIndex++) {
                System.out.print(table[rowIndex][columnIndex]);
            }

            System.out.println();
        }
    }

    public static int lcs(String s, String t) {
        // So the table will always be longer than it is taller, always make t
        // the longer string
        if (t.length() < s.length()) {
            String temp = s;
            s = t;
            t = temp;
        }

        // Set the table (and its headers) up
        int[][] table = new int[s.length() + 1][t.length() + 1];

        LinkedHashMap<Integer, Character> rowHeaders = new LinkedHashMap<>();
        LinkedHashMap<Integer, Character> columnHeaders = new LinkedHashMap<>();

        for (int rowIndex = 0; rowIndex < s.length() + 1; rowIndex++) {
            rowHeaders.put(
                    rowIndex,
                    (rowIndex == 0) ? '-' : s.charAt(rowIndex - 1)
            );

            for (int columnIndex = 0;
                    columnIndex < t.length() + 1;
                    columnIndex++) {
                columnHeaders.put(
                        columnIndex,
                        (columnIndex == 0) ? '-' : t.charAt(columnIndex - 1)
                );

                table[rowIndex][columnIndex] = 0;
            }
        }

        // Commence the dynamic programming stage
        // Prepare multithreading variables
        // Anti-prematurely-evaluate-the-diagonal semaphore
        Semaphore semaphore = new Semaphore(0);

        // A counter shared across all threads
        SharedCounter counter = new SharedCounter(0);

        // A hand-made thread pool made specifically for a parallel
        // implementation of LCS
        CellThreadPool cellThreadPool
                = new CellThreadPool(
                        Runtime.getRuntime().availableProcessors(),
                        semaphore,
                        counter
                );

        // Upper triangle
        for (int columnIndex = 1; columnIndex < t.length() + 1; columnIndex++) {
            int rowIndex = 1;
            int diagonalColumnIndex = columnIndex;

            // Perform a diagonal traversal of the evaluations to avoid
            // dependencies and race conditions
            while (rowIndex < s.length() + 1 && diagonalColumnIndex >= 1) {
                // Evaluate this cell
                cellThreadPool.put(
                        new Cell(table,
                                rowHeaders,
                                columnHeaders,
                                rowIndex,
                                diagonalColumnIndex),
                        counter
                );

                rowIndex += 1;
                diagonalColumnIndex -= 1;
            }

            while (true) {
                try {
                    // Wait until all diagonal evaluations have finished
                    semaphore.acquire();

                    break;
                } catch (InterruptedException ex) {
                }
            }
        }

        // Lower triangle
        for (int rowIndex = 2; rowIndex < s.length() + 1; rowIndex++) {
            int diagonalRowIndex = rowIndex;
            int columnIndex = t.length();

            // Perform a diagonal traversal of the evaluations to avoid
            // dependencies and race conditions
            while (diagonalRowIndex < s.length() + 1) {
                // Evaluate this cell
                cellThreadPool.put(
                        new Cell(table,
                                rowHeaders,
                                columnHeaders,
                                diagonalRowIndex,
                                columnIndex),
                        counter
                );

                diagonalRowIndex += 1;
                columnIndex -= 1;
            }

            while (true) {
                try {
                    // Wait until all diagonal evaluations have finished
                    semaphore.acquire();

                    break;
                } catch (InterruptedException ex) {
                }
            }
        }

        // Check the values of the table
        printTable(table, rowHeaders, columnHeaders, s, t);

        return table[s.length()][t.length()];
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt the user for the strings
        System.out.print("s: ");
        String s = scanner.nextLine();

        System.out.print("t: ");
        String t = scanner.nextLine();

        // Compute the length of the longest common subsequence of these strings
        System.out.println("Length of LCS: " + lcs(s, t));
    }
}
