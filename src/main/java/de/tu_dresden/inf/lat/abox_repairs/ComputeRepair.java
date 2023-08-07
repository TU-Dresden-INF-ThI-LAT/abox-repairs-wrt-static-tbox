package de.tu_dresden.inf.lat.abox_repairs;

import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManager;
import de.tu_dresden.inf.lat.abox_repairs.repair_manager.RepairManagerBuilder;
import de.tu_dresden.inf.lat.abox_repairs.saturation.SaturationException;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunctionParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ComputeRepair {
    public static void main(String[] args) throws OWLOntologyCreationException, SeedFunctionParser.ParsingException, IOException, SaturationException, OWLOntologyStorageException {
        if(args.length!=4) {
            System.out.println("Usage:");
            System.out.println(ComputeRepair.class.getCanonicalName()+" ONTOLOGY SEED_FUNCTION_FILE [iq|cq] OUTPUT_FILE");
            System.exit(1);
        }

        File ontologyFile = new File(args[0]);
        File seedFunctionFile = new File(args[1]);
        String variant = args[2];
        File outputFile = new File(args[3]);

        RepairManagerBuilder.RepairVariant repairVariant = null;

        switch (variant) {
            case "iq":
                repairVariant= RepairManagerBuilder.RepairVariant.IQ;
                break;
            case "cq":
                repairVariant= RepairManagerBuilder.RepairVariant.CQ;
		break;
            default:
                System.out.println("Unsupported repair variant: "+variant);
                System.exit(1);
        }

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        SeedFunction seedFunction =
                new SeedFunctionParser(manager.getOWLDataFactory(), ontology)
                .parseSeedFunction(seedFunctionFile);

        RepairManager repairManager =
                new RepairManagerBuilder()
                        .setOntology(ontology)
                        .setVariant(repairVariant)
                        .setNeedsSaturation(true)
                        .setSeedFunction(seedFunction)
                        .build();

        ontology = repairManager.initAndPerformRepair();

        manager.saveOntology(ontology , new ManchesterSyntaxDocumentFormat(), new FileOutputStream(outputFile));
    }
}
