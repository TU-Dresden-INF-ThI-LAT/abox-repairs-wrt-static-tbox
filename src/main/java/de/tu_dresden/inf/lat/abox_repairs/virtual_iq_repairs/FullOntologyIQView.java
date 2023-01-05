package de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs;

import de.tu_dresden.inf.lat.abox_repairs.ontology_tools.ConvertAnonymousVariables;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts anonymous individuals to named individuals, so that they can be taken into account when answering IQs.
 * The named individuals introduced in this way are filtered out from the query answers.
 */
public class FullOntologyIQView implements IQBlackbox {

    private final OWLOntology ontology;

    private final OWLDataFactory factory;
    private final OWLReasoner reasoner;

    private final Set<OWLNamedIndividual> filterOut;

    /**
     * ATTENTION: changes ontology!
     */
    public FullOntologyIQView(OWLOntology ontology) {
        this.ontology=ontology;
        this.factory=ontology.getOWLOntologyManager().getOWLDataFactory();
        this.filterOut = new ConvertAnonymousVariables(factory).convert(ontology);
        this.reasoner = new ElkReasonerFactory().createReasoner(ontology);
    }



    @Override
    public boolean isEntailed(OWLClassAssertionAxiom ce) {
        return reasoner.isEntailed(ce);
    }

    @Override
    public Collection<OWLNamedIndividual> query(OWLClassExpression ce) {
        return reasoner.getInstances(ce)
                .entities()
                .filter(a -> !filterOut.contains(a))
                .collect(Collectors.toSet());
    }
}
