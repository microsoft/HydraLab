// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import com.microsoft.hydralab.android.client.vpn.HydraLabVpnService;

public class VpnActivity extends Activity {
    private static final String VPN_ACTION_START = "com.microsoft.hydralab.android.client.vpn.START";
    private static final String VPN_ACTION_STOP = "com.microsoft.hydralab.android.client.vpn.STOP";
    private static final int REQUEST_VPN_PROFILE = 9;

    private String mVpnOutputPath;
    private String mVpnAppsStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(VPN_ACTION_START)) {
                this.mVpnOutputPath = intent.getExtras().getString("output");
                this.mVpnAppsStr = intent.getExtras().getString("apps");
                Intent prepareIt = VpnService.prepare(this);
                if (prepareIt != null) {
                    startActivityForResult(prepareIt, REQUEST_VPN_PROFILE);
                }
                else {
                    StartVpnService();
                }
            } else if (action.equals(VPN_ACTION_STOP)) {
                StopVpnService();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_VPN_PROFILE:
                StartVpnService();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void StartVpnService() {
        Intent it = new Intent(this, HydraLabVpnService.class);
        it.setAction(VPN_ACTION_START);
        it.putExtra("output", this.mVpnOutputPath);
        it.putExtra("apps", this.mVpnAppsStr);
        startService(it);
    }

    private void StopVpnService() {
        Intent it = new Intent(this, HydraLabVpnService.class);
        it.setAction(VPN_ACTION_STOP);
        startService(it);
    }

}
