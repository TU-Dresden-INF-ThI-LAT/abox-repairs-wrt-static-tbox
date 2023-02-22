package de.tu_dresden.inf.lat.abox_repairs.experiments.generation;

import de.tu_dresden.inf.lat.abox_repairs.experiments.cade21.RunExperiment1;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManager;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.saturation.SaturationException;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import de.tu_dresden.inf.lat.abox_repairs.tools.FullIRIShortFormProvider;
import de.tu_dresden.inf.lat.abox_repairs.tools.Timer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;

import java.io.*;

/**
 * Experiment with flat repair requests
 */
public class FlatExperimentGenerator extends ExperimentGenerator {

    public FlatExperimentGenerator(OWLOntology ontology) {
        super(ontology);
    }

    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException, IOException {
        if(args.length!=5) {
            System.out.println("Usage:");
            System.out.println(FlatExperimentGenerator.class.getCanonicalName()
                    + " ONTOLOGY PROPORTION_INDIVIDUALS PROPORTION_CLASS_NAMES REPAIR_REQUEST_FILE SEED_FUNCTION_FILE");
            System.exit(1);
        }

        String ontFilename = args[0];
        double proportionIndividuals = Double.parseDouble(args[1]);
        double proportionClassNames = Double.parseDouble(args[2]);
        String repairRequestFile = args[3];
        String seedFunctionFile = args[4];


        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(ontFilename));

        FlatExperimentGenerator generator = new FlatExperimentGenerator(ontology);

        generator.createExperimentFiles(
                proportionIndividuals,
                proportionClassNames,
                new File(repairRequestFile),
                new File(seedFunctionFile));
        System.out.println("done");
    }



    public void createExperimentFiles(double proportionIndividuals,
                                       double proportionClassNames,
                                       File repairRequestFile,
                                       File seedFunctionFile) throws OWLOntologyCreationException, SaturationException, IOException {
        System.out.println("Generating repair request...");
        Timer timer = Timer.newTimer();
        timer.startTimer();
        RepairRequest repairRequest = generateRepairRequest(proportionIndividuals,proportionClassNames);
        System.out.println("generating repair request took "+timer.getTime());
        super.createExperimentFiles(repairRequest, repairRequestFile, seedFunctionFile);
    }

    public RepairRequest generateRepairRequest(double proportionIndividuals, double proportionClassNames){
        RunExperiment1 runExperiment1 = new RunExperiment1();
        runExperiment1.initAnonymousVariableDetector(false, RepairManagerBuilder.RepairVariant.IQ);
        return runExperiment1.generateRepairRequest(ontology,0.1,0.1);
    }

}
