//package de.unima.ar.collector.ui.dialog;
//
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.support.v4.app.DialogFragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import de.unima.ar.collector.R;
//
//
///**
// * @author Fabian Kramm
// */
//public class ChooseFromListDialog extends DialogFragment
//{
//    private String                          title    = null;
//    private String[]                        items    = null;
//    private DialogInterface.OnClickListener listener = null;
//
//
//    public void setTitle(String title)
//    {
//        this.title = title;
//    }
//
//
//    public void setItems(String[] items)
//    {
//        this.items = items;
//    }
//
//
//    public String getItem(int pos)
//    {
//        if(items.length < pos) {
//            return null;
//        }
//
//        return items[pos];
//    }
//
//
//    public void setOnClickListener(DialogInterface.OnClickListener listener)
//    {
//        this.listener = listener;
//    }
//
//
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState)
//    {
//        if(listener == null || items == null || title == null) {
//            return null;
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle(title).setItems(items, listener);
//
//        builder.setNegativeButton(R.string.activity_dialog_cancel, new DialogInterface.OnClickListener()
//        {
//            public void onClick(DialogInterface dialog, int id)
//            {
//                // User cancelled the dialog
//            }
//        });
//
//        return builder.create();
//    }
//}