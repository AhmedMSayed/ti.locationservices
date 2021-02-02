package ti.locationservices;

import android.annotation.SuppressLint;
import android.location.Location;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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


@Kroll.module(name = "TiLocationservices", id = "ti.locationservices")
public class TiLocationservicesModule extends KrollModule {

    private static final String LCAT = "TiLocationservicesModule";
    private final TiApplication context = TiApplication.getInstance();
    private final FusedLocationProviderClient mFusedLocationClient;
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;
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

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            KrollDict coords = new KrollDict();
                            coords.put("latitude", location.getLatitude());
                            coords.put("longitude", location.getLongitude());

                            KrollDict data = new KrollDict();
                            data.put("coords", coords);
                            data.put("accuracy", location.getAccuracy());
                            data.put("altitude", location.getAltitude());
                            data.put("provider", location.getProvider());
                            data.put("speed", location.getSpeed());
                            data.put("bearing", location.getBearing());
                            data.put("time", location.getTime());
                            data.put("isFromMockProvider", location.isFromMockProvider());
                            fireEvent("location", data);
                        }
                    }
                }
            }
        };

        builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
    }

    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
    }

    // Methods
    @Kroll.method
    public void checkLocationSettings(KrollDict dict) {
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(context.getCurrentActivity()).checkLocationSettings(builder.build());

        if (dict.containsKeyAndNotNull("onComplete")) {
            KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");
            KrollDict data = new KrollDict();

            result.addOnSuccessListener(locationSettingsResponse -> {
                data.put("success", true);
                completeCallback.callAsync(getKrollObject(), data);
            });

            result.addOnFailureListener(e -> {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;

                    if (resolvable.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            Log.i(LCAT, "Location Settings are not satisfied, setting dialog going to show now");

                            TiBaseActivity baseActivity = (TiBaseActivity) context.getCurrentActivity();
                            REQUEST_CHECK_SETTINGS = baseActivity.getUniqueResultCode();

                            final TiLifecycle.OnActivityResultEvent resultEvent = (activity, requestCode, resultCode, intent) -> {
                                if (requestCode == REQUEST_CHECK_SETTINGS) {
                                    if (resultCode == RESULT_OK) {
                                        data.put("success", true);
                                    } else {
                                        data.put("success", false);
                                        data.put("message", "Failed :: User canceled");
                                    }
                                    completeCallback.callAsync(getKrollObject(), data);
                                }
                            };

                            baseActivity.addOnActivityResultListener(resultEvent);
                            resolvable.startResolutionForResult(baseActivity, REQUEST_CHECK_SETTINGS);

                        } catch (Exception exception) {
                            data.put("success", false);
                            data.put("message", "Failed :: " + exception.getMessage());
                            completeCallback.callAsync(getKrollObject(), data);
                        }
                    } else {
                        Log.i(LCAT, "Location settings can't be changed to meet the requirements, no dialog pops up");
                        data.put("success", false);
                        data.put("message", "Failed :: " + LocationSettingsStatusCodes.getStatusCodeString(resolvable.getStatusCode()));
                        completeCallback.callAsync(getKrollObject(), data);
                    }
                } else {
                    data.put("success", false);
                    data.put("message", "Failed :: " + e.getMessage());
                    completeCallback.callAsync(getKrollObject(), data);
                }
            });
        } else {
            Log.e(LCAT, "Missing or Null onComplete() method inside checkLocationSettings()");
        }
    }

    @SuppressLint("MissingPermission")
    @Kroll.method
    public void getCurrentLocation(KrollDict dict) {
        Task<Location> locationTask = mFusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null);

        if (dict.containsKeyAndNotNull("onComplete")) {
            KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");
            KrollDict data = new KrollDict();

            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    KrollDict coords = new KrollDict();
                    coords.put("latitude", location.getLatitude());
                    coords.put("longitude", location.getLongitude());

                    data.put("success", true);
                    data.put("coords", coords);
                    data.put("accuracy", location.getAccuracy());
                    data.put("altitude", location.getAltitude());
                    data.put("provider", location.getProvider());
                    data.put("speed", location.getSpeed());
                    data.put("bearing", location.getBearing());
                    data.put("time", location.getTime());
                    data.put("isFromMockProvider", location.isFromMockProvider());
                } else {
                    data.put("success", false);
                    data.put("message", "Failed :: There is no cached location in this device, call startLocationUpdates() to get current location");
                }
                completeCallback.callAsync(getKrollObject(), data);
            });

            locationTask.addOnFailureListener(e -> {
                data.put("success", false);
                data.put("message", "Failed :: " + e.getMessage());
                completeCallback.callAsync(getKrollObject(), data);
            });
        }
    }


    @SuppressLint("MissingPermission")
    @Kroll.method
    public void getLastLocation(KrollDict dict) {
        Task<Location> locationTask = mFusedLocationClient.getLastLocation();

        if (dict.containsKeyAndNotNull("onComplete")) {
            KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");
            KrollDict data = new KrollDict();

            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    KrollDict coords = new KrollDict();
                    coords.put("latitude", location.getLatitude());
                    coords.put("longitude", location.getLongitude());

                    data.put("success", true);
                    data.put("coords", coords);
                    data.put("accuracy", location.getAccuracy());
                    data.put("altitude", location.getAltitude());
                    data.put("provider", location.getProvider());
                    data.put("speed", location.getSpeed());
                    data.put("bearing", location.getBearing());
                    data.put("time", location.getTime());
                    data.put("isFromMockProvider", location.isFromMockProvider());
                } else {
                    data.put("success", false);
                    data.put("message", "Failed :: There is no cached location in this device, call startLocationUpdates() to get current location");
                }
                completeCallback.callAsync(getKrollObject(), data);
            });

            locationTask.addOnFailureListener(e -> {
                data.put("success", false);
                data.put("message", "Failed :: " + e.getMessage());
                completeCallback.callAsync(getKrollObject(), data);
            });
        }
    }

    @Kroll.method
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(KrollDict dict) {
        if (dict.containsKey("interval")) {
            locationRequest.setInterval(TiConvert.toInt(dict.getInt("interval")));
        }

        if (dict.containsKey("fastestInterval")) {
            locationRequest.setFastestInterval(TiConvert.toInt(dict.getInt("fastestInterval")));
        }

        if (dict.containsKey("priority")) {
            int priority = TiConvert.toInt(dict, "priority");
            switch (priority) {
                case 1:
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    break;
                case 2:
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    break;
                case 3:
                    locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                    break;
                default:
                    locationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
                    break;

            }
        }

        Task<Void> locationTask = mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        if (dict.containsKeyAndNotNull("onComplete")) {
            KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");
            KrollDict data = new KrollDict();

            locationTask.addOnSuccessListener(aVoid -> {
                data.put("success", true);
                completeCallback.callAsync(getKrollObject(), data);

            });

            locationTask.addOnFailureListener(e -> {
                data.put("success", false);
                data.put("message", "Failed :: " + e.getMessage());
                completeCallback.callAsync(getKrollObject(), data);
            });
        }
    }

    @Kroll.method
    public void stopLocationUpdates(KrollDict dict) {
        Task<Void> locationTask = mFusedLocationClient.removeLocationUpdates(locationCallback);

        if (dict.containsKeyAndNotNull("onComplete")) {
            KrollFunction completeCallback = (KrollFunction) dict.get("onComplete");
            KrollDict data = new KrollDict();

            locationTask.addOnSuccessListener(aVoid -> {
                data.put("success", true);
                completeCallback.callAsync(getKrollObject(), data);
            });

            locationTask.addOnFailureListener(e -> {
                data.put("success", false);
                data.put("message", "Failed :: " + e.getMessage());
                completeCallback.callAsync(getKrollObject(), data);
            });
        }
    }
}

