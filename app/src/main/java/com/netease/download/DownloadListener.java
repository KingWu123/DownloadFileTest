package com.netease.download;

/**
 * Created by king.wu on 2/1/16.
 */
public interface DownloadListener {

    void onDownloadReady(DownloadTask task);

    void OnDownloadStarted(DownloadTask task);

    void onDownloadProgress(DownloadTask task, int percent, long downloadedLength, long totalLength);

    void OnDownloadFinished(DownloadTask task);

    void OnDownloadFailed(DownloadTask task, DownloadError error);
}
