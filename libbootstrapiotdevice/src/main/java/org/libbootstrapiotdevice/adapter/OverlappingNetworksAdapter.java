package org.libbootstrapiotdevice.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.libbootstrapiotdevice.OverlappingNetworks;

import org.libbootstrapiotdevice.BootstrapDevice;
import org.libbootstrapiotdevice.R;
import org.libbootstrapiotdevice.WirelessNetwork;

import java.util.List;


/**
 * Build a list of wireless networks from all devices and
 */
public class OverlappingNetworksAdapter extends RecyclerView.Adapter<OverlappingNetworksAdapter.ViewHolder> implements CompoundButton.OnCheckedChangeListener {
    OverlappingNetworks data = new OverlappingNetworks();
    int selected = 0;
    boolean onBind = false;

    Drawable d[] = new Drawable[]{null,null,null,null,null,null};

    onSelectionChange observer;
    public void setOnSelectionChangeListener(onSelectionChange observer) {
        this.observer = observer;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (onBind)
            return;
        if (!b)
            return;
        Integer position = (Integer)compoundButton.getTag();
        if (position == null)
            return;
        int last = selected;
        selected = position;
        if (last != -1) {
            notifyItemChanged(last);
        }
        notifyItemChanged(position);

        if (observer != null)
            observer.onSelectionChanged();
    }

    public WirelessNetwork getSelectedNetwork() {
        if (selected == -1)
            return null;
        return data.networks.get(selected);
    }

    // inner class to hold a reference to each item of RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RadioButton txtTitle;
        public TextView txtSubtitle;
        public ImageView imgViewIcon;

        public ViewHolder(View itemLayoutView, CompoundButton.OnCheckedChangeListener checkedChange) {
            super(itemLayoutView);
            txtTitle = (RadioButton)itemLayoutView.findViewById(R.id.title);
            txtSubtitle = (TextView)itemLayoutView.findViewById(R.id.subtitle);
            imgViewIcon = (ImageView)itemLayoutView.findViewById(R.id.image);
            txtTitle.setOnCheckedChangeListener(checkedChange);
        }
    }

    public OverlappingNetworksAdapter(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            d[0] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp,context.getTheme());
            d[1] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_0_bar_black_24dp,context.getTheme());
            d[2] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_1_bar_black_24dp,context.getTheme());
            d[3] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_2_bar_black_24dp,context.getTheme());
            d[4] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_3_bar_black_24dp,context.getTheme());
            d[5] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp,context.getTheme());
        } else {
            d[0] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp);
            d[1] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_0_bar_black_24dp);
            d[2] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_1_bar_black_24dp);
            d[3] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_2_bar_black_24dp);
            d[4] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_3_bar_black_24dp);
            d[5] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return data.networks.size();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public OverlappingNetworksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bootstrap_entry, parent, false);
        return new ViewHolder(itemLayoutView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Context c = viewHolder.txtTitle.getContext();
        viewHolder.txtTitle.setText(data.networks.get(position).ssid);
        viewHolder.txtTitle.setTag(position);
        onBind = true;
        viewHolder.txtTitle.setChecked(position == selected);
        onBind = false;
        viewHolder.txtSubtitle.setText(c.getString(R.string.network_entry, data.networks.get(position).deviceCount(), data.devices.size()));
        int strength = data.networks.get(position).getStrength();
        viewHolder.imgViewIcon.setImageDrawable(d[strength*6/100]);
    }

    public void addDevice(BootstrapDevice device) {
        int sizeBefore = data.networks.size();
        List<Integer> changes = data.addDevice(device);

        for (Integer index: changes) {
            notifyItemChanged(index);
        }

        if (sizeBefore != data.networks.size()) {
            notifyItemRangeInserted(sizeBefore, data.networks.size()-1);
        }
    }
}
