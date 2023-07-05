package de.tu_dresden.inf.lat.abox_repairs.experiments.comparison;

import org.semanticweb.elk.reasoner.saturation.inferences.SubClassInclusionExpandedDefinition;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parse the times used for querying from the output files created by IQPerformances, assuming certain file naming.
 */
public class ParseTimes {

    public static void main(String[] args) throws FileNotFoundException {

        String path = args[0];

        int precomputedFailed = 0, virtualFailed=0, total=0;

        Map<Result, Integer> failuresPrecomputed = new HashMap<>();
        Map<Result, Integer> failuresVirtual = new HashMap<>();

        PrintWriter writer = new PrintWriter(new File("query-times.data"));
        PrintWriter writer2 = new PrintWriter(new File("query-times-incl-repair-computation.data"));

        BinaryEventCounter virtualFasterWithoutRepairComputation = new BinaryEventCounter();
        BinaryEventCounter virtualFasterInclRepairComputation = new BinaryEventCounter();

        List<Triple<Double,Double,Double>> repairSize2TimeList = new ArrayList<>();
        List<Triple<Double,Double,Double>> relativeRepairSize2TimeList = new ArrayList<>();
        List<Triple<Double,Double,Double>> diffRepairSize2TimeList = new ArrayList<>();

        for(String seedFilename: new File(path).list(ParseTimes::seedFunctionFile)){
            total++;
            System.out.println(seedFilename);
            //File repair = new File(path, seedFilename+".repair");
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
                failuresVirtual.putIfAbsent(resultVirtual, 0);
                failuresVirtual.put(resultVirtual, failuresVirtual.get(resultVirtual)+1);
            }
            else if(resultPrecomputed.isSuccessful()) {
                File repairSizeFile = new File(path, seedFilename+".repair.size");
                int repairSize = repairSizeFile.exists() ? getNumberInFile(repairSizeFile) : -1;

                double relRepairSize = -1;
                double diffRepairSize = -1;

                String ontFile = seedFilename.substring(0, seedFilename.lastIndexOf(".owl"))+".owl";
                int ontSize = getNumberInFile(new File(path,ontFile+".size"));

                if(repairSizeFile.exists()) {
                    repairSize2TimeList.add(new Triple<Double, Double, Double>(
                            (double)repairSize, ((SUCCESS) resultPrecomputed).time, ((SUCCESS) resultVirtual).time));
                    relativeRepairSize2TimeList.add(new Triple<>(
                            ((double)repairSize)/ontSize,
                            ((SUCCESS) resultPrecomputed).time, ((SUCCESS) resultVirtual).time));

                    diffRepairSize2TimeList.add(new Triple<>(
                            ((double)repairSize)-ontSize,
                            ((SUCCESS) resultPrecomputed).time, ((SUCCESS) resultVirtual).time));

                    relRepairSize = ((double)repairSize)/ontSize;
                    diffRepairSize = repairSize-ontSize;
                }

                writer.println(resultPrecomputed + " " + resultVirtual+ " "+seedFilename+" "+ontSize+" "+repairSize
                +" "+diffRepairSize+" "+resultPrecomputed.minus(resultVirtual)
                +" "+relRepairSize+" "+resultPrecomputed.divideBy(resultVirtual));
               // if(((SUCCESS)resultPrecomputed).time > 30) {
                    virtualFasterWithoutRepairComputation.add(
                            ((SUCCESS) resultPrecomputed).time >= ((SUCCESS) resultVirtual).time
                    );
                //}
                //if(repair.exists()){
                File repairOutput = new File(path, seedFilename+".repair-output");
                Optional<Double> repairTime = getRepairTime(repairOutput);
                if(repairTime.isPresent()) {
                    double completeTime = repairTime.get() + ((SUCCESS) resultPrecomputed).time;
                    writer2.println(completeTime + " " + resultVirtual + " " + seedFilename+ " "+ontSize+" "+repairSize);
                    virtualFasterInclRepairComputation.add(
                            completeTime > ((SUCCESS)resultVirtual).time
                    );
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

        System.out.println(virtualFasterWithoutRepairComputation+" times the virtual repair was faster if we do not count repair computation.");
        System.out.println(virtualFasterInclRepairComputation+" times the virtual repair was faster if we include repair computation.");

        System.out.println("Buckets by size:");
        evaluateBuckets(5, repairSize2TimeList);
        System.out.println();
        System.out.println("Buckets by relative size:");
        evaluateBuckets(5, relativeRepairSize2TimeList);
        System.out.println();
        System.out.println("Buckets by size difference:");
        evaluateBuckets(5, diffRepairSize2TimeList);
        System.out.println();


        System.out.println("Buckets by repair_size:");
        evaluateAtPercentile(.85, repairSize2TimeList);
        System.out.println();
        System.out.println("Buckets by (repair_size/ontology_size):");
        evaluateAtPercentile(.85, relativeRepairSize2TimeList);
        System.out.println();
        System.out.println("Buckets by (repair_size-ontology_size):");
        evaluateAtPercentile(.85, diffRepairSize2TimeList);
        System.out.println();

    }

    private static void evaluateAtPercentile(double percentile, List<Triple<Double,Double,Double>> triples) {
        triples.sort((a,b) -> Double.compare(a._1,b._1));
        int[] buckets = new int[2];
        int bucket1Size = (int)(((double)triples.size())*percentile);
        int bucket2Size = triples.size()-bucket1Size;
        System.out.println("Bucket sizes: "+bucket1Size+", "+ bucket2Size);
        System.out.println("Border value: "+triples.get(bucket1Size)._1);
        int currentBucket = 0;
        int currentBucketCount = 0;
        for(int i=0; i<triples.size(); i++){
            currentBucketCount++;
            if(currentBucketCount==bucket1Size){
                currentBucket++;
                currentBucketCount=0;
                buckets[currentBucket]=0;
            }
            if(triples.get(i)._2>triples.get(i)._3)
                buckets[currentBucket]++;
        }

        System.out.println("How often precomputed was slower: "+
                ((double)buckets[0])/bucket1Size+", "+((double)buckets[1])/bucket2Size
        +" of cases.");
    }

    private static void evaluateBuckets(int numBuckets, List<Triple<Double,Double,Double>> triples){
        triples.sort((a,b) -> Double.compare(a._1,b._1));
        int[] buckets = new int[numBuckets];
        double[] bucketBounds = new double[numBuckets];
        double bucketSize = ((double)triples.size())/numBuckets;
        System.out.println("Bucket size: "+bucketSize);
        int currentBucket = 0;
        int currentBucketCount = 0;
        for(int i=0; i<triples.size(); i++){
            currentBucketCount++;
            if(currentBucketCount>bucketSize){
                currentBucket++;
                currentBucketCount=0;
                buckets[currentBucket]=0;
                bucketBounds[currentBucket]=triples.get(i)._1;
            }
            if(triples.get(i)._2>triples.get(i)._3)
                buckets[currentBucket]++;
        }

        System.out.println("Bucket bounds: "+Arrays.toString(bucketBounds));
        System.out.println("How often precomputed was slower: "+
                Arrays.stream(buckets)
                        .mapToDouble(a -> ((double)a)/bucketSize)
                        .mapToObj(Double::toString).collect(Collectors.joining(" / "))
        +" of cases.");
    }

    private static int getNumberInFile(File file) throws FileNotFoundException {
        return Integer.parseInt(
            new BufferedReader(new FileReader(file))
                    .lines()
                    .collect(Collectors.toList()).get(1)
                    .trim());
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


    private static class Result {
        final String name;
        public Result(String name) {
            this.name=name;
        }
        public boolean isSuccessful() { return false; }
        public String toString() {return name; }

        public Result minus(Result resultVirtual) {
            if(resultVirtual.equals(this))
                return this;
            else
                return EXCEPTION;
        }

        public Result divideBy(Result resultVirtual) {
            if(resultVirtual.equals(this))
                return this;
            else
                return EXCEPTION;
        }
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
        @Override
        public Result minus(Result result) {
            if(result.isSuccessful()){
                return new SUCCESS(time-((SUCCESS)result).time);
            } else
                return EXCEPTION;
        }
        @Override
        public Result divideBy(Result result) {
            if(result.isSuccessful()){
                return new SUCCESS(time/((SUCCESS)result).time);
            } else
                return EXCEPTION;
        }
    }

    private static class BinaryEventCounter {
        int positive = 0, max=0;
        public void addPositive() {
            positive+=1; max+=1;
        }
        public void addNegative() {
            max+=1;
        }
        public void add(boolean value) {
            if(value)
                addPositive();
            else
                addNegative();
        }
        @Override
        public String toString(){
            return positive+"/"+max+" ("+ 100 * (double) positive / max + "%)";
        }
    }

    private final static class Triple<A,B,C> {
        public final A _1;
        public final B _2;
        public final C _3;

        public Triple(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }


    }

}
