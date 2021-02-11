package de.tu_dresden.lat.abox_repairs;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import de.tu_dresden.lat.abox_repairs.ontology_tools.CycleChecker;
import de.tu_dresden.lat.abox_repairs.ontology_tools.ELRestrictor;
import de.tu_dresden.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.lat.abox_repairs.repair_types.RepairType;
import de.tu_dresden.lat.abox_repairs.saturation.CanonicalModelGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.ChaseGenerator;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;

import javax.print.attribute.standard.RequestingUserName;


public class Main {
	
	/**
	 * Check: usually, static fields (variables) should be avoided where possible.
	 * In this case: check whether they are really needed to be outside the main method,
	 * and otherwise, add them. 
	 */
	private OWLOntology ontology;
	
	private Map<OWLNamedIndividual, RepairType> seedFunction;
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> repairRequest;
	
	private ReasonerFacade reasonerWithTBox, reasonerWithoutTBox;
	
	//private Scanner reader;
	
	//private CycleChecker checker;

	public enum RepairVariant {IQ, CQ};
	
	public static void main(String args[]) throws IOException, OWLOntologyCreationException, SaturationException {
		
		Main m = new Main();
		
		int i = 0;
		while(i < args.length) {
			// Initialize ontology
			// m.ontologyInitialisation(args, i);

			OWLOntology ontology =
					OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[i]));

			System.out.println("after loading ontology: ");
			for(OWLAxiom ax : ontology.getTBoxAxioms(Imports.INCLUDED)) {
				System.out.println("tbox axiom " + ax);
			}

			
			// // Initialize parser
			// m.parserInitialisation();
			
			// Initialize repair request

			File file = new File(args[i+1]);
			RepairRequestParser rrParser = new RepairRequestParser(m.ontology);
			RepairRequest repairRequest = rrParser.repairRequestScanning(file);


			RepairVariant variant;

			switch(args[i+2]){
				case "IQ": variant = RepairVariant.IQ; break;
				case "CQ": variant = RepairVariant.CQ; break;
				default:
					System.out.println("Unknown repairVariant: "+args[i+2]);
					System.exit(1);
					variant = RepairVariant.CQ;
			}

			m.performRepair(ontology, repairRequest, variant);

