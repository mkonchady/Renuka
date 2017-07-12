package org.mkonchady.renuka;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

public class PermissionActivity extends Activity {
    private final String TAG = "PermissionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        final int PERM_CODE = 100;
        super.onResume();
        String permission = getIntent().getStringExtra("permission");
        ActivityCompat.requestPermissions(this, new String[]{permission}, PERM_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        boolean granted;
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            granted = true;
        } else {
            granted = false;
        }
        Intent data = getIntent();
        data.putExtra("granted", granted + "");
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


}
