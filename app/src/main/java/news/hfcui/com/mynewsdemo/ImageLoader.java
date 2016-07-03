package news.hfcui.com.mynewsdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by hfcui on 2016/7/2.
 */
public class ImageLoader {

    private ImageView mImageView;
    private String mUrl;
    private LruCache<String, Bitmap> mCache; //创建Cache对缓存进行处理。
    private ListView mListView;
    private Set<NewsAsyncTask> mSet;

    public ImageLoader(ListView listView) {
        mListView = listView;
        mSet = new HashSet<>();
        //获取最大的可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 4;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //在每次存入缓存的时候调用此方法。
                return value.getByteCount();
                // return super.sizeOf(key, value);
            }
        };
    }

    /**
     * 保存图片到缓存
     *
     * @param url
     * @param bitmap
     */
    public void addBitmapToCache(String url, Bitmap bitmap) {
        //判断当前缓存是否存在，如果不存在的话保存到缓存中
        if (getBitmapFromCache(url) == null) {
            mCache.put(url, bitmap);
        }
    }

    /**
     * 获取缓存中的图片
     *
     * @param url
     * @return
     */
    public Bitmap getBitmapFromCache(String url) {
        return mCache.get(url);
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //判断tag是否是要显示的图片，防止缓存的图片会干扰正确图片的显示（解决ListView item错乱或者闪屏的bug）
            if (mImageView.getTag().equals(mUrl)) {
                mImageView.setImageBitmap((Bitmap) msg.obj);
            }
        }
    };

    //-----------------------------使用Thread异步加载--------------------------------
    public void showImageByThread(ImageView imageView, final String url) {
        mImageView = imageView;
        mUrl = url;

        new Thread() {
            @Override
            public void run() {
                super.run();
                Bitmap bitmap = getBitmapFromURL(url);
                Message message = Message.obtain();
                message.obj = bitmap;
                handler.sendMessage(message);
            }
        }.start();
    }

    public Bitmap getBitmapFromURL(String urlString) {
        Bitmap bitmap;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(connection.getInputStream());
            bitmap = BitmapFactory.decodeStream(is);
            connection.disconnect();//释放资源
            return bitmap;
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

//-----------------------------使用AsyncTask异步加载--------------------------------

    public void showImageByAsyncTask(ImageView imageView, String url) {
        //从缓存中取出对应的图片。
        Bitmap bitmap = getBitmapFromCache(url);
        if (bitmap == null) {
            imageView.setImageResource(R.mipmap.ic_launcher);
        } else {
            //有的话直接使用该图片。
            imageView.setImageBitmap(bitmap);
        }
    }

    // 获取对应条目的url。
    public void loadImages(int start, int end) {
        for (int i = start; i < end; i++) {
            String url = NewsAdapter.URLS[i];
            //从缓存中取出对应的图片。
            Bitmap bitmap = getBitmapFromCache(url);
            //如果缓存中没有此图片则从网络下载。
            if (bitmap == null) {
                NewsAsyncTask task = new NewsAsyncTask(url);
                task.execute(url);
                mSet.add(task);
            } else {
                //有的话直接使用该图片。
                ImageView imageView = (ImageView) mListView.findViewWithTag(url);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    //取消所有正在运行的任务
    public void cancelAll() {
        if (mSet != null) {
            for (NewsAsyncTask task : mSet) {
                task.cancel(false);
            }
        }
    }

    private class NewsAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private String mUrl;

        public NewsAsyncTask(String url) {
            mUrl = url;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            //从网络获取图片。
            Bitmap bitmap = getBitmapFromURL(url);
            if (bitmap != null) {
                //将缓存里没有的图片加入到缓存中。
                addBitmapToCache(url, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView = (ImageView) mListView.findViewWithTag(mUrl);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            mSet.remove(this);
        }
    }
}
