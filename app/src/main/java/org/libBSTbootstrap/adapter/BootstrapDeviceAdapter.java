package org.libBSTbootstrap.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.openhab.bootstrap.R;
import org.libBSTbootstrap.BootstrapDevice;
import org.libBSTbootstrap.deviceList.BootstrapDeviceList;
import org.libBSTbootstrap.deviceList.BootstrapDeviceUpdateListener;
import org.libBSTbootstrap.WirelessNetwork;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Lists all bootstrap devices in range. Shows the name and wifi signal strengths and a checkbox
 * per device. To be filled with the helper class {@see org.libBSTbootstrap.deviceList.BootstrapDevicesViaWifiScan}
 */
public class BootstrapDeviceAdapter extends RecyclerView.Adapter<BootstrapDeviceAdapter.ViewHolder> implements BootstrapDeviceUpdateListener, CompoundButton.OnCheckedChangeListener {
    private BootstrapDeviceList data;
    Drawable d[] = new Drawable[]{null,null,null,null,null,null};
    boolean onBind = false;

    onSelectionChange observer;
    private boolean selectionEnabled = true;

    public void setOnSelectionChangeListener(onSelectionChange observer) {
        this.observer = observer;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BootstrapDeviceAdapter(Context context) {
        d[0] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp,context.getTheme());
        d[1] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_0_bar_black_24dp,context.getTheme());
        d[2] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_1_bar_black_24dp,context.getTheme());
        d[3] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_2_bar_black_24dp,context.getTheme());
        d[4] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_3_bar_black_24dp,context.getTheme());
        d[5] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp,context.getTheme());
    }

    public void setData(BootstrapDeviceList data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public boolean isOneSelected() {
        for (BootstrapDevice device: data.devices)
            if (device.isSelected())
                return true;
        return false;
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
    }

    // inner class to hold a reference to each item of RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.title) public TextView txtViewTitle;
        @Bind(R.id.subtitle) public TextView txtSubTitle;
        @Bind(R.id.image) public ImageView imgViewIcon;

        public ViewHolder(View itemLayoutView, BootstrapDeviceAdapter adapter) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
            if (adapter.selectionEnabled) {
                getCheckbox().setOnCheckedChangeListener(adapter);
            }
        }

        CheckBox getCheckbox() {
            return (CheckBox)txtViewTitle;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (onBind)
            return;
        Integer position = (Integer)compoundButton.getTag();
        if (position == null)
            return;
        data.devices.get(position).setSelected(b);

        if (observer != null)
            observer.onSelectionChanged();
    }


    @Override
    public int getItemCount() {
        if (data == null)
            return 0;
        return data.devices.size();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BootstrapDeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
                selectionEnabled ? R.layout.bootstrap_entry_checkable : R.layout.bootstrap_entry , parent, false);
        return new ViewHolder(itemLayoutView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Context c = viewHolder.txtViewTitle.getContext();
        BootstrapDevice device =  data.devices.get(position);
        viewHolder.txtViewTitle.setText(device.device_name);
        viewHolder.txtViewTitle.setTag(position);
        if (selectionEnabled) {
            onBind = true;
            viewHolder.getCheckbox().setChecked(device.isSelected());
            viewHolder.txtSubTitle.setText(device.uid);
            if (device.is_bound)
                viewHolder.txtSubTitle.setText(c.getString(R.string.device_entry_bound,device.uid));
            else
                viewHolder.txtSubTitle.setText(device.uid);
            onBind = false;
        } else {
            viewHolder.txtSubTitle.setText(device.getStateText(c));
        }
        int strength = device.strength;
        viewHolder.imgViewIcon.setImageDrawable(d[strength*6/100]);
    }

    @Override
    public void updateFinished(List<WirelessNetwork> entries) {
        int sizeBefore = data.devices.size();
        List<Integer> changes = data.updateFinished(entries);

        for (Integer index: changes) {
            notifyItemChanged(index);
        }

        if (sizeBefore != data.devices.size()) {
            notifyItemRangeInserted(sizeBefore, data.devices.size()-1);
        }
    }

    @Override
    public void updateStarted() {

    }
}
