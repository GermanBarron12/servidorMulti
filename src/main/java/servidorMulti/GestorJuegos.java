package servidorMulti;

import java.util.HashMap;
import java.util.Map;

public class GestorJuegos {
    
    private static Map<String, String> invitacionesPendientes = new HashMap<>();
    private static Map<String, JuegoGato> partidasActivas = new HashMap<>();
    
    /**
     * Envía una invitación de juego
     * @return 1 si éxito, -1 si invitado ya tiene invitación, -2 si ya existe partida,
     *         -3 si invita a sí mismo, -4 si invitador está en partida, -5 si invitado está en partida
     */
    public static int enviarInvitacion(String invitador, String invitado) {
        if (invitador.equals(invitado)) {
            return -3; // No puedes jugar contigo mismo
        }
        
        // VALIDACIÓN NUEVA: Verificar si el invitador ya está en una partida
        if (obtenerPartida(invitador) != null) {
            return -4; // El invitador ya está jugando
        }
        
        // VALIDACIÓN NUEVA: Verificar si el invitado ya está en una partida
        if (obtenerPartida(invitado) != null) {
            return -5; // El invitado ya está jugando
        }
        
        if (invitacionesPendientes.containsKey(invitado)) {
            return -1; // Ya tiene invitación pendiente
        }
        
        if (existePartida(invitador, invitado)) {
            return -2; // Ya existe partida entre ellos
        }
        
        invitacionesPendientes.put(invitado, invitador);
        System.out.println("[GATO] " + invitador + " invito a " + invitado);
        return 1;
    }
    
    /**
     * Acepta una invitación pendiente
     */
    public static JuegoGato aceptarInvitacion(String invitado) {
        if (!invitacionesPendientes.containsKey(invitado)) {
            return null; 
        }
        
        String invitador = invitacionesPendientes.remove(invitado);
        
        // Crear nueva partida
        JuegoGato juego = new JuegoGato(invitador, invitado);
        String clavePartida = generarClavePartida(invitador, invitado);
        partidasActivas.put(clavePartida, juego);
        
        System.out.println("[GATO] Nueva partida: " + invitador + " vs " + invitado);
        return juego;
    }
    
    /**
     * Rechaza una invitación pendiente
     */
    public static boolean rechazarInvitacion(String invitado) {
        if (!invitacionesPendientes.containsKey(invitado)) {
            return false;
        }
        
        String invitador = invitacionesPendientes.remove(invitado);
        System.out.println("[GATO] " + invitado + " rechazo invitación de " + invitador);
        return true;
    }
    
    /**
     * Obtiene la partida activa de un jugador
     */
    public static JuegoGato obtenerPartida(String jugador) {
        for (JuegoGato juego : partidasActivas.values()) {
            if (juego.contieneJugador(jugador)) {
                return juego;
            }
        }
        return null;
    }
    
    /**
     * Verifica si ya existe una partida entre dos jugadores
     */
    private static boolean existePartida(String jugador1, String jugador2) {
        String clave1 = generarClavePartida(jugador1, jugador2);
        String clave2 = generarClavePartida(jugador2, jugador1);
        return partidasActivas.containsKey(clave1) || partidasActivas.containsKey(clave2);
    }
    
    /**
     * Genera una clave única para identificar una partida
     */
    private static String generarClavePartida(String j1, String j2) {
        // Ordenar alfabéticamente para consistencia
        if (j1.compareTo(j2) < 0) {
            return j1 + "-" + j2;
        } else {
            return j2 + "-" + j1;
        }
    }
    
    /**
     * Termina una partida activa
     */
    public static void terminarPartida(String jugador) {
        JuegoGato juego = obtenerPartida(jugador);
        if (juego != null) {
            String clave = generarClavePartida(juego.getJugador1(), juego.getJugador2());
            partidasActivas.remove(clave);
            System.out.println("[GATO] Partida terminada: " + juego.getJugador1() + " vs " + juego.getJugador2());
        }
    }
    
    /**
     * Verifica si un usuario tiene invitación pendiente
     */
    public static boolean tieneInvitacionPendiente(String usuario) {
        return invitacionesPendientes.containsKey(usuario);
    }
    
    /**
     * Obtiene el nombre del invitador
     */
    public static String getInvitador(String invitado) {
        return invitacionesPendientes.get(invitado);
    }
    
    /**
     * Cancela invitaciones relacionadas con un usuario
     */
    public static void cancelarInvitacion(String usuario) {
        invitacionesPendientes.remove(usuario);
        invitacionesPendientes.values().remove(usuario);
    }
    
    /**
     * Verifica si un usuario está en una partida activa
     */
    public static boolean estaEnPartida(String usuario) {
        return obtenerPartida(usuario) != null;
    }
}