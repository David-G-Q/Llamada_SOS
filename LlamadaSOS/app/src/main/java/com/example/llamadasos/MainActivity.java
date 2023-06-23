package com.example.llamadasos;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Elementos de la interfaz de usuario
    private EditText editTelefono;
    private Button btnAgregar;

    // Variables de SharedPreferences e Intent del servicio
    private SharedPreferences sharedPreferences;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar elementos de la interfaz de usuario
        editTelefono = findViewById(R.id.editTelefono);
        btnAgregar = findViewById(R.id.btnAgregar);

        // Inicializar SharedPreferences e Intent del servicio
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serviceIntent = new Intent(this, SegundoPlano.class);

        // Obtener el número de teléfono guardado en SharedPreferences y mostrarlo en el campo de texto
        String telefono = sharedPreferences.getString("telefono", "");
        editTelefono.setText(telefono);

        // Configurar el listener del botón "Agregar"
        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarNumeroTelefono();
                reiniciarServicio();
                reiniciarActividad();
            }
        });

        // Iniciar el servicio en segundo plano si hay un número de teléfono guardado
        if (!telefono.equals("")) {
            startService(serviceIntent);
        }
    }

    // Guardar el número de teléfono en SharedPreferences
    private void guardarNumeroTelefono() {
        String numeroTelefono = editTelefono.getText().toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("telefono", numeroTelefono);
        editor.apply();
        Toast.makeText(MainActivity.this, "Guardado", Toast.LENGTH_SHORT).show();
    }

    // Reiniciar el servicio en segundo plano
    private void reiniciarServicio() {
        if (isServiceRunning(SegundoPlano.class)) {
            stopService(serviceIntent);
            Toast.makeText(MainActivity.this, "Actualizando", Toast.LENGTH_SHORT).show();
        }
        startService(serviceIntent);
    }

    private void reiniciarActividad() {
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
    }

    // Verificar si el servicio está en ejecución
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