				i+=3;
				if(i < args.length) System.out.println("\n" + "=================================================");
//			}
		}		
	}

	public void performRepair(OWLOntology ontology,
							  RepairRequest repairRequest,
							  RepairVariant repairVariant) throws OWLOntologyCreationException, SaturationException {

		setOntology(ontology);
		this.repairRequest=repairRequest;

		boolean tboxExists = true;
		if (ontology.getTBoxAxioms(Imports.INCLUDED).isEmpty()) {
			tboxExists = false;
		}

		if (repairVariant.equals(RepairVariant.CQ) && tboxExists) {
			cqSaturate();
//				for(OWLAxiom ax : m.ontology.getTBoxAxioms(Imports.INCLUDED)) {
//					System.out.println("axiom debug" + ax);
//				}
		}

		// Initialize reasoner
		initReasonerFacade();

		System.out.println("after initializing reasoners: ");
		for (OWLAxiom ax : ontology.getTBoxAxioms(Imports.INCLUDED)) {
			System.out.println("tbox axiom " + ax);
		}

		if(!RepairRequest.checkValid(repairRequest, reasonerWithTBox)) {
			throw new IllegalArgumentException("Invalid repair request.");
		}

		// Saturate the ontology
		if (repairVariant.equals(RepairVariant.IQ) && tboxExists) {
			iqSaturate();
		}


//			if(!(args[i+2].equals("CQ") && m.checker.cyclic())) {
		if (isCompliant(ontology, repairRequest)) {
			System.out.println("\nThe ontology is compliant!");
		} else {
			System.out.println("\nThe ontology is not compliant!");

			seedFunctionConstruction(repairRequest);
			Set<OWLNamedIndividual> setIndividuals = seedFunction.keySet();
			Iterator<OWLNamedIndividual> iteSetIndividuals = setIndividuals.iterator();
			System.out.println("\nSeed Function");
			while (iteSetIndividuals.hasNext()) {
				OWLNamedIndividual oni = iteSetIndividuals.next();
				System.out.println("- " + oni);
				RepairType type = seedFunction.get(oni);
				System.out.println(type.getClassExpressions());
				System.out.println();
			}

			cleanOntology();

			if (repairVariant.equals(RepairVariant.IQ)) {
				IQRepair();

				System.out.println("Individuals in the IQ-repair:" + ontology.getIndividualsInSignature().size());
			} else {
				CQRepair();

				System.out.println("Individuals in the CQ-repair:" + ontology.getIndividualsInSignature().size());
			}


			initReasonerFacade();

//					if(args[i+2] instanceof String && args[i+2].equals("IQ") && tboxExists) {
//						m.iqSaturate();
//					}
//					else if(args[i+2] instanceof String && args[i+2].equals("CQ") && tboxExists) {
//						m.cqSaturate();
//					}

			if (isCompliant(ontology, repairRequest)) {
				System.out.println("The ontology is now compliant");
			} else {
				System.out.println("The ontology is still not compliant");
			}
		}
	}
	
	/*private void ontologyInitialisation(String input[], int i) throws OWLOntologyCreationException, FileNotFoundException {
		manager = OWLManager.createOWLOntologyManager();

		setOntology(manager.loadOntologyFromOntologyDocument(new File(input[i])));
	}*/

	public void setOntology(OWLOntology ontology) {
		this.ontology=ontology;
		ELRestrictor.restrictToEL(ontology);
//		checker = new CycleChecker(ontology);
	}
	


	private void initReasonerFacade() throws OWLOntologyCreationException {
		List<OWLClassExpression> additionalExpressions = new LinkedList<>();

		for(Collection<OWLClassExpression> exps:repairRequest.values()){
			for(OWLClassExpression exp: exps){
				additionalExpressions.add(exp);
				additionalExpressions.addAll(exp.getNestedClassExpressions());
			}
		}
		
//		ontology.axioms().forEach(ax -> System.out.println("During reasoner initialisation " + ax.toString()));
//		System.out.println("Size expression" + additionalExpressions.size());
		reasonerWithTBox = ReasonerFacade.newReasonerFacadeWithTBox(ontology, additionalExpressions);
//		System.out.println("split");
		reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(ontology, additionalExpressions);
	}


	private void cqSaturate() throws SaturationException {
		System.out.println("\nCQ-saturation");
		ChaseGenerator chase = new ChaseGenerator();
		chase.saturate(ontology);
//		ontology = chase.getOntology(); 
	}
	
	private void iqSaturate() throws SaturationException {
		System.out.println("\nIQ-saturation");
		CanonicalModelGenerator cmg = new CanonicalModelGenerator(reasonerWithTBox);
		cmg.saturate(ontology);
//		ontology = cmg.getOntology(); 
	}
	
	private void seedFunctionConstruction(Map<OWLNamedIndividual, Set<OWLClassExpression>> inputRepairRequest) {
		SeedFunctionHandler seedFunctionHandler = new SeedFunctionHandler(reasonerWithTBox, reasonerWithoutTBox);
		seedFunctionHandler.constructSeedFunction(inputRepairRequest);
		seedFunction = seedFunctionHandler.getSeedFunction();
	}
	
	private boolean isCompliant(OWLOntology inputOntology, Map<OWLNamedIndividual, Set<OWLClassExpression>> inputRepairRequest) {
		boolean compliant = true;
		
		for(OWLNamedIndividual individual : inputRepairRequest.keySet()) {
			for(OWLClassExpression concept : inputRepairRequest.get(individual)) {
				if(reasonerWithTBox.instanceOf(individual, concept)) {
//					System.out.println("Not Compliant! " + individual + " " + concept);
					return false;
				}
			}
			
		}
		
		return compliant;
	}
	
	
	private void CQRepair() throws OWLOntologyCreationException {
		CQRepairGenerator generator = new CQRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.CQRepair();
		ontology = generator.getOntology();
	}
	
	private void IQRepair() throws OWLOntologyCreationException {
		IQRepairGenerator generator = new IQRepairGenerator(ontology, seedFunction);
		generator.setReasoner(reasonerWithTBox, reasonerWithoutTBox);
		generator.IQRepair();
		ontology = generator.getOntology();
	}
	
	private void cleanOntology() {
		reasonerWithTBox.cleanOntology();
	}
}
