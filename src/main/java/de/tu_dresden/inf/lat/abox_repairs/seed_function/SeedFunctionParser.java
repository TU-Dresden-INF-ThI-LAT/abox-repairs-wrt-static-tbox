package de.tu_dresden.inf.lat.abox_repairs.seed_function;

import de.tu_dresden.inf.lat.abox_repairs.tools.FullIRIShortFormProvider;
import de.tu_dresden.inf.lat.abox_repairs.tools.SimpleOWLEntityChecker;
import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLExpressionParser;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class SeedFunctionParser {

    private final OWLDataFactory factory;
    private final OWLOntology ontology;


    public SeedFunctionParser(OWLDataFactory factory, OWLOntology ontology) {
        this.factory=factory;
        this.ontology=ontology;
    }

    public SeedFunction parseSeedFunction(File file) throws IOException, ParsingException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        return parseSeedFunction(reader);
    }

    public SeedFunction parseSeedFunction(InputStream input) throws IOException, ParsingException {
        return parseSeedFunction(new BufferedReader(new InputStreamReader(input)));
    }

    private SeedFunction parseSeedFunction(BufferedReader reader) throws IOException, ParsingException {

        //OWLExpressionParser<OWLClassExpression> parser =
        //        new ManchesterOWLSyntaxClassExpressionParser(factory, new SimpleOWLEntityChecker(factory));

        ManchesterOWLSyntaxParserImpl manchesterParser =
                new ManchesterOWLSyntaxParserImpl(new OntologyConfigurator(), factory);
        manchesterParser.setOWLEntityChecker(new ShortFormEntityChecker(
                new BidirectionalShortFormProviderAdapter(new DefaultPrefixManager())){
            @Override
            public OWLClass getOWLClass(String string) {
                OWLClass result = super.getOWLClass(string);
                if(result==null)
                    result = factory.getOWLClass(IRI.create(string));
                return result;
            }

        });


             //   new SimpleOWLEntityChecker(factory));
        manchesterParser.setDefaultOntology(ontology);

       // ontology.classesInSignature().forEach(System.out::println);



        SeedFunction seedFunction = new SeedFunction();
        OWLNamedIndividual currentIndividual = null;
        Set<OWLClassExpression> classExpressions = null;
        int lineNr = 0;


        boolean finished = false;

        try {
            for (String line = reader.readLine();
                 !finished;
                 line = reader.readLine(), lineNr++) {

                boolean finalize = false;

                if(line==null) {
                    if(currentIndividual==null)
                        throw new ParsingException("No individual specified!");
                    seedFunction.put(currentIndividual, new RepairType(classExpressions));
                    finished = true;
                }
                else if(line.trim()=="")
                    ; // do nothing
                else if(line.startsWith("#")  // allow comments
                        || line.trim().isEmpty() ) {
                    // nothing
                } else if(!line.startsWith("\t")) {
                    if(currentIndividual!=null)
                        seedFunction.put(currentIndividual, new RepairType(classExpressions));
                    String iri = line.trim();
                    iri = iri.substring(1, iri.length()-2);
                    currentIndividual = factory.getOWLNamedIndividual(iri);
                    classExpressions = new HashSet<>();
                } else {
                    if(currentIndividual==null){
                        throw new ParsingException("Incorrect file format (line "+lineNr+")");
                    } else {
                       // line = "<http://purl.obolibrary.org/obo/BFO_0000052> some (<http://purl.obolibrary.org/obo/BFO_0000004> and (<http://purl.obolibrary.org/obo/BFO_0000053> some owl:Thing))";
                       // line = line.trim();
                       // System.out.println(line);
                        line = cleanLine(line);
                       // System.out.println(line);
                        //classExpressions.add(parser.parse(line.trim()));
                        classExpressions.add(manchesterParser.parseClassExpression(line));
                    }
                }
            }
        } finally {
            reader.close();
        }
        return seedFunction;
    }

    private static Pattern problematicPattern = Pattern.compile("<[^>]*<([^>]*)>>");
    private static Pattern getProblematicPattern2 = Pattern.compile("and \\(([^ ]+ some [^ ]+)\\)");

    private static Pattern spaces = Pattern.compile("\\s+");

    public static String cleanLine(String line)  {
        line = line.trim();

        // very dirty workaround to deal with a bug in the manchester parser I don't know how to solve

        line = problematicPattern.matcher(line).replaceAll("<$1>");
        line = spaces.matcher(line).replaceAll(" ");
        line = getProblematicPattern2.matcher(line).replaceAll("and $1");


        return line;
    }

    public static class ParsingException extends Exception {
        public ParsingException(String s) {
            super(s);
        }
    }
}
