package com.xiongdi.recognition.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.xiongdi.recognition.interfaces.DatePickerInterface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by moubiao on 2016/3/22.
 * 日期选择器的dialog
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private DatePickerInterface dateInter;
    private Date saveDate;

    public void setData(String dateStr, DatePickerInterface dateInter) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            saveDate = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.dateInter = dateInter;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(saveDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        String month_ext = String.valueOf(monthOfYear + 1);
        String day_ext = String.valueOf(dayOfMonth);
        if (month_ext.length() <= 1) {
            month_ext = "0" + month_ext;
        }
        if (day_ext.length() <= 1) {
            day_ext = "0" + day_ext;
        }

        String date = String.valueOf(year) + "-" + month_ext + "-" + day_ext;
        dateInter.setDate(date);
    }
}
