package eu.modernmt.decoder.neural.memory.lucene.rescoring.util;


import eu.modernmt.decoder.neural.memory.lucene.rescoring.util.*;

import java.util.*;

import eu.modernmt.model.Sentence;

// TODO: Potentially use integers instead of Strings, if overhead is worth it
public class Ngram {

    private String[] toks;
    private int hash = 0;

    public int getOrder() {
        return order;
    }

    private int order = 0;

    public Ngram(String[] toks) {
        this.toks = toks;
        this.order = toks.length;
    }

    public int hashCode() {
        if (hash == 0) {
            for(String tok : toks) {
                hash ^= HashUtil.smear(tok.hashCode());
            }
        }
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Ngram) {
            // TODO: Slow
            Ngram other = (Ngram) obj;
            return toks.equals(other.toks);
        } else {
            throw new RuntimeException("Comparing n-gram to non-n-gram");
        }
    }

    public String toString() {
        return toks.toString();
    }


//    public static void main(String[] args) {
//
//        String sentence = "This is a hypothesis sentence .";
//
//        String[] toks = sentence.split(" ");
//
//        Ngram ngram = new Ngram(toks);
//        System.err.println("ngram:" + ngram + " order:" + ngram.getOrder() + " hash:" + ngram.hashCode());
//        System.err.println("ngram:" + ngram + " order:" + ngram.getOrder() + " hash:" + ngram.hashCode());
//
//        sentence = "This is a longer hypothesis sentence .";
//        toks = sentence.split(" ");
//
//        ngram = new Ngram(toks);
//        System.err.println("ngram:" + ngram + " order:" + ngram.getOrder() + " hash:" + ngram.hashCode());
//        System.err.println("ngram:" + ngram + " order:" + ngram.getOrder() + " hash:" + ngram.hashCode());
//
//        sentence = "";
//        toks = sentence.split(" ");
//
//        ngram = new Ngram(toks);
//        System.err.println("ngram:" + ngram + " order:" + ngram.getOrder() + " hash:" + ngram.hashCode());
//        System.err.println("ngram:" + ngram + " order:" + ngram.getOrder() + " hash:" + ngram.hashCode());
//
//    }
}
