package servidorMulti.messenger;

import servidorMulti.*;
import servidorMulti.session.ClientSession;

import java.io.DataOutputStream;
import java.io.IOException;

public class PrivateMessageHandler {
    
    private final DataOutputStream output;
    private final ClientSession session;

    public PrivateMessageHandler(DataOutputStream output, ClientSession session) {
        this.output = output;
        this.session = session;
    }

    public void sendPrivateMessage(String message) throws IOException {
        String[] parts = message.split(" ", 2);
        String recipient = parts[0].substring(1);
        String content = parts.length > 1 ? parts[1] : "";

        unCliente recipientClient = findClientByUsername(recipient);

        if (recipientClient == null) {
            sendMessage("Usuario '" + recipient + "' no conectado");
            return;
        }

        if (!canSendPrivateMessage(recipientClient, recipient)) {
            return;
        }

        deliverPrivateMessage(recipientClient, recipient, content);
    }

    private boolean canSendPrivateMessage(unCliente recipient, String recipientName) throws IOException {
        if (arePlayingTogether(recipient, recipientName)) {
            return true;
        }

        if (session.isInGame()) {
            sendMessage("No puedes enviar mensajes mientras estas en una partida");
            return false;
        }

        if (recipient.isEnPartida()) {
            sendMessage("No puedes enviar mensajes a " + recipientName + " porque esta en una partida");
            return false;
        }

        if (isBlocked(recipient, recipientName)) {
            return false;
        }

        return true;
    }

    private boolean arePlayingTogether(unCliente recipient, String recipientName) {
        if (!session.isInGame() || !recipient.isEnPartida()) {
            return false;
        }

        JuegoGato game = GestorJuegos.obtenerPartida(session.getUsername());
        return game != null && game.contieneJugador(recipientName);
    }

    private boolean isBlocked(unCliente recipient, String recipientName) throws IOException {
        if (!session.isAuthenticated()) {
            return false;
        }

        if (DatabaseManager.estaBloquedo(recipientName, session.getUsername())) {
            sendMessage("No puedes enviar mensajes a '" + recipientName + "'");
            return true;
        }

        if (DatabaseManager.estaBloquedo(session.getUsername(), recipientName)) {
            sendMessage("Has bloqueado a '" + recipientName + "'");
            return true;
        }

        return false;
    }

    private void deliverPrivateMessage(unCliente recipient, String recipientName, String content) throws IOException {
        String sender = session.isAuthenticated() 
            ? session.getUsername() 
            : "Cliente #" + session.getClientId();

        recipient.getSalida().writeUTF("[Privado de " + sender + "]: " + content);
        recipient.getSalida().flush();

        sendMessage("[Enviado a " + recipientName + "]");
    }

    private unCliente findClientByUsername(String username) {
        for (unCliente c : ServidorMulti.clientes.values()) {
            if (c.isAutenticado() && c.getNombreUsuario() != null && 
                c.getNombreUsuario().equals(username)) {
                return c;
            }
        }
        return null;
    }

    private void sendMessage(String message) throws IOException {
        output.writeUTF(message);
        output.flush();
    }
}