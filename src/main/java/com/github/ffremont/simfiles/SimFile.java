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
public class SimFile {
    boolean isDir;
    private String filename;
    private long size;
    
    private long modified;

    public SimFile(String filename, boolean isDir) {
        this.filename = filename;
        this.isDir = isDir;
    }

    public boolean isIsDir() {
        return isDir;
    }

    public void setIsDir(boolean isDir) {
        this.isDir = isDir;
    }
    
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }
    
    
}
