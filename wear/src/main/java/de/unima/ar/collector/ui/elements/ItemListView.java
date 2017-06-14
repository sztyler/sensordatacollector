package de.unima.ar.collector.ui.elements;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import de.unima.ar.collector.R;

public class ItemListView extends FrameLayout implements WearableListView.Item
{
    // circle
    private       float scale;
    private       float defaultCircleRadius;
    private       float selectedCircleRadius;
    private final int   fadedCircleColor;
    private final int   chosenCircleColor;

    // square
    private float defaultSquareSize;
    private float selectedSquareSize;
    private int   defaultSquarePadding;
    private int   selectedsquarePadding;
    private int   leftPadding;

    // generell
    private boolean circle;

    // elements
    private final CircledImageView imgView;
    private final TextView         txtView;


    public ItemListView(Context context, boolean circle)
    {
        super(context);
        this.circle = circle;

        View.inflate(context, R.layout.round_chooser_itemrow, this);

        // elements and properties
        this.imgView = (CircledImageView) findViewById(R.id.image);
        this.txtView = (TextView) findViewById(R.id.text);
        this.fadedCircleColor = ContextCompat.getColor(context, android.R.color.darker_gray);
        this.chosenCircleColor = ContextCompat.getColor(context, android.R.color.holo_blue_dark);
        this.defaultCircleRadius = getResources().getDimension(R.dimen.default_settings_circle_radius);
        this.selectedCircleRadius = getResources().getDimension(R.dimen.selected_settings_circle_radius);

        this.imgView.setCircleColor(this.fadedCircleColor);

        if(!circle) {
            // calc size
            float totalSize = getResources().getDimensionPixelSize(R.dimen.wear_itemrow_dimension);
            this.selectedSquareSize = getResources().getDimensionPixelSize(R.dimen.wear_itemrow_square_large);
            this.defaultSquareSize = getResources().getDimensionPixelSize(R.dimen.wear_itemrow_square_small);
            this.leftPadding = getResources().getDimensionPixelSize(R.dimen.wear_itemrow_leftspace1);
            this.defaultSquarePadding = ((int) (totalSize - defaultSquareSize)) / 2;
            this.selectedsquarePadding = ((int) (totalSize - selectedSquareSize)) / 2;

            // set default values
            FrameLayout.LayoutParams layoutParams = (LayoutParams) imgView.getLayoutParams();
            layoutParams.height = (int) this.defaultSquareSize;
            layoutParams.width = (int) this.defaultSquareSize;
            layoutParams.setMargins(leftPadding + defaultSquarePadding, defaultSquarePadding, 0, 0);
            imgView.setLayoutParams(layoutParams);

            // set properties
            this.imgView.setBackgroundResource(R.drawable.square_background_default);
            this.imgView.setCircleBorderColor(this.fadedCircleColor);

            // disable circle
            this.defaultCircleRadius = 0;
            this.selectedCircleRadius = 0;
        }
    }


    @Override
    public float getProximityMinValue()
    {
        return defaultCircleRadius;
    }


    @Override
    public float getProximityMaxValue()
    {
        return selectedCircleRadius;
    }


    @Override
    public float getCurrentProximityValue()
    {
        return this.scale;
    }


    @Override
    public void setScalingAnimatorValue(float value)
    {
        this.scale = value;

        this.imgView.setCircleRadius(this.scale);
        this.imgView.setCircleRadiusPressed(this.scale);
    }


    @Override
    public void onScaleUpStart()
    {
        this.imgView.setAlpha(1f);
        this.imgView.setCircleColor(this.chosenCircleColor);

        this.txtView.setAlpha(1f);
        this.txtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f);

        if(circle) {
            return;
        }

        new Handler().post(new Runnable()
        {
            public void run()
            {
                imgView.setBackgroundResource(R.drawable.square_background_selected);
                imgView.setCircleBorderColor(chosenCircleColor);

                FrameLayout.LayoutParams layoutParams = (LayoutParams) imgView.getLayoutParams();
                layoutParams.height = (int) selectedSquareSize;
                layoutParams.width = (int) selectedSquareSize;
                layoutParams.setMargins(leftPadding + selectedsquarePadding, selectedsquarePadding, 0, 0);
                imgView.setLayoutParams(layoutParams);

                imgView.invalidate();
            }
        });
    }


    @Override
    public void onScaleDownStart()
    {
        this.imgView.setAlpha(0.5f);
        this.imgView.setCircleColor(this.fadedCircleColor);

        this.txtView.setAlpha(0.5f);
        this.txtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);

        if(circle) {
            return;
        }

        new Handler().post(new Runnable()
        {
            public void run()
            {
                imgView.setBackgroundResource(R.drawable.square_background_default);
                imgView.setCircleBorderColor(fadedCircleColor);

                FrameLayout.LayoutParams layoutParams = (LayoutParams) imgView.getLayoutParams();
                layoutParams.height = (int) defaultSquareSize;
                layoutParams.width = (int) defaultSquareSize;
                layoutParams.setMargins(leftPadding + defaultSquarePadding, defaultSquarePadding, 0, 0);
                imgView.setLayoutParams(layoutParams);


                imgView.invalidate();
            }
        });
    }
}