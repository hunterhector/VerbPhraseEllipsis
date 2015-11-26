package edu.cmu.cs.lti.neilson.baseline;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This baseline is directly copied from Nelson's code to make sure we follow its experiments exactly.
 */
public class NielsenBaseline {

    FMeasure eval;

    public NielsenBaseline() {
        eval = new FMeasure();
    }

    public class FMeasure {
        int tp = 0;
        int fp = 0;
        int fn = 0;

        public void addTp(int val) {
            tp += val;
        }

        public void addFp(int val) {
            fp += val;
        }

        public void addFn(int val) {
            fn += val;
        }

        public double safeDiv(double divider, double dividend) {
            if (dividend != 0) {
                return divider / dividend;
            } else {
                return 0;
            }
        }

        public double getPrecision() {
            return safeDiv(tp, (tp + fp));
        }

        public double getRecall() {
            return safeDiv(tp, (tp + fn));
        }

        public double getF1() {
            double prec = getPrecision();
            double recall = getRecall();
            return safeDiv(2 * prec * recall, (prec + recall));
        }

        public String toString() {
            return String.format("Prec\tRecall\tF1\n%.4f\t%.4f\t%.4f\n", getPrecision(), getRecall(), getF1());
        }
    }

    public FMeasure getEval() {
        return eval;
    }

