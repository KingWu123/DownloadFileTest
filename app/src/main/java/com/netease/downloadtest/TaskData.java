package com.netease.downloadtest;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by king.wu on 2/16/16.
 *
 */
public class TaskData {

    public TaskData(){

    }

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = "state")
    private int state;

    @DatabaseField(columnName = "progress")
    private int progress;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
