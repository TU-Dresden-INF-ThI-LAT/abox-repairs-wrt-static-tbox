package de.tu_dresden.lat.abox_repairs.saturation;

import de.tu_dresden.lat.abox_repairs.Main;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.ArrayList;
import java.util.List;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.manchester.cs.owl.owlapi.AnonymousIndividualCollector;

public abstract class AnonymousVariableDetector {

    public abstract boolean isAnonymous(OWLNamedIndividual individual);

    public static AnonymousVariableDetector newInstance(boolean saturated, Main.RepairVariant variant) {
        if(!saturated)
            return new AnonymousVariableDetector.DefaultVersion();
        else if(variant.equals(Main.RepairVariant.CQ))
            return new AnonymousVariableDetector.CQVersion();
        else {
            assert variant.equals(Main.RepairVariant.IQ);
            return new AnonymousVariableDetector.IQVersion();
        }
    }

    /**
     * Use this for non-saturated ontologies.
     */
    public static class DefaultVersion extends AnonymousVariableDetector {
        @Override
        public boolean isAnonymous(OWLNamedIndividual individual) {
            return false;
        }
    }

    /**
     * Use this for CQ-saturated ontologies.
     */
    public static class CQVersion extends AnonymousVariableDetector {

        @Override
        public boolean isAnonymous(OWLNamedIndividual individual){
            throw new AssertionError("NOT IMPLEMENTED!");
        }
    }

    /**
     * Use this for IQ-saturated ontologies;
     */
    public static class IQVersion extends AnonymousVariableDetector {

        @Override
        public boolean isAnonymous(OWLNamedIndividual individual){
            return individual.getIRI().toString().startsWith("__i__");
        }
    }

    public boolean isNamed(OWLNamedIndividual ind) {
        return !isAnonymous(ind);
    }

    public List<OWLNamedIndividual> getNamedIndividuals(OWLOntology ontology) {
        List<OWLNamedIndividual> result = new ArrayList<>();
        ontology.individualsInSignature().filter(this::isNamed).forEach(result::add);
        return result;
    }
}