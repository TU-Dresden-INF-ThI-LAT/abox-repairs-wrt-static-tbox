package de.tu_dresden.inf.lat.abox_repairs.seed_function;

import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SeedFunction extends HashMap<OWLNamedIndividual, RepairType> {

    public SeedFunction() {
        super();
    }

    @Override
    public RepairType get(Object key) {
        return super.getOrDefault(key, RepairType.empty());
    }

    public Set<OWLNamedIndividual> individuals() {
        return super.keySet();
    }

    public boolean emptyRepairType(OWLNamedIndividual ind) {
        return !super.containsKey(ind) || get(ind).isEmpty();
    }

    public Set<OWLClassExpression> getNestedClassExpression() {
        return values().stream().flatMap(rt ->
                rt.getClassExpressions()
                        .stream()
                        .flatMap(OWLClassExpression::nestedClassExpressions))
                .collect(Collectors.toSet());
    }
}
