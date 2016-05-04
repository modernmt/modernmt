/*
 * Copyright (C) 2015 Universitat d'Alacant
 *
 * author: Miquel Esplà Gomis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package eu.modernmt.aligner.symal;

//import es.ua.dlsi.utils.CmdLineParser;

import eu.modernmt.aligner.fastalign.FastAlign;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author miquel
 */
public class Symmetrisation {
    private static final Logger logger = LogManager.getLogger(eu.modernmt.aligner.fastalign.SymmetrizedAligner.class);

    public enum Strategy {
        Intersection,
        Union,
        GrowDiagFinalAnd
    }

    /**
     * Symmetrisation method which produces, as a result, the intersection of the
     * S2T and T2S alignments.
     *
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @param t2s Target to source asymmetric alignment produced with GIZA++
     * @return
     */
    static public boolean[][] IntersectionSymal(Set<Integer>[] s2t, Set<Integer>[] t2s) {
        boolean[][] intersection = new boolean[s2t.length][t2s.length];
        //Initialisation of the alignment matrix to "all the words are unaligned"
        for (boolean[] row : intersection)
            Arrays.fill(row, false);
        //For all the alignments in S2T
        for (int s_word = 0; s_word < s2t.length; s_word++) {
            if (s2t[s_word] != null) {
                for (Integer t_word : s2t[s_word]) {
                    if (t2s[t_word] != null) {
                        //If the alignment appears in both the asymmetric alignments, it is added to the symmetrised alignment
                        if (t2s[t_word].contains(s_word))
                            intersection[s_word][t_word] = true;
                    }
                }
            }
        }
        return intersection;
    }

    /**
     * Symetrisation method which produces, as a result, the union of the
     *
     * @param s2t Source to target assymetric alignment produced with GIZA++
     * @param t2s Target to source assymetric alignment produced with GIZA++
     * @return
     */
    static public boolean[][] UnionSymal(Set<Integer>[] s2t, Set<Integer>[] t2s) {
        boolean[][] union = new boolean[s2t.length][t2s.length];
        //Initialisation of the alignment matrix to "all the words are unaligned"
        for (boolean[] row : union)
            Arrays.fill(row, false);
        //All the alignments in S2T are added to the union
        for (int s_word = 0; s_word < s2t.length; s_word++) {
            if (s2t[s_word] != null) {
                for (Integer t_word : s2t[s_word]) {
                    union[s_word][t_word] = true;
                }
            }
        }
        //All the alignments in T2S are added to the union
        for (int t_word = 0; t_word < t2s.length; t_word++) {
            if (t2s[t_word] != null) {
                for (Integer s_word : t2s[t_word]) {
                    union[s_word][t_word] = true;
                }
            }
        }
        return union;
    }


