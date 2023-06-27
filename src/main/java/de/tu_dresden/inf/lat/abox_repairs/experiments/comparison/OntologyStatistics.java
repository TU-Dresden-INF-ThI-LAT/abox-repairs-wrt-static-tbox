package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class OntologyStatistics {
    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Assume file with each line of the form \"ONTOLOGY_SIZE ABOX_SIZE TBOX_SIZE\"");
        File inputFile = new File(args[0]);

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        reader.lines().forEach(x -> addValues(x));

        System.out.println("Ontology: "+statistics(ontology));
        System.out.println("ABox: "+statistics(abox));
        System.out.println("TBox: "+statistics(tbox));
    }

    private static String statistics(List<Integer> values) {
        values.sort(Comparator.comparing(x -> x));
        System.out.println("min max med avg");
        return values.get(0)+" "+values.get(values.size()-1)+values.get(values.size()/2)+" "+avg(values);
    }
    private static double avg(List<Integer> values) {
        double sum = 0.0;
        for(int value:values)
            sum+=value;
        return sum/values.size();
    }

    private static List<Integer> ontology = new ArrayList<>();
    private static List<Integer> abox = new ArrayList<>();
    private static List<Integer> tbox = new ArrayList<>();

    public static void addValues(String line) {
        String[] columns = line.split(" ");
        assert(columns.length==3);
        ontology.add(Integer.parseInt(columns[0]));
        abox.add(Integer.parseInt(columns[1]));
        tbox.add(Integer.parseInt(columns[2]));
    }
}
