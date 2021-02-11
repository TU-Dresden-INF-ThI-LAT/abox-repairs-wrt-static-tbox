package de.tu_dresden.lat.abox_repairs.experiments;

import de.tu_dresden.lat.abox_repairs.Main;
import de.tu_dresden.lat.abox_repairs.RepairRequest;
import de.tu_dresden.lat.abox_repairs.saturation.SaturationException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RunExperiment1 {


    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException {
        if(args.length<4) {
            System.out.println("Usage: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+ " ONTOLOGY_FILE IQ|CQ PROPORTION1 PROPORTION2 [SEED]");
            System.out.println();
            System.out.println("Generates a repair of ONTOLOGY_FILE with a randomly generated repair request that");
            System.out.println("randomly assigns concept names to some individual name so that a proportion of ");
            System.out.println("PROPORTION2 of the entire set of concept names is selected, and a proportion of ");
            System.out.println("PROPORTION1 of the individuals gets a repair request. You may optionally provide");
            System.out.println("a seed value for the random number generator used.");
            System.out.println();
            System.out.println("Example: ");
            System.out.println("java -cp ... "+RunExperiment1.class.getCanonicalName()+" ore_ont_3453.owl IQ 0.1 0.2");
            System.exit(0);
        }

        String ontologyFileName = args[0];
        Main.RepairVariant repairVariant;
        switch(args[1]){
            case "IQ": repairVariant = Main.RepairVariant.IQ; break;
            case "CQ": repairVariant = Main.RepairVariant.CQ; break;
            default:
                System.out.println("Unexpected repair variant: "+args[1]);
                System.out.println("Call without parameters to get help information");
                repairVariant=Main.RepairVariant.CQ;
                System.exit(1);
        }

        double proportionIndividuals = Double.parseDouble(args[2]);
        double proportionClassNames = Double.parseDouble(args[3]);

        RunExperiment1 experiment = new RunExperiment1();

        if(args.length>4){
            long seed = Long.parseLong(args[4]);
            experiment.setSeed(seed);
        }
        try {
            experiment.startExperiment(ontologyFileName, repairVariant, proportionIndividuals, proportionClassNames);
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("Used seed: "+experiment.getSeed());
    }

    private final Random random;
    private long seed;

    private RunExperiment1(){
        random = new Random();
        seed = random.nextLong();
        random.setSeed(seed);
    }

    private long getSeed(){
        return seed;
    }

    private void setSeed(long seed) {
        this.seed=seed;
        random.setSeed(seed);
    }

    private void startExperiment(String ontologyFileName, Main.RepairVariant repairVariant, double proportionIndividuals, double proportionClassNames)
            throws OWLOntologyCreationException, SaturationException {

        OWLOntology ontology =
                OWLManager.createOWLOntologyManager()
                        .loadOntologyFromOntologyDocument(new File(ontologyFileName));

        RepairRequest repairRequest = generateRepairRequest(ontology, proportionIndividuals, proportionClassNames);

        Main main = new Main();
        main.performRepair(ontology, repairRequest, repairVariant);
    }

    private RepairRequest generateRepairRequest(
            OWLOntology ontology, double proportionIndividuals, double proportionClassNames) {
        RepairRequest request = new RepairRequest();

        List<OWLClass> classList = ontology.classesInSignature().collect(Collectors.toList());

        Set<OWLNamedIndividual> individuals = randomIndividuals(ontology, proportionIndividuals);

        for(OWLNamedIndividual individual: individuals) {
            request.put(individual, randomClasses(classList, proportionClassNames));
        }

        return request;
    }

    private Set<OWLNamedIndividual> randomIndividuals(OWLOntology ontology, double proportion) {
        Set<OWLNamedIndividual> result = new HashSet<>();

        List<OWLNamedIndividual> individuals = ontology.individualsInSignature().collect(Collectors.toList());

        System.out.println("Requests for "+((int)(proportion*individuals.size()))+" individual names.");

        for(int i=0; i<proportion*individuals.size(); i++) {
            OWLNamedIndividual ind = individuals.get(random.nextInt(individuals.size()));
            while(result.contains(ind)){
                ind = individuals.get(random.nextInt(individuals.size()));
            }
            result.add(ind);
        }
        return result;
    }

    private Set<OWLClassExpression> randomClasses(List<OWLClass> classList, double proportion) {
        Set<OWLClassExpression> result = new HashSet<>();

        for(int i=0; i<proportion*classList.size(); i++) {
            OWLClass cl = classList.get(random.nextInt(classList.size()));
            while(result.contains(cl)){
                cl = classList.get(random.nextInt(classList.size()));
            }
            result.add(cl);
        }
        return result;
    }
}