package com.baleksan.lid;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:baleksan@yammer-inc.com" boris/>
 */
public class NutchLanguageIdentifier implements LanguageIdentifierService {
    private static final Logger LOG = LoggerFactory.getLogger(NutchLanguageIdentifier.class);

    private int lengthThreshold;

    private GenericObjectPool lidPool;

    public NutchLanguageIdentifier() {
        lidPool = new GenericObjectPool(new NutchLidPoolableObjectFactory());

        lengthThreshold = 150;
    }

    public String language(String content) {
        if (null == content) {
            LOG.error("Content could not be null");
            return UNABLE_TO_DEFINE;
        }

        //lid is error prone if the content length is too small, so we assume for every document
        //which is shorter then the threshold that it is English. this is a better class of errors to make.
        if (lengthCheck(content)) {
            return "en";
        }

        LanguageIdentifier lid;
        try {
            lid = (LanguageIdentifier) lidPool.borrowObject();
        } catch (Exception e) {
            LOG.error("Cannot obtain lid from the pool ", e);
            return UNABLE_TO_DEFINE;
        }

        try {
            String iso639code = lid.identify(content);

            LOG.info("Identified language as " + iso639code + " for " +
                    (content.length() < 100
                            ? content
                            : content.substring(0, Math.min(content.length(), 100)) + " ... "));

            return iso639code;
        } catch (Exception ex) {
            //todo report and fix errors as they go into the Nutch developer code base. for now log
            //todo and assume English
            LOG.error("Internal error in Nutch Lid: ", ex.getMessage());
            return UNABLE_TO_DEFINE;
        } finally {
            try {
                lidPool.returnObject(lid);
            } catch (Exception e) {
                LOG.error("Cannot return the object to pool", e);
            }
        }
    }

    @Override
    public String encoding(String document) {
        EncodingAnalyzer analyzer = new EncodingAnalyzer("utf-8");
        return analyzer.guessEncoding(document);
    }

    private boolean lengthCheck(String content) {
        return content.length() < lengthThreshold;
    }
}
