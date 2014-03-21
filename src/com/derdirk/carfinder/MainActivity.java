package com.derdirk.carfinder;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity 
implements OnMapLongClickListener, LocationListener
{
  // Variables
  
  private GoogleMap        _map;
  private Marker           _carMarker;
  private Location         _currentLocation;
  private LocationProvider _locationProvider;
  private Boolean          _showInitialPosition = true;

  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    _locationProvider = new LocationProvider(this, this);
    if (!_locationProvider.init())
      finish();
        
    FragmentManager myFragmentManager = getSupportFragmentManager();
    SupportMapFragment mySupportMapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.map);
    _map = mySupportMapFragment.getMap();
    _map.setMyLocationEnabled(true);
    _map.setOnMapLongClickListener(this);

    final Button setCarbutton = (Button) findViewById(R.id.setCarButton);
    setCarbutton.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        if (_currentLocation == null)
        {
          Toast.makeText(getApplicationContext(), "Current location not yet found", Toast.LENGTH_LONG).show();
        }
        else
        {
          setMarker(_currentLocation.getLatitude(), _currentLocation.getLongitude());
        }
      }
    });
    
    final Button findCarbutton = (Button) findViewById(R.id.findCarButton);
    findCarbutton.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        if (_carMarker == null)
          Toast.makeText(getApplicationContext(), "No car", Toast.LENGTH_LONG).show();
        else
          moveToLocation(_carMarker.getPosition());
      }
    });
    
    final Button navigateCarbutton = (Button) findViewById(R.id.navigateCarButton);
    navigateCarbutton.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View v)
      {
        if (_carMarker == null)
        {
          Toast.makeText(getApplicationContext(), "No car", Toast.LENGTH_LONG).show();
        }
        else if (_currentLocation == null)
        {
          Toast.makeText(getApplicationContext(), "Current location not yet found", Toast.LENGTH_LONG).show();
        }
        else
        {
          LatLng carPoint = _carMarker.getPosition();
          String uri1 = "geo:" + carPoint.latitude + "," + carPoint.longitude + "?q=" + carPoint.latitude + "," + carPoint.longitude;
          //Toast.makeText(getApplicationContext(),  uri1, Toast.LENGTH_LONG).show();
          Intent i1 = new Intent(Intent.ACTION_VIEW, Uri.parse(uri1));
          startActivity(i1);
        }
      }
    });
  }

  ////////////////////////////////////
  // Activity lifecycle overrides
  
  @Override
  protected void onResume()
  {
    super.onResume();

    // Restore preferences
    SharedPreferences settings = getPreferences(MODE_PRIVATE);
    double latitude = settings.getFloat("latitude", 0);
    double longitude = settings.getFloat("longitude", 0);

    if (latitude != 0 || longitude != 0)
      setMarker(latitude, longitude);
    
    _locationProvider.resume();
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    
    if (_carMarker != null)
    {
      LatLng pos = _carMarker.getPosition();
      
      // We need an Editor object to make preference changes.
      // All objects are from android.context.Context
      SharedPreferences settings = getPreferences(MODE_PRIVATE);
      SharedPreferences.Editor editor = settings.edit();
      editor.putFloat("latitude", (float) pos.latitude);
      editor.putFloat("longitude", (float) pos.longitude);
  
      // Commit the edits!
      editor.commit();
    }
    
    _locationProvider.pause();
  }

  /*
   * Handle results returned to the FragmentActivity by Google Play services
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
      // Decide what to do based on the original request code
      switch (requestCode)
      {
          case LocationProvider.CONNECTION_FAILURE_RESOLUTION_REQUEST :
          /*
           * If the result code is Activity.RESULT_OK, try
           * to connect again
           */
              switch (resultCode)
              {
                  case Activity.RESULT_OK :
                  /*
                   * Try the request again
                   */
                  break;
              }
      }
  }
  
  @Override
  public void onMapLongClick(LatLng point)
  {
    // Toast.makeText(getApplicationContext(), point.toString(),
    // Toast.LENGTH_SHORT).show();
    // _map.animateCamera(CameraUpdateFactory.newLatLng(point));

    double latitude = point.latitude;
    double longitude = point.longitude;

    setMarker(latitude, longitude);
  }
  
  ////////////////////////////////////
  // Helper
  
  private void setMarker(double latitude, double longitude)
  {
    String addressText = "";

    Geocoder geocoder = new Geocoder(this);
    try
    {
      List<Address> result = geocoder.getFromLocation(latitude, longitude, 1);
      addressText = Utilities.AddressToText(result.get(0)).toString();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    catch (IndexOutOfBoundsException e)
    {}

    if (_carMarker != null)
    {
      _carMarker.setPosition(new LatLng(latitude, longitude));
      _carMarker.setSnippet(addressText);
    }
    else
    {
      _carMarker = _map.addMarker(new MarkerOptions()
        .position(new LatLng(latitude, longitude))
        .anchor(0.5f, 0.5f)
        .title(getString(R.string.car_location))
        .snippet(addressText)
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_orange_icon)));
    }
  }

  private void moveToLocation(LatLng latLng)
  {
    moveToLocation(latLng, _map.getCameraPosition().zoom);
  }
  
  private void moveToLocation(LatLng latLng, float zoom)
  {
    CameraPosition cameraPosition = new CameraPosition.Builder()
      .target(latLng)
      .zoom(zoom)
      .build();
    _map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }
  
  // Define the callback method that receives location updates
  @Override
  public void onLocationChanged(Location location)
  {
    _currentLocation = location;
    if (_showInitialPosition)
    {
      moveToLocation(new LatLng(location.getLatitude(), location.getLongitude()), 17);
      _showInitialPosition = false;
    }
  }
      
  // @Override
  // public boolean onCreateOptionsMenu(Menu menu) {
  // // Inflate the menu; this adds items to the action bar if it is present.
  // getMenuInflater().inflate(R.menu.main, menu);
  // return true;
  // }

}

