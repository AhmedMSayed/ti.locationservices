package ti.locationservices;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Build;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
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
import org.appcelerator.titanium.util.TiConvert;

import static android.app.Activity.RESULT_OK;

import androidx.annotation.NonNull;


@Kroll.module(name = "TiLocationservices", id = "ti.locationservices")
public class TiLocationservicesModule extends KrollModule {

    private static final String LCAT = "TiLocationservicesModule";
    private final TiApplication context = TiApplication.getInstance();
    private final FusedLocationProviderClient mFusedLocationClient;
    private final LocationRequest locationRequest;
    private final LocationRequest.Builder locationBuilder;
    private LocationCallback locationCallback;
    private final LocationSettingsRequest.Builder builder;
    private int REQUEST_CHECK_SETTINGS;

    @Kroll.constant
    public static final int PRIORITY_BALANCED_POWER_ACCURACY = 1;
    @Kroll.constant
    public static final int PRIORITY_HIGH_ACCURACY = 2;
    @Kroll.constant
    public static final int PRIORITY_LOW_POWER = 3;
    @Kroll.constant
    public static final int PRIORITY_NO_POWER = 4;


    public TiLocationservicesModule() {
        super();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context.getCurrentActivity());

        locationBuilder = new LocationRequest
                .Builder(10000)
                .setMinUpdateIntervalMillis(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationRequest = locationBuilder.build();

        builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
    }

    private LocationCallback getLocationCallback() {
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
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(context.getCurrentActivity()).checkLocationSettings(builder.build());

        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        result.addOnSuccessListener(locationSettingsResponse -> sendUpdate(completeCallback, true, ""));

        result.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException resolvable) {
                if (resolvable.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        Log.i(LCAT, "Location Settings are not satisfied, setting dialog going to show now");

                        TiBaseActivity baseActivity = (TiBaseActivity) context.getCurrentActivity();
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
        Task<Location> locationTask = mFusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null);

        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                onLocationRequestSuccess(completeCallback, location);
                return;
            }

            onLocationRequestFailure(completeCallback);
        });

        locationTask.addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    @SuppressLint("MissingPermission")
    @Kroll.method
    public void getLastLocation(KrollDict dict) {
        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");
        Task<Location> locationTask = mFusedLocationClient.getLastLocation();

        locationTask.addOnSuccessListener(location -> {
            if (location == null) {
                onLocationRequestFailure(completeCallback);
                return;
            }

            onLocationRequestSuccess(completeCallback, location);
        });

        locationTask.addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    @Kroll.method
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(KrollDict dict) {
        if (dict.containsKey("interval")) {
            locationBuilder.setIntervalMillis(TiConvert.toInt(dict.getInt("interval")));
        }

        if (dict.containsKey("fastestInterval")) {
            locationBuilder.setMinUpdateIntervalMillis(TiConvert.toInt(dict.getInt("fastestInterval")));
        }

        if (dict.containsKey("priority")) {
            int priority = TiConvert.toInt(dict, "priority");
            switch (priority) {
                case 1:
                    locationBuilder.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY);
                    break;
                case 2:
                    locationBuilder.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
                    break;
                case 3:
                    locationBuilder.setPriority(Priority.PRIORITY_LOW_POWER);
                    break;
                default:
                    locationBuilder.setPriority(Priority.PRIORITY_PASSIVE);
                    break;
            }
        }

        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        Task<Void> locationTask = mFusedLocationClient.requestLocationUpdates(locationRequest, getLocationCallback(), null);
        locationTask.addOnSuccessListener(aVoid -> sendUpdate(completeCallback, true, ""));
        locationTask.addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
    }

    @Kroll.method
    public void stopLocationUpdates(KrollDict dict) {
        KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");

        if (locationCallback == null) {
            sendUpdate(completeCallback, false, "Cannot stop location updates since its callback is null");
            return;
        }

        Task<Void> locationTask = mFusedLocationClient.removeLocationUpdates(locationCallback);
        locationTask.addOnSuccessListener(aVoid -> sendUpdate(completeCallback, true, ""));
        locationTask.addOnFailureListener(e -> sendUpdate(completeCallback, false, e.getMessage()));
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

    private void onLocationRequestFailure(KrollFunction callback) {
        if (callback == null) {
            return;
        }

        KrollDict data = new KrollDict();
        data.put("success", false);
        data.put("message", "There is no cached location in this device, call startLocationUpdates() to get current location");

        callback.callAsync(getKrollObject(), data);
    }

    private void onLocationRequestSuccess(KrollFunction callback, Location location) {
        if (callback == null) {
            return;
        }

        callback.callAsync(getKrollObject(), createLocationPayload(location));
    }

    private KrollDict createLocationPayload(Location location) {
        KrollDict coords = new KrollDict();
        coords.put("latitude", location.getLatitude());
        coords.put("longitude", location.getLongitude());

        KrollDict locationData = new KrollDict();
        locationData.put("success", true);
        locationData.put("coords", coords);
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("altitude", location.getAltitude());
        locationData.put("provider", location.getProvider());
        locationData.put("speed", location.getSpeed());
        locationData.put("bearing", location.getBearing());
        locationData.put("time", location.getTime());
        locationData.put("isFromMockProvider", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && location.isMock());

        return locationData;
    }
}

