package servidorMulti.command;

import java.util.Arrays;
import java.util.List;

public class CommandValidator {
    
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;

    public static final List<String> COMANDOS_RESERVADOS = Arrays.asList(
        "registro", "login", "bloquear", "desbloquear", "bloqueados",
        "usuarios", "online", "gato", "aceptar", "rechazar", "jugar",
        "tablero", "rendirse", "ranking", "stats", "creargrupo",
        "unirgrupo", "entrargrupo", "eliminargrupo", "grupos",
        "misgrupos", "miembros", "ayuda", "salir"
    );
    
    public boolean isValidUsername(String username) {
        if (username == null || username.length() < MIN_USERNAME_LENGTH){
            return false;
        }
        
        // Rechazar caracteres especiales de control y espacios
        if (contieneCaracteresInvalidos(username)) {
            return false;
        }
        
        String lowerUsername = username.toLowerCase();
        return !COMANDOS_RESERVADOS.contains(lowerUsername) &&
               !lowerUsername.startsWith("/");
    }
    
    /**
     * Verifica si el username contiene caracteres invalidos
     */
    private boolean contieneCaracteresInvalidos(String username) {
        // Rechazar espacios y caracteres de control (tab, newline, etc)
        for (char c : username.toCharArray()) {
            if (Character.isWhitespace(c) || Character.isISOControl(c)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        
        // Rechazar contraseñas con caracteres de control
        for (char c : password.toCharArray()) {
            if (Character.isISOControl(c)) {
                return true;
            }
        }
        
        return true;
    }

    public boolean isValidCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate <= 2;
    }

    public boolean hasExpectedParts(String[] parts, int expected) {
        return parts != null && parts.length == expected;
    }

    public String extractGroupName(String message) {
        String[] parts = message.split(" ", 2);
        return parts.length == 2 ? parts[1].trim() : null;
    }
    
    public boolean isReservedCommand(String username){
        if(username == null){
            return false;
        }
        String lowerUsername = username.toLowerCase();
        
        if (lowerUsername.startsWith("/")){
            return true;
        }
        return COMANDOS_RESERVADOS.contains(lowerUsername);
    }
    
    public String getUserNameErrorMessage(String username){
        if (username == null || username.isEmpty()){
            return "El nombre de usuario no puede estar vacio";
        }
        if (username.length() < MIN_USERNAME_LENGTH){
            return "Usuario minimo 3 caracteres";
        }
        if (username.startsWith("/") || username.startsWith("\\")){
            return "El nombre de usuario no puede empezar con / o \\";
        }
        if (contieneCaracteresInvalidos(username)){
            return "El nombre de usuario no puede contener espacios ni caracteres especiales";
        }
        if (isReservedCommand(username)){
            return "No puedes usar '" + username + "' como nombre de usuario (comando reservado)";
        }
        return null;
    }
}