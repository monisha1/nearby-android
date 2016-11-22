/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.esri.android.nearbyplaces.places;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.esri.android.nearbyplaces.R;
import com.esri.android.nearbyplaces.data.CategoryHelper;
import com.esri.android.nearbyplaces.data.LocationService;
import com.esri.android.nearbyplaces.data.Place;
import com.esri.arcgisruntime.geometry.Envelope;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PlacesFragment extends Fragment implements PlacesContract.View,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

  private PlacesContract.Presenter mPresenter;

  private PlacesFragment.PlacesAdapter mPlaceAdapter;

  private RecyclerView mPlacesView;

  private static final String TAG = PlacesFragment.class.getSimpleName();

  private GoogleApiClient mGoogleApiClient;
  private Location mLastLocation;

  public PlacesFragment(){

  }
  public static  PlacesFragment newInstance(){
    return new PlacesFragment();

  }
  @Override
  public final void onCreate(@NonNull final Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    // retain this fragment
    setRetainInstance(true);
    final List<Place> placeList = new ArrayList<>();

    mPlaceAdapter = new PlacesFragment.PlacesAdapter(getContext(), R.id.placesContainer,placeList);

    // Create an instance of GoogleAPIClient.
    if (mGoogleApiClient == null) {
      mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .addApi(LocationServices.API)
          .build();
    }
  }

  @Nullable
  @Override
  public final View onCreateView(final LayoutInflater inflater, final ViewGroup container,
      final Bundle savedInstance){

    mPlacesView= (RecyclerView) inflater.inflate(
        R.layout.places_fragment2, container, false);

    mPlacesView.setLayoutManager(new LinearLayoutManager(mPlacesView.getContext()));
    mPlacesView.setAdapter(mPlaceAdapter);

    return mPlacesView;
  }

  @Override
  public final void onResume() {
    super.onResume();
    if (mPresenter != null){
      mPresenter.start();
    }
  }

  @Override
  public final void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override public final void showNearbyPlaces(final List<Place> places) {
    Collections.sort(places);
    mPlaceAdapter.setPlaces(places);
    mPlaceAdapter.notifyDataSetChanged();
  }

  // TODO: Implement support for progress indicators
  @Override public void showProgressIndicator(final boolean active) {
  }

  @Override public final boolean isActive() {
    return false;
  }

  @Override public final void setPresenter(final PlacesContract.Presenter presenter) {
    mPresenter = checkNotNull(presenter);
  }


  public  class PlacesAdapter extends RecyclerView.Adapter<PlacesFragment.RecyclerViewHolder> {

    private List<Place> mPlaces = Collections.emptyList();
    public PlacesAdapter(final Context context, final int resource, final List<Place> places){
          mPlaces = places;
    }

    public final void setPlaces(final List<Place> places){
      checkNotNull(places);
      mPlaces = places;
      notifyDataSetChanged();
    }

    @Override public final PlacesFragment.RecyclerViewHolder onCreateViewHolder(final ViewGroup parent,
        final int viewType) {
      final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      final View itemView = inflater.inflate(R.layout.place, parent, false);
      return new PlacesFragment.RecyclerViewHolder(itemView);
    }


    @Override public final void onBindViewHolder(final PlacesFragment.RecyclerViewHolder holder, final int position) {
      final Place place = mPlaces.get(position);
      holder.placeName.setText(place.getName());
      holder.address.setText(place.getAddress());
      final Drawable drawable = assignIcon(position);
      holder.icon.setImageDrawable(drawable);
      holder.bearing.setText(place.getBearing());
      holder.distance.setText(place.getDistance() + "m");
      holder.bind(place);
    }

    @Override public final int getItemCount() {
      return mPlaces.size();
    }

    private Drawable assignIcon(final int position){
      final Place p = mPlaces.get(position);
      return CategoryHelper.getDrawableForPlace(p, getActivity());
    }
  }


  @Override public final void onConnected(@Nullable final Bundle bundle) {
    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
        mGoogleApiClient);
    if (mLastLocation != null){
      Log.i(PlacesFragment.TAG, "Latitude/longitude from FusedLocationApi " + mLastLocation.getLatitude() + '/' + mLastLocation.getLongitude());
      mPresenter.setLocation(mLastLocation);
      final LocationService locationService = LocationService.getInstance();
      locationService.setCurrentLocation(mLastLocation);
      mPresenter.start();
    }
  }

  @Override public void onConnectionSuspended(final int i) {

  }
  @Override public final void onStart() {
    mGoogleApiClient.connect();
    super.onStart();
  }

  @Override  public final void onStop() {
    mGoogleApiClient.disconnect();
    super.onStop();
  }

  @Override public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {

  }
  public class RecyclerViewHolder extends RecyclerView.ViewHolder {

    public final TextView placeName;
    public final TextView address;
    public final ImageView icon;
    public final TextView bearing;
    public final TextView distance;

    public RecyclerViewHolder(final View itemView) {
      super(itemView);
      placeName = (TextView) itemView.findViewById(R.id.placeName);
      address = (TextView) itemView.findViewById(R.id.placeAddress);
      icon = (ImageView) itemView.findViewById(R.id.placeTypeIcon);
      bearing = (TextView) itemView.findViewById(R.id.placeBearing);
      distance = (TextView) itemView.findViewById(R.id.placeDistance);
    }
    public final void bind(final Place place){
      itemView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(final View v) {
          final Envelope envelope = mPresenter.getExtentForNearbyPlaces();
          final Intent intent = PlacesActivity.createMapIntent(getActivity(),envelope);
          intent.putExtra("PLACE_DETAIL", place.getName());
          startActivity(intent);
        }
      });
    }

  }
}