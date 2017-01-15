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
public abstract class VestigeURLStreamHandler extends URLStreamHandler {

    private DelegateURLStreamHandler delegateURLStreamHandler;

    public VestigeURLStreamHandler(final DelegateURLStreamHandler delegateURLStreamHandler) {
        this.delegateURLStreamHandler = delegateURLStreamHandler;
    }

    @Override
    protected boolean equals(final URL u1, final URL u2) {
        return super.equals(u1, u2);
    }

    @Override
    protected int getDefaultPort() {
        return super.getDefaultPort();
    }

    @Override
    protected InetAddress getHostAddress(final URL u) {
        return super.getHostAddress(u);
    }

    @Override
    protected int hashCode(final URL u) {
        return super.hashCode(u);
    }

    @Override
    protected boolean hostsEqual(final URL u1, final URL u2) {
        return super.hostsEqual(u1, u2);
    }

    @Override
    protected URLConnection openConnection(final URL u, final Proxy p) throws IOException {
        return super.openConnection(u, p);
    }

    @Override
    protected void parseURL(final URL u, final String spec, final int start, final int limit) {
        super.parseURL(u, spec, start, limit);
    }

    @Override
    protected boolean sameFile(final URL u1, final URL u2) {
        return super.sameFile(u1, u2);
    }

    @Override
    protected void setURL(final URL u, final String protocol, final String host, final int port, final String authority, final String userInfo, final String path, final String query, final String ref) {
        delegateURLStreamHandler.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }

    @Override
    protected void setURL(final URL u, final String protocol, final String host, final int port, final String file, final String ref) {
        delegateURLStreamHandler.setURL(u, protocol, host, port, file, ref);
    }

    @Override
    protected String toExternalForm(final URL u) {
        return super.toExternalForm(u);
    }

    @Override
    protected abstract URLConnection openConnection(final URL u) throws IOException;

}
