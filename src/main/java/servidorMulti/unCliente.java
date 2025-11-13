package servidorMulti;

import servidorMulti.command.CommandProcessor;
import servidorMulti.messenger.MessageDistributor;
import servidorMulti.messenger.PrivateMessageHandler;
import servidorMulti.session.ClientSession;
import servidorMulti.session.GestorSesiones;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class unCliente implements Runnable {

    private final DataOutputStream salida;
    private final DataInputStream entrada;
    private final String idCliente;

    private final ClientSession session;
    private final CommandProcessor commandProcessor;
    private final MessageDistributor messageDistributor;
    private final PrivateMessageHandler privateMessageHandler;

    private boolean enPartida = false;

    public unCliente(Socket socket, String id) throws IOException {
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.entrada = new DataInputStream(socket.getInputStream());
        this.idCliente = id;

        this.session = new ClientSession(id);
        this.commandProcessor = new CommandProcessor(salida, session, this);
        this.messageDistributor = new MessageDistributor(salida, session);
        this.privateMessageHandler = new PrivateMessageHandler(salida, session);
    }

    @Override
    public void run() {
        try {
            sendWelcomeMessage();
            processClientMessages();
        } catch (IOException ex) {
            System.out.println("[DESCONEXION] Cliente #" + idCliente);
        } finally {
            cleanup();
        }
    }

    private void sendWelcomeMessage() throws IOException {
        salida.writeUTF("-------- BIENVENIDO AL CHAT --------");
        salida.writeUTF("Conectado al servidor. ID: #" + idCliente);
        salida.writeUTF("Comandos: /ayuda para ver todos los comandos disponibles");
        salida.flush();
    }

    private void processClientMessages() throws IOException {
        while (true) {
            String message = entrada.readUTF();

            // Comandos especiales que no requieren autenticacion ni cuentan
            if (message.equals("/ayuda")) {
                showHelp();
                continue;
            }

            if ("/salir".equalsIgnoreCase(message)) {
                salida.writeUTF("Hasta pronto!");
                salida.flush();
                break;
            }

            // SIEMPRE permitir autenticacion (no cuenta como mensaje)
            if (isAuthenticationCommand(message)) {
                processCommand(message);
                continue;
            }

            // Si es un comando (empieza con /), procesarlo sin contar
            if (message.startsWith("/")) {
                // Comandos requieren autenticacion (excepto /registro y /login)
                if (!session.isAuthenticated()) {
                    salida.writeUTF("Debes autenticarte primero. Usa: /registro o /login");
                    salida.flush();
                    continue;
                }
                processCommand(message);
                continue;
            }

            // LIMITE SOLO PARA MENSAJES DE CHAT (no comandos)
            if (!session.isAuthenticated() && !session.canSendMessage()) {
                salida.writeUTF("Limite alcanzado. Usa: /registro o /login");
                salida.flush();
                continue;
            }

            // Incrementar SOLO si es mensaje de chat y no esta autenticado
            if (!session.isAuthenticated()) {
                session.incrementMessagesSent();
            }

            // Procesar mensaje de chat
            if (message.startsWith("@")) {
                privateMessageHandler.sendPrivateMessage(message);
            } else {
                messageDistributor.distributeBroadcastMessage(message);
            }
        }
    }

    private boolean isAuthenticationCommand(String message) {
        return message.startsWith("/registro ") || message.startsWith("/login ");
    }

    private void processCommand(String message) throws IOException {
        if (message.startsWith("/registro ")) {
            commandProcessor.processRegistration(message);
        } else if (message.startsWith("/login ")) {
            commandProcessor.processLogin(message);
        } else if (message.startsWith("/bloquear ")) {
            commandProcessor.processBlock(message);
        } else if (message.startsWith("/desbloquear ")) {
            commandProcessor.processUnblock(message);
        } else if (message.equals("/bloqueados")) {
            commandProcessor.processListBlocked();
        } else if (message.equals("/usuarios")) {
            commandProcessor.processListUsers();
        } else if (message.equals("/online")) {
            commandProcessor.processOnlineUsers();
        } else if (message.startsWith("/gato")) {
            commandProcessor.processGameInvitation(message);
        } else if (message.equals("/aceptar")) {
            commandProcessor.processAcceptInvitation();
        } else if (message.equals("/rechazar")) {
            commandProcessor.processRejectInvitation();
        } else if (message.startsWith("/jugar")) {
            commandProcessor.processGameMove(message);
        } else if (message.equals("/tablero")) {
            commandProcessor.processShowBoard();
        } else if (message.equals("/rendirse")) {
            commandProcessor.processSurrender();
        } else if (message.equals("/ranking")) {
            commandProcessor.processRanking();
        } else if (message.startsWith("/stats")) {
            commandProcessor.processStats(message);
        } else if (message.startsWith("/creargrupo ")) {
            commandProcessor.processCreateGroup(message);
        } else if (message.startsWith("/unirgrupo")) {
            commandProcessor.processJoinGroup(message);
        } else if (message.startsWith("/entrargrupo")) {
            commandProcessor.processEnterGroup(message);
        } else if (message.startsWith("/eliminargrupo")) {
            commandProcessor.processDeleteGroup(message);
        } else if (message.equals("/grupos")) {
            commandProcessor.processListGroups();
        } else if (message.equals("/misgrupos")) {
            commandProcessor.processMyGroups();
        } else if (message.startsWith("/miembros")) {
            commandProcessor.processMembers(message);
        }
    }

    private void showHelp() throws IOException {
        String help = """
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
                      /salir - Cerrar conexion
                     --------------------------------------
                     """;

        salida.writeUTF(help);
        salida.flush();
    }

    private void cleanup() {
        cleanupGameSession();

        //  CERRAR SESION DEL GESTOR
        if (session.isAuthenticated() && session.getUsername() != null) {
            GestorSesiones.cerrarSesion(session.getUsername());
        }

        removeClientFromServer();
        closeConnections();
    }

    private void cleanupGameSession() {
        if (!session.isAuthenticated() || session.getUsername() == null) {
            return;
        }

        JuegoGato game = GestorJuegos.obtenerPartida(session.getUsername());

        if (game != null) {
            handleGameAbandonment(game);
        }

        GestorJuegos.cancelarInvitacion(session.getUsername());
    }

    private void handleGameAbandonment(JuegoGato game) {
        String opponent = game.getOponente(session.getUsername());
        game.terminarPorAbandono(session.getUsername());

        unCliente opponentClient = GestorSesiones.obtenerCliente(opponent);

        if (opponentClient != null) {
            opponentClient.setEnPartida(false);
            notifyOpponentAbandonment(opponentClient, opponent);
        }

        GestorJuegos.terminarPartida(session.getUsername());
    }

    private void notifyOpponentAbandonment(unCliente opponentClient, String opponent) {
        try {
            opponentClient.getSalida().writeUTF(session.getUsername()
                    + " se desconecto. Ganaste por abandono!, obtienes 2 puntos.");
            opponentClient.getSalida().writeUTF("Ahora puedes volver a chatear normalmente");
            opponentClient.getSalida().flush();
        } catch (IOException ignored) {
        }
    }

    private void removeClientFromServer() {
        ServidorMulti.removeCliente(idCliente);
    }

    private void closeConnections() {
        try {
            entrada.close();
            salida.close();
        } catch (IOException ignored) {
        }
    }

    // Getters para compatibilidad
    public boolean isAutenticado() {
        return session.isAuthenticated();
    }

    public String getNombreUsuario() {
        return session.getUsername();
    }

    public String getIdCliente() {
        return idCliente;
    }

    public DataOutputStream getSalida() {
        return salida;
    }

    public boolean isEnPartida() {
        return enPartida;
    }

    public void setEnPartida(boolean enPartida) {
        this.enPartida = enPartida;
        session.setInGame(enPartida);
    }

    public String getGrupoActual() {
        return session.getCurrentGroup();
    }
}
