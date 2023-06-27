package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;

public class OntologySize {
    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[0]));

        //System.out.println(ontology.getAxiomCount());
        System.out.println(ontology.getLogicalAxiomCount()+" "
                + ontology.getABoxAxioms(Imports.INCLUDED).size() + " "
                + ontology.getTBoxAxioms(Imports.INCLUDED).size()  );
    }

}
