package com.ezymd.restaurantapp.delivery.order;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ezymd.restaurantapp.delivery.BaseActivity;
import com.ezymd.restaurantapp.delivery.R;
import com.ezymd.restaurantapp.delivery.customviews.SnapTextView;
import com.ezymd.restaurantapp.delivery.order.adapter.ItemsAdapter;
import com.ezymd.restaurantapp.delivery.order.model.OrderItems;
import com.ezymd.restaurantapp.delivery.utils.JSONKeys;

import java.util.ArrayList;


/**
 * Created by Mohit on 3/8/2018.
 */

public class ItemsDialogFragment extends DialogFragment {

    private SnapTextView title, proceed,
            error;
    private ListView list;
    private View view;
    private View.OnClickListener onclik;

    public ItemsDialogFragment() {
    }

    public static ItemsDialogFragment newInstance(ArrayList<OrderItems> list) {
        ItemsDialogFragment frag = new ItemsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(JSONKeys.OBJECT, list);
        frag.setArguments(args);
        return frag;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogFadeAnimation;
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(true);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {

                }
            });
        }
        return view = inflater.inflate(R.layout.fragment_dialog_item_list, container);
    }

    @Override
    public void onResume() {
        // Store access variables for window and blank point
        Window window = getDialog().getWindow();
        Point size = new Point();
        // Store dimensions of the screen in `size`
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);
        // Set the width of the dialog proportional to 75% of the screen width
        window.setLayout((int) (size.x * 0.95), WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        // Call super onResume after sizing
        super.onResume();
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<OrderItems> list = (ArrayList<OrderItems>) getArguments().getSerializable(JSONKeys.OBJECT);


        setGui(view, list);

    }


    private void setGui(View view, ArrayList<OrderItems> list) {
        title = view.findViewById(R.id.title);
        this.list = view.findViewById(android.R.id.list);


        proceed = view.findViewById(R.id.proceed);

        error = view.findViewById(R.id.error);


        setListener(list);
        setData(list);


    }


    private void setListener(ArrayList<OrderItems> list) {
        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int positon, long id) {

            }
        });

    }

    private void setData(ArrayList<OrderItems> list) {
        proceed.setClickable(false);
        error.setVisibility(View.GONE);
        ItemsAdapter adapter = new ItemsAdapter(list, getActivity());
        this.list.setAdapter(adapter);

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkData(adapter);
            }
        });

    }

    private void checkData(ItemsAdapter adapter) {
        ArrayList<OrderItems> orderItems = adapter.getData();
        for (OrderItems item : orderItems) {
            if (!item.isSelected()) {
                ((BaseActivity) getActivity()).showError(view, false, "Please check if item is received from restaurant");
                return;
            }
        }

        onclik.onClick(view);
        this.dismissAllowingStateLoss();

    }


    public void setOnClickListener(View.OnClickListener onClickListener) {
        onclik = onClickListener;
    }
}