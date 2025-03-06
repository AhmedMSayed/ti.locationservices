package ti.locationservices;

import android.annotation.SuppressLint;
import android.location.Location;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiLifecycle;

import static android.app.Activity.RESULT_OK;

import static ti.locationservices.Utils.createLocationPayload;

import androidx.annotation.NonNull;


@Kroll.module(name = "TiLocationservices", id = "ti.locationservices")
public class TiLocationservicesModule extends KrollModule {

    private static final String LCAT = "TiLocationservicesModule";
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private int REQUEST_CHECK_SETTINGS;

    @Kroll.constant
    public static final int PRIORITY_BALANCED_POWER_ACCURACY = 1;
    @Kroll.constant
    public static final int PRIORITY_HIGH_ACCURACY = 2;
    @Kroll.constant
    public static final int PRIORITY_LOW_POWER = 3;
    @Kroll.constant
    public static final int PRIORITY_NO_POWER = 4;


    private FusedLocationProviderClient createLocationClient() {
        return LocationServices.getFusedLocationProviderClient(TiApplication.getAppRootOrCurrentActivity());
    }

    private FusedLocationProviderClient getOrCreateLocationClient() {
        if (mFusedLocationClient == null) {
            mFusedLocationClient = createLocationClient();
        }

        return mFusedLocationClient;
    }

    private LocationCallback getOrCreateLocationCallback() {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            fireEvent("location", createLocationPayload(location));
                        }
                    }
                }
            };
        }

        return locationCallback;
    }

    @Kroll.method
    public void checkLocationSettings(KrollDict dict) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest
                .Builder()
                .addLocationRequest(Utils.createLocationRequest(null));

        builder.setAlwaysShow(true);

        TiBaseActivity baseActivity = (TiBaseActivity) TiApplication.getAppCurrentActivity();
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(baseActivity).checkLocationSettings(builder.build());

        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        result.addOnSuccessListener(locationSettingsResponse -> sendUpdate(completeCallback, true, ""));

        result.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                ResolvableApiException resolvable = (ResolvableApiException) e;
                if (resolvable.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        Log.i(LCAT, "Location Settings are not satisfied, setting dialog going to show now");

                        REQUEST_CHECK_SETTINGS = baseActivity.getUniqueResultCode();
                        
                        final TiLifecycle.OnActivityResultEvent resultEvent = (activity, requestCode, resultCode, intent) -> {
                            if (requestCode == REQUEST_CHECK_SETTINGS) {
                                sendUpdate(completeCallback, resultCode == RESULT_OK, "");
                            }
                        };

                        baseActivity.addOnActivityResultListener(resultEvent);
                        resolvable.startResolutionForResult(baseActivity, REQUEST_CHECK_SETTINGS);

                    } catch (Exception exception) {
                        sendUpdate(completeCallback, false, exception.getMessage());
                    }
                } else {
                    Log.i(LCAT, "Location settings can't be changed to meet the requirements, no dialog pops up");
                    sendUpdate(completeCallback, false, LocationSettingsStatusCodes.getStatusCodeString(resolvable.getStatusCode()));
                }
            } else {
                sendUpdate(completeCallback, false, e.getMessage());
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Kroll.method
    public void getCurrentLocation(KrollDict dict) {
        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        createLocationClient()
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> onLocationListenerSuccess(location, completeCallback))
                .addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    @SuppressLint("MissingPermission")
    @Kroll.method
    public void getLastLocation(KrollDict dict) {
        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        createLocationClient()
                .getLastLocation()
                .addOnSuccessListener(location -> onLocationListenerSuccess(location, completeCallback))
                .addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    @Kroll.method
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(KrollDict dict) {
        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        getOrCreateLocationClient().requestLocationUpdates(
                Utils.createLocationRequest(dict),
                        getOrCreateLocationCallback(),
                        null)
                .addOnSuccessListener(aVoid -> sendUpdate(completeCallback, true, ""))
                .addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    @Kroll.method
    public void stopLocationUpdates(KrollDict dict) {
        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        if (locationCallback == null) {
            sendUpdate(completeCallback, false, "Cannot stop location updates since its callback is null");
            return;
        }

        getOrCreateLocationClient().removeLocationUpdates(locationCallback)
                .addOnSuccessListener(aVoid -> sendUpdate(completeCallback, true, ""))
                .addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    private void sendUpdate(KrollFunction callback, boolean success, String message) {
        if (callback == null) {
            return;
        }

        KrollDict data = new KrollDict();
        data.put("success", success);
        data.put("message", message);

        callback.callAsync(getKrollObject(), data);
    }

    private void onLocationListenerSuccess(Location location, KrollFunction callback) {
        if (callback == null) {
            return;
        }

        if (location == null) {
            sendUpdate(callback, false, "No cached location available");
            return;
        }

        callback.callAsync(getKrollObject(), createLocationPayload(location));
    }
}

