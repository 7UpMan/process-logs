package com.s4apps.processlog.model;


import jakarta.persistence.*;

@Entity
@Table(name = "IgnoreServers")
public class IgnoreServer {

    @Id
    @Column(name = "server", length = 32, nullable = false)
    private String server;

    @Column(name = "description", length = 200)
    private String description;

    // Getters and setters can be generated if you'd like.
}