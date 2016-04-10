package bms.bmsprototype.helper;

import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by gaua2616 on 2016-02-02.
 */
public class PermissionHelper {

    /**
     * Check the permission result
     *
     * @param grantResults
     * @param requestedPermissions
     * @return True if the permission is Granted for everything, false otherwise.
     */
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
