package de.tu_dresden.inf.lat.abox_repairs.reasoning;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * Reasoner facade that avoids normalization.
 */
public class SimpleReasonerFacade implements IReasonerFacade
{
    private final OWLReasoner reasoner;
    private final OWLDataFactory factory;

    public SimpleReasonerFacade() throws OWLOntologyCreationException {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        OWLOntology emptyOntology = manager.createOntology();

        reasoner = new ElkReasonerFactory().createReasoner(emptyOntology);
        factory = manager.getOWLDataFactory();
    }

    @Override
    public boolean instanceOf(OWLIndividual ind, OWLClassExpression exp) {
        return reasoner.isEntailed(factory.getOWLClassAssertionAxiom(exp, ind));
    }

    @Override
    public boolean subsumedBy(OWLClassExpression subsumee, OWLClassExpression subsumer) {
        return reasoner.isEntailed(factory.getOWLSubClassOfAxiom(subsumee,subsumer));
    }
}
