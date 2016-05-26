package org.libbootstrapiotdevice.adapter;

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

import org.libbootstrapiotdevice.BootstrapDevice;
import org.libbootstrapiotdevice.DeviceMode;
import org.libbootstrapiotdevice.R;
import org.libbootstrapiotdevice.WirelessNetwork;
import org.libbootstrapiotdevice.network.BootstrapDeviceUpdateListener;
import org.libbootstrapiotdevice.network.DeviceState;

import java.util.List;


/**
 * Lists all bootstrap devices in range. Shows the name and wifi signal strengths and a checkbox
 * per device. To be filled with the helper class {@see BootstrapDevicesViaWifiScan}
 */
public class BootstrapDeviceAdapter extends RecyclerView.Adapter<BootstrapDeviceAdapter.ViewHolder> implements BootstrapDeviceUpdateListener, CompoundButton.OnCheckedChangeListener {
    Drawable d[] = new Drawable[]{null,null,null,null,null,null};
    boolean onBind = false;
    onSelectionChange observer;
    private List<BootstrapDevice> data;
    private boolean selectionEnabled = true;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BootstrapDeviceAdapter(Context context) {
        d[0] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp,context.getTheme());
        d[1] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_0_bar_black_24dp,context.getTheme());
        d[2] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_1_bar_black_24dp,context.getTheme());
        d[3] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_2_bar_black_24dp,context.getTheme());
        d[4] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_3_bar_black_24dp,context.getTheme());
        d[5] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp,context.getTheme());
    }

    public void setOnSelectionChangeListener(onSelectionChange observer) {
        this.observer = observer;
    }

    public void setData(List<BootstrapDevice> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public boolean isOneSelected() {
        for (BootstrapDevice device : data)
            if (device.isSelected())
                return true;
        return false;
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (onBind)
            return;
        Integer position = (Integer)compoundButton.getTag();
        if (position == null)
            return;
        data.get(position).setSelected(b);

        if (observer != null)
            observer.onSelectionChanged();
    }

    @Override
    public int getItemCount() {
        if (data == null)
            return 0;
        return data.size();
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
        BootstrapDevice device = data.get(position);
        viewHolder.txtViewTitle.setText(device.device_name);
        viewHolder.txtViewTitle.setTag(position);
        if (selectionEnabled) {
            onBind = true;
            viewHolder.getCheckbox().setChecked(device.isSelected());
            if (device.getMode() == DeviceMode.Bound)
                viewHolder.txtSubTitle.setText(c.getString(R.string.device_entry_bound,device.uid));
            else
                viewHolder.txtSubTitle.setText(device.uid);
            onBind = false;
        } else {
            viewHolder.txtSubTitle.setText(DeviceState.toString(c, device.getState()));
        }

        WirelessNetwork network = device.getWirelessNetwork();
        if (network == null)
            viewHolder.imgViewIcon.setVisibility(View.INVISIBLE);
        else {
            viewHolder.imgViewIcon.setVisibility(View.VISIBLE);
            viewHolder.imgViewIcon.setImageDrawable(d[network.strength * 6 / 100]);
        }
    }

    @Override
    public void deviceUpdated(int index, boolean added) {
        if (added)
            notifyItemInserted(index);
        else
            notifyItemChanged(index);
    }

    @Override
    public void deviceRemoved(int index) {
        notifyItemRemoved(index);
    }

    @Override
    public void deviceRemoveAll() {
        notifyDataSetChanged();
    }

    @Override
    public void deviceChangesFinished() {

    }

    // inner class to hold a reference to each item of RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtViewTitle;
        public TextView txtSubTitle;
        public ImageView imgViewIcon;

        public ViewHolder(View itemLayoutView, BootstrapDeviceAdapter adapter) {
            super(itemLayoutView);
            txtViewTitle = (TextView) itemLayoutView.findViewById(R.id.title);
            txtSubTitle = (TextView) itemLayoutView.findViewById(R.id.subtitle);
            imgViewIcon = (ImageView) itemLayoutView.findViewById(R.id.image);
            if (adapter.selectionEnabled) {
                getCheckbox().setOnCheckedChangeListener(adapter);
            }
        }

        CheckBox getCheckbox() {
            return (CheckBox) txtViewTitle;
        }
    }
}
