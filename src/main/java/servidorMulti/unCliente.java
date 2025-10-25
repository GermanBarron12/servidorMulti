package servidorMulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class unCliente implements Runnable {

    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;
    final String idCliente;
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    private String nombreUsuario = null;
    private boolean enPartida = false;

    public boolean isAutenticado() {
        return autenticado;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    unCliente(Socket s, String id) throws IOException {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        this.idCliente = id;
    }

    @Override
    public void run() {
        try {

            salida.writeUTF("-------- BIENVENIDO AL CHAT --------");
            salida.writeUTF("Conectado al servidor. ID: #" + idCliente);
            salida.writeUTF("Comandos: /ayuda para ver todos los comandos disponibles");
            salida.flush();

            while (true) {
                String mensaje = entrada.readUTF();

                if (mensaje.startsWith("/registro ")) {
                    procesarRegistro(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/login ")) {
                    procesarLogin(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/bloquear ")) {
                    procesarBloquear(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/desbloquear ")) {
                    procesarDesbloquear(mensaje);
                    continue;
                }

                if (mensaje.equals("/bloqueados")) {
                    procesarListaBloqueados();
                    continue;
                }

                if (mensaje.equals("/usuarios")) {
                    procesarListaUsuarios();
                    continue;
                }

                if (mensaje.equals("/online")) {
                    procesarUsuariosOnline();
                    continue;
                }

                if (mensaje.startsWith("/gato")) {
                    procesarInvitacionGato(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/aceptar")) {
                    procesarAceptarGato();
                    continue;
                }

                if (mensaje.startsWith("/rechazar")) {
                    procesarRechazarGato();
                    continue;
                }

                if (mensaje.startsWith("/jugar")) {
                    procesarMovimientoGato(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/tablero")) {
                    mostrarTableroGato();
                    continue;
                }

                if (mensaje.startsWith("/rendirse")) {
                    procesarRendirse();
                    continue;
                }
                
                if (mensaje.equals("/ranking")){
                    procesarRanking();
                    continue;
                }
                
                if (mensaje.equals("/estadisticas")){
                    procesarEstadisticas(mensaje);
                    continue;
                }

                if (mensaje.equals("/ayuda")) {
                    mostrarAyuda();
                    continue;
                }

                if ("salir".equalsIgnoreCase(mensaje)) {
                    salida.writeUTF("¡Hasta pronto!");
                    salida.flush();
                    break;
                }

                if (!autenticado) {
                    if (mensajesEnviados >= 3) {
                        salida.writeUTF("Limite alcanzado. Usa: /registro o /login");
                        salida.flush();
                        continue;
                    }
                    mensajesEnviados++;
                }

                if (mensaje.startsWith("@")) {
                    enviarMensajePrivado(mensaje);
                } else {
                    enviarBroadcast(mensaje);
                }
            }
        } catch (IOException ex) {
            System.out.println("[DESCONEXION] Cliente #" + idCliente);
        } finally {
            if (autenticado && nombreUsuario != null) {
                JuegoGato juego = GestorJuegos.obtenerPartida(nombreUsuario);
                if (juego != null) {
                    String oponente = juego.getOponente(nombreUsuario);
                    juego.terminarPorAbandono(nombreUsuario);

                    unCliente clienteOponente = buscarClientePorNombre(oponente);
                    if (clienteOponente != null) {
                        clienteOponente.enPartida = false;
                        try {
                            clienteOponente.salida.writeUTF("- " + nombreUsuario + " se desconecto. Ganaste por abandono!, obtienes 2 puntos.");
                            clienteOponente.salida.writeUTF("Ahora puedes volver a chatear normalmente");
                            clienteOponente.salida.flush();
                        } catch (IOException ignored) {}
                    }
                    GestorJuegos.terminarPartida(nombreUsuario);
                }
                GestorJuegos.cancelarInvitacion(nombreUsuario);
            }

            ServidorMulti.clientes.remove(idCliente);
            try {
                entrada.close();
                salida.close();
            } catch (IOException ignored) {}
        }
    }

    private void procesarRegistro(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF("Usa: /registro <usuario> <password>");
            salida.flush();
            return;
        }

        String usuario = partes[1];
        String password = partes[2];

        if (usuario.length() < 3) {
            salida.writeUTF("Usuario minimo 3 caracteres");
            salida.flush();
            return;
        }

        if (password.length() < 4) {
            salida.writeUTF("Contraseña minimo 4 caracteres");
            salida.flush();
            return;
        }

        if (DatabaseManager.registrarUsuario(usuario, password)) {
            autenticado = true;
            nombreUsuario = usuario;
            mensajesEnviados = 0;

            salida.writeUTF("Registro exitoso. Bienvenido, " + usuario + "!");
            salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
            salida.flush();
        } else {
            salida.writeUTF("El usuario '" + usuario + "' ya existe.");
            salida.flush();
        }
    }

    private void procesarLogin(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF("Usa: /login <usuario> <password>");
            salida.flush();
            return;
        }

        String usuario = partes[1];
        String password = partes[2];

        if (!DatabaseManager.usuarioExiste(usuario)) {
            salida.writeUTF("El usuario '" + usuario + "' no existe.");
            salida.flush();
            return;
        }

        if (!DatabaseManager.verificarLogin(usuario, password)) {
            salida.writeUTF("Contraseña incorrecta.");
            salida.flush();
            return;
        }

        autenticado = true;
        nombreUsuario = usuario;
        mensajesEnviados = 0;

        salida.writeUTF("Inicio de sesion exitoso. Bienvenido, " + usuario + "!");
        salida.writeUTF("Ahora puedes enviar mensajes ilimitados.");
        salida.flush();
    }

    private void procesarBloquear(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado");
            salida.flush();
            return;
        }

        String[] partes = mensaje.split(" ");
        if (partes.length != 2) {
            salida.writeUTF("Usa: /bloquear <usuario>");
            salida.flush();
            return;
        }

        String usuarioABloquear = partes[1];
        int resultado = DatabaseManager.bloquearUsuario(nombreUsuario, usuarioABloquear);

        switch (resultado) {
            case 1:
                salida.writeUTF("Has bloqueado a '" + usuarioABloquear + "'");
                break;
            case -1:
                salida.writeUTF("No puedes bloquearte a ti mismo");
                break;
            case -2:
                salida.writeUTF("El usuario '" + usuarioABloquear + "' no existe");
                break;
            case -3:
                salida.writeUTF("Ya esta bloqueado");
                break;
            default:
                salida.writeUTF("Error al bloquear");
        }
        salida.flush();
    }

    private void procesarDesbloquear(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado");
            salida.flush();
            return;
        }

        String[] partes = mensaje.split(" ");
        if (partes.length != 2) {
            salida.writeUTF("Usa: /desbloquear <usuario>");
            salida.flush();
            return;
        }

        String usuarioADesbloquear = partes[1];
        int resultado = DatabaseManager.desbloquearUsuario(nombreUsuario, usuarioADesbloquear);

        switch (resultado) {
            case 1:
                salida.writeUTF("Has desbloqueado a '" + usuarioADesbloquear + "'");
                break;
            case -1:
                salida.writeUTF("No esta bloqueado");
                break;
            default:
                salida.writeUTF("Error al desbloquear");
        }
        salida.flush();
    }

    private void procesarListaBloqueados() throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado");
            salida.flush();
            return;
        }

        String lista = DatabaseManager.obtenerListaBloqueados(nombreUsuario);
        int cantidad = DatabaseManager.contarBloqueados(nombreUsuario);
        salida.writeUTF("Bloqueados (" + cantidad + "): " + lista);
        salida.flush();
    }

    private void mostrarAyuda() throws IOException {
        String ayuda = """
                     -------- COMANDOS DISPONIBLES --------
                      /registro <usuario> <password>
                      /login <usuario> <password>
                      mensaje - Enviar a todos
                      @usuario mensaje - Privado
                      /usuarios - Lista de usuarios registrados
                      /online - Usuarios conectados
                      /bloquear <usuario>
                      /desbloquear <usuario>
                      /bloqueados

                       Juego del gatito
                      /gato <usuario> - Invitar a jugar
                      /aceptar - Aceptar invitacion
                      /rechazar - Rechazar invitacion
                      /jugar <fila> <columna> - Hacer jugada (0-2)
                      /tablero - Ver tablero actual
                      /rendirse - Abandonar partida
                       
                       Estadisticas
                      /ranking - Ver ranking general
                      /estadisticas - <usuario1> <usuario2> - Comparar jugadores
                      /ayuda - Ver lista de comandos
                      salir - Cerrar conexion
                     --------------------------------------
                     """;

        salida.writeUTF(ayuda);
        salida.flush();
    }

    private void enviarMensajePrivado(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ", 2);
        String aQuien = partes[0].substring(1);
        String contenido = partes.length > 1 ? partes[1] : "";

        unCliente clienteDestino = null;
        for (unCliente c : ServidorMulti.clientes.values()) {
            if (c.autenticado && c.nombreUsuario != null && c.nombreUsuario.equals(aQuien)) {
                clienteDestino = c;
                break;
            }
        }

        if (clienteDestino == null) {
            salida.writeUTF(" Usuario '" + aQuien + "' no conectado");
            salida.flush();
            return;
        }
        
         if (this.enPartida && clienteDestino.enPartida) {
        JuegoGato miPartida = GestorJuegos.obtenerPartida(nombreUsuario);
        if (miPartida != null && miPartida.contieneJugador(aQuien)) {
            // Estan jugando juntos, permitir chat
            String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;
            clienteDestino.salida.writeUTF("[Privado de " + remitente + "]: " + contenido);
            clienteDestino.salida.flush();
            salida.writeUTF("[Mensaje privado enviado a " + aQuien + "]");
            salida.flush();
            return;
        }
    }
         
         if (this.enPartida) {
        salida.writeUTF("No puedes enviar mensajes mientras estas en una partida");
        salida.flush();
        return;
    }
         
          if (clienteDestino.enPartida) {
        salida.writeUTF("No puedes enviar mensajes a " + aQuien + " porque esta en una partida");
        salida.flush();
        return;
    }
          
          

        if (autenticado && DatabaseManager.estaBloquedo(clienteDestino.nombreUsuario, nombreUsuario)) {
            salida.writeUTF(" No puedes enviar mensajes a '" + aQuien + "'");
            salida.flush();
            return;
        }

        if (autenticado && DatabaseManager.estaBloquedo(nombreUsuario, clienteDestino.nombreUsuario)) {
            salida.writeUTF(" Has bloqueado a '" + aQuien + "'");
            salida.flush();
            return;
        }

        String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;
        clienteDestino.salida.writeUTF(" [Privado de " + remitente + "]: " + contenido);
        clienteDestino.salida.flush();
        salida.writeUTF("️ [Enviado a " + aQuien + "]");
        salida.flush();
    }

    private void enviarBroadcast(String mensaje) throws IOException {
        String remitente = autenticado ? nombreUsuario : "Cliente #" + idCliente;

        for (unCliente cliente : ServidorMulti.clientes.values()) {
            if (!cliente.idCliente.equals(this.idCliente)) {
                if (autenticado && cliente.autenticado
                        && DatabaseManager.estaBloquedo(cliente.nombreUsuario, nombreUsuario)) {
                    continue;
                }

                if (autenticado && cliente.autenticado
                        && DatabaseManager.estaBloquedo(nombreUsuario, cliente.nombreUsuario)) {
                    continue;
                }
                
                if (cliente.enPartida){
                    continue;
                }
                
                if (this.enPartida){
                    continue;
                }

                cliente.salida.writeUTF(" [" + remitente + "]: " + mensaje);
                cliente.salida.flush();
            }
        }
    }

    private void procesarListaUsuarios() throws IOException {
        if (!autenticado) {
            salida.writeUTF(" Debes estar autenticado para ver la lista de usuarios");
            salida.flush();
            return;
        }

        String lista = DatabaseManager.obtenerListaUsuarios();
        int total = DatabaseManager.contarUsuarios();

        salida.writeUTF("-------- USUARIOS REGISTRADOS --------");
        salida.writeUTF("Total: " + total + " usuarios");
        salida.writeUTF("*" + lista);
        salida.flush();
    }

    private void procesarUsuariosOnline() throws IOException {
        if (!autenticado) {
            salida.writeUTF(" Debes estar autenticado para ver usuarios online");
            salida.flush();
            return;
        }

        String lista = DatabaseManager.obtenerUsuariosConectados(ServidorMulti.clientes);
        int totalConectados = 0;

        for (unCliente cliente : ServidorMulti.clientes.values()) {
            if (cliente.isAutenticado() && cliente.getNombreUsuario() != null) {
                totalConectados++;
            }
        }

        salida.writeUTF("-------- USUARIOS ONLINE --------");
        salida.writeUTF("Conectados: " + totalConectados + " usuarios");
        salida.writeUTF("*" + lista);
        salida.flush();
    }

    private void procesarInvitacionGato(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado para jugar");
            salida.flush();
            return;
        }

        String[] partes = mensaje.split(" ");
        if (partes.length != 2) {
            salida.writeUTF("Usa: /gato <usuario>");
            salida.flush();
            return;
        }

        String invitado = partes[1];

        // Buscar si está conectado
        unCliente clienteInvitado = buscarClientePorNombre(invitado);

        if (clienteInvitado == null) {
            if (DatabaseManager.usuarioExiste(invitado)){
                salida.writeUTF("El usuario "+ invitado +" no esta conectado en este momento");
                } else {
                    salida.writeUTF("El usuario '" + invitado + "' no existe");
            }
            salida.flush();
            return;
        }
        
        
        if (DatabaseManager.estaBloquedo(nombreUsuario, invitado)){
            salida.writeUTF("No puedes invitar a " + invitado + " porque lo tienes bloqueado");
            salida.flush();
            return;
        }
        
        if (DatabaseManager.estaBloquedo(invitado, nombreUsuario)){
            salida.writeUTF("No puedes invitar a " + invitado + " en este momento");
            salida.flush();
            return;
        }

        // Intentar enviar la invitación
        int resultado = GestorJuegos.enviarInvitacion(nombreUsuario, invitado);

        switch (resultado) {
            case 1:
                salida.writeUTF("Invitacion enviada a " + invitado);
                salida.flush();

                String mensajeInvitacion = """
        ------------------------------------
              INVITACION AL JUEGO GATO        
        ------------------------------------
         %s te invita a jugar al Gato
           Usa: /aceptar  o  /rechazar
        """.formatted(nombreUsuario);

                clienteInvitado.salida.writeUTF(mensajeInvitacion);
                clienteInvitado.salida.flush();
                break;
            case -1:
                salida.writeUTF("- " + invitado + " ya tiene una invitacion pendiente");
                break;
            case -2:
                salida.writeUTF("️ Ya tienes una partida activa con " + invitado);
                break;
            case -3:
                salida.writeUTF(" No puedes jugar contigo mismo");
                break;
            case -4:
                salida.writeUTF(" Ya estas en una partida. Terminala primero antes de invitar a alguien mas");
                break;
            case -5:
                salida.writeUTF(" " + invitado + " ya esta en una partida activa");
                break;
            default:
                salida.writeUTF(" Error al enviar invitacion");
        }
        salida.flush();
    }

    private void procesarAceptarGato() throws IOException {
        if (!autenticado) {
            salida.writeUTF(" Debes estar autenticado");
            salida.flush();
            return;
        }

        if (!GestorJuegos.tieneInvitacionPendiente(nombreUsuario)) {
            salida.writeUTF(" No tienes invitaciones pendientes");
            salida.flush();
            return;
        }

        JuegoGato juego = GestorJuegos.aceptarInvitacion(nombreUsuario);

        if (juego == null) {
            salida.writeUTF(" Error al aceptar invitacion");
            salida.flush();
            return;
        }
        
        this.enPartida = true;

        // Notificar a ambos jugadores
        String oponente = juego.getOponente(nombreUsuario);
        unCliente clienteOponente = buscarClientePorNombre(oponente);
        
        if (clienteOponente != null){
            clienteOponente.enPartida = true;
        }

        String tablero = juego.getTableroVisual();

        String mensajeInicio = """
    ¡Partida iniciada!
    %s
    Usa: /jugar <fila> <columna> (0-2)
    Usa: /tablero para ver el tablero
    Usa: /rendirse para abandonar
    NOTA: No recibiras mensajes del chat mientras juegas                                                      
    """.formatted(tablero);

        salida.writeUTF(mensajeInicio);
        salida.flush();

        if (clienteOponente != null) {
            String mensajeOponente = """
        - %s acepto! ¡Partida iniciada!
        %s
        Usa: /jugar <fila> <columna> (0-2)
        Usa: /tablero para ver el tablero
        Usa: /rendirse para abandonar
        NOTA: No recibiras mensajes del chat mientras juegas 
        """.formatted(nombreUsuario, tablero);

            clienteOponente.salida.writeUTF(mensajeOponente);
            clienteOponente.salida.flush();
        }
    }

    private void procesarRechazarGato() throws IOException {
        if (!autenticado) {
            salida.writeUTF(" Debes estar autenticado");
            salida.flush();
            return;
        }

        if (!GestorJuegos.tieneInvitacionPendiente(nombreUsuario)) {
            salida.writeUTF(" No tienes invitaciones pendientes");
            salida.flush();
            return;
        }

        String invitador = GestorJuegos.getInvitador(nombreUsuario);
        boolean rechazado = GestorJuegos.rechazarInvitacion(nombreUsuario);

        if (rechazado) {
            salida.writeUTF(" Invitacin rechazada");
            salida.flush();

            // Notificar al invitador
            unCliente clienteInvitador = buscarClientePorNombre(invitador);
            if (clienteInvitador != null) {
                clienteInvitador.salida.writeUTF(" " + nombreUsuario + " rechazo tu invitacion al Gato");
                clienteInvitador.salida.flush();
            }
        }
    }

    private void procesarMovimientoGato(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF(" Debes estar autenticado");
            salida.flush();
            return;
        }

        JuegoGato juego = GestorJuegos.obtenerPartida(nombreUsuario);

        if (juego == null) {
            salida.writeUTF(" No tienes una partida activa");
            salida.flush();
            return;
        }

        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF(" Usa: /jugar <fila> <columna> (0-2)");
            salida.flush();
            return;
        }

        try {
            int fila = Integer.parseInt(partes[1]);
            int columna = Integer.parseInt(partes[2]);

            boolean movimientoValido = juego.realizarMovimiento(nombreUsuario, fila, columna);

            if (!movimientoValido) {
                if (!juego.getTurnoActual().equals(nombreUsuario)) {
                    salida.writeUTF("️ No es tu turno");
                } else {
                    salida.writeUTF(" Movimiento invalido. La casilla debe estar vacia y dentro del tablero");
                }
                salida.flush();
                return;
            }

            // Mostrar tablero actualizado a ambos jugadores
            String tablero = juego.getTableroVisual();
            String oponente = juego.getOponente(nombreUsuario);
            unCliente clienteOponente = buscarClientePorNombre(oponente);

            salida.writeUTF(tablero);
            salida.flush();

            if (clienteOponente != null) {
                clienteOponente.salida.writeUTF(tablero);
                clienteOponente.salida.flush();
            }

            // Si el juego terminó
            if (juego.isJuegoTerminado()) {
                String ganador = juego.getGanador();
                
                this.enPartida = false;
                if(clienteOponente != null){
                    clienteOponente.enPartida = false;
                }
                
                DatabaseManager.registrarResultado(juego.getGanador(), juego.getJugador2(), ganador);

                if (ganador.equals("EMPATE")) {
                    salida.writeUTF("¡EMPATE! Bien jugado, obtienes 1 punto.");
                    salida.writeUTF("Ahora puedes volver a chatear normalmente");
                    salida.flush();
                    if (clienteOponente != null) {
                        clienteOponente.salida.writeUTF("¡EMPATE! Bien jugado, obtienes 1 punto.");
                        clienteOponente.salida.writeUTF("Ahora puedes volver a chatear normalmente");
                        clienteOponente.salida.flush();
                    }
                } else {
                    if (ganador.equals(nombreUsuario)) {
                        salida.writeUTF("¡FELICIDADES! Ganaste la partida, obtienes 2 puntos.");
                        salida.writeUTF("Ahora puedes volver a chatear normalmente");
                        salida.flush();
                        if (clienteOponente != null) {
                            clienteOponente.salida.writeUTF("Perdiste la partida, obtienes 0 puntos.");
                            clienteOponente.salida.writeUTF("Ahora puedes volver a chatear normalmente");
                            clienteOponente.salida.flush();
                        }
                    } else {
                        salida.writeUTF("Perdiste la partida, obtienes 0 puntos.");
                        salida.writeUTF("Ahora puedes volver a chatear normalmente");
                        salida.flush();
                        if (clienteOponente != null) {
                            clienteOponente.salida.writeUTF("¡FELICIDADES! Ganaste la partida, obtienes 2 puntos.");
                            clienteOponente.salida.writeUTF("Ahora puedes volver a chatear normalmente");
                            clienteOponente.salida.flush();
                        }
                    }
                }

                GestorJuegos.terminarPartida(nombreUsuario);
            }

        } catch (NumberFormatException e) {
            salida.writeUTF("Las coordenadas deben ser numeros entre 0 y 2");
            salida.flush();
        }
    }

    private void mostrarTableroGato() throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado");
            salida.flush();
            return;
        }

        JuegoGato juego = GestorJuegos.obtenerPartida(nombreUsuario);

        if (juego == null) {
            salida.writeUTF("No tienes una partida activa");
            salida.flush();
            return;
        }

        salida.writeUTF(juego.getTableroVisual());
        salida.flush();
    }

    private void procesarRendirse() throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado");
            salida.flush();
            return;
        }

        JuegoGato juego = GestorJuegos.obtenerPartida(nombreUsuario);

        if (juego == null) {
            salida.writeUTF("No tienes una partida activa");
            salida.flush();
            return;
        }

        String oponente = juego.getOponente(nombreUsuario);
        juego.terminarPorAbandono(nombreUsuario);

        salida.writeUTF("Te has rendido. " + oponente + " gana por abandono, obtienes 0 puntos.");
        salida.writeUTF("Ahora puedes volver a chatear normalmente");
        salida.flush();

        unCliente clienteOponente = buscarClientePorNombre(oponente);
        if (clienteOponente != null) {
            clienteOponente.salida.writeUTF("- " + nombreUsuario + " se rindio. Ganaste por abandono!, obtienes 2 puntos.");
            clienteOponente.salida.writeUTF("Ahora puedes volver a chatear normalmente");
            clienteOponente.salida.flush();
        }

        GestorJuegos.terminarPartida(nombreUsuario);
    }

    private unCliente buscarClientePorNombre(String nombre) {
        for (unCliente c : ServidorMulti.clientes.values()) {
            if (c.autenticado && c.nombreUsuario != null && c.nombreUsuario.equals(nombre)) {
                return c;
            }
        }
        return null;
    }
    private void procesarRanking() throws IOException {
    if (!autenticado) {
        salida.writeUTF("Debes estar autenticado para ver el ranking");
        salida.flush();
        return;
    }
    
    String ranking = DatabaseManager.obtenerRanking(10);
    salida.writeUTF(ranking);
    salida.flush();
}
    private void procesarEstadisticas(String mensaje) throws IOException {
    if (!autenticado) {
        salida.writeUTF("Debes estar autenticado para ver estadisticas");
        salida.flush();
        return;
    }
    
    String[] partes = mensaje.split(" ");
    if (partes.length != 3) {
        salida.writeUTF("Usa: /estadisticas <usuario1> <usuario2>");
        salida.flush();
        return;
    }
    
    String jugador1 = partes[1];
    String jugador2 = partes[2];
    
    String estadisticas = DatabaseManager.obtenerEstadisticasEntre(jugador1, jugador2);
    salida.writeUTF(estadisticas);
    salida.flush();
}
    
}
