package net.datatecsolution.admintools;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderUtil {

    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "hola123"; // La contraseña que deseas codificar
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("Encoded Password: " + encodedPassword);
    }
}