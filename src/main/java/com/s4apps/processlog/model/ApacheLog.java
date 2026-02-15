package com.s4apps.processlog.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "ApacheLogs",
    indexes = {
        @Index(name = "idx_logs_date", columnList = "date"),
        @Index(name = "idx_ip", columnList = "ip"),
        @Index(name = "method", columnList = "method"),
        @Index(name = "url", columnList = "url")
    }
)
public class ApacheLog {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "ip", length = 32)
    private String ip;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "url", length = 200)
    private String url;

    @Column(name = "queryString", length = 1000)
    private String queryString;

    @Column(name = "response")
    private Integer response;

    @Column(name = "size")
    private Integer size;

    @Column(name = "server", length = 200)
    private String server;

    @Column(name = "server2", length = 300)
    private String server2;

    @Column(name = "browser", length = 1000)
    private String browser;

    @Column(name = "ignoreReason")
    private Integer ignoreReason;

    @Column(name = "ignoreUrl", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean ignoreUrl;

    @Column(name = "ignoreServer", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean ignoreServer;

    @Column(name = "ignoreIp", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean ignoreIp;

    @Column(name = "ignoreMethod", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean ignoreMethod;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public Integer getResponse() {
        return response;
    }

    public void setResponse(Integer response) {
        this.response = response;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getServer2() {
        return server2;
    }

    public void setServer2(String server2) {
        this.server2 = server2;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public Integer getIgnoreReason() {
        return ignoreReason;
    }

    public void setIgnoreReason(Integer ignoreReason) {
        this.ignoreReason = ignoreReason;
    }

    public boolean isIgnoreUrl() {
        return ignoreUrl;
    }

    public void setIgnoreUrl(boolean ignoreUrl) {
        this.ignoreUrl = ignoreUrl;
    }

    public boolean isIgnoreServer() {
        return ignoreServer;
    }

    public void setIgnoreServer(boolean ignoreServer) {
        this.ignoreServer = ignoreServer;
    }

    public boolean isIgnoreIp() {
        return ignoreIp;
    }

    public void setIgnoreIp(boolean ignoreIp) {
        this.ignoreIp = ignoreIp;
    }

    public boolean isIgnoreMethod() {
        return ignoreMethod;
    }

    public void setIgnoreMethod(boolean ignoreMethod) {
        this.ignoreMethod = ignoreMethod;
    }

    @Override
    public String toString() {
        return "ApacheLog{" +
                "id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", date=" + date +
                ", method='" + method + '\'' + 
                ", url='" + url + '\'' +
                ", queryString='" + queryString + '\'' +
                ", response=" + response +
                ", size=" + size +
                ", server='" + server + '\'' +
                ", server2='" + server2 + '\'' +
                ", browser='" + browser + '\'' +
                ", ignoreReason=" + ignoreReason +
                ", ignoreUrl=" + ignoreUrl +
                ", ignoreServer=" + ignoreServer +
                ", ignoreIp=" + ignoreIp +
                ", ignoreMethod=" + ignoreMethod +
                '}';
    }
}
