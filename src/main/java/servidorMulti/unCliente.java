package servidorMulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import servidorMulti.grupos.GestorGrupos;
import servidorMulti.grupos.GrupoDAO;
import servidorMulti.grupos.MensajeDAO;
import servidorMulti.grupos.MensajeGrupo;

public class unCliente implements Runnable {

    final DataOutputStream salida;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataInputStream entrada;
    final String idCliente;
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    private String nombreUsuario = null;
    private boolean enPartida = false;
    private String grupoActual = "Todos";

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

                if (mensaje.equals("/aceptar")) {
                    procesarAceptarGato();
                    continue;
                }

                if (mensaje.equals("/rechazar")) {
                    procesarRechazarGato();
                    continue;
                }

                if (mensaje.startsWith("/jugar")) {
                    procesarMovimientoGato(mensaje);
                    continue;
                }

                if (mensaje.equals("/tablero")) {
                    mostrarTableroGato();
                    continue;
                }

                if (mensaje.equals("/rendirse")) {
                    procesarRendirse();
                    continue;
                }

                if (mensaje.equals("/ranking")) {
                    procesarRanking();
                    continue;
                }

                if (mensaje.startsWith("/stats")) {
                    procesarStats(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/creargrupo ")) {
                    procesarCrearGrupo(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/unirgrupo")) {
                    procesarUnirGrupo(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/entrargrupo")) {
                    procesarEntrarGrupo(mensaje);
                    continue;
                }

                if (mensaje.startsWith("/eliminargrupo")) {
                    procesarEliminarGrupo(mensaje);
                    continue;
                }

                if (mensaje.equals("/grupos")) {
                    procesarListarGrupos();
                    continue;
                }

                if (mensaje.equals("/misgrupos")) {
                    procesarMisGrupos();
                    continue;
                }

                if (mensaje.startsWith("/miembros")) {
                    procesarMiembros(mensaje);
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
                        } catch (IOException ignored) {
                        }
                    }
                    GestorJuegos.terminarPartida(nombreUsuario);
                }
                GestorJuegos.cancelarInvitacion(nombreUsuario);
            }

            ServidorMulti.clientes.remove(idCliente);
            try {
                entrada.close();
                salida.close();
            } catch (IOException ignored) {
            }
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
            GrupoDAO.unirseGrupo(usuario, "Todos");
            grupoActual = "Todos";
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
                       Autenticacion:
                      /registro <usuario> <password>
                      /login <usuario> <password>
                       
                       Mensajeria:
                      mensaje - Enviar a todos
                      @usuario mensaje - Privado
                       
                       Grupos:
                      /creargrupo <nombre> - Crear nuevo grupo
                      /unirgrupo <nombre> - Unirse a grupo
                      /entrargrupo <nombre> - Entrar a grupo
                      /eliminargrupo <nombre> - Eliminar grupo
                      /grupos - Lista de grupos
                      /misgrupos - Mis grupos
                      /miembros [grupo] - Miembros del grupo
                       
                       Informacion:
                      /usuarios - Lista de usuarios registrados
                      /online - Usuarios conectados
                       
                       Bloqueos:
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
                      /stats - <usuario1> <usuario2> - Comparar jugadores
                       
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
        if (!autenticado) {
            enviarBroadcastAnonimo(mensaje);
            return;
        }

