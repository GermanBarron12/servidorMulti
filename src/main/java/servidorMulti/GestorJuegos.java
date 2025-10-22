package servidorMulti;

import java.util.HashMap;
import java.util.Map;


public class GestorJuegos {
    
    private static Map<String, String> invitacionesPendientes = new HashMap<>();
    
    
    private static Map<String, JuegoGato> partidasActivas = new HashMap<>();
    
    
    public static int enviarInvitacion(String invitador, String invitado) {
        if (invitador.equals(invitado)) {
            return -3; 
        }
        
        if (invitacionesPendientes.containsKey(invitado)) {
            return -1;
        }
        
        if (existePartida(invitador, invitado)) {
            return -2; 
        }
        
        invitacionesPendientes.put(invitado, invitador);
        System.out.println("[GATO] " + invitador + " invito a " + invitado);
        return 1;
    }
    
    
    public static JuegoGato aceptarInvitacion(String invitado) {
        if (!invitacionesPendientes.containsKey(invitado)) {
            return null; 
        }
        
        String invitador = invitacionesPendientes.remove(invitado);
        
        
        JuegoGato juego = new JuegoGato(invitador, invitado);
        String clavePartida = generarClavePartida(invitador, invitado);
        partidasActivas.put(clavePartida, juego);
        
        System.out.println("[GATO] Nueva partida: " + invitador + " vs " + invitado);
        return juego;
    }
    
    
    public static boolean rechazarInvitacion(String invitado) {
        if (!invitacionesPendientes.containsKey(invitado)) {
            return false;
        }
        
        String invitador = invitacionesPendientes.remove(invitado);
        System.out.println("[GATO] " + invitado + " rechazó invitación de " + invitador);
        return true;
    }
    
    
    public static JuegoGato obtenerPartida(String jugador) {
        for (JuegoGato juego : partidasActivas.values()) {
            if (juego.contieneJugador(jugador)) {
                return juego;
            }
        }
        return null;
    }
    
    
    private static boolean existePartida(String jugador1, String jugador2) {
        String clave1 = generarClavePartida(jugador1, jugador2);
        String clave2 = generarClavePartida(jugador2, jugador1);
        return partidasActivas.containsKey(clave1) || partidasActivas.containsKey(clave2);
    }
    
    
    private static String generarClavePartida(String j1, String j2) {
        // Ordenar alfabéticamente para consistencia
        if (j1.compareTo(j2) < 0) {
            return j1 + "-" + j2;
        } else {
            return j2 + "-" + j1;
        }
    }
    
    
    public static void terminarPartida(String jugador) {
        JuegoGato juego = obtenerPartida(jugador);
        if (juego != null) {
            String clave = generarClavePartida(juego.getJugador1(), juego.getJugador2());
            partidasActivas.remove(clave);
            System.out.println("[GATO] Partida terminada: " + juego.getJugador1() + " vs " + juego.getJugador2());
        }
    }
    
    
    public static boolean tieneInvitacionPendiente(String usuario) {
        return invitacionesPendientes.containsKey(usuario);
    }
    
    
    public static String getInvitador(String invitado) {
        return invitacionesPendientes.get(invitado);
    }
    
    
    public static void cancelarInvitacion(String usuario) {
        invitacionesPendientes.remove(usuario);
        invitacionesPendientes.values().remove(usuario);
    }
}