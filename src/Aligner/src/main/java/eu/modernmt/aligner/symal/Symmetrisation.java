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
        GrowDiagFinalAnd,
        GrowDiagFinalAnd_ORIGINAL,
        GrowDiag
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
     * Symmetrisation method which produces, as a result, the union of the
     *
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @param t2s Target to source asymmetric alignment produced with GIZA++
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

    static public boolean[][] GrowDiagFinalAnd_ORIGINAL(Set<Integer>[] s2t, Set<Integer>[] t2s) {
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

        //Intersection of the alignments (starting point)
        boolean[][] currentpoints = IntersectionSymal(s2t, t2s); //symmetric alignment
        //Union of the alignments (space for growing)
        boolean[][] unionalignment = UnionSymal(s2t, t2s); //union alignment
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

        boolean added;
        //Grow-diag
        do {
            added = false;
            //For all the current alignment
            for (int current_row = 0; current_row < currentpoints.length; current_row++) {
                for (int current_col = 0; current_col < currentpoints[current_row].length; current_col++) {
                    //If the word is aligned, the neibourghs are checked
                    if (currentpoints[current_row][current_col]) {
                        for (Pair<Integer, Integer> n : neighbors) {
                            int p1 = current_row + n.getFirst();
                            int p2 = current_col + n.getSecond();
                            if (p1 >= 0 && p1 < currentpoints.length && p2 >= 0 && p2 < currentpoints[0].length) {
                                //Check the neighbours
                                if ((unaligned_s.contains(p1) || unaligned_t.contains(p2)) &&
                                        unionalignment[p1][p2]) {
                                    currentpoints[p1][p2] = true;
                                    unaligned_s.remove(p1);
                                    unaligned_t.remove(p2);
                                    added = true;
                                }
                            }
                        }
                    }
                }
            }
        } while (added);

        //Final-and
        for (int sw : unaligned_s) {
            int t_toremove = -1;
            for (int tw : unaligned_t) {
                if (unionalignment[sw][tw]) {
                    t_toremove = tw;
                    currentpoints[sw][tw] = true;
                    break;
                }
            }
            unaligned_t.remove(t_toremove);
        }

        return currentpoints;
    }

    /**
     * Symmetrisation method which produces, as a result, the final-and symmetrised alignment using the forward alignment
     *
     * @param currentpoints current aligned points
     * @param unaligned_s set of unalinged source columns
     * @param unaligned_t set of unalinged target columns
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @return
     */
    static public void FinalAndForward(boolean[][] currentpoints, Set<Integer> unaligned_s, Set<Integer> unaligned_t, Set<Integer>[] s2t) {
        for (int s_word = 0; s_word < s2t.length; s_word++) {
            if (s2t[s_word] != null) {
                for (Integer t_word : s2t[s_word]) {
                    if (!currentpoints[s_word][t_word]) {
                        if (unaligned_s.contains(s_word) && unaligned_t.contains(t_word)) {
                            currentpoints[s_word][t_word] = true;
                            unaligned_s.remove(s_word);
                            unaligned_t.remove(t_word);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Symmetrisation method which produces, as a result, the final-and symmetrised alignment using the backward alignment
     *
     * @param currentpoints current aligned points
     * @param unaligned_s set of unalinged source columns
     * @param unaligned_t set of unalinged target columns
     * @param t2s Target to source asymmetric alignment produced with GIZA++
     * @return
     */
    static public void FinalAndBackward(boolean[][] currentpoints, Set<Integer> unaligned_s, Set<Integer> unaligned_t, Set<Integer>[] t2s) {
        for (int t_word = 0; t_word < t2s.length; t_word++) {
            if (t2s[t_word] != null) {
                for (Integer s_word : t2s[t_word]) {
                    if (!currentpoints[s_word][t_word]) {
                        if (unaligned_s.contains(s_word) && unaligned_t.contains(t_word)) {
                            currentpoints[s_word][t_word] = true;
                            unaligned_s.remove(s_word);
                            unaligned_t.remove(t_word);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Symmetrisation method which produces, as a result, the grow-diag symmetrised alignment
     *
     * @param currentpoints current aligned points
     * @param unaligned_s set of unalinged source columns
     * @param unaligned_t set of unalinged target columns
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @param t2s Target to source asymmetric alignment produced with GIZA++
     * @return
     */
    static public void GrowDiag(boolean[][] currentpoints, Set<Integer> unaligned_s, Set<Integer> unaligned_t, Set<Integer>[] s2t, Set<Integer>[] t2s) {

        List<Pair<Integer, Integer>> neighbors = new LinkedList<Pair<Integer, Integer>>(); //neighbors
/*
        //Defining neighborhood on the axes and on the diagonals
        //order of visit
        //            column
        //          j-1 j j+1
        // row i-1:  5  1  6
        // row i:    2  P  3
        // row i+1:  7  4  8
        neighbors.add(new Pair(-1, 0));
        neighbors.add(new Pair(0, -1));
        neighbors.add(new Pair(0, 1));
        neighbors.add(new Pair(1, 0));
        neighbors.add(new Pair(-1, -1));
        neighbors.add(new Pair(-1, 1));
        neighbors.add(new Pair(1, -1));
        neighbors.add(new Pair(1, 1));
*/
        //Defining neighborhood on the axes and on the diagonals
        //order of visit (like in GrowDiagFinalAnd_ORIGINAL)
        //            column
        //          j-1 j j+1
        // row i-1:  1  6  2
        // row i:    7  P  5
        // row i+1:  3  8  4
        neighbors.add(new Pair(-1, -1));
        neighbors.add(new Pair(-1, 1));
        neighbors.add(new Pair(1, -1));
        neighbors.add(new Pair(1, 1));
        neighbors.add(new Pair(0, 1));
        neighbors.add(new Pair(-1, -0));
        neighbors.add(new Pair(0, -1));
        neighbors.add(new Pair(1, 0));

        boolean[][] unionalignment = UnionSymal(s2t, t2s); //union alignment

        boolean added;
        //Grow-diag
        do {
            added = false;
            //For all the current alignment
            for (int row = 0; row < currentpoints.length; row++) {
                for (int col = 0; col < currentpoints[row].length; col++) {
                    //If the word is aligned, the neibourghs are checked
                    if (currentpoints[row][col]) {
                        for (Pair<Integer, Integer> n : neighbors) {
                            int p1 = row + n.getFirst();
                            int p2 = col + n.getSecond();
                            if (p1 >= 0 && p1 < currentpoints.length && p2 >= 0 && p2 < currentpoints[0].length) {
                                //Check the neighbours
                                if ((unaligned_s.contains(p1) || unaligned_t.contains(p2)) &&
                                        unionalignment[p1][p2]) {
                                    currentpoints[p1][p2] = true;
                                    unaligned_s.remove(p1);
                                    unaligned_t.remove(p2);
                                    added = true;
                                }
                            }
                        }
                    }
                }
            }
        } while (added);
    }


    /**
     * Symmetrisation method which produces, as a result, the grow-diag-final-and symmetrised alignment
     *
     * @param currentpoints current aligned points
     * @param unaligned_s set of unalinged source columns
     * @param unaligned_t set of unalinged target columns
     * @return
     */
    static public void SetUnaligned(boolean[][] currentpoints, Set<Integer> unaligned_s, Set<Integer> unaligned_t) {

        //Adding currently unaligned words in SL to the list
        for (int row = 0; row < currentpoints.length; row++) {
            boolean aligned = false;
            for (int col = 0; col < currentpoints[row].length; col++) {
                if (currentpoints[row][col]) {
                    aligned = true;
                    break;
                }
            }
            if (!aligned)
                unaligned_s.add(row);
        }
        //Adding currently unaligned words in TL to the list
        for (int col = 0; col < currentpoints[0].length; col++) {
            boolean aligned = false;
            for (boolean[] currentpoint : currentpoints) {
                if (currentpoint[col]) {
                    aligned = true;
                    break;
                }
            }
            if (!aligned)
                unaligned_t.add(col);
        }
    }

    /**
     * Symmetrisation method which produces, as a result, the grow-diag-final-and symmetrised alignment
     *
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @param t2s Target to source asymmetric alignment produced with GIZA++
     * @return
     */
    static public boolean[][] GrowDiagFinalAnd(Set<Integer>[] s2t, Set<Integer>[] t2s) {
        //Intersection of the alignments (starting point); matrix format, where a row corresponds to a source, and a column column to a target
        boolean[][] currentpoints = IntersectionSymal(s2t, t2s); //symmetric alignment

        //Adding currently unaligned words in SL and TL to the list
        Set<Integer> unaligned_s = new LinkedHashSet<Integer>();
        Set<Integer> unaligned_t = new LinkedHashSet<Integer>();
        SetUnaligned(currentpoints, unaligned_s, unaligned_t);

        GrowDiag(currentpoints, unaligned_s, unaligned_t, s2t, t2s);
        FinalAndForward(currentpoints, unaligned_s, unaligned_t, s2t);
        FinalAndBackward(currentpoints, unaligned_s, unaligned_t, t2s);

        return currentpoints;
    }


    /**
     * Symmetrisation method which produces, as a result, the grow-diag symmetrised alignment
     *
     * @param s2t Source to target asymmetric alignment produced with GIZA++
     * @param t2s Target to source asymmetric alignment produced with GIZA++
     * @return
     */
    static public boolean[][] GrowDiag(Set<Integer>[] s2t, Set<Integer>[] t2s) {
        //Intersection of the alignments (starting point); matrix format, where a row corresponds to a source, and a column column to a target
        boolean[][] currentpoints = IntersectionSymal(s2t, t2s); //symmetric alignment

        //Adding currently unaligned words in SL and TL to the list
        Set<Integer> unaligned_s = new LinkedHashSet<Integer>();
        Set<Integer> unaligned_t = new LinkedHashSet<Integer>();
        SetUnaligned(currentpoints, unaligned_s, unaligned_t);

        GrowDiag(currentpoints, unaligned_s, unaligned_t, s2t, t2s);

        return currentpoints;
    }



    /**
     * Method that extracts an alignment representation from an asymmetric alignment in GIZA++ format
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


    static public int[][] symmetriseGizaFormatAlignment(String sl_sent_s2t, String alg_info_s2t, String sl_sent_t2s, String alg_info_t2s, Strategy strategy) {

        Set<Integer>[] s2talignment = ReadGizaAsymmetricAlignment(sl_sent_s2t, alg_info_s2t);
        Set<Integer>[] t2salignment = ReadGizaAsymmetricAlignment(sl_sent_t2s, alg_info_t2s);

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
            case GrowDiag:
                al = Symmetrisation.GrowDiag(s2talignment, t2salignment);
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
            case GrowDiag:
                al = Symmetrisation.GrowDiag(s2talignment, t2salignment);
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
            case GrowDiagFinalAnd_ORIGINAL:
                al = Symmetrisation.GrowDiagFinalAnd_ORIGINAL(s2talignment, t2salignment);
                break;
            case GrowDiag:
                al = Symmetrisation.GrowDiag(s2talignment, t2salignment);
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
        Strategy strategy = Strategy.GrowDiagFinalAnd;
        //Strategy strategy = Strategy.GrowDiag;
        int[][] forward = new int[][]{{0, 0},{1, 1},{2, 2},{3, 3},{4, 4},{5, 5},{6, 6},{5, 7},{10, 9},{5, 10}};
        int[][] backward = new int[][]{{0, 0},{1, 1},{2, 2},{3, 3},{4, 4},{5, 5},{6, 6},{7, 6},{8, 0},{9, 8},{10, 9},{11, 5},{12, 11}};

        String slAlignment, tlAlignment, symmAlignment;

        slAlignment= "";
        for (int[] alg : forward) { slAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("forward: " + slAlignment);
        System.out.print("forward: " + slAlignment + "\n");

        tlAlignment= "";
        for (int[] alg : backward) { tlAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("backward: " + tlAlignment);
        System.out.print("backward: " + tlAlignment + "\n");


        int[][] symmetrized = symmetriseAlignment(forward, backward, strategy);

        symmAlignment= "";
        for (int[] alg : symmetrized) { symmAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("symmetrized: " + symmAlignment + "\n");
        System.out.print("symmetrized: " + symmAlignment + "\n");


        //int[][] forward2 = new int[][]{{0, 0},{1, 1}};
        //int[][] backward2 = new int[][]{{0, 0},{1, 1}};


        int[][] forward2 = new int[][]{{0, 0},{1, 1},{1,2},{2,3},{8,4},{9,5},{6,6},{3, 8},{1,9},{6,10}};
        int[][] backward2 = new int[][]{{0, 1},{1, 1},{2, 2},{3, 8},{4, 5},{6, 10},{8, 4},{9, 5}};

//        int[][] forward2 = new int[][]{{0, 0},{1, 1},{2, 4},{3, 2},{4, 5},{4, 6},{5, 7},{6, 8},{8, 10},{9, 11},{10,11}, {11,9}};
//        int[][] backward2 = new int[][]{{0, 0},{1, 1},{2, 4},{3, 2},{4, 4},{5,7},{6, 7},{7, 8},{8, 10},{9, 11},{10,11}, {11,9}};


        slAlignment= "";
        for (int[] alg : forward2) { slAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("forward2: " + slAlignment);
        System.out.print("forward2: " + slAlignment + "\n");

        tlAlignment= "";
        for (int[] alg : backward2) { tlAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("backward2: " + tlAlignment);
        System.out.print("backward2: " + tlAlignment + "\n");


        int[][] symmetrized2 = symmetriseAlignment(forward2, backward2, strategy);

        symmAlignment= "";
        for (int[] alg : symmetrized2) { symmAlignment += " " + alg[0] + "-" + alg[1]; }
        logger.debug("symmetrized2: " + symmAlignment + "\n");
        System.out.print("symmetrized2: " + symmAlignment + "\n");
    }

}