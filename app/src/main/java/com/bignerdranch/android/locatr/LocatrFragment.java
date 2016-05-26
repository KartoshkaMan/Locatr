package com.bignerdranch.android.locatr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class LocatrFragment extends SupportMapFragment {

    private static final int REQUEST_PERMISSION_FINE_LOCATION = 0;
    private static final String TAG = "LocatrFragment";

    private Bitmap mMapImage;
    private GalleryItem mMapItem;
    private GoogleApiClient mClient;
    private GoogleMap mMap;
    private Location mCurrentLocation;



    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_locate:
                checkPermissionAndFindImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);

        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                updateUI();
            }
        });

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);

        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mClient.isConnected());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSION_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findImage();
                    Toast.makeText(getActivity(), "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
                }

                break;
            }
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }



    private void checkPermissionAndFindImage() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION);
    }
    private void findImage() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, request, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    new SearchTask().execute(location);
                }
            });
        } catch (SecurityException se) {
            Log.e(TAG, "Permission DENIED", se);
        }
    }
    private void updateUI() {
        if (mMap == null || mMapImage == null) {
            return;
        }

        LatLng itemPoint = new LatLng(mMapItem.getLat(), mMapItem.getLon());
        LatLng userPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);
        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(itemBitmap);
        MarkerOptions userMarker = new MarkerOptions()
                .position(userPoint);

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(itemPoint)
                .include(userPoint)
                .build();

        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(userMarker);

        int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        mMap.animateCamera(update);

    }



    private class SearchTask extends AsyncTask<Location, Void, Void> {

        private Bitmap mBitmap;
        private GalleryItem mGalleryItem;
        private Location mLocation;

        @Override
        protected Void doInBackground(Location... params) {
            mLocation = params[0];
            FlickrFetchr ff = new FlickrFetchr();
            List<GalleryItem> items = ff.searchPhotos(mLocation);

            if (items.size() == 0) {
                return null;
            } else {
                mGalleryItem = items.get(0);

                try {
                    byte[] bytes = ff.getUrlBytes(mGalleryItem.getUrl());
                    mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (IOException ioe) {
                    Log.i(TAG, "Unable to download bitmap", ioe);
                }

                return null;
            }


        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mCurrentLocation = mLocation;
            mMapImage = mBitmap;
            mMapItem = mGalleryItem;

            updateUI();
        }
    }


}
