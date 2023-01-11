package de.tu_dresden.inf.lat.abox_repairs.ontology_tools;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ELK ignores anonymous variables, yet our repairs may contain them. The purpose of this class is to replace anonymous
 * individuals with named individuals, while keeping a list of individual names that stand for anonymous variables. This
 * way, we can perform IQ queries on EL ontologies with anonymous variables, filtering out those names that stand for
 * anonymous variables.
 */
public class ConvertAnonymousVariables {
    private final OWLDataFactory factory;

    public ConvertAnonymousVariables(OWLDataFactory factory) {
        this.factory=factory;
    }

    /**
     * Replace all anonymous variables in the ontology by named individuals, returning the set of named individuals
     * used for this.
     *
     * ATTENTION: changes the ontology!
     */
    public Set<OWLNamedIndividual> convert(OWLOntology ontology) {
        Set<OWLAxiom> remove = new HashSet<>();
        Map<OWLAnonymousIndividual, OWLNamedIndividual> replacements = new HashMap<>();

        FreshOWLEntityFactory.FreshOWLNamedIndividualFactory indFactory =
                FreshOWLEntityFactory.FreshOWLNamedIndividualFactory.of(ontology);

        ontology.getAxioms(AxiomType.CLASS_ASSERTION).forEach(clAss -> {
            if(clAss.getIndividual().isAnonymous()){
                OWLAnonymousIndividual an = clAss.getIndividual().asOWLAnonymousIndividual();
                if(!replacements.containsKey(an))
                    replacements.put(an, indFactory.newFreshEntity());
                OWLNamedIndividual named = replacements.get(an);
                ontology.add(factory.getOWLClassAssertionAxiom(clAss.getClassExpression(),named));
                remove.add(clAss);
            }
        });

        ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION).forEach(prpAss -> {
            OWLNamedIndividual ind1, ind2;
            boolean someAnonymous = false;

            if(prpAss.getSubject().isAnonymous()){
                someAnonymous=true;
                OWLAnonymousIndividual an = prpAss.getSubject().asOWLAnonymousIndividual();
                if(!replacements.containsKey(an))
                    replacements.put(an, indFactory.newFreshEntity());
                ind1 = replacements.get(an);
            } else
                ind1 = prpAss.getSubject().asOWLNamedIndividual();

            if(prpAss.getObject().isAnonymous()){
                someAnonymous=true;
                OWLAnonymousIndividual an = prpAss.getObject().asOWLAnonymousIndividual();
                if(!replacements.containsKey(an))
                    replacements.put(an, indFactory.newFreshEntity());
                ind2 = replacements.get(an);
            } else
                ind2 = prpAss.getObject().asOWLNamedIndividual();

            if(someAnonymous){
                ontology.add(factory.getOWLObjectPropertyAssertionAxiom(
                        prpAss.getProperty(), ind1, ind2));
                remove.add(prpAss);
            }
        });

        ontology.removeAxioms(remove);

        return new HashSet<>(replacements.values());
    }
}
