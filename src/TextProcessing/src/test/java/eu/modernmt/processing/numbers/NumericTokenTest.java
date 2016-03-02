package eu.modernmt.processing.numbers;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumericTokenTest {

    @Test
    public void testCurrency_Matching() {
        NumericToken source = new NumericToken("1,76$", "0,00$");
        NumericToken target = new NumericToken("$0.00");

        target.applyTransformation(source);
        assertEquals("$1.76", target.getText());
        assertEquals("$1.76", target.toString());
    }

    @Test
    public void testCurrency_NotMatching() {
        NumericToken source = new NumericToken("1,76$", "0,00$");
        NumericToken target = new NumericToken("$0.000");

        target.applyTransformation(source);
        assertEquals("1,76$", target.getText());
        assertEquals("1,76$", target.toString());
    }

    @Test
    public void testBigNumber_Matching() {
        NumericToken source = new NumericToken("147.530,50", "111.111,11");
        NumericToken target = new NumericToken("000000.00");

        target.applyTransformation(source);
        assertEquals("147530.50", target.getText());
        assertEquals("147530.50", target.toString());
    }

    @Test
    public void testBigNumber_NotMatching() {
        NumericToken source = new NumericToken("147.530,50", "111.111,11");
        NumericToken target = new NumericToken("00/00/00");

        target.applyTransformation(source);
        assertEquals("147.530,50", target.getText());
        assertEquals("147.530,50", target.toString());
    }

}
