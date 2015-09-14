//package de.unima.ar.collector.ui;
//
//import de.unima.ar.collector.R;
//
//import android.app.Activity;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.TextView;
//import android.widget.ToggleButton;
//
//
///**
// * @author Fabian Kramm
// *
// */
//public class ServiceRowAdapter extends ArrayAdapter<String>
//{
//    private final Activity context;
//
//    static class ViewHolder
//    {
//        public TextView     text;
//        public ToggleButton toggleBtn;
//    }
//
//
//    public ServiceRowAdapter(Activity context)
//    {
//        super(context, R.layout.listitemservice, new String[] { "Service" });
//
//        this.context = context;
//    }
//
//
//    /*
//     * (non-Javadoc)
//     * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
//     */
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent)
//    {
//        View rowView = convertView;
//
//        if(rowView == null) {
//            LayoutInflater inflater = context.getLayoutInflater();
//            rowView = inflater.inflate(R.layout.listitemservice, parent, false);
//            ViewHolder viewHolder = new ViewHolder();
//            viewHolder.toggleBtn = (ToggleButton) rowView.findViewById(R.id.toggleButton1);
//            viewHolder.toggleBtn.setText("Start");
//            rowView.setTag(viewHolder);
//        }
//
//        return rowView;
//    }
//}