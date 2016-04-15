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

import java.util.*;

/**
 * @author miquel
 */
public class Symmetrisation {

    public enum Strategy {
        Intersection,
        Union,
        GrowDiagFinalAnd
    }

    /**
     * Symetrisation method which produces, as a result, the intersection of the
     * S2T and T2S alignments.
     *
     * @param s2t Source to target assymetric alignment produced with GIZA++
     * @param t2s Target to source assymetric alignment produced with GIZA++
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
                        //If the alignment appears in both the asymetric alignments, it is added to the symetrised alignment
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
     * @param s2t Source to target assymetric alignment produced with GIZA++
     * @param t2s Target to source assymetric alignment produced with GIZA++
     * @return
     */
    static public boolean[][] GrowDiagFinalAnd(Set<Integer>[] s2t, Set<Integer>[] t2s) {
        List<Pair<Integer, Integer>> neighbors = new LinkedList<Pair<Integer, Integer>>(); //neighbors

        //Defining neibourhood
        neighbors.add(new Pair(0, 1));
        neighbors.add(new Pair(-1, -0));
        neighbors.add(new Pair(0, -1));
        neighbors.add(new Pair(1, 0));

        //Diagonal (diag) neigourhood
        neighbors.add(new Pair(-1, -1));
        neighbors.add(new Pair(-1, 1));
        neighbors.add(new Pair(1, -1));
        neighbors.add(new Pair(1, 1));

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

}