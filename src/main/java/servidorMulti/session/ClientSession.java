package servidorMulti.session;

public class ClientSession {
    private final String clientId;
    private boolean authenticated;
    private String username;
    private int messagesSent;
    private boolean inGame;
    private String currentGroup;

    public ClientSession(String clientId) {
        this.clientId = clientId;
        this.authenticated = false;
        this.messagesSent = 0;
        this.inGame = false;
        this.currentGroup = "Todos";
    }

    // Getters
    public String getClientId() { return clientId; }
    public boolean isAuthenticated() { return authenticated; }
    public String getUsername() { return username; }
    public int getMessagesSent() { return messagesSent; }
    public boolean isInGame() { return inGame; }
    public String getCurrentGroup() { return currentGroup; }

    // Setters
    public void setAuthenticated(boolean authenticated) { 
        this.authenticated = authenticated; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
    
    public void setInGame(boolean inGame) { 
        this.inGame = inGame; 
    }
    
    public void setCurrentGroup(String currentGroup) { 
        this.currentGroup = currentGroup; 
    }

    // Metodos de utilidad
    public void incrementMessagesSent() { 
        this.messagesSent++; 
    }
    
    public void resetMessagesSent() { 
        this.messagesSent = 0; 
    }
    
    public boolean canSendMessage() {
        return authenticated || messagesSent < 3;
    }
}