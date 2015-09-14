package de.unima.ar.collector.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WearableListView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.adapter.ItemListViewAdapter;
import de.unima.ar.collector.api.BroadcastService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.Utils;

public class Chooser extends Activity
{
    private ItemListViewAdapter adapterPosture;
    private ItemListViewAdapter adapterPosition;
    private GestureDetector     detector;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_chooser);
        detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
        {
            public void onLongPress(MotionEvent e)
            {
                DismissOverlayView dismissOverlay = (DismissOverlayView) (Chooser.this).findViewById(R.id.dismiss_overlay);
                dismissOverlay.setIntroText(R.string.welcome_long_press_exit);
                dismissOverlay.showIntroIfNecessary();
                dismissOverlay.show();
            }
        });

        // register activity
        ActivityController.getInstance().add("Chooser", this);

        // adapter
        this.adapterPosture = new ItemListViewAdapter(Chooser.this, true);
        this.adapterPosition = new ItemListViewAdapter(Chooser.this, true);

        // view
        TextView tv = (TextView) findViewById(R.id.posture_posture_headline);
        tv.setText(R.string.posture_headline);

        WearableListView view = (WearableListView) findViewById(R.id.posture_posture_list);
        view.setAdapter(this.adapterPosture);
        view.setClickListener(new PostureClickListener());

        // send database request
        String query = "SELECT name FROM " + SQLTableName.POSTURES;
        BroadcastService.getInstance().sendMessage("/database/request/posture", query);
    }


    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev)
    {
        return detector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }


    public void createPostureList(final String s)
    {
        // process data
        final NavigableSet<String> data = new TreeSet<>();
        data.addAll(Arrays.asList(s.split("\n")));

        // refresh UI
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // data
                List<String> postures = new ArrayList<>(data);
                MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                String selectedElement = ((TextView) main.findViewById(R.id.posture_posture)).getText().toString();
                int pos = Utils.getPosition(selectedElement, postures);

                // set current element
                Chooser.this.adapterPosture.setSelectedElements(Arrays.asList(selectedElement));

                // progressbar
                ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
                pb.setVisibility(View.INVISIBLE);

                // update adapter
                Chooser.this.adapterPosture.update(null, postures);
                Chooser.this.adapterPosture.notifyDataSetChanged();

                // view
                WearableListView view = (WearableListView) findViewById(R.id.posture_posture_list);
                view.scrollToPosition(pos);
            }
        });
    }


    public void createPositionList(final String s)
    {
        // process data
        final NavigableSet<String> data = new TreeSet<>();
        data.addAll(Arrays.asList(s.split("\n")));

        // refresh UI
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                // data
                List<String> postures = new ArrayList<>(data);
                MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
                String selectedElement = ((TextView) main.findViewById(R.id.posture_position)).getText().toString();
                selectedElement = selectedElement.substring(1, selectedElement.length() - 1);
                int pos = Utils.getPosition(selectedElement, postures);

                // set current element
                Chooser.this.adapterPosition.setSelectedElements(Arrays.asList(selectedElement));

                // progressbar
                ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
                pb.setVisibility(View.INVISIBLE);

                // view
                TextView tv = (TextView) findViewById(R.id.posture_posture_headline);
                tv.setText(R.string.position_headline);

                // update adapter
                Chooser.this.adapterPosition.update(null, new ArrayList<>(data));
                Chooser.this.adapterPosition.notifyDataSetChanged();

                // update view
                WearableListView view = (WearableListView) findViewById(R.id.posture_posture_list);
                view.setAdapter(Chooser.this.adapterPosition);
                view.setClickListener(new PositionClickListener());
                view.scrollToPosition(pos);
            }
        });
    }


    private class PostureClickListener implements WearableListView.ClickListener
    {
        @Override
        public void onClick(WearableListView.ViewHolder viewHolder)
        {
            // parse data
            String posture = Chooser.this.adapterPosture.get(viewHolder.getPosition());

            // update mobile device
            BroadcastService.getInstance().sendMessage("/posture/update", posture);

            // update local device
            MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
            mc.updatePostureView(posture);

            // clean up
            ((TextView) findViewById(R.id.posture_posture_headline)).setText("");
            WearableListView view = (WearableListView) findViewById(R.id.posture_posture_list);
            view.setAdapter(null);
            view.setOnClickListener(null);

            // progressbar
            ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
            pb.setVisibility(View.VISIBLE);

            // send database request
            String query = "SELECT name FROM " + SQLTableName.POSITIONS;
            BroadcastService.getInstance().sendMessage("/database/request/position", query);
        }


        @Override
        public void onTopEmptyRegionClick()
        {
            // TODO
        }
    }


    private class PositionClickListener implements WearableListView.ClickListener
    {
        @Override
        public void onClick(WearableListView.ViewHolder viewHolder)
        {
            // parse data
            String position = Chooser.this.adapterPosition.get(viewHolder.getPosition());

            // update mobile device
            BroadcastService.getInstance().sendMessage("/position/update", position);

            // update local device
            MainActivity mc = (MainActivity) ActivityController.getInstance().get("MainActivity");
            mc.updatePositionView(position);

            // finish
            Chooser.this.finish();
            Chooser.this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }


        @Override
        public void onTopEmptyRegionClick()
        {
            // TODO
        }
    }
}