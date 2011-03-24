package com.baleksan.lid;


/**
 * @author <a href="mailto:baleksan@yammer-inc.com" boris/>
 */
public interface LanguageIdentifierService {
    String UNABLE_TO_DEFINE = "undefined";

    String language( String document );

    String encoding(String document);
}
