package com.example.jy.demo.fingerprint;

public class CallFprint {
    public native static int pgmChangeToXyt(String inFile, String outFile);

    public native static int fprintCompare(String ScanXytFile, String RolledXytFile);

    static {
        System.loadLibrary("fprint");
    }
}