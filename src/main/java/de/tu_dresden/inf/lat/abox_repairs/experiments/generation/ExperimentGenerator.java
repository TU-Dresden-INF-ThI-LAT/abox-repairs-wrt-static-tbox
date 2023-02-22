package de.tu_dresden.inf.lat.abox_repairs.experiments.generation;

import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManager;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import de.tu_dresden.inf.lat.abox_repairs.repair_request.RepairRequest;
import de.tu_dresden.inf.lat.abox_repairs.saturation.SaturationException;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import de.tu_dresden.inf.lat.abox_repairs.tools.FullIRIShortFormProvider;
import de.tu_dresden.inf.lat.abox_repairs.tools.Timer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class ExperimentGenerator {

    protected final OWLOntology ontology;
    protected final OWLDataFactory factory;
    public ExperimentGenerator(OWLOntology ontology){
        this.ontology=ontology;
        this.factory=ontology
                .getOWLOntologyManager()
                .getOWLDataFactory();

    }

    public void createExperimentFiles(RepairRequest repairRequest, File repairRequestFile, File seedFunctionFile)
            throws IOException, OWLOntologyCreationException, SaturationException {
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

        Timer timer = Timer.newTimer();
        timer.startTimer();
        System.out.println("Generating seed function...");
        SeedFunction seedFunction = generateSeedFunction(repairRequest);
        System.out.println("Generating seed function took "+timer.getTime());
        saveSeedFunction(seedFunction,seedFunctionFile);
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
