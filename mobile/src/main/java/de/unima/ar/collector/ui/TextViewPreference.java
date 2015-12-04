package de.unima.ar.collector.ui;


import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.unima.ar.collector.R;

public class TextViewPreference extends Preference
{
    private String title;
    private String description;


    public TextViewPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        for(int i = 0; i < attrs.getAttributeCount(); i++) {
            String attr = attrs.getAttributeName(i);
            String val = attrs.getAttributeValue(i);

            if("title".equals(attr)) {
                this.title = context.getString(Integer.parseInt(val.substring(1)));
            }
            if("summary".equals(attr)) {
                this.description = context.getString(Integer.parseInt(val.substring(1)));
            }

            if("clickable".equals(attr) && "false".equals(val)) {
                this.setEnabled(false);
            }
        }
    }


    @Override
    public void onClick()
    {
        super.onClick();

        persistBoolean(!getPersistedBoolean(false));
    }


    @Override
    protected View onCreateView(ViewGroup parent)
    {
        super.onCreateView(parent);

        LinearLayout ll = (LinearLayout) View.inflate(getContext(), R.layout.preferences_about, null);

        TextView text1 = (TextView) ll.findViewById(android.R.id.text1);
        text1.setText(this.title);

        TextView text2 = (TextView) ll.findViewById(android.R.id.text2);
        text2.setText(this.description);

        return ll;
    }
}