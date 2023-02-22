package de.tu_dresden.inf.lat.abox_repairs.reasoning;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;

public interface IReasonerFacade {

    boolean instanceOf(OWLIndividual ind, OWLClassExpression exp);

    boolean subsumedBy(OWLClassExpression subsumee, OWLClassExpression subsumer);
}
