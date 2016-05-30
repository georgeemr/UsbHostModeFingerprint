package com.xiongdi.recognition.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by moubiao on 2016/3/23.
 * 采集信息的viewpager的adapter
 */
public class GatherInfoVpAdapter extends FragmentPagerAdapter {
    private List<Fragment> fgList;
    private List<String> fgTitle;

    public GatherInfoVpAdapter(FragmentManager fm, List<Fragment> fgList, List<String> fgTitle) {
        super(fm);
        this.fgList = fgList;
        this.fgTitle = fgTitle;
    }

    @Override
    public Fragment getItem(int position) {
        return fgList.get(position);
    }

    @Override
    public int getCount() {
        return fgList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return fgTitle.get(position);
    }
}
