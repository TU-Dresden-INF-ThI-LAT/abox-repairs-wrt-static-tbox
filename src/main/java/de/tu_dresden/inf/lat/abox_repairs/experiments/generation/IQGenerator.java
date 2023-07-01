package de.tu_dresden.inf.lat.abox_repairs.experiments.generation;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.saturation.CanonicalModelGenerator;
import de.tu_dresden.inf.lat.abox_repairs.tools.FullIRIShortFormProvider;
import de.tu_dresden.inf.lat.abox_repairs.tools.ManchesterSyntaxCleaner;
import org.semanticweb.owlapi.expression.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Generating IQs that, for a given ontology, give at least one answer
 */
public class IQGenerator {
    private final OWLOntology ontology;
    private final Random random;
    private final OWLDataFactory dataFactory;

    private double probabilityExists=0.5;
    private double probabilityConjunction=0.5;
    private int minDepth = 0;
    private int maxDepth = Integer.MAX_VALUE;

    private int maxSize = Integer.MAX_VALUE;

    public static void main(String[] args) throws OWLOntologyCreationException, IOException, IQGenerationException {

        if(args.length!=3) {
            System.out.println("Usage: ");
            System.out.println(IQGenerator.class.getCanonicalName()+" ONTOLOGY NUMBER OUTPUT");
            System.exit(1);
        }

        OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(args[0]));
        int number = Integer.parseInt(args[1]);
        File outputFile = new File(args[2]);

        PrintWriter writer = new PrintWriter(new FileWriter(outputFile));

        IQGenerator iqGenerator = new IQGenerator(ontology);

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(new FullIRIShortFormProvider());

