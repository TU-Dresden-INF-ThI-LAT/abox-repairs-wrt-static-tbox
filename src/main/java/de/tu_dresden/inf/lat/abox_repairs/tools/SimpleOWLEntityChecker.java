package de.tu_dresden.inf.lat.abox_repairs.tools;

import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nullable;

public class SimpleOWLEntityChecker implements OWLEntityChecker {

    private final OWLDataFactory factory;

    public SimpleOWLEntityChecker(OWLDataFactory factory) {
        this.factory=factory;
    }

    @Nullable
    @Override
    public OWLClass getOWLClass(String s) {
        System.out.println("checking: "+s);
        if(s==null)
            return null;
        else
            return factory.getOWLClass(IRI.create(s));
    }

    @Nullable
    @Override
    public OWLObjectProperty getOWLObjectProperty(String s) {
        if(s==null)
            return null;
        else
            return factory.getOWLObjectProperty(IRI.create(s));
    }

    @Nullable
    @Override
    public OWLDataProperty getOWLDataProperty(String s) {
        if(s==null)
            return null;
        else
            return factory.getOWLDataProperty(IRI.create(s));
    }

    @Nullable
    @Override
    public OWLNamedIndividual getOWLIndividual(String s) {
        if(s==null)
            return null;
        else
            return factory.getOWLNamedIndividual(IRI.create(s));
    }

    @Nullable
    @Override
    public OWLDatatype getOWLDatatype(String s) {
        if(s==null)
            return null;
        else
            return factory.getOWLDatatype(IRI.create(s));
    }

    @Nullable
    @Override
    public OWLAnnotationProperty getOWLAnnotationProperty(String s) {
        if(s==null)
            return null;
        else
            return factory.getOWLAnnotationProperty(IRI.create(s));
    }
}
