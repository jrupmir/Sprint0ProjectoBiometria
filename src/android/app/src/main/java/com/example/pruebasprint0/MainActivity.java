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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empieza ");

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanResult() ");

                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onBatchScanResults() ");

            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanFailed() ");

            }
        };

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empezamos a escanear ");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): NO tengo permisos para escanear ");
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
        BluetoothDevice device = resultado.getDevice();

        String nombre = null;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                nombre = device.getName();
            }
        } catch (SecurityException e) {
            Log.e(ETIQUETA_LOG, "Sin permiso para obtener nombre del dispositivo", e);
        }

        if (nombre == null) nombre = "Desconocido";

        Log.d(ETIQUETA_LOG, "****************************************************");
        Log.d(ETIQUETA_LOG, "Dispositivo detectado BTLE:");
        Log.d(ETIQUETA_LOG, "Nombre = " + nombre);
        Log.d(ETIQUETA_LOG, "Dirección = " + device.getAddress());
        Log.d(ETIQUETA_LOG, "RSSI = " + resultado.getRssi());

        ScanRecord record = resultado.getScanRecord();
        if (record == null) {
            Log.d(ETIQUETA_LOG, "Sin ScanRecord disponible");
            return;
        }

        byte[] bytes = record.getBytes();
        Log.d(ETIQUETA_LOG, "Bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        TramaIBeacon tib = new TramaIBeacon(bytes);
        Log.d(ETIQUETA_LOG, "UUID = " + Utilidades.bytesToHexString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, "Major = " + Utilidades.bytesToInt(tib.getMajor()));
        Log.d(ETIQUETA_LOG, "Minor = " + Utilidades.bytesToInt(tib.getMinor()));
        Log.d(ETIQUETA_LOG, "****************************************************");
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
    // Buscar dispositivo por UUID (nuevo método)
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
        buscarEsteDispositivoBTLE("fistro");
    }

    public void botonBuscarPorUUIDPulsado(View v) {
        // UUID ejemplo (debes reemplazarlo por el tuyo real)
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

    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializarBlueTooth();
    }
}
