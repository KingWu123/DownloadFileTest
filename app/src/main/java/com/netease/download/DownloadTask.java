package com.netease.download;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.SimpleTimeZone;


/**
 * Created by king.wu on 1/29/16.
 *
 * downloadTask can be pause, resume, cancel.
 * if one add to the session, it should be execute in the future.
 */
public class DownloadTask implements Runnable, Comparable<DownloadTask> {

    /**
     * if resume state is true, it can be paused and resume.
     * if not, if you pause, it will download in the beginning.
     */
    public  enum  ResumeState{
        NOT_DETERMINED , TRUE , FALSE
    }

    //task level
    public static final int DEFAULT_LEVEL_TASK = 1;
    public static final int HIGH_LEVEL_TASK = 2;

    //task state
    public static final int INIT        = 0;
    public static final int READY       = 1;
    public static final int DOWNLOADING = 2;
    public static final int PAUSED      = 3;
    public static final int CANCELED    = 4;
    public static final int FINISHED    = 5;
    public static final int FAILED      = 6;


    //timeout and buffer size
    private static final int DEFAULT_TIMEOUT_MS = 1000 * 20;
    private static final int BUFFER_SIZE = 1024;

    //frequency of post progress
    private static final int THRESHOLD = BUFFER_SIZE * 10;
    private int  downloadByteThreshold = 0;

    private WeakReference<Thread> mExecuteThread;
    private WeakReference<DownloadSession> mDownloadSession;
    private Handler mMainThreadHandler;

    //task id
    private static long countId = 0;
    public final long id = countId++;

    private String mUrlStr;
    private String mSaveAddress;
    private DownloadListener mListener;

    private int mCurrentTimeoutMs;
    private int mWeightLevel;  //task has weightLevel, high level has external thread to execute.
    private long mTotalSize;
    private int mProgress;
    private int mState; //taskState

    private ResumeState mResumeState;


    public DownloadTask(String url, String dirName, String fileName, boolean isOverWrite,
                        DownloadListener listener){
        init(url, dirName, fileName, isOverWrite, listener);
    }

    public DownloadTask(String url, String dirName, String fileName,
                        DownloadListener listener){
        init(url, dirName, fileName, false, listener);
    }


    /**
     * all download file save in external storage, you should check it exist
     * @param url   remote url
     * @param dirName  dir name
     * @param isAppend if the file already exist, append in the end.
     *  @param listener callBack
     */
    private void init(String url, String dirName, String fileName, boolean isOverWrite,
                      DownloadListener listener){
        setTaskState(INIT, null);

        mUrlStr = url;
        setDestinationInExternalPublicDir(dirName, fileName);
        mListener = listener;
        mMainThreadHandler = new Handler(Looper.getMainLooper());

        mCurrentTimeoutMs = DEFAULT_TIMEOUT_MS;
        mTotalSize = -1;
        mProgress = 0;
        mWeightLevel = DEFAULT_LEVEL_TASK;

        mResumeState = ResumeState.NOT_DETERMINED;

        if (isOverWrite){
            File file = new File(mSaveAddress);
            if (file.exists()){
                file.delete();
            }
        }
    }

    public void setListener(DownloadListener listener){
        mListener = listener;
    }

    public int getTimeOutTime(){
        return mCurrentTimeoutMs;
    }

    public void setTimeOutTime(int timeOutMs){

        mCurrentTimeoutMs = timeOutMs;
    }

    public int getWeightLevel() {
        return mWeightLevel;
    }

    public void setWeightLevel(int mWeightLevel) {
        this.mWeightLevel = mWeightLevel;
    }

    public int getTaskState(){
        return mState;
    }

    public int getProgress(){
        return mProgress;
    }

    public ResumeState getResumeState(){
        return mResumeState;
    }


    /**
     * pause the task
     */
    public synchronized void pause() throws DownloadError{

        if (mState == DOWNLOADING && mExecuteThread != null && mExecuteThread.get() != null) {
            mExecuteThread.get().interrupt();
        }else {
            throw new DownloadError("can't paused");
        }

        setTaskState(PAUSED, null);
    }

