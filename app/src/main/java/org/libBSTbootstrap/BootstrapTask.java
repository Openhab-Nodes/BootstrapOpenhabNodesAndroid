package org.libBSTbootstrap;

import android.os.AsyncTask;

import org.libBSTbootstrap.network.BootstrapNetwork;
import org.libBSTbootstrap.network.BootstrapNetworkReceiveAPI;
import org.libBSTbootstrap.network.WifiUtils;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 30.04.16.
 */
class BootstrapTask extends AsyncTask<Void, Integer, Boolean> implements WifiUtils.Callback, BootstrapNetworkReceiveAPI {
    private final BootstrapService mService;
    BootstrapDevice device;
    final int total;
    int current = -1;
    Semaphore waitCallback = new Semaphore(1);
    Semaphore welcomeMessageCallback = new Semaphore(1);

    BootstrapTask(BootstrapService service) {
        mService = service;
        total = mService.bootstrapDeviceList.devices.size();
    }

    @Override
    protected Boolean doInBackground(Void... no) {
        Thread.currentThread().setName("BootstrapTask");
        mService.bootstrapNetwork.setObserver(this);
        for (BootstrapDevice device : mService.bootstrapDeviceList.devices) {
            this.device = device;
            ++current;
            try {
                waitCallback.acquire();
                welcomeMessageCallback.acquire();
                mService.bootstrapNetwork.useDevice(device);

                switch (mService.mode) {
                    case BindMode:
                        if (!connectToDeviceWifi(mService.default_pwd))
                            continue;
                        if (!waitForWelcome())
                            continue;
                        if (!bindDevice())
                            continue;
                        break;
                    case DeviceInfoMode:
                        if (!connectToDeviceWifi(mService.specific_pwd))
                            continue;
                        if (!waitForWelcome())
                            continue;
                        if (!getWifiListFromDevice())
                            continue;
                        break;
                    case BootstrapMode:
                        if (!connectToDeviceWifi(mService.specific_pwd))
                            continue;
                        if (!waitForWelcome())
                            continue;
                        if (!bootstrapDevice())
                            continue;
                        break;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                waitCallback.release();
                welcomeMessageCallback.release();
            }
        }
        return null;
    }

    private boolean bootstrapDevice() throws InterruptedException {
        device.state = BootstrapDevice.DeviceState.Bootstrapping;
        publishProgress(current, 800);

        mService.bootstrapNetwork.sendBootstrapData(mService.data);
        if (!waitCallback.tryAcquire(800, TimeUnit.MILLISECONDS)) {
            device.state = BootstrapDevice.DeviceState.BootstrappingError;
            publishProgress(current);
            return false;
        }
        device.state = BootstrapDevice.DeviceState.BootstrappingSuccess;
        publishProgress(current);
        return true;
    }

    private boolean getWifiListFromDevice() throws InterruptedException {
        device.state = BootstrapDevice.DeviceState.WaitForNetworks;
        publishProgress(current, 2000);

        mService.bootstrapNetwork.requestWifiList();
        waitCallback.tryAcquire(1000, TimeUnit.MILLISECONDS);

        if (device.reachableNetworks == null || device.reachableNetworks.isEmpty()) {
            mService.bootstrapNetwork.requestWifiList();
            waitCallback.tryAcquire(1000, TimeUnit.MILLISECONDS);
        }
        if (device.reachableNetworks == null || device.reachableNetworks.isEmpty()) {
            device.state = BootstrapDevice.DeviceState.ConnectionError;
            publishProgress(current);
            return false;
        }
        device.state = BootstrapDevice.DeviceState.DeviceReady;
        publishProgress(current);
        return true;
    }

    private boolean connectToDeviceWifi(String pwd) throws InterruptedException {
        device.state = BootstrapDevice.DeviceState.Connect;
        publishProgress(current, 20000);

        mService.bootstrapNetwork.closeListenerSocket();
        mService.wifiUtils.ConnectToWifi(device.ssid, pwd, false, this);

        // The ConnectToWifi callback will release waitCallback in every case
        waitCallback.acquire();
        if (mService.bootstrapNetwork.isSocketOpen()) {
            return true;
        } else {
            mService.wifiUtils.removeStoredNetwork(device.ssid);
            device.state = BootstrapDevice.DeviceState.ConnectionError;
            publishProgress(current);
            return false;
        }
    }

    private boolean waitForWelcome() throws InterruptedException {
        device.state = BootstrapDevice.DeviceState.WaitForWelcome;
        publishProgress(current, 600);

        int i = 0;
        do {
            mService.bootstrapNetwork.sendHello();
            welcomeMessageCallback.tryAcquire(200, TimeUnit.MILLISECONDS);
        } while (device.addresses.isEmpty() && ++i <= 3);

        if (device.addresses.isEmpty()) {
            device.state = BootstrapDevice.DeviceState.ConnectionError;
            publishProgress(current);
            return false;
        }

        mService.bootstrapNetwork.useDevice(device);
        return true;
    }

    private boolean bindDevice() throws InterruptedException {
        device.state = BootstrapDevice.DeviceState.Binding;
        publishProgress(current, 1000);

        boolean sendOK = true;

        int i=0;
        do {
            sendOK = mService.bootstrapNetwork.bindToDevice(mService.specific_pwd);
            if (!sendOK)
                break;
            waitCallback.tryAcquire(200, TimeUnit.MILLISECONDS);
        } while(device.state != BootstrapDevice.DeviceState.BindingSuccess && ++i <= 3);

        if (sendOK && device.state != BootstrapDevice.DeviceState.BindingSuccess) {
            device.state = BootstrapDevice.DeviceState.BindingError;
            publishProgress(current);
            return false;
        }

        publishProgress(current);
        Thread.sleep(2000);
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... current) {
        int waitTimeMS = -1;
        if (current.length>1)
            waitTimeMS = current[1];
        mService.observer.BootstrapProgress(mService.bootstrapDeviceList.devices.get(current[0]), current[0], total, waitTimeMS);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        mService.observer.BootstrapFinished();
        mService.mode = BootstrapService.Mode.UnknownMode;
    }

    @Override
    public void connected(boolean connected, List<InetAddress> addresses) {
        device.state = BootstrapDevice.DeviceState.WaitForWelcome;
        if (connected)
            mService.bootstrapNetwork.recreateSocket();
        else
            mService.bootstrapNetwork.closeListenerSocket();
        waitCallback.release();
    }

    @Override
    public void canConnect(boolean canConnect) {

    }

    @Override
    public void device_wifiList(BootstrapDevice device, List<WirelessNetwork> networkList) {
        if (device.state == BootstrapDevice.DeviceState.WaitForNetworks) {
            device.reachableNetworks = networkList;
            waitCallback.release();
        }
    }

    @Override
    public void device_lastError(BootstrapDevice device, int errorCode, String log) {
        if (log != null)
            device.errorString = log;
        else
            device.errorString = BootstrapNetwork.getErrorString(mService, errorCode);
    }

    @Override
    public void device_bindingAccepted(BootstrapDevice device) {
        if (device.state == BootstrapDevice.DeviceState.Binding) {
            device.state = BootstrapDevice.DeviceState.BindingSuccess;
            waitCallback.release();
        }
    }

    @Override
    public void device_dataAccepted(BootstrapDevice device) {
        if (device.state == BootstrapDevice.DeviceState.Bootstrapping) {
            device.state = BootstrapDevice.DeviceState.BootstrappingSuccess;
            waitCallback.release();
        }
    }

    @Override
    public void device_welcomeMessage(BootstrapDevice device, InetAddress address, int newSessionID) {
        device.addresses.add(address);

        welcomeMessageCallback.release();
    }
}
