/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ffremont.simfiles;

import java.text.DecimalFormat;

/**
 *
 * @author florent
 */
public class SimFile {
    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;
    
    boolean isDir;
    private String filename;
    private long size;
    private String labelSize;
    
    private long modified;

    public SimFile(String filename, boolean isDir) {
        this.filename = filename;
        this.isDir = isDir;
    }
    
    private static String convertToStringRepresentation(final long value) {
        if(value <= 0){
            return "0 octet";
        }
        final long[] dividers = new long[]{T, G, M, K, 1};
        final String[] units = new String[]{"To", "Go", "Mo", "Ko", "octets"};
        String result = null;
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(final long value,
            final long divider,
            final String unit) {
        final double result
                = divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#,##0.#").format(result) + " " + unit;
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
        this.labelSize = convertToStringRepresentation(size);
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public String getLabelSize() {
        return labelSize;
    }
}
