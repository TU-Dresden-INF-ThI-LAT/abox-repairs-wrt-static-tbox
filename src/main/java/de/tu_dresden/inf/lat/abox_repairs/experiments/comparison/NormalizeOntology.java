package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.ABoxFlattener;
import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.ELRestrictor;
import de.tu_dresden.inf.lat.abox_repairs.tools.Timer;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class NormalizeOntology {

    public static void main(String[] args) throws OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
        if(args.length!=2) {
            System.out.println("Usage:");
            System.out.println(NormalizeOntology.class+ " INPUT_FILE OUTPUT_FILE");
        }
        String input = args[0];
        String output = args[1];

        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(input));

        prepareOntology(ontology);

        OWLManager.createOWLOntologyManager().saveOntology(ontology,
                new ManchesterSyntaxDocumentFormat(),
                new BufferedOutputStream(new FileOutputStream(output))
                );
    }

    public static void prepareOntology(OWLOntology ontology) {

        Timer timer = Timer.newTimer();

        timer.startTimer();
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
        new ELRestrictor(factory).restrict(ontology);
        new ABoxFlattener(factory).flatten(ontology);
        ontology.removeAxioms(ontology.getAxioms(AxiomType.DECLARATION));

        System.out.println("Preparing everything: "+timer.getTime()+" seconds.");
    }
}
