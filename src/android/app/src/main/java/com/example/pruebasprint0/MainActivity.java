package com.example.pruebasprint0;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
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

// --------------------------------------------------------------
// MainActivity - versión corregida BLE Android 12+
// --------------------------------------------------------------

public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = ">>>>";
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;

    // --------------------------------------------------------------
    // Escaneo general de todos los dispositivos BLE
    // --------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTL(): empieza");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTL(): onScanResult()");
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTL(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, "buscarTodosLosDispositivosBTL(): onScanFailed() código: " + errorCode);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_SCAN no concedido.");
            return;
        }

        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTL(): empezamos a escanear");
        this.elEscanner.startScan(this.callbackDelEscaneo);
    }

    // --------------------------------------------------------------
    // Mostrar información detallada de un dispositivo detectado
    // --------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, "****************************************************");
        Log.d(ETIQUETA_LOG, "****** DISPOSITIVO DETECTADO BTLE ******************");
        Log.d(ETIQUETA_LOG, "****************************************************");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_CONNECT no concedido.");
            return;
        }

        Log.d(ETIQUETA_LOG, "nombre = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, "dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, "rssi = " + rssi);
        Log.d(ETIQUETA_LOG, "bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        TramaIBeacon tib = new TramaIBeacon(bytes);

        Log.d(ETIQUETA_LOG, "----------------------------------------------------");
        Log.d(ETIQUETA_LOG, "uuid  = " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, "major = " + Utilidades.bytesToInt(tib.getMajor()));
        Log.d(ETIQUETA_LOG, "minor = " + Utilidades.bytesToInt(tib.getMinor()));
        Log.d(ETIQUETA_LOG, "txPower = " + tib.getTxPower());
        Log.d(ETIQUETA_LOG, "****************************************************");
    }

    // --------------------------------------------------------------
    // Escanear buscando un nombre de dispositivo específico
    // --------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): empieza");

        this.callbackDelEscaneo = new ScanCallback() {
            @SuppressLint("MissingPermission") // Permiso ya controlado
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                BluetoothDevice device = resultado.getDevice();
                String nombre = null;

                // Comprobamos permiso antes de leer el nombre
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    nombre = device.getName();
                } else {
                    Log.w(ETIQUETA_LOG, "Permiso BLUETOOTH_CONNECT no concedido: no se puede leer el nombre");
                }

                if (nombre != null && nombre.contains(dispositivoBuscado)) {
                    Log.d(ETIQUETA_LOG, "Dispositivo encontrado: " + nombre);
                    mostrarInformacionDispositivoBTLE(resultado);
                } else {
                    Log.d(ETIQUETA_LOG, "Descartado: " + nombre);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onScanFailed() - Código: " + errorCode);
            }
        };

        // --- Crear filtro de escaneo ---
        List<ScanFilter> filtros = new ArrayList<>();
        ScanFilter sf = new ScanFilter.Builder().setDeviceName(dispositivoBuscado).build();
        filtros.add(sf);

        // --- Configurar ajustes del escaneo ---
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_SCAN no concedido.");
            return;
        }

        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): escaneando nombre: " + dispositivoBuscado);
        this.elEscanner.startScan(filtros, settings, this.callbackDelEscaneo);
    }

    // --------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {
        if (this.callbackDelEscaneo == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_SCAN no concedido.");
            return;
        }

        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;
    }

    // --------------------------------------------------------------
    // Botones de la interfaz
    // --------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "Botón buscar dispositivos BTLE pulsado");
        this.buscarTodosLosDispositivosBTLE();
    }

    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "Botón buscar nuestro dispositivo BTLE pulsado");
        this.buscarEsteDispositivoBTLE("fistro");
    }

    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "Botón detener búsqueda dispositivos BTLE pulsado");
        this.detenerBusquedaDispositivosBTLE();
    }

    // --------------------------------------------------------------
    // Inicialización Bluetooth
    // --------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): obteniendo adaptador BT");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            Log.e(ETIQUETA_LOG, "inicializarBlueTooth(): este dispositivo no soporta Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Permiso BLUETOOTH_CONNECT no concedido.");
            return;
        }

        bta.enable();
        this.elEscanner = bta.getBluetoothLeScanner();

        if (this.elEscanner == null) {
            Log.e(ETIQUETA_LOG, "inicializarBlueTooth(): no se pudo obtener escáner BLE");
        }

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): pidiendo permisos si faltan");

        // Petición de permisos runtime (Android 12+)
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS
            );
        } else {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): ya se tienen los permisos necesarios");
        }
    }

    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(ETIQUETA_LOG, "onCreate(): empieza");
        inicializarBlueTooth();
        Log.d(ETIQUETA_LOG, "onCreate(): termina");
    }

    // --------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PETICION_PERMISOS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(ETIQUETA_LOG, "Permisos concedidos correctamente");
            } else {
                Log.e(ETIQUETA_LOG, "Permisos NO concedidos, BLE deshabilitado");
            }
        }
    }
}