    /**
     * resume the task
     */
    public synchronized void resume () throws DownloadError{

        if (mState == PAUSED || mState == FAILED){

            //try to execute
            if (mDownloadSession != null && mDownloadSession.get()!=null){
                mDownloadSession.get().tryToExecuteTask(this);
            }

        }else {
            throw new DownloadError("can't resume");
        }
    }

    /**
     * cancel the task
     * --todo--
     */
    public synchronized void cancel() throws DownloadError{

        if (mState != INIT && mState != READY && mState != FINISHED
                && mDownloadSession != null && mDownloadSession.get() != null){

            //if task is downloading, pause it first
            if (mState == DOWNLOADING && mExecuteThread != null && mExecuteThread.get() != null) {
                mExecuteThread.get().interrupt();
            }

            boolean isRemoveSuccess = mDownloadSession.get().remove(this);
            if (!isRemoveSuccess){
                throw new DownloadError("can't cancel");
            }else {

                setTaskState(CANCELED, null);
                return;
            }
        }
        throw new DownloadError("can't cancel");
    }


    @Override
    public void run() {

        if ( mState == DOWNLOADING || mState == FINISHED || mState == INIT){

            downloadFailed(new DownloadError("state error, can't download"));
            return;
        }

        setTaskState(DOWNLOADING, null);
        mExecuteThread = new WeakReference<>(Thread.currentThread()) ;

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            URL url = new URL(mUrlStr);
            urlConnection = (HttpURLConnection) url.openConnection();


            long alreadyDownloadedByte = getAlreadyDownloadedByte();

            //init urlConnection
            urlConnection.setConnectTimeout(mCurrentTimeoutMs);
            urlConnection.setReadTimeout(mCurrentTimeoutMs);
            urlConnection.setRequestProperty("Range", "bytes=" + alreadyDownloadedByte + "-" + "");


            int responseCode = urlConnection.getResponseCode();

            if (responseCode == -1) {
                throw new IllegalStateException("Could not retrieve response code from HttpUrlConnection.");
            }else if (responseCode == 416){
                throw new IllegalStateException("Requested Range not satisfiable.");
            }

            //init resumeState
            if (responseCode == HttpURLConnection.HTTP_PARTIAL
                    && urlConnection.getHeaderFields().get("Content-Range") != null){
                mResumeState = ResumeState.TRUE;
            }else {
                mResumeState = ResumeState.FALSE;
                alreadyDownloadedByte = 0;
            }

            long contentLength = urlConnection.getContentLength();
            if (contentLength != -1) {
                mTotalSize = contentLength +alreadyDownloadedByte;
            }

            //read inputStream and output to file
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
            FileOutputStream fileOutputStream = new FileOutputStream(mSaveAddress, mResumeState == ResumeState.TRUE);
            outputStream = new BufferedOutputStream(fileOutputStream);

            int bytesRead = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!Thread.currentThread().isInterrupted()
                    && (bytesRead = inputStream.read(buffer)) != -1) {

                outputStream.write(buffer, 0, bytesRead);

                alreadyDownloadedByte += bytesRead;

                downloadProgress(bytesRead, alreadyDownloadedByte, mTotalSize);
            }

            outputStream.flush();

