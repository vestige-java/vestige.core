package fr.gaellalire.vestige.core.url;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author Gael Lalire
 */
public class DelegateURLStreamHandler extends URLStreamHandler {

    private VestigeURLStreamHandler delegate;

    public void setDelegate(final VestigeURLStreamHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    protected boolean equals(final URL u1, final URL u2) {
        return delegate.equals(u1, u2);
    }

    @Override
    public boolean equals(final Object obj) {
        return delegate.equals(obj);
    }

    @Override
    protected int getDefaultPort() {
        return delegate.getDefaultPort();
    }

    @Override
    protected InetAddress getHostAddress(final URL u) {
        return delegate.getHostAddress(u);
    }

    @Override
    protected int hashCode(final URL u) {
        return delegate.hashCode(u);
    }

    @Override
    protected boolean hostsEqual(final URL u1, final URL u2) {
        return delegate.hostsEqual(u1, u2);
    }

    @Override
    protected URLConnection openConnection(final URL u, final Proxy p) throws IOException {
        return delegate.openConnection(u, p);
    }

    @Override
    protected void parseURL(final URL u, final String spec, final int start, final int limit) {
        delegate.parseURL(u, spec, start, limit);
    }

    @Override
    protected boolean sameFile(final URL u1, final URL u2) {
        return delegate.sameFile(u1, u2);
    }

    @Override
    protected void setURL(final URL u, final String protocol, final String host, final int port, final String authority, final String userInfo, final String path, final String query, final String ref) {
        super.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void setURL(final URL u, final String protocol, final String host, final int port, final String file, final String ref) {
        super.setURL(u, protocol, host, port, file, ref);
    }

    @Override
    protected String toExternalForm(final URL u) {
        return delegate.toExternalForm(u);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    protected URLConnection openConnection(final URL u) throws IOException {
        return delegate.openConnection(u);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

}
