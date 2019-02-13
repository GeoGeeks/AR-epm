package com.esri.alejo.arepm;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatusChangedEvent;
import com.esri.arcgisruntime.loadable.LoadStatusChangedListener;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.FirstPersonCameraController;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.PhoneMotionDataSource;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {

    private Button btnLocalizacion;
    private TextView txtLocalizacion;

    final static String TAG = "MainActivity";

    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};

    private LocationManager locationManager;
    public Location location;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    boolean locationServiceAvailable;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 0;//1000 * 60 * 1; // 1 minute

    ///// para la parte de realidad aumentada
    private SceneView mSceneView;
    private android.hardware.Camera mCamera;
    private CameraPreview mPreview;
    private static final int PERMISSION_TO_USE_CAMERA = 0;

    //// parte del mini map
    private MapView vistaMapLittle;
    private ArcGISMap mapaLittle;
    public LocationDisplay locationDisplay;
    public RelativeLayout contentMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud9088059687,none,HC5X0H4AH4YDXH46C082");
        setContentView(R.layout.activity_main);
        initRecursos();
        //contenido de ar
        mSceneView = findViewById(R.id.sceneView);
        //inicia el servicio de localizacion
        initLocationService();
        // Request camera permissions...
        checkForCameraPermissions();
        //agregar mapa pequeño
        createLittleMap();

    }

    private void initRecursos() {
        txtLocalizacion = (TextView) findViewById(R.id.txtLocalizacion);

        btnLocalizacion = (Button) this.findViewById(R.id.btnLocalizacion);
        btnLocalizacion.setOnClickListener(this);
        contentMap = (RelativeLayout) this.findViewById(R.id.layout_miniMap);
    }

    /*public void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSIONS_CODE);
        } else {
            initLocationService();
        }
    }*/

    private void initLocationService() {

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        try   {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

            // Get GPS and network status
            this.isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            this.isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isNetworkEnabled && !isGPSEnabled)    {
                // cannot get location
                this.locationServiceAvailable = false;
            }

            this.locationServiceAvailable = true;

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (locationManager != null)   {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    updateLatestLocation();
                }
            }

            if (isGPSEnabled)  {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null)  {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateLatestLocation();
                }
            }
        } catch (Exception ex)  {
            Log.e(TAG, ex.getMessage());

        }
    }

    private void updateLatestLocation() {
        if (location != null) {
            txtLocalizacion.setText(String.format("lat: %s \nlon: %s \naltitude: %s \naccuracy: %s \ngrados: %s",
                    location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy(), location.getBearing()));
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLocalizacion:
                //getLocalizacion();
                //Toast.makeText(v.getContext(), "localizacion", Toast.LENGTH_SHORT).show();
                //requestLocationPermission();
                initLocationService();
                break;
        }
    }
    /**
    public void getGeoLocalizacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            String locationProviderGPS = locationManager.GPS_PROVIDER;
            String locationProviderNET = locationManager.NETWORK_PROVIDER;
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProviderGPS);
            String longitude = "Longitude: " + lastKnownLocation.getLongitude();
            String latitude = "Latitude: " + lastKnownLocation.getLatitude();
            txtLocalizacion.setText(longitude+'-'+latitude);
        }catch (Exception ex)  {
            Log.e(TAG, ex.getMessage());
        }

     }**/

    @Override
    public void onLocationChanged(Location location) {
        updateLatestLocation();
    }

     @Override
     public void onStatusChanged(String provider, int status, Bundle extras) {

     }

     @Override
     public void onProviderEnabled(String provider) {

     }

     @Override
     public void onProviderDisabled(String provider) {

     }

    /** set up para uso de realidad aumentada
     * Determine if we're able to use the camera
     */
    private void checkForCameraPermissions() {
        // Explicitly check for privilege
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            final int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "Camera permission granted");
                setUpARScene();

            } else {
                Log.i("MainActivity", "Camera permission not granted, asking ....");
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.CAMERA },
                        PERMISSION_TO_USE_CAMERA);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_TO_USE_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("MainActivity", "Camera permission granted...");
                    setUpARScene();
                } else {
                    Log.i("MainActivity", "Camera permission denied...");
                }
                return;
            }
        }
    }

    //Setup the Scene for Augmented Reality
    private void setUpARScene() {
        try{
            // Create scene without a basemap.  Background for scene content provided by device camera.
            mSceneView.setScene(new ArcGISScene());

            // Add San Diego scene layer.  This operational data will render on a video feed (eg from the device camera).
            //mSceneView.getScene().getOperationalLayers().add(new ArcGISSceneLayer("https://tiles.arcgis.com/tiles/Imiq6naek6ZWdour/arcgis/rest/services/San_Diego_Textured_Buildings/SceneServer/layers/0"));
            mSceneView.getScene().getOperationalLayers().add(new ArcGISSceneLayer("http://geogeeks2.maps.arcgis.com/home/webscene/viewer.html?webscene=cd82b7b61458456896bb3226b86bb20b"));

            // Enable AR for scene view.
            mSceneView.setARModeEnabled(true);

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this);

            // Create an instance of Camera
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            completeSetup();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void completeSetup() {
        // Scene camera controlled by sensors
        //Camera cameraSanDiego = new Camera(32.707, -117.157, 60, 180, 0, 0);
        Camera cam = new Camera(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getBearing(), 0, 0);
        FirstPersonCameraController fpcController = new FirstPersonCameraController();
        fpcController.setInitialPosition(cam);

        // PhoneMotionDataSource works with both Android and iOS.
        PhoneMotionDataSource phoneSensors = new PhoneMotionDataSource(this);
        fpcController.setDeviceMotionDataSource(phoneSensors);

        fpcController.setFramerate(FirstPersonCameraController.FirstPersonFramerate.BALANCED);
        mSceneView.setCameraController(fpcController);

        // To update position and orientation of the camera with device sensors use:
        phoneSensors.startUpdatingAngles(false);
        // To update location of camera in the scene with device location (GPS) use:
        //phoneSensors.startUpdatingInitialPosition();
        // To update both use:
        //phoneSensors.startAll();
    }

    @Override
    protected void onPause(){
        releaseCamera();
        mSceneView.pause();
        vistaMapLittle.pause();
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mSceneView.resume();
        vistaMapLittle.resume();
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * mapa pequeño
     * **/

    public void createLittleMap(){
        //fragMap = new fragmentMapa();
        vistaMapLittle = this.findViewById(R.id.mapView);
        //mapaLittle = new ArcGISMap(this.getResources().getString(R.string.URL_mapa_alrededores));
        Log.d("MainActivity", "se agrega la url");

        Basemap.Type basemapType = Basemap.Type.STREETS_VECTOR;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        int levelOfDetail = 11;
        ArcGISMap mapaLittle = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);

        vistaMapLittle.setMap(mapaLittle);
        vistaMapLittle.setVisibility(View.VISIBLE);
        vistaMapLittle.setBackgroundGrid(new BackgroundGrid(Color.WHITE, Color.WHITE, 0, vistaMapLittle.getBackgroundGrid().getGridSize()));
        vistaMapLittle.setWrapAroundMode(WrapAroundMode.DISABLED);

        locationDisplay = vistaMapLittle.getLocationDisplay();
        locationDisplay.startAsync();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        //vistaMapLittle.setOnTouchListener(new IdentifyFeatureLayerTouchListener(vistaMapLittle.getContext(), vistaMapLittle));
        mapaLittle.addLoadStatusChangedListener(new LoadStatusChangedListener() {
            @Override
            public void loadStatusChanged(LoadStatusChangedEvent loadStatusChangedEvent) {
                Log.d("MainActivity", "entra a la carga");
                String mapLoadStatus;
                mapLoadStatus = loadStatusChangedEvent.getNewLoadStatus().name();
                switch (mapLoadStatus) {
                    case "LOADED":
                        Log.d("MainActivity", "cargado");
                        Toast.makeText(vistaMapLittle.getContext(),"Cargado",Toast.LENGTH_LONG).show();
                        contentMap.setVisibility(View.VISIBLE);

                        /**hacerConsulta(getResources().getString(R.string.URL_capa_parqueaderos));
                        LayerList layers = mapaLittle.getOperationalLayers();
                        if(!layers.isEmpty()){
                            parqueaderos = (FeatureLayer) layers.get(0);
                            restaurantes = (FeatureLayer) layers.get(1);
                            hoteles = (FeatureLayer) layers.get(2);
                        }**/
                        if(mapaLittle.getInitialViewpoint() != null){
                            vistaMapLittle.setViewpoint(mapaLittle.getInitialViewpoint());
                        }
                        break;
                }
            }
        });
    }
 }
