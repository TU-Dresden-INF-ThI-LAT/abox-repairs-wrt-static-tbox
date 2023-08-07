import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunctionParser;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.IOException;
import java.io.InputStream;

public class TestSeedFunctionParser {

    @Test
    public void test1() throws IOException, OWLOntologyCreationException, SeedFunctionParser.ParsingException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(
                TestSeedFunctionParser.class
                        .getResource("ore_ont_13233.owl.norm")
                        .openStream());

        SeedFunctionParser parser = new SeedFunctionParser(factory, ontology);

        parser.parseSeedFunction(TestSeedFunctionParser
                .class
                .getResource("ore_ont_13233.owl.seedFunction.5")
                .openStream());
    }
}
