/*
 * Copyright (C) 2013 ECOSUR, Andrew Waterman
 *
 * Licensed under the Academic Free License v. 3.0.
 * http://www.opensource.org/licenses/afl-3.0.php
 */

package mx.ecosur.multigame.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

/**
 * The user class is a JPA entity that simply stores user data in the application
 * data-source for use during authentication and authorization.
 */
@Entity
public class User {

    private int id;

    private String username;

    private String password;

    private String email;

    private String firstname;

    private String lastname;

    private UUID uid;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(nullable = false, unique = true)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(nullable = false)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(nullable = false)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(nullable = false)
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @Column(nullable = false)
    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    @Column(nullable = true)
    public String getUid() {
        return uid.toString();
    }

    public void setUid(String uidval) {
        this.uid = UUID.fromString(uidval);
    }
}
