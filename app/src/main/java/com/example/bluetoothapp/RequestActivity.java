package com.example.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestActivity extends AppCompatActivity {

    private static final String HC06_ADDRESS = "00:00:00:00:00:00"; // Endereço MAC do HC-06
    private static final UUID HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private TextView responseTextView;
    private Button sendBluetoothButton;
    private String responseMessage, token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request_activity);

        responseTextView = findViewById(R.id.text_response);
        sendBluetoothButton = findViewById(R.id.button_send_bluetooth);

        String qrCode = getIntent().getStringExtra("qrCode");
        makeHttpRequest(qrCode);

        sendBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (responseMessage != null) {
                    sendViaBluetooth(responseMessage);
                } else {
                    Toast.makeText(RequestActivity.this, "Nenhuma resposta para enviar", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void makeHttpRequest(String url) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        responseMessage = response;
                        responseTextView.setText(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(RequestActivity.this, "Erro na requisição: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        queue.add(stringRequest);
    }

    private void sendViaBluetooth(String message) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não disponível", Toast.LENGTH_LONG).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothAdapter.enable();
        }

        BluetoothDevice hc06 = bluetoothAdapter.getRemoteDevice(HC06_ADDRESS);
        try {
            bluetoothSocket = hc06.createRfcommSocketToServiceRecord(HC06_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(message.getBytes());
            Toast.makeText(this, "Mensagem enviada via Bluetooth", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao enviar mensagem: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Erro ao fechar conexão Bluetooth: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
