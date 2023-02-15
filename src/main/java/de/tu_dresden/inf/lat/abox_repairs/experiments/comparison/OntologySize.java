package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;

public class OntologySize {
    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[0]));

        System.out.println(ontology.getAxiomCount());
    }
}