    /**
     * Given an array of words, an array of pos tags, the target word to test. Return whether this is a VPE case
     * according to the heuristics.
     *
     * @param words The list of words.
     * @param tags  The list of pos tags.
     * @param j     The target word index.
     * @return Whether the word at the target index is a VPE target.
     */
    public boolean is_LDC_Baseline(String[] words, String[] tags, int j) {
        //boolean result = false;

        // definition of AUX, excluding MD
        String for_search =
                "(V\\w{1,2})|(MD)|(JJ\\w{0,1})|(NN\\w{0,1})|(IN)|(DT)|(CD)|(RP)|(NNP)|(NNPS)|(WP\\w{0,1})|(WRB)";
        //"(V\\w\\w)|(AJ\\w)|(N\\w\\w)|(PRP)|(D\\w\\w)|(CRD)|(AVP)|(PNI)|(PNX)|(PNQ)|(ORD)|(AVQ)|(PRF)";
        Pattern for_verb_match = Pattern.compile(for_search);
        String back_search = "(do)|(Do)|(have)|(Have)|('ve)";
        //(VDB)|(VHB)|(VHI)"; // needs to be changed too DTQ, TO0, VV\\w
        Pattern back_verb_match = Pattern.compile(back_search);

        int for_range = 7; // how many words to look around for verbs
        int back_range = 3;
        boolean semi_aux = false;
        boolean punct_prox = false; // check for a punct within range
        boolean punct_stop = true; // stop forw search if punct encountered

        // reverted to PUN check for the moment. the (;) here is wrong as it covers
        // &equo; etc as well under a .find()
        // need more empirical backup for removing dash and hellip.
        //Pattern comma_p = Pattern.compile("(,)|(;)|(!)|(\\?)|(&mdash;)|(&hellip;)");
        boolean clear_asides = false; // clear (, bla,)
        boolean immediate_check = true; // look for lexical clues (so did, as did etc)
        boolean AV0_check = true; //immediate area check for AV0

        boolean detect_bounds = true; // restrict search to the S(?)-struct the AUX is in

        boolean ellipsis_found = false;

        if (is_LDC_aux(words[j], tags[j])) {
            ellipsis_found = true;

//            System.out.println("Candidate is [" + words[j] + " " + tags[j] + "]");

            // Here you only check 2 for backward
            for (int k = j - 1; k > ((j - back_range > 0) ? (j - back_range) : 0); k--) {
//                System.out.println("Checking backward word : " + words[k]);
                Matcher m = back_verb_match.matcher(words[k].toString());
                if (m.find()) {
                    ellipsis_found = false;
//                    System.out.println("Discarded due to back " + k + " (" + tags[k] + ")");
                }
                if (punct_stop) {
                    if (tags[k].matches("(comma)|(period)|(semicolon)|(CC)|(IN)"))
                        //considering punctutation separation
                        //(PUN)|(CJC)|(CJS)
                        break;
                }
            }

            // Here you only check 6 for forward
            for (int k = j + 1; k < ((j + for_range < tags.length) ? (j + for_range) : tags.length); k++) {
//                System.out.println("Checking forward tag : " + tags[k]);
                Matcher m = for_verb_match.matcher(tags[k].toString());
                if (m.find()) {
                    ellipsis_found = false;
//                    System.out.println("Discarded due to forw " + k + " (" + tags[k] + ")");
                }
                if (punct_stop) {

          /*m = comma_p.matcher(sent_words[k].toString());
            if(m.find()) //considering punctutation separation*/
                    if (tags[k].matches("(comma)|(period)|(semicolon)|(CC)|(IN)")) {
                        //punct_found = true;
                        break;
                    }
                }
            }

            if (AV0_check) {
                // separate check for AV0, PNP

                if (j < tags.length - 1) {
                    if (tags[j + 1].equals("RB\\w{0,1}")) //|| sent_tags[j+1].equals("PNP")) // but not "so"
                    {
//                        System.out.println("iiiiiiii Immediate AV0 no-go clue iiiiiiiii");
                        ellipsis_found = false;
                    }
                }
            }

            if (immediate_check) // cgeck for lexical giveaways, independent of all else
            { // ignore punct_prox ftm

                //String prev_match = "(VDB)|(VDD)|(VDI)|(VDZ)|(VBD)|(VBN)|(VDG)|(VDN)";

                String prev_imm_match =
                        "(am)|(Am)|(are)|(Are)|('m)|('re)|(be)|(Be)(do)|(Do)|(was)|(Was)|"
                                + "(were)|(Were)|(is)|(Is)|('s)|(did)|(Did)|(does)|(Does)|(have)|(Have)|('ve)|(had)|" +
                                "(Had)|('d)|"
                                + "(has)|(Has)";
                //"(VBB)|(VBD)|(VBZ)|(VDB)|(VDD)|(VDI)|(VDZ)|(VHB)|(VHD)|(VHI)|(VHN)|(VHZ)|(VM0)";
                if (words[j].matches(prev_imm_match) || tags[j].matches("MD")) {
                    if (j > 0) {
                        if (words[j - 1].matches("(so)|(So)|(as)|(As)")) {

//                            System.out.println("lllllll Immediate lexical clue lllllll");
                            ellipsis_found = true;

                            if (j < tags.length - 1) {
                                Pattern stop = Pattern.compile("(RB)|(V\\w{0,1})|(MD)");
                                Matcher m = stop.matcher(tags[j + 1].toString());
                                if (m.find()) {
                                    ellipsis_found = false;
//                                    System.out.println("lllllll discarded due to AV0|AJ0 after lllllll");
                                }
                            }
                        }
                    }
                }

                if (j < tags.length - 1) {
                    if (words[j + 1].equals("so")) {
//                        System.out.println("lllllll Immediate lexical clue lllllll");
                        ellipsis_found = true;

                        if (j < tags.length - 2) {
                            Pattern stop = Pattern.compile("(RB)|(JJ)|(V\\w{0,1})|(DT)");
                            Matcher m = stop.matcher(words[j + 2].toString());
                            if (m.find()) {
                                ellipsis_found = false;
//                                System.out.println("lllllll discarded due to AV0|AJ0 after lllllll");
                            }
                        }
                    }
                }
            }
        }
        return ellipsis_found;
    }

    public static boolean is_LDC_aux(String word, String tag) {
        String aux_match =
                "(am)|(Am)|(are)|(Are)|('m)|('re)|(be)|(Be)|(was)|(Was)"
                        + "|(were)|(Were)|(being)|(Being)|(been)|(Been)|(is)|(Is)|('s)"
                        + "|(do)|(Do)|(did)|(Did)|(doing)|(Doing)|(done)|(Done)|(does)|(Does)"
                        + "|(have)|(Have)|('ve)|(had)|(Had)|('d)|(having)|(Having)|(has)|(Has)"
                        + "|(ai)|(Ai)|(ca)|(Ca)|(wo)|(Wo)|(sha)|(Sha)";

        if (word.matches(aux_match))
            return true;

        if (tag.equals("MD"))
            return true;

        if (tag.matches("TO"))
            return true;

        return false;
    }

