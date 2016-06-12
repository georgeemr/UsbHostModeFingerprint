package com.xiongdi.recognition.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.bean.Person;

import java.util.List;

/**
 * Created by moubiao on 2016/6/12.
 * 搜索结果的adapter
 */
public class SearchResultAdapter extends BaseAdapter {
    private Context mContext;
    private List<Person> mPersonList;

    public SearchResultAdapter(Context context, List<Person> personList) {
        mContext = context;
        mPersonList = personList;
    }

    @Override
    public int getCount() {
        return mPersonList.size();
    }

    @Override
    public Object getItem(int position) {
        return mPersonList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.search_result_item, parent, false);
            viewHolder.resultIDTV = (TextView) convertView.findViewById(R.id.result_ID_tv);
            viewHolder.resultNameTV = (TextView) convertView.findViewById(R.id.result_name_tv);
            viewHolder.resultGenderTV = (TextView) convertView.findViewById(R.id.result_gender_tv);
            viewHolder.resultBirthdayTV = (TextView) convertView.findViewById(R.id.result_birthday_tv);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Person person = mPersonList.get(position);
        viewHolder.resultIDTV.setText(person.getID_NO());
        viewHolder.resultNameTV.setText(person.getName());
        viewHolder.resultGenderTV.setText(person.getGender());
        viewHolder.resultBirthdayTV.setText(person.getBirthday());

        return convertView;
    }

    private class ViewHolder {
        private TextView resultIDTV, resultNameTV, resultGenderTV, resultBirthdayTV;
    }
}
