package servidorMulti;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:chat_usuarios.db";

    public static void inicializarBaseDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

            crearTablaUsuarios(stmt);
            crearTablaBloqueos(stmt);
            crearTablaEstadisticasGato(stmt);
            crearTablaHistorialGato(stmt);
            crearTablaGrupos(stmt);
            crearTablaMiembrosGrupo(stmt);
            crearTablaMensajesGrupo(stmt);
            crearTablaLecturasMensajes(stmt);

            inicializarGrupoTodos();

            System.out.println("[DB] Base de datos inicializada correctamente");

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al inicializar: " + e.getMessage());
        }
    }

    private static void crearTablaUsuarios(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS usuarios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "usuario TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaBloqueos(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS bloqueos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "usuario_bloqueador TEXT NOT NULL,"
                + "usuario_bloqueado TEXT NOT NULL,"
                + "fecha_bloqueo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE(usuario_bloqueador, usuario_bloqueado)"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaEstadisticasGato(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS estadisticas_gato ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "usuario TEXT NOT NULL,"
                + "victorias INTEGER DEFAULT 0,"
                + "empates INTEGER DEFAULT 0,"
                + "derrotas INTEGER DEFAULT 0,"
                + "puntos INTEGER DEFAULT 0,"
                + "FOREIGN KEY(usuario) REFERENCES usuarios(usuario),"
                + "UNIQUE(usuario)"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaHistorialGato(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS historial_gato ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "jugador1 TEXT NOT NULL,"
                + "jugador2 TEXT NOT NULL,"
                + "ganador TEXT,"
                + "fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(jugador1) REFERENCES usuarios(usuario),"
                + "FOREIGN KEY(jugador2) REFERENCES usuarios(usuario)"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaGrupos(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS grupos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "nombre TEXT UNIQUE NOT NULL,"
                + "creador TEXT NOT NULL,"
                + "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "es_sistema INTEGER DEFAULT 0"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaMiembrosGrupo(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS miembros_grupo ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "grupo_id INTEGER NOT NULL,"
                + "usuario TEXT NOT NULL,"
                + "fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(grupo_id) REFERENCES grupos(id),"
                + "UNIQUE(grupo_id, usuario)"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaMensajesGrupo(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mensajes_grupo ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "grupo_id INTEGER NOT NULL,"
                + "usuario TEXT NOT NULL,"
                + "mensaje TEXT NOT NULL,"
                + "fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(grupo_id) REFERENCES grupos(id)"
                + ")";
        stmt.execute(sql);
    }

    private static void crearTablaLecturasMensajes(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS lecturas_mensajes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "mensaje_id INTEGER NOT NULL,"
                + "usuario TEXT NOT NULL,"
                + "FOREIGN KEY(mensaje_id) REFERENCES mensajes_grupo(id),"
                + "UNIQUE(mensaje_id, usuario)"
                + ")";
        stmt.execute(sql);
    }

    private static void inicializarGrupoTodos() {
        String sqlCheck = "SELECT COUNT(*) FROM grupos WHERE nombre = 'Todos'";
        String sqlInsert = "INSERT INTO grupos (nombre, creador, es_sistema) VALUES ('Todos', 'SYSTEM', 1)";

        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(sqlCheck);
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(sqlInsert);
                System.out.println("[DB] Grupo 'Todos' creado");
            }

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error creando grupo Todos: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ============== USUARIOS ==============
    public static boolean registrarUsuario(String usuario, String password) {
        String sql = "INSERT INTO usuarios (usuario, password) VALUES (?, ?)";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            System.out.println("[DB] Usuario registrado: " + usuario);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return false;
            }
            System.err.println("[DB ERROR] " + e.getMessage());
            return false;
        }
    }

    public static boolean verificarLogin(String usuario, String password) {
        String sql = "SELECT password FROM usuarios WHERE usuario = ?";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
            return false;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] " + e.getMessage());
            return false;
        }
    }

    public static boolean usuarioExiste(String usuario) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] " + e.getMessage());
            return false;
        }
    }

    public static int contarUsuarios() {
        String sql = "SELECT COUNT(*) FROM usuarios";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    public static String obtenerListaUsuarios() {
        String sql = "SELECT usuario FROM usuarios ORDER BY usuario";
        StringBuilder lista = new StringBuilder();
        int contador = 0;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                if (lista.length() > 0) {
                    lista.append(", ");
                }
                lista.append(rs.getString("usuario"));
                contador++;
            }

            String resultado = lista.length() > 0 ? lista.toString() : "No hay usuarios";
            System.out.println("[DB] Lista de usuarios solicitada (" + contador + " usuarios)");
            return resultado;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error obteniendo lista: " + e.getMessage());
            return "Error al obtener lista";
        }
    }

    public static String obtenerUsuariosConectados() {
        StringBuilder lista = new StringBuilder();

        for (servidorMulti.unCliente cliente : ServidorMulti.getClientes().values()) {
            if (cliente.isAutenticado() && cliente.getNombreUsuario() != null) {
                if (lista.length() > 0) {
                    lista.append(", ");
                }
                lista.append(cliente.getNombreUsuario());
            }
        }

        return lista.length() > 0 ? lista.toString() : "Ninguno";
    }

    // ============== BLOQUEOS ==============
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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bloqueador);
            pstmt.setString(2, bloqueado);
            pstmt.executeUpdate();

            System.out.println("[DB] " + bloqueador + " bloqueo a " + bloqueado);
            return 1;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] " + e.getMessage());
            return 0;
        }
    }

    public static int desbloquearUsuario(String desbloqueador, String desbloqueado) {
        if (!estaBloquedo(desbloqueador, desbloqueado)) {
            return -1;
        }

        String sql = "DELETE FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, desbloqueador);
            pstmt.setString(2, desbloqueado);
            int filas = pstmt.executeUpdate();

            if (filas > 0) {
                System.out.println("[DB] " + desbloqueador + " desbloqueo a " + desbloqueado);
                return 1;
            }
            return 0;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] " + e.getMessage());
            return 0;
        }
    }

    public static boolean estaBloquedo(String bloqueador, String bloqueado) {
        String sql = "SELECT COUNT(*) FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    // ============== ESTADISTICAS DEL GATO ==============
    public static void inicializarEstadisticas(String usuario) {
        String sql = "INSERT OR IGNORE INTO estadisticas_gato (usuario, victorias, empates, derrotas, puntos) VALUES (?, 0, 0, 0, 0)";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al inicializar estadisticas: " + e.getMessage());
        }
    }

    public static void registrarResultado(String jugador1, String jugador2, String ganador) {
        inicializarEstadisticas(jugador1);
        inicializarEstadisticas(jugador2);

        try (Connection conn = getConnection()) {

            String sqlHistorial = "INSERT INTO historial_gato (jugador1, jugador2, ganador) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlHistorial)) {
                pstmt.setString(1, jugador1);
                pstmt.setString(2, jugador2);
                pstmt.setString(3, ganador);
                pstmt.executeUpdate();
            }

            if (ganador.equals("EMPATE")) {
                actualizarEstadisticasEmpate(conn, jugador1);
                actualizarEstadisticasEmpate(conn, jugador2);
                System.out.println("[DB] Empate registrado: " + jugador1 + " vs " + jugador2);
            } else {
                String perdedor = ganador.equals(jugador1) ? jugador2 : jugador1;
                actualizarEstadisticasVictoria(conn, ganador);
                actualizarEstadisticasDerrota(conn, perdedor);
                System.out.println("[DB] Victoria registrada: " + ganador + " derroto a " + perdedor);
            }

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al registrar resultado: " + e.getMessage());
        }
    }

    private static void actualizarEstadisticasVictoria(Connection conn, String usuario) throws SQLException {
        String sql = "UPDATE estadisticas_gato SET victorias = victorias + 1, puntos = puntos + 2 WHERE usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.executeUpdate();
        }
    }

    private static void actualizarEstadisticasDerrota(Connection conn, String usuario) throws SQLException {
        String sql = "UPDATE estadisticas_gato SET derrotas = derrotas + 1 WHERE usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.executeUpdate();
        }
    }

    private static void actualizarEstadisticasEmpate(Connection conn, String usuario) throws SQLException {
        String sql = "UPDATE estadisticas_gato SET empates = empates + 1, puntos = puntos + 1 WHERE usuario = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.executeUpdate();
        }
    }

    public static String obtenerRanking(int limite) {
        String sql = "SELECT usuario, victorias, empates, derrotas, puntos "
                + "FROM estadisticas_gato "
                + "WHERE (victorias + empates + derrotas) > 0 "
                + "ORDER BY puntos DESC, victorias DESC "
                + "LIMIT ?";

        StringBuilder ranking = new StringBuilder();
        int posicion = 1;

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limite);
            ResultSet rs = pstmt.executeQuery();

            ranking.append("\n==========================================\n");
            ranking.append("         RANKING GENERAL - GATO\n");
            ranking.append("==========================================\n");
            ranking.append(String.format("%-4s %-15s %3s %3s %3s %5s\n",
                    "POS", "JUGADOR", "V", "E", "D", "PTS"));
            ranking.append("------------------------------------------\n");

            boolean hayDatos = false;
            while (rs.next()) {
                hayDatos = true;
                String usuario = rs.getString("usuario");
                int victorias = rs.getInt("victorias");
                int empates = rs.getInt("empates");
                int derrotas = rs.getInt("derrotas");
                int puntos = rs.getInt("puntos");

                ranking.append(String.format("%-4d %-15s %3d %3d %3d %5d\n",
                        posicion, usuario, victorias, empates, derrotas, puntos));
                posicion++;
            }

            if (!hayDatos) {
                ranking.append("  No hay estadisticas registradas aun\n");
            }

            ranking.append("==========================================\n");
            ranking.append("V=Victorias E=Empates D=Derrotas PTS=Puntos\n");

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener ranking: " + e.getMessage());
            return "Error al obtener el ranking";
        }

        return ranking.toString();
    }

    public static String obtenerEstadisticasEntre(String jugador1, String jugador2) {
        if (!usuarioExiste(jugador1)) {
            return "El usuario '" + jugador1 + "' no existe";
        }

        if (!usuarioExiste(jugador2)) {
            return "El usuario '" + jugador2 + "' no existe";
        }

        inicializarEstadisticas(jugador1);
        inicializarEstadisticas(jugador2);

        String sql = "SELECT ganador FROM historial_gato "
                + "WHERE (jugador1 = ? AND jugador2 = ?) OR (jugador1 = ? AND jugador2 = ?)";

        int victoriasJ1 = 0;
        int victoriasJ2 = 0;
        int empates = 0;

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, jugador1);
            pstmt.setString(2, jugador2);
            pstmt.setString(3, jugador2);
            pstmt.setString(4, jugador1);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String ganador = rs.getString("ganador");
                if (ganador.equals("EMPATE")) {
                    empates++;
                } else if (ganador.equals(jugador1)) {
                    victoriasJ1++;
                } else if (ganador.equals(jugador2)) {
                    victoriasJ2++;
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB ERROR] Error al obtener estadisticas: " + e.getMessage());
            return "Error al obtener estadisticas";
        }

        int totalPartidas = victoriasJ1 + victoriasJ2 + empates;

        StringBuilder stats = new StringBuilder();
        stats.append("\n==========================================\n");
        stats.append("       ESTADISTICAS ENTRE JUGADORES\n");
        stats.append("==========================================\n");
        stats.append(jugador1).append(" vs ").append(jugador2).append("\n");
        stats.append("------------------------------------------\n");

        if (totalPartidas == 0) {
            stats.append("  No han jugado partidas entre si\n");
        } else {
            double porcentajeJ1 = (victoriasJ1 * 100.0) / totalPartidas;
            double porcentajeJ2 = (victoriasJ2 * 100.0) / totalPartidas;
            double porcentajeEmpate = (empates * 100.0) / totalPartidas;

            stats.append("Total de partidas: ").append(totalPartidas).append("\n\n");
            stats.append(String.format("%-15s: %2d victorias (%.1f%%)\n", jugador1, victoriasJ1, porcentajeJ1));
            stats.append(String.format("%-15s: %2d victorias (%.1f%%)\n", jugador2, victoriasJ2, porcentajeJ2));
            stats.append(String.format("%-15s: %2d empates   (%.1f%%)\n", "Empates", empates, porcentajeEmpate));
        }

        stats.append("==========================================\n");

        return stats.toString();
    }
}
