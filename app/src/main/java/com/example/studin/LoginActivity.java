package com.example.studin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText usuario, contrasena;
    Button botonLogin;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pantalla_login);

        usuario = findViewById(R.id.usuario);
        contrasena = findViewById(R.id.contrasena);
        botonLogin = findViewById(R.id.InicioBoton);

            botonLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String user = usuario.getText().toString();
                    String pass = contrasena.getText().toString();
                    if (user.equals("admin") && pass.equals("admin")) {
                        Toast.makeText(LoginActivity.this, "Inicio correcto", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Inicio incorrecto", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
}

