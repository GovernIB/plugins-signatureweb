package org.fundaciobit.plugins.signatureweb.tester;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestTransactionInfo {

    private static final Pattern PATH_PATTERN
            = Pattern.compile("(?<base>/(?<ssid>.+?)/(?<index>.+?))/(?<query>.+)");

    private final String query;
    private final String ssid;
    private final int index;
    private final String baseRelativa;
    private final String baseAbsoluta;

    public RequestTransactionInfo(HttpServletRequest request) {
        Matcher matcher = PATH_PATTERN.matcher(request.getPathInfo());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("requestURI no té el format esperat: " + request.getRequestURI());
        }
        this.ssid = matcher.group("ssid");
        this.index = Integer.parseInt(matcher.group("index"));
        this.query = matcher.group("query");
        this.baseRelativa = request.getContextPath() + request.getServletPath() + matcher.group("base");
        this.baseAbsoluta = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + this.baseRelativa;
    }

    public RequestTransactionInfo(String ssid, int index, String servletPath, HttpServletRequest request) {
        this.ssid = ssid;
        this.index = index;
        this.query = null;
        this.baseRelativa = request.getContextPath() + servletPath + "/" + ssid + "/" + index;
        this.baseAbsoluta = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                + this.baseRelativa;
    }

    public String getQuery() {
        return query;
    }

    public String getSsid() {
        return ssid;
    }

    public int getIndex() {
        return index;
    }

    public String getBaseRelativa() {
        return baseRelativa;
    }

    public String getBaseAbsoluta() {
        return baseAbsoluta;
    }
}
