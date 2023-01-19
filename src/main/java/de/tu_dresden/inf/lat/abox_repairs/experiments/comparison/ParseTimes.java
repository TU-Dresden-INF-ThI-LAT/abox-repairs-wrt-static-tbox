package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Parse the times used for querying from the output files created by IQPerformances, assuming certain file naming.
 */
public class ParseTimes {

    private static class Result {
        final String name;
        public Result(String name) {
            this.name=name;
        }
        public boolean isSuccessful() { return false; }
        public String toString() {return name; }
    }
    private static Result NO_REPAIR = new Result("no Repair");
    private static Result EXCEPTION = new Result("Exception");
    private static Result NO_FILE = new Result("no File");
    private static Result OTHER = new Result("Other");
    private static class SUCCESS extends Result {
        public final double time;
        public SUCCESS(double time){
            super(Double.toString(time));
            this.time=time;
        }
        @Override
        public boolean isSuccessful(){ return true; }
    }

    public static void main(String[] args) throws FileNotFoundException {

        String path = args[0];

        int precomputedFailed = 0, virtualFailed=0, total=0;

        Map<Result, Integer> failuresPrecomputed = new HashMap<>();
        Map<Result, Integer> failuresVirtual = new HashMap<>();

        PrintWriter writer = new PrintWriter(new File("query-times.data"));
        PrintWriter writer2 = new PrintWriter(new File("query-times-incl-repair-computation.data"));

        for(String seedFilename: new File(path).list(ParseTimes::seedFunctionFile)){
            total++;
            System.out.println(seedFilename);
            File repair = new File(path, seedFilename+".repair");
            File outputPrecomputed = new File(path,seedFilename+".output-precomputed");
            File outputVirtual = new File(path,seedFilename+".output-virtual");

            Result resultPrecomputed = getTime(outputPrecomputed);
//                    repair.exists() ? getTime(outputPrecomputed)
//                                    : NO_REPAIR;
            Result resultVirtual = getTime(outputVirtual);

            if(!resultPrecomputed.isSuccessful()) {
                if(!(resultPrecomputed==NO_REPAIR)){
                    System.out.println("Repair: "+resultPrecomputed);
                }
                precomputedFailed++;
                failuresPrecomputed.putIfAbsent(resultPrecomputed,0);
                failuresPrecomputed.put(resultPrecomputed, failuresPrecomputed.get(resultPrecomputed)+1);
            }
            if(!resultVirtual.isSuccessful()) {
                virtualFailed++;
                System.out.println("virtual failed!");
                failuresVirtual.putIfAbsent(resultVirtual,0);
                failuresVirtual.put(resultVirtual, failuresVirtual.get(resultVirtual)+1);
            }
            else if(resultPrecomputed.isSuccessful()) {
                writer.println(resultPrecomputed + " " + resultVirtual+ " "+seedFilename);
                //if(repair.exists()){
                    File repairOutput = new File(path, seedFilename+".repair-output");
                    Optional<Double> repairTime = getRepairTime(repairOutput);
                    if(repairTime.isPresent()) {
                        double completeTime = repairTime.get() + ((SUCCESS) resultPrecomputed).time;
                        writer2.println(completeTime + " " + resultVirtual + " " + seedFilename);
                    }
                //}
            }
        }

        writer.close();
        writer2.close();

        System.out.println(precomputedFailed+"/"+total+" times querying the precomputed repair failed");
        System.out.println(virtualFailed+"/"+total+" times querying the virtual repair failed");

        System.out.println("Precomputed: "+failuresPrecomputed);
        System.out.println("Virtual: "+failuresVirtual);
    }

    private static Optional<Double> getRepairTime(File repairOutput) throws FileNotFoundException {
        if(repairOutput.exists()){
            return new BufferedReader(new FileReader(repairOutput))
                    .lines()
                    .map(String::trim)
                    .filter(s -> s.startsWith("real"))
                    .map(s -> parseTime(s.split("\\s+")[1]))
                    .findFirst();
        } else {
            System.out.println("No time for: "+repairOutput);
            return Optional.empty();
        }
    }

    private static double parseTime(String string) {
        String[] components = string.split("s")[0].split("m");
        return Double.parseDouble(components[0])*60+Double.parseDouble(components[1]);
    }

    private static Result getTime(File outputFile) throws FileNotFoundException {
        if(outputFile.exists()){
            Optional<String> optString = new BufferedReader(new FileReader(outputFile))
                    .lines()
                    .map(String::trim)
                    .filter(s -> s.startsWith("TIMES: ") || s.contains("Exception"))
                    .findFirst();

            if(!optString.isPresent())
                return OTHER;
            else {
                String string = optString.get();
                if(string.startsWith("TIMES: "))
                    return new SUCCESS(Double.parseDouble(string.split(" ")[2])); // format: "TIMES: parsing querying"
                else if(string.contains("java.io.FileNotFoundException"))
                    return NO_REPAIR;
                else {
                    System.out.println(string);
                    // string contains "Exception"
                    return EXCEPTION;
                }
            }

            // Format: "TIMES: parsingTime queryTime"
        } else
            return NO_FILE;
    }

    public static boolean seedFunctionFile(File dir, String name) {
        return name.matches(".*seedFunction.[0-9]+")
                && new File(dir, name).length()>0;
    }
}
