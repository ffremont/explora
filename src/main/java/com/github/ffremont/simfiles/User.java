/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.simfiles;

/**
 *
 * @author florent
 */
public class User {
    private String id;
    private String password;
    private String directory;

    public User(String id, String password, String directory) {
        this.id = id;
        this.password = password;
        this.directory = directory;
    }
    
    public static User getDefault(){
        return new User("admin", "admin", System.getProperty("user.home"));
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