    public static void run_table(String tableName) throws IOException {
        Scanner scanner = new Scanner(System.in);
        NielsenBaseline bl = new NielsenBaseline();
        while (true) {
            System.out.println("Press Enter to parse " + tableName);
            scanner.nextLine(); // Just read an Enter.

            List<String> lines = Files.readAllLines(Paths.get(tableName), Charset.defaultCharset());

            int count = 0;
            for (String l : lines) {
                if (l.split("\t").length >= 6) {
                    count++;
                }
            }

            String[] tags = new String[count];
            String[] words = new String[count];

            int index = 0;
            for (String l : lines) {
                String[] fs = l.split("\t");
                if (fs.length < 6) {
                    continue;
                }
                tags[index] = fs[5];
                words[index] = fs[1];
                index++;
            }

            for (String tag : tags) {
                System.out.print(tag + " ");
            }
            System.out.println();

            for (String word : words) {
                System.out.print(word + " ");
            }
            System.out.println();

            int num_vpe_found = 0;
            for (int i = 0; i < tags.length; i++) {
                if (bl.is_LDC_Baseline(words, tags, i)) {
                    System.out.println("This is a vpe");
                    System.out.println(String.format("%s at %d", words[i], i));
                    num_vpe_found++;
                }
            }

            if (num_vpe_found > 0)
                System.out.println(num_vpe_found + " VPE found in sentence");
            else
                System.out.println("VPE Not Found");
        }
    }

    public void evalNext(StanfordCoreNLP pipeline, File inputFile, File goldFile) throws IOException {
        Set<Pair<Integer, Integer>> goldTargets = getGoldStandard(goldFile);

        String text = FileUtils.readFileToString(inputFile);
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        Set<Pair<Integer, Integer>> targets = new HashSet<>();

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            String[] words = new String[tokens.size()];
            String[] tags = new String[tokens.size()];

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel tokenLabel = tokens.get(i);
                words[i] = tokenLabel.get(CoreAnnotations.TextAnnotation.class);
                tags[i] = tokenLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            }

//            System.out.println(String.format("Checking %d tokens.", tokens.size()));

            for (int i = 0; i < tokens.size(); i++) {
                if (is_LDC_Baseline(words, tags, i)) {
                    CoreLabel tokenLabel = tokens.get(i);
                    int begin = tokenLabel.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                    int end = tokenLabel.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
                    targets.add(Pair.of(begin, end));

                    System.out.println(String.format("Potential target : %s, [%d, %d]", tokenLabel.get
                            (CoreAnnotations.TextAnnotation.class), begin, end));
                }
            }
        }


        Set<Pair<Integer, Integer>> diff = new HashSet<>(goldTargets);
        diff.removeAll(targets);
        Set<Pair<Integer, Integer>> intersection = new HashSet<>(goldTargets);
        intersection.retainAll(targets);

        int fn = diff.size();
        int tp = intersection.size();
        int fp = targets.size() - tp;

        eval.addTp(tp);
        eval.addFp(fp);
        eval.addFn(fn);
    }

    public Set<Pair<Integer, Integer>> getGoldStandard(File goldFile) throws IOException {
        Set<Pair<Integer, Integer>> goldStandards = new HashSet<>();
        if (!goldFile.exists()) {
            return goldStandards;
        }
        for (String line : FileUtils.readLines(goldFile)) {
            String[] fields = line.split(" ");
            if (fields.length < 6) {
                continue;
            }
            int targetStart = Integer.parseInt(fields[1]);
            int targetEnd = Integer.parseInt(fields[2]);
            goldStandards.add(Pair.of(targetStart, targetEnd));
        }
        return goldStandards;
    }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        File inputTextDir = new File(args[0]);
        File annotationDir = new File(args[1]);

        NielsenBaseline baseline = new NielsenBaseline();

        System.out.println("Input text directory is " + inputTextDir.getPath());

        for (File inputFile : FileUtils.listFiles(inputTextDir, new String[]{"txt"}, false)) {
            System.out.println("Running baseline on " + inputFile.getName());
            String annName = inputFile.getName().replace(".txt", ".ann");
            File annoFile = new File(annotationDir, annName);
            baseline.evalNext(pipeline, inputFile, annoFile);
        }

        FMeasure fMeasure = baseline.getEval();

        System.out.println(fMeasure);
    }
}
