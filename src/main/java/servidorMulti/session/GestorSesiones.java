package servidorMulti.session;

import servidorMulti.unCliente;
import java.util.HashMap;
import java.util.Map;

public class GestorSesiones {
    
    private static final Map<String, unCliente> sesionesActivas = new HashMap<>();
    
    /**
     * Registra una nueva sesion de usuario
     * @return true si se registro exitosamente, false si ya existia
     */
    public static boolean registrarSesion(String username, unCliente cliente) {
        if (sesionesActivas.containsKey(username)) {
            return false;
        }
        
        sesionesActivas.put(username, cliente);
        System.out.println("[SESION] Nueva sesion registrada: " + username);
        return true;
    }
    
    /**
     * Cierra una sesion activa
     */
    public static void cerrarSesion(String username) {
        unCliente cliente = sesionesActivas.remove(username);
        if (cliente != null) {
            System.out.println("[SESION] Sesion cerrada: " + username);
        }
    }
    
    /**
     * Verifica si un usuario tiene sesion activa
     */
    public static boolean tieneSesionActiva(String username) {
        return sesionesActivas.containsKey(username);
    }
    
    /**
     * Obtiene el cliente de una sesion activa
     */
    public static unCliente obtenerCliente(String username) {
        return sesionesActivas.get(username);
    }
    
    /**
     * Obtiene todas las sesiones activas
     */
    public static Map<String, unCliente> obtenerSesiones() {
        return new HashMap<>(sesionesActivas);
    }
    
    /**
     * Cuenta las sesiones activas
     */
    public static int contarSesiones() {
        return sesionesActivas.size();
    }
}