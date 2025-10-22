package servidorMulti;

import java.util.Random;

/**
 * Clase que representa una partida del juego Gato (Tic-Tac-Toe)
 */
public class JuegoGato {
    private final String jugador1;
    private final String jugador2;
    private char[][] tablero;
    private String turnoActual;
    private boolean juegoTerminado;
    private String ganador;
    
    public JuegoGato(String jugador1, String jugador2) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.tablero = new char[3][3];
        this.juegoTerminado = false;
        this.ganador = null;
        
        // Inicializar tablero vacío
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = '-';
            }
        }
        
        // Determinar quién empieza aleatoriamente
        Random random = new Random();
        this.turnoActual = random.nextBoolean() ? jugador1 : jugador2;
    }
    
    public String getJugador1() {
        return jugador1;
    }
    
    public String getJugador2() {
        return jugador2;
    }
    
    public String getTurnoActual() {
        return turnoActual;
    }
    
    public boolean isJuegoTerminado() {
        return juegoTerminado;
    }
    
    public String getGanador() {
        return ganador;
    }
    
    /**
     * Verifica si un jugador está en esta partida
     */
    public boolean contieneJugador(String usuario) {
        return jugador1.equals(usuario) || jugador2.equals(usuario);
    }
    
    /**
     * Obtiene el oponente de un jugador
     */
    public String getOponente(String jugador) {
        return jugador.equals(jugador1) ? jugador2 : jugador1;
    }
    
    /**
     * Obtiene el símbolo del jugador (X o O)
     */
    public char getSimboloJugador(String jugador) {
        return jugador.equals(jugador1) ? 'X' : 'O';
    }
    
    /**
     * Realiza un movimiento en el tablero
     * @return true si el movimiento es válido, false si no
     */
    public boolean realizarMovimiento(String jugador, int fila, int columna) {
        // Validar que sea el turno del jugador
        if (!jugador.equals(turnoActual)) {
            return false;
        }
        
        // Validar coordenadas
        if (fila < 0 || fila > 2 || columna < 0 || columna > 2) {
            return false;
        }
        
        // Validar que la casilla esté vacía
        if (tablero[fila][columna] != '-') {
            return false;
        }
        
        // Realizar movimiento
        tablero[fila][columna] = getSimboloJugador(jugador);
        
        // Verificar si hay ganador o empate
        verificarEstadoJuego();
        
        // Cambiar turno si el juego no terminó
        if (!juegoTerminado) {
            turnoActual = getOponente(jugador);
        }
        
        return true;
    }
    
    /**
     * Verifica si hay ganador o empate
     */
    private void verificarEstadoJuego() {
        // Verificar filas
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] != '-' && 
                tablero[i][0] == tablero[i][1] && 
                tablero[i][1] == tablero[i][2]) {
                juegoTerminado = true;
                ganador = tablero[i][0] == 'X' ? jugador1 : jugador2;
                return;
            }
        }
        
        // Verificar columnas
        for (int j = 0; j < 3; j++) {
            if (tablero[0][j] != '-' && 
                tablero[0][j] == tablero[1][j] && 
                tablero[1][j] == tablero[2][j]) {
                juegoTerminado = true;
                ganador = tablero[0][j] == 'X' ? jugador1 : jugador2;
                return;
            }
        }
        
        // Verificar diagonal principal
        if (tablero[0][0] != '-' && 
            tablero[0][0] == tablero[1][1] && 
            tablero[1][1] == tablero[2][2]) {
            juegoTerminado = true;
            ganador = tablero[0][0] == 'X' ? jugador1 : jugador2;
            return;
        }
        
        // Verificar diagonal secundaria
        if (tablero[0][2] != '-' && 
            tablero[0][2] == tablero[1][1] && 
            tablero[1][1] == tablero[2][0]) {
            juegoTerminado = true;
            ganador = tablero[0][2] == 'X' ? jugador1 : jugador2;
            return;
        }
        
        // Verificar empate (tablero lleno)
        boolean tableroLleno = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (tablero[i][j] == '-') {
                    tableroLleno = false;
                    break;
                }
            }
        }
        
        if (tableroLleno) {
            juegoTerminado = true;
            ganador = "EMPATE";
        }
    }
    
    /**
     * Obtiene representación visual del tablero
     */
    public String getTableroVisual() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔═══════════════════╗\n");
        sb.append("║   JUEGO DEL GATO  ║\n");
        sb.append("╠═══════════════════╣\n");
        sb.append("║     0   1   2     ║\n");
        sb.append("║   ┌───┬───┬───┐   ║\n");
        
        for (int i = 0; i < 3; i++) {
            sb.append("║ ").append(i).append(" │");
            for (int j = 0; j < 3; j++) {
                char c = tablero[i][j];
                sb.append(" ").append(c == '-' ? ' ' : c).append(" ");
                if (j < 2) sb.append("│");
            }
            sb.append("│   ║\n");
            if (i < 2) {
                sb.append("║   ├───┼───┼───┤   ║\n");
            }
        }
        
        sb.append("║   └───┴───┴───┘   ║\n");
        sb.append("╚═══════════════════╝\n");
        
        // Mostrar información del juego
        sb.append("👥 ").append(jugador1).append(" (X) vs ").append(jugador2).append(" (O)\n");
        
        if (!juegoTerminado) {
            sb.append("🎯 Turno de: ").append(turnoActual).append(" (").append(getSimboloJugador(turnoActual)).append(")\n");
        } else {
            if (ganador.equals("EMPATE")) {
                sb.append("🤝 ¡EMPATE! Tablero lleno\n");
            } else {
                sb.append("🏆 ¡Ganador: ").append(ganador).append("!\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Termina el juego por abandono
     */
    public void terminarPorAbandono(String jugadorQueAbandona) {
        juegoTerminado = true;
        ganador = getOponente(jugadorQueAbandona);
    }
}
