package bms.bmsprototype.helper;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.support.v13.app.FragmentCompat;
import android.util.Log;

import bms.bmsprototype.dialog.ConfirmationDialog;

/**
 * Created by gaua2616 on 2016-02-02.
 */
public class PermissionHelper {

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private static boolean shouldShowRequestPermissionRationale(Fragment frag, String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(frag, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    public static void requestVideoPermissions(Fragment frag, int req, String[] perms, String dialog) {
        if (shouldShowRequestPermissionRationale(frag, perms)) {
            ConfirmationDialog.newInstance(perms,req).show(frag.getChildFragmentManager(), dialog);
        } else {
            FragmentCompat.requestPermissions(frag, perms, req);
        }
    }

    public static boolean validateRequest(int[] grantResults, String[] requestedPermissions) {
        Log.d("PermissionHelper", "onRequestPermissionsResult");
        if (grantResults.length != requestedPermissions.length)
            return false;

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
