import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.FullOntologyIQView;
import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.IQBlackbox;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import javax.swing.text.html.Option;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class TestQuerying {

    @Test
    public void testQueryWithoutAnonymousVariables() throws OWLOntologyCreationException {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology ont = man.createOntology();
        OWLDataFactory factory = man.getOWLDataFactory();

        OWLNamedIndividual ind = factory.getOWLNamedIndividual(IRI.create("a"));
        OWLNamedIndividual suc = factory.getOWLNamedIndividual(IRI.create("b"));
        OWLObjectProperty prp = factory.getOWLObjectProperty(IRI.create("r"));
        OWLObjectPropertyAssertionAxiom ass = factory.getOWLObjectPropertyAssertionAxiom(prp,ind,suc);
        ont.add(ass);

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ont);
        reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        OWLClassExpression iq = factory.getOWLObjectSomeValuesFrom(prp, factory.getOWLThing());

        Set<OWLNamedIndividual> answer = reasoner.instances(iq).collect(Collectors.toSet());
        assertEquals(1,answer.size());
        assertTrue(answer.contains(ind));
    }

    @Test
    public void testQueryWithAnonymousVariables() throws OWLOntologyCreationException {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology ont = man.createOntology();
        OWLDataFactory factory = man.getOWLDataFactory();

        OWLNamedIndividual ind = factory.getOWLNamedIndividual(IRI.create("a"));
        OWLAnonymousIndividual an = factory.getOWLAnonymousIndividual();
        OWLObjectProperty prp = factory.getOWLObjectProperty(IRI.create("r"));
        OWLObjectPropertyAssertionAxiom ass = factory.getOWLObjectPropertyAssertionAxiom(prp,ind,an);
        ont.add(ass);

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ont);
        reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        OWLClassExpression iq = factory.getOWLObjectSomeValuesFrom(prp, factory.getOWLThing());

        Set<OWLNamedIndividual> answer = reasoner.instances(iq).collect(Collectors.toSet());
        assertEquals(0,answer.size());
    }


    @Test
    public void testQueryWithBlackbox() throws OWLOntologyCreationException {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology ont = man.createOntology();
        OWLDataFactory factory = man.getOWLDataFactory();

        OWLNamedIndividual ind = factory.getOWLNamedIndividual(IRI.create("a"));
        OWLAnonymousIndividual an = factory.getOWLAnonymousIndividual();
        OWLObjectProperty prp = factory.getOWLObjectProperty(IRI.create("r"));
        OWLObjectPropertyAssertionAxiom ass = factory.getOWLObjectPropertyAssertionAxiom(prp,ind,an);
        ont.add(ass);

        OWLAnonymousIndividual an2 = factory.getOWLAnonymousIndividual();
        OWLObjectPropertyAssertionAxiom ass2 = factory.getOWLObjectPropertyAssertionAxiom(prp,an,an2);
        ont.add(ass);


        IQBlackbox blackbox = new FullOntologyIQView(ont);

        OWLClassExpression iq = factory.getOWLObjectSomeValuesFrom(prp, factory.getOWLThing());

        Collection<OWLNamedIndividual> answer = blackbox.query(iq);
        assertEquals(1,answer.size());
        assertTrue(answer.contains(ind));
    }
}
