package servidorMulti;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:chat_usuarios.db";

    public static void inicializarBaseDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL); 
                Statement stmt = conn.createStatement()) {

            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "usuario TEXT UNIQUE NOT NULL,"
                    + "password TEXT NOT NULL,"
                    + "fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            stmt.execute(sqlUsuarios);

            String sqlBloqueos = "CREATE TABLE IF NOT EXISTS bloqueos ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "usuario_bloqueador TEXT NOT NULL,"
                    + "usuario_bloqueado TEXT NOT NULL,"
                    + "fecha_bloqueo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE(usuario_bloqueador, usuario_bloqueado)"
                    + ")";
            stmt.execute(sqlBloqueos);
            
            String sqlEstadisticas = "CREATE TABLE IF NOT EXISTS estadisticas_gato (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "usuario TEXT NOT NULL," +
                "victorias INTEGER DEFAULT 0," +
                "empates INTEGER DEFAULT 0," +
                "derrotas INTEGER DEFAULT 0," +
                "puntos INTEGER DEFAULT 0," +
                "FOREIGN KEY(usuario) REFERENCES usuarios(usuario)," +
                "UNIQUE(usuario)" +
                ")";
        stmt.execute(sqlEstadisticas);
        
        String sqlHistorial = "CREATE TABLE IF NOT EXISTS historial_gato (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "jugador1 TEXT NOT NULL," +
                "jugador2 TEXT NOT NULL," +
                "ganador TEXT," +
                "fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(jugador1) REFERENCES usuarios(usuario)," +
                "FOREIGN KEY(jugador2) REFERENCES usuarios(usuario)" +
                ")";
        stmt.execute(sqlHistorial);

            System.out.println(" [DB] Base de datos inicializada");

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
    }

    public static boolean registrarUsuario(String usuario, String password) {
        String sql = "INSERT INTO usuarios (usuario, password) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            System.out.println(" [DB] Usuario registrado: " + usuario);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return false;
            }
            System.err.println(" [DB ERROR] " + e.getMessage());
            return false;
        }
    }

    public static boolean verificarLogin(String usuario, String password) {
        String sql = "SELECT password FROM usuarios WHERE usuario = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
            return false;

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
            return false;
        }
    }

    public static boolean usuarioExiste(String usuario) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
            return false;
        }
    }

    public static int contarUsuarios() {
        String sql = "SELECT COUNT(*) FROM usuarios";

        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    public static int bloquearUsuario(String bloqueador, String bloqueado) {
        if (bloqueador.equals(bloqueado)) {
            return -1;
        }
        if (!usuarioExiste(bloqueado)) {
            return -2;
        }
        if (estaBloquedo(bloqueador, bloqueado)) {
            return -3;
        }

        String sql = "INSERT INTO bloqueos (usuario_bloqueador, usuario_bloqueado) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            pstmt.executeUpdate();

            System.out.println(" [DB] " + bloqueador + " bloqueo a " + bloqueado);
            return 1;

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
            return 0;
        }
    }

    public static int desbloquearUsuario(String desbloqueador, String desbloqueado) {
        if (!estaBloquedo(desbloqueador, desbloqueado)) {
            return -1;
        }

        String sql = "DELETE FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, desbloqueador);
            pstmt.setString(2, desbloqueado);
            int filas = pstmt.executeUpdate();

            if (filas > 0) {
                System.out.println(" [DB] " + desbloqueador + " desbloqueo a " + desbloqueado);
                return 1;
            }
            return 0;

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
            return 0;
        }
    }

    public static boolean estaBloquedo(String bloqueador, String bloqueado) {
        String sql = "SELECT COUNT(*) FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            ResultSet rs = pstmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            return false;
        }
    }

    public static String obtenerListaBloqueados(String usuario) {
        String sql = "SELECT usuario_bloqueado FROM bloqueos WHERE usuario_bloqueador = ?";
        StringBuilder lista = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                if (lista.length() > 0) {
                    lista.append(", ");
                }
                lista.append(rs.getString("usuario_bloqueado"));
            }

            return lista.length() > 0 ? lista.toString() : "Ninguno";

        } catch (SQLException e) {
            return "Error";
        }
    }

    public static int contarBloqueados(String usuario) {
        String sql = "SELECT COUNT(*) FROM bloqueos WHERE usuario_bloqueador = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Obtiene la lista de todos los usuarios registrados
     */
    public static String obtenerListaUsuarios() {
        String sql = "SELECT usuario FROM usuarios ORDER BY usuario";
        StringBuilder lista = new StringBuilder();
        int contador = 0;

        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                if (lista.length() > 0) {
                    lista.append(", ");
                }
                lista.append(rs.getString("usuario"));
                contador++;
            }

            String resultado = lista.length() > 0 ? lista.toString() : "No hay usuarios";
            System.out.println(" [DB] Lista de usuarios solicitada (" + contador + " usuarios)");
            return resultado;

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] Error obteniendo lista: " + e.getMessage());
            return "Error al obtener lista";
        }
    }

    /**
     * Obtiene la lista de usuarios conectados actualmente
     */
    public static String obtenerUsuariosConectados(java.util.HashMap<String, ?> clientes) {
        StringBuilder lista = new StringBuilder();
        int contador = 0;

        for (Object obj : clientes.values()) {
            if (obj instanceof servidorMulti.unCliente) {
                servidorMulti.unCliente cliente = (servidorMulti.unCliente) obj;
                if (cliente.isAutenticado() && cliente.getNombreUsuario() != null) {
                    if (lista.length() > 0) {
                        lista.append(", ");
                    }
                    lista.append(cliente.getNombreUsuario());
                    contador++;
                }
            }
        }

        return lista.length() > 0 ? lista.toString() : "Ninguno";
    }
}