        try {
            for (int i = 0; i < number; i++) {
                OWLClassExpression iq = iqGenerator.generateIQ();
                writer.println(renderer.render(iq).replaceAll("\\s+", " "));
            }
        } finally {
            writer.close();
        }

    }

    public static List<OWLClassExpression> parseIQs(File file, OWLDataFactory factory, OWLOntology ontology) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        return parseIQs(reader, factory, ontology);
    }

    public static List<OWLClassExpression> parseIQs(BufferedReader reader, OWLDataFactory factory, OWLOntology ontology) throws IOException {
        ManchesterOWLSyntaxParserImpl parser =
                new ManchesterOWLSyntaxParserImpl(new OntologyConfigurator(), factory);
        parser.setDefaultOntology(ontology);

	// added 30/06.2023
	parser.setOWLEntityChecker(new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(new DefaultPrefixManager())){
            @Override
            public OWLClass getOWLClass(String string) {
                OWLClass result = super.getOWLClass(string);
                if(result==null)
                    result = factory.getOWLClass(IRI.create(string));
                return result;
            }

        });


        List<OWLClassExpression> result = new LinkedList<>();

        for(String line=reader.readLine(); line!=null; line=reader.readLine()){
            //   System.out.println(line);
            try {
                line = cleanLine(line);
            } catch (ParseException e) {
                throw new IOException(e);
            }
            // System.out.println(line);
            result.add(parser.parseClassExpression(line));
        }

        return result;
    }

    // added 30/06/2023
    private static Pattern problematicPattern = Pattern.compile("<[^>]*<([^>]*)>>");
    private static Pattern getProblematicPattern2 = Pattern.compile("and \\(([^ ]+ some [^ ]+)\\)");

    private static Pattern spaces = Pattern.compile("\\s+");

    private static String cleanLine(String line) throws ParseException {
        line = line.trim();

        // very dirty workaround to deal with a bug in the manchester parser I don't know how to solve

        line = problematicPattern.matcher(line).replaceAll("<$1>");
        line = spaces.matcher(line).replaceAll(" ");
        //line = getProblematicPattern2.matcher(line).replaceAll("and $1");
        line = ManchesterSyntaxCleaner.clean(line);


        return line;
    }


    /**
     * Careful: changes the ontology!
     */
    public IQGenerator(OWLOntology ontology) {
        this(ontology, new Random());
    }

    /**
     * Careful: saturates the ontology!
     */
    public IQGenerator(OWLOntology ontology, Random random) {
        this.ontology=ontology;
        this.dataFactory=ontology.getOWLOntologyManager().getOWLDataFactory();
        this.random=random;

        // create the IQ-saturation
        new CanonicalModelGenerator(ReasonerFacade.newReasonerFacadeWithTBox(ontology)).saturate(ontology);
    }

    public OWLClassExpression generateIQ() throws IQGenerationException {
        List<OWLNamedIndividual> inds = ontology.individualsInSignature(Imports.INCLUDED)
                .collect(Collectors.toList());

        if(inds.isEmpty())
            throw new IQGenerationException("Ontology contains no individuals!");

        while(!inds.isEmpty()){
            OWLNamedIndividual root = takeRandom(inds);
            Optional<OWLClassExpression> opt = generateIQ(root,0,new ValueHolder<>(0));
            if(opt.isPresent())
                return opt.get();
        }

        throw new IQGenerationException("No IQ under the constraints could be constructed! (minDepth: "+minDepth+", maxDepth: "+maxDepth+")");

    }

    public Optional<OWLClassExpression> generateIQ(OWLNamedIndividual individual) throws IQGenerationException {
        return generateIQ(individual, 0, new ValueHolder<>(0));
    }

    private Optional<OWLClassExpression> generateIQ(OWLIndividual ind, int currentDepth, ValueHolder<Integer> currentSize) {
        boolean hasSuccessors = ontology.objectPropertyAssertionAxioms(ind)
                .findAny()
                .isPresent();

//        System.out.println(currentSize);

//        System.out.println("ind: "+ind);
//        System.out.println("has successors: "+hasSuccessors);

        if(currentSize.value>=maxSize)
            return Optional.empty();

        if(!hasSuccessors && currentDepth<minDepth) {
//            System.out.println("cannot continue this path");
            return Optional.empty();
        }

        boolean chooseExists = hasSuccessors &&
                (currentDepth < minDepth
                        || (currentDepth < maxDepth && random.nextDouble() < probabilityExists ));

//        System.out.println("Choose exists: "+chooseExists);

        OWLClassExpression result = null; //

        if(!chooseExists){

            List<OWLClass> names = ontology.classAssertionAxioms(ind)
                    .map(x -> x.getClassExpression())
                    .filter(x -> x instanceof OWLClass && !x.isOWLThing())
                    .map(x -> (OWLClass) x)
                    .collect(Collectors.toList());

//            System.out.println("names: "+names);

            if(names.isEmpty())
                return Optional.empty();
            else {
                currentSize.value++;
                result = takeRandom(names);
            }

            assert result!=null;
        } else {
            // chooseExists
            List<OWLObjectPropertyAssertionAxiom> successors =
                    ontology.objectPropertyAssertionAxioms(ind)
                            .collect(Collectors.toList());

            boolean done = false;

            while(!done && !successors.isEmpty()) {
                OWLObjectPropertyAssertionAxiom ra = takeRandom(successors);
                currentSize.value+=2;
                Optional<OWLClassExpression> filler = generateIQ(ra.getObject(), currentDepth+1, currentSize);
                if(filler.isPresent()){
                    result = dataFactory.getOWLObjectSomeValuesFrom(
                            ra.getProperty(),
                            filler.get());
                } else {
                    result = dataFactory.getOWLObjectSomeValuesFrom(
                                ra.getProperty(),
                                dataFactory.getOWLThing());
                }
                done = true;
            }

            if(!done && successors.isEmpty()) {
//                System.out.println("no successor of "+ind+" was successful!");
                return Optional.empty();
            }
            assert result!=null;
        }
        assert result!=null;


        if(random.nextDouble()<probabilityConjunction){
//            System.out.println("adding a conjunct!");
//            System.out.println("Until here: "+result);
            currentSize.value++;
            Optional<OWLClassExpression> conjunct = generateIQ(ind,currentDepth, currentSize); // we can assume this to be successful if we got that far
            if(conjunct.isPresent())
                return Optional.of(
                    dataFactory.getOWLObjectIntersectionOf(result,conjunct.get()));
            else
                return Optional.of(result);
        } else
            return Optional.of(result);
    }


    private <T> T takeRandom(List<T> list) {
        return list.remove(
                random.nextInt(
                        list.size()));
    }

    public double getProbabilityExists() {
        return probabilityExists;
    }

    public void setProbabilityExists(double probabilityExists) {
        this.probabilityExists = probabilityExists;
    }

    public double getProbabilityConjunction() {
        return probabilityConjunction;
    }

    public void setProbabilityConjunction(double probabilityConjunction) {
        this.probabilityConjunction = probabilityConjunction;
    }


    public void setMaxSize(int maxSize) {
        this.maxSize=maxSize;
    }

    public int getMinDepth() {
        return minDepth;
    }

    public void setMinDepth(int minDepth) {
        this.minDepth = minDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }


    private final static class ValueHolder<X> {
        public ValueHolder(X value) {
            this.value=value;
        }
        
        public X value;

        public String toString() {
            return "(" + value + ")";
        }
    }
}
