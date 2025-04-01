package com.example.fitvisionapp.ui.combos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fitvisionapp.R;

import java.util.List;

public class ComboGridAdapter extends BaseAdapter {

    private Context context;
    private List<String> comboList;

    public ComboGridAdapter(Context context, List<String> comboList) {
        this.context = context;
        this.comboList = comboList;
    }

    @Override
    public int getCount() {
        return comboList.size();
    }

    @Override
    public String getItem(int position) {
        return comboList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_combo_thumbnail, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.comboName);

        textView.setText(comboList.get(position));



        return convertView;
    }


}
