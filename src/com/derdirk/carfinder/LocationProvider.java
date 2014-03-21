package com.derdirk.carfinder;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationProvider implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener
{
  // Variables
  
  private Context           _context;
  private LocationListener  _locationListener;
  private LocationRequest   _locationRequest;
  private LocationClient    _locationClient;
  
  // Global constants
  
  // Request code to send to Google Play services. This code is returned in Activity.onActivityResult.
  public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    
  // Milliseconds per second
  private static final int MILLISECONDS_PER_SECOND = 1000;
  // Update frequency in seconds
  public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
  // Update frequency in milliseconds
  private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
  // The fastest update frequency, in seconds
  private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
  // A fast frequency ceiling in milliseconds
  private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
  
  // Define a DialogFragment that displays the error dialog
  public static class ErrorDialogFragment extends DialogFragment {
      // Global field to contain the error dialog
      private Dialog mDialog;
      // Default constructor. Sets the dialog field to null
      public ErrorDialogFragment() {
          super();
          mDialog = null;
      }
      // Set the dialog to display
      public void setDialog(Dialog dialog) {
          mDialog = dialog;
      }
      // Return a Dialog to the DialogFragment.
      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
          return mDialog;
      }
  }
  
  public LocationProvider(Context context, LocationListener listener)
  {
    _context          = context;
    _locationListener = listener;
  }
  
  public Boolean init()
  {
    if (!ensurePlayServiceIsAvailable()) // TODO: Ok? Don't we miss later updates then?
      return false;
    
    _locationClient = new LocationClient(_context, this, this);

    // Create the LocationRequest object
    _locationRequest = LocationRequest.create();
    // Use high accuracy
    _locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    // Set the update interval to 5 seconds
    _locationRequest.setInterval(UPDATE_INTERVAL);
    // Set the fastest update interval to 1 second
    _locationRequest.setFastestInterval(FASTEST_INTERVAL);

    Log.i("LocationProvider", "Location provider initialized");
    
    return true;
  }

  public Boolean ensurePlayServiceIsAvailable()
  {
    // check Google Play service APK is available and up to date.
    // see http://developer.android.com/google/play-services/setup.html
    final int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(_context);
    if (result != ConnectionResult.SUCCESS)
    {
      String message = "Google Play service is not available (status=" + result + ")";
      Toast.makeText(_context, message, Toast.LENGTH_LONG).show();
      Log.e("LocationProvider", message);
      return false;
    }
    return true;
  }
  
  public void pause()
  {
    Log.i("LocationProvider", "Pausing location provider");
    
    if (_locationClient.isConnected())
      _locationClient.removeLocationUpdates(this);
    _locationClient.disconnect();
  }

  public void resume()
  {
    Log.i("LocationProvider", "Resuming location provider");
    
    if (!ensurePlayServiceIsAvailable())
      return;

    if (!_locationClient.isConnected())
      _locationClient.connect();
  }
  
  ////////////////////////////////////
  // LocationClient overrides
  
  @Override
  public void onConnected(Bundle arg0)
  {
    //Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
    Log.i("LocationProvider", "Location provider connected");
    _locationClient.requestLocationUpdates(_locationRequest, this);
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult)
  {
    Toast.makeText(_context, "Connection Failed", Toast.LENGTH_LONG).show();
    Log.e("LocationProvider", "Error connecting to location provider");
    
    int errorCode = connectionResult.getErrorCode();
    // Get the error dialog from Google Play services
    Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode,
            (Activity)_context,
            CONNECTION_FAILURE_RESOLUTION_REQUEST);
    // If Google Play services can provide an error dialog
    if (errorDialog != null) {
        // Create a new DialogFragment for the error dialog
        ErrorDialogFragment errorFragment =
                new ErrorDialogFragment();
        // Set the dialog in the DialogFragment
        errorFragment.setDialog(errorDialog);
        // Show the error dialog in the DialogFragment
        errorFragment.show(
                ((FragmentActivity)_context).getSupportFragmentManager(),
                "Location Updates");
    }
  }

  @Override
  public void onDisconnected()
  {
    Log.i("LocationProvider", "Disconnected location provider");
    //Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
  }

  // Define the callback method that receives location updates
  @Override
  public void onLocationChanged(Location location)
  {
    Log.d("LocationProvider", "Location changed to " + location.toString());
    _locationListener.onLocationChanged(location);
  }

  
}
