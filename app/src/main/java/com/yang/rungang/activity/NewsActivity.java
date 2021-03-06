package com.yang.rungang.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.yang.rungang.R;
import com.yang.rungang.adapter.NewsRecyclerAdapter;
import com.yang.rungang.listener.OnRecyclerViewListener;
import com.yang.rungang.model.biz.ActivityManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import cn.bmob.newim.BmobIM;
import cn.bmob.newim.bean.BmobIMConversation;
import cn.bmob.newim.event.MessageEvent;
import cn.bmob.newim.event.OfflineMessageEvent;
import cn.bmob.newim.listener.MessageListHandler;

public class NewsActivity extends BaseActivity implements View.OnClickListener,MessageListHandler {

    private LinearLayout newsLayout;
    private ImageView backImg;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private NewsRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_news);
        ActivityManager.getInstance().pushOneActivity(this);

        initComponent();
        setListener();
    }

    private void initComponent() {
        newsLayout = (LinearLayout) findViewById(R.id.news_layout);
        backImg = (ImageView) findViewById(R.id.news_back_img);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.news_sw_refresh);
        recyclerView = (RecyclerView) findViewById(R.id.news_recyclerview);

        layoutManager = new LinearLayoutManager(this);
        adapter = new NewsRecyclerAdapter(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        backImg.setOnClickListener(this);
    }

    private void setListener(){

        newsLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                newsLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                refreshLayout.setRefreshing(true);
                query();

            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                query();

            }
        });

        adapter.setOnRecyclerViewListener(new OnRecyclerViewListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(NewsActivity.this,ChatActivity.class);
                Bundle bundle = new Bundle();
                BmobIMConversation conversation = adapter.getItem(position);
                bundle.putSerializable("c",conversation);
                intent.putExtra(getPackageName(), bundle);
                startActivity(intent);
            }

            @Override
            public boolean onItemLongClick(final int position) {

                new AlertDialog.Builder(NewsActivity.this)
                        .setMessage("确定删除这个消息")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BmobIM.getInstance().deleteConversation(adapter.getItem(position));
                                adapter.remove(position);
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();

                return true;
            }
        });
    }


    /**
     查询本地会话
     */
    public void query(){
        adapter.bindDatas(BmobIM.getInstance().loadAllConversation());
        adapter.notifyDataSetChanged();
        refreshLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.news_back_img:
                this.finish();
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshLayout.setRefreshing(true);
        query();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //注册EventBus
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        //解注册EventBus
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onMessageReceive(List<MessageEvent> list) {

    }

    /**注册离线消息接收事件
     * @param event
     */
    @Subscribe
    public void onEventMainThread(OfflineMessageEvent event){
        //重新刷新列表
        adapter.bindDatas(BmobIM.getInstance().loadAllConversation());
        adapter.notifyDataSetChanged();
    }

    /**注册消息接收事件
     * @param event
     * 1、与用户相关的由开发者自己维护，SDK内部只存储用户信息
     * 2、开发者获取到信息后，可调用SDK内部提供的方法更新会话
     */
    @Subscribe
    public void onEventMainThread(MessageEvent event){
        //重新获取本地消息并刷新列表
        adapter.bindDatas(BmobIM.getInstance().loadAllConversation());
        adapter.notifyDataSetChanged();
    }

}
