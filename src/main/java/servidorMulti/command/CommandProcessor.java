package servidorMulti.command;

import servidorMulti.*;
import servidorMulti.grupos.*;
import servidorMulti.session.ClientSession;
import servidorMulti.session.GestorSesiones;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class CommandProcessor {
    
    private final DataOutputStream output;
    private final ClientSession session;
    private final CommandValidator validator;
    private final unCliente client;

    public CommandProcessor(DataOutputStream output, ClientSession session, unCliente client) {
        this.output = output;
        this.session = session;
        this.validator = new CommandValidator();
        this.client = client;
    }

    // ========== AUTENTICACION ==========
    
    public void processRegistration(String message) throws IOException {
        String[] parts = message.split(" ");
        
        if (!validator.hasExpectedParts(parts, 3)) {
            sendMessage("Usa: /registro <usuario> <password>");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (!validator.isValidUsername(username)) {
            sendMessage("Usuario minimo 3 caracteres");
            return;
        }

        if (!validator.isValidPassword(password)) {
            sendMessage("Contrasena minimo 4 caracteres");
            return;
        }

        // Verificar si ya existe el usuario
        if (DatabaseManager.usuarioExiste(username)) {
            sendMessage("El usuario '" + username + "' ya existe.");
            return;
        }

        // Registrar en base de datos
        if (!DatabaseManager.registrarUsuario(username, password)) {
            sendMessage("Error al registrar usuario");
            return;
        }

        // Verificar si ya tiene sesion activa
        if (GestorSesiones.tieneSesionActiva(username)) {
            sendMessage("Este usuario ya esta conectado desde otro lugar");
            sendMessage("Cierra la otra sesion primero");
            return;
        }

        // Registrar sesion
        if (!GestorSesiones.registrarSesion(username, client)) {
            sendMessage("Error: No se pudo iniciar sesion");
            return;
        }

        session.setAuthenticated(true);
        session.setUsername(username);
        session.resetMessagesSent();
        
        sendMessage("Registro exitoso. Bienvenido, " + username + "!");
        sendMessage("Ahora puedes enviar mensajes ilimitados.");
        
        GrupoDAO.unirseGrupo(username, "Todos");
        session.setCurrentGroup("Todos");
    }

    public void processLogin(String message) throws IOException {
        String[] parts = message.split(" ");
        
        if (!validator.hasExpectedParts(parts, 3)) {
            sendMessage("Usa: /login <usuario> <password>");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        if (!DatabaseManager.usuarioExiste(username)) {
            sendMessage("El usuario '" + username + "' no existe.");
            return;
        }

        if (!DatabaseManager.verificarLogin(username, password)) {
            sendMessage("Contrasena incorrecta.");
            return;
        }

        //  VERIFICAR SI YA ESTÁ CONECTADO
        if (GestorSesiones.tieneSesionActiva(username)) {
            sendMessage("========================================");
            sendMessage("Este usuario ya esta conectado");
            sendMessage("Cierra la otra sesion primero");
            sendMessage("========================================");
            return;
        }

        // Registrar sesion
        if (!GestorSesiones.registrarSesion(username, client)) {
            sendMessage("Error: No se pudo iniciar sesion");
            return;
        }

        session.setAuthenticated(true);
        session.setUsername(username);
        session.resetMessagesSent();

        sendMessage("Inicio de sesion exitoso. Bienvenido, " + username + "!");
        sendMessage("Ahora puedes enviar mensajes ilimitados.");
    }

    // ========== BLOQUEOS ==========
    
    public void processBlock(String message) throws IOException {
        if (!requireAuthentication()) return;

        String[] parts = message.split(" ");
        if (!validator.hasExpectedParts(parts, 2)) {
            sendMessage("Usa: /bloquear <usuario>");
            return;
        }

        String userToBlock = parts[1];
        int result = DatabaseManager.bloquearUsuario(session.getUsername(), userToBlock);

        switch (result) {
            case 1 -> sendMessage("Has bloqueado a '" + userToBlock + "'");
            case -1 -> sendMessage("No puedes bloquearte a ti mismo");
            case -2 -> sendMessage("El usuario '" + userToBlock + "' no existe");
            case -3 -> sendMessage("Ya esta bloqueado");
            default -> sendMessage("Error al bloquear");
        }
    }

    public void processUnblock(String message) throws IOException {
        if (!requireAuthentication()) return;

        String[] parts = message.split(" ");
        if (!validator.hasExpectedParts(parts, 2)) {
            sendMessage("Usa: /desbloquear <usuario>");
            return;
        }

        String userToUnblock = parts[1];
        int result = DatabaseManager.desbloquearUsuario(session.getUsername(), userToUnblock);

        switch (result) {
            case 1 -> sendMessage("Has desbloqueado a '" + userToUnblock + "'");
            case -1 -> sendMessage("No esta bloqueado");
            default -> sendMessage("Error al desbloquear");
        }
    }

    public void processListBlocked() throws IOException {
        if (!requireAuthentication()) return;

        String list = DatabaseManager.obtenerListaBloqueados(session.getUsername());
        int count = DatabaseManager.contarBloqueados(session.getUsername());
        sendMessage("Bloqueados (" + count + "): " + list);
    }

    // ========== USUARIOS ==========
    
    public void processListUsers() throws IOException {
        if (!requireAuthentication()) return;

        String list = DatabaseManager.obtenerListaUsuarios();
        int total = DatabaseManager.contarUsuarios();

        sendMessage("-------- USUARIOS REGISTRADOS --------");
        sendMessage("Total: " + total + " usuarios");
        sendMessage("*" + list);
    }

    public void processOnlineUsers() throws IOException {
        if (!requireAuthentication()) return;

        String list = DatabaseManager.obtenerUsuariosConectados();
        int totalConnected = GestorSesiones.contarSesiones();

        sendMessage("-------- USUARIOS ONLINE --------");
        sendMessage("Conectados: " + totalConnected + " usuarios");
        sendMessage("*" + list);
    }

    // ========== JUEGO GATO ==========
    
    public void processGameInvitation(String message) throws IOException {
        if (!requireAuthentication()) return;

        String[] parts = message.split(" ");
        if (!validator.hasExpectedParts(parts, 2)) {
            sendMessage("Usa: /gato <usuario>");
            return;
        }

        String invitee = parts[1];
        unCliente inviteeClient = findClientByUsername(invitee);

        if (!validateGameInvitation(invitee, inviteeClient)) return;

        int result = GestorJuegos.enviarInvitacion(session.getUsername(), invitee);
        handleInvitationResult(result, invitee, inviteeClient);
    }

    private boolean validateGameInvitation(String invitee, unCliente inviteeClient) throws IOException {
        if (inviteeClient == null) {
            if (DatabaseManager.usuarioExiste(invitee)) {
                sendMessage("El usuario " + invitee + " no esta conectado en este momento");
            } else {
                sendMessage("El usuario '" + invitee + "' no existe");
            }
            return false;
        }

        if (DatabaseManager.estaBloquedo(session.getUsername(), invitee)) {
            sendMessage("No puedes invitar a " + invitee + " porque lo tienes bloqueado");
            return false;
        }

        if (DatabaseManager.estaBloquedo(invitee, session.getUsername())) {
            sendMessage("No puedes invitar a " + invitee + " en este momento");
            return false;
        }

        return true;
    }

    private void handleInvitationResult(int result, String invitee, unCliente inviteeClient) throws IOException {
        switch (result) {
            case 1:
                sendMessage("Invitacion enviada a " + invitee);
                sendInvitationNotification(inviteeClient);
                break;
            case -1:
                sendMessage(invitee + " ya tiene una invitacion pendiente");
                break;
            case -2:
                sendMessage("Ya tienes una partida activa con " + invitee);
                break;
            case -3:
                sendMessage("No puedes jugar contigo mismo");
                break;
            case -4:
                sendMessage("Ya estas en una partida. Terminala primero antes de invitar a alguien mas");
                break;
            case -5:
                sendMessage(invitee + " ya esta en una partida activa");
                break;
            default:
                sendMessage("Error al enviar invitacion");
        }
    }

    private void sendInvitationNotification(unCliente inviteeClient) throws IOException {
        String notification = String.format("""
        ------------------------------------
              INVITACION AL JUEGO GATO        
        ------------------------------------
         %s te invita a jugar al Gato
           Usa: /aceptar  o  /rechazar
        """, session.getUsername());

        inviteeClient.getSalida().writeUTF(notification);
        inviteeClient.getSalida().flush();
    }

    public void processAcceptInvitation() throws IOException {
        if (!requireAuthentication()) return;

        if (!GestorJuegos.tieneInvitacionPendiente(session.getUsername())) {
            sendMessage("No tienes invitaciones pendientes");
            return;
        }

        JuegoGato game = GestorJuegos.aceptarInvitacion(session.getUsername());

        if (game == null) {
            sendMessage("Error al aceptar invitacion");
            return;
        }

        session.setInGame(true);
        client.setEnPartida(true);

        String opponent = game.getOponente(session.getUsername());
        unCliente opponentClient = findClientByUsername(opponent);

        if (opponentClient != null) {
            opponentClient.setEnPartida(true);
        }

        sendGameStartMessages(game, opponentClient);
    }

    private void sendGameStartMessages(JuegoGato game, unCliente opponentClient) throws IOException {
        String board = game.getTableroVisual();

        String startMessage = String.format("""
        Partida iniciada!
        %s
        Usa: /jugar <fila> <columna> (0-2)
        Usa: /tablero para ver el tablero
        Usa: /rendirse para abandonar
        NOTA: No recibiras mensajes del chat mientras juegas
        """, board);

        sendMessage(startMessage);

        if (opponentClient != null) {
            String opponentMessage = String.format("""
            %s acepto! Partida iniciada!
            %s
            Usa: /jugar <fila> <columna> (0-2)
            Usa: /tablero para ver el tablero
            Usa: /rendirse para abandonar
            NOTA: No recibiras mensajes del chat mientras juegas
            """, session.getUsername(), board);

            opponentClient.getSalida().writeUTF(opponentMessage);
            opponentClient.getSalida().flush();
        }
    }

    public void processRejectInvitation() throws IOException {
        if (!requireAuthentication()) return;

        if (!GestorJuegos.tieneInvitacionPendiente(session.getUsername())) {
            sendMessage("No tienes invitaciones pendientes");
            return;
        }

        String inviter = GestorJuegos.getInvitador(session.getUsername());
        GestorJuegos.rechazarInvitacion(session.getUsername());

        sendMessage("Invitacion rechazada");

        unCliente inviterClient = findClientByUsername(inviter);
        if (inviterClient != null) {
            inviterClient.getSalida().writeUTF(session.getUsername() + " rechazo tu invitacion al Gato");
            inviterClient.getSalida().flush();
        }
    }

    public void processGameMove(String message) throws IOException {
        if (!requireAuthentication()) return;

        JuegoGato game = GestorJuegos.obtenerPartida(session.getUsername());

        if (game == null) {
            sendMessage("No tienes una partida activa");
            return;
        }

        String[] parts = message.split(" ");
        if (!validator.hasExpectedParts(parts, 3)) {
            sendMessage("Usa: /jugar <fila> <columna> (0-2)");
            return;
        }

        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);

            if (!validator.isValidCoordinate(row) || !validator.isValidCoordinate(col)) {
                sendMessage("Las coordenadas deben ser numeros entre 0 y 2");
                return;
            }

            processValidMove(game, row, col);

        } catch (NumberFormatException e) {
            sendMessage("Las coordenadas deben ser numeros entre 0 y 2");
        }
    }

    private void processValidMove(JuegoGato game, int row, int col) throws IOException {
        boolean validMove = game.realizarMovimiento(session.getUsername(), row, col);

        if (!validMove) {
            if (!game.getTurnoActual().equals(session.getUsername())) {
                sendMessage("No es tu turno");
            } else {
                sendMessage("Movimiento invalido. La casilla debe estar vacia y dentro del tablero");
            }
            return;
        }

        updateGameBoard(game);

        if (game.isJuegoTerminado()) {
            handleGameEnd(game);
        }
    }

    private void updateGameBoard(JuegoGato game) throws IOException {
        String board = game.getTableroVisual();
        sendMessage(board);

        String opponent = game.getOponente(session.getUsername());
        unCliente opponentClient = findClientByUsername(opponent);

        if (opponentClient != null) {
            opponentClient.getSalida().writeUTF(board);
            opponentClient.getSalida().flush();
        }
    }

    private void handleGameEnd(JuegoGato game) throws IOException {
        String winner = game.getGanador();
        String opponent = game.getOponente(session.getUsername());
        unCliente opponentClient = findClientByUsername(opponent);

        session.setInGame(false);
        client.setEnPartida(false);
        
        if (opponentClient != null) {
            opponentClient.setEnPartida(false);
        }

        DatabaseManager.registrarResultado(game.getJugador1(), game.getJugador2(), winner);
        sendGameResults(winner, opponent, opponentClient);
        GestorJuegos.terminarPartida(session.getUsername());
    }

    private void sendGameResults(String winner, String opponent, unCliente opponentClient) throws IOException {
        if (winner.equals("EMPATE")) {
            sendMessage("EMPATE! Bien jugado, obtienes 1 punto.\nAhora puedes volver a chatear normalmente");
            if (opponentClient != null) {
                opponentClient.getSalida().writeUTF("EMPATE! Bien jugado, obtienes 1 punto.\nAhora puedes volver a chatear normalmente");
                opponentClient.getSalida().flush();
            }
        } else if (winner.equals(session.getUsername())) {
            sendMessage("FELICIDADES! Ganaste la partida, obtienes 2 puntos.\nAhora puedes volver a chatear normalmente");
            if (opponentClient != null) {
                opponentClient.getSalida().writeUTF("Perdiste la partida, obtienes 0 puntos.\nAhora puedes volver a chatear normalmente");
                opponentClient.getSalida().flush();
            }
        } else {
            sendMessage("Perdiste la partida, obtienes 0 puntos.\nAhora puedes volver a chatear normalmente");
            if (opponentClient != null) {
                opponentClient.getSalida().writeUTF("FELICIDADES! Ganaste la partida, obtienes 2 puntos.\nAhora puedes volver a chatear normalmente");
                opponentClient.getSalida().flush();
            }
        }
    }

    public void processShowBoard() throws IOException {
        if (!requireAuthentication()) return;

        JuegoGato game = GestorJuegos.obtenerPartida(session.getUsername());

        if (game == null) {
            sendMessage("No tienes una partida activa");
            return;
        }

        sendMessage(game.getTableroVisual());
    }

    public void processSurrender() throws IOException {
        if (!requireAuthentication()) return;

        JuegoGato game = GestorJuegos.obtenerPartida(session.getUsername());

        if (game == null) {
            sendMessage("No tienes una partida activa");
            return;
        }

        String opponent = game.getOponente(session.getUsername());
        game.terminarPorAbandono(session.getUsername());

        sendMessage("Te has rendido. " + opponent + " gana por abandono, obtienes 0 puntos.\nAhora puedes volver a chatear normalmente");

        unCliente opponentClient = findClientByUsername(opponent);
        if (opponentClient != null) {
            opponentClient.getSalida().writeUTF(session.getUsername() + " se rindio. Ganaste por abandono!, obtienes 2 puntos.\nAhora puedes volver a chatear normalmente");
            opponentClient.getSalida().flush();
        }

        GestorJuegos.terminarPartida(session.getUsername());
    }

    public void processRanking() throws IOException {
        if (!requireAuthentication()) return;
        String ranking = DatabaseManager.obtenerRanking(10);
        sendMessage(ranking);
    }

    public void processStats(String message) throws IOException {
        if (!requireAuthentication()) return;

        String[] parts = message.split(" ");
        if (!validator.hasExpectedParts(parts, 3)) {
            sendMessage("Usa: /stats <usuario1> <usuario2>");
            return;
        }

        String player1 = parts[1];
        String player2 = parts[2];

        String stats = DatabaseManager.obtenerEstadisticasEntre(player1, player2);
        sendMessage(stats);
    }

    // ========== GRUPOS ==========
    
    public void processCreateGroup(String message) throws IOException {
        if (!requireAuthentication()) return;

        String groupName = validator.extractGroupName(message);
        if (groupName == null) {
            sendMessage("Usa: /creargrupo <nombre>");
            return;
        }

        int result = GrupoDAO.crearGrupo(groupName, session.getUsername());

        if (result == 1) {
            GrupoDAO.unirseGrupo(session.getUsername(), groupName);
            sendMessage("Grupo '" + groupName + "' creado exitosamente");
            sendMessage("Te has unido automaticamente al grupo");
        } else if (result == -1) {
            sendMessage("El grupo '" + groupName + "' ya existe");
        } else {
            sendMessage("Error al crear el grupo");
        }
    }

    public void processJoinGroup(String message) throws IOException {
        if (!requireAuthentication()) return;

        String groupName = validator.extractGroupName(message);
        if (groupName == null) {
            sendMessage("Usa: /unirgrupo <nombre>");
            return;
        }

        int result = GrupoDAO.unirseGrupo(session.getUsername(), groupName);

        if (result == 1) {
            sendMessage("Te has unido al grupo '" + groupName + "'");
            sendMessage("Usa /entrargrupo " + groupName + " para ver mensajes");
        } else if (result == -1) {
            sendMessage("El grupo '" + groupName + "' no existe");
        } else if (result == -2) {
            sendMessage("Ya eres miembro del grupo '" + groupName + "'");
        } else {
            sendMessage("Error al unirse al grupo");
        }
    }

    public void processEnterGroup(String message) throws IOException {
        if (!requireAuthentication()) return;

        String groupName = validator.extractGroupName(message);
        if (groupName == null) {
            sendMessage("Usa: /entrargrupo <nombre>");
            return;
        }

        if (!GrupoDAO.esMiembro(session.getUsername(), groupName)) {
            sendMessage("No eres miembro del grupo '" + groupName + "'");
            sendMessage("Usa /unirgrupo " + groupName + " para unirte");
            return;
        }

        session.setCurrentGroup(groupName);
        GestorGrupos.cambiarGrupo(session.getUsername(), groupName);

        sendMessage("Ahora estas en el grupo: " + groupName);
        showUnreadMessages();
        sendMessage("Escribe mensajes normalmente para este grupo");
    }

    private void showUnreadMessages() throws IOException {
        List<MensajeGrupo> messages = MensajeDAO.obtenerMensajesNoLeidos(
            session.getUsername(), session.getCurrentGroup());

        if (!messages.isEmpty()) {
            sendMessage("\n--- Mensajes no leidos ---");

            for (MensajeGrupo msg : messages) {
                sendMessage(msg.formatear());
                MensajeDAO.marcarComoLeido(msg.getId(), session.getUsername());
            }

            sendMessage("--- Fin de mensajes ---\n");
        }
    }

    public void processDeleteGroup(String message) throws IOException {
        if (!requireAuthentication()) return;

        String groupName = validator.extractGroupName(message);
        if (groupName == null) {
            sendMessage("Usa: /eliminargrupo <nombre>");
            return;
        }

        int result = GrupoDAO.eliminarGrupo(groupName, session.getUsername());

        if (result == 1) {
            if (session.getCurrentGroup().equals(groupName)) {
                session.setCurrentGroup("Todos");
            }
            sendMessage("Grupo '" + groupName + "' eliminado");
        } else if (result == -1) {
            sendMessage("El grupo '" + groupName + "' no existe");
        } else if (result == -2) {
            sendMessage("No puedes eliminar el grupo 'Todos'");
        } else if (result == -3) {
            sendMessage("Solo el creador puede eliminar el grupo");
        } else {
            sendMessage("Error al eliminar el grupo");
        }
    }

    public void processListGroups() throws IOException {
        if (!requireAuthentication()) return;
        String list = GestorGrupos.formatearListaGrupos();
        sendMessage(list);
    }

    public void processMyGroups() throws IOException {
        if (!requireAuthentication()) return;
        String list = GestorGrupos.formatearMisGrupos(session.getUsername());
        sendMessage(list);
        sendMessage("Grupo actual: " + session.getCurrentGroup());
    }

    public void processMembers(String message) throws IOException {
        if (!requireAuthentication()) return;

        String groupName = validator.extractGroupName(message);
        if (groupName == null) {
            groupName = session.getCurrentGroup();
        }

        String list = GestorGrupos.formatearMiembros(groupName);
        sendMessage(list);
    }

    // ========== UTILIDADES ==========
    
    private boolean requireAuthentication() throws IOException {
        if (!session.isAuthenticated()) {
            sendMessage("Debes estar autenticado");
            return false;
        }
        return true;
    }

    private unCliente findClientByUsername(String username) {
        return GestorSesiones.obtenerCliente(username);
    }

    private void sendMessage(String message) throws IOException {
        output.writeUTF(message);
        output.flush();
    }

    public ClientSession getSession() {
        return session;
    }
}