package edu.cmu.cs.lti.neilson.annotation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/4/15
 * Time: 4:43 PM
 * <p/>
 * This class convert the annotations in Nielsen (2005) to the recent corpus format by Bos & Spenader (2007)
 * <p/>
 * [1] A corpus-based study of verb phrase ellipsis identification and resolution, Nielsen, 2005, King's College London
 * <p/>
 * [2] An annotated corpus for the analysis of VP ellipsis, Johan Bos and Jennifer Spenader, 2001,
 * Language Resource and Evaluation 45(4): 463-494
 * <p/>
 * This resulting annotation files are still off by some offsets, and we need human to fix them.
 *
 * @author Zhengzhong Liu
 */
public class BNCAnnotationConverter {
    File bncTextDirectory;
    File vpeAnnotationFile;
    File outputDirectory;

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    DocumentBuilder documentBuilder;

    public BNCAnnotationConverter(File bncTextDirectory, File vpeAnnotationFile, File outputDirectory) throws
            ParserConfigurationException {
        this.bncTextDirectory = bncTextDirectory;
        this.vpeAnnotationFile = vpeAnnotationFile;
        this.outputDirectory = outputDirectory;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        documentBuilder = factory.newDocumentBuilder();
    }

    private class VpeAnnotation {
        private String docid;
        private boolean isVpe;
        private int targetLineNumber;
        private int targetWordIndex;
        private int sourceLineNumber;
        private int sourceWordBegin;
        private int sourceWordEnd;

        private String rawStr;

        public VpeAnnotation(String annotationLine, String prefix) {
            String[] parts = annotationLine.split("\\s+->|\\s+\\|\\s");

            isVpe = false;

            if (parts.length < 2) {
                return;
            }

            if (!parts[0].startsWith(prefix)) {
                return;
            }

            rawStr = annotationLine;

            String target = parts[0].substring(prefix.length());
            String source = parts[1];
            String[] targetParts = target.split("\\.");

            String section = targetParts[0];
            // Here we assume target is only one words, this works for BNC corpus.
            int targetLineNumber = Integer.parseInt(targetParts[1]);
            int targetWordIndex = Integer.parseInt(targetParts[2]);

            String[] sourceParts = source.split("\\.");

            int sourceLineNumber = Integer.parseInt(sourceParts[0]);

            String[] sourceSpan = sourceParts[1].split("-");
            int sourceWordBegin = Integer.parseInt(sourceSpan[0]);
            int sourceWordEnd = sourceWordBegin;
            if (sourceSpan.length == 2) {
                sourceWordEnd = Integer.parseInt(sourceSpan[1]);
            }

            this.docid = section;
            this.sourceLineNumber = sourceLineNumber;
            this.sourceWordBegin = sourceWordBegin;
            this.sourceWordEnd = sourceWordEnd;
            this.targetLineNumber = targetLineNumber;
            this.targetWordIndex = targetWordIndex;
            isVpe = true;
        }

        public String toString() {
            return String.format("Doc : %s, Target Line : %d, Target Word : %d, Source Line : %d, Source Word Begin :" +
                            " %d, Source Word End : %d", docid, targetLineNumber, targetWordIndex, sourceLineNumber,
                    sourceWordBegin, sourceWordEnd);
        }
    }

    private class BNCWord {
        private String surface;
        private int startChar;
        private int endChar;

        public BNCWord(String surface, int begin, int end) {
            this.surface = surface;
            this.startChar = begin;
            this.endChar = end;
        }

        public String toString() {
            return String.format("%s_(%d:%d)", surface, startChar, endChar);
        }
    }

    /**
     * Convert Neilson's annotation into an internal representation.
     *
     * @throws IOException
     * @throws SAXException
     */
    public void convert() throws IOException, SAXException {
        String prefix = "bnc_";

        ArrayListMultimap<String, VpeAnnotation> annotations = ArrayListMultimap.create();

        for (String l : FileUtils.readLines(vpeAnnotationFile)) {
            VpeAnnotation anno = new VpeAnnotation(l, prefix);
            if (anno.isVpe) {
                annotations.put(anno.docid, anno);
            }
        }
        convert(annotations);
    }

