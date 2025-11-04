package servidorMulti.messenger;

import servidorMulti.*;
import servidorMulti.grupos.MensajeDAO;
import servidorMulti.session.ClientSession;

import java.io.DataOutputStream;
import java.io.IOException;

public class MessageDistributor {
    
    private final DataOutputStream output;
    private final ClientSession session;

    public MessageDistributor(DataOutputStream output, ClientSession session) {
        this.output = output;
        this.session = session;
    }

    public void distributeBroadcastMessage(String message) throws IOException {
        if (!session.isAuthenticated()) {
            distributeAnonymousMessage(message);
            return;
        }

        distributeAuthenticatedMessage(message);
    }

    private void distributeAnonymousMessage(String message) throws IOException {
        String sender = "Cliente #" + session.getClientId();

        for (unCliente client : ServidorMulti.clientes.values()) {
            if (shouldReceiveAnonymousMessage(client)) {
                client.getSalida().writeUTF("[" + sender + "]: " + message);
                client.getSalida().flush();
            }
        }
    }

    private void distributeAuthenticatedMessage(String message) throws IOException {
        saveMessageToGroup(message);
        distributeToGroupMembers(message);
    }

    private void saveMessageToGroup(String message) {
        MensajeDAO.enviarMensaje(
            session.getUsername(), 
            session.getCurrentGroup(), 
            message
        );
    }

    private void distributeToGroupMembers(String message) throws IOException {
        for (unCliente client : ServidorMulti.clientes.values()) {
            if (shouldReceiveGroupMessage(client)) {
                client.getSalida().writeUTF("[" + session.getUsername() + "]: " + message);
                client.getSalida().flush();
            }
        }
    }

    private boolean shouldReceiveAnonymousMessage(unCliente client) {
        return !client.getIdCliente().equals(session.getClientId())
                && !client.isEnPartida()
                && !session.isInGame();
    }

    private boolean shouldReceiveGroupMessage(unCliente client) {
        if (client.getIdCliente().equals(session.getClientId())) {
            return false;
        }

        if (client.isEnPartida() || session.isInGame()) {
            return false;
        }

        if (!client.isAutenticado()) {
            return session.getCurrentGroup().equals("Todos");
        }

        if (!client.getGrupoActual().equals(session.getCurrentGroup())) {
            return false;
        }

        return !hasMutualBlock(client);
    }

    private boolean hasMutualBlock(unCliente client) {
        String clientUsername = client.getNombreUsuario();
        return DatabaseManager.estaBloquedo(clientUsername, session.getUsername())
                || DatabaseManager.estaBloquedo(session.getUsername(), clientUsername);
    }
}