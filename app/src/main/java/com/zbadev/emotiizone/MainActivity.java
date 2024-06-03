package com.zbadev.emotiizone;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    // Declaración de variables
    private Button logoutButton; // Botón para cerrar sesión
    private FirebaseAuth auth; // Instancia de FirebaseAuth para gestionar la autenticación
    private GoogleSignInClient mGoogleSignInClient; // Cliente de inicio de sesión con Google
    private TextView userEmail; // TextView para mostrar el correo del usuario
    private ImageView userProfilePic; // ImageView para mostrar la foto de perfil del usuario
    private Executor executor; // Executor para manejar tareas de autenticación biométrica en el hilo principal
    private BiometricPrompt biometricPrompt; // Prompt para la autenticación biométrica
    private BiometricPrompt.PromptInfo promptInfo; // Información del prompt de autenticación biométrica
    private static final int REQUEST_CODE = 101010; // Código de solicitud para el enroll biométrico
    private static final String TAG = "MainActivity"; // Tag para logs de depuración
    private SharedPreferences sharedPreferences; // Preferencias compartidas para almacenar datos persistentes
    private Handler inactivityHandler; // Manejador para gestionar la inactividad del usuario
    private Runnable inactivityRunnable; // Runnable que se ejecuta tras un período de inactividad
    private static final long INACTIVITY_TIMEOUT = 5 * 60 * 1000; // Tiempo de inactividad en milisegundos (5 minutos)
    private boolean isAppInBackground = false; // Bandera para verificar si la app está en segundo plano

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de variables y componentes de la interfaz de loco
        logoutButton = findViewById(R.id.btn_logout);
        userEmail = findViewById(R.id.user_email);
        userProfilePic = findViewById(R.id.user_profile_pic);
        auth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("EmotiizonePrefs", MODE_PRIVATE);

        // Inicialización del manejador de inactividad
        inactivityHandler = new Handler();
        inactivityRunnable = new Runnable() {
            @Override
            public void run() {
                if (auth.getCurrentUser() != null) {
                    // Si el usuario está autenticado, solicitar autenticación biométrica tras inactividad
                    authenticateUser();
                }
            }
        };

        // Verificar si hay un usuario autenticado
        if (auth.getCurrentUser() == null) {
            // Si no hay usuario autenticado, redirigir a la pantalla de inicio de sesión
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        } else {
            // Si hay usuario autenticado, actualizar la interfaz de usuario
            updateUI(auth.getCurrentUser());
            // Reiniciar el temporizador de inactividad
            resetInactivityTimer();
        }

        // Configurar inicio de sesión con Google
        configureGoogleSignIn();

        // Acción del botón de cerrar sesión
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Llamar al método para cerrar sesión
                signOut();
            }
        });
    }

    // Configuración de inicio de sesión con Google
    private void configureGoogleSignIn() {
        // Configuración de opciones de inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Inicialización del cliente de inicio de sesión con Google
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // Método para cerrar sesión
    private void signOut() {
        // Cerrar sesión en Firebase
        auth.signOut();

        // Cerrar sesión en Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Mostrar mensaje de confirmación
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            // Actualizar preferencia de sesión iniciada
            sharedPreferences.edit().putBoolean("loggedIn", false).apply();
            // Redirigir a la pantalla de inicio de sesión
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    // Actualizar la interfaz de usuario con la información del usuario
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Obtener el correo electrónico del usuario
            String email = user.getEmail();
            userEmail.setText(email);

            // Obtener la foto de perfil del usuario
            Uri photoUrl = user.getPhotoUrl();
            if (photoUrl != null) {
                // Cargar la foto de perfil usando Glide
                Glide.with(this).load(photoUrl).into(userProfilePic);
            } else {
                // Establecer una imagen por defecto si no hay foto de perfil
                userProfilePic.setImageResource(R.drawable.baseline_person_24);
            }
        }
    }

    // Método para autenticar al usuario usando biometría
    private void authenticateUser() {
        // Obtener instancia del administrador biométrico
        BiometricManager biometricManager = BiometricManager.from(this);
        // Verificar si se puede autenticar usando biometría
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // La autenticación biométrica está disponible
                Log.d(TAG, "La aplicación puede autenticarse mediante datos biométricos.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // No hay hardware biométrico disponible
                Toast.makeText(getApplicationContext(), "No hay hardware biométrico disponible", Toast.LENGTH_SHORT).show();
                // Redirigir a la pantalla de inicio de sesión
                LoginRedirect();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // El hardware biométrico no está disponible actualmente
                Toast.makeText(getApplicationContext(), "Hardware biométrico no disponible actualmente", Toast.LENGTH_SHORT).show();
                // Redirigir a la pantalla de inicio de sesión
                LoginRedirect();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // No hay datos biométricos registrados
                final Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
                startActivityForResult(enrollIntent, REQUEST_CODE);
                Toast.makeText(getApplicationContext(), "No hay huellas digitales registradas", Toast.LENGTH_SHORT).show();
                // Redirigir a la pantalla de inicio de sesión
                LoginRedirect();
                break;
        }

        // Inicializar el executor para manejar las tareas de autenticación biométrica
        executor = ContextCompat.getMainExecutor(this);
        // Crear una instancia de BiometricPrompt
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Mostrar mensaje de error de autenticación
                Toast.makeText(getApplicationContext(), "Error de autenticación: " + errString, Toast.LENGTH_SHORT).show();
                // Redirigir a la pantalla de inicio de sesión
                LoginRedirect();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Mostrar mensaje de autenticación exitosa
                Toast.makeText(getApplicationContext(), "Autenticación correcta!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // Mostrar mensaje de autenticación fallida
                Toast.makeText(getApplicationContext(), "Autenticación fallida", Toast.LENGTH_SHORT).show();
                // Redirigir a la pantalla de inicio de sesión
                LoginRedirect();
            }
        });

        // Configurar la información del prompt de autenticación biométrica
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Inicio de sesión biométrico para EmotiiZone")
                .setSubtitle("Inicie sesión con su credencial biométrica")
                .setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)
                .build();

        // Mostrar el prompt de autenticación biométrica
        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si la aplicación estaba en segundo plano y el usuario está autenticado
        if (isAppInBackground) {
            if (auth.getCurrentUser() != null && sharedPreferences.getBoolean("loggedIn", false)) {
                // Solicitar autenticación biométrica
                authenticateUser();
            }
            // Marcar que la aplicación ya no está en segundo plano
            isAppInBackground = false;
        }
        // Reiniciar el temporizador de inactividad
        resetInactivityTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Si el usuario está autenticado, marcar que la sesión está iniciada
        if (auth.getCurrentUser() != null) {
            sharedPreferences.edit().putBoolean("loggedIn", true).apply();
        }
        // Marcar que la aplicación está en segundo plano
        isAppInBackground = true;
        // Detener el temporizador de inactividad
        stopInactivityTimer();
    }

    // Método para redirigir a la pantalla de inicio de sesión y cerrar la sesión actual
    public void LoginRedirect() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        signOut();
        finish();
    }

    // Método para reiniciar el temporizador de inactividad
    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT);
    }

    // Método para detener el temporizador de inactividad
    private void stopInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
    }
}
