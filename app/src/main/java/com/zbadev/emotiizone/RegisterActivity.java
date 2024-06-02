package com.zbadev.emotiizone;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText signupEmail, signupPassword, SignupConfirmPassword;
    private Button signupButton;
    private TextView LoginRedirectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Redirect();
        Register2();
    }

    void Redirect(){
        //rediccionando
        LoginRedirectText = findViewById(R.id.RedirectLogin);
        LoginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    void Register() {
        try {
            auth = FirebaseAuth.getInstance();
            signupEmail = findViewById(R.id.txt_reg_email);
            signupPassword = findViewById(R.id.txt_reg_pass);
            signupButton = findViewById(R.id.btn_registro);
            LoginRedirectText = findViewById(R.id.RedirectLogin);
            SignupConfirmPassword = findViewById(R.id.txt_reg_confirm_pass);

            signupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String pass = signupPassword.getText().toString().trim();
                    String user = signupEmail.getText().toString().trim();
                    String confirmPass = SignupConfirmPassword.getText().toString().trim();
                    if (user.isEmpty()) {
                        signupEmail.setError("El correo electrónico no puede estar vacío");
                        return;
                    }
                    if (pass.isEmpty()) {
                        signupPassword.setError("La contraseña no puede estar vacía");
                        return;
                    } else {
                        auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Registro Exitoso", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                } else {
                                    Toast.makeText(RegisterActivity.this, "El registro falló: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });

            LoginRedirectText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Algo salio mal"+ e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "registro: ALgo salio mal:"+ e.getMessage());
            e.printStackTrace();

        }
    }
    void Register2() {
        try {
            auth = FirebaseAuth.getInstance();
            signupEmail = findViewById(R.id.txt_reg_email);
            signupPassword = findViewById(R.id.txt_reg_pass);
            signupButton = findViewById(R.id.btn_registro);
            LoginRedirectText = findViewById(R.id.RedirectLogin);
            SignupConfirmPassword = findViewById(R.id.txt_reg_confirm_pass);

            signupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String pass = signupPassword.getText().toString().trim();
                    String user = signupEmail.getText().toString().trim();
                    String confirmPass = SignupConfirmPassword.getText().toString().trim();

                    if (user.isEmpty()) {
                        signupEmail.setError("El correo electrónico no puede estar vacío");
                        return;
                    }
                    if (pass.isEmpty()) {
                        signupPassword.setError("La contraseña no puede estar vacía");
                        return;
                    }
                    if (confirmPass.isEmpty()) {
                        SignupConfirmPassword.setError("Confirma tu contraseña");
                        return;
                    }
                    if (!pass.equals(confirmPass)) { // Compara las contraseñas
                        SignupConfirmPassword.setError("Las contraseñas no coinciden");
                        return;
                    }

                    auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this, "Registro Exitoso", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            } else {
                                Toast.makeText(RegisterActivity.this, "El registro falló: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            });

            LoginRedirectText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Algo salio mal"+ e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "registro: ALgo salio mal:"+ e.getMessage());
            Log.w(TAG, "signInWithCredential:failure" +e.getMessage());
            e.printStackTrace();

        }
    }
}