    /**
     * Symetrisation method which produces, as a result, the grow-diag-final-and symmetrised alignment
     *
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @param t2s Target to source asymmetric alignment produced with GIZA++
     * @return
     */
    static public boolean[][] GrowDiagFinalAnd(Set<Integer>[] s2t, Set<Integer>[] t2s) {
        List<Pair<Integer, Integer>> neighbors = new LinkedList<Pair<Integer, Integer>>(); //neighbors

        //Diagonal (diag) neigourhood
        neighbors.add(new Pair(-1, -1));
        neighbors.add(new Pair(-1, 1));
        neighbors.add(new Pair(1, -1));
        neighbors.add(new Pair(1, 1));

        //Defining neibourhood
        neighbors.add(new Pair(0, 1));
        neighbors.add(new Pair(-1, -0));
        neighbors.add(new Pair(0, -1));
        neighbors.add(new Pair(1, 0));

        //Intersection of the alignments (starting point); matrix format, where a row corresponds to a source, and a column column to a target
        boolean[][] currentpoints = IntersectionSymal(s2t, t2s); //symmetric alignment
        //Union of the alignments (space for growing) matrix format, where a row corresponds to a source, and a column column to a target

        boolean[][] unionalignment = UnionSymal(s2t, t2s); //union alignment

        //points which correspond to the difference between the union points and intersection points
        List<Pair<Integer, Integer>> newpoints = new LinkedList<Pair<Integer, Integer>>();
        for (int union_row = 0; union_row < unionalignment.length; union_row++) {
            boolean aligned = false;
            for (int union_col = 0; union_col < unionalignment[union_row].length; union_col++) {
                if (unionalignment[union_row][union_col] && !currentpoints[union_row][union_col]) {
                    newpoints.add(new Pair(union_row, union_col));
                }
            }
        }

        //Adding currently unaligned words in SL to the list
        Set<Integer> unaligned_s = new LinkedHashSet<Integer>();
        for (int current_row = 0; current_row < currentpoints.length; current_row++) {
            boolean aligned = false;
            for (int current_col = 0; current_col < currentpoints[current_row].length; current_col++) {
                if (currentpoints[current_row][current_col]) {
                    aligned = true;
                    break;
                }
            }
            if (!aligned)
                unaligned_s.add(current_row);
        }
        //Adding currently unaligned words in TL to the list
        Set<Integer> unaligned_t = new LinkedHashSet<Integer>();
        for (int current_col = 0; current_col < currentpoints[0].length; current_col++) {
            boolean aligned = false;
            for (boolean[] currentpoint : currentpoints) {
                if (currentpoint[current_col]) {
                    aligned = true;
                    break;
                }
            }
            if (!aligned)
                unaligned_t.add(current_col);
        }

        boolean keep_going = (newpoints.size() > 0);
        //Grow-diag
        while (keep_going) {

            //points which are added in the current iteration
            List<Pair<Integer, Integer>> added = new LinkedList<Pair<Integer, Integer>>();

            keep_going = false;
            //loop over new points
            for (Pair<Integer, Integer> np : newpoints) {
                int p1 = np.getFirst();
                int p2 = np.getSecond();
                boolean toInsert = true;
                //check whether the row or the column is already aligned
                if (!unaligned_s.contains(p1) && !unaligned_t.contains(p2)) { //both row and column are already aligned; the point cannot be inserted
                    toInsert = false;
                } else { //either row or columns are unaligned
                    //check whether any neighbor is unaligned; if true the point can be inserted
                    toInsert = false;
                    for (Pair<Integer, Integer> nb : neighbors) {
                        int p1_nb = p1 + nb.getFirst();
                        if ((p1_nb < 0) || (p1_nb >= s2t.length)) { //p1_nb is out of boundary
                            continue;
                        }
                        int p2_nb = p2 + nb.getSecond();
                        if ((p2_nb < 0) || (p2_nb >= t2s.length)) { //p1_nb is out of boundary
                            continue;
                        }
                        if (currentpoints[p1_nb][p2_nb]) {
                            toInsert = true;
                            continue; //exit the loop over the neighbors
                        }
                    }
                }
                //if the new point (np) can be inserted (toInsert==true)
                //add it to the currentpoints and to the list of added points
                //set the keep_going flag to true in order to continue the do-while loop
                if (toInsert) {
                    //currentpoints[p1][p2] = true;
                    added.add(np);
                    keep_going = true;
                }
            }

            //remove all added points from newpoints List, from the unaligned_s and unaligned_t Lisy
            for (Pair<Integer, Integer> p : added) {
                int p1 = p.getFirst();
                int p2 = p.getSecond();
                currentpoints[p1][p2] = true;
                newpoints.remove(p);
                unaligned_s.remove(p1);
                unaligned_t.remove(p2);
            }
        }

//Final-and
        //points which are added in the current iteration
        List<Pair<Integer, Integer>> added = new LinkedList<Pair<Integer, Integer>>();
        for (Pair<Integer, Integer> np : newpoints) {
            int p1 = np.getFirst();
            int p2 = np.getSecond();
            if (unaligned_s.contains(p1) && unaligned_t.contains(p2)) {
                added.add(np);
            }
        }

        //remove all added points from newpoints List, from the unaligned_s and unaligned_t Lisy
        for (Pair<Integer, Integer> p : added) {
            int p1 = p.getFirst();
            int p2 = p.getSecond();
            currentpoints[p1][p2] = true;
            newpoints.remove(p);
            unaligned_s.remove(p1);
            unaligned_t.remove(p2);
        }
        /*
        //Final-and  for direct alignments
        //insert all points of the direct alignment which are not already contained in the currentpoints
        for (int s_word = 0; s_word < s2t.length; s_word++) {
            if (s2t[s_word] != null) {
                for (Integer t_word : s2t[s_word]) {
                    if (unaligned_s.contains(s_word) && unaligned_t.contains(t_word)) {
                        //if both row and the column are unaligned, the point can be inserted
                        currentpoints[s_word][t_word] = true;
                        unaligned_s.remove(s_word);
                        unaligned_t.remove(t_word);
                    }
                }
            }
        }

        //Final-and  for inverse alignments
        //insert all points of the direct alignment which are not already contained in the currentpoints
        for (int t_word = 0; t_word < t2s.length; t_word++) {
            if (t2s[t_word] != null) {
                for (Integer s_word : t2s[t_word]) {
                    if (unaligned_s.contains(s_word) && unaligned_t.contains(t_word)) {
                        //if both row and the column are unaligned, the point can be inserted
                        currentpoints[s_word][t_word] = true;
                        unaligned_s.remove(s_word);
                        unaligned_t.remove(t_word);
                    }
                }
            }
        }*/

        return currentpoints;
    }


