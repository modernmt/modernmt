package eu.modernmt.decoder.opennmt.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.opennmt.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static eu.modernmt.decoder.opennmt.memory.DomainAsserts.assertContainsBothDirections;
import static eu.modernmt.decoder.opennmt.memory.DomainAsserts.assertSize;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest {

    private static final Locale EN = new Locale("en");
    private static final Locale EN_US = new Locale("en", "US");
    private static final Locale ES = new Locale("es");
    private static final Locale FR = new Locale("fr");
    private static final Locale IT = new Locale("it");

    private static final String EN_TEXT = "hello world";
    private static final String IT_TEXT = "ciao mondo";

    private static final LanguagePair FR__ES = new LanguagePair(FR, ES);
    private static final LanguagePair EN__IT = new LanguagePair(EN, IT);
    private static final LanguagePair IT__EN = new LanguagePair(IT, EN);
    private static final LanguagePair EN_US__IT = new LanguagePair(EN_US, IT);
    private static final LanguagePair IT__EN_US = new LanguagePair(IT, EN_US);

    // Utils -----------------------------------------------------------------------------------------------------------

    private static List<TranslationUnit> tuList(LanguagePair language) {
        String source = "en".equals(language.source.getLanguage()) ? EN_TEXT : IT_TEXT;
        String target = "en".equals(language.target.getLanguage()) ? EN_TEXT : IT_TEXT;

        return Arrays.asList(
                TestUtils.tu(language, source, target),
                TestUtils.tu(language, source + " 2", target + " 2")
        );
    }

    private static MultilingualCorpus corpus(LanguagePair language) {
        String source = "en".equals(language.source.getLanguage()) ? EN_TEXT : IT_TEXT;
        String target = "en".equals(language.target.getLanguage()) ? EN_TEXT : IT_TEXT;

        return TestUtils.corpus(language, source, target, source + " 2", target + " 2");
    }

    private static void assertContainsTUs(LuceneTranslationMemory memory, LanguagePair language) throws IOException {
        String source = "en".equals(language.source.getLanguage()) ? EN_TEXT : IT_TEXT;
        String target = "en".equals(language.target.getLanguage()) ? EN_TEXT : IT_TEXT;

        assertContainsBothDirections(memory, language, source, target);
        assertContainsBothDirections(memory, language, source + " 2", target + " 2");
    }

    // -----------------------------------------------------------------------------------------------------------------

    private LuceneTranslationMemory memory;

    public void setup(LanguagePair... languages) throws Throwable {
        this.memory = new LuceneTranslationMemory(new LanguageIndex(Arrays.asList(languages)), new RAMDirectory());
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    // Base Mono-directional

    @Test
    public void test_BaseMonodirectional_ContributionDirect() throws Throwable {
        setup(EN__IT);

        memory.onDataReceived(tuList(EN__IT));
        assertSize(memory, 2 + 1);
        assertContainsTUs(memory, EN__IT);
    }

    @Test
    public void test_BaseMonodirectional_ContributionReversed() throws Throwable {
        setup(EN__IT);

        memory.onDataReceived(tuList(IT__EN));
        assertSize(memory, 2 + 1);
        assertContainsTUs(memory, IT__EN);
    }

    @Test
    public void test_BaseMonodirectional_BaseDomainAdd() throws Throwable {
        setup(EN__IT);

        memory.add(new Domain(1), corpus(EN__IT));
        assertSize(memory, 2);
        assertContainsTUs(memory, EN__IT);
    }

    @Test
    public void test_BaseMonodirectional_ReversedDomainAdd() throws Throwable {
        setup(EN__IT);

        memory.add(new Domain(1), corpus(IT__EN));
        assertSize(memory, 2);
        assertContainsTUs(memory, EN__IT);
    }

    @Test
    public void test_BaseMonodirectional_DialectDomainAdd() throws Throwable {
        setup(EN__IT);

        memory.add(new Domain(1), corpus(EN_US__IT));

        assertSize(memory, 2);
        assertContainsTUs(memory, EN__IT);
    }

    // Base Bi-directional

    @Test
    public void test_BaseBidirectional_Contribution() throws Throwable {
        setup(EN__IT, IT__EN);

        memory.onDataReceived(tuList(EN__IT));
        assertSize(memory, 2 + 1);
        assertContainsTUs(memory, EN__IT);
    }

    @Test
    public void test_BaseBidirectional_BaseDomainAdd() throws Throwable {
        setup(EN__IT, IT__EN);

        memory.add(new Domain(1), corpus(EN__IT));
        assertSize(memory, 2);
        assertContainsTUs(memory, EN__IT);
    }

    @Test
    public void test_BaseBidirectional_DialectDomainAdd() throws Throwable {
        setup(EN__IT, IT__EN);

        memory.add(new Domain(1), corpus(EN_US__IT));

        assertSize(memory, 2);
        assertContainsTUs(memory, EN__IT);
    }

    // Dialect Mono-directional

    @Test
    public void test_DialectMonodirectional_ContributionDirect() throws Throwable {
        setup(EN_US__IT);

        memory.onDataReceived(tuList(EN_US__IT));
        assertSize(memory, 2 + 1);
        assertContainsTUs(memory, EN_US__IT);
    }

    @Test
    public void test_DialectMonodirectional_ContributionReversed() throws Throwable {
        setup(EN_US__IT);

        memory.onDataReceived(tuList(IT__EN_US));
        assertSize(memory, 2 + 1);
        assertContainsTUs(memory, EN_US__IT);
    }

    @Test
    public void test_DialectMonodirectional_BaseDomainAdd() throws Throwable {
        setup(EN_US__IT);

        memory.add(new Domain(1), corpus(EN__IT));
        assertSize(memory, 0);
    }

    @Test
    public void test_DialectMonodirectional_ReversedDomainAdd() throws Throwable {
        setup(EN_US__IT);

        memory.add(new Domain(1), corpus(IT__EN));
        assertSize(memory, 0);
    }

    @Test
    public void test_DialectMonodirectional_DialectDomainAdd() throws Throwable {
        setup(EN_US__IT);

        memory.add(new Domain(1), corpus(EN_US__IT));

        assertSize(memory, 2);
        assertContainsTUs(memory, EN_US__IT);
    }

    // Dialect Bi-directional

    @Test
    public void test_DialectBidirectional_Contribution() throws Throwable {
        setup(EN_US__IT, IT__EN_US);

        memory.onDataReceived(tuList(EN_US__IT));
        assertSize(memory, 2 + 1);
        assertContainsTUs(memory, EN_US__IT);
    }

    @Test
    public void test_DialectBidirectional_BaseDomainAdd() throws Throwable {
        setup(EN_US__IT, IT__EN_US);

        memory.add(new Domain(1), corpus(EN__IT));
        assertSize(memory, 0);
    }

    @Test
    public void test_DialectBidirectional_DialectDomainAdd() throws Throwable {
        setup(EN_US__IT, IT__EN_US);

        memory.add(new Domain(1), corpus(EN_US__IT));

        assertSize(memory, 2);
        assertContainsTUs(memory, EN_US__IT);
    }

    // Others

    @Test
    public void test_Multilingual_Contribution() throws Throwable {
        setup(EN__IT, FR__ES);

        memory.onDataReceived(Arrays.asList(
                TestUtils.tu(EN__IT, "hello", "ciao"),
                TestUtils.tu(FR__ES, "bonjour", "hola")
        ));

        assertSize(memory, 2 + 1);
        assertContainsBothDirections(memory, EN__IT, "hello", "ciao");
        assertContainsBothDirections(memory, FR__ES, "bonjour", "hola");
    }

}