    /**
     * Convert the BNC xml into raw text, and VPE annotations as Brat annotation format.
     *
     * @param annotationsBySec The organized vpe annotations by Neilson.
     * @throws IOException
     * @throws SAXException
     */
    private void convert(ArrayListMultimap<String, VpeAnnotation> annotationsBySec) throws IOException, SAXException {
        String tokenizedTextSubPath = "tokenized";
        String rawSubPath = "raw";
        String annSubPath = "ann";
        String potentialErrorPath = "errors.txt";

        List<String> potentialErrors = new ArrayList<>();
        File errorOut = joinPathsAsFile(outputDirectory.getPath(), potentialErrorPath);

        Files.walk(bncTextDirectory.toPath()).filter(p -> p.toString().contains(".xml")).forEach(p -> {
            logger.info("Converting " + p);

            String sectionName = FilenameUtils.removeExtension(p.getFileName().toString());
            File tokenizedOut = joinPathsAsFile(outputDirectory.getPath(), tokenizedTextSubPath, sectionName + ".txt");
            File rawOut = joinPathsAsFile(outputDirectory.getPath(), rawSubPath, sectionName + ".txt");
            File annOut = joinPathsAsFile(outputDirectory.getPath(), annSubPath, sectionName + ".ann");

            Collection<VpeAnnotation> annotations = annotationsBySec.get(sectionName);

            File bncTextFile = p.toFile();

            StringBuilder tokenizedText = new StringBuilder();
            StringBuilder rawText = new StringBuilder();

            Table<Integer, Integer, BNCWord> bncWordById = HashBasedTable.create();
            NodeList sentNodes = null;
            try {
                sentNodes = documentBuilder.parse(bncTextFile).getElementsByTagName("s");
                for (int sentIndex = 0; sentIndex < sentNodes.getLength(); sentIndex++) {
                    // The sentence number used in the annotation started from 1.
                    int sentId = sentIndex + 1;
                    tokenizedText.append(sentId).append("\t");

                    Node sentNode = sentNodes.item(sentIndex);

                    NodeList wordNodes = sentNode.getChildNodes();
                    for (int wordIndex = 0; wordIndex < wordNodes.getLength(); wordIndex++) {
                        // The word number used in the annotation started from 1.
                        // The word nubmer are actually wrong because we didn't handle multi-word expression.
                        int wordId = wordIndex + 1;
                        Node wordNode = wordNodes.item(wordIndex);
                        String wordText = wordNode.getTextContent();
                        int offset = rawText.length();
                        tokenizedText.append(wordId).append(":").append(wordText);
                        rawText.append(wordText);
                        String trimmedWordText = wordText.trim();
                        bncWordById.put(sentId, wordId,
                                new BNCWord(trimmedWordText, offset, offset + trimmedWordText.length()));
                    }
                    tokenizedText.append("\n");
                    rawText.append("\n");
                }
                FileUtils.write(tokenizedOut, tokenizedText.toString());
                FileUtils.write(rawOut, rawText.toString());

            } catch (SAXException | IOException e) {
                e.printStackTrace();
            }

            List<String> bratFormatAnnotations = annotationToBrat(annotations, bncWordById, rawText.toString(),
                    potentialErrors);

            try {
                FileUtils.writeLines(annOut, bratFormatAnnotations);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        FileUtils.writeLines(errorOut, potentialErrors);
    }

    /**
     * Conver the vpe annotations into Brat's standoff format.
     *
     * @param vpeAnnotations List of annotations recorded by sentence and word index.
     * @param bncWordByIndex A table that map sentence and word index to the word.
     * @return The Brat format VPE annotations.
     */
    private List<String> annotationToBrat(Collection<VpeAnnotation> vpeAnnotations, Table<Integer, Integer, BNCWord>
            bncWordByIndex, String rawText, List<String> potentialErrors) {
        List<String> bratStandoffLines = new ArrayList<>();

        int nextSpanIndex = 1;
        int nextRelationIndex = 1;
        int notesCount = 1;
        for (VpeAnnotation vpeAnnotation : vpeAnnotations) {
            BNCWord targetWord = bncWordByIndex.get(vpeAnnotation.targetLineNumber, vpeAnnotation.targetWordIndex);
            BNCWord sourceBeginWord = bncWordByIndex.get(vpeAnnotation.sourceLineNumber, vpeAnnotation.sourceWordBegin);
            BNCWord sourceEndWord = bncWordByIndex.get(vpeAnnotation.sourceLineNumber, vpeAnnotation.sourceWordEnd);

            String targetSpanId = "T" + nextSpanIndex++;
            String sourceSpanId = "T" + nextSpanIndex++;
            String relationId = "R" + nextRelationIndex++;

            if (targetWord == null) {
                System.out.println("Cannot find target " + vpeAnnotation.targetLineNumber + " " + vpeAnnotation
                        .targetWordIndex);
                potentialErrors.add(vpeAnnotation.toString());
                continue;
            }

            bratStandoffLines.add(String.format("%s\tTarget %s %s\t%s", targetSpanId, targetWord.startChar,
                    targetWord.endChar, rawText.substring(targetWord.startChar, targetWord.endChar)));
            bratStandoffLines.add(String.format("#%d\tAnnotatorNotes %s\t%s", notesCount++, targetSpanId,
                    vpeAnnotation.rawStr));

            if (sourceBeginWord == null || sourceEndWord == null) {
                potentialErrors.add(vpeAnnotation.toString());
                continue;
            }

            // Write down the source line.
            bratStandoffLines.add(String.format("%s\tSource %s %s\t%s", sourceSpanId, sourceBeginWord.startChar,
                    sourceEndWord.endChar, rawText.substring(sourceBeginWord.startChar, sourceEndWord.endChar)));

            // Compare the two lines.
            bratStandoffLines.add(String.format("%s\tVPE Arg1:%s Arg2:%s", relationId, targetSpanId, sourceSpanId));
        }

        return bratStandoffLines;
    }

    private String getAttributeValue(Node node, String attributeName) {
        return node.getAttributes().getNamedItem(attributeName).getNodeValue();
    }

    private static File joinPathsAsFile(String... directories) {
        if (directories.length == 1) {
            return new File(directories[0]);
        } else {
            String[] rest = Arrays.copyOfRange(directories, 1, directories.length);
            return new File(directories[0], joinPathsAsFile(rest).getPath());
        }
    }

    public static void main(String[] argv) throws IOException, ParserConfigurationException, SAXException {
        // Argv[0] is the directory of the BNC text one downloaded.
        // i.e. BNC/Texts directory

        if (argv.length < 1) {
            System.err.println("Please provide a path to the BNC text diretory.");
            System.err.println("i.e. BNC/Texts");
            throw new IllegalArgumentException("BNC text directory not provided");
        }

        BNCAnnotationConverter converter = new BNCAnnotationConverter(new File(argv[0]),
                new File("data/neilson/BNC_RASP_with_text"), new File("data/bnc_converted/"));
        converter.convert();
    }
}