    /**
     * Method that extracts an alignment representation from an assymetric alignment in GIZA++ format
     *
     * @param ssentence
     * @param aligninfo
     * @return
     */
    static public Set<Integer>[] ReadGizaAsymmetricAlignment(String ssentence, String aligninfo) {
        String[] step1 = aligninfo.substring(0, aligninfo.length() - 3).split(" \\}\\) ");
        Set[] alignment = new Set[step1.length];
        for (int i = 1; i < step1.length; i++) {
            alignment[i - 1] = null;
            if (step1[i].lastIndexOf('{') != step1[i].length() - 1) {
                String[] pair = step1[i].split(" \\(\\{ ");
                if (pair.length > 1) {
                    String[] indexes = pair[1].split(" ");
                    for (String index : indexes) {
                        if (alignment[i - 1] == null) {
                            alignment[i - 1] = new HashSet<Integer>();
                        }
                        alignment[i - 1].add(Integer.parseInt(index) - 1);
                    }
                }
            }
        }
        return alignment;
    }

    /**
     * Method that extracts an alignment representation from an symmetric alignment in Moses format
     *
     * @param symalignment
     * @return
     */

    static public Set<Integer>[] ReadGizaSymmetricAlignment(int[][] symalignment, int highest_index) {
        Set[] alignment = new Set[highest_index];
        Arrays.fill(alignment, null);

        for (int[] alg : symalignment) {
            int w1 = alg[0];
            int w2 = alg[1];
            if (alignment[w1] == null)
                alignment[w1] = new HashSet();
            alignment[w1].add(w2);
        }
        return alignment;
    }


    /**
     * Method that extracts an alignment representation from any kind of alignemnt in the format "src-trg src-trg src-trg ..."
     * and stores in a vector (with index src) of Sets containing all trg associated with src
     *
     * @param from_alignment
     * @param highest_index
     * @return
     */
    static public Set<Integer>[] ReadForwardAlignment(int[][] from_alignment, int highest_index) {
        Set[] alignment = new Set[highest_index];
        Arrays.fill(alignment, null);

        for (int[] alg : from_alignment) {
            int w1 = alg[0]; //index of the vector is src
            int w2 = alg[1];
            if (alignment[w1] == null)
                alignment[w1] = new HashSet();
            alignment[w1].add(w2);
        }
        return alignment;
    }
    /**
     * Method that extracts an alignment representation from anu kind of alignemnt in the format "src-trg src-trg src-trg ..."
     * and stores in a vector (with index trg) of Sets containing all src associated with trg
     *
     * @param from_alignment
     * @param highest_index
     * @return
     */
    static public Set<Integer>[] ReadBackwardAlignment(int[][] from_alignment, int highest_index) {
        Set[] alignment = new Set[highest_index];
        Arrays.fill(alignment, null);

        for (int[] alg : from_alignment) {
            int w1 = alg[1]; //index of the vector is trg
            int w2 = alg[0];
            if (alignment[w1] == null)
                alignment[w1] = new HashSet();
            alignment[w1].add(w2);
        }
        return alignment;
    }


    static public void printWordAlignment(String s1, String s2, String al) {

        String[] s1Tok = s1.split(" ");

        String[] s2Tok = s2.split(" ");

        String[] toks = al.split(" ");

        if (!al.isEmpty())
            for (String t : toks) {
                String[] val = t.split("-");
                //System.out.println(t);
                System.out.println(s1Tok[Integer.valueOf(val[0])] + " -- " + s2Tok[Integer.valueOf(val[1])]);

            }

    }


    static public void printAlignment(boolean[][] al) {

        StringBuilder out = new StringBuilder();

        //Printing in the Moses symmetrised alignment format
        for (int row = 0; row < al.length; row++) {
            for (int col = 0; col < al[row].length; col++) {
                if (al[row][col]) {
                    out.append(row);
                    out.append("-");
                    out.append(col);
                    out.append(" ");
                }
            }
        }

        System.out.println(out.toString());
    }


    static public int[][] symmetriseGizaFormatAlignment(String sl_sent_s2t, String alg_info_s2t, String sl_sent_t2s, String alg_info_t2s, int symindex) {

        Set<Integer>[] s2talignment = ReadGizaAsymmetricAlignment(sl_sent_s2t, alg_info_s2t);
        Set<Integer>[] t2salignment = ReadGizaAsymmetricAlignment(sl_sent_t2s, alg_info_t2s);

        boolean[][] al = null;

        //Producing the symmetrised alignment
        switch (symindex) {
            case 1:
                al = Symmetrisation.UnionSymal(s2talignment, t2salignment);
                break;
            case 2:
                al = Symmetrisation.IntersectionSymal(s2talignment, t2salignment);
                break;
            case 3:
                al = Symmetrisation.GrowDiagFinalAnd(s2talignment, t2salignment);
                break;
        }

        return returnAlignment(al);

    }


