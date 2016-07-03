package news.hfcui.com.mynewsdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by hfcui on 2016/7/2.
 */
public class NewsAdapter extends BaseAdapter implements AbsListView.OnScrollListener {

    private List<NewsBean> mList;
    private LayoutInflater mInflater;
    private ImageLoader mImageLoader;
    private int mStart, mEnd;
    public static String[] URLS;
    private boolean isFirst;


    public NewsAdapter(Context context, List<NewsBean> data, ListView listView) {
        mList = data;
        mInflater = LayoutInflater.from(context);
        mImageLoader = new ImageLoader(listView);
        URLS = new String[data.size()];
        for (int i = 0; i < data.size(); i++) {
            URLS[i] = data.get(i).newsIconUrl;
        }
        isFirst = true;//第一次启动
        listView.setOnScrollListener(this);//ListView添加滑动事件
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_layout, null);
            viewHolder.iv_picture = (ImageView) convertView.findViewById(R.id.iv_picture);
            viewHolder.tv_title = (TextView) convertView.findViewById(R.id.tv_title);
            viewHolder.tv_content = (TextView) convertView.findViewById(R.id.tv_content);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.iv_picture.setImageResource(R.mipmap.ic_launcher);//展示默认的图片
        viewHolder.iv_picture.setTag(mList.get(position).newsIconUrl);//设置Tag
        // new ImageLoader().showImageByThread(viewHolder.iv_picture, mList.get(position).newsIconUrl);//Thread方式加载图片
        // new ImageLoader().showImageByAsyncTask(viewHolder.iv_picture, mList.get(position).newsIconUrl);//AsyncTask方式加载图片
        // 注意这里使用了lruCache对缓存进行优化。
        mImageLoader.showImageByAsyncTask(viewHolder.iv_picture, mList.get(position).newsIconUrl);//AsyncTask方式加载图片
        viewHolder.tv_title.setText(mList.get(position).newsTitle);
        viewHolder.tv_content.setText(mList.get(position).newsContent);
        return convertView;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            //加载可见项
            mImageLoader.loadImages(mStart, mEnd);
        } else {
            //停止任务
            mImageLoader.cancelAll();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mStart = firstVisibleItem;//滑动的起点
        mEnd = firstVisibleItem + visibleItemCount;//滑动的终点
        //第一次进入调用 主动加载首屏图片。
        if (isFirst && visibleItemCount > 0) {
            mImageLoader.loadImages(mStart, mEnd);
            isFirst = false;
        }
    }

    class ViewHolder {
        public TextView tv_title;
        public TextView tv_content;
        public ImageView iv_picture;
    }
}

