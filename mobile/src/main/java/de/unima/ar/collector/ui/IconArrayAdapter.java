package de.unima.ar.collector.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wallet.fragment.Dimension;

import java.util.List;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.container.Screens;
import de.unima.ar.collector.controller.ActivityController;

public class IconArrayAdapter extends BaseAdapter
{
    private Context            context;
    private List<Integer>      icons;
    private List<String>       texts;
    private List<Screens.Type> screens;


    public IconArrayAdapter(Context context, List<Integer> icons, List<String> texts, List<Screens.Type> screens)
    {
        super();

        this.context = context;
        this.icons = icons;
        this.texts = texts;
        this.screens = screens;
    }


    @Override
    public int getCount()
    {
        return icons.size();
    }


    @Override
    public Object getItem(int position)
    {
        return icons.get(position);
    }


    @Override
    public long getItemId(int position)
    {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listitem, parent, false);
        }

        TextView text = (TextView) convertView.findViewById(R.id.list_item_title);
        //        if(position < (getCount() - 1)) {
        //            text.setBackgroundResource(R.drawable.textlines);
        //        }
        text.setCompoundDrawablesWithIntrinsicBounds(icons.get(position), 0, 0, 0);
        text.setCompoundDrawablePadding(50);
        text.setText(texts.get(position));
        text.setTextSize(Dimension.UNIT_SP, 16);
        text.setTextColor(Color.parseColor("#464646"));
        text.setPadding(30, 15, 15, 15);
        text.setTypeface(null, Typeface.BOLD);

        Screens.Type current = Screens.current().first;
        final Screens.Type screen = screens.get(position);

        if(screen.equals(current)) {
            text.setBackgroundColor(Color.parseColor("#A4A4A4"));
            text.setTextColor(Color.parseColor("#000000"));
        }

        text.setOnTouchListener(new View.OnTouchListener()
        {
            float lastY;


            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setBackgroundColor(Color.parseColor("#A4A4A4"));
                        lastY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        MainActivity activity = (MainActivity) ActivityController.getInstance().get("MainActivity");
                        Screens.Type current = Screens.current().first;

                        if(screen.equals(current)) {
                            DrawerLayout drawerLayout = (DrawerLayout) ((Activity) context).findViewById(R.id.drawer_layout);
                            drawerLayout.closeDrawers();
                            return true;
                        }

                        v.setBackgroundColor(Color.parseColor("#D2D2D2"));

                        switch(screen) {
                            case SENSOREN:
                                activity.showSensoren();
                                break;
                            case ANALYZE:
                                activity.showAnalyze();
                                break;
                            case OPTIONS:
                                activity.showOptions();
                                break;
                            case SETTINGS:
                                Toast.makeText(activity, "TODO", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:
                        float abs = Math.abs(lastY - event.getY());

                        if(abs > 25) {
                            v.setBackgroundColor(Color.parseColor("#D2D2D2"));
                        }
                        break;
                }
                return true;
            }
        });

        if(position == 0) {
//            text.setPadding(30, 50, 15, 15);
        }

        return convertView;
    }
}