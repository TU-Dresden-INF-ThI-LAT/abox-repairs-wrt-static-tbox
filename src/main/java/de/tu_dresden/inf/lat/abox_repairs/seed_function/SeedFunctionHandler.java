package de.tu_dresden.inf.lat.abox_repairs.seed_function;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairTypeHandler;
import de.tu_dresden.inf.lat.abox_repairs.saturation.AnonymousVariableDetector;
import org.semanticweb.owlapi.model.*;


public class SeedFunctionHandler {
	// TODO: make random choices determined by Random object


	//private Map<OWLNamedIndividual, RepairType> seedFunction;
	//private Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction;
	//private RepairRequest repairRequest;
	private OWLOntology ontology;
	private final RepairTypeHandler typeHandler;
	private final AnonymousVariableDetector detector;
	private final ReasonerFacade reasonerWithTBox;
	private final ReasonerFacade reasonerWithoutTBox;

	
	
	public SeedFunctionHandler(
			OWLOntology ontology,
			//RepairRequest repairRequest, RepairManager.RepairVariant variant,
			ReasonerFacade reasonerWithTBox, ReasonerFacade reasonerWithoutTBox,
			AnonymousVariableDetector detector) {
		
		this.ontology = ontology;
		//this.repairRequest = repairRequest;

		this.detector=detector;

		this.reasonerWithTBox = reasonerWithTBox;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.typeHandler = new RepairTypeHandler(reasonerWithTBox, reasonerWithoutTBox);
	}
	
	
	/**
	 * 
	 * Given a repair request, the method constructs a seed function that 
	 * maps each individual in the repair request to a repair type 
	 * 
	 * Assumption: every individual in repairRequest is named
	 * @return
	 */
	
	
	/*
	 * The way we construct our seed function at the moment is still not optimal, in the sense that 
	 * the generated random seed function is not minimal yet w.r.t. covering relation.
	 * 
	 * This would be challenging to find a good approach for directly constructing a minimal hitting set or
	 * a minimal seed function so that the resulting repair can also be optimal
	 */
	public SeedFunction computeRandomSeedFunction(RepairRequest repairRequest) {
		
		Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction =
				computeRandomHittingSet(repairRequest);

		SeedFunction seedFunction = new SeedFunction();
		Set<OWLNamedIndividual> setOfMappedIndividuals = hittingSetFunction.keySet();

		for(OWLNamedIndividual individual : setOfMappedIndividuals) {
			RepairType type = typeHandler.convertToRandomRepairType(hittingSetFunction.get(individual), individual);
			seedFunction.put(individual, type);
		}
		
		/*
		 *  In the beginning, the iteration is needed in order to map individuals that are not contained in the repair request.
		 *  And to map those individuals, we need an additional feature to distinguish whether the individuals in the saturated
		 *  ontology is named or anonymous, so that the anonymous detector is used.
		 *  But now we could easily employ the getter method that has been provided in the class SeedFunction.java.
		 *  This implies that we can now remove the creation of setOfRemainingIndividuals and the below iteration. 
		 */
		Set<OWLNamedIndividual> setOfRemainingIndividuals = ontology.getIndividualsInSignature()
															.stream()
															.filter(ind -> !setOfMappedIndividuals.contains(ind) &&
																			detector.isNamed(ind))
															.collect(Collectors.toSet());
		
		for(OWLNamedIndividual individual : setOfRemainingIndividuals) {
			RepairType type = typeHandler.newMinimisedRepairType(new HashSet<>());
			seedFunction.put(individual, type);
		}
		
		return seedFunction;
	}
	
	/**
	 *
	 * Given a repair request, for each individual in the request, the method
	 * constructs a hitting set of the top-level conjuncts of concepts that are 
	 * asserted to the individual.
	 *
	 * The "randomness" comes with the conjunctions.
	 */
	
	
	private Map<OWLNamedIndividual, Set<OWLClassExpression>> computeRandomHittingSet(RepairRequest repairRequest) {

		Map<OWLNamedIndividual, Set<OWLClassExpression>> hittingSetFunction = new HashMap<>(); // TODO use Multimap

		System.out.println("choosing random hitting set..");

		for(OWLNamedIndividual individual : repairRequest.individuals()) {

			// first process atoms (include everything that is entailed)
			hittingSetFunction.put(individual,
			reasonerWithTBox.types(individual) // faster to go from here, rather than from the repair type
					//.filter(x -> !(x instanceof OWLObjectIntersectionOf))
					.filter(x -> repairRequest.get(individual).contains(x))
					.map( x-> {
						if(x instanceof OWLObjectIntersectionOf) {
							return x.asConjunctSet().stream()
									.filter(con -> !reasonerWithTBox.equivalentToOWLThing(con))
									.findAny().orElseThrow(() -> new IllegalArgumentException("Invalid Repair Request"));
						} else
							return x;
							})
					.collect(Collectors.toSet()));


			// now pick selection from conjunctions
			/*repairRequest.get(individual)
					.stream()
					.filter(x -> x instanceof OWLObjectIntersectionOf)
					.forEach(concept -> {

			//for(OWLClassExpression concept : repairRequest.get(individual)) {
				
				if(reasonerWithTBox.instanceOf(individual, concept) ) {
					
				//	if(concept instanceof OWLObjectIntersectionOf) {
						OWLClassExpression atom = concept.asConjunctSet().stream()
								.filter(con -> !reasonerWithTBox.equivalentToOWLThing(con))
								.findAny().orElseThrow(() -> new IllegalArgumentException("Invalid Repair Request"));
								
						concept = atom;
						
				//	}
				//	if(!hittingSetFunction.containsKey(individual))
				//		hittingSetFunction.put(individual, new HashSet<>());

					hittingSetFunction.get(individual).add(concept);
					
				}
			});
			*/
		}
		return hittingSetFunction;
	}
}
