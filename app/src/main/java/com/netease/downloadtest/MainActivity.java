package com.netease.downloadtest;

import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.netease.download.DownloadError;
import com.netease.download.DownloadListener;
import com.netease.download.DownloadTask;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mRecyclerView = (RecyclerView)findViewById(R.id.recycleView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
//        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));//这里用线性宫格显示 类似于grid view
//        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, OrientationHelper.VERTICAL));//这里用线性宫格显示 类似于瀑布流
        mRecyclerView.setAdapter(new DownloadAdapter(this));
    }











    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }else if (id == R.id.action_createDao){
            testOrmSqlite();
            return true;
        }else if (id == R.id.action_showList){
            testList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void testOrmSqlite(){
        TaskDataBaseHelper helper = new TaskDataBaseHelper(getApplicationContext());

        try {
            TaskData data = new TaskData();

            data.setProgress(10);
            data.setState(1);
            helper.getTaskDataDao().create(data);

            data.setProgress(22);
            data.setState(2);
            helper.getTaskDataDao().create(data);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        helper.close();

    }


    public void testList()
    {
        TaskDataBaseHelper helper = new TaskDataBaseHelper(getApplicationContext());
        try
        {
            TaskData data = new TaskData();
            data.setId(1);
            List<TaskData> taskDatas = helper.getTaskDataDao().queryForAll();
            Log.i("TAG", taskDatas.toString());
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        helper.close();
    }

}
