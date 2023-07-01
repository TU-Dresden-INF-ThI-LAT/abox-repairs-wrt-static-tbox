import de.tu_dresden.inf.lat.abox_repairs.experiments.generation.IQGenerator;
import de.tu_dresden.inf.lat.abox_repairs.tools.ManchesterSyntaxCleaner;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class TestIQParser {
    @Test
    public void testCorpusFile1() throws OWLOntologyCreationException, IOException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getClass().getResourceAsStream("ore_ont_8723.owl.norm"));

        OWLDataFactory factory = manager.getOWLDataFactory();

        List<OWLClassExpression> iqs = IQGenerator.parseIQs(new BufferedReader(
                new InputStreamReader(
                    getClass().getResourceAsStream("ore_ont_8723.owl.norm.iqs"))
                ), factory, ontology
        );
    }
    @Test
    public void testCorpusFile2() throws OWLOntologyCreationException, IOException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getClass().getResourceAsStream("ore_ont_13035.owl.norm"));

        OWLDataFactory factory = manager.getOWLDataFactory();

        List<OWLClassExpression> iqs = IQGenerator.parseIQs(new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream("ore_ont_13035.owl.norm.iqs"))
                ), factory, ontology
        );
    }
    @Test
    public void testCorpusFile3() throws OWLOntologyCreationException, IOException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(getClass().getResourceAsStream("ore_ont_13078.owl.norm"));

        OWLDataFactory factory = manager.getOWLDataFactory();

        List<OWLClassExpression> iqs = IQGenerator.parseIQs(new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream("ore_ont_13078.owl.norm.iqs"))
                ), factory, ontology
        );
    }
    @Test
    public void testBracketCleaning1() throws ParseException {
        String string =
            "<http://www.absoluteiri.edu/RELAPPROXC63300> and (<http://www.absoluteiri.edu/RELAPPROXC63185> and <http://www.absoluteiri.edu/RELAPPROXC63300>)";

        Optional<String> cleaned = ManchesterSyntaxCleaner.cleanOneStep(string);

        assert cleaned.isPresent();
        assertEquals(cleaned.get(),
                "<http://www.absoluteiri.edu/RELAPPROXC63300> and <http://www.absoluteiri.edu/RELAPPROXC63185> and <http://www.absoluteiri.edu/RELAPPROXC63300>");
    }


    @Test
    public void testBracketCleaning2() throws ParseException {
        String string =
                "<http://purl.obolibrary.org/obo/so#derives_from> some (<http://purl.obolibrary.org/obo/SO_0000831> and (<http://purl.obolibrary.org/obo/so#member_of> some <http://purl.obolibrary.org/obo/SO_0001411>))";

        Optional<String> cleaned = ManchesterSyntaxCleaner.cleanOneStep(string);

        assert cleaned.isPresent();
        assertEquals(cleaned.get(),
                "<http://purl.obolibrary.org/obo/so#derives_from> some (<http://purl.obolibrary.org/obo/SO_0000831> and <http://purl.obolibrary.org/obo/so#member_of> some <http://purl.obolibrary.org/obo/SO_0001411>)");
    }


    @Test
    public void testBracketCleaning3() throws ParseException {
        String string =
                "(<http://purl.obolibrary.org/obo/BFO_0000050> some <http://www.absoluteiri.edu/RELAPPROXC63185>) and <http://purl.obolibrary.org/obo/so#has_quality> some (<http://purl.obolibrary.org/obo/SO_0000010> and <http://www.absoluteiri.edu/RELAPPROXC63300>)";

        Optional<String> cleaned = ManchesterSyntaxCleaner.cleanOneStep(string);

        assert cleaned.isPresent();
        assertEquals(cleaned.get(),
                "<http://purl.obolibrary.org/obo/BFO_0000050> some <http://www.absoluteiri.edu/RELAPPROXC63185> and <http://purl.obolibrary.org/obo/so#has_quality> some (<http://purl.obolibrary.org/obo/SO_0000010> and <http://www.absoluteiri.edu/RELAPPROXC63300>)");
    }

    @Test
    public void testBracketCleaning4() throws ParseException {
        String string = "r some (A and B) and C";
        Optional<String> cleaned = ManchesterSyntaxCleaner.cleanOneStep(string);
        assert(!cleaned.isPresent());
        string = "r some ((B and C) and D)";
        cleaned = ManchesterSyntaxCleaner.cleanOneStep(string);
        assert(cleaned.isPresent());
        assertEquals(cleaned.get(), "r some (B and C and D)");
    }

    @Test
    public void testBracketCleaning5() throws ParseException {
        String string =
                "<http://xmlns.com/foaf/0.1/depicts> some (<http://purl.obolibrary.org/obo/BFO_0000050> some ((<http://purl.obolibrary.org/obo/HAO_0000003> and <http://purl.obolibrary.org/obo/HAO_0000041>) and (<http://purl.obolibrary.org/obo/BFO_0000050> some (((<http://purl.obolibrary.org/obo/BFO_0000050> some <http://purl.obolibrary.org/obo/HAO_0000006>) and (<http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000003> and (<http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>)))) and (<http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>))))))";

        String cleaned = ManchesterSyntaxCleaner.clean(string);

        System.out.println(cleaned);

        assertEquals(
                "<http://xmlns.com/foaf/0.1/depicts> some (<http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000003> and <http://purl.obolibrary.org/obo/HAO_0000041> and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/BFO_0000050> some <http://purl.obolibrary.org/obo/HAO_0000006> and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000003> and <http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>) and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>))))",
                cleaned);

    }


    @Test
    public void testBracketCleaning5partial() throws ParseException {
        String string =
                "r some (((<http://purl.obolibrary.org/obo/BFO_0000050> some <http://purl.obolibrary.org/obo/HAO_0000006>) and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000003> and <http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>)) and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>))";

        String cleaned = ManchesterSyntaxCleaner.clean(string);

        System.out.println(cleaned);

        assertEquals(
            "r some (<http://purl.obolibrary.org/obo/BFO_0000050> some <http://purl.obolibrary.org/obo/HAO_0000006> and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000003> and <http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>) and <http://purl.obolibrary.org/obo/BFO_0000050> some (<http://purl.obolibrary.org/obo/HAO_0000000> and <http://purl.obolibrary.org/obo/HAO_0000006>))",
                 cleaned);

    }
}
