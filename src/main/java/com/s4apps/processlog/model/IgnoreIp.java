package com.s4apps.processlog.model;

import jakarta.persistence.*;

@Entity
@Table(name = "IgnoreIps")
public class IgnoreIp {

    @Id
    @Column(name = "ip", length = 32, nullable = false)
    private String ip;

    @Column(name = "description", length = 200)
    private String description;

    // Getters and setters can be generated if you'd like.
}