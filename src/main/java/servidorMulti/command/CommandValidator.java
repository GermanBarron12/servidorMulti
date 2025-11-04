package servidorMulti.command;

public class CommandValidator {
    
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;

    public boolean isValidUsername(String username) {
        return username != null && username.length() >= MIN_USERNAME_LENGTH;
    }

    public boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    public boolean isValidCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate <= 2;
    }

    public boolean hasExpectedParts(String[] parts, int expected) {
        return parts != null && parts.length == expected;
    }

    public String extractGroupName(String message) {
        String[] parts = message.split(" ", 2);
        return parts.length == 2 ? parts[1].trim() : null;
    }
}