//package de.unima.ar.collector.ui;
//
//import android.content.Context;
//
//public class AboutAdapter extends SeparatedListAdapter
//{
//    public AboutAdapter(Context context, int maxPosition)
//    {
//        super(context, maxPosition);
//    }
//
//
//    @Override
//    public boolean isEnabled(int position)
//    {
//        int size = this.getCount();
//
//        if(position < (size - 4)) {
//            return true;
//        }
//
//        return false;
//    }
//
//
//    @Override
//    public boolean areAllItemsEnabled()
//    {
//        return false;
//    }
//}