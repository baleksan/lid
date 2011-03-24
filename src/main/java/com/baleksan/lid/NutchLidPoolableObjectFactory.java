package com.baleksan.lid;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * @author <a href="mailto:baleksan@yammer-inc.com" boris/>
 */
public class NutchLidPoolableObjectFactory implements PoolableObjectFactory {
    public Object makeObject() throws Exception {
        return new LanguageIdentifier();
    }

    public void destroyObject(Object obj) throws Exception {
    }

    public boolean validateObject(Object obj) {
        return true;
    }

    public void activateObject(Object obj) throws Exception {
    }

    public void passivateObject(Object obj) throws Exception {
    }
}

