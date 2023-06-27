package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.ELRestrictor;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;

public class OntologySize {
    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[0]));

        new ELRestrictor(ontology.getOWLOntologyManager().getOWLDataFactory()).restrict(ontology);

        //System.out.println(ontology.getAxiomCount());
        System.out.println(args[0]+" "+ontology.getLogicalAxiomCount()+" "
                + ontology.getABoxAxioms(Imports.INCLUDED).size() + " "
                + ontology.getTBoxAxioms(Imports.INCLUDED).size()  );
    }

}
