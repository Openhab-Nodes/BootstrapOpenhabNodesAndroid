package org.openhab_node.bootstrapopenhabnodes.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.openhab_node.bootstrapopenhabnodes.bootstrap.OverlappingNetworks;
import org.openhab_node.bootstrapopenhabnodes.R;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.BootstrapDevice;
import org.openhab_node.bootstrapopenhabnodes.bootstrap.WirelessNetwork;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

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

        @Bind(R.id.title) public RadioButton txtTitle;
        @Bind(R.id.subtitle) public TextView txtSubtitle;
        @Bind(R.id.image) public ImageView imgViewIcon;

        public ViewHolder(View itemLayoutView, CompoundButton.OnCheckedChangeListener checkedChange) {
            super(itemLayoutView);
            ButterKnife.bind(this, itemLayoutView);
            txtTitle.setOnCheckedChangeListener(checkedChange);
        }
    }

    public OverlappingNetworksAdapter(Context context) {
        d[0] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_off_black_24dp,context.getTheme());
        d[1] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_0_bar_black_24dp,context.getTheme());
        d[2] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_1_bar_black_24dp,context.getTheme());
        d[3] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_2_bar_black_24dp,context.getTheme());
        d[4] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_3_bar_black_24dp,context.getTheme());
        d[5] = context.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_black_24dp,context.getTheme());
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
