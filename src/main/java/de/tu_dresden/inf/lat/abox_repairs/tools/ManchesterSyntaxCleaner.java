package de.tu_dresden.inf.lat.abox_repairs.tools;

import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunctionParser;
import org.checkerframework.checker.nullness.Opt;

import javax.swing.text.html.Option;
import java.text.ParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * to clean up class expressions in manchester syntax so that the manchester parser can parse them.
 * in particular: remove unnecessarry brackets as in:
 * (A and (r some B))   ---> (A and r some B)
 * (A and (B and C))    ---> (A and B and C)
 */
public class ManchesterSyntaxCleaner {


    // patterns make the point of the opening bracket to be removed
    private static Pattern andAnd = Pattern.compile("and\\s+(\\()[^\\s+]+\\s+and\\s");
    private static Pattern andSome = Pattern.compile("and\\s+(\\()[^\\s+]+\\s+some\\s");

    private static Pattern some = Pattern.compile("(\\()[^\\s\\(]+\\s+some");
    private static Pattern and = Pattern.compile("(\\()[^\\s\\(]+\\s+and");

    private static Pattern singletonBracket = Pattern.compile("(\\()[^\\s\\)]+\\)");

    private static Pattern and2 = Pattern.compile("\\) and");

    public static String clean(String string) throws ParseException {
        string = string.replaceAll("\\s+", " ");
        boolean done=false;
        while(!done){
            done = true;
            Optional<String> oneStep = cleanOneStep(string);
            if(oneStep.isPresent()){
                string= oneStep.get();
                done=false;
            }
        }
        return string;
    }

    /**
     * Returns the cleaned string if there was something to clean, and otherwise None.
     */
    public static Optional<String> cleanOneStep(String string) throws ParseException {

        // (A) -> A
        Matcher matcher0 = singletonBracket.matcher(string);
        if(matcher0.find()){
            int start = matcher0.start(1);
            int end = findClosingBracket(string, start);
            string = string.substring(0,start)+string.substring(start+1,end)+string.substring(end+1);
            return Optional.of(string);
        }

        // A and (B and C) --> A and B and C
        Matcher matcher1 = andAnd.matcher(string);
        if(matcher1.find()){
            int start = matcher1.start(1);
            int end = findClosingBracket(string, start);
            string = string.substring(0,start)+string.substring(start+1,end)+string.substring(end+1);
            return Optional.of(string);
        }

        // A and (r some B) --> A and r some B
        Matcher matcher2 = andSome.matcher(string);
        if(matcher2.find()){
            int start = matcher2.start(1);
            int end = findClosingBracket(string, start);
            string = string.substring(0,start)+string.substring(start+1,end)+string.substring(end+1);
            return Optional.of(string);
        }

        // (r some A) and B --> r some A and B
        Matcher someMatcher = some.matcher(string);
        while(someMatcher.find()){
            int start = someMatcher.start(1);
            int end = findClosingBracket(string,start);

           /* String subString = string.substring(start,end);

            System.out.println("Suspicous find: "+subString);

            String before = string.substring(0, start-1);

            System.out.println("Before: "+before);
*/

            if(start==0 || !string.substring(0,start-1).endsWith("some")){
                string = string.substring(0,start)+string.substring(start+1,end)+string.substring(end+1);
                return Optional.of(string);
            }
        }

        // (A and B) and C ---> A and B and C
        Matcher andMatcher = and.matcher(string);
        while(andMatcher.find()){ // this case needs to match at the beginning
            int start = andMatcher.start(1);
            int end = findClosingBracket(string,start);
            // we need to determine whether the  surrounding brackets can be removed
            // this is the case if there is no "some" before
            if(start==0 || !string.substring(0,start-1).endsWith("some")){
                string = string.substring(0,start)+string.substring(start+1,end)+string.substring(end+1);
                return Optional.of(string);
            }
        }


        return Optional.empty();
    }

    private static String removeNested(String string) {
        String oldString = "";
        while(!string.equals(oldString)){
            oldString=string;
            string = string.replaceAll("\\([^\\)]*\\)", "");
        }
        return string;
    }

    private static int findClosingBracket(String string, int positionStart) throws ParseException {
        assert(string.charAt(positionStart)=='(');
        int openedBrackets = 1;
        int currentPosition = positionStart;
        while(currentPosition<string.length()){
            currentPosition+=1;
            if(string.charAt(currentPosition)=='('){
                openedBrackets++;
            } else if(string.charAt(currentPosition)==')') {
                openedBrackets--;
                if(openedBrackets==0)
                    return currentPosition;
            }
        }
        assert openedBrackets>0;
            throw new ParseException("Bracket at position "+positionStart+" never closed: "+string, positionStart);
    }


    private static int findOpeningBracket(String string, int positionEnd) throws ParseException {
        assert(string.charAt(positionEnd)==')');
        int closedBrackets = 1;
        int currentPosition = positionEnd;
        while(currentPosition>0){
            currentPosition-=1;
            if(string.charAt(currentPosition)==')'){
                closedBrackets++;
            } else if(string.charAt(currentPosition)=='(') {
                closedBrackets--;
                if(closedBrackets==0)
                    return currentPosition;
            }
        }
        assert closedBrackets>0;
        throw new ParseException("Bracket at position "+positionEnd+" never opened: "+string, positionEnd);
    }
}
