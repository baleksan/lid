package com.baleksan.lid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * @author <a href="mailto:baleksan@yammer-inc.com" boris/>
 */
public class EncodingAnalyzer {
    public static final Logger LOG = LoggerFactory.getLogger(EncodingAnalyzer.class);

    public static final int NO_THRESHOLD = -1;

    private static final HashMap<String, String> ALIASES = new HashMap<String, String>();

    // CharsetDetector will die without a minimum amount of data.
    private static final int MIN_LENGTH = 4;

    static {
        /*
        * the following map is not an alias mapping table, but
        * maps character encodings which are often used in mislabelled
        * documents to their correct encodings. For instance,
        * there are a lot of documents labelled 'ISO-8859-1' which contain
        * characters not covered by ISO-8859-1 but covered by windows-1252.
        * Because windows-1252 is a superset of ISO-8859-1 (sharing code points
        * for the common part), it's better to treat ISO-8859-1 as
        * synonymous with windows-1252 than to reject, as invalid, documents
        * labelled as ISO-8859-1 that have characters outside ISO-8859-1.
        */
        ALIASES.put("ISO-8859-1", "windows-1252");
        ALIASES.put("EUC-KR", "x-windows-949");
        ALIASES.put("x-EUC-CN", "GB18030");
        ALIASES.put("GBK", "GB18030");
        //ALIASES.put("Big5", "Big5HKSCS");
        //ALIASES.put("TIS620", "Cp874");
        //ALIASES.put("ISO-8859-11", "Cp874");
    }

    private String defautlEncoding;

    private final int minConfidence;

    private final CharsetDetector detector;

    private final List<EncodingClue> clues;

    public EncodingAnalyzer(String defautlEncoding) {
        this.defautlEncoding = defautlEncoding;

        minConfidence = NO_THRESHOLD;
        detector = new CharsetDetector();
        clues = new ArrayList<EncodingClue>();
    }

    public String guessEncoding(String content) {
        autoDetectClues(content, true);
        return determingEncodingAfterAnalysis(defautlEncoding);
    }

    private void autoDetectClues(String content, boolean filter) {
        byte[] data = content.getBytes();

        if (minConfidence >= 0 && data.length > MIN_LENGTH) {
            CharsetMatch[] matches = null;

            // do all these in a try/catch; setText and detect/detectAll
            // will sometimes throw exceptions
            try {
                detector.enableInputFilter(filter);
                if (data.length > MIN_LENGTH) {
                    detector.setText(data);
                    matches = detector.detectAll();
                }
            } catch (Exception ex) {
                LOG.error("Exception from ICU4J (ignoring): ", ex);
            }

            if (matches != null) {
                for (CharsetMatch match : matches) {
                    addClue(match.getName(), "detect", match.getConfidence());
                }
            }
        }
    }

    private void addClue(String value, String source, int confidence) {
        if (value == null || "".equals(value)) {
            return;
        }
        value = resolveEncodingAlias(value);
        if (value != null) {
            clues.add(new EncodingClue(value, source, confidence));
        }
    }


    /**
     * Guess the encoding with the previously specified list of clues.
     *
     * @param defaultValue Default encoding to return if no encoding can be
     *                     detected with enough confidence.
     * @return Guessed encoding or defaultValue
     */
    private String determingEncodingAfterAnalysis(String defaultValue) {
        /*
        * Go down the list of encoding "clues". Use a clue if:
        *  1. Has a confidence value which meets our confidence threshold, OR
        *  2. Doesn't meet the threshold, but is the best try,
        *     since nothing else is available.
        */
        EncodingClue defaultClue = new EncodingClue(defaultValue, "default");
        EncodingClue bestClue = defaultClue;

        for (EncodingClue clue : clues) {
            String charset = clue.value;
            if (minConfidence >= 0 && clue.confidence >= minConfidence) {
                return resolveEncodingAlias(charset).toLowerCase();
            } else if (clue.confidence == NO_THRESHOLD && bestClue == defaultClue) {
                bestClue = clue;
            }
        }

        return bestClue.value.toLowerCase();
    }


    private static String resolveEncodingAlias(String encoding) {
        if (encoding == null || !Charset.isSupported(encoding))
            return null;
        String canonicalName = Charset.forName(encoding).name();
        return ALIASES.containsKey(canonicalName) ? ALIASES.get(canonicalName)
                : canonicalName;
    }

    private class EncodingClue {
        private final String value;
        private final String source;
        private final int confidence;

        // Constructor for clues with no confidence values (ignore thresholds)
        public EncodingClue(String value, String source) {
            this(value, source, NO_THRESHOLD);
        }

        public EncodingClue(String value, String source, int confidence) {
            this.value = value.toLowerCase();
            this.source = source;
            this.confidence = confidence;
        }

        @SuppressWarnings("unused")
        public String getSource() {
            return source;
        }

        @SuppressWarnings("unused")
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value + " (" + source +
                    ((confidence >= 0) ? ", " + confidence + "% confidence" : "") + ")";
        }

        public boolean isEmpty() {
            return (value == null || "".equals(value));
        }
    }

}
