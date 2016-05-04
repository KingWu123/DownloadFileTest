package com.netease.download;

import android.util.Log;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by king.wu on 1/29/16.
 *
 *   session is used to add download task to execute,
 *
 *   example
 *    DownloadTask task = new DownloadTask(params...);
 *    DownloadSession session = new DownloadSession();
 *    session.add(task);
 *
 *    注, 一个应用里, 将session建立成单例为好.
 *
 */
public class DownloadSession {

    private Queue<DownloadTask> mDownloadQueue;

    private ExecutorService mDefaultLevelThreadPool;
    private ExecutorService mHighLevelThreadPool;

    private static final int DEFAULT_POOL_SIZE = 3;
    private static final int HIGH_POOL_SIZE = 3;

    public DownloadSession(){

        mDownloadQueue = new LinkedBlockingQueue<>();

        mDefaultLevelThreadPool = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
        mHighLevelThreadPool = Executors.newFixedThreadPool(HIGH_POOL_SIZE);

    }


    /**
     *
     * add task to the download queue and try to start it
     * @param task download queue
     * @return if task already exist, return false. otherwise return true
     */
    public synchronized boolean add(DownloadTask task){

        if (isTaskAlreadyExist(task)){
            return false;
        }

        mDownloadQueue.add(task);
        task.setDownloadSession(this);

        tryToExecuteTask(task);
        return true;
    }


    /**
     * remove the task
     * @param task downloadTask
     * @return if remove success, return true. otherwise return false
     */
    synchronized boolean remove(DownloadTask task){

         return mDownloadQueue.remove(task);
    }


    /**
     * try to execute task if has free thread
     * @param task downloadTask
     */
    synchronized void tryToExecuteTask(DownloadTask task){

        Log.i("mDefaultLevelThreadPool",
                "activeThread = " + ((ThreadPoolExecutor) mDefaultLevelThreadPool).getActiveCount()
                        + "poolSize = " + ((ThreadPoolExecutor) mDefaultLevelThreadPool).getPoolSize());

        if (task.getWeightLevel() == DownloadTask.DEFAULT_LEVEL_TASK){

            task.setTaskState(DownloadTask.READY, null);
            mDefaultLevelThreadPool.execute(task);

        }else if (task.getWeightLevel() == DownloadTask.HIGH_LEVEL_TASK) {

            task.setTaskState(DownloadTask.READY, null);
            mHighLevelThreadPool.execute(task);
        }
    }


    /**
     * check new task is exist or not, use url to check
     * @param newTask new task
     * @return true if exist, otherwise not
     */
    private boolean isTaskAlreadyExist(DownloadTask newTask){

        Iterator<DownloadTask>iterator = mDownloadQueue.iterator();
        while (iterator.hasNext()){
            DownloadTask task = iterator.next();
            if (task.compareTo(newTask) == 0){
                return true;
            }
        }
        return false;
    }


    /**
     * find one task in collection by id
     * @param collection store the tasks
     * @param taskId taskId
     * @return downloadTask
     */
    public  static DownloadTask findDownloadTaskById(Collection<DownloadTask> collection, long taskId){
        Iterator<DownloadTask> iterator = collection.iterator();
        while (iterator.hasNext()){
            DownloadTask task = iterator.next();
            if (task.id == taskId){
                return task;
            }
        }
        return null;
    }

}