        enviarBroadcastAutenticado(mensaje);
    }

    private void enviarBroadcastAnonimo(String mensaje) throws IOException {
        String remitente = "Cliente #" + idCliente;

        for (unCliente cliente : ServidorMulti.clientes.values()) {
            if (debeRecibirMensaje(cliente)) {
                cliente.salida.writeUTF("[" + remitente + "]: " + mensaje);
                cliente.salida.flush();
            }
        }
    }

    private void enviarBroadcastAutenticado(String mensaje) throws IOException {
        guardarMensajeEnGrupo(mensaje);
        distribuirMensajeEnGrupo(mensaje);
    }

    private void guardarMensajeEnGrupo(String mensaje) {
        MensajeDAO.enviarMensaje(nombreUsuario, grupoActual, mensaje);
    }

    private void distribuirMensajeEnGrupo(String mensaje) throws IOException {
        for (unCliente cliente : ServidorMulti.clientes.values()) {
            if (debeRecibirMensajeGrupo(cliente)) {
                cliente.salida.writeUTF("[" + nombreUsuario + "]: " + mensaje);
                cliente.salida.flush();
            }
        }
    }

    private boolean debeRecibirMensaje(unCliente cliente) {
        return !cliente.idCliente.equals(this.idCliente)
                && !cliente.enPartida
                && !this.enPartida;
    }

    private boolean debeRecibirMensajeGrupo(unCliente cliente) {
        if (cliente.idCliente.equals(this.idCliente)) {
            return false;
        }

        if (cliente.enPartida || this.enPartida) {
            return false;
        }

        if (!cliente.autenticado) {
            return grupoActual.equals("Todos");
        }

        if (!cliente.grupoActual.equals(this.grupoActual)) {
            return false;
        }

        return !hayBloqueoMutuo(cliente);
    }

    private boolean hayBloqueoMutuo(unCliente cliente) {
        return DatabaseManager.estaBloquedo(cliente.nombreUsuario, nombreUsuario)
                || DatabaseManager.estaBloquedo(nombreUsuario, cliente.nombreUsuario);
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
            if (DatabaseManager.usuarioExiste(invitado)) {
                salida.writeUTF("El usuario " + invitado + " no esta conectado en este momento");
            } else {
                salida.writeUTF("El usuario '" + invitado + "' no existe");
            }
            salida.flush();
            return;
        }

        if (DatabaseManager.estaBloquedo(nombreUsuario, invitado)) {
            salida.writeUTF("No puedes invitar a " + invitado + " porque lo tienes bloqueado");
            salida.flush();
            return;
        }

        if (DatabaseManager.estaBloquedo(invitado, nombreUsuario)) {
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

        if (clienteOponente != null) {
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
                if (clienteOponente != null) {
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

    private void procesarStats(String mensaje) throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado para ver estadisticas");
            salida.flush();
            return;
        }

        String[] partes = mensaje.split(" ");
        if (partes.length != 3) {
            salida.writeUTF("Usa: /stats <usuario1> <usuario2>");
            salida.flush();
            return;
        }

        String jugador1 = partes[1];
        String jugador2 = partes[2];

        String estadisticas = DatabaseManager.obtenerEstadisticasEntre(jugador1, jugador2);
        salida.writeUTF(estadisticas);
        salida.flush();
    }

    // ============== METODOS DE GRUPOS ==============
    private void procesarCrearGrupo(String mensaje) throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String nombreGrupo = extraerNombreGrupo(mensaje);
        if (nombreGrupo == null) {
            salida.writeUTF("Usa: /creargrupo <nombre>");
            salida.flush();
            return;
        }

        ejecutarCreacionGrupo(nombreGrupo);
    }

    private boolean validarAutenticacionGrupo() throws IOException {
        if (!autenticado) {
            salida.writeUTF("Debes estar autenticado para usar grupos");
            salida.flush();
            return false;
        }
        return true;
    }

    private String extraerNombreGrupo(String mensaje) {
        String[] partes = mensaje.split(" ", 2);
        return partes.length == 2 ? partes[1].trim() : null;
    }

    private void ejecutarCreacionGrupo(String nombreGrupo) throws IOException {
        int resultado = GrupoDAO.crearGrupo(nombreGrupo, nombreUsuario);

        if (resultado == 1) {
            GrupoDAO.unirseGrupo(nombreUsuario, nombreGrupo);
            salida.writeUTF("Grupo '" + nombreGrupo + "' creado exitosamente");
            salida.writeUTF("Te has unido automaticamente al grupo");
        } else if (resultado == -1) {
            salida.writeUTF("El grupo '" + nombreGrupo + "' ya existe");
        } else {
            salida.writeUTF("Error al crear el grupo");
        }
        salida.flush();
    }

    private void procesarUnirGrupo(String mensaje) throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String nombreGrupo = extraerNombreGrupo(mensaje);
        if (nombreGrupo == null) {
            salida.writeUTF("Usa: /unirgrupo <nombre>");
            salida.flush();
            return;
        }

        ejecutarUnionGrupo(nombreGrupo);
    }

    private void ejecutarUnionGrupo(String nombreGrupo) throws IOException {
        int resultado = GrupoDAO.unirseGrupo(nombreUsuario, nombreGrupo);

        if (resultado == 1) {
            salida.writeUTF("Te has unido al grupo '" + nombreGrupo + "'");
            salida.writeUTF("Usa /entrargrupo " + nombreGrupo + " para ver mensajes");
        } else if (resultado == -1) {
            salida.writeUTF("El grupo '" + nombreGrupo + "' no existe");
        } else if (resultado == -2) {
            salida.writeUTF("Ya eres miembro del grupo '" + nombreGrupo + "'");
        } else {
            salida.writeUTF("Error al unirse al grupo");
        }
        salida.flush();
    }

    private void procesarEntrarGrupo(String mensaje) throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String nombreGrupo = extraerNombreGrupo(mensaje);
        if (nombreGrupo == null) {
            salida.writeUTF("Usa: /entrargrupo <nombre>");
            salida.flush();
            return;
        }

        ejecutarEntradaGrupo(nombreGrupo);
    }

    private void ejecutarEntradaGrupo(String nombreGrupo) throws IOException {
        if (!GrupoDAO.esMiembro(nombreUsuario, nombreGrupo)) {
            salida.writeUTF("No eres miembro del grupo '" + nombreGrupo + "'");
            salida.writeUTF("Usa /unirgrupo " + nombreGrupo + " para unirte");
            salida.flush();
            return;
        }

        cambiarAGrupo(nombreGrupo);
    }

    private void cambiarAGrupo(String nombreGrupo) throws IOException {
        grupoActual = nombreGrupo;
        GestorGrupos.cambiarGrupo(nombreUsuario, nombreGrupo);

        salida.writeUTF("Ahora estas en el grupo: " + nombreGrupo);

        mostrarMensajesNoLeidos();

        salida.writeUTF("Escribe mensajes normalmente para este grupo");
        salida.flush();
    }

    private void mostrarMensajesNoLeidos() throws IOException {
        List<MensajeGrupo> mensajes = MensajeDAO.obtenerMensajesNoLeidos(nombreUsuario, grupoActual);

        if (!mensajes.isEmpty()) {
            salida.writeUTF("\n--- Mensajes no leidos ---");

            for (MensajeGrupo msg : mensajes) {
                salida.writeUTF(msg.formatear());
                MensajeDAO.marcarComoLeido(msg.getId(), nombreUsuario);
            }

            salida.writeUTF("--- Fin de mensajes ---\n");
        }
        salida.flush();
    }

    private void procesarEliminarGrupo(String mensaje) throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String nombreGrupo = extraerNombreGrupo(mensaje);
        if (nombreGrupo == null) {
            salida.writeUTF("Usa: /eliminargrupo <nombre>");
            salida.flush();
            return;
        }

        ejecutarEliminacionGrupo(nombreGrupo);
    }

    private void ejecutarEliminacionGrupo(String nombreGrupo) throws IOException {
        int resultado = GrupoDAO.eliminarGrupo(nombreGrupo, nombreUsuario);

        if (resultado == 1) {
            if (grupoActual.equals(nombreGrupo)) {
                grupoActual = "Todos";
            }
            salida.writeUTF("Grupo '" + nombreGrupo + "' eliminado");
        } else if (resultado == -1) {
            salida.writeUTF("El grupo '" + nombreGrupo + "' no existe");
        } else if (resultado == -2) {
            salida.writeUTF("No puedes eliminar el grupo 'Todos'");
        } else if (resultado == -3) {
            salida.writeUTF("Solo el creador puede eliminar el grupo");
        } else {
            salida.writeUTF("Error al eliminar el grupo");
        }
        salida.flush();
    }

    private void procesarListarGrupos() throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String lista = GestorGrupos.formatearListaGrupos();
        salida.writeUTF(lista);
        salida.flush();
    }

    private void procesarMisGrupos() throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String lista = GestorGrupos.formatearMisGrupos(nombreUsuario);
        salida.writeUTF(lista);
        salida.writeUTF("Grupo actual: " + grupoActual);
        salida.flush();
    }

    private void procesarMiembros(String mensaje) throws IOException {
        if (!validarAutenticacionGrupo()) {
            return;
        }

        String nombreGrupo = extraerNombreGrupo(mensaje);
        if (nombreGrupo == null) {
            nombreGrupo = grupoActual;
        }

        String lista = GestorGrupos.formatearMiembros(nombreGrupo);
        salida.writeUTF(lista);
        salida.flush();
    }

}
