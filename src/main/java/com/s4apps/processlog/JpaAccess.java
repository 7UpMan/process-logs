package com.s4apps.processlog;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.s4apps.processlog.model.ApacheLog;

public class JpaAccess {

    private static final int BATCH_SIZE = 1000;

    private final EntityManagerFactory emf;
    private final EntityManager em;

    private List<ApacheLog> allRowsBatch = null;
    private int allRowsIndex = 0;
    private int allRowsOffset = 0;

    public JpaAccess() {
        LoggingConfig.configure();
        DbConfig cfg = new DbConfig();
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", cfg.getUrl());
        props.put("jakarta.persistence.jdbc.user", cfg.getUser());
        props.put("jakarta.persistence.jdbc.password", cfg.getPassword());
        props.put("jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
        props.put("hibernate.hbm2ddl.auto", "validate");
        props.put("hibernate.hikari.minimumIdle", "2");
        props.put("hibernate.hikari.maximumPoolSize", "10");
        props.put("hibernate.hikari.idleTimeout", "30000");
        props.put("hibernate.hikari.connectionTimeout", "30000");
        props.put("hibernate.hikari.poolName", "processlog-hikari");

        emf = Persistence.createEntityManagerFactory("processlog", props);
        em = emf.createEntityManager();
    }

    public int insertRow(RowStringStorage rowStringStorage) {
        ensureTransaction();
        // If the record id exists, return.
        ApacheLog existing = em.find(ApacheLog.class, rowStringStorage.getId());
        if (existing != null) {
            return 0;
        }

        ApacheLog entity = new ApacheLog();
        toApacheLog(rowStringStorage, entity);
        em.persist(entity);
        return 1;
    }

    public int updateRow(RowStringStorage rowStringStorage) {
        ensureTransaction();
        ApacheLog entity = em.find(ApacheLog.class, rowStringStorage.getId());
        if (entity == null) {
            return 0;
        }

        toApacheLog(rowStringStorage, entity);
        em.merge(entity);
        return 1;
    }

    public int updateRowFlags(RowStringStorage rowStringStorage) {
        ensureTransaction();
        ApacheLog entity = em.find(ApacheLog.class, rowStringStorage.getId());
        if (entity == null) {
            return 0;
        }

        applyFlags(rowStringStorage, entity);
        em.merge(entity);
        return 1;
    }

    public int delRow(RowStringStorage rowStringStorage) {
        ensureTransaction();
        ApacheLog entity = em.find(ApacheLog.class, rowStringStorage.getId());
        if (entity == null) {
            return 0;
        }

        em.remove(entity);
        return 1;
    }

    public int delOld(LocalDateTime delBeforeDate) {
        ensureTransaction();
        return em.createQuery("DELETE FROM ApacheLog a WHERE a.date < :cutoff")
                .setParameter("cutoff", delBeforeDate)
                .executeUpdate();
    }

    public RowStringStorage getAllRows(ConfigData cd) {
        if (allRowsBatch == null || allRowsIndex >= allRowsBatch.size()) {
            loadNextBatch();
            if (allRowsBatch.isEmpty()) {
                return null;
            }
        }

        ApacheLog entity = allRowsBatch.get(allRowsIndex++);
        return new RowStringStorage(cd, entity);
    }

    public List<String> getIgnoreIps() {
        return em.createQuery("SELECT i.ip FROM IgnoreIp i", String.class)
                .getResultList();
    }

    public List<String> getIgnoreMethods() {
        return em.createQuery("SELECT i.method FROM IgnoreMethod i", String.class)
                .getResultList();
    }

    public List<String> getIgnoreServers() {
        return em.createQuery("SELECT i.server FROM IgnoreServer i", String.class)
                .getResultList();
    }

    public List<String> getIgnoreUrls() {
        return em.createQuery("SELECT i.url FROM IgnoreUrl i", String.class)
                .getResultList();
    }

    public List<String> getDeleteIps() {
        return em.createQuery("SELECT d.ip FROM DeleteIp d", String.class)
                .getResultList();
    }

    public List<String> getDeleteMethods() {
        return em.createQuery("SELECT d.method FROM DeleteMethod d", String.class)
                .getResultList();
    }

    public List<String> getDeleteServers() {
        return em.createQuery("SELECT d.server FROM DeleteServer d", String.class)
                .getResultList();
    }

    public List<String> getDeleteUrls() {
        return em.createQuery("SELECT d.url FROM DeleteUrl d", String.class)
                .getResultList();
    }

    public long countDeleteIpMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, DeleteIp d WHERE al.ip = d.ip",
                Long.class)
                .getSingleResult();
    }

    public long countDeleteUrlMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, DeleteUrl d "
                + "WHERE substring(al.url, 1, length(d.url)) = d.url",
                Long.class)
                .getSingleResult();
    }

    public long countDeleteServerMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, DeleteServer d WHERE al.server = d.server",
                Long.class)
                .getSingleResult();
    }

    public long countDeleteMethodMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, DeleteMethod d "
                + "WHERE substring(al.method, 1, length(d.method)) = d.method",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreIpMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, IgnoreIp ig WHERE al.ip = ig.ip",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreIpFlagged() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al WHERE al.ignoreIp = true",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreUrlMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, IgnoreUrl ig "
                + "WHERE substring(al.url, 1, length(ig.url)) = ig.url",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreUrlFlagged() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al WHERE al.ignoreUrl = true",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreServerMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, IgnoreServer ig WHERE al.server = ig.server",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreServerFlagged() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al WHERE al.ignoreServer = true",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreMethodMatches() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al, IgnoreMethod ig WHERE al.method = ig.method",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreMethodFlagged() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al WHERE al.ignoreMethod = true",
                Long.class)
                .getSingleResult();
    }

    public long countIgnoreReasonMismatch() {
        return em.createQuery(
                "SELECT COUNT(al) FROM ApacheLog al WHERE "
                + "(CASE WHEN al.ignoreIp = true THEN 2 ELSE 0 END) "
                + "+ (CASE WHEN al.ignoreUrl = true THEN 4 ELSE 0 END) "
                + "+ (CASE WHEN al.ignoreServer = true THEN 8 ELSE 0 END) "
                + "+ (CASE WHEN al.ignoreMethod = true THEN 16 ELSE 0 END) "
                + "<> al.ignoreReason",
                Long.class)
                .getSingleResult();
    }

    public void commit() {
        EntityTransaction tx = em.getTransaction();
        if (tx.isActive()) {
            tx.commit();
        }
    }

    public void close() {
        try {
            commit();
        } finally {
            em.close();
            emf.close();
        }
    }

    private void ensureTransaction() {
        EntityTransaction tx = em.getTransaction();
        if (!tx.isActive()) {
            tx.begin();
        }
    }

    private void loadNextBatch() {
        TypedQuery<ApacheLog> query = em.createQuery(
                "SELECT a FROM ApacheLog a ORDER BY a.id",
                ApacheLog.class);
        query.setFirstResult(allRowsOffset);
        query.setMaxResults(BATCH_SIZE);
        allRowsBatch = query.getResultList();
        allRowsIndex = 0;
        allRowsOffset += allRowsBatch.size();
    }

    private void toApacheLog(RowStringStorage rowStringStorage, ApacheLog entity) {
        entity.setId(rowStringStorage.getId());
        entity.setIp(blankToNull(rowStringStorage.getIp()));
        entity.setDate(parseDateTime(rowStringStorage.getDate()));
        entity.setMethod(blankToNull(rowStringStorage.getMethod()));
        entity.setUrl(blankToNull(rowStringStorage.getUrl()));
        entity.setQueryString(blankToNull(rowStringStorage.getQueryString()));
        entity.setResponse(parseInteger(rowStringStorage.getResponse()));
        entity.setSize(parseInteger(rowStringStorage.getSize()));
        entity.setServer(blankToNull(rowStringStorage.getServer()));
        entity.setServer2(blankToNull(rowStringStorage.getServer2()));
        entity.setBrowser(blankToNull(rowStringStorage.getBrowser()));
        applyFlags(rowStringStorage, entity);
    }

    private void applyFlags(RowStringStorage rowStringStorage, ApacheLog entity) {
        entity.setIgnoreReason(rowStringStorage.getIgnoreReason());
        entity.setIgnoreIp(rowStringStorage.ignoreIp());
        entity.setIgnoreUrl(rowStringStorage.ignoreUrl());
        entity.setIgnoreServer(rowStringStorage.ignoreServer());
        entity.setIgnoreMethod(rowStringStorage.ignoreMethod());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return null;
        }
        return value;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank() || text.equals("-")) {
            return null;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Date parsedDate = dateFormat.parse(text);
            return parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ex) {
            return null;
        }
    }
}
