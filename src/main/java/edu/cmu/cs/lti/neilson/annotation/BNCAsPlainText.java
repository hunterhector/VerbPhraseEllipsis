package edu.cmu.cs.lti.neilson.annotation;

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
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/4/15
 * Time: 4:43 PM
 * <p/>
 * This class convert the BNC text as plain text only. These text will match the provided annotation. This seems to
 * be much faster than the python equivalent.
 *
 * @author Zhengzhong Liu
 */
public class BNCAsPlainText {
    File bncTextDirectory;
    File outputDirectory;

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    DocumentBuilder documentBuilder;

    public BNCAsPlainText(File bncTextDirectory, File outputDirectory) throws
            ParserConfigurationException {
        this.bncTextDirectory = bncTextDirectory;
        this.outputDirectory = outputDirectory;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        documentBuilder = factory.newDocumentBuilder();
    }

    private void convert() throws IOException, SAXException {
        String rawSubPath = "bnc_raw_java";

        Files.walk(bncTextDirectory.toPath()).filter(p -> p.toString().contains(".xml")).forEach(p -> {
            logger.info("Writing " + p);

            String sectionName = FilenameUtils.removeExtension(p.getFileName().toString());
            File rawOut = joinPathsAsFile(outputDirectory.getPath(), rawSubPath, sectionName + ".txt");

            File bncTextFile = p.toFile();

            StringBuilder rawText = new StringBuilder();

            NodeList sentNodes = null;
            try {
                sentNodes = documentBuilder.parse(bncTextFile).getElementsByTagName("s");
                for (int sentIndex = 0; sentIndex < sentNodes.getLength(); sentIndex++) {
                    Node sentNode = sentNodes.item(sentIndex);
                    NodeList wordNodes = sentNode.getChildNodes();
                    for (int wordIndex = 0; wordIndex < wordNodes.getLength(); wordIndex++) {
                        Node wordNode = wordNodes.item(wordIndex);
                        String wordText = wordNode.getTextContent();
                        rawText.append(wordText);
                    }
                    rawText.append("\n");
                }
                FileUtils.write(rawOut, rawText.toString());

            } catch (SAXException | IOException e) {
                e.printStackTrace();
            }
        });
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

        BNCAsPlainText converter = new BNCAsPlainText(new File(argv[0]), new File("data/bnc_converted/"));
        converter.convert();
    }
}