package com.netease.downloadtest;

import android.content.Context;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.netease.download.DownloadError;
import com.netease.download.DownloadListener;
import com.netease.download.DownloadSession;
import com.netease.download.DownloadTask;

import java.util.ArrayList;

/**
 * Created by king.wu on 2/2/16.
 */
public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder>{

    private LayoutInflater mLayoutInflater;
    private ArrayList<DownloadTask> mDownloadTaskArr = new ArrayList<>();
    private DownloadSession mDownloadSession = new DownloadSession();

    public DownloadAdapter(Context context){
        mLayoutInflater = LayoutInflater.from(context);
        initDownloadData();
    }

    private void initDownloadData(){
        for (int i=0; i< 20; i++){

            DownloadTask task;
            if (i == 1 || i == 2){
                 task = new DownloadTask("http://down.mumayi.com/41052/mbaidu",
                        Environment.DIRECTORY_DCIM + "/DownloadTest",
                        "download5File" + i, true, null);
            }else {
                task = new DownloadTask("http://down.mumayi.com/41052/mbaidu",
                        Environment.DIRECTORY_DCIM + "/DownloadTest",
                        "download5File" + i, null);
            }

            mDownloadTaskArr.add(task);
        }
    }


    @Override
    public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DownloadViewHolder(mLayoutInflater.inflate(R.layout.item_download, parent, false), mDownloadSession);
    }

    @Override
    public void onBindViewHolder(DownloadViewHolder holder, int position) {

        DownloadTask task = mDownloadTaskArr.get(position);
        holder.setDownloadTask(task);
    }

    @Override
    public int getItemCount() {
        return mDownloadTaskArr.size();
    }


//
//    private int findTaskPostionById(long taskId){
//        for (int position=0; position< mDownloadTaskArr.size(); position++){
//            if (mDownloadTaskArr.get(position).id == taskId){
//                return position;
//            }
//        }
//        return -1;
//    }



    public static class DownloadViewHolder extends RecyclerView.ViewHolder implements DownloadListener {

        TextView mIdTextView;
        Button mDownloadButton;
        Button mPauseButton;
        Button mResumeButton;
        Button mCancelButton;
        ProgressBar mProgressBar;
        TextView mStateTextView;

        DownloadTask mDownloadTask;
        DownloadSession mDownloadSession;


        DownloadViewHolder(View view, DownloadSession downloadSession) {
            super(view);
            mIdTextView = (TextView)view.findViewById(R.id.textViewId);
            mDownloadButton = (Button)view.findViewById(R.id.downloadButton);
            mPauseButton = (Button)view.findViewById(R.id.pauseButton);
            mResumeButton = (Button)view.findViewById(R.id.resumeButton);
            mCancelButton = (Button)view.findViewById(R.id.deleteButton);
            mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);
            mStateTextView = (TextView)view.findViewById(R.id.stateTextView);

            mDownloadSession = downloadSession;

            //下载
            mDownloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mDownloadSession.add(mDownloadTask);
                    mDownloadButton.setEnabled(false);

                }
            });

            //暂停
            mPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mDownloadTask.pause();

                        mStateTextView.setText("paused");

                    } catch (DownloadError error) {
                        error.printStackTrace();
                    }
                }
            });

            //恢复
            mResumeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mDownloadTask.resume();
                    } catch (DownloadError error) {
                        error.printStackTrace();
                    }
                }
            });

            //删除
            mCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mDownloadTask.cancel();
                        mStateTextView.setText("canceled");
                    } catch (DownloadError error) {
                        error.printStackTrace();
                    }
                }
            });
        }

        void setDownloadTask(DownloadTask downloadTask){
            mIdTextView.setText(downloadTask.id + "");
            mProgressBar.setProgress(downloadTask.getProgress());

            mStateTextView.setText(getStateString(downloadTask.getTaskState()));

            mDownloadTask = downloadTask;
            mDownloadTask.setListener(this);
        }

        String getStateString(int state){
            switch (state){
                case DownloadTask.INIT:
                    return "init";

                case DownloadTask.READY:
                    return "ready";

                case DownloadTask.DOWNLOADING:
                    return "downloading";

                case  DownloadTask.PAUSED:
                    return "paused";

                case  DownloadTask.FINISHED:
                    return "finished";

                case  DownloadTask.FAILED:
                    return "failed";

                default:
                    return "unKnow";
            }
        }

        @Override
        public void onDownloadReady(DownloadTask task) {

            mStateTextView.setText("ready");
        }

        @Override
        public void OnDownloadStarted(DownloadTask task) {
            mStateTextView.setText("downloading");
        }

        @Override
        public void onDownloadProgress(DownloadTask task, int percent, long downloadedLength, long totalLength) {

            mProgressBar.setProgress(percent);
        }

        @Override
        public void OnDownloadFinished(DownloadTask task) {
            mStateTextView.setText("finished");
        }

        @Override
        public void OnDownloadFailed(DownloadTask task, DownloadError error) {
            mStateTextView.setText("failed");

            Log.i("downloadFailed", error.getMessage());

        }
    }
}
