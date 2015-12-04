package de.unima.ar.collector.database;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.util.UIUtils;

public class DatabaseDelete extends AsyncTask<String, Void, Boolean>
{
    private Context context;


    public DatabaseDelete(Context context)
    {
        this.context = context;
    }


    @Override
    protected void onPreExecute()
    {
        showProgressBar();
    }


    @Override
    protected Boolean doInBackground(String... params)
    {
        return SQLDBController.getInstance().deleteDatabase();
    }


    @Override
    protected void onPostExecute(final Boolean success)
    {
        if(success) {
            UIUtils.makeToast((Activity) context, R.string.option_export_delete_success, Toast.LENGTH_SHORT);
        } else {
            UIUtils.makeToast((Activity) context, R.string.option_export_delete_failed1, Toast.LENGTH_LONG);
        }

        hideProgressBar();
    }


    private void showProgressBar()
    {
        ((Activity) context).runOnUiThread(new Runnable()
        {
            public void run()
            {
                ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                FrameLayout progressBarHolder = (FrameLayout) ((Activity) context).findViewById(R.id.progressBarHolder);
                progressBarHolder.setVisibility(View.VISIBLE);
            }
        });
    }


    private void hideProgressBar()
    {
        ((Activity) context).runOnUiThread(new Runnable()
        {
            public void run()
            {
                ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                FrameLayout progressBarHolder = (FrameLayout) ((Activity) context).findViewById(R.id.progressBarHolder);
                progressBarHolder.setVisibility(View.GONE);
            }
        });
    }
}