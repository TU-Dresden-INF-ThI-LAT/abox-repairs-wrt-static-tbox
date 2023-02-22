package de.tu_dresden.inf.lat.abox_repairs.experiments.generation;

import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.saturation.SaturationException;
import de.tu_dresden.inf.lat.abox_repairs.tools.Timer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.IOException;

public class ComplexExperimentGenerator extends ExperimentGenerator {

    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException, IOException, IQGenerationException {
        if(args.length!=6) {
            System.out.println("Usage:");
            System.out.println(ComplexExperimentGenerator.class.getCanonicalName()
                    + " ONTOLOGY PROPORTION_INDIVIDUALS REQUESTS_PER_INDIVIDUAL MAX_CONCEPT_SIZE REPAIR_REQUEST_FILE SEED_FUNCTION_FILE");
            System.exit(1);
        }

        String ontFilename = args[0];
        double proportionIndividuals = Double.parseDouble(args[1]);
        int requestsPerIndividual = Integer.parseInt(args[2]);
        int maxConceptSize = Integer.parseInt(args[3]);
        String repairRequestFile = args[4];
        String seedFunctionFile = args[5];


        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(ontFilename));

        ComplexExperimentGenerator generator = new ComplexExperimentGenerator(ontology);

        generator.createExperimentFiles(
                proportionIndividuals,
                requestsPerIndividual,
                maxConceptSize,
                new File(repairRequestFile),
                new File(seedFunctionFile));
        System.out.println("done");
    }

    public ComplexExperimentGenerator(OWLOntology ontology){
        super(ontology);
    }

    public void createExperimentFiles(double proportionIndividuals,
                                      int requestsPerIndividual,
                                      int maxConceptSize,
                                      File repairRequestFile,
                                      File seedFunctionFile) throws IOException, IQGenerationException, OWLOntologyCreationException, SaturationException {
        System.out.println("Generating repair request...");
        Timer timer = Timer.newTimer();
        timer.startTimer();
        ComplexRepairRequestGenerator gen = new ComplexRepairRequestGenerator(ontology);
        gen.getIqGenerator().setMaxSize(maxConceptSize);
        RepairRequest repairRequest = gen.generateRepairRequest(proportionIndividuals,requestsPerIndividual);
        System.out.println("generating repair request took " + timer.getTime());
        saveRepairRequest(repairRequest, repairRequestFile);

        super.createExperimentFiles(repairRequest,repairRequestFile,seedFunctionFile);
    }
}