    static public int[][] symmetriseMosesFormatAlignment(int[][] sl, int[][] tl, Strategy strategy) {

        Set<Integer>[] s2talignment = null;
        Set<Integer>[] t2salignment = null;

        SortedSet<Integer> sl_index_set = new TreeSet<Integer>();
        SortedSet<Integer> tl_index_set = new TreeSet<Integer>();
        for (int[] alignment : sl) {
            sl_index_set.add(alignment[0]);
            tl_index_set.add(alignment[1]);
        }
        for (int[] alignment : tl) {
            sl_index_set.add(alignment[1]);
            tl_index_set.add(alignment[0]);
        }
        s2talignment = ReadGizaSymmetricAlignment(sl, sl_index_set.last() + 1);
        t2salignment = ReadGizaSymmetricAlignment(tl, tl_index_set.last() + 1);

        boolean[][] al = null;

        //Producing the symmetrised alignment
        switch (strategy) {
            case Union:
                al = Symmetrisation.UnionSymal(s2talignment, t2salignment);
                break;
            case Intersection:
                al = Symmetrisation.IntersectionSymal(s2talignment, t2salignment);
                break;
            case GrowDiagFinalAnd:
                al = Symmetrisation.GrowDiagFinalAnd(s2talignment, t2salignment);
                break;
        }

        return returnAlignment(al);
    }

    static public int[][] symmetriseAlignment(int[][] sl, int[][] tl, Strategy strategy) {

        Set<Integer>[] s2talignment = null;
        Set<Integer>[] t2salignment = null;

        SortedSet<Integer> sl_index_set = new TreeSet<Integer>();
        SortedSet<Integer> tl_index_set = new TreeSet<Integer>();
        for (int[] alignment : sl) {
            sl_index_set.add(alignment[0]);
            tl_index_set.add(alignment[1]);
        }
        for (int[] alignment : tl) {
            sl_index_set.add(alignment[0]);
            tl_index_set.add(alignment[1]);
        }
        s2talignment = ReadForwardAlignment(sl, sl_index_set.last() + 1);
        t2salignment = ReadBackwardAlignment(tl, tl_index_set.last() + 1);

        boolean[][] al = null;

        //Producing the symmetrised alignment
        switch (strategy) {
            case Union:
                al = Symmetrisation.UnionSymal(s2talignment, t2salignment);
                break;
            case Intersection:
                al = Symmetrisation.IntersectionSymal(s2talignment, t2salignment);
                break;
            case GrowDiagFinalAnd:
                al = Symmetrisation.GrowDiagFinalAnd(s2talignment, t2salignment);
                break;
        }
        return returnAlignment(al);
    }


    static private int[][] returnAlignment(boolean[][] al) {
        ArrayList<int[]> alignments = new ArrayList<>(al.length);

        for (int row = 0; row < al.length; row++) {
            for (int col = 0; col < al[row].length; col++) {
                if (al[row][col]) {
                    alignments.add(new int[]{row, col});
                }
            }
        }
        int[][] result = new int[alignments.size()][2];
        alignments.toArray(result);
        return result;
    }

    public static void main(String[] args){
        /*
        int[][] forward = new int[][]{{0, 0},{1, 1},{2, 2},{3, 3},{4, 4},{5, 5},{6, 6},{5, 7},{10, 9},{5, 10}};
        int[][] backward = new int[][]{{0, 0},{1, 1},{2, 2},{3, 3},{4, 4},{5, 5},{6, 6},{7, 6},{8, 0},{9, 8},{10, 9},{11, 5},{12, 11}};
        */
        int[][] forward = new int[][]{{0, 0},{1, 1},{2, 4},{3, 2},{4, 5},{4, 6},{5, 7},{6, 8},{8, 10},{9, 11},{10,11}, {11,9}};
        int[][] backward = new int[][]{{0, 0},{1, 1},{2, 4},{3, 2},{4, 4},{5,7},{6, 7},{7, 8},{8, 10},{9, 11},{10,11}, {11,9}};

        String slAlignment= "";
        for (int[] alg : forward) { slAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("forward: " + slAlignment);
        System.out.print("forward: " + slAlignment + "\n");

        String tlAlignment= "";
        for (int[] alg : backward) { tlAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("backward: " + tlAlignment);
        System.out.print("backward: " + tlAlignment + "\n");


        int[][] symmetrized = symmetriseAlignment(forward, backward, Strategy.GrowDiagFinalAnd);

        String symmAlignment= "";
        for (int[] alg : symmetrized) { symmAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("symmetrized: " + symmAlignment + "\n");
        System.out.print("symmetrized: " + symmAlignment + "\n");
    }

}