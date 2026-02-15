package com.s4apps.processlog.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "ProcessedLogs",
    indexes = {
        @Index(name = "idx_processedlogs_apache_log_id", columnList = "apacheLogId", unique = true)
    }
)
public class ProcessedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "apacheLogId", length = 64, nullable = false)
    private String apacheLogId;

    @Column(name = "leadId", length = 64)
    private String leadId;

    // Constructors
    public ProcessedLog() {
    }

    public ProcessedLog(String apacheLogId, String leadId) {
        this.apacheLogId = apacheLogId;
        this.leadId = leadId;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApacheLogId() {
        return apacheLogId;
    }

    public void setApacheLogId(String apacheLogId) {
        this.apacheLogId = apacheLogId;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    @Override
    public String toString() {
        return "ProcessedLog{" +
                "id=" + id +
                ", apacheLogId='" + apacheLogId + '\'' +
            ", leadId='" + leadId + '\'' +
                '}';
    }
}
