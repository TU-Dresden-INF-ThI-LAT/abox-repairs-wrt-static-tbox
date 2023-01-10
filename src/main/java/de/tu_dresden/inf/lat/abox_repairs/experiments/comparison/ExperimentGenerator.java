package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

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

public class ExperimentGenerator {
    private final OWLOntology ontology;
    private final OWLDataFactory factory;

    public static void main(String[] args) throws OWLOntologyCreationException, SaturationException, IOException {
        if(args.length!=5) {
            System.out.println("Usage:");
            System.out.println(ExperimentGenerator.class.getCanonicalName()
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

        ExperimentGenerator generator = new ExperimentGenerator(ontology);

        generator.createExperimentFiles(
                proportionIndividuals,
                proportionClassNames,
                new File(repairRequestFile),
                new File(seedFunctionFile));
        System.out.println("done");
    }

    public ExperimentGenerator(OWLOntology ontology){
        this.ontology=ontology;
        this.factory=ontology
                .getOWLOntologyManager()
                .getOWLDataFactory();

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
        saveRepairRequest(repairRequest,repairRequestFile);

        /*
        // speed up generation by using a module
        System.out.println("extracting module...");
        timer.reset();
        timer.startTimer();
        SyntacticLocalityModuleExtractor extractor =
                new SyntacticLocalityModuleExtractor(LocalityClass.STAR, ontology.axioms());

        Stream<OWLEntity> aboxSignature = ontology.aboxAxioms(Imports.INCLUDED).flatMap(OWLAxiom::signature);

        Stream<OWLAxiom> module = extractor.extract(aboxSignature);
        System.out.println("module extraction took "+timer.getTime());

        System.out.println("ontology size: "+ontology.getAxiomCount());

        ontology.remove(ontology.axioms());
        module.forEach(ontology::add);

        System.out.println("module size: "+ontology.getAxiomCount());
*/

        timer.reset();
        timer.startTimer();
        System.out.println("Generating seed function...");
        SeedFunction seedFunction = generateSeedFunction(repairRequest);
        System.out.println("Generating seed function took "+timer.getTime());
        saveSeedFunction(seedFunction,seedFunctionFile);
    }

    public RepairRequest generateRepairRequest(double proportionIndividuals, double proportionClassNames){
        RunExperiment1 runExperiment1 = new RunExperiment1();
        runExperiment1.initAnonymousVariableDetector(false, RepairManagerBuilder.RepairVariant.IQ);
        return runExperiment1.generateRepairRequest(ontology,0.1,0.1);
    }

    public void saveRepairRequest(RepairRequest repairRequest, File file) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        try {
            repairRequest.individuals().forEach(ind -> {
                        writer.println(ind+":");
                        repairRequest.get(ind).forEach(ce -> {
                            writer.println("\t "+renderer.render(ce));
                        });
                        writer.println();
                    }
            );
        } finally {
            writer.close();
        }
    }

    public SeedFunction generateSeedFunction(RepairRequest repairRequest) throws OWLOntologyCreationException, SaturationException {

        System.out.println("building repair manager...");
        RepairManager repairManager = new RepairManagerBuilder()
                .trustRepairRequests(true)
                .setNeedsSaturation(true)
                .setVariant(RepairManagerBuilder.RepairVariant.IQ)
                .setRepairRequest(repairRequest)
                .setOntology(ontology)
                .build();

        System.out.println("init for repairing...");
        repairManager.initForRepairing();

        return repairManager.getSeedFunction();
    }

    public void saveSeedFunction(SeedFunction seedFunction, File file) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(new FullIRIShortFormProvider());
        try {
            seedFunction.individuals().forEach(ind -> {
                        if (!seedFunction.emptyRepairType(ind)) {
                            writer.println(ind + ":");
                            seedFunction.get(ind).getClassExpressions().forEach(ce -> {
                                writer.println("\t " + renderer.render(ce).replaceAll("\\s", " "));
                            });
                            writer.println();
                        }
                    }
            );
        } finally {
            writer.close();
        }
    }


}