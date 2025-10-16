package com.example.pruebasprint0;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pruebasprint0.API.RetrofitClient;
import com.example.pruebasprint0.LOGIC.Utilidades;
import com.example.pruebasprint0.POJO.Medicion;
import com.example.pruebasprint0.POJO.TramaIBeacon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// ------------------------------------------------------------------
// ------------------------------------------------------------------

public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = ">>>>";
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;

    // --------------------------------------------------------------
    // Buscar todos los dispositivos BLE
    // --------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTL(): empieza");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, "Error en el escaneo: " + errorCode);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "No tengo permisos para escanear BLE.");
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    CODIGO_PETICION_PERMISOS);
            return;
        }
        this.elEscanner.startScan(this.callbackDelEscaneo);
    }

    // --------------------------------------------------------------
    // Mostrar información de un dispositivo BLE
    // --------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, "****************************************************");
        Log.d(ETIQUETA_LOG, "****** DISPOSITIVO DETECTADO BTLE ******************");
        Log.d(ETIQUETA_LOG, "****************************************************");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "mostrarInformacionDispositivoBTLE(): NO tengo permisos para conectar");
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS);
            return;
        }

        Log.d(ETIQUETA_LOG, "Nombre = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, "Dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, "RSSI = " + rssi);

        // Revisamos que tengamos manufacturer data
        if (bytes == null || bytes.length == 0) {
            Log.d(ETIQUETA_LOG, "No hay datos de scan disponibles");
            return;
        }

        TramaIBeacon tib = new TramaIBeacon(bytes);

        // Extraemos major y minor
        byte[] majorBytes = tib.getMajor();  // 2 bytes
        byte[] minorBytes = tib.getMinor();  // 2 bytes, normalmente valor de la medición

        if (majorBytes == null || majorBytes.length < 2) {
            Log.d(ETIQUETA_LOG, "Major data incompleta");
            return;
        }

        // Primer byte de major = ID de la medición
        int tipoMedida = majorBytes[0] & 0xFF;
        // Segundo byte de major = contador
        int contador = majorBytes[1] & 0xFF;
        // Valor de la medición = minor
        int valorMedicion = Utilidades.bytesToInt(minorBytes);

        // Convertir ID a string para identificar tipo de medida
        String tipoString;
        String unidad;
        switch (tipoMedida) {
            case 11:
                tipoString = "CO2";
                unidad = "ppm";
                break;
            case 12:
                tipoString = "Temperatura";
                unidad = "ºC";
                break;
            case 13:
                tipoString = "Ruido";
                unidad = "dB";
                break;
            default:
                tipoString = "Desconocido";
                unidad = "Desconocido";
                break;
        }

        // Log limpio solo con lo que importa
        Log.d(ETIQUETA_LOG, "*************** BEACON DETECTADO ***************");
        Log.d(ETIQUETA_LOG, "UUID = " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, "Tipo de medida (ID) = " + tipoMedida + " -> " + tipoString);
        Log.d(ETIQUETA_LOG, "Valor = " + valorMedicion + " " + unidad);
        Log.d(ETIQUETA_LOG, "Contador = " + contador);
        Log.d(ETIQUETA_LOG, "txPower = " + tib.getTxPower());
        Log.d(ETIQUETA_LOG, "RSSI = " + rssi);
        Log.d(ETIQUETA_LOG, "****************************************************");
        insertarMedicion(valorMedicion, tipoString);

    }


    // --------------------------------------------------------------
    // Buscar dispositivo por nombre
    // --------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, "Buscando dispositivo con nombre: " + dispositivoBuscado);

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);

                BluetoothDevice device = resultado.getDevice();
                String nombre = null;

                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                        nombre = device.getName();
                    }
                } catch (SecurityException e) {
                    Log.e(ETIQUETA_LOG, "Error accediendo al nombre del dispositivo", e);
                }

                if (nombre != null && nombre.contains(dispositivoBuscado)) {
                    Log.d(ETIQUETA_LOG, "Dispositivo encontrado por nombre: " + nombre);
                    mostrarInformacionDispositivoBTLE(resultado);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_SCAN no concedido.");
            return;
        }

        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(new ScanFilter.Builder().setDeviceName(dispositivoBuscado).build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        elEscanner.startScan(filtros, settings, callbackDelEscaneo);
    }

    // --------------------------------------------------------------
    // Buscar dispositivo por UUID
    // --------------------------------------------------------------
    private void buscarDispositivoPorUUID(final UUID uuidBuscado) {
        Log.d(ETIQUETA_LOG, "Buscando dispositivo con UUID: " + uuidBuscado);

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);

                ScanRecord record = resultado.getScanRecord();
                if (record == null) return;

                byte[] bytes = record.getBytes();
                TramaIBeacon tib = new TramaIBeacon(bytes);

                UUID uuidDetectado = null;
                try {
                    byte[] uuidBytes = tib.getUUID();
                    if (uuidBytes != null && uuidBytes.length >= 16) {
                        long msb = Utilidades.bytesToLong(java.util.Arrays.copyOfRange(uuidBytes, 0, 8));
                        long lsb = Utilidades.bytesToLong(java.util.Arrays.copyOfRange(uuidBytes, 8, 16));
                        uuidDetectado = new UUID(msb, lsb);
                    }
                } catch (Exception e) {
                    Log.e(ETIQUETA_LOG, "Error al convertir UUID", e);
                }

                if (uuidDetectado != null && uuidDetectado.equals(uuidBuscado)) {
                    Log.d(ETIQUETA_LOG, "Dispositivo encontrado por UUID: " + uuidDetectado);
                    mostrarInformacionDispositivoBTLE(resultado);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_SCAN no concedido.");
            return;
        }

        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(new ScanFilter.Builder()
                .setServiceUuid(new android.os.ParcelUuid(uuidBuscado))
                .build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        elEscanner.startScan(filtros, settings, callbackDelEscaneo);
    }

    // --------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {
        if (callbackDelEscaneo == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        elEscanner.stopScan(callbackDelEscaneo);
        callbackDelEscaneo = null;
        Log.d(ETIQUETA_LOG, "Escaneo detenido.");
    }

    // --------------------------------------------------------------
    // Botones
    // --------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        buscarTodosLosDispositivosBTLE();
    }

    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        buscarEsteDispositivoBTLE("beaconfrito");
    }

    public void botonBuscarPorUUIDPulsado(View v) {
        // UUID ejemplo (ajusta según el de tu beacon)
        UUID uuid = UUID.fromString("74278bda-b644-4520-8f0c-720eaf059935");
        buscarDispositivoPorUUID(uuid);
    }

    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        detenerBusquedaDispositivosBTLE();
    }

    // --------------------------------------------------------------
    private void inicializarBlueTooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            Log.e(ETIQUETA_LOG, "No se ha encontrado adaptador Bluetooth.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            bta.enable();
        }

        elEscanner = bta.getBluetoothLeScanner();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS
            );
        }
    }

    private void insertarMedicion(int major, String tipo ) {
        RetrofitClient.getApiService().insertarMedicion(new Medicion(tipo,  major)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("API Response", "Medición insertada correctamente");
                } else {
                    Log.d("API Error", "Error en la respuesta: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API Failure", "Error al conectar con el servidor", t);
            }
        });

    }
    /************************************************************
     * @fn double getMedicionsBeacon(ScanResult resultado)
     * @brief Método que obtiene el valor de la medición de un beacon.
     * @param[in] resultado Objeto ScanResult con la información del dispositivo detectado.
     * @return Valor de la medición.
     ************************************************************/
    public double getMedicionsBeacon(ScanResult resultado) {
        byte[] bytes = resultado.getScanRecord().getBytes();
        TramaIBeacon tib = new TramaIBeacon(bytes);
        return Utilidades.bytesToInt(tib.getMinor());
    }


    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializarBlueTooth();
    }
}
