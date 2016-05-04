package com.netease.downloadtest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by king.wu on 2/16/16.
 *
 */
public class TaskDataBaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TABLE_NAME = "TaskTable.db";
    private static final int VERSION = 1;

    Dao<TaskData, Integer> taskDataDao;

    public TaskDataBaseHelper(Context context){
        super(context, TABLE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {

        try {
            TableUtils.createTable(connectionSource, TaskData.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

        try
        {
            TableUtils.dropTable(connectionSource, TaskData.class, true);
            onCreate(database, connectionSource);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public Dao<TaskData, Integer> getTaskDataDao() throws SQLException
    {
        if (taskDataDao == null)
        {
            taskDataDao = getDao(TaskData.class);
        }
        return taskDataDao;
    }

    @Override
    public void close(){
        super.close();
        taskDataDao = null;
    }
}
