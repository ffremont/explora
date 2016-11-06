/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.simfiles;

import java.util.List;

/**
 *
 * @author florent
 */
public class SimFolder {
    private String path;
    private long freeSpace;
    private long totalSpace;
    private float ratioSpace;
    
    List<SimFile> files;

    public SimFolder(String path, long freeSpace, long totalSpace, List<SimFile> files) {
        this.path = path;
        this.freeSpace = freeSpace;
        this.totalSpace = totalSpace;
        this.files = files;
        this.ratioSpace = (float) (100 - (totalSpace > 0 ? Math.round(((float)freeSpace/totalSpace)*10000) / 100.0 : 0.0));
    }

    public List<SimFile> getFiles() {
        return files;
    }

    public void setFiles(List<SimFile> files) {
        this.files = files;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public long getTotalSpace() {
        return totalSpace;
    }

    public float getRatioSpace() {
        return ratioSpace;
    }


}
