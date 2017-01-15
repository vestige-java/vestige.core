package fr.gaellalire.vestige.core.url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * @author Gael Lalire
 */
public class DelegateURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private URLStreamHandlerFactory delegate;

    public void setDelegate(final URLStreamHandlerFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
        if (delegate != null) {
            return delegate.createURLStreamHandler(protocol);
        }
        return null;
    }

}
