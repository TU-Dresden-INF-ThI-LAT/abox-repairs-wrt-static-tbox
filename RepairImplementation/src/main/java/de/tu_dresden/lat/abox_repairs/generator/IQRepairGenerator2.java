package de.tu_dresden.lat.abox_repairs.generator;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.AnonymousVariableDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.*;

public class IQRepairGenerator2 extends RepairGenerator {

    private static Logger logger = LogManager.getLogger(IQRepairGenerator.class);

    private final Set<CopiedOWLIndividual> individualsInTheRepair = new HashSet<>();
    private final CopiedOWLIndividual.Factory copiedOWLIndividualFactory = new CopiedOWLIndividual.Factory();
    private final Map<OWLNamedIndividual, RepairType> seedFunction;
    private final Queue<CopiedOWLIndividual> queue = new LinkedList<>();

    public IQRepairGenerator2(OWLOntology inputOntology, Map<OWLNamedIndividual, RepairType> inputSeedFunction) {
        super(inputOntology, inputSeedFunction);
        this.seedFunction = inputSeedFunction;
    }

    @Override
    public int getNumberOfCollectedIndividuals() {
        return individualsInTheRepair.size();
    }

    @Override
    protected void initialise() {
        reasonerWithTBox.cleanOntology(); // TODO: bad code design
        try {
            repair = ontology.getOWLOntologyManager().createOntology();
            repair.add(ontology.getTBoxAxioms(Imports.INCLUDED));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        anonymousDetector = AnonymousVariableDetector.newInstance(true, Main.RepairVariant.IQ);
    }

    @Override
    protected void generateVariables() {
        inputObjectNames.stream()
                .filter(anonymousDetector::isNamed)
                .map(individualName ->
                        copiedOWLIndividualFactory.newNamedIndividual(individualName, seedFunction.get(individualName))
                ).forEach(queue::offer);
        while (!queue.isEmpty()) {
            final CopiedOWLIndividual subject = queue.poll();
            ontology.classAssertionAxioms(subject.getIndividualInTheSaturation())
                    .map(OWLClassAssertionAxiom::getClassExpression)
                    .filter(owlClassExpression ->
                            !subject.getRepairType().getClassExpressions().contains(owlClassExpression))
//                    .filter(owlClassExpression -> !FreshNameProducer.isFreshOWLClass(owlClassExpression))
                    .map(owlClassExpression ->
                            OWLManager.getOWLDataFactory().getOWLClassAssertionAxiom(owlClassExpression, subject.getIndividualInTheRepair()))
                    .forEach(repair::add);
            ontology.objectPropertyAssertionAxioms(subject.getIndividualInTheSaturation())
                    .forEach(axiom -> {
                        typeHandler.findCoveringRepairTypes(
                                RepairType.empty(),
                                computeSuccessorSet(
                                        subject.getRepairType(),
                                        axiom.getProperty().getNamedProperty(),
                                        axiom.getObject().asOWLNamedIndividual()),
                                axiom.getObject().asOWLNamedIndividual()
                        ).forEach(repairType -> {
                            final CopiedOWLIndividual object =
                                    copiedOWLIndividualFactory.getOrElseCreateNewAnonymousIndividual(axiom.getObject().asOWLNamedIndividual(), repairType);
                            repair.add(OWLManager.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), subject.getIndividualInTheRepair(), object.getIndividualInTheRepair()));
                            if (individualsInTheRepair.add(object)) {
                                queue.offer(object);
                            }
                        });
                    });
        }
    }

    @Override
    protected void generateMatrix() throws OWLOntologyCreationException {
        /* The matrix is already populated during the above enumeration of all object names. */
    }
}