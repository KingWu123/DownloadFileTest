package com.netease.download;

/**
 * Created by king.wu on 2/1/16.
 */
public class DownloadError extends Exception {

    public DownloadError(){

    }

    public DownloadError(String errorStr){
        super(errorStr);
    }
}