            //if all bytes read successfully, download finished
            if (!Thread.currentThread().isInterrupted()){
                downloadFinished();
            }

        }catch(SocketTimeoutException e){

            downloadFailed(new DownloadError(e.getMessage()));
        }catch (MalformedURLException e) {

            downloadFailed(new DownloadError(e.getMessage()));
        } catch (FileNotFoundException e) {

            downloadFailed(new DownloadError(e.getMessage()));
        }
        catch (IOException e) {

            downloadFailed(new DownloadError(e.getMessage()));
        } catch (Exception e){

            downloadFailed(new DownloadError(e.getMessage()));
        }
        finally {

            if (urlConnection != null){
                urlConnection.disconnect();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public int compareTo(DownloadTask another) {
        //if two task has the same url and saveAddress, it is equal
        if (this.mUrlStr.equals(another.mUrlStr)
                && this.mSaveAddress.equals(another.mSaveAddress)){
            return 0;
        }
        return -1;
    }


    /**
     * you can't call this method yourself
     * @param state taskState
     */
    void setTaskState(int state, final DownloadError error){
        mState = state;

        if (mListener == null){
            return;
        }
        switch (mState){
            case READY:
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onDownloadReady(DownloadTask.this);
                    }
                });

                break;
            case DOWNLOADING:
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.OnDownloadStarted(DownloadTask.this);
                    }
                });

                break;
            case PAUSED:

                break;
            case FINISHED:
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.OnDownloadFinished(DownloadTask.this);
                    }
                });

                break;
            case FAILED:
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.OnDownloadFailed(DownloadTask.this, error);
                    }
                });

                break;
        }
    }


    /**
     * every task has a week reference to session, this just should be called in session add task
     * @param session downloadSession
     */
    void setDownloadSession(DownloadSession session){
        mDownloadSession = new WeakReference<>(session);
    }



    //download progress
    private void downloadProgress(int byteRead, final long alreadyDownloadedByte, final long totalSize){

        downloadByteThreshold += byteRead;
        if (downloadByteThreshold > THRESHOLD) {
            downloadByteThreshold = 0;

            //post progress
            if (mListener != null && totalSize > 0){

                mProgress =  (int)(alreadyDownloadedByte * 1.0/totalSize * 100);

                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onDownloadProgress(DownloadTask.this, mProgress, alreadyDownloadedByte, totalSize);
                    }
                });

            }
        }

    }

    //download finished process
    private void downloadFinished(){

        //post progress
        if (mListener != null && mTotalSize > 0) {

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mProgress = 100;
                    mListener.onDownloadProgress(DownloadTask.this, 100, mTotalSize, mTotalSize);
                }
            });

        }


        //移除任务
        if (mDownloadSession != null && mDownloadSession.get() != null) {
            mDownloadSession.get().remove(this);
        }

        setTaskState(FINISHED, null);
    }

    //download failed process
    private void downloadFailed(final DownloadError error){

        setTaskState(FAILED, error);
    }


    /**
     * Set the local destination for the downloaded file to a path within
     * the public external storage directory (as returned by
     * {@link Environment#getExternalStoragePublicDirectory(String)}).
     * <p>
     * The downloaded file is not scanned by MediaScanner. But it can be
     *
     * @param dirType the directory type to pass to {@link Environment#getExternalStoragePublicDirectory(String)}
     * @param subPath the path within the external directory, including the
     *            destination filename
     *
     * @throws IllegalStateException If the external storage directory
     *             cannot be found or created.
     */
    private void setDestinationInExternalPublicDir(String dirType, String subPath) {
        File file = Environment.getExternalStoragePublicDirectory(dirType);
        if (file == null) {
            throw new IllegalStateException("Failed to get external storage public directory");
        } else if (file.exists()) {
            if (!file.isDirectory()) {
                throw new IllegalStateException(file.getAbsolutePath() +
                        "file already exists and is not a directory");
            }
        } else {
            if (!file.mkdirs()) {
                throw new IllegalStateException("Unable to create directory: "+
                        file.getAbsolutePath());
            }
        }
        setDestinationFromBase(file, subPath);
    }

    private void setDestinationFromBase(File base, String subPath) {
        if (subPath == null) {
            throw new NullPointerException("subPath cannot be null");
        }

        mSaveAddress = base.getAbsolutePath() + "/" + subPath;
    }


    private long getAlreadyDownloadedByte(){
        File file = new File(mSaveAddress);

        if (file.exists()){
            return file.length();
        }
        return 0;
    }

//    private long getFileTotalSize(HttpURLConnection urlConnection){
//        String[] ranges = urlConnection.getHeaderFields().get("Content-Range").get(0).split("/");
//        long totalSize = Long.parseLong(ranges[ranges.length - 1]);
//        return totalSize;
//    }



}
