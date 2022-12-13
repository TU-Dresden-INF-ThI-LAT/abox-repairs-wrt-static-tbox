package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunctionParser;
import de.tu_dresden.inf.lat.abox_repairs.tools.Timer;
import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.FullOntologyIQView;
import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.IQBlackbox;
import de.tu_dresden.inf.lat.abox_repairs.virtual_iq_repairs.VirtualIQRepair;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class IQPerformances {
    public static void main(String[] args) throws OWLOntologyCreationException, SeedFunctionParser.ParsingException, IOException {
        if(args.length==0)
            printHelpMessage();
        switch(args[0]) {
            case "precomputed": evaluatePrecomputedRepair(args); break;
            case "virtual": evaluateVirtualRepair(args); break;
            default: printHelpMessage();
        }
    }

    private static void evaluatePrecomputedRepair(String[] args) throws OWLOntologyCreationException, IOException {
        File ontologyFile = new File(args[1]);
        File iqFile = new File(args[2]);


        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        System.out.println("Loading file");
        Timer loadTimer = Timer.newTimer();
        loadTimer.startTimer();
        OWLOntology repair = manager.loadOntologyFromOntologyDocument(ontologyFile);
        double loadingTime = loadTimer.getTime();

        List<OWLClassExpression> iqs = IQGenerator.parseIQs(iqFile, manager.getOWLDataFactory(), repair);

        Timer queryTimer = Timer.newTimer();
        queryTimer.startTimer();
        IQBlackbox iqView = new FullOntologyIQView(repair);
        iqs.forEach(iqView::query);
        double queryingTime = queryTimer.getTime();
        System.out.println("TIMES: "+loadingTime+" "+queryingTime);
    }

    private static void evaluateVirtualRepair(String[] args) throws OWLOntologyCreationException, IOException, SeedFunctionParser.ParsingException {
        File ontologyFile = new File(args[1]);
        File seedFunctionFile = new File(args[2]);
        File iqFile = new File(args[3]);


        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        System.out.println("Loading file");
        Timer loadTimer = Timer.newTimer();
        loadTimer.startTimer();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        double loadingTime = loadTimer.getTime();

        SeedFunction seedFunction = new SeedFunctionParser(manager.getOWLDataFactory(), ontology).parseSeedFunction(seedFunctionFile);

        List<OWLClassExpression> iqs = IQGenerator.parseIQs(iqFile, manager.getOWLDataFactory(), ontology);

        Timer queryTimer = Timer.newTimer();
        queryTimer.startTimer();
        IQBlackbox iqView = new VirtualIQRepair(ontology,seedFunction);
        iqs.forEach(iqView::query);
        double queryingTime = queryTimer.getTime();
        System.out.println("TIMES: "+loadingTime+" "+queryingTime);
    }

    private static void printHelpMessage() {
        System.out.println("Usage:");
        System.out.println(IQPerformances.class.getCanonicalName()+" precomputed REPAIR IQs");
        System.out.println(IQPerformances.class.getCanonicalName()+" virtual ONTOLOGY SEED_FUNCTION IQs ");
        System.exit(1);
    }
